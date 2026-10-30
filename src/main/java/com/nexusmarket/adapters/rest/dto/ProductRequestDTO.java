package com.nexusmarket.adapters.rest.dto;

import com.nexusmarket.domain.models.ProductType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Peticion para crear un producto.
 *
 * <p>El vendedor se obtiene del token JWT, nunca del cuerpo de la peticion.</p>
 */
public record ProductRequestDTO(
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
