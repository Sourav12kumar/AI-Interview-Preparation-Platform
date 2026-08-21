package com.sourav.interviewprep.profile.entity;

import com.sourav.interviewprep.auth.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "user_profiles")
public class CandidateProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(length = 180)
    private String headline;

    @Column(length = 30)
    private String phone;

    @Column(length = 120)
    private String location;

    @Column(name = "education_level", length = 80)
    private String educationLevel;

    @Column(length = 180)
    private String institution;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(name = "years_of_experience", nullable = false, precision = 4, scale = 1)
    private BigDecimal yearsOfExperience;

    @Column(name = "target_role", length = 120)
    private String targetRole;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CandidateProfileEntity() {
    }

    public CandidateProfileEntity(UserEntity user) {
        this.user = user;
        this.yearsOfExperience = BigDecimal.ZERO;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void update(
            String headline,
            String phone,
            String location,
            String educationLevel,
            String institution,
            Integer graduationYear,
            BigDecimal yearsOfExperience,
            String targetRole,
            String bio) {
        this.headline = trimToNull(headline);
        this.phone = trimToNull(phone);
        this.location = trimToNull(location);
        this.educationLevel = trimToNull(educationLevel);
        this.institution = trimToNull(institution);
        this.graduationYear = graduationYear;
        this.yearsOfExperience = yearsOfExperience == null ? BigDecimal.ZERO : yearsOfExperience;
        this.targetRole = trimToNull(targetRole);
        this.bio = trimToNull(bio);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public Long getId() { return id; }
    public UserEntity getUser() { return user; }
    public String getHeadline() { return headline; }
    public String getPhone() { return phone; }
    public String getLocation() { return location; }
    public String getEducationLevel() { return educationLevel; }
    public String getInstitution() { return institution; }
    public Integer getGraduationYear() { return graduationYear; }
    public BigDecimal getYearsOfExperience() { return yearsOfExperience; }
    public String getTargetRole() { return targetRole; }
    public String getBio() { return bio; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
