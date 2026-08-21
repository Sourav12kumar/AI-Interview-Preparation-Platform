package com.sourav.interviewprep.profile.repository;

import com.sourav.interviewprep.profile.entity.SkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkillRepository extends JpaRepository<SkillEntity, Long> {
    Optional<SkillEntity> findByNameIgnoreCase(String name);
}
