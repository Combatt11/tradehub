# 02 - Diagrama de Contenedores

## Propósito

Este documento muestra los contenedores desplegables de NexusMarket y las tecnologías principales. El frontend se representa como un contenedor asumido, ya que el repositorio actual contiene únicamente la API REST.

## Contenedores

| Contenedor | Tecnología | Responsabilidad |
| --- | --- | --- |
| **Frontend Web** | SPA (asumido) | Interfaz para compradores, vendedores y administradores. |
| **API REST NexusMarket** | Java 17, Spring Boot 3.5, Spring Security, SpringDoc | Expone los casos de uso, aplica JWT/RBAC y orquesta el dominio. |
| **MySQL** | Base de datos relacional | Persistencia de usuarios, productos, variantes, inventario, carritos y órdenes. |
| **MongoDB** | Base de datos documental | Persistencia de registros de auditoría. |
| **PSP** | Sistema externo | Procesa cobros. En desarrollo se usa una pasarela simulada. |
| **Servicio de Notificaciones** | Sistema externo | Envía notificaciones transaccionales. |

## Diagrama C4 de Contenedores

```mermaid
C4Container
    title NexusMarket - Diagrama de Contenedores (C4)

    Person(buyer, "Comprador (Buyer)", "Explora el catálogo, gestiona su carrito, realiza checkout y paga sus órdenes.")
    Person(seller, "Vendedor (Seller)", "Publica productos, gestiona variantes y aprovisiona inventario.")
    Person(admin, "Administrador (Admin)", "Administra usuarios, catálogo, inventario y operaciones de la plataforma.")

    System_Boundary(nexus, "NexusMarket") {
        Container(web, "Frontend Web", "SPA (asumido)", "Interfaz de usuario para compradores, vendedores y administradores.")
        Container(api, "API REST NexusMarket", "Java 17, Spring Boot 3.5, Spring Security, SpringDoc", "Expone los casos de uso, aplica JWT/RBAC y orquesta el dominio.")
        ContainerDb(mysql, "MySQL", "Base de datos relacional", "Usuarios, productos, variantes, inventario, carritos y órdenes.")
        ContainerDb(mongo, "MongoDB", "Base de datos documental", "Registro de auditoría de eventos.")
    }

    System_Ext(psp, "Pasarela de Pago (PSP)", "Procesa cobros. En desarrollo: SimulatedPaymentGatewayAdapter.")
    System_Ext(notifications, "Servicio de Notificaciones", "Envía correos y mensajes transaccionales.")

    Rel(buyer, web, "Usa", "HTTPS")
    Rel(seller, web, "Usa", "HTTPS")
    Rel(admin, web, "Usa", "HTTPS")

    Rel(web, api, "Consume", "HTTPS/JSON")
    Rel(api, mysql, "Lee y escribe", "JDBC/JPA")
    Rel(api, mongo, "Escribe auditoría", "MongoDB driver")
    Rel(api, psp, "Cobra pagos", "HTTPS/API")
    Rel(api, notifications, "Envía notificaciones", "HTTPS/SMTP")

    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

## Notas de despliegue

- La API REST es stateless: la autenticación se basa en JWT y no se crean sesiones HTTP.
- MySQL concentra el estado transaccional y de catálogo.
- MongoDB se utiliza para auditoría; el adaptador `AuditLogAdapter` escribe documentos en la colección de auditoría.
- La pasarela de pago es sustituible mediante el puerto `PaymentGatewayPort`.
- El servicio de notificaciones es una integración externa prevista; no forma parte del código actual.
