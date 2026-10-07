package com.examportal.service;

import com.examportal.dto.AdminDashboardStatsDto;
import com.examportal.dto.AttemptResultDto;
import com.examportal.model.ExamAttempt;
import com.examportal.model.Role;
import com.examportal.repository.ExamAttemptRepository;
import com.examportal.repository.ExamRepository;
import com.examportal.repository.QuestionRepository;
import com.examportal.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ExamAttemptRepository attemptRepository;
    private final ExamSessionService examSessionService;

    public AnalyticsService(ExamRepository examRepository,
                            QuestionRepository questionRepository,
                            UserRepository userRepository,
                            ExamAttemptRepository attemptRepository,
                            ExamSessionService examSessionService) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
        this.examSessionService = examSessionService;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public AdminDashboardStatsDto getAdminStats() {
        AdminDashboardStatsDto stats = new AdminDashboardStatsDto();
        stats.setTotalExams(examRepository.count());
        stats.setTotalQuestions(questionRepository.count());
        stats.setTotalStudents(userRepository.findAll().stream().filter(u -> u.getRole() == Role.STUDENT).count());

        List<ExamAttempt> attempts = attemptRepository.findAll();
        stats.setTotalAttempts((long) attempts.size());

        List<ExamAttempt> finished = attempts.stream()
                .filter(a -> !"IN_PROGRESS".equals(a.getStatus()))
                .collect(Collectors.toList());

        if (finished.isEmpty()) {
            stats.setAverageScore(0.0);
            stats.setPassPercentage(0.0);
        } else {
            double avgScore = finished.stream().mapToDouble(ExamAttempt::getScore).average().orElse(0.0);
            stats.setAverageScore(Math.round(avgScore * 10.0) / 10.0);

            long passed = finished.stream()
                    .filter(a -> a.getScore() >= a.getExam().getPassingMarks())
                    .count();
            double passPct = (double) passed / finished.size() * 100.0;
            stats.setPassPercentage(Math.round(passPct * 10.0) / 10.0);
        }

        return stats;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<AttemptResultDto> getAllAttempts() {
        return attemptRepository.findAllByOrderByStartTimeDesc().stream()
                .map(a -> examSessionService.getAttemptResult(a.getId(), null, true))
                .collect(Collectors.toList());
    }
}
