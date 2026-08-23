package com.sourav.interviewprep.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AnalyticsTrendPointResponse(
        LocalDate date,
        BigDecimal averageInterviewScore,
        BigDecimal averageCodingScore,
        int interviewsCompleted,
        int codingSubmissions
) {
}
