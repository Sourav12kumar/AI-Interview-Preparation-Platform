package com.sourav.interviewprep.resume.entity;

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
@Table(name = "resumes")
public class ResumeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "extracted_text", columnDefinition = "LONGTEXT")
    private String extractedText;

    @Column(name = "target_role", length = 120)
    private String targetRole;

    @Column(name = "ats_score", precision = 5, scale = 2)
    private BigDecimal atsScore;

    @Column(columnDefinition = "JSON")
    private String strengths;

    @Column(columnDefinition = "JSON")
    private String weaknesses;

    @Column(name = "missing_keywords", columnDefinition = "JSON")
    private String missingKeywords;

    @Column(name = "analysis_summary", columnDefinition = "TEXT")
    private String analysisSummary;

    @Column(columnDefinition = "JSON")
    private String suggestions;

    @Column(name = "ai_model", length = 80)
    private String aiModel;

    @Column(name = "prompt_version", length = 30)
    private String promptVersion;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false, length = 20)
    private ResumeAnalysisStatus analysisStatus;

    @Column(name = "analyzed_at")
    private Instant analyzedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ResumeEntity() {
    }

    public ResumeEntity(
            UserEntity user,
            String originalFilename,
            String storageKey,
            String contentType,
            long fileSizeBytes,
            String extractedText) {
        this.user = user;
        this.originalFilename = originalFilename;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.extractedText = extractedText;
        this.analysisStatus = ResumeAnalysisStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void markProcessing(String targetRole) {
        this.targetRole = targetRole;
        this.analysisStatus = ResumeAnalysisStatus.PROCESSING;
        this.atsScore = null;
        this.strengths = null;
        this.weaknesses = null;
        this.missingKeywords = null;
        this.analysisSummary = null;
        this.suggestions = null;
        this.aiModel = null;
        this.promptVersion = null;
        this.analyzedAt = null;
        this.failureReason = null;
    }

    public void completeAnalysis(
            BigDecimal atsScore,
            String strengths,
            String weaknesses,
            String missingKeywords,
            String analysisSummary,
            String suggestions,
            String aiModel,
            String promptVersion) {
        this.atsScore = atsScore;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.missingKeywords = missingKeywords;
        this.analysisSummary = analysisSummary;
        this.suggestions = suggestions;
        this.aiModel = aiModel;
        this.promptVersion = promptVersion;
        this.analysisStatus = ResumeAnalysisStatus.COMPLETED;
        this.analyzedAt = Instant.now();
        this.failureReason = null;
    }

    public void failAnalysis(String reason) {
        this.analysisStatus = ResumeAnalysisStatus.FAILED;
        this.failureReason = reason == null ? "Resume analysis failed" : reason.substring(0, Math.min(reason.length(), 500));
    }

    public Long getId() { return id; }
    public UserEntity getUser() { return user; }
    public String getOriginalFilename() { return originalFilename; }
    public String getStorageKey() { return storageKey; }
    public String getContentType() { return contentType; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public String getExtractedText() { return extractedText; }
    public String getTargetRole() { return targetRole; }
    public BigDecimal getAtsScore() { return atsScore; }
    public String getStrengths() { return strengths; }
    public String getWeaknesses() { return weaknesses; }
    public String getMissingKeywords() { return missingKeywords; }
    public String getAnalysisSummary() { return analysisSummary; }
    public String getSuggestions() { return suggestions; }
    public String getAiModel() { return aiModel; }
    public String getPromptVersion() { return promptVersion; }
    public String getFailureReason() { return failureReason; }
    public ResumeAnalysisStatus getAnalysisStatus() { return analysisStatus; }
    public Instant getAnalyzedAt() { return analyzedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
