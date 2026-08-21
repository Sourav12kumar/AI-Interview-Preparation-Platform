package com.sourav.interviewprep.coding.repository;

import com.sourav.interviewprep.coding.entity.CodingSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodingSubmissionRepository extends JpaRepository<CodingSubmissionEntity, Long> {
    List<CodingSubmissionEntity> findAllByUser_IdOrderBySubmittedAtDesc(Long userId);
    Optional<CodingSubmissionEntity> findByIdAndUser_Id(Long id, Long userId);
}
