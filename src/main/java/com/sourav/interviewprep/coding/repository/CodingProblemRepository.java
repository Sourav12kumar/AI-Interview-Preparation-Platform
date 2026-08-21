package com.sourav.interviewprep.coding.repository;

import com.sourav.interviewprep.coding.entity.CodingProblemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodingProblemRepository extends JpaRepository<CodingProblemEntity, Long> {
    List<CodingProblemEntity> findAllByActiveTrueOrderByDifficultyAscTitleAsc();
    Optional<CodingProblemEntity> findByIdAndActiveTrue(Long id);
}
