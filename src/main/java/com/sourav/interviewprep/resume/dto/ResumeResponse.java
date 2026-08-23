package com.sourav.interviewprep.resume.dto;

import com.sourav.interviewprep.resume.entity.ResumeAnalysisStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ResumeResponse(
        Long id,
        String originalFilename,
        String contentType,
        long fileSizeBytes,
        ResumeAnalysisStatus analysisStatus,
        String targetRole,
        BigDecimal atsScore,
        String summary,
        List<String> strengths,
        List<String> weaknesses,
        List<String> missingKeywords,
        List<String> suggestions,
        String aiModel,
        String promptVersion,
        String failureReason,
        Instant analyzedAt,
        Instant createdAt
) {
}
