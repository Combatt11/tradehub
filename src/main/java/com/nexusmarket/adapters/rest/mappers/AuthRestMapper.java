package com.nexusmarket.adapters.rest.mappers;

import com.nexusmarket.adapters.rest.dto.AuthResponseDTO;
import com.nexusmarket.adapters.rest.dto.RegisterRequestDTO;
import com.nexusmarket.application.services.AuthApplicationService.AuthResult;
import com.nexusmarket.domain.models.DocumentId;
import com.nexusmarket.domain.models.User;

import java.util.Objects;

/**
 * Traduccion entre los DTOs de autenticacion y el dominio.
 */
public final class AuthRestMapper {

    private AuthRestMapper() {
    }

    public static DocumentId toDocumentId(RegisterRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");

        boolean hasAnyDocumentField = request.documentType() != null
                || request.documentNumber() != null
                || request.documentCountry() != null;

        if (!hasAnyDocumentField) {
            return null;
        }

        if (request.documentType() == null
                || request.documentNumber() == null
                || request.documentCountry() == null) {
            throw new IllegalArgumentException(
                    "documentType, documentNumber and documentCountry are required together");
        }

        return new DocumentId(
                request.documentType(),
                request.documentNumber(),
                request.documentCountry()
        );
    }

    public static AuthResponseDTO toResponse(AuthResult result) {
        Objects.requireNonNull(result, "result must not be null");

        User user = result.user();

        return new AuthResponseDTO(
                result.token(),
                "Bearer",
                result.expiresIn(),
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
    }
}
