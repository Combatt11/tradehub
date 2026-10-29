package com.nexusmarket.domain.models;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Producto: agregado comercial y de contenido que agrupa variantes/SKU.
 */
public final class Product {

    private final UUID id;
    private final UUID sellerId;
    private final String name;
    private final String description;
    private final ProductType type;
    private final List<Variant> variants;
    private ProductStatus status;

    public Product(
            UUID id,
            UUID sellerId,
            String name,
            String description,
            ProductType type,
            ProductStatus status,
            List<Variant> variants
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.sellerId = Objects.requireNonNull(sellerId, "sellerId must not be null");
        this.name = required(name, "name", 200);
        this.description = optional(description, "description", 5000);
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.variants = new ArrayList<>(Objects.requireNonNull(variants, "variants must not be null"));

        validateUniqueVariants();
    }

    public static Product createDraft(
            UUID id,
            UUID sellerId,
            String name,
            String description,
            ProductType type
    ) {
        return new Product(id, sellerId, name, description, type, ProductStatus.DRAFT, List.of());
    }

    public void addVariant(Variant variant) {
        Objects.requireNonNull(variant, "variant must not be null");
        ensureNotDiscontinued();

        if (variants.stream().anyMatch(existing -> existing.id().equals(variant.id()))) {
            throw new IllegalArgumentException("Variant id already exists: " + variant.id());
        }
        if (variants.stream().anyMatch(existing -> existing.sku().equalsIgnoreCase(variant.sku()))) {
            throw new IllegalArgumentException("Variant sku already exists: " + variant.sku());
        }

        variants.add(variant);
    }

    public void removeVariant(UUID variantId) {
        Objects.requireNonNull(variantId, "variantId must not be null");
        ensureNotDiscontinued();

        boolean removed = variants.removeIf(variant -> variant.id().equals(variantId));
        if (!removed) {
            throw new IllegalArgumentException("Variant not found: " + variantId);
        }
    }

    /**
     * Precio "desde" derivado de las variantes publicadas.
     */
    public Money displayPriceFrom() {
        return variants.stream()
                .map(Variant::price)
                .min(Comparator.naturalOrder())
                .orElseThrow(() -> new IllegalStateException("Product has no variants"));
    }

    public void publish() {
        if (variants.isEmpty()) {
            throw new IllegalStateException("A product needs at least one variant to be published");
        }
        ensureNotDiscontinued();
        this.status = ProductStatus.PUBLISHED;
    }

    public void suspend() {
        ensureNotDiscontinued();
        this.status = ProductStatus.SUSPENDED;
    }

    public void discontinue() {
        this.status = ProductStatus.DISCONTINUED;
    }

    public boolean isPurchasable() {
        return status.isPurchasable();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ProductType getType() {
        return type;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public List<Variant> getVariants() {
        return List.copyOf(variants);
    }

    private void validateUniqueVariants() {
        Set<UUID> ids = new HashSet<>();
        Set<String> skus = new HashSet<>();

        for (Variant variant : variants) {
            if (variant == null) {
                throw new IllegalArgumentException("variants must not contain null");
            }
            if (!ids.add(variant.id())) {
                throw new IllegalArgumentException("Duplicate variant id: " + variant.id());
            }
            if (!skus.add(variant.sku().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Duplicate variant sku: " + variant.sku());
            }
        }
    }

    private void ensureNotDiscontinued() {
        if (status == ProductStatus.DISCONTINUED) {
            throw new IllegalStateException("A discontinued product cannot be modified");
        }
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
