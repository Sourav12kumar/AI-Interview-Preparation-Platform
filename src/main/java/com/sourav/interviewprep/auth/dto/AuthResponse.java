package com.sourav.interviewprep.auth.dto;

import java.time.Instant;

public record AuthResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        UserResponse user
) {
}
