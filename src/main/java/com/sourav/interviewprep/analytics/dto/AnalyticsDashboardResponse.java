package com.sourav.interviewprep.analytics.dto;

import java.util.List;

public record AnalyticsDashboardResponse(
        AnalyticsPeriodResponse period,
        AnalyticsOverviewResponse overview,
        List<AnalyticsTrendPointResponse> trends,
        List<TopicPerformanceResponse> strongestTopics,
        List<TopicPerformanceResponse> improvementTopics
) {
}
