package com.sourav.interviewprep.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PerformanceReportSummaryResponse(
        Long id,
        LocalDate periodStart,
        LocalDate periodEnd,
        int interviewsCompleted,
        int codingProblemsAttempted,
        BigDecimal averageInterviewScore,
        BigDecimal averageCodingScore,
        Instant generatedAt
) {
}
