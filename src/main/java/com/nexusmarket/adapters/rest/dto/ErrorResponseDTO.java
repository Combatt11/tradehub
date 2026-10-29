package com.nexusmarket.adapters.rest.dto;

import java.time.Instant;

/**
 * Cuerpo JSON comun para errores de la API.
 */
public record ErrorResponseDTO(
        Instant timestamp,
        int status,
        String error,
        String message
) {
}
