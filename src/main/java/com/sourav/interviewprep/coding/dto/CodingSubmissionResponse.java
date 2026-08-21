package com.sourav.interviewprep.coding.dto;

import com.sourav.interviewprep.coding.entity.ProgrammingLanguage;
import com.sourav.interviewprep.coding.entity.SubmissionVerdict;

import java.math.BigDecimal;
import java.time.Instant;

public record CodingSubmissionResponse(
        Long id,
        Long problemId,
        String problemTitle,
        ProgrammingLanguage language,
        String sourceCode,
        SubmissionVerdict verdict,
        int passedTestCases,
        int totalTestCases,
        Long executionTimeMs,
        Long memoryUsedKb,
        BigDecimal score,
        String message,
        Instant submittedAt
) {
}
