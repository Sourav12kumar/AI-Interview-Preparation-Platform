package com.sourav.interviewprep.analytics.exception;

public class PerformanceReportNotFoundException extends RuntimeException {
    public PerformanceReportNotFoundException() {
        super("Performance report not found");
    }
}
