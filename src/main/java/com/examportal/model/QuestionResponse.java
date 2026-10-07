package com.examportal.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "question_responses")
public class QuestionResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    @JsonIgnore
    private ExamAttempt examAttempt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(length = 10)
    private String selectedOption; // "A", "B", "C", "D", or null

    @Column(nullable = false)
    private Boolean markedForReview = false;

    private Boolean isCorrect;

    @Column(nullable = false)
    private Double marksAwarded = 0.0;

    public QuestionResponse() {
    }

    public QuestionResponse(ExamAttempt examAttempt, Question question) {
        this.examAttempt = examAttempt;
        this.question = question;
        this.selectedOption = null;
        this.markedForReview = false;
        this.isCorrect = null;
        this.marksAwarded = 0.0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExamAttempt getExamAttempt() {
        return examAttempt;
    }

    public void setExamAttempt(ExamAttempt examAttempt) {
        this.examAttempt = examAttempt;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
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

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public Double getMarksAwarded() {
        return marksAwarded;
    }

    public void setMarksAwarded(Double marksAwarded) {
        this.marksAwarded = marksAwarded;
    }
}
