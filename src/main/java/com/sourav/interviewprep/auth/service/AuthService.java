package com.sourav.interviewprep.auth.service;

import com.sourav.interviewprep.auth.dto.AuthResponse;
import com.sourav.interviewprep.auth.dto.LoginRequest;
import com.sourav.interviewprep.auth.dto.RegisterRequest;
import com.sourav.interviewprep.auth.dto.UserResponse;
import com.sourav.interviewprep.auth.entity.RefreshTokenEntity;
import com.sourav.interviewprep.auth.entity.RoleEntity;
import com.sourav.interviewprep.auth.entity.UserEntity;
import com.sourav.interviewprep.auth.exception.DuplicateEmailException;
import com.sourav.interviewprep.auth.exception.InvalidTokenException;
import com.sourav.interviewprep.auth.repository.RefreshTokenRepository;
import com.sourav.interviewprep.auth.repository.RoleRepository;
import com.sourav.interviewprep.auth.repository.UserRepository;
import com.sourav.interviewprep.auth.security.TokenPair;
import com.sourav.interviewprep.auth.security.TokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            TokenService tokenService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }

        RoleEntity defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Default role is not configured"));
        UserEntity user = new UserEntity(
                email,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                defaultRole
        );
        userRepository.saveAndFlush(user);
        return createSession(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));

        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
        user.recordLogin();
        return createSession(user);
    }

    @Transactional(noRollbackFor = InvalidTokenException.class)
    public AuthResponse refresh(String refreshToken) {
        Jwt jwt = tokenService.decodeRefreshToken(refreshToken);
        String jti = requireJti(jwt);
        RefreshTokenEntity current = refreshTokenRepository.findByJtiForUpdate(jti)
                .orElseThrow(() -> new InvalidTokenException("Refresh token has been revoked"));

        Instant now = Instant.now();
        if (!current.isUsable(now)) {
            refreshTokenRepository.revokeAllActiveByUserId(current.getUser().getId(), now);
            throw new InvalidTokenException("Refresh token reuse or expiry detected");
        }
        if (!current.getUser().isActive()) {
            throw new InvalidTokenException("User account is not active");
        }

        TokenPair replacement = tokenService.issueTokenPair(current.getUser());
        current.revoke(replacement.refreshJti());
        refreshTokenRepository.save(new RefreshTokenEntity(
                replacement.refreshJti(), current.getUser(), replacement.refreshTokenExpiresAt()));
        return response(current.getUser(), replacement);
    }

    @Transactional
    public void logout(String refreshToken) {
        Jwt jwt = tokenService.decodeRefreshToken(refreshToken);
        RefreshTokenEntity token = refreshTokenRepository.findByJtiForUpdate(requireJti(jwt))
                .orElseThrow(() -> new InvalidTokenException("Refresh token has already been revoked"));
        if (token.getRevokedAt() == null) {
            token.revoke(null);
        }
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(UserResponse::from)
                .orElseThrow(() -> new UsernameNotFoundException("User no longer exists"));
    }

    private AuthResponse createSession(UserEntity user) {
        TokenPair pair = tokenService.issueTokenPair(user);
        refreshTokenRepository.save(new RefreshTokenEntity(
                pair.refreshJti(), user, pair.refreshTokenExpiresAt()));
        return response(user, pair);
    }

    private AuthResponse response(UserEntity user, TokenPair pair) {
        return new AuthResponse(
                "Bearer",
                pair.accessToken(),
                pair.refreshToken(),
                pair.accessTokenExpiresAt(),
                UserResponse.from(user)
        );
    }

    private String requireJti(Jwt jwt) {
        if (jwt.getId() == null || jwt.getId().isBlank()) {
            throw new InvalidTokenException("Refresh token identifier is missing");
        }
        return jwt.getId();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
