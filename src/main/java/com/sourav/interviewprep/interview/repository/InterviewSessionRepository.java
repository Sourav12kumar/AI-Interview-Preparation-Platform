package com.sourav.interviewprep.interview.repository;

import com.sourav.interviewprep.interview.entity.InterviewSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import com.sourav.interviewprep.interview.entity.InterviewStatus;

public interface InterviewSessionRepository extends JpaRepository<InterviewSessionEntity, Long> {
    List<InterviewSessionEntity> findAllByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<InterviewSessionEntity> findByIdAndUser_Id(Long id, Long userId);

    @Query("select session from InterviewSessionEntity session "
            + "where session.user.id = :userId and session.status = :status "
            + "and session.completedAt >= :from and session.completedAt < :to "
            + "order by session.completedAt asc")
    List<InterviewSessionEntity> findCompletedInPeriod(
            @Param("userId") Long userId,
            @Param("status") InterviewStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from InterviewSessionEntity session "
            + "where session.id = :id and session.user.id = :userId")
    Optional<InterviewSessionEntity> findOwnedForUpdate(
            @Param("id") Long id,
            @Param("userId") Long userId);
}
