package com.nexusmarket.domain.models;

import java.util.Locale;
import java.util.Objects;

/**
 * Snapshot inmutable de una direccion.
 *
 * <p>Se usa como value object en el pedido para no depender de la libreta de
 * direcciones del comprador (RN-DIR-02).</p>
 */
public record Address(
        String recipientName,
        String phone,
        String street,
        String number,
        String unit,
        String complement,
        String city,
        String state,
        String postalCode,
        String country,
        String deliveryInstructions
) {

    public Address {
        recipientName = required(recipientName, "recipientName", 150);
        phone = required(phone, "phone", 30);
        street = required(street, "street", 200);
        number = required(number, "number", 20);
        unit = optional(unit, "unit", 20);
        complement = optional(complement, "complement", 200);
        city = required(city, "city", 100);
        state = optional(state, "state", 100);
        postalCode = required(postalCode, "postalCode", 20);
        country = required(country, "country", 2).toUpperCase(Locale.ROOT);
        deliveryInstructions = optional(deliveryInstructions, "deliveryInstructions", 500);

        if (!country.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("country must be an ISO 3166-1 alpha-2 code");
        }
    }

    /**
     * Representacion legible de la via, sin datos sensibles adicionales.
     */
    public String formattedStreet() {
        StringBuilder result = new StringBuilder(street).append(' ').append(number);
        if (unit != null) {
            result.append(", ").append(unit);
        }
        if (complement != null) {
            result.append(", ").append(complement);
        }
        return result.toString();
    }

    private static String required(String value, String fieldName, int maxLength) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " exceeds max length " + maxLength);
        }
        return trimmed;
    }

    private static String optional(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " exceeds max length " + maxLength);
        }
        return trimmed;
    }
}
