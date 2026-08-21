package com.sourav.interviewprep.profile.entity;

import com.sourav.interviewprep.auth.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "user_skills")
public class UserSkillEntity {

    @EmbeddedId
    private UserSkillId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @MapsId("skillId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private SkillEntity skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Proficiency proficiency;

    @Column(name = "years_used", nullable = false, precision = 4, scale = 1)
    private BigDecimal yearsUsed;

    protected UserSkillEntity() {
    }

    public UserSkillEntity(UserEntity user, SkillEntity skill, Proficiency proficiency, BigDecimal yearsUsed) {
        this.id = new UserSkillId(user.getId(), skill.getId());
        this.user = user;
        this.skill = skill;
        update(proficiency, yearsUsed);
    }

    public void update(Proficiency proficiency, BigDecimal yearsUsed) {
        this.proficiency = proficiency;
        this.yearsUsed = yearsUsed == null ? BigDecimal.ZERO : yearsUsed;
    }

    public UserEntity getUser() { return user; }
    public SkillEntity getSkill() { return skill; }
    public Proficiency getProficiency() { return proficiency; }
    public BigDecimal getYearsUsed() { return yearsUsed; }
}
