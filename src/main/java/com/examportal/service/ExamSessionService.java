package com.examportal.service;

import com.examportal.dto.*;
import com.examportal.model.*;
import com.examportal.repository.ExamAttemptRepository;
import com.examportal.repository.ExamRepository;
import com.examportal.repository.QuestionResponseRepository;
import com.examportal.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamSessionService {

    public static final int MAX_STRIKES = 3;

    private final ExamAttemptRepository attemptRepository;
    private final QuestionResponseRepository responseRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;

    public ExamSessionService(ExamAttemptRepository attemptRepository,
                              QuestionResponseRepository responseRepository,
                              ExamRepository examRepository,
                              UserRepository userRepository) {
        this.attemptRepository = attemptRepository;
        this.responseRepository = responseRepository;
        this.examRepository = examRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public StartExamResponse startExam(Long examId, Long userId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found: " + examId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (!Boolean.TRUE.equals(exam.getPublished())) {
            throw new IllegalStateException("Exam is not currently published");
        }

        if (exam.getQuestions().isEmpty()) {
            throw new IllegalStateException("This exam does not contain any questions yet");
        }

        // Check if there is already an active in-progress attempt
        List<ExamAttempt> existing = attemptRepository.findByUserIdOrderByStartTimeDesc(userId);
        for (ExamAttempt prev : existing) {
            if ("IN_PROGRESS".equals(prev.getStatus()) && prev.getExam().getId().equals(examId)) {
                // Check if time expired
                long elapsedSeconds = Duration.between(prev.getStartTime(), LocalDateTime.now()).getSeconds();
                long totalAllowedSeconds = (long) prev.getExam().getDurationMinutes() * 60;
                if (elapsedSeconds >= totalAllowedSeconds) {
                    // Auto submit previous attempt
                    submitExam(userId, new SubmitExamRequest());
                } else {
                    // Resume active attempt
                    return buildStartExamResponse(prev, totalAllowedSeconds - elapsedSeconds);
                }
            }
        }

        // Create new attempt
        ExamAttempt attempt = new ExamAttempt(user, exam);
        attempt = attemptRepository.save(attempt);

        List<Question> questions = new ArrayList<>(exam.getQuestions());
        if (Boolean.TRUE.equals(exam.getShuffleQuestions())) {
            Collections.shuffle(questions);
        }

        List<QuestionResponse> responses = new ArrayList<>();
        for (Question q : questions) {
            QuestionResponse qr = new QuestionResponse(attempt, q);
            responses.add(responseRepository.save(qr));
        }
        attempt.setResponses(responses);

        long totalSeconds = (long) exam.getDurationMinutes() * 60;
        return buildStartExamResponse(attempt, totalSeconds);
    }

    private StartExamResponse buildStartExamResponse(ExamAttempt attempt, long remainingSeconds) {
        Exam exam = attempt.getExam();
        StartExamResponse response = new StartExamResponse();
        response.setAttemptId(attempt.getId());
        response.setExamId(exam.getId());
        response.setExamTitle(exam.getTitle());
        response.setCategory(exam.getCategory());
        response.setDurationMinutes(exam.getDurationMinutes());
        response.setTotalMarks(exam.getTotalMarks());
        response.setNegativeMarksPerWrong(exam.getNegativeMarksPerWrong());
        response.setStartTime(attempt.getStartTime());
        response.setRemainingSeconds(Math.max(0, remainingSeconds));
        response.setStrikes(attempt.getTabSwitchStrikes());

        List<ExamQuestionDto> questionDtos = new ArrayList<>();
        int qNum = 1;
        for (QuestionResponse qr : attempt.getResponses()) {
            Question q = qr.getQuestion();
            ExamQuestionDto qDto = new ExamQuestionDto();
            qDto.setId(q.getId());
            qDto.setQuestionNumber(qNum++);
            qDto.setQuestionText(q.getQuestionText());
            qDto.setOptionA(q.getOptionA());
            qDto.setOptionB(q.getOptionB());
            qDto.setOptionC(q.getOptionC());
            qDto.setOptionD(q.getOptionD());
            qDto.setMarks(q.getMarks());
            qDto.setDifficulty(q.getDifficulty());
            qDto.setSelectedOption(qr.getSelectedOption());
            qDto.setMarkedForReview(qr.getMarkedForReview());
            questionDtos.add(qDto);
        }
        response.setQuestions(questionDtos);
        return response;
    }

    @Transactional
    public void saveAnswer(Long userId, SaveAnswerRequest request) {
        ExamAttempt attempt = attemptRepository.findByIdAndUserId(request.getAttemptId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid attempt session"));

        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw new IllegalStateException("Attempt is no longer active");
        }

        QuestionResponse response = responseRepository.findByExamAttemptIdAndQuestionId(
                request.getAttemptId(), request.getQuestionId())
                .orElseThrow(() -> new IllegalArgumentException("Question response not found"));

        if (request.getSelectedOption() != null) {
            String opt = request.getSelectedOption().trim().toUpperCase();
            response.setSelectedOption(opt.isEmpty() ? null : opt);
        } else {
            response.setSelectedOption(null);
        }

        if (request.getMarkedForReview() != null) {
            response.setMarkedForReview(request.getMarkedForReview());
        }

        responseRepository.save(response);
    }

    @Transactional
    public Map<String, Object> recordViolation(Long userId, ViolationRequest request) {
        ExamAttempt attempt = attemptRepository.findByIdAndUserId(request.getAttemptId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid attempt session"));

        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            return Map.of("status", attempt.getStatus(), "strikes", attempt.getTabSwitchStrikes(), "disqualified", true);
        }

        int strikes = attempt.getTabSwitchStrikes() + 1;
        attempt.setTabSwitchStrikes(strikes);

        boolean disqualified = false;
        if (strikes >= MAX_STRIKES) {
            disqualified = true;
            attempt.setStatus("DISQUALIFIED_CHEATING");
            finalizeGrading(attempt);
        } else {
            attemptRepository.save(attempt);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("strikes", strikes);
        result.put("maxStrikes", MAX_STRIKES);
        result.put("disqualified", disqualified);
        result.put("status", attempt.getStatus());
        return result;
    }

    @Transactional
    public AttemptResultDto submitExam(Long userId, SubmitExamRequest request) {
        ExamAttempt attempt = attemptRepository.findByIdAndUserId(request.getAttemptId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid attempt session"));

        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            return buildResultDto(attempt);
        }

        if ("TIME_EXPIRED".equalsIgnoreCase(request.getAutoSubmitReason())) {
            attempt.setStatus("AUTO_SUBMITTED_TIME");
        } else if ("CHEATING_VIOLATION".equalsIgnoreCase(request.getAutoSubmitReason())) {
            attempt.setStatus("DISQUALIFIED_CHEATING");
        } else {
            attempt.setStatus("SUBMITTED");
        }

        finalizeGrading(attempt);
        return buildResultDto(attempt);
    }

    private void finalizeGrading(ExamAttempt attempt) {
        attempt.setEndTime(LocalDateTime.now());
        Exam exam = attempt.getExam();
        double totalScore = 0.0;
        int correct = 0;
        int wrong = 0;
        int unattempted = 0;
        double negativePenalty = exam.getNegativeMarksPerWrong() != null ? exam.getNegativeMarksPerWrong() : 0.0;

        List<QuestionResponse> responses = responseRepository.findByExamAttemptId(attempt.getId());
        for (QuestionResponse qr : responses) {
            Question q = qr.getQuestion();
            String chosen = qr.getSelectedOption();
            String correctAns = q.getCorrectOption();

            if (chosen == null || chosen.trim().isEmpty()) {
                unattempted++;
                qr.setIsCorrect(false);
                qr.setMarksAwarded(0.0);
            } else if (chosen.equalsIgnoreCase(correctAns)) {
                correct++;
                qr.setIsCorrect(true);
                double marks = q.getMarks() != null ? q.getMarks() : 1.0;
                qr.setMarksAwarded(marks);
                totalScore += marks;
            } else {
                wrong++;
                qr.setIsCorrect(false);
                double penalty = -1 * negativePenalty;
                qr.setMarksAwarded(penalty);
                totalScore += penalty;
            }
            responseRepository.save(qr);
        }

        // Ensure score does not drop below 0
        totalScore = Math.max(0.0, totalScore);

        attempt.setScore(Math.round(totalScore * 100.0) / 100.0);
        attempt.setCorrectCount(correct);
        attempt.setWrongCount(wrong);
        attempt.setUnattemptedCount(unattempted);
        attempt.setTotalQuestions(responses.size());

        attemptRepository.save(attempt);
    }

    @Transactional(readOnly = true)
    public AttemptResultDto getAttemptResult(Long attemptId, Long userId, boolean isAdmin) {
        ExamAttempt attempt;
        if (isAdmin) {
            attempt = attemptRepository.findById(attemptId)
                    .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));
        } else {
            attempt = attemptRepository.findByIdAndUserId(attemptId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Attempt not found for user: " + attemptId));
        }
        return buildResultDto(attempt);
    }

    @Transactional(readOnly = true)
    public List<AttemptResultDto> getUserAttempts(Long userId) {
        return attemptRepository.findByUserIdOrderByStartTimeDesc(userId).stream()
                .map(this::buildResultDto)
                .collect(Collectors.toList());
    }

    private AttemptResultDto buildResultDto(ExamAttempt attempt) {
        Exam exam = attempt.getExam();
        User user = attempt.getUser();

        AttemptResultDto dto = new AttemptResultDto();
        dto.setAttemptId(attempt.getId());
        dto.setExamId(exam.getId());
        dto.setExamTitle(exam.getTitle());
        dto.setStudentUsername(user.getUsername());
        dto.setStudentFullName(user.getFullName());
        dto.setScore(attempt.getScore());
        dto.setTotalMarks(exam.getTotalMarks());
        dto.setPassingMarks(exam.getPassingMarks());

        double percentage = (exam.getTotalMarks() > 0)
                ? Math.round((attempt.getScore() / exam.getTotalMarks() * 100.0) * 10.0) / 10.0
                : 0.0;
        dto.setPercentage(percentage);
        dto.setPassed(attempt.getScore() >= exam.getPassingMarks());

        dto.setTotalQuestions(attempt.getTotalQuestions());
        dto.setCorrectCount(attempt.getCorrectCount());
        dto.setWrongCount(attempt.getWrongCount());
        dto.setUnattemptedCount(attempt.getUnattemptedCount());
        dto.setStrikes(attempt.getTabSwitchStrikes());
        dto.setStatus(attempt.getStatus());
        dto.setStartTime(attempt.getStartTime());
        dto.setEndTime(attempt.getEndTime());

        if (attempt.getStartTime() != null && attempt.getEndTime() != null) {
            dto.setDurationSeconds(Duration.between(attempt.getStartTime(), attempt.getEndTime()).getSeconds());
        } else {
            dto.setDurationSeconds(0L);
        }

        List<QuestionBreakdownDto> breakdowns = new ArrayList<>();
        List<QuestionResponse> responses = responseRepository.findByExamAttemptId(attempt.getId());
        int qNum = 1;
        for (QuestionResponse qr : responses) {
            Question q = qr.getQuestion();
            QuestionBreakdownDto bDto = new QuestionBreakdownDto();
            bDto.setQuestionId(q.getId());
            bDto.setQuestionNumber(qNum++);
            bDto.setQuestionText(q.getQuestionText());
            bDto.setOptionA(q.getOptionA());
            bDto.setOptionB(q.getOptionB());
            bDto.setOptionC(q.getOptionC());
            bDto.setOptionD(q.getOptionD());
            bDto.setSelectedOption(qr.getSelectedOption());
            bDto.setCorrectOption(q.getCorrectOption());
            bDto.setIsCorrect(qr.getIsCorrect());
            bDto.setMarksAwarded(qr.getMarksAwarded());
            bDto.setMaxMarks(q.getMarks());
            bDto.setExplanation(q.getExplanation());
            breakdowns.add(bDto);
        }
        dto.setQuestionBreakdowns(breakdowns);

        return dto;
    }
}
