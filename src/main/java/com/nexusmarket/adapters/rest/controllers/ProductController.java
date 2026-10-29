package com.nexusmarket.adapters.rest.controllers;

import com.nexusmarket.adapters.rest.dto.ProductRequestDTO;
import com.nexusmarket.adapters.rest.dto.ProductResponseDTO;
import com.nexusmarket.adapters.rest.mappers.RestMapper;
import com.nexusmarket.domain.models.Product;
import com.nexusmarket.domain.ports.in.ManageProductUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class ProductController {

    private final ManageProductUseCase manageProductUseCase;

    public ProductController(ManageProductUseCase manageProductUseCase) {
        this.manageProductUseCase = Objects.requireNonNull(
                manageProductUseCase, "manageProductUseCase must not be null");
    }

    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(
            @Valid @RequestBody ProductRequestDTO request
    ) {
        Product product = manageProductUseCase.createProduct(
                request.sellerId(),
                request.name(),
                request.description(),
                request.type(),
                RestMapper.toVariants(request.variants())
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RestMapper.toProductResponse(product));
    }
}
