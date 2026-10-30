package com.nexusmarket.domain.models;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShoppingCartTest {

    private final UUID cartId = UUID.randomUUID();
    private final UUID buyerId = UUID.randomUUID();

    @Test
    void addItemAccumulatesQuantityForSameVariant() {
        UUID variantId = UUID.randomUUID();
        ShoppingCart cart = ShoppingCart.create(cartId, buyerId);

        cart.addItem(variantId, 2);
        cart.addItem(variantId, 3);

        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.getItems().get(0).quantity());
        assertEquals(5, cart.totalQuantity());
    }

    @Test
    void addItemRejectsMoreThan99UnitsOnSingleLine() {
        UUID variantId = UUID.randomUUID();
        ShoppingCart cart = ShoppingCart.create(cartId, buyerId);
        cart.addItem(variantId, CartItem.MAX_QUANTITY_PER_LINE);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cart.addItem(variantId, 1)
        );

        assertTrue(exception.getMessage().contains("99"));
        assertEquals(CartItem.MAX_QUANTITY_PER_LINE, cart.getItems().get(0).quantity());
    }

    @Test
    void addItemRejectsQuantityAboveMaximumOnFirstAdd() {
        ShoppingCart cart = ShoppingCart.create(cartId, buyerId);

        assertThrows(
                IllegalArgumentException.class,
                () -> cart.addItem(UUID.randomUUID(), CartItem.MAX_QUANTITY_PER_LINE + 1)
        );

        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void removeItemDeletesVariantFromCart() {
        UUID firstVariantId = UUID.randomUUID();
        UUID secondVariantId = UUID.randomUUID();
        ShoppingCart cart = ShoppingCart.create(cartId, buyerId);
        cart.addItem(firstVariantId, 1);
        cart.addItem(secondVariantId, 2);

        cart.removeItem(firstVariantId);

        assertEquals(1, cart.getItems().size());
        assertEquals(secondVariantId, cart.getItems().get(0).variantId());
    }

    @Test
    void removeItemThrowsWhenVariantIsNotInCart() {
        ShoppingCart cart = ShoppingCart.create(cartId, buyerId);

        assertThrows(
                IllegalArgumentException.class,
                () -> cart.removeItem(UUID.randomUUID())
        );
    }

    @Test
    void calculateTotalUsesPriceResolverAndLineQuantities() {
        UUID firstVariantId = UUID.randomUUID();
        UUID secondVariantId = UUID.randomUUID();
        ShoppingCart cart = ShoppingCart.create(cartId, buyerId);
        cart.addItem(firstVariantId, 2);
        cart.addItem(secondVariantId, 3);

        Map<UUID, Money> prices = Map.of(
                firstVariantId, Money.of("10.50", "USD"),
                secondVariantId, Money.of("2.00", "USD")
        );

        Money total = cart.calculateTotal(prices::get);

        assertEquals(Money.of("27.00", "USD"), total);
    }

    @Test
    void calculateTotalThrowsWhenCartIsEmpty() {
        ShoppingCart cart = ShoppingCart.create(cartId, buyerId);

        assertThrows(
                IllegalStateException.class,
                () -> cart.calculateTotal(variantId -> Money.of("1.00", "USD"))
        );
    }
}
