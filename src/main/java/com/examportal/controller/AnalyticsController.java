package com.examportal.controller;

import com.examportal.dto.AdminDashboardStatsDto;
import com.examportal.dto.AttemptResultDto;
import com.examportal.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/analytics")
    public ResponseEntity<AdminDashboardStatsDto> getAnalytics() {
        return ResponseEntity.ok(analyticsService.getAdminStats());
    }

    @GetMapping("/submissions")
    public ResponseEntity<List<AttemptResultDto>> getSubmissions() {
        return ResponseEntity.ok(analyticsService.getAllAttempts());
    }
}
