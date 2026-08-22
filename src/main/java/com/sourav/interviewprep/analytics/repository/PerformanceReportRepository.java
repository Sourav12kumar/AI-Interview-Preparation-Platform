package com.sourav.interviewprep.analytics.repository;

import com.sourav.interviewprep.analytics.entity.PerformanceReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PerformanceReportRepository extends JpaRepository<PerformanceReportEntity, Long> {
    List<PerformanceReportEntity> findAllByUser_IdOrderByGeneratedAtDesc(Long userId);
    Optional<PerformanceReportEntity> findByIdAndUser_Id(Long id, Long userId);
    Optional<PerformanceReportEntity> findByUser_IdAndPeriodStartAndPeriodEnd(
            Long userId,
            LocalDate periodStart,
            LocalDate periodEnd);
}
