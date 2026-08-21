package com.sourav.interviewprep.interview.ai;

import com.sourav.interviewprep.interview.entity.Difficulty;
import com.sourav.interviewprep.interview.entity.InterviewType;

import java.math.BigDecimal;
import java.util.List;

public record InterviewGenerationContext(
        InterviewType interviewType,
        Difficulty difficulty,
        int questionCount,
        String targetRole,
        String headline,
        String educationLevel,
        String institution,
        Integer graduationYear,
        BigDecimal yearsOfExperience,
        String bio,
        List<String> targetCompanies,
        List<String> skills
) {
}
