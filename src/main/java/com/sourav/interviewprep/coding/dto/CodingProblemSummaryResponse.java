package com.sourav.interviewprep.coding.dto;

import com.sourav.interviewprep.coding.entity.CodingDifficulty;

import java.util.List;

public record CodingProblemSummaryResponse(
        Long id,
        String title,
        String slug,
        CodingDifficulty difficulty,
        List<String> tags
) {
}
