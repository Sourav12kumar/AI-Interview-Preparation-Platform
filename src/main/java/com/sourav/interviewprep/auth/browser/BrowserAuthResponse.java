package com.sourav.interviewprep.auth.browser;

import com.sourav.interviewprep.auth.dto.AuthResponse;
import com.sourav.interviewprep.auth.dto.UserResponse;

import java.time.Instant;

public record BrowserAuthResponse(
        String tokenType,
        String accessToken,
        Instant accessTokenExpiresAt,
        UserResponse user
) {
    static BrowserAuthResponse from(AuthResponse response) {
        return new BrowserAuthResponse(
                response.tokenType(), response.accessToken(),
                response.accessTokenExpiresAt(), response.user());
    }
}
