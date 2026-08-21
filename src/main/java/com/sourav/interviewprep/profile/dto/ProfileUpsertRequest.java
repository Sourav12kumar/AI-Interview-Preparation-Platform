package com.sourav.interviewprep.profile.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ProfileUpsertRequest(
        @Size(max = 180) String headline,
        @Size(max = 30) String phone,
        @Size(max = 120) String location,
        @Size(max = 80) String educationLevel,
        @Size(max = 180) String institution,
        @Min(1900) @Max(2200) Integer graduationYear,
        @DecimalMin("0.0") @DecimalMax("99.9") BigDecimal yearsOfExperience,
        @Size(max = 120) String targetRole,
        @Size(max = 5000) String bio,
        @Size(max = 20) List<@Valid @NotBlank @Size(max = 120) String> targetCompanies
) {
}
