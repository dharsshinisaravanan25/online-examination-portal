package com.examportal.dto;

import jakarta.validation.constraints.NotNull;

public class ViolationRequest {
    @NotNull(message = "Attempt ID is required")
    private Long attemptId;

    private String violationType; // "TAB_SWITCH", "FULLSCREEN_EXIT", "BLUR"

    public ViolationRequest() {}

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public String getViolationType() {
        return violationType;
    }

    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }
}
