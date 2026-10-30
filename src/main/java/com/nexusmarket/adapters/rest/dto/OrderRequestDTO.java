package com.nexusmarket.adapters.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Peticion para crear una orden.
 *
 * <p>El comprador se obtiene del token JWT. El vendedor sigue identificando la
 * parte del catalogo sobre la que se crea la orden.</p>
 */
public record OrderRequestDTO(
        @NotNull(message = "sellerId is required")
        UUID sellerId,

        @NotEmpty(message = "lines must contain at least one item")
        @Valid
        List<OrderLineRequestDTO> lines
) {
}
