package com.sourav.interviewprep.profile.dto;

import com.sourav.interviewprep.profile.entity.Proficiency;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SkillCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 60) String category,
        @NotNull Proficiency proficiency,
        @NotNull @DecimalMin("0.0") @DecimalMax("99.9") BigDecimal yearsUsed
) {
}
