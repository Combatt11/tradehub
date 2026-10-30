package com.nexusmarket.adapters.persistence.jpa;

import com.nexusmarket.domain.models.CartItem;
import com.nexusmarket.domain.models.ShoppingCart;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Traduccion entre {@link ShoppingCart} y {@link CartEntity}.
 */
public final class CartMapper {

    private CartMapper() {
    }

    public static ShoppingCart toDomain(CartEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");

        List<CartItem> items = entity.getItems().stream()
                .map(CartMapper::toDomain)
                .toList();

        return new ShoppingCart(entity.getId(), entity.getBuyerId(), items);
    }

    public static CartEntity toEntity(ShoppingCart cart) {
        Objects.requireNonNull(cart, "cart must not be null");

        CartEntity entity = new CartEntity();
        entity.setId(cart.getId());
        entity.setBuyerId(cart.getBuyerId());

        for (CartItem item : cart.getItems()) {
            entity.getItems().add(toEntity(item, entity));
        }

        return entity;
    }

    /**
     * Actualiza una entidad gestionada conservando los identificadores de las
     * lineas existentes.
     */
    public static void updateEntity(CartEntity entity, ShoppingCart cart) {
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(cart, "cart must not be null");

        entity.setBuyerId(cart.getBuyerId());

        Map<UUID, CartItemEntity> existingByVariant = entity.getItems().stream()
                .collect(Collectors.toMap(
                        CartItemEntity::getVariantId,
                        Function.identity(),
                        (first, ignored) -> first
                ));

        Set<UUID> domainVariantIds = cart.getItems().stream()
                .map(CartItem::variantId)
                .collect(Collectors.toSet());

        entity.getItems().removeIf(item -> !domainVariantIds.contains(item.getVariantId()));

        for (CartItem item : cart.getItems()) {
            CartItemEntity itemEntity = existingByVariant.get(item.variantId());
            if (itemEntity == null) {
                itemEntity = new CartItemEntity();
                itemEntity.setId(UUID.randomUUID());
                itemEntity.setCart(entity);
                entity.getItems().add(itemEntity);
            }
            itemEntity.setVariantId(item.variantId());
            itemEntity.setQuantity(item.quantity());
        }
    }

    private static CartItemEntity toEntity(CartItem item, CartEntity cart) {
        CartItemEntity entity = new CartItemEntity();
        entity.setId(UUID.randomUUID());
        entity.setCart(cart);
        entity.setVariantId(item.variantId());
        entity.setQuantity(item.quantity());
        return entity;
    }

    private static CartItem toDomain(CartItemEntity entity) {
        return new CartItem(entity.getVariantId(), entity.getQuantity());
    }
}
