package com.examportal.dto;

import java.time.LocalDateTime;
import java.util.List;

public class StartExamResponse {
    private Long attemptId;
    private Long examId;
    private String examTitle;
    private String category;
    private Integer durationMinutes;
    private Integer totalMarks;
    private Double negativeMarksPerWrong;
    private LocalDateTime startTime;
    private Long remainingSeconds;
    private Integer strikes;
    private List<ExamQuestionDto> questions;

    public StartExamResponse() {}

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public String getExamTitle() {
        return examTitle;
    }

    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(Integer totalMarks) {
        this.totalMarks = totalMarks;
    }

    public Double getNegativeMarksPerWrong() {
        return negativeMarksPerWrong;
    }

    public void setNegativeMarksPerWrong(Double negativeMarksPerWrong) {
        this.negativeMarksPerWrong = negativeMarksPerWrong;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public Integer getStrikes() {
        return strikes;
    }

    public void setStrikes(Integer strikes) {
        this.strikes = strikes;
    }

    public List<ExamQuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<ExamQuestionDto> questions) {
        this.questions = questions;
    }
}
