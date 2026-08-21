package com.sourav.interviewprep.profile.dto;

import com.sourav.interviewprep.profile.entity.Proficiency;
import com.sourav.interviewprep.profile.entity.UserSkillEntity;

import java.math.BigDecimal;

public record SkillResponse(
        Long id,
        String name,
        String category,
        Proficiency proficiency,
        BigDecimal yearsUsed
) {
    public static SkillResponse from(UserSkillEntity userSkill) {
        return new SkillResponse(
                userSkill.getSkill().getId(),
                userSkill.getSkill().getName(),
                userSkill.getSkill().getCategory(),
                userSkill.getProficiency(),
                userSkill.getYearsUsed()
        );
    }
}
