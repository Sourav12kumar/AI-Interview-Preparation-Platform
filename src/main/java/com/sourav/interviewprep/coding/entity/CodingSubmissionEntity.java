package com.sourav.interviewprep.coding.entity;

import com.sourav.interviewprep.auth.entity.UserEntity;
import com.sourav.interviewprep.coding.runner.CodeExecutionResult;
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
@Table(name = "coding_submissions")
public class CodingSubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private CodingProblemEntity problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProgrammingLanguage language;

    @Column(name = "source_code", nullable = false, columnDefinition = "LONGTEXT")
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubmissionVerdict verdict;

    @Column(name = "passed_test_cases", nullable = false)
    private int passedTestCases;

    @Column(name = "total_test_cases", nullable = false)
    private int totalTestCases;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "memory_used_kb")
    private Long memoryUsedKb;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "result_message", length = 1000)
    private String resultMessage;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    protected CodingSubmissionEntity() {
    }

    public CodingSubmissionEntity(
            UserEntity user,
            CodingProblemEntity problem,
            ProgrammingLanguage language,
            String sourceCode,
            CodeExecutionResult result) {
        this.user = user;
        this.problem = problem;
        this.language = language;
        this.sourceCode = sourceCode;
        this.verdict = result.verdict();
        this.passedTestCases = result.passedTestCases();
        this.totalTestCases = result.totalTestCases();
        this.executionTimeMs = result.executionTimeMs();
        this.memoryUsedKb = result.memoryUsedKb();
        this.score = result.score();
        this.resultMessage = truncate(result.message(), 1000);
    }

    @PrePersist
    void onCreate() {
        submittedAt = Instant.now();
    }

    private String truncate(String value, int limit) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.substring(0, Math.min(trimmed.length(), limit));
    }

    public Long getId() { return id; }
    public UserEntity getUser() { return user; }
    public CodingProblemEntity getProblem() { return problem; }
    public ProgrammingLanguage getLanguage() { return language; }
    public String getSourceCode() { return sourceCode; }
    public SubmissionVerdict getVerdict() { return verdict; }
    public int getPassedTestCases() { return passedTestCases; }
    public int getTotalTestCases() { return totalTestCases; }
    public Long getExecutionTimeMs() { return executionTimeMs; }
    public Long getMemoryUsedKb() { return memoryUsedKb; }
    public BigDecimal getScore() { return score; }
    public String getResultMessage() { return resultMessage; }
    public Instant getSubmittedAt() { return submittedAt; }
}
