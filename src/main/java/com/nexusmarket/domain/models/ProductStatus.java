package com.nexusmarket.domain.models;

/**
 * Estados de publicacion de un producto.
 */
public enum ProductStatus {
    DRAFT,
    PUBLISHED,
    SUSPENDED,
    DISCONTINUED;

    public boolean isPurchasable() {
        return this == PUBLISHED;
    }
}
