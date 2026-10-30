package com.nexusmarket.adapters.persistence.jpa;

import com.nexusmarket.domain.models.DocumentId;
import com.nexusmarket.domain.models.User;

import java.util.Objects;

/**
 * Traduccion entre {@link User} y {@link UserEntity}.
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static User toDomain(UserEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");

        DocumentId documentId = toDocumentId(entity);

        return new User(
                entity.getId(),
                documentId,
                entity.getEmail(),
                entity.getRole(),
                entity.getStatus()
        );
    }

    public static void copyToEntity(User user, String passwordHash, UserEntity entity) {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        Objects.requireNonNull(entity, "entity must not be null");

        entity.setId(user.getId());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(passwordHash);
        entity.setRole(user.getRole());
        entity.setStatus(user.getStatus());

        DocumentId documentId = user.getDocumentId();
        if (documentId == null) {
            entity.setDocumentType(null);
            entity.setDocumentNumber(null);
            entity.setDocumentCountry(null);
            return;
        }

        entity.setDocumentType(documentId.type());
        entity.setDocumentNumber(documentId.number());
        entity.setDocumentCountry(documentId.country());
    }

    private static DocumentId toDocumentId(UserEntity entity) {
        if (entity.getDocumentType() == null
                && entity.getDocumentNumber() == null
                && entity.getDocumentCountry() == null) {
            return null;
        }

        return new DocumentId(
                entity.getDocumentType(),
                entity.getDocumentNumber(),
                entity.getDocumentCountry()
        );
    }
}
