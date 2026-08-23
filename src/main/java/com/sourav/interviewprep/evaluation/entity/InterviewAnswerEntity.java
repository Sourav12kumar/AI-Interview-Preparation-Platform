package com.sourav.interviewprep.evaluation.entity;

import com.sourav.interviewprep.interview.entity.InterviewQuestionEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "interview_answers")
public class InterviewAnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private InterviewQuestionEntity question;

    @Column(name = "answer_text", nullable = false, columnDefinition = "LONGTEXT")
    private String answerText;

    @Column(name = "response_time_seconds")
    private Integer responseTimeSeconds;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    protected InterviewAnswerEntity() {
    }

    public InterviewAnswerEntity(
            InterviewQuestionEntity question,
            String answerText,
            Integer responseTimeSeconds) {
        this.question = question;
        this.answerText = answerText.trim();
        this.responseTimeSeconds = responseTimeSeconds;
    }

    @PrePersist
    void onCreate() {
        submittedAt = Instant.now();
    }

    public Long getId() { return id; }
    public InterviewQuestionEntity getQuestion() { return question; }
    public String getAnswerText() { return answerText; }
    public Integer getResponseTimeSeconds() { return responseTimeSeconds; }
    public Instant getSubmittedAt() { return submittedAt; }
}
