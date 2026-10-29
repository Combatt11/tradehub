package com.nexusmarket.adapters.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Linea de pedido recibida por la API.
 *
 * <p>El cliente solo identifica la variante y la cantidad. El precio se
 * resuelve siempre en el servidor desde el catalogo.</p>
 */
public record OrderLineRequestDTO(
        @NotNull(message = "variantId is required")
        UUID variantId,

        @Min(value = 1, message = "quantity must be greater than zero")
        int quantity
) {
}
