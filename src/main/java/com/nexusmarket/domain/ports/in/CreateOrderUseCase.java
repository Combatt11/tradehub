package com.nexusmarket.domain.ports.in;

import com.nexusmarket.domain.models.Order;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Puerto de entrada para crear una orden a partir de las variantes y cantidades
 * de un carrito previamente validado.
 *
 * <p>Los precios no forman parte de la entrada: se resuelven en el servidor a
 * partir del catalogo de variantes.</p>
 */
public interface CreateOrderUseCase {

    /**
     * Variante y cantidad solicitadas por el cliente.
     */
    record OrderLineInput(UUID variantId, int quantity) {

        public OrderLineInput {
            Objects.requireNonNull(variantId, "variantId must not be null");
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be greater than zero");
            }
        }
    }

    /**
     * Crea una orden en estado PENDING_PAYMENT y calcula su total con los
     * precios vigentes en el servidor.
     *
     * @param buyerId  comprador propietario de la compra
     * @param sellerId vendedor al que pertenece la orden
     * @param lines    variantes y cantidades del carrito
     * @return orden persistida
     */
    Order createOrder(UUID buyerId, UUID sellerId, List<OrderLineInput> lines);
}
