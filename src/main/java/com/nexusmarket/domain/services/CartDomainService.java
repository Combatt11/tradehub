package com.nexusmarket.domain.services;

import com.nexusmarket.domain.models.ShoppingCart;
import com.nexusmarket.domain.ports.in.ManageCartUseCase;
import com.nexusmarket.domain.ports.out.ShoppingCartRepositoryPort;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

/**
 * Servicio de dominio para la gestion del carrito de compras.
 *
 * <p>No depende de Spring ni de persistencia: usa exclusivamente el puerto de
 * salida del carrito.</p>
 */
public final class CartDomainService implements ManageCartUseCase {

    private final ShoppingCartRepositoryPort shoppingCartRepository;

    public CartDomainService(ShoppingCartRepositoryPort shoppingCartRepository) {
        this.shoppingCartRepository = Objects.requireNonNull(
                shoppingCartRepository, "shoppingCartRepository must not be null");
    }

    @Override
    public ShoppingCart addToCart(UUID buyerId, UUID variantId, int quantity) {
        Objects.requireNonNull(buyerId, "buyerId must not be null");

        ShoppingCart cart = shoppingCartRepository.findByBuyerId(buyerId)
                .orElseGet(() -> ShoppingCart.create(UUID.randomUUID(), buyerId));

        cart.addItem(variantId, quantity);

        return shoppingCartRepository.save(cart);
    }

    @Override
    public ShoppingCart getCart(UUID buyerId) {
        Objects.requireNonNull(buyerId, "buyerId must not be null");

        return shoppingCartRepository.findByBuyerId(buyerId)
                .orElseGet(() -> shoppingCartRepository.save(
                        ShoppingCart.create(UUID.randomUUID(), buyerId)));
    }

    @Override
    public void removeFromCart(UUID buyerId, UUID variantId) {
        Objects.requireNonNull(buyerId, "buyerId must not be null");

        ShoppingCart cart = shoppingCartRepository.findByBuyerId(buyerId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Cart not found for buyer: " + buyerId));

        cart.removeItem(variantId);
        shoppingCartRepository.save(cart);
    }

    @Override
    public void clearCart(UUID buyerId) {
        Objects.requireNonNull(buyerId, "buyerId must not be null");
        shoppingCartRepository.deleteByBuyerId(buyerId);
    }
}
