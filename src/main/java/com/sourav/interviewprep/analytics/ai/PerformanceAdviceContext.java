package com.sourav.interviewprep.analytics.ai;

import com.sourav.interviewprep.analytics.dto.TopicPerformanceResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PerformanceAdviceContext(
        LocalDate periodStart,
        LocalDate periodEnd,
        int interviewsCompleted,
        int codingProblemsAttempted,
        int codingSubmissions,
        BigDecimal averageInterviewScore,
        BigDecimal averageCodingScore,
        List<TopicPerformanceResponse> strongestTopics,
        List<TopicPerformanceResponse> improvementTopics
) {
}
