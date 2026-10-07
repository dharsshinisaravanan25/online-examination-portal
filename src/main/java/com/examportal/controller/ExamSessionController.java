package com.examportal.controller;

import com.examportal.dto.*;
import com.examportal.service.ExamSessionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/session")
public class ExamSessionController {

    private final ExamSessionService examSessionService;

    public ExamSessionController(ExamSessionService examSessionService) {
        this.examSessionService = examSessionService;
    }

    private Long resolveUserId(HttpSession session, Long headerUserId) {
        Long userId = (Long) session.getAttribute("userId");
        return (userId != null) ? userId : headerUserId;
    }

    @PostMapping("/start/{examId}")
    public ResponseEntity<?> startExam(@PathVariable Long examId,
                                       HttpSession session,
                                       @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = resolveUserId(session, headerUserId);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "User must be authenticated to start an exam"));
        }

        try {
            StartExamResponse response = examSessionService.startExam(examId, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/save-answer")
    public ResponseEntity<?> saveAnswer(@Valid @RequestBody SaveAnswerRequest request,
                                        HttpSession session,
                                        @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = resolveUserId(session, headerUserId);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "User not authenticated"));
        }

        try {
            examSessionService.saveAnswer(userId, request);
            return ResponseEntity.ok(Map.of("status", "SAVED"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/violation")
    public ResponseEntity<?> recordViolation(@Valid @RequestBody ViolationRequest request,
                                             HttpSession session,
                                             @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = resolveUserId(session, headerUserId);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "User not authenticated"));
        }

        try {
            Map<String, Object> result = examSessionService.recordViolation(userId, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitExam(@Valid @RequestBody SubmitExamRequest request,
                                        HttpSession session,
                                        @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = resolveUserId(session, headerUserId);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "User not authenticated"));
        }

        try {
            AttemptResultDto result = examSessionService.submitExam(userId, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/result/{attemptId}")
    public ResponseEntity<?> getAttemptResult(@PathVariable Long attemptId,
                                              @RequestParam(value = "admin", defaultValue = "false") boolean admin,
                                              HttpSession session,
                                              @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = resolveUserId(session, headerUserId);
        if (userId == null && !admin) {
            return ResponseEntity.status(401).body(Map.of("message", "User not authenticated"));
        }

        try {
            AttemptResultDto result = examSessionService.getAttemptResult(attemptId, userId, admin);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/my-attempts")
    public ResponseEntity<?> getMyAttempts(HttpSession session,
                                           @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = resolveUserId(session, headerUserId);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "User not authenticated"));
        }

        List<AttemptResultDto> attempts = examSessionService.getUserAttempts(userId);
        return ResponseEntity.ok(attempts);
    }
}
