package com.sourav.interviewprep.profile.dto;

import com.sourav.interviewprep.profile.entity.Proficiency;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SkillUpdateRequest(
        @NotNull Proficiency proficiency,
        @NotNull @DecimalMin("0.0") @DecimalMax("99.9") BigDecimal yearsUsed
) {
}
