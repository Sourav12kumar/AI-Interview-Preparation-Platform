package com.sourav.interviewprep.coding.dto;

import com.sourav.interviewprep.coding.entity.ProgrammingLanguage;
import com.sourav.interviewprep.coding.entity.SubmissionVerdict;

import java.math.BigDecimal;
import java.time.Instant;

public record CodingSubmissionSummaryResponse(
        Long id,
        Long problemId,
        String problemTitle,
        ProgrammingLanguage language,
        SubmissionVerdict verdict,
        int passedTestCases,
        int totalTestCases,
        BigDecimal score,
        Instant submittedAt
) {
}
