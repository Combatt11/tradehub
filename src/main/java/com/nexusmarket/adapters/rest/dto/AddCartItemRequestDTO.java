package com.nexusmarket.adapters.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Peticion para añadir una variante al carrito.
 */
public record AddCartItemRequestDTO(
        @NotNull(message = "variantId is required")
        UUID variantId,

        @Min(value = 1, message = "quantity must be greater than zero")
        @Max(value = 99, message = "quantity must not exceed 99")
        int quantity
) {
}
