package com.sourav.interviewprep.analytics.controller;

import com.sourav.interviewprep.analytics.dto.AnalyticsDashboardResponse;
import com.sourav.interviewprep.analytics.dto.GeneratePerformanceReportRequest;
import com.sourav.interviewprep.analytics.dto.PerformanceReportResponse;
import com.sourav.interviewprep.analytics.dto.PerformanceReportSummaryResponse;
import com.sourav.interviewprep.analytics.service.AnalyticsService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    AnalyticsDashboardResponse dashboard(
            Authentication authentication,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analyticsService.dashboard(authentication.getName(), from, to);
    }

    @PostMapping("/reports")
    PerformanceReportResponse generateReport(
            Authentication authentication,
            @Valid @RequestBody GeneratePerformanceReportRequest request) {
        return analyticsService.generateReport(authentication.getName(), request);
    }

    @GetMapping("/reports")
    List<PerformanceReportSummaryResponse> listReports(Authentication authentication) {
        return analyticsService.listReports(authentication.getName());
    }

    @GetMapping("/reports/{reportId}")
    PerformanceReportResponse getReport(
            Authentication authentication,
            @PathVariable Long reportId) {
        return analyticsService.getReport(authentication.getName(), reportId);
    }
}
