package com.nexusmarket.domain.models;

import java.util.Locale;
import java.util.Objects;

/**
 * Value object que representa la identidad documental de un usuario.
 *
 * <p>La unicidad real se valida en persistencia mediante un hash
 * (RN-USR-01); este objeto solo conserva los datos necesarios para el dominio.</p>
 */
public record DocumentId(IdentityDocumentType type, String number, String country) {

    public DocumentId {
        Objects.requireNonNull(type, "type must not be null");
        number = required(number, "number", 64);
        country = required(country, "country", 2).toUpperCase(Locale.ROOT);

        if (!country.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("country must be an ISO 3166-1 alpha-2 code");
        }
    }

    /**
     * Devuelve el numero enmascarado para logs o vistas no privilegiadas.
     */
    public String maskedNumber() {
        if (number.length() <= 4) {
            return "*".repeat(number.length());
        }
        return "*".repeat(number.length() - 4) + number.substring(number.length() - 4);
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
