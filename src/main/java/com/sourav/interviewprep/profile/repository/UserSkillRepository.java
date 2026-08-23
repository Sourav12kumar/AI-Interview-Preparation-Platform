package com.sourav.interviewprep.profile.repository;

import com.sourav.interviewprep.profile.entity.UserSkillEntity;
import com.sourav.interviewprep.profile.entity.UserSkillId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSkillRepository extends JpaRepository<UserSkillEntity, UserSkillId> {
    List<UserSkillEntity> findAllByUser_IdOrderBySkill_NameAsc(Long userId);
    Optional<UserSkillEntity> findByUser_IdAndSkill_Id(Long userId, Long skillId);
    void deleteByUser_Id(Long userId);
}
