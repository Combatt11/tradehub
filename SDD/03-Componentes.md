# 03 - Diagrama de Componentes

## Propósito

Este documento muestra la organización interna de NexusMarket siguiendo **Arquitectura Hexagonal** (Ports & Adapters). El flujo principal es:

```text
Adaptadores de entrada -> Puertos de entrada -> Servicios de dominio/aplicación
-> Puertos de salida -> Adaptadores de salida
```

## Diagrama de Componentes

```mermaid
graph TD
    subgraph IN["Adaptadores de Entrada"]
        REST["REST Controllers<br/>AuthController<br/>ProductController<br/>CartController<br/>CheckoutController<br/>OrderController<br/>InventoryController<br/>ReportController"]
        JWT["JwtAuthenticationFilter<br/>(Spring Security)"]
    end

    subgraph PORTS_IN["Puertos de Entrada (Use Cases)"]
        UC["ManageCartUseCase<br/>CheckoutUseCase<br/>CreateOrderUseCase<br/>ProcessPaymentUseCase<br/>ManageProductUseCase<br/>ReserveInventoryUseCase<br/>AddInventoryUseCase<br/>GetSalesReportUseCase"]
    end

    subgraph DOMAIN["Dominio / Aplicación"]
        APP["Servicios de Aplicación<br/>CheckoutApplicationService<br/>AuthApplicationService<br/>ProductApplicationService<br/>ReportApplicationService"]
        SVC["Servicios de Dominio<br/>CartDomainService<br/>OrderDomainService<br/>InventoryDomainService"]
        MODEL["Modelos de Dominio<br/>User, Product, Variant<br/>InventoryItem, ShoppingCart<br/>Order, OrderLine, Money"]
    end

    subgraph PORTS_OUT["Puertos de Salida"]
        REPO["Repositorios<br/>UserRepositoryPort<br/>ProductRepositoryPort<br/>VariantRepositoryPort<br/>InventoryRepositoryPort<br/>ShoppingCartRepositoryPort<br/>OrderRepositoryPort"]
        EXT["Integraciones<br/>PaymentGatewayPort<br/>JwtTokenPort<br/>PasswordHasherPort"]
    end

    subgraph OUT["Adaptadores de Salida"]
        JPA["JPA / MySQL<br/>UserRepositoryAdapter<br/>ProductRepositoryAdapter<br/>VariantRepositoryAdapter<br/>InventoryRepositoryAdapter<br/>ShoppingCartRepositoryAdapter<br/>OrderRepositoryAdapter"]
        PAY["SimulatedPaymentGatewayAdapter"]
        SEC["JwtService<br/>BCryptPasswordHasherAdapter"]
        MONGO["AuditLogAdapter<br/>(MongoDB, transversal)"]
    end

    JWT -.-> REST
    REST --> UC
    UC --> APP
    UC --> SVC
    APP --> SVC
    APP --> MODEL
    SVC --> MODEL
    APP --> REPO
    SVC --> REPO
    APP --> EXT
    SVC --> EXT
    REPO --> JPA
    EXT --> PAY
    EXT --> SEC
    APP -.->|eventos de auditoría| MONGO
```

## Responsabilidades por capa

| Capa | Componentes | Responsabilidad |
| --- | --- | --- |
| **Entrada** | Controllers, filtro JWT | Recibir HTTP, validar DTOs, resolver el usuario autenticado y delegar en puertos de entrada. |
| **Puertos de entrada** | Interfaces de casos de uso | Definir el contrato que consume la capa REST sin acoplarla al dominio concreto. |
| **Dominio / Aplicación** | Servicios y modelos | Aplicar reglas de negocio, orquestar casos de uso y mantener el dominio libre de Spring/JPA. |
| **Puertos de salida** | Repositorios e integraciones | Abstraer persistencia, pagos, JWT y hashing de contraseñas. |
| **Salida** | Adaptadores JPA, pago, seguridad, auditoría | Implementar los puertos con tecnologías concretas. |

## Reglas de dependencia

- `domain` no importa Spring, JPA, MongoDB ni anotaciones de persistencia.
- Los controladores nunca exponen modelos de dominio; usan DTOs de request/response.
- Los servicios de dominio/aplicación reciben dependencias por constructor.
- `CheckoutApplicationService` depende de puertos de entrada (`CreateOrderUseCase`, `ReserveInventoryUseCase`) y de puertos de salida para la compensación.
- El adaptador de auditoría MongoDB es transversal; actualmente no existe un `AuditPort` dedicado en el dominio.
