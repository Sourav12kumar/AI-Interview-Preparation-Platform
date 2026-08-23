package com.sourav.interviewprep.auth.browser;

import com.sourav.interviewprep.auth.dto.AuthResponse;
import com.sourav.interviewprep.auth.dto.LoginRequest;
import com.sourav.interviewprep.auth.dto.RegisterRequest;
import com.sourav.interviewprep.auth.exception.InvalidTokenException;
import com.sourav.interviewprep.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/browser")
public class BrowserAuthController {

    private final AuthService authService;
    private final BrowserRefreshCookieService cookieService;

    public BrowserAuthController(
            AuthService authService,
            BrowserRefreshCookieService cookieService) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    @PostMapping("/register")
    ResponseEntity<BrowserAuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return authenticated(authService.register(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    ResponseEntity<BrowserAuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return authenticated(authService.login(request), HttpStatus.OK);
    }

    @PostMapping("/refresh")
    ResponseEntity<BrowserAuthResponse> refresh(
            @CookieValue(
                    name = BrowserRefreshCookieService.COOKIE_NAME,
                    required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException("Browser refresh session is missing");
        }
        return authenticated(authService.refresh(refreshToken), HttpStatus.OK);
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            @CookieValue(
                    name = BrowserRefreshCookieService.COOKIE_NAME,
                    required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                authService.logout(refreshToken);
            } catch (InvalidTokenException ignored) {
                // Logout remains idempotent and always removes the browser credential.
            }
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieService.clear().toString())
                .cacheControl(CacheControl.noStore())
                .build();
    }

    private ResponseEntity<BrowserAuthResponse> authenticated(
            AuthResponse response,
            HttpStatus status) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookieService.issue(response.refreshToken()).toString())
                .cacheControl(CacheControl.noStore())
                .body(BrowserAuthResponse.from(response));
    }
}
