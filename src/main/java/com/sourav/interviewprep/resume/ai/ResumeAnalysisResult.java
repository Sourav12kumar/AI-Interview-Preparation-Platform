package com.sourav.interviewprep.resume.ai;

import java.math.BigDecimal;
import java.util.List;

public record ResumeAnalysisResult(
        BigDecimal atsScore,
        String summary,
        List<String> strengths,
        List<String> weaknesses,
        List<String> missingKeywords,
        List<String> suggestions,
        String model,
        String promptVersion
) {
}
