package com.nexusmarket.adapters.persistence.jpa;

import com.nexusmarket.domain.models.ShoppingCart;
import com.nexusmarket.domain.ports.out.ShoppingCartRepositoryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida que implementa {@link ShoppingCartRepositoryPort}
 * mediante JPA.
 */
@Repository
public class ShoppingCartRepositoryAdapter implements ShoppingCartRepositoryPort {

    private final JpaCartRepository cartRepository;

    public ShoppingCartRepositoryAdapter(JpaCartRepository cartRepository) {
        this.cartRepository = Objects.requireNonNull(
                cartRepository, "cartRepository must not be null");
    }

    @Override
    @Transactional
    public ShoppingCart save(ShoppingCart cart) {
        Objects.requireNonNull(cart, "cart must not be null");

        CartEntity entity = cartRepository.findById(cart.getId())
                .map(existing -> {
                    CartMapper.updateEntity(existing, cart);
                    return existing;
                })
                .orElseGet(() -> CartMapper.toEntity(cart));

        return CartMapper.toDomain(cartRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShoppingCart> findByBuyerId(UUID buyerId) {
        Objects.requireNonNull(buyerId, "buyerId must not be null");
        return cartRepository.findByBuyerId(buyerId).map(CartMapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteByBuyerId(UUID buyerId) {
        Objects.requireNonNull(buyerId, "buyerId must not be null");
        cartRepository.findByBuyerId(buyerId).ifPresent(cartRepository::delete);
    }
}
