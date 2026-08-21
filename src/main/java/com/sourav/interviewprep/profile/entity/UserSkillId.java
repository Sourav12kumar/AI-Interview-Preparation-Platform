package com.sourav.interviewprep.profile.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserSkillId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "skill_id")
    private Long skillId;

    protected UserSkillId() {
    }

    public UserSkillId(Long userId, Long skillId) {
        this.userId = userId;
        this.skillId = skillId;
    }

    public Long getUserId() { return userId; }
    public Long getSkillId() { return skillId; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof UserSkillId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(skillId, that.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, skillId);
    }
}
