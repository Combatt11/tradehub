package com.nexusmarket.domain.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Carrito de compras de un comprador.
 *
 * <p>El carrito es un agregado previo al pedido: no reserva inventario ni
 * congela precios. Cada comprador tiene como maximo un carrito activo.</p>
 */
public final class ShoppingCart {

    private final UUID id;
    private final UUID buyerId;
    private final List<CartItem> items;

    public ShoppingCart(UUID id, UUID buyerId, List<CartItem> items) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.buyerId = Objects.requireNonNull(buyerId, "buyerId must not be null");
        this.items = new ArrayList<>(Objects.requireNonNull(items, "items must not be null"));

        validateItems(this.items);
    }

    public static ShoppingCart create(UUID id, UUID buyerId) {
        return new ShoppingCart(id, buyerId, List.of());
    }

    /**
     * Añade una variante. Si ya existe, suma la cantidad respetando el maximo
     * por linea.
     */
    public void addItem(UUID variantId, int quantity) {
        Objects.requireNonNull(variantId, "variantId must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }

        int currentIndex = indexOf(variantId);
        int currentQuantity = currentIndex >= 0 ? items.get(currentIndex).quantity() : 0;
        int newQuantity = Math.addExact(currentQuantity, quantity);

        if (newQuantity > CartItem.MAX_QUANTITY_PER_LINE) {
            throw new IllegalArgumentException(
                    "quantity must not exceed " + CartItem.MAX_QUANTITY_PER_LINE);
        }

        if (currentIndex >= 0) {
            items.set(currentIndex, new CartItem(variantId, newQuantity));
        } else {
            items.add(new CartItem(variantId, newQuantity));
        }
    }

    /**
     * Elimina una variante del carrito.
     */
    public void removeItem(UUID variantId) {
        Objects.requireNonNull(variantId, "variantId must not be null");

        boolean removed = items.removeIf(item -> item.variantId().equals(variantId));
        if (!removed) {
            throw new IllegalArgumentException("Variant not found in cart: " + variantId);
        }
    }

    public void clear() {
        items.clear();
    }

    /**
     * Calcula el total usando el precio actual de cada variante.
     *
     * <p>El resolver se inyecta desde la capa que conoce el catalogo, de modo
     * que el dominio del carrito no dependa de persistencia.</p>
     */
    public Money calculateTotal(Function<UUID, Money> priceResolver) {
        Objects.requireNonNull(priceResolver, "priceResolver must not be null");

        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot calculate the total of an empty cart");
        }

        Money total = null;
        for (CartItem item : items) {
            Money unitPrice = Objects.requireNonNull(
                    priceResolver.apply(item.variantId()),
                    "priceResolver must not return null");
            Money lineTotal = unitPrice.multiply(item.quantity());
            total = total == null ? lineTotal : total.plus(lineTotal);
        }

        return total;
    }

    public int totalQuantity() {
        return items.stream().mapToInt(CartItem::quantity).sum();
    }

    public UUID getId() {
        return id;
    }

    public UUID getBuyerId() {
        return buyerId;
    }

    public List<CartItem> getItems() {
        return List.copyOf(items);
    }

    private int indexOf(UUID variantId) {
        for (int index = 0; index < items.size(); index++) {
            if (items.get(index).variantId().equals(variantId)) {
                return index;
            }
        }
        return -1;
    }

    private static void validateItems(List<CartItem> items) {
        Set<UUID> variantIds = new HashSet<>();

        for (CartItem item : items) {
            Objects.requireNonNull(item, "items must not contain null elements");
            if (!variantIds.add(item.variantId())) {
                throw new IllegalArgumentException(
                        "Duplicate variant in cart: " + item.variantId());
            }
        }
    }
}
