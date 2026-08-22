package com.sourav.interviewprep.coding.runner;

import com.sourav.interviewprep.coding.entity.SubmissionVerdict;

import java.math.BigDecimal;

public record CodeExecutionResult(
        SubmissionVerdict verdict,
        int passedTestCases,
        int totalTestCases,
        Long executionTimeMs,
        Long memoryUsedKb,
        BigDecimal score,
        String message
) {
}
