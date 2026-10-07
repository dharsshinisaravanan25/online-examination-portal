package com.examportal.controller;

import com.examportal.dto.AuthRequest;
import com.examportal.dto.AuthResponse;
import com.examportal.dto.RegisterRequest;
import com.examportal.model.User;
import com.examportal.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request, HttpSession session) {
        try {
            AuthResponse response = authService.login(request);
            session.setAttribute("userId", response.getId());
            session.setAttribute("role", response.getRole().name());
            session.setAttribute("username", response.getUsername());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpSession session) {
        try {
            AuthResponse response = authService.register(request);
            session.setAttribute("userId", response.getId());
            session.setAttribute("role", response.getRole().name());
            session.setAttribute("username", response.getUsername());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session,
                                            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            userId = headerUserId;
        }

        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        try {
            User user = authService.getUserById(userId);
            return ResponseEntity.ok(new AuthResponse(
                    "SESSION",
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("message", "User session expired"));
        }
    }
}
