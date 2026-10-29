package com.nexusmarket.domain.models;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Usuario de la plataforma. Cada cuenta tiene exactamente un rol.
 */
public final class User {

    private final UUID id;
    private final DocumentId documentId;
    private final String email;
    private final UserRole role;
    private UserStatus status;

    public User(UUID id, DocumentId documentId, String email, UserRole role, UserStatus status) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.email = normalizeEmail(email);

        if (role.requiresIdentityDocument() && documentId == null) {
            throw new IllegalArgumentException("documentId is required for role " + role);
        }
        this.documentId = documentId;
    }

    public static User create(UUID id, DocumentId documentId, String email, UserRole role) {
        return new User(id, documentId, email, role, UserStatus.ACTIVE);
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void block() {
        if (status == UserStatus.INACTIVE) {
            throw new IllegalStateException("An inactive user cannot be blocked");
        }
        this.status = UserStatus.BLOCKED;
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }

    public boolean isActive() {
        return status.isActive();
    }

    public UUID getId() {
        return id;
    }

    public DocumentId getDocumentId() {
        return documentId;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    private static String normalizeEmail(String email) {
        Objects.requireNonNull(email, "email must not be null");
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > 254) {
            throw new IllegalArgumentException("email must have between 1 and 254 characters");
        }
        if (!normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("email format is invalid");
        }
        return normalized;
    }
}
