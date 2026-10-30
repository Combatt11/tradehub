package com.nexusmarket.adapters.rest.controllers;

import com.nexusmarket.adapters.rest.dto.ProductRequestDTO;
import com.nexusmarket.adapters.rest.dto.ProductResponseDTO;
import com.nexusmarket.adapters.rest.mappers.RestMapper;
import com.nexusmarket.adapters.security.AuthenticatedUser;
import com.nexusmarket.domain.models.Product;
import com.nexusmarket.domain.ports.in.ManageProductUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * API REST de productos.
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Productos", description = "Operaciones de gestión de productos")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ManageProductUseCase manageProductUseCase;

    public ProductController(ManageProductUseCase manageProductUseCase) {
        this.manageProductUseCase = Objects.requireNonNull(
                manageProductUseCase, "manageProductUseCase must not be null");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMINISTRATOR')")
    @Operation(summary = "Crea un producto para el vendedor autenticado")
    public ResponseEntity<ProductResponseDTO> createProduct(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ProductRequestDTO request
    ) {
        Product product = manageProductUseCase.createProduct(
                user.id(),
                request.name(),
                request.description(),
                request.type(),
                RestMapper.toVariants(request.variants())
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RestMapper.toProductResponse(product));
    }
}
