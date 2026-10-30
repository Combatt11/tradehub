package com.nexusmarket.adapters.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales de inicio de sesion.
 */
public record LoginRequestDTO(
        @NotBlank(message = "email is required")
        @Email(message = "email format is invalid")
        String email,

        @NotBlank(message = "password is required")
        String password
) {
}
