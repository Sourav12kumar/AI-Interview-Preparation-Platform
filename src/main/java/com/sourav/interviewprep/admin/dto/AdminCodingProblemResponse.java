package com.sourav.interviewprep.admin.dto;

import com.sourav.interviewprep.coding.entity.CodingDifficulty;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record AdminCodingProblemResponse(
        Long id,
        String title,
        String slug,
        String description,
        CodingDifficulty difficulty,
        JsonNode starterCode,
        JsonNode testCases,
        JsonNode tags,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
