package com.nexusmarket.adapters.persistence.jpa;

import com.nexusmarket.domain.models.Money;
import com.nexusmarket.domain.models.Product;
import com.nexusmarket.domain.models.Variant;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Traduccion entre {@link Product} y {@link ProductEntity}.
 */
public final class ProductMapper {

    private ProductMapper() {
    }

    public static Product toDomain(ProductEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");

        List<Variant> variants = entity.getVariants().stream()
                .map(ProductMapper::toDomain)
                .toList();

        return new Product(
                entity.getId(),
                entity.getSellerId(),
                entity.getName(),
                entity.getDescription(),
                entity.getType(),
                entity.getStatus(),
                variants
        );
    }

    public static ProductEntity toEntity(Product product) {
        Objects.requireNonNull(product, "product must not be null");

        ProductEntity entity = new ProductEntity();
        entity.setId(product.getId());
        copyScalars(entity, product);

        for (Variant variant : product.getVariants()) {
            entity.getVariants().add(toEntity(variant, entity));
        }

        return entity;
    }

    /**
     * Actualiza una entidad ya gestionada conservando los identificadores de
     * las variantes existentes.
     */
    public static void updateEntity(ProductEntity entity, Product product) {
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(product, "product must not be null");

        copyScalars(entity, product);

        Map<UUID, VariantEntity> existingById = entity.getVariants().stream()
                .collect(Collectors.toMap(
                        VariantEntity::getId,
                        Function.identity(),
                        (first, ignored) -> first
                ));

        Set<UUID> domainVariantIds = product.getVariants().stream()
                .map(Variant::id)
                .collect(Collectors.toSet());

        entity.getVariants().removeIf(variant -> !domainVariantIds.contains(variant.getId()));

        for (Variant variant : product.getVariants()) {
            VariantEntity variantEntity = existingById.get(variant.id());
            if (variantEntity == null) {
                variantEntity = new VariantEntity();
                variantEntity.setProduct(entity);
                entity.getVariants().add(variantEntity);
            }
            variantEntity.setId(variant.id());
            variantEntity.setSku(variant.sku());
            variantEntity.setPriceAmount(variant.price().amount());
            variantEntity.setPriceCurrency(variant.price().currency().getCurrencyCode());
            variantEntity.getAttributes().clear();
            variantEntity.getAttributes().putAll(variant.attributes());
        }
    }

    private static void copyScalars(ProductEntity entity, Product product) {
        entity.setSellerId(product.getSellerId());
        entity.setName(product.getName());
        entity.setDescription(product.getDescription());
        entity.setType(product.getType());
        entity.setStatus(product.getStatus());
    }

    private static VariantEntity toEntity(Variant variant, ProductEntity product) {
        VariantEntity entity = new VariantEntity();
        entity.setProduct(product);
        entity.setId(variant.id());
        entity.setSku(variant.sku());
        entity.setPriceAmount(variant.price().amount());
        entity.setPriceCurrency(variant.price().currency().getCurrencyCode());
        entity.getAttributes().putAll(variant.attributes());
        return entity;
    }

    private static Variant toDomain(VariantEntity entity) {
        return new Variant(
                entity.getId(),
                entity.getSku(),
                toMoney(entity.getPriceAmount(), entity.getPriceCurrency()),
                entity.getAttributes()
        );
    }

    private static Money toMoney(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }
}
