package com.sourav.interviewprep.resume.dto;

import jakarta.validation.constraints.Size;

public record AnalyzeResumeRequest(
        @Size(max = 120) String targetRole,
        @Size(max = 20000) String jobDescription
) {
}
