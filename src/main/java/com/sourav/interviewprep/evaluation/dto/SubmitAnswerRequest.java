package com.sourav.interviewprep.evaluation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitAnswerRequest(
        @NotNull Long questionId,
        @NotBlank @Size(max = 10000) String answerText,
        @Min(0) @Max(7200) Integer responseTimeSeconds
) {
}
