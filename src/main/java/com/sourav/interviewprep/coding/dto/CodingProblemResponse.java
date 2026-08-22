package com.sourav.interviewprep.coding.dto;

import com.sourav.interviewprep.coding.entity.CodingDifficulty;
import tools.jackson.databind.JsonNode;

import java.util.List;

public record CodingProblemResponse(
        Long id,
        String title,
        String slug,
        String description,
        CodingDifficulty difficulty,
        JsonNode starterCode,
        List<String> tags
) {
}
