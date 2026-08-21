package com.sourav.interviewprep.interview.repository;

import com.sourav.interviewprep.interview.entity.InterviewSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewSessionRepository extends JpaRepository<InterviewSessionEntity, Long> {
    List<InterviewSessionEntity> findAllByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<InterviewSessionEntity> findByIdAndUser_Id(Long id, Long userId);
}
