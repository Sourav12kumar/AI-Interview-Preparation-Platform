package com.sourav.interviewprep.analytics.dto;

import com.sourav.interviewprep.analytics.entity.TopicSource;

import java.math.BigDecimal;

public record TopicPerformanceResponse(
        String topic,
        TopicSource source,
        int attempts,
        BigDecimal averageScore
) {
}
