package com.nexusmarket.adapters.security;

import com.nexusmarket.domain.models.User;
import com.nexusmarket.domain.models.UserRole;

import java.util.Objects;
import java.util.UUID;

/**
 * Principal autenticado expuesto a los controladores REST.
 */
public record AuthenticatedUser(UUID id, String email, UserRole role) {

    public AuthenticatedUser {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(role, "role must not be null");
    }

    public static AuthenticatedUser from(User user) {
        Objects.requireNonNull(user, "user must not be null");
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole());
    }
}
