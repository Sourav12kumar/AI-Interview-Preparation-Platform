package com.sourav.interviewprep.analytics.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record GeneratePerformanceReportRequest(
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd
) {
}
