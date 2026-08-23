package com.sourav.interviewprep.interview.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "interview_questions")
public class InterviewQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSessionEntity session;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private QuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "expected_topics", columnDefinition = "JSON")
    private String expectedTopicsJson;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "ai_generated", nullable = false)
    private boolean aiGenerated;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InterviewQuestionEntity() {
    }

    public InterviewQuestionEntity(
            InterviewSessionEntity session,
            String questionText,
            QuestionType questionType,
            Difficulty difficulty,
            String expectedTopicsJson,
            int sequenceNumber) {
        this.session = session;
        this.questionText = questionText;
        this.questionType = questionType;
        this.difficulty = difficulty;
        this.expectedTopicsJson = expectedTopicsJson;
        this.sequenceNumber = sequenceNumber;
        this.aiGenerated = true;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public InterviewSessionEntity getSession() { return session; }
    public String getQuestionText() { return questionText; }
    public QuestionType getQuestionType() { return questionType; }
    public Difficulty getDifficulty() { return difficulty; }
    public String getExpectedTopicsJson() { return expectedTopicsJson; }
    public int getSequenceNumber() { return sequenceNumber; }
    public boolean isAiGenerated() { return aiGenerated; }
}
