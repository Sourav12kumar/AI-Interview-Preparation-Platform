package com.sourav.interviewprep.evaluation.dto;

import java.time.Instant;

public record SubmittedAnswerResponse(
        Long id,
        Long questionId,
        String answerText,
        Integer responseTimeSeconds,
        Instant submittedAt,
        AnswerEvaluationResponse evaluation
) {
}
