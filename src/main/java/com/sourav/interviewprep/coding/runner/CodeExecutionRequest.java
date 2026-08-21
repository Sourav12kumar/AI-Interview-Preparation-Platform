package com.sourav.interviewprep.coding.runner;

import com.sourav.interviewprep.coding.entity.ProgrammingLanguage;
import tools.jackson.databind.JsonNode;

public record CodeExecutionRequest(
        String problemSlug,
        ProgrammingLanguage language,
        String sourceCode,
        JsonNode testCases,
        int timeLimitMs,
        int memoryLimitMb
) {
}
