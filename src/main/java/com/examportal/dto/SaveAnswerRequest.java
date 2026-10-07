package com.examportal.dto;

import jakarta.validation.constraints.NotNull;

public class SaveAnswerRequest {
    @NotNull(message = "Attempt ID is required")
    private Long attemptId;

    @NotNull(message = "Question ID is required")
    private Long questionId;

    private String selectedOption; // "A", "B", "C", "D" or null
    private Boolean markedForReview;

    public SaveAnswerRequest() {}

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(String selectedOption) {
        this.selectedOption = selectedOption;
    }

    public Boolean getMarkedForReview() {
        return markedForReview;
    }

    public void setMarkedForReview(Boolean markedForReview) {
        this.markedForReview = markedForReview;
    }
}
