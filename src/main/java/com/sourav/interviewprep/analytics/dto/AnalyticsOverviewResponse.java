package com.sourav.interviewprep.analytics.dto;

import java.math.BigDecimal;

public record AnalyticsOverviewResponse(
        int interviewsCompleted,
        int codingProblemsAttempted,
        int codingSubmissions,
        int acceptedSubmissions,
        BigDecimal averageInterviewScore,
        BigDecimal averageCodingScore,
        BigDecimal codingAcceptanceRate
) {
}
