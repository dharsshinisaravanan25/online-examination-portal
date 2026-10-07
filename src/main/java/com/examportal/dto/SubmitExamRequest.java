package com.examportal.dto;

import jakarta.validation.constraints.NotNull;

public class SubmitExamRequest {
    @NotNull(message = "Attempt ID is required")
    private Long attemptId;

    private String autoSubmitReason; // "USER_CLICK", "TIME_EXPIRED", "CHEATING_VIOLATION"

    public SubmitExamRequest() {}

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public String getAutoSubmitReason() {
        return autoSubmitReason;
    }

    public void setAutoSubmitReason(String autoSubmitReason) {
        this.autoSubmitReason = autoSubmitReason;
    }
}
