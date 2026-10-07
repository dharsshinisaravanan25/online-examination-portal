package com.examportal.repository;

import com.examportal.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByPublishedTrue();
    List<Exam> findByCategoryIgnoreCase(String category);
}
