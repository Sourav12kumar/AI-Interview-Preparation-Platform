package com.sourav.interviewprep.auth.browser;

import com.sourav.interviewprep.auth.security.JwtProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class BrowserRefreshCookieService {

    public static final String COOKIE_NAME = "ai_interview_refresh";
    private static final String COOKIE_PATH = "/api/v1/auth/browser";

    private final BrowserAuthProperties browserProperties;
    private final JwtProperties jwtProperties;

    public BrowserRefreshCookieService(
            BrowserAuthProperties browserProperties,
            JwtProperties jwtProperties) {
        this.browserProperties = browserProperties;
        this.jwtProperties = jwtProperties;
    }

    ResponseCookie issue(String refreshToken) {
        return base(refreshToken)
                .maxAge(jwtProperties.refreshTtl())
                .build();
    }

    ResponseCookie clear() {
        return base("")
                .maxAge(0)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(browserProperties.browserCookieSecure())
                .sameSite("Strict")
                .path(COOKIE_PATH);
    }
}
