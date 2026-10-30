# 06 - Contratos de API

## Propósito

Este documento resume los endpoints principales de NexusMarket, su autenticación, los cuerpos de petición/respuesta y los códigos HTTP que produce la implementación actual.

## Convenciones

- Formato: `application/json`.
- Autenticación: `Authorization: Bearer <jwt>`.
- Rutas públicas: `/api/auth/**`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**` y `/swagger-ui.html`.
- El resto de rutas `/api/**` requieren autenticación.
- Los errores usan `ErrorResponseDTO`:

```json
{
  "timestamp": "2026-10-29T20:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Cart is empty for buyer: ..."
}
```

## Endpoints principales

| Método | Endpoint | Autenticación / Rol | Request | Respuesta de éxito | Códigos de error |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/auth/login` | Público | `LoginRequestDTO` | `200 OK` + `AuthResponseDTO` | `400 Bad Request`, `401 Unauthorized` |
| `POST` | `/api/cart/items` | JWT, `BUYER` | `AddCartItemRequestDTO` | `200 OK` + `CartResponseDTO` | `400 Bad Request`, `401 Unauthorized`, `403 Forbidden` |
| `POST` | `/api/checkout` | JWT, `BUYER` | Sin cuerpo | `201 Created` + `CheckoutResponseDTO` | `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `409 Conflict` |
| `POST` | `/api/orders/{id}/pay` | JWT, `BUYER` propietario | Sin cuerpo | `200 OK` + `PaymentResponseDTO` | `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `409 Conflict` |
| `GET` | `/api/reports/sales` | JWT, `ADMINISTRATOR` | Query `start` y `end` ISO-8601 | `200 OK` + `SalesReportDTO` | `400 Bad Request`, `401 Unauthorized`, `403 Forbidden` |

## Detalle de los endpoints principales

### `POST /api/auth/login`

Solicitud:

```json
{
  "email": "buyer@nexusmarket.test",
  "password": "Secret123!"
}
```

Respuesta `200 OK`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "buyer@nexusmarket.test",
  "role": "BUYER"
}
```

Errores:

| Código | Causa |
| --- | --- |
| `400 Bad Request` | Email o password inválidos por validación. |
| `401 Unauthorized` | Credenciales incorrectas o usuario inactivo. |

### `POST /api/cart/items`

Solicitud:

```json
{
  "variantId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "quantity": 2
}
```

Respuesta `200 OK`:

```json
{
  "id": "e5f6a7b8-c9d0-4e1f-8a2b-3c4d5e6f7a8b",
  "buyerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "items": [
    {
      "variantId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "quantity": 2
    }
  ]
}
```

Errores:

| Código | Causa |
| --- | --- |
| `400 Bad Request` | `variantId` nulo, cantidad menor que 1 o mayor que 99. |
| `401 Unauthorized` | Falta el token o es inválido. |
| `403 Forbidden` | El usuario autenticado no tiene rol `BUYER`. |

### `POST /api/checkout`

No requiere cuerpo. El servidor lee el carrito del comprador autenticado y resuelve los precios desde el catálogo.

Respuesta `201 Created`:

```json
{
  "orderIds": [
    "11111111-1111-1111-1111-111111111111",
    "22222222-2222-2222-2222-222222222222"
  ]
}
```

Errores:

| Código | Causa |
| --- | --- |
| `401 Unauthorized` | Falta el token o es inválido. |
| `403 Forbidden` | El usuario autenticado no tiene rol `BUYER`. |
| `404 Not Found` | Alguna variante del carrito ya no existe. |
| `409 Conflict` | Carrito inexistente, carrito vacío o stock insuficiente. |

### `POST /api/orders/{id}/pay`

No requiere cuerpo. El `id` de la orden se toma del path y el comprador se obtiene del token.

Respuesta `200 OK` — pago aprobado:

```json
{
  "successful": true,
  "transactionId": "SIM-123e4567-e89b-12d3-a456-426614174000",
  "failureReason": null
}
```

Respuesta `200 OK` — pago rechazado:

```json
{
  "successful": false,
  "transactionId": null,
  "failureReason": "INSUFFICIENT_FUNDS"
}
```

Errores:

| Código | Causa |
| --- | --- |
| `401 Unauthorized` | Falta el token o es inválido. |
| `403 Forbidden` | El usuario no es `BUYER` o la orden pertenece a otro comprador. |
| `404 Not Found` | La orden no existe. |
| `409 Conflict` | La orden no está en `PENDING_PAYMENT` o el total no es positivo. |

### `GET /api/reports/sales`

Requiere el rol `ADMINISTRATOR`. El rango es inclusivo y se expresa en UTC con formato ISO-8601.

Ejemplo:

```http
GET /api/reports/sales?start=2026-10-01T00:00:00&end=2026-10-31T23:59:59
```

Respuesta `200 OK`:

```json
{
  "totalFacturado": {
    "amount": 150.00,
    "currency": "USD"
  },
  "numeroOrdenes": 2,
  "ticketPromedio": {
    "amount": 75.00,
    "currency": "USD"
  }
}
```

El reporte contabiliza únicamente órdenes con pago capturado. El GMV se calcula antes de reembolsos, conforme al catálogo de métricas de la especificación.

Errores:

| Código | Causa |
| --- | --- |
| `400 Bad Request` | Rango inválido, fechas ausentes o formato incorrecto. |
| `401 Unauthorized` | Falta el token o es inválido. |
| `403 Forbidden` | El usuario no tiene rol `ADMINISTRATOR`. |

## Endpoints secundarios

| Método | Endpoint | Autenticación / Rol | Respuesta de éxito | Errores principales |
| --- | --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Público | `201 Created` + `AuthResponseDTO` | `400 Bad Request` |
| `GET` | `/api/cart` | JWT, `BUYER` | `200 OK` + `CartResponseDTO` | `401`, `403` |
| `DELETE` | `/api/cart/items/{variantId}` | JWT, `BUYER` | `204 No Content` | `400`, `401`, `403`, `404` |
| `DELETE` | `/api/cart` | JWT, `BUYER` | `204 No Content` | `401`, `403` |
| `POST` | `/api/products` | JWT, `SELLER` o `ADMINISTRATOR` | `201 Created` + `ProductResponseDTO` | `400`, `401`, `403` |
| `POST` | `/api/orders` | JWT, `BUYER` | `201 Created` + `OrderResponseDTO` | `400`, `401`, `403`, `404` |
| `POST` | `/api/inventory/reserve` | JWT, `SELLER`, `LOGISTICS_OPERATOR` o `ADMINISTRATOR` | `200 OK` + `InventoryResponseDTO` | `400`, `401`, `403`, `404`, `409` |
| `POST` | `/api/inventory/add` | JWT, `SELLER` o `ADMINISTRATOR` | `200 OK` + `InventoryResponseDTO` | `400`, `401`, `403` |

## Matriz de códigos HTTP

| Código | Significado en NexusMarket |
| --- | --- |
| `200 OK` | Operación exitosa con cuerpo de respuesta. |
| `201 Created` | Recurso creado: registro, producto, orden o checkout. |
| `204 No Content` | Operación exitosa sin cuerpo: eliminar/vaciar carrito. |
| `400 Bad Request` | DTO inválido, cantidad fuera de rango o argumento de negocio inválido. |
| `401 Unauthorized` | Token ausente, inválido o credenciales incorrectas. |
| `403 Forbidden` | Rol insuficiente o recurso propiedad de otro usuario. |
| `404 Not Found` | Usuario, producto, variante, carrito u orden inexistente. |
| `409 Conflict` | Conflicto de estado: carrito vacío, stock insuficiente u orden en estado no pagable. |
