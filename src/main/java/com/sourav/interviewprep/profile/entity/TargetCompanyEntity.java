package com.sourav.interviewprep.profile.entity;

import com.sourav.interviewprep.auth.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "target_companies", uniqueConstraints =
        @UniqueConstraint(name = "uq_target_companies_user_name", columnNames = {"user_id", "company_name"}))
public class TargetCompanyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "company_name", nullable = false, length = 120)
    private String companyName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TargetCompanyEntity() {
    }

    public TargetCompanyEntity(UserEntity user, String companyName) {
        this.user = user;
        this.companyName = companyName.trim();
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public UserEntity getUser() { return user; }
    public String getCompanyName() { return companyName; }
    public void rename(String name) { companyName = name.trim(); }
}
