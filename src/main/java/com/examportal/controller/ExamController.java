package com.examportal.controller;

import com.examportal.dto.ExamDto;
import com.examportal.dto.QuestionDto;
import com.examportal.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public ResponseEntity<List<ExamDto>> getExams(@RequestParam(value = "all", defaultValue = "false") boolean all) {
        return ResponseEntity.ok(examService.getAllExams(all));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamDto> getExamById(@PathVariable Long id) {
        return ResponseEntity.ok(examService.getExamById(id));
    }

    @PostMapping
    public ResponseEntity<?> createExam(@Valid @RequestBody ExamDto dto) {
        try {
            ExamDto created = examService.createExam(dto);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateExam(@PathVariable Long id, @Valid @RequestBody ExamDto dto) {
        try {
            ExamDto updated = examService.updateExam(id, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExam(@PathVariable Long id) {
        try {
            examService.deleteExam(id);
            return ResponseEntity.ok(Map.of("message", "Exam deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/questions")
    public ResponseEntity<List<QuestionDto>> getQuestionsByExamId(@PathVariable Long id) {
        return ResponseEntity.ok(examService.getQuestionsByExamId(id));
    }

    @PostMapping("/{id}/questions")
    public ResponseEntity<?> addQuestion(@PathVariable Long id, @Valid @RequestBody QuestionDto dto) {
        try {
            QuestionDto created = examService.addQuestion(id, dto);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/questions/{questionId}")
    public ResponseEntity<?> updateQuestion(@PathVariable Long questionId, @Valid @RequestBody QuestionDto dto) {
        try {
            QuestionDto updated = examService.updateQuestion(questionId, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long questionId) {
        try {
            examService.deleteQuestion(questionId);
            return ResponseEntity.ok(Map.of("message", "Question deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
