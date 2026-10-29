package com.nexusmarket.domain.ports.in;

import com.nexusmarket.domain.models.Product;
import com.nexusmarket.domain.models.ProductType;
import com.nexusmarket.domain.models.Variant;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada para la gestion de productos.
 */
public interface ManageProductUseCase {

    /**
     * Crea un producto en estado DRAFT.
     */
    Product createProduct(
            UUID sellerId,
            String name,
            String description,
            ProductType type,
            List<Variant> variants
    );

    /**
     * Publica un producto existente.
     *
     * @throws java.util.NoSuchElementException si el producto no existe
     * @throws IllegalStateException             si no puede publicarse
     */
    Product publishProduct(UUID productId);
}
