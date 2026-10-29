package com.nexusmarket.adapters.rest.dto;

import com.nexusmarket.domain.models.ProductType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Peticion para crear un producto.
 */
public record ProductRequestDTO(
        @NotNull(message = "sellerId is required")
        UUID sellerId,

        @NotBlank(message = "name is required")
        @Size(max = 200, message = "name must not exceed 200 characters")
        String name,

        @Size(max = 5000, message = "description must not exceed 5000 characters")
        String description,

        @NotNull(message = "type is required")
        ProductType type,

        @NotNull(message = "variants are required")
        @Valid
        List<VariantRequestDTO> variants
) {
}
