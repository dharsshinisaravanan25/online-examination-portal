package com.examportal.repository;

import com.examportal.model.QuestionResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionResponseRepository extends JpaRepository<QuestionResponse, Long> {
    List<QuestionResponse> findByExamAttemptId(Long attemptId);
    Optional<QuestionResponse> findByExamAttemptIdAndQuestionId(Long attemptId, Long questionId);
}
