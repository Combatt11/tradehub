package com.nexusmarket.application.services;

import com.nexusmarket.domain.models.DocumentId;
import com.nexusmarket.domain.models.InvalidCredentialsException;
import com.nexusmarket.domain.models.User;
import com.nexusmarket.domain.models.UserRole;
import com.nexusmarket.domain.ports.out.JwtTokenPort;
import com.nexusmarket.domain.ports.out.PasswordHasherPort;
import com.nexusmarket.domain.ports.out.UserRepositoryPort;
import com.nexusmarket.domain.ports.out.UserRepositoryPort.UserCredentials;

import java.util.Objects;
import java.util.UUID;

/**
 * Servicio de aplicacion para registro y autenticacion de usuarios.
 */
public final class AuthApplicationService {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final JwtTokenPort jwtTokenPort;

    public AuthApplicationService(
            UserRepositoryPort userRepositoryPort,
            PasswordHasherPort passwordHasherPort,
            JwtTokenPort jwtTokenPort
    ) {
        this.userRepositoryPort = Objects.requireNonNull(
                userRepositoryPort, "userRepositoryPort must not be null");
        this.passwordHasherPort = Objects.requireNonNull(
                passwordHasherPort, "passwordHasherPort must not be null");
        this.jwtTokenPort = Objects.requireNonNull(
                jwtTokenPort, "jwtTokenPort must not be null");
    }

    public AuthResult register(
            String email,
            String rawPassword,
            UserRole role,
            DocumentId documentId
    ) {
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(rawPassword, "rawPassword must not be null");
        Objects.requireNonNull(role, "role must not be null");

        if (!isPubliclyRegistrable(role)) {
            throw new IllegalArgumentException(
                    "Public registration only supports BUYER and SELLER roles");
        }

        if (userRepositoryPort.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = User.create(UUID.randomUUID(), documentId, email, role);
        userRepositoryPort.save(user, passwordHasherPort.encode(rawPassword));

        return createResult(user);
    }

    public AuthResult login(String email, String rawPassword) {
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(rawPassword, "rawPassword must not be null");

        UserCredentials credentials = userRepositoryPort.findCredentialsByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasherPort.matches(rawPassword, credentials.passwordHash())
                || !credentials.user().isActive()) {
            throw new InvalidCredentialsException();
        }

        return createResult(credentials.user());
    }

    private AuthResult createResult(User user) {
        return new AuthResult(
                jwtTokenPort.generateToken(user),
                jwtTokenPort.expirationSeconds(),
                user
        );
    }

    private static boolean isPubliclyRegistrable(UserRole role) {
        return role == UserRole.BUYER || role == UserRole.SELLER;
    }

    public record AuthResult(String token, long expiresIn, User user) {

        public AuthResult {
            Objects.requireNonNull(token, "token must not be null");
            Objects.requireNonNull(user, "user must not be null");
            if (expiresIn <= 0) {
                throw new IllegalArgumentException("expiresIn must be greater than zero");
            }
        }
    }
}
