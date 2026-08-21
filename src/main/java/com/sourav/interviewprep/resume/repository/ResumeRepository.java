package com.sourav.interviewprep.resume.repository;

import com.sourav.interviewprep.resume.entity.ResumeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<ResumeEntity, Long> {
    List<ResumeEntity> findAllByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<ResumeEntity> findByIdAndUser_Id(Long id, Long userId);
}
