package com.sourav.interviewprep.interview.ai;

import java.util.List;

public record QuestionGenerationResult(
        String model,
        String promptVersion,
        List<GeneratedQuestion> questions
) {
}
