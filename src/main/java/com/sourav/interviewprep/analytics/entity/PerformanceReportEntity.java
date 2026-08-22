package com.sourav.interviewprep.analytics.entity;

import com.sourav.interviewprep.auth.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "performance_reports",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_reports_user_period",
                columnNames = {"user_id", "period_start", "period_end"}))
public class PerformanceReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "interviews_completed", nullable = false)
    private int interviewsCompleted;

    @Column(name = "coding_problems_attempted", nullable = false)
    private int codingProblemsAttempted;

    @Column(name = "average_interview_score", precision = 5, scale = 2)
    private BigDecimal averageInterviewScore;

    @Column(name = "average_coding_score", precision = 5, scale = 2)
    private BigDecimal averageCodingScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strongest_topics", columnDefinition = "JSON")
    private String strongestTopics;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "improvement_topics", columnDefinition = "JSON")
    private String improvementTopics;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_recommendations", columnDefinition = "JSON")
    private String aiRecommendations;

    @Column(name = "analysis_summary", columnDefinition = "TEXT")
    private String analysisSummary;

    @Column(name = "ai_model", length = 80)
    private String aiModel;

    @Column(name = "prompt_version", length = 30)
    private String promptVersion;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    protected PerformanceReportEntity() {
    }

    public PerformanceReportEntity(UserEntity user, LocalDate periodStart, LocalDate periodEnd) {
        this.user = user;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    public void refresh(
            int interviewsCompleted,
            int codingProblemsAttempted,
            BigDecimal averageInterviewScore,
            BigDecimal averageCodingScore,
            String strongestTopics,
            String improvementTopics,
            String aiRecommendations,
            String analysisSummary,
            String aiModel,
            String promptVersion) {
        this.interviewsCompleted = interviewsCompleted;
        this.codingProblemsAttempted = codingProblemsAttempted;
        this.averageInterviewScore = averageInterviewScore;
        this.averageCodingScore = averageCodingScore;
        this.strongestTopics = strongestTopics;
        this.improvementTopics = improvementTopics;
        this.aiRecommendations = aiRecommendations;
        this.analysisSummary = analysisSummary;
        this.aiModel = aiModel;
        this.promptVersion = promptVersion;
        this.generatedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        if (generatedAt == null) generatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        generatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public UserEntity getUser() { return user; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public int getInterviewsCompleted() { return interviewsCompleted; }
    public int getCodingProblemsAttempted() { return codingProblemsAttempted; }
    public BigDecimal getAverageInterviewScore() { return averageInterviewScore; }
    public BigDecimal getAverageCodingScore() { return averageCodingScore; }
    public String getStrongestTopics() { return strongestTopics; }
    public String getImprovementTopics() { return improvementTopics; }
    public String getAiRecommendations() { return aiRecommendations; }
    public String getAnalysisSummary() { return analysisSummary; }
    public String getAiModel() { return aiModel; }
    public String getPromptVersion() { return promptVersion; }
    public Instant getGeneratedAt() { return generatedAt; }
}
