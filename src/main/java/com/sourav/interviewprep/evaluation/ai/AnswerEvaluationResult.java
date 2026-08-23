package com.sourav.interviewprep.evaluation.ai;

import java.math.BigDecimal;
import java.util.List;

public record AnswerEvaluationResult(
        BigDecimal technicalScore,
        BigDecimal relevanceScore,
        BigDecimal clarityScore,
        BigDecimal confidenceScore,
        BigDecimal overallScore,
        List<String> strengths,
        List<String> improvements,
        String idealAnswer,
        String model
) {
}
