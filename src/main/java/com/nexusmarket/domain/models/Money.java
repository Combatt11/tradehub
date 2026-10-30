package com.nexusmarket.domain.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value object inmutable para importes monetarios.
 *
 * <p>Normaliza el importe a la escala de la moneda y aplica redondeo half-up,
 * tal como exige RN-GEN-03. No permite mezclar monedas en operaciones
 * aritméticas.</p>
 */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");

        int scale = Math.max(currency.getDefaultFractionDigits(), 0);
        amount = amount.setScale(scale, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public static Money zero(String currencyCode) {
        return zero(Currency.getInstance(currencyCode));
    }

    public static Money zero(Currency currency) {
        Objects.requireNonNull(currency, "currency must not be null");
        int scale = Math.max(currency.getDefaultFractionDigits(), 0);
        return new Money(BigDecimal.ZERO.setScale(scale), currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money multiply(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must not be negative");
        }
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
    }

    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor must not be null");
        return new Money(amount.multiply(factor), currency);
    }

    public Money divide(long divisor) {
        if (divisor <= 0) {
            throw new IllegalArgumentException("divisor must be greater than zero");
        }
        int scale = Math.max(currency.getDefaultFractionDigits(), 0);
        return new Money(
                amount.divide(BigDecimal.valueOf(divisor), scale, RoundingMode.HALF_UP),
                currency
        );
    }

    public Money requirePositive(String fieldName) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        return this;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other money must not be null");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: " + currency.getCurrencyCode()
                            + " vs " + other.currency.getCurrencyCode());
        }
    }
}
