package com.nexusmarket.adapters.security;

import com.nexusmarket.domain.models.User;
import com.nexusmarket.domain.ports.out.JwtTokenPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * Servicio de emision y validacion de tokens JWT con JJWT.
 */
@Service
public class JwtService implements JwtTokenPort {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${nexusmarket.security.jwt.secret}") String secret,
            @Value("${nexusmarket.security.jwt.expiration}") long expirationMillis
    ) {
        Objects.requireNonNull(secret, "secret must not be null");
        if (expirationMillis <= 0) {
            throw new IllegalArgumentException("expiration must be greater than zero");
        }

        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    @Override
    public String generateToken(User user) {
        Objects.requireNonNull(user, "user must not be null");

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(expirationMillis);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public long expirationSeconds() {
        return expirationMillis / 1000;
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public boolean isValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public Claims extractClaims(String token) {
        Objects.requireNonNull(token, "token must not be null");

        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
