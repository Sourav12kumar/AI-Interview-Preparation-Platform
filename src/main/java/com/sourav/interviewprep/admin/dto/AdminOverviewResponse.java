package com.sourav.interviewprep.admin.dto;

public record AdminOverviewResponse(
        long totalUsers,
        long activeUsers,
        long suspendedUsers,
        long totalCodingProblems,
        long activeCodingProblems,
        long codingSubmissions,
        long interviewSessions
) {
}
