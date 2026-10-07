package com.examportal.service;

import com.examportal.dto.ExamDto;
import com.examportal.dto.QuestionDto;
import com.examportal.model.Exam;
import com.examportal.model.Question;
import com.examportal.repository.ExamRepository;
import com.examportal.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;

    public ExamService(ExamRepository examRepository, QuestionRepository questionRepository) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public List<ExamDto> getAllExams(boolean forAdmin) {
        List<Exam> exams = forAdmin ? examRepository.findAll() : examRepository.findByPublishedTrue();
        return exams.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExamDto getExamById(Long id) {
        Exam exam = findExamOrThrow(id);
        return mapToDto(exam);
    }

    @Transactional
    public ExamDto createExam(ExamDto dto) {
        Exam exam = new Exam(
                dto.getTitle().trim(),
                dto.getDescription(),
                dto.getCategory() != null ? dto.getCategory().trim() : "General",
                dto.getDurationMinutes(),
                dto.getTotalMarks(),
                dto.getPassingMarks(),
                dto.getNegativeMarksPerWrong(),
                dto.getShuffleQuestions(),
                dto.getPublished()
        );

        exam = examRepository.save(exam);
        return mapToDto(exam);
    }

    @Transactional
    public ExamDto updateExam(Long id, ExamDto dto) {
        Exam exam = findExamOrThrow(id);
        exam.setTitle(dto.getTitle().trim());
        exam.setDescription(dto.getDescription());
        exam.setCategory(dto.getCategory() != null ? dto.getCategory().trim() : "General");
        exam.setDurationMinutes(dto.getDurationMinutes());
        exam.setTotalMarks(dto.getTotalMarks());
        exam.setPassingMarks(dto.getPassingMarks());
        exam.setNegativeMarksPerWrong(dto.getNegativeMarksPerWrong() != null ? dto.getNegativeMarksPerWrong() : 0.0);
        exam.setShuffleQuestions(dto.getShuffleQuestions() != null ? dto.getShuffleQuestions() : false);
        exam.setPublished(dto.getPublished() != null ? dto.getPublished() : true);

        exam = examRepository.save(exam);
        return mapToDto(exam);
    }

    @Transactional
    public void deleteExam(Long id) {
        if (!examRepository.existsById(id)) {
            throw new IllegalArgumentException("Exam not found with id: " + id);
        }
        examRepository.deleteById(id);
    }

    public List<QuestionDto> getQuestionsByExamId(Long examId) {
        findExamOrThrow(examId);
        return questionRepository.findByExamId(examId).stream()
                .map(this::mapQuestionToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public QuestionDto addQuestion(Long examId, QuestionDto dto) {
        Exam exam = findExamOrThrow(examId);

        Question question = new Question(
                exam,
                dto.getQuestionText().trim(),
                dto.getOptionA().trim(),
                dto.getOptionB().trim(),
                dto.getOptionC().trim(),
                dto.getOptionD().trim(),
                dto.getCorrectOption().trim().toUpperCase(),
                dto.getExplanation(),
                dto.getMarks() != null ? dto.getMarks() : 1,
                dto.getDifficulty() != null ? dto.getDifficulty().toUpperCase() : "MEDIUM"
        );

        exam.addQuestion(question);
        question = questionRepository.save(question);
        return mapQuestionToDto(question);
    }

    @Transactional
    public QuestionDto updateQuestion(Long questionId, QuestionDto dto) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with id: " + questionId));

        question.setQuestionText(dto.getQuestionText().trim());
        question.setOptionA(dto.getOptionA().trim());
        question.setOptionB(dto.getOptionB().trim());
        question.setOptionC(dto.getOptionC().trim());
        question.setOptionD(dto.getOptionD().trim());
        question.setCorrectOption(dto.getCorrectOption().trim().toUpperCase());
        question.setExplanation(dto.getExplanation());
        if (dto.getMarks() != null) {
            question.setMarks(dto.getMarks());
        }
        if (dto.getDifficulty() != null) {
            question.setDifficulty(dto.getDifficulty().toUpperCase());
        }

        question = questionRepository.save(question);
        return mapQuestionToDto(question);
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with id: " + questionId));
        Exam exam = question.getExam();
        if (exam != null) {
            exam.removeQuestion(question);
        }
        questionRepository.delete(question);
    }

    public Exam findExamOrThrow(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with id: " + id));
    }

    private ExamDto mapToDto(Exam exam) {
        ExamDto dto = new ExamDto();
        dto.setId(exam.getId());
        dto.setTitle(exam.getTitle());
        dto.setDescription(exam.getDescription());
        dto.setCategory(exam.getCategory());
        dto.setDurationMinutes(exam.getDurationMinutes());
        dto.setTotalMarks(exam.getTotalMarks());
        dto.setPassingMarks(exam.getPassingMarks());
        dto.setNegativeMarksPerWrong(exam.getNegativeMarksPerWrong());
        dto.setShuffleQuestions(exam.getShuffleQuestions());
        dto.setPublished(exam.getPublished());
        dto.setQuestionCount(exam.getQuestions() != null ? exam.getQuestions().size() : 0);
        dto.setCreatedAt(exam.getCreatedAt());
        return dto;
    }

    private QuestionDto mapQuestionToDto(Question q) {
        QuestionDto dto = new QuestionDto();
        dto.setId(q.getId());
        dto.setExamId(q.getExam() != null ? q.getExam().getId() : null);
        dto.setQuestionText(q.getQuestionText());
        dto.setOptionA(q.getOptionA());
        dto.setOptionB(q.getOptionB());
        dto.setOptionC(q.getOptionC());
        dto.setOptionD(q.getOptionD());
        dto.setCorrectOption(q.getCorrectOption());
        dto.setExplanation(q.getExplanation());
        dto.setMarks(q.getMarks());
        dto.setDifficulty(q.getDifficulty());
        return dto;
    }
}
