package com.nexusmarket.adapters.rest.mappers;

import com.nexusmarket.adapters.rest.dto.InventoryResponseDTO;
import com.nexusmarket.adapters.rest.dto.OrderLineRequestDTO;
import com.nexusmarket.adapters.rest.dto.OrderLineResponseDTO;
import com.nexusmarket.adapters.rest.dto.OrderResponseDTO;
import com.nexusmarket.adapters.rest.dto.PaymentResponseDTO;
import com.nexusmarket.adapters.rest.dto.ProductRequestDTO;
import com.nexusmarket.adapters.rest.dto.ProductResponseDTO;
import com.nexusmarket.adapters.rest.dto.VariantRequestDTO;
import com.nexusmarket.adapters.rest.dto.VariantResponseDTO;
import com.nexusmarket.domain.models.InventoryItem;
import com.nexusmarket.domain.models.Money;
import com.nexusmarket.domain.models.Order;
import com.nexusmarket.domain.models.Product;
import com.nexusmarket.domain.models.ProductStatus;
import com.nexusmarket.domain.models.Variant;
import com.nexusmarket.domain.ports.in.CreateOrderUseCase.OrderLineInput;
import com.nexusmarket.domain.ports.out.PaymentGatewayPort.PaymentResult;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Traduccion entre DTOs REST y modelos de dominio.
 */
public final class RestMapper {

    private RestMapper() {
    }

    public static List<OrderLineInput> toOrderLineInputs(List<OrderLineRequestDTO> lines) {
        Objects.requireNonNull(lines, "lines must not be null");

        return lines.stream()
                .map(line -> new OrderLineInput(line.variantId(), line.quantity()))
                .toList();
    }

    public static OrderResponseDTO toOrderResponse(Order order) {
        Objects.requireNonNull(order, "order must not be null");

        List<OrderLineResponseDTO> lines = order.getLines().stream()
                .map(line -> new OrderLineResponseDTO(
                        line.variantId(),
                        line.quantity(),
                        line.unitPrice().amount(),
                        line.unitPrice().currency().getCurrencyCode(),
                        line.lineTotal().amount()
                ))
                .toList();

        return new OrderResponseDTO(
                order.getId(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getTotalAmount().amount(),
                order.getTotalAmount().currency().getCurrencyCode(),
                order.getStatus(),
                lines
        );
    }

    public static PaymentResponseDTO toPaymentResponse(PaymentResult result) {
        Objects.requireNonNull(result, "result must not be null");

        return new PaymentResponseDTO(
                result.successful(),
                result.transactionId(),
                result.failureReason()
        );
    }

    public static List<Variant> toVariants(List<VariantRequestDTO> variants) {
        Objects.requireNonNull(variants, "variants must not be null");

        return variants.stream()
                .map(variant -> new Variant(
                        variant.id() != null ? variant.id() : UUID.randomUUID(),
                        variant.sku(),
                        Money.of(variant.price(), variant.currency()),
                        variant.attributes()
                ))
                .toList();
    }

    /**
     * Convierte la peticion en un producto borrador listo para persistir.
     */
    public static Product toProduct(ProductRequestDTO request) {
        Objects.requireNonNull(request, "request must not be null");

        return new Product(
                UUID.randomUUID(),
                request.sellerId(),
                request.name(),
                request.description(),
                request.type(),
                ProductStatus.DRAFT,
                toVariants(request.variants())
        );
    }

    public static ProductResponseDTO toProductResponse(Product product) {
        Objects.requireNonNull(product, "product must not be null");

        List<VariantResponseDTO> variants = product.getVariants().stream()
                .map(variant -> new VariantResponseDTO(
                        variant.id(),
                        variant.sku(),
                        variant.price().amount(),
                        variant.price().currency().getCurrencyCode(),
                        variant.attributes()
                ))
                .toList();

        return new ProductResponseDTO(
                product.getId(),
                product.getSellerId(),
                product.getName(),
                product.getDescription(),
                product.getType(),
                product.getStatus(),
                variants
        );
    }

    public static InventoryResponseDTO toInventoryResponse(InventoryItem item) {
        Objects.requireNonNull(item, "item must not be null");

        return new InventoryResponseDTO(
                item.getVariantId(),
                item.getWarehouseId(),
                item.getAvailableStock(),
                item.getReservedStock()
        );
    }
}
