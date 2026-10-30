package com.nexusmarket.adapters.rest.dto;

import com.nexusmarket.domain.ports.in.CheckoutItemDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Peticion de checkout multi-vendedor.
 *
 * <p>El comprador se obtiene del token JWT, nunca del cuerpo de la peticion.</p>
 */
public record CheckoutRequestDTO(
        @NotEmpty(message = "items must contain at least one item")
        @Valid
        List<CheckoutItemDTO> items
) {
}
