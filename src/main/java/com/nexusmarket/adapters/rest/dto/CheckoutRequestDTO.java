package com.nexusmarket.adapters.rest.dto;

import com.nexusmarket.domain.ports.in.CheckoutItemDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Peticion de checkout multi-vendedor.
 */
public record CheckoutRequestDTO(
        @NotNull(message = "buyerId is required")
        UUID buyerId,

        @NotEmpty(message = "items must contain at least one item")
        @Valid
        List<CheckoutItemDTO> items
) {
}
