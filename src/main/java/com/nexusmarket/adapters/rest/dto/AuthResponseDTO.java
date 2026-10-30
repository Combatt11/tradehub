package com.nexusmarket.adapters.rest.dto;

import com.nexusmarket.domain.models.UserRole;

import java.util.UUID;

/**
 * Respuesta de autenticacion con el token JWT emitido.
 */
public record AuthResponseDTO(
        String token,
        String tokenType,
        long expiresIn,
        UUID userId,
        String email,
        UserRole role
) {
}
