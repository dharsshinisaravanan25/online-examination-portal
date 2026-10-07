package com.examportal.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_attempts")
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    @JsonIgnore
    private Exam exam;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Column(nullable = false, length = 30)
    private String status = "IN_PROGRESS"; // IN_PROGRESS, SUBMITTED, AUTO_SUBMITTED_TIME, DISQUALIFIED_CHEATING

    @Column(nullable = false)
    private Double score = 0.0;

    @Column(nullable = false)
    private Integer totalQuestions = 0;

    @Column(nullable = false)
    private Integer correctCount = 0;

    @Column(nullable = false)
    private Integer wrongCount = 0;

    @Column(nullable = false)
    private Integer unattemptedCount = 0;

    @Column(nullable = false)
    private Integer tabSwitchStrikes = 0;

    @OneToMany(mappedBy = "examAttempt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<QuestionResponse> responses = new ArrayList<>();

    public ExamAttempt() {
    }

    public ExamAttempt(User user, Exam exam) {
        this.user = user;
        this.exam = exam;
        this.startTime = LocalDateTime.now();
        this.status = "IN_PROGRESS";
        this.score = 0.0;
        this.totalQuestions = exam.getQuestions().size();
        this.correctCount = 0;
        this.wrongCount = 0;
        this.unattemptedCount = exam.getQuestions().size();
        this.tabSwitchStrikes = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Exam getExam() {
        return exam;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(Integer correctCount) {
        this.correctCount = correctCount;
    }

    public Integer getWrongCount() {
        return wrongCount;
    }

    public void setWrongCount(Integer wrongCount) {
        this.wrongCount = wrongCount;
    }

    public Integer getUnattemptedCount() {
        return unattemptedCount;
    }

    public void setUnattemptedCount(Integer unattemptedCount) {
        this.unattemptedCount = unattemptedCount;
    }

    public Integer getTabSwitchStrikes() {
        return tabSwitchStrikes;
    }

    public void setTabSwitchStrikes(Integer tabSwitchStrikes) {
        this.tabSwitchStrikes = tabSwitchStrikes;
    }

    public List<QuestionResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<QuestionResponse> responses) {
        this.responses = responses;
    }
}
