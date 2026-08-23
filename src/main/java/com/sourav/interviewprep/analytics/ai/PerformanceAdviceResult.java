package com.sourav.interviewprep.analytics.ai;

import java.util.List;

public record PerformanceAdviceResult(
        String summary,
        List<String> recommendations,
        String model,
        String promptVersion
) {
}
