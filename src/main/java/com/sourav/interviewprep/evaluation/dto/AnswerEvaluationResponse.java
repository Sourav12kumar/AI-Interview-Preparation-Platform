package com.sourav.interviewprep.evaluation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AnswerEvaluationResponse(
        Long id,
        BigDecimal technicalScore,
        BigDecimal relevanceScore,
        BigDecimal clarityScore,
        BigDecimal confidenceScore,
        BigDecimal overallScore,
        List<String> strengths,
        List<String> improvements,
        String idealAnswer,
        String aiModel,
        Instant evaluatedAt
) {
}
