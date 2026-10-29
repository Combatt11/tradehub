package com.nexusmarket.domain.models;

/**
 * Estados comerciales de una cuenta de usuario.
 */
public enum UserStatus {
    ACTIVE,
    BLOCKED,
    INACTIVE;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
