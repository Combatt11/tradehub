package com.nexusmarket.domain.models;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void plusAddsAmountsWithSameCurrency() {
        Money first = Money.of("10.50", "USD");
        Money second = Money.of("2.25", "USD");

        assertEquals(Money.of("12.75", "USD"), first.plus(second));
    }

    @Test
    void plusRejectsDifferentCurrencies() {
        Money usd = Money.of("10.00", "USD");
        Money eur = Money.of("10.00", "EUR");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> usd.plus(eur)
        );

        assertEquals("Currency mismatch: USD vs EUR", exception.getMessage());
    }

    @Test
    void multiplyByQuantityScalesAmount() {
        Money unitPrice = Money.of("19.99", "USD");

        assertEquals(Money.of("59.97", "USD"), unitPrice.multiply(3));
    }

    @Test
    void multiplyByBigDecimalScalesAmount() {
        Money amount = Money.of("10.00", "USD");

        assertEquals(Money.of("25.00", "USD"), amount.multiply(new BigDecimal("2.5")));
    }

    @Test
    void constructorRoundsHalfUpToCurrencyScale() {
        assertEquals(Money.of("10.01", "USD"), Money.of("10.005", "USD"));
    }

    @Test
    void divideCalculatesAverageWithCurrencyScale() {
        Money total = Money.of("100.00", "USD");

        assertEquals(Money.of("33.33", "USD"), total.divide(3));
    }

    @Test
    void divideRejectsNonPositiveDivisor() {
        Money total = Money.of("100.00", "USD");

        assertThrows(IllegalArgumentException.class, () -> total.divide(0));
    }

    @Test
    void zeroUsesTheCurrencyFractionDigits() {
        assertEquals(new BigDecimal("0.00"), Money.zero("USD").amount());
        assertEquals(new BigDecimal("0"), Money.zero("JPY").amount());
    }

    @Test
    void requirePositiveRejectsZeroAndNegativeAmounts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Money.of("0.00", "USD").requirePositive("amount")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> Money.of("-1.00", "USD").requirePositive("amount")
        );
    }
}
