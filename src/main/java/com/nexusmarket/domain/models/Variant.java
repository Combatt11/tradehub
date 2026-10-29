package com.nexusmarket.domain.models;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Variante/SKU: unidad vendible e inventariable de un producto.
 *
 * <p>El precio autoritativo vive aqui, no en el producto (DEB-01).</p>
 */
public record Variant(
        UUID id,
        String sku,
        Money price,
        Map<String, String> attributes
) {

    public Variant {
        Objects.requireNonNull(id, "id must not be null");
        sku = required(sku, "sku", 64);
        Objects.requireNonNull(price, "price must not be null").requirePositive("price");
        attributes = immutableAttributes(attributes);
    }

    public Variant(UUID id, String sku, Money price) {
        this(id, sku, price, Map.of());
    }

    private static Map<String, String> immutableAttributes(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Map.of();
        }

        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = required(entry.getKey(), "attribute key", 64);
            String value = required(entry.getValue(), "attribute value", 255);
            if (copy.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("Duplicate attribute key: " + key);
            }
        }
        return Collections.unmodifiableMap(copy);
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
}
