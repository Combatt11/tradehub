package com.nexusmarket.adapters.rest.controllers;

import com.nexusmarket.adapters.rest.dto.CheckoutRequestDTO;
import com.nexusmarket.adapters.rest.dto.CheckoutResponseDTO;
import com.nexusmarket.domain.ports.in.CheckoutUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * API REST del checkout multi-vendedor.
 */
@RestController
public class CheckoutController {

    private final CheckoutUseCase checkoutUseCase;

    public CheckoutController(CheckoutUseCase checkoutUseCase) {
        this.checkoutUseCase = Objects.requireNonNull(
                checkoutUseCase, "checkoutUseCase must not be null");
    }

    @PostMapping("/api/checkout")
    public ResponseEntity<CheckoutResponseDTO> checkout(
            @Valid @RequestBody CheckoutRequestDTO request
    ) {
        List<UUID> orderIds = checkoutUseCase.checkout(
                request.buyerId(),
                request.items()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CheckoutResponseDTO(orderIds));
    }
}
