package com.sourav.interviewprep.coding.dto;

import com.sourav.interviewprep.coding.entity.ProgrammingLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitCodeRequest(
        @NotNull ProgrammingLanguage language,
        @NotBlank @Size(max = 100000) String sourceCode
) {
}
