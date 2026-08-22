package com.sourav.interviewprep.coding.repository;

import com.sourav.interviewprep.coding.entity.CodingSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CodingSubmissionRepository extends JpaRepository<CodingSubmissionEntity, Long> {
    List<CodingSubmissionEntity> findAllByUser_IdOrderBySubmittedAtDesc(Long userId);
    Optional<CodingSubmissionEntity> findByIdAndUser_Id(Long id, Long userId);

    @Query("select submission from CodingSubmissionEntity submission "
            + "join fetch submission.problem problem "
            + "where submission.user.id = :userId "
            + "and submission.submittedAt >= :from and submission.submittedAt < :to "
            + "order by submission.submittedAt asc")
    List<CodingSubmissionEntity> findAllInPeriod(
            @Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
