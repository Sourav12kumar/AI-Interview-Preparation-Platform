package com.sourav.interviewprep.auth.security;

import com.sourav.interviewprep.auth.entity.UserEntity;
import com.sourav.interviewprep.auth.exception.InvalidTokenException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder tokenJwtDecoder;
    private final JwtProperties properties;

    public TokenService(
            JwtEncoder jwtEncoder,
            @Qualifier("tokenJwtDecoder") JwtDecoder tokenJwtDecoder,
            JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.tokenJwtDecoder = tokenJwtDecoder;
        this.properties = properties;
    }

    public TokenPair issueTokenPair(UserEntity user) {
        Instant now = Instant.now();
        Instant accessExpiry = now.plus(properties.accessTtl());
        Instant refreshExpiry = now.plus(properties.refreshTtl());
        String accessJti = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();

        String accessToken = encode(user, "access", accessJti, now, accessExpiry);
        String refreshToken = encode(user, "refresh", refreshJti, now, refreshExpiry);
        return new TokenPair(accessToken, refreshToken, accessExpiry, refreshExpiry, refreshJti);
    }

    public Jwt decodeRefreshToken(String token) {
        try {
            Jwt jwt = tokenJwtDecoder.decode(token);
            if (!"refresh".equals(jwt.getClaimAsString("token_type"))) {
                throw new InvalidTokenException("A refresh token is required");
            }
            return jwt;
        } catch (JwtException exception) {
            throw new InvalidTokenException("Refresh token is invalid or expired", exception);
        }
    }

    private String encode(UserEntity user, String type, String jti, Instant issuedAt, Instant expiresAt) {
        List<String> roles = user.getRoles().stream().map(role -> role.getName()).sorted().toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.getEmail())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(jti)
                .claim("user_id", user.getId())
                .claim("roles", roles)
                .claim("token_type", type)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
