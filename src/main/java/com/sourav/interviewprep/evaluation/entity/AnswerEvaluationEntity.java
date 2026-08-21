package com.sourav.interviewprep.evaluation.entity;

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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "answer_evaluations")
public class AnswerEvaluationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "answer_id", nullable = false, unique = true)
    private InterviewAnswerEntity answer;

    @Column(name = "technical_score", precision = 5, scale = 2)
    private BigDecimal technicalScore;

    @Column(name = "relevance_score", precision = 5, scale = 2)
    private BigDecimal relevanceScore;

    @Column(name = "clarity_score", precision = 5, scale = 2)
    private BigDecimal clarityScore;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "overall_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(columnDefinition = "JSON")
    private String strengths;

    @Column(columnDefinition = "JSON")
    private String improvements;

    @Column(name = "ideal_answer", columnDefinition = "LONGTEXT")
    private String idealAnswer;

    @Column(name = "ai_model", nullable = false, length = 80)
    private String aiModel;

    @Column(name = "evaluated_at", nullable = false, updatable = false)
    private Instant evaluatedAt;

    protected AnswerEvaluationEntity() {
    }

    public AnswerEvaluationEntity(
            InterviewAnswerEntity answer,
            BigDecimal technicalScore,
            BigDecimal relevanceScore,
            BigDecimal clarityScore,
            BigDecimal confidenceScore,
            BigDecimal overallScore,
            String strengths,
            String improvements,
            String idealAnswer,
            String aiModel) {
        this.answer = answer;
        this.technicalScore = technicalScore;
        this.relevanceScore = relevanceScore;
        this.clarityScore = clarityScore;
        this.confidenceScore = confidenceScore;
        this.overallScore = overallScore;
        this.strengths = strengths;
        this.improvements = improvements;
        this.idealAnswer = idealAnswer;
        this.aiModel = aiModel;
    }

    @PrePersist
    void onCreate() { evaluatedAt = Instant.now(); }

    public Long getId() { return id; }
    public InterviewAnswerEntity getAnswer() { return answer; }
    public BigDecimal getTechnicalScore() { return technicalScore; }
    public BigDecimal getRelevanceScore() { return relevanceScore; }
    public BigDecimal getClarityScore() { return clarityScore; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public BigDecimal getOverallScore() { return overallScore; }
    public String getStrengths() { return strengths; }
    public String getImprovements() { return improvements; }
    public String getIdealAnswer() { return idealAnswer; }
    public String getAiModel() { return aiModel; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
}
