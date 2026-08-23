package com.sourav.interviewprep.admin.dto;

import com.sourav.interviewprep.coding.entity.CodingDifficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

public record AdminCodingProblemRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 190)
        @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
        @NotBlank @Size(max = 10000) String description,
        @NotNull CodingDifficulty difficulty,
        @NotNull JsonNode starterCode,
        @NotNull JsonNode testCases,
        @NotNull JsonNode tags,
        @NotNull Boolean active
) {
}
