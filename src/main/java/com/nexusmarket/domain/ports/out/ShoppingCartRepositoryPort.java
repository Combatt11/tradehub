package com.nexusmarket.domain.ports.out;

import com.nexusmarket.domain.models.ShoppingCart;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para persistir carritos de compras.
 */
public interface ShoppingCartRepositoryPort {

    ShoppingCart save(ShoppingCart cart);

    Optional<ShoppingCart> findByBuyerId(UUID buyerId);

    void deleteByBuyerId(UUID buyerId);
}
