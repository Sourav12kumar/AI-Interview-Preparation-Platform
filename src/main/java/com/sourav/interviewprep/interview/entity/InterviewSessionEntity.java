package com.sourav.interviewprep.interview.entity;

import com.sourav.interviewprep.auth.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "interview_sessions")
public class InterviewSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", nullable = false, length = 30)
    private InterviewType interviewType;

    @Column(name = "target_role", nullable = false, length = 120)
    private String targetRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewStatus status;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "ai_model", length = 80)
    private String aiModel;

    @Column(name = "prompt_version", length = 20)
    private String promptVersion;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InterviewSessionEntity() {
    }

    public InterviewSessionEntity(
            UserEntity user,
            InterviewType interviewType,
            String targetRole,
            Difficulty difficulty,
            int totalQuestions,
            String aiModel,
            String promptVersion) {
        this.user = user;
        this.interviewType = interviewType;
        this.targetRole = targetRole;
        this.difficulty = difficulty;
        this.status = InterviewStatus.CREATED;
        this.totalQuestions = totalQuestions;
        this.aiModel = aiModel;
        this.promptVersion = promptVersion;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public UserEntity getUser() { return user; }
    public InterviewType getInterviewType() { return interviewType; }
    public String getTargetRole() { return targetRole; }
    public Difficulty getDifficulty() { return difficulty; }
    public InterviewStatus getStatus() { return status; }
    public int getTotalQuestions() { return totalQuestions; }
    public BigDecimal getOverallScore() { return overallScore; }
    public String getAiModel() { return aiModel; }
    public String getPromptVersion() { return promptVersion; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
