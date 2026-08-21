package com.sourav.interviewprep.interview.dto;

import com.sourav.interviewprep.interview.entity.Difficulty;
import com.sourav.interviewprep.interview.entity.InterviewSessionEntity;
import com.sourav.interviewprep.interview.entity.InterviewStatus;
import com.sourav.interviewprep.interview.entity.InterviewType;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

public record InterviewSessionResponse(
        Long id,
        InterviewType interviewType,
        String targetRole,
        Difficulty difficulty,
        InterviewStatus status,
        int totalQuestions,
        String aiModel,
        String promptVersion,
        BigDecimal overallScore,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt,
        List<InterviewQuestionResponse> questions
) {
    public static InterviewSessionResponse from(
            InterviewSessionEntity session,
            List<InterviewQuestionResponse> questions) {
        return new InterviewSessionResponse(
                session.getId(), session.getInterviewType(), session.getTargetRole(),
                session.getDifficulty(), session.getStatus(), session.getTotalQuestions(),
                session.getAiModel(), session.getPromptVersion(), session.getOverallScore(),
                session.getStartedAt(), session.getCompletedAt(), session.getCreatedAt(), questions);
    }
}
