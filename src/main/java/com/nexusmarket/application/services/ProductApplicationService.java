package com.nexusmarket.application.services;

import com.nexusmarket.domain.models.Product;
import com.nexusmarket.domain.models.ProductStatus;
import com.nexusmarket.domain.models.ProductType;
import com.nexusmarket.domain.models.Variant;
import com.nexusmarket.domain.ports.in.ManageProductUseCase;
import com.nexusmarket.domain.ports.out.ProductRepositoryPort;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

/**
 * Servicio de aplicacion que implementa la gestion de productos.
 *
 * <p>Vive fuera del dominio para mantenerlo libre de infraestructura. La
 * logica de estado y validacion permanece en la entidad {@link Product}.</p>
 */
public final class ProductApplicationService implements ManageProductUseCase {

    private final ProductRepositoryPort productRepository;

    public ProductApplicationService(ProductRepositoryPort productRepository) {
        this.productRepository = Objects.requireNonNull(
                productRepository, "productRepository must not be null");
    }

    @Override
    public Product createProduct(
            UUID sellerId,
            String name,
            String description,
            ProductType type,
            List<Variant> variants
    ) {
        Objects.requireNonNull(sellerId, "sellerId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(variants, "variants must not be null");

        Product product = new Product(
                UUID.randomUUID(),
                sellerId,
                name,
                description,
                type,
                ProductStatus.DRAFT,
                variants
        );

        return productRepository.save(product);
    }

    @Override
    public Product publishProduct(UUID productId) {
        Objects.requireNonNull(productId, "productId must not be null");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Product not found: " + productId));

        product.publish();
        return productRepository.save(product);
    }
}
