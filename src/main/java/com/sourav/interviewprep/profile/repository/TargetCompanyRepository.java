package com.sourav.interviewprep.profile.repository;

import com.sourav.interviewprep.profile.entity.TargetCompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TargetCompanyRepository extends JpaRepository<TargetCompanyEntity, Long> {
    List<TargetCompanyEntity> findAllByUser_IdOrderByCompanyNameAsc(Long userId);
    void deleteByUser_Id(Long userId);
}
