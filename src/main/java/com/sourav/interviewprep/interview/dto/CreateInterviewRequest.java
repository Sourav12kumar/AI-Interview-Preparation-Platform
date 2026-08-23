package com.sourav.interviewprep.interview.dto;

import com.sourav.interviewprep.interview.entity.Difficulty;
import com.sourav.interviewprep.interview.entity.InterviewType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateInterviewRequest(
        @NotNull InterviewType interviewType,
        @NotNull Difficulty difficulty,
        @Min(1) @Max(10) int questionCount,
        @Size(max = 120) String targetRole
) {
}
