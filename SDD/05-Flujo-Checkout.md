# 05 - Flujo de Checkout y Pago

## Propósito

Este documento describe el flujo de compra de NexusMarket. El checkout crea una orden por vendedor y reserva el inventario de cada línea. Si una reserva falla, se ejecuta una compensación manual: se liberan las reservas ya realizadas y se cancelan las órdenes ya creadas.

> **Nota de implementación:** en el código actual el checkout y el pago son endpoints separados. `POST /api/checkout` crea las órdenes y reserva stock; `POST /api/orders/{id}/pay` procesa el cobro posteriormente. El diagrama muestra ambas fases para cubrir el flujo completo.

## Diagrama de Secuencia

```mermaid
sequenceDiagram
    autonumber
    actor Buyer
    participant CartController
    participant CheckoutController
    participant CheckoutService as CheckoutApplicationService
    participant OrderService as OrderDomainService
    participant InventoryService as InventoryDomainService
    participant VariantRepo as VariantRepositoryPort
    participant OrderRepo as OrderRepositoryPort
    participant InventoryRepo as InventoryRepositoryPort
    participant CartRepo as ShoppingCartRepositoryPort
    participant PSP as PaymentGatewayPort
    participant OrderController

    Note over Buyer,CartController: Fase 1 - Carrito (opcional)
    Buyer->>CartController: POST /api/cart/items
    CartController->>CartRepo: addToCart(buyerId, variantId, quantity)
    CartRepo-->>CartController: ShoppingCart
    CartController-->>Buyer: 200 OK

    Note over Buyer,CheckoutController: Fase 2 - Checkout
    Buyer->>CheckoutController: POST /api/checkout
    CheckoutController->>CheckoutService: checkout(buyerId)
    CheckoutService->>CartRepo: findByBuyerId(buyerId)
    CartRepo-->>CheckoutService: ShoppingCart

    alt Carrito inexistente o vacío
        CheckoutService-->>CheckoutController: IllegalStateException
        CheckoutController-->>Buyer: 409 Conflict
    else Carrito con ítems
        loop Por cada vendedor
            CheckoutService->>VariantRepo: findSellerIdByVariantId(variantId)
            VariantRepo-->>CheckoutService: sellerId
            CheckoutService->>OrderService: createOrder(buyerId, sellerId, lines)
            OrderService->>VariantRepo: findByIdAndSellerId(variantId, sellerId)
            VariantRepo-->>OrderService: Variant
            OrderService->>OrderRepo: save(order)
            OrderRepo-->>OrderService: Order
            OrderService-->>CheckoutService: Order

            CheckoutService->>InventoryService: reserveStock(variantId, warehouseId, quantity)
            InventoryService->>InventoryRepo: findByVariantAndWarehouse(...)
            InventoryRepo-->>InventoryService: InventoryItem

            alt Stock suficiente
                InventoryService->>InventoryService: item.reserve(quantity)
                InventoryService->>InventoryRepo: save(item)
                InventoryRepo-->>InventoryService: InventoryItem
                InventoryService-->>CheckoutService: InventoryItem
            else Stock insuficiente
                InventoryService-->>CheckoutService: InsufficientStockException
                Note over CheckoutService,OrderRepo: Rollback manual de la transacción
                CheckoutService->>InventoryRepo: liberar reservas previas
                InventoryRepo-->>CheckoutService: OK
                CheckoutService->>OrderRepo: findById(orderId)
                OrderRepo-->>CheckoutService: Order
                CheckoutService->>CheckoutService: order.cancel()
                CheckoutService->>OrderRepo: save(order)
                CheckoutService-->>CheckoutController: relanza InsufficientStockException
                CheckoutController-->>Buyer: 409 Conflict
            end
        end

        CheckoutService->>CartRepo: deleteByBuyerId(buyerId)
        CheckoutService-->>CheckoutController: orderIds
        CheckoutController-->>Buyer: 201 Created
    end

    Note over Buyer,PSP: Fase 3 - Pago (endpoint separado)
    Buyer->>OrderController: POST /api/orders/{id}/pay
    OrderController->>OrderService: processPayment(orderId, buyerId)
    OrderService->>OrderRepo: findById(orderId)
    OrderRepo-->>OrderService: Order

    alt Orden inexistente
        OrderService-->>OrderController: NoSuchElementException
        OrderController-->>Buyer: 404 Not Found
    else Orden de otro comprador
        OrderService-->>OrderController: UnauthorizedOperationException
        OrderController-->>Buyer: 403 Forbidden
    else Orden en estado distinto de PENDING_PAYMENT
        OrderService-->>OrderController: InvalidOrderStateException
        OrderController-->>Buyer: 409 Conflict
    else Orden propia en PENDING_PAYMENT
        OrderService->>PSP: charge(orderId, totalAmount)
        PSP-->>OrderService: PaymentResult
        alt Pago aprobado
            OrderService->>OrderService: order.markAsPaid()
            OrderService->>OrderRepo: save(order)
            OrderService-->>OrderController: PaymentResult(success)
            OrderController-->>Buyer: 200 OK
        else Pago rechazado
            OrderService->>OrderService: order.markPaymentFailed()
            OrderService->>OrderRepo: save(order)
            OrderService-->>OrderController: PaymentResult(declined)
            OrderController-->>Buyer: 200 OK (failureReason)
        end
    end
```

## Puntos importantes

1. **Una orden por vendedor:** el carrito se agrupa por `sellerId`; cada grupo genera una orden independiente.
2. **Precio resuelto en el servidor:** el cliente nunca envía precios; `OrderDomainService` obtiene el precio autoritativo desde `VariantRepositoryPort`.
3. **Reserva y compensación:** cada reserva exitosa se registra en memoria. Si falla una reserva posterior, se liberan las anteriores y se cancelan las órdenes creadas.
4. **Carrito vacío:** el checkout lanza `IllegalStateException`, que se traduce a `409 Conflict`.
5. **Pago separado:** `POST /api/orders/{id}/pay` valida que la orden pertenezca al comprador autenticado antes de llamar a la pasarela.
6. **Pago rechazado:** la orden pasa a `PAYMENT_FAILED` y se persiste; la respuesta HTTP sigue siendo `200 OK` con `successful=false` y `failureReason`.
