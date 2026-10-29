package com.nexusmarket.domain.models;

/**
 * Tipo comercial de producto.
 */
public enum ProductType {
    PHYSICAL,
    DIGITAL;

    public boolean requiresShipping() {
        return this == PHYSICAL;
    }
}
