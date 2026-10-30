package com.nexusmarket.adapters.persistence.jpa;

import com.nexusmarket.domain.models.Money;
import com.nexusmarket.domain.models.Order;
import com.nexusmarket.domain.models.OrderLine;

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
 * Traduccion entre {@link Order} y {@link OrderEntity}.
 */
public final class OrderMapper {

    private OrderMapper() {
    }

    public static Order toDomain(OrderEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");

        List<OrderLine> lines = entity.getLines().stream()
                .map(OrderMapper::toDomain)
                .toList();

        return new Order(
                entity.getId(),
                entity.getBuyerId(),
                entity.getSellerId(),
                lines,
                toMoney(entity.getTotalAmount(), entity.getCurrency()),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    public static OrderEntity toEntity(Order order) {
        Objects.requireNonNull(order, "order must not be null");

        OrderEntity entity = new OrderEntity();
        entity.setId(order.getId());
        copyScalars(entity, order);

        for (OrderLine line : order.getLines()) {
            entity.getLines().add(toEntity(line, entity));
        }

        return entity;
    }

    /**
     * Actualiza una entidad ya gestionada conservando los identificadores de
     * las lineas existentes.
     */
    public static void updateEntity(OrderEntity entity, Order order) {
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(order, "order must not be null");

        copyScalars(entity, order);

        Map<UUID, OrderLineEntity> existingByVariant = entity.getLines().stream()
                .collect(Collectors.toMap(
                        OrderLineEntity::getVariantId,
                        Function.identity(),
                        (first, ignored) -> first
                ));

        Set<UUID> domainVariantIds = order.getLines().stream()
                .map(OrderLine::variantId)
                .collect(Collectors.toSet());

        entity.getLines().removeIf(line -> !domainVariantIds.contains(line.getVariantId()));

        for (OrderLine line : order.getLines()) {
            OrderLineEntity lineEntity = existingByVariant.get(line.variantId());
            if (lineEntity == null) {
                lineEntity = new OrderLineEntity();
                lineEntity.setOrder(entity);
                entity.getLines().add(lineEntity);
            }
            lineEntity.setVariantId(line.variantId());
            lineEntity.setQuantity(line.quantity());
            lineEntity.setUnitPriceAmount(line.unitPrice().amount());
            lineEntity.setUnitPriceCurrency(line.unitPrice().currency().getCurrencyCode());
        }
    }

    private static void copyScalars(OrderEntity entity, Order order) {
        entity.setBuyerId(order.getBuyerId());
        entity.setSellerId(order.getSellerId());
        entity.setTotalAmount(order.getTotalAmount().amount());
        entity.setCurrency(order.getTotalAmount().currency().getCurrencyCode());
        entity.setStatus(order.getStatus());
        entity.setCreatedAt(order.getCreatedAt());
    }

    private static OrderLineEntity toEntity(OrderLine line, OrderEntity order) {
        OrderLineEntity entity = new OrderLineEntity();
        entity.setOrder(order);
        entity.setVariantId(line.variantId());
        entity.setQuantity(line.quantity());
        entity.setUnitPriceAmount(line.unitPrice().amount());
        entity.setUnitPriceCurrency(line.unitPrice().currency().getCurrencyCode());
        return entity;
    }

    private static OrderLine toDomain(OrderLineEntity entity) {
        return new OrderLine(
                entity.getVariantId(),
                entity.getQuantity(),
                toMoney(entity.getUnitPriceAmount(), entity.getUnitPriceCurrency())
        );
    }

    private static Money toMoney(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }
}
