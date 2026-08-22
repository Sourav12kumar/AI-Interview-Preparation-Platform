package com.sourav.interviewprep.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PerformanceReportResponse(
        Long id,
        LocalDate periodStart,
        LocalDate periodEnd,
        int interviewsCompleted,
        int codingProblemsAttempted,
        BigDecimal averageInterviewScore,
        BigDecimal averageCodingScore,
        List<TopicPerformanceResponse> strongestTopics,
        List<TopicPerformanceResponse> improvementTopics,
        String summary,
        List<String> recommendations,
        String aiModel,
        String promptVersion,
        Instant generatedAt
) {
}
