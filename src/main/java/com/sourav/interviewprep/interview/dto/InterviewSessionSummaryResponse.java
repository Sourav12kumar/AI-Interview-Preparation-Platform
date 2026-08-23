package com.sourav.interviewprep.interview.dto;

import com.sourav.interviewprep.interview.entity.Difficulty;
import com.sourav.interviewprep.interview.entity.InterviewSessionEntity;
import com.sourav.interviewprep.interview.entity.InterviewStatus;
import com.sourav.interviewprep.interview.entity.InterviewType;

import java.time.Instant;
import java.math.BigDecimal;

public record InterviewSessionSummaryResponse(
        Long id,
        InterviewType interviewType,
        String targetRole,
        Difficulty difficulty,
        InterviewStatus status,
        int totalQuestions,
        String aiModel,
        BigDecimal overallScore,
        Instant createdAt
) {
    public static InterviewSessionSummaryResponse from(InterviewSessionEntity session) {
        return new InterviewSessionSummaryResponse(
                session.getId(), session.getInterviewType(), session.getTargetRole(),
                session.getDifficulty(), session.getStatus(), session.getTotalQuestions(),
                session.getAiModel(), session.getOverallScore(), session.getCreatedAt());
    }
}
