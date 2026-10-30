package com.nexusmarket.adapters.rest.dto;

import com.nexusmarket.domain.models.IdentityDocumentType;
import com.nexusmarket.domain.models.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de registro de una cuenta.
 */
public record RegisterRequestDTO(
        @NotBlank(message = "email is required")
        @Email(message = "email format is invalid")
        @Size(max = 254, message = "email exceeds max length")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must have between 8 and 72 characters")
        String password,

        @NotNull(message = "role is required")
        UserRole role,

        IdentityDocumentType documentType,

        @Size(max = 64, message = "documentNumber exceeds max length")
        String documentNumber,

        @Size(min = 2, max = 2, message = "documentCountry must have 2 characters")
        String documentCountry
) {
}
