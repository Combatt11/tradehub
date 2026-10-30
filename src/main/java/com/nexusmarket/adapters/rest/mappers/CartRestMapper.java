package com.nexusmarket.adapters.rest.mappers;

import com.nexusmarket.adapters.rest.dto.CartItemResponseDTO;
import com.nexusmarket.adapters.rest.dto.CartResponseDTO;
import com.nexusmarket.domain.models.ShoppingCart;

import java.util.List;
import java.util.Objects;

/**
 * Traduccion entre el carrito de dominio y los DTOs REST.
 */
public final class CartRestMapper {

    private CartRestMapper() {
    }

    public static CartResponseDTO toResponse(ShoppingCart cart) {
        Objects.requireNonNull(cart, "cart must not be null");

        List<CartItemResponseDTO> items = cart.getItems().stream()
                .map(item -> new CartItemResponseDTO(
                        item.variantId(),
                        item.quantity()
                ))
                .toList();

        return new CartResponseDTO(
                cart.getId(),
                cart.getBuyerId(),
                items
        );
    }
}
