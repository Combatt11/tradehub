package com.nexusmarket.adapters.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Peticion para reservar stock.
 */
public record InventoryRequestDTO(
        @NotNull(message = "variantId is required")
        UUID variantId,

        @NotNull(message = "warehouseId is required")
        UUID warehouseId,

        @Min(value = 1, message = "quantity must be greater than zero")
        int quantity
) {
}
