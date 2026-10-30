package com.nexusmarket.domain.ports.in;

import com.nexusmarket.domain.models.ShoppingCart;

import java.util.UUID;

/**
 * Puerto de entrada para la gestion del carrito de compras.
 */
public interface ManageCartUseCase {

    ShoppingCart addToCart(UUID buyerId, UUID variantId, int quantity);

    ShoppingCart getCart(UUID buyerId);

    void removeFromCart(UUID buyerId, UUID variantId);

    void clearCart(UUID buyerId);
}
