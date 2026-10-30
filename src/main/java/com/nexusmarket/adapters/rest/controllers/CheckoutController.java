package com.nexusmarket.adapters.rest.controllers;

import com.nexusmarket.adapters.rest.dto.CheckoutResponseDTO;
import com.nexusmarket.adapters.security.AuthenticatedUser;
import com.nexusmarket.domain.ports.in.CheckoutUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * API REST del checkout multi-vendedor.
 */
@RestController
@Tag(name = "Checkout", description = "Operaciones de compra")
@SecurityRequirement(name = "bearerAuth")
public class CheckoutController {

    private final CheckoutUseCase checkoutUseCase;

    public CheckoutController(CheckoutUseCase checkoutUseCase) {
        this.checkoutUseCase = Objects.requireNonNull(
                checkoutUseCase, "checkoutUseCase must not be null");
    }

    @PostMapping("/api/checkout")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Realiza el checkout del carrito actual")
    public ResponseEntity<CheckoutResponseDTO> checkout(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        List<UUID> orderIds = checkoutUseCase.checkout(user.id());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CheckoutResponseDTO(orderIds));
    }
}
