package com.examportal.repository;

import com.examportal.model.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
    List<ExamAttempt> findByUserIdOrderByStartTimeDesc(Long userId);
    List<ExamAttempt> findByExamIdOrderByScoreDesc(Long examId);
    List<ExamAttempt> findAllByOrderByStartTimeDesc();
    Optional<ExamAttempt> findByIdAndUserId(Long id, Long userId);
}
