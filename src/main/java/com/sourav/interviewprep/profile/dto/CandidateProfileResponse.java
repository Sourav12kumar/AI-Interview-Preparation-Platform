package com.sourav.interviewprep.profile.dto;

import com.sourav.interviewprep.profile.entity.CandidateProfileEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CandidateProfileResponse(
        Long id,
        String headline,
        String phone,
        String location,
        String educationLevel,
        String institution,
        Integer graduationYear,
        BigDecimal yearsOfExperience,
        String targetRole,
        List<String> targetCompanies,
        String bio,
        List<SkillResponse> skills,
        Instant createdAt,
        Instant updatedAt
) {
    public static CandidateProfileResponse from(
            CandidateProfileEntity profile,
            List<String> targetCompanies,
            List<SkillResponse> skills) {
        return new CandidateProfileResponse(
                profile.getId(),
                profile.getHeadline(),
                profile.getPhone(),
                profile.getLocation(),
                profile.getEducationLevel(),
                profile.getInstitution(),
                profile.getGraduationYear(),
                profile.getYearsOfExperience(),
                profile.getTargetRole(),
                targetCompanies,
                profile.getBio(),
                skills,
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
