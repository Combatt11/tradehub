# 04 - Modelo de Dominio

## Propósito

Este documento presenta las entidades y objetos de valor principales del dominio de NexusMarket, junto con sus relaciones. El modelo es deliberadamente rico: las reglas de negocio viven en los propios modelos y servicios de dominio, no en los adaptadores.

## Diagrama de Clases

```mermaid
classDiagram
    class User {
        +UUID id
        +DocumentId documentId
        +String email
        +UserRole role
        +UserStatus status
        +activate() void
        +block() void
        +deactivate() void
        +isActive() boolean
    }

    class Product {
        +UUID id
        +UUID sellerId
        +String name
        +String description
        +ProductType type
        +ProductStatus status
        +List~Variant~ variants
        +addVariant(Variant variant) void
        +removeVariant(UUID variantId) void
        +publish() void
        +suspend() void
        +discontinue() void
        +displayPriceFrom() Money
    }

    class Variant {
        <<record>>
        +UUID id
        +String sku
        +Money price
        +Map~String,String~ attributes
    }

    class InventoryItem {
        +UUID variantId
        +UUID warehouseId
        +int availableStock
        +int reservedStock
        +reserve(int quantity) void
        +confirmReservation(int quantity) void
        +releaseReservation(int quantity) void
        +addStock(int quantity) void
        +restock(int quantity) void
        +onHandStock() int
    }

    class Order {
        +UUID id
        +UUID buyerId
        +UUID sellerId
        +List~OrderLine~ lines
        +Money totalAmount
        +OrderStatus status
        +LocalDateTime createdAt
        +addLine(OrderLine line) void
        +removeLine(UUID variantId) void
        +markAsPaid() void
        +markPaymentFailed() void
        +cancel() void
    }

    class OrderLine {
        <<record>>
        +UUID variantId
        +int quantity
        +Money unitPrice
        +lineTotal() Money
    }

    class ShoppingCart {
        +UUID id
        +UUID buyerId
        +List~CartItem~ items
        +addItem(UUID variantId, int quantity) void
        +removeItem(UUID variantId) void
        +clear() void
        +calculateTotal(Function priceResolver) Money
        +totalQuantity() int
    }

    class CartItem {
        <<record>>
        +UUID variantId
        +int quantity
        +int MAX_QUANTITY_PER_LINE
    }

    class Money {
        +BigDecimal amount
        +Currency currency
        +plus(Money other) Money
        +multiply(int factor) Money
        +divide(long divisor) Money
        +isPositive() boolean
    }

    User "1" --> "*" Product : sellerId
    Product "1" *-- "*" Variant : contiene
    Variant "1" --> "*" InventoryItem : stock por almacén
    User "1" --> "*" Order : comprador
    User "1" --> "*" Order : vendedor
    Order "1" *-- "*" OrderLine : contiene
    OrderLine "*" --> "1" Variant : referencia
    User "1" --> "0..1" ShoppingCart : posee
    ShoppingCart "1" *-- "*" CartItem : contiene
    CartItem "*" --> "1" Variant : referencia
    Variant "1" --> "1" Money : precio
    Order "1" --> "1" Money : total
    OrderLine "1" --> "1" Money : precio unitario

    note for ShoppingCart "Máximo 99 unidades por línea (CartItem.MAX_QUANTITY_PER_LINE)"
    note for InventoryItem "availableStock + reservedStock = stock físico total"
    note for Order "En checkout multi-vendedor se crea una orden por vendedor"
```

## Invariantes principales

| Entidad | Invariante |
| --- | --- |
| **User** | Cada cuenta tiene exactamente un rol; el email se normaliza y debe ser único. |
| **Product** | Un producto pertenece a un vendedor y agrupa variantes con SKU único. |
| **Variant** | El precio autoritativo vive en la variante y debe ser positivo. |
| **InventoryItem** | `availableStock` y `reservedStock` no pueden ser negativos; el stock físico es la suma de ambos. |
| **Order** | El total debe coincidir con la suma de las líneas; las líneas solo se modifican antes del pago. |
| **OrderLine** | El precio unitario queda congelado al crear la línea. |
| **ShoppingCart** | Un comprador tiene como máximo un carrito activo y cada línea admite hasta 99 unidades. |
| **Money** | Operaciones monetarias con la misma moneda y sin valores negativos en precios. |

## Relaciones clave

- Un **Buyer** puede tener un carrito y múltiples órdenes.
- Un **Seller** puede tener múltiples productos y recibir órdenes.
- Cada **Order** pertenece a un único vendedor; el checkout multi-vendedor genera una orden por vendedor.
- Cada **OrderLine** referencia una **Variant** y conserva el precio del momento de la compra.
- Cada **InventoryItem** representa el stock de una **Variant** en un almacén concreto.
