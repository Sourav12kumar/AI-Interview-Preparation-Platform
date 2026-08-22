package com.sourav.interviewprep.admin.dto;

import com.sourav.interviewprep.auth.entity.AccountStatus;

import java.time.Instant;
import java.util.List;

public record AdminUserResponse(
        Long id,
        String email,
        String fullName,
        AccountStatus status,
        boolean emailVerified,
        List<String> roles,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {
}
