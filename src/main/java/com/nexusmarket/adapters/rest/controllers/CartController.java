package com.nexusmarket.adapters.rest.controllers;

import com.nexusmarket.adapters.rest.dto.AddCartItemRequestDTO;
import com.nexusmarket.adapters.rest.dto.CartResponseDTO;
import com.nexusmarket.adapters.rest.mappers.CartRestMapper;
import com.nexusmarket.adapters.security.AuthenticatedUser;
import com.nexusmarket.domain.models.ShoppingCart;
import com.nexusmarket.domain.ports.in.ManageCartUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * API REST del carrito de compras.
 */
@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('BUYER')")
@Tag(name = "Carrito", description = "Operaciones del carrito de compras")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final ManageCartUseCase manageCartUseCase;

    public CartController(ManageCartUseCase manageCartUseCase) {
        this.manageCartUseCase = Objects.requireNonNull(
                manageCartUseCase, "manageCartUseCase must not be null");
    }

    @GetMapping
    @Operation(summary = "Obtiene el carrito del comprador autenticado")
    public ResponseEntity<CartResponseDTO> getCart(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        ShoppingCart cart = manageCartUseCase.getCart(user.id());
        return ResponseEntity.ok(CartRestMapper.toResponse(cart));
    }

    @PostMapping("/items")
    @Operation(summary = "Añade una variante al carrito")
    public ResponseEntity<CartResponseDTO> addItem(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AddCartItemRequestDTO request
    ) {
        ShoppingCart cart = manageCartUseCase.addToCart(
                user.id(),
                request.variantId(),
                request.quantity()
        );

        return ResponseEntity.ok(CartRestMapper.toResponse(cart));
    }

    @DeleteMapping("/items/{variantId}")
    @Operation(summary = "Elimina una variante del carrito")
    public ResponseEntity<Void> removeItem(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable("variantId") UUID variantId
    ) {
        manageCartUseCase.removeFromCart(user.id(), variantId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Vacía el carrito del comprador autenticado")
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        manageCartUseCase.clearCart(user.id());
        return ResponseEntity.noContent().build();
    }
}
