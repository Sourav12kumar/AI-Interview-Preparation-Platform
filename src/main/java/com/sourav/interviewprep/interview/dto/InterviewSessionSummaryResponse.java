package com.sourav.interviewprep.interview.dto;

import com.sourav.interviewprep.interview.entity.Difficulty;
import com.sourav.interviewprep.interview.entity.InterviewSessionEntity;
import com.sourav.interviewprep.interview.entity.InterviewStatus;
import com.sourav.interviewprep.interview.entity.InterviewType;

import java.time.Instant;

public record InterviewSessionSummaryResponse(
        Long id,
        InterviewType interviewType,
        String targetRole,
        Difficulty difficulty,
        InterviewStatus status,
        int totalQuestions,
        String aiModel,
        Instant createdAt
) {
    public static InterviewSessionSummaryResponse from(InterviewSessionEntity session) {
        return new InterviewSessionSummaryResponse(
                session.getId(), session.getInterviewType(), session.getTargetRole(),
                session.getDifficulty(), session.getStatus(), session.getTotalQuestions(),
                session.getAiModel(), session.getCreatedAt());
    }
}
