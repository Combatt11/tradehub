# NexusMarket - Plataforma de Marketplace Multi-Vendedor

NexusMarket es una plataforma de comercio electrónico multi-vendedor construida con Java 17 y Spring Boot 3.5.16. Permite a compradores explorar productos, gestionar un carrito, realizar checkout multi-vendedor y pagar sus órdenes; a vendedores publicar productos y administrar inventario; y a administradores supervisar la operación de la plataforma.

## Contenido

- [Tech Stack](#tech-stack)
- [Arquitectura](#arquitectura)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Quick Start](#quick-start)
- [Variables de entorno](#variables-de-entorno)
- [Usuarios por defecto](#usuarios-por-defecto)
- [Documentación](#documentación)
- [Pruebas](#pruebas)
- [API principal](#api-principal)
- [Seguridad](#seguridad)
- [Detener el entorno](#detener-el-entorno)
- [Solución de problemas](#solución-de-problemas)

## Tech Stack

| Categoría | Tecnología |
| --- | --- |
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.5.16 |
| Persistencia relacional | Spring Data JPA + MySQL 8 |
| Persistencia documental | Spring Data MongoDB + MongoDB 7 |
| Seguridad | Spring Security + JWT (JJWT 0.12.6) |
| Documentación | SpringDoc OpenAPI + Swagger UI |
| Build | Maven Wrapper (Maven 3.9.16) |
| Infraestructura local | Docker + Docker Compose |
| Utilidades | Lombok, Jakarta Bean Validation, Actuator |

## Arquitectura

NexusMarket sigue **Arquitectura Hexagonal (Ports & Adapters)**. El dominio es Java puro y no depende de Spring, JPA ni MongoDB. Los controladores REST son adaptadores de entrada; los repositorios JPA/MongoDB, la pasarela de pago y los servicios de seguridad son adaptadores de salida. La comunicación entre capas se realiza mediante puertos de entrada y salida, lo que mantiene las reglas de negocio aisladas de la infraestructura.

La documentación de arquitectura, los diagramas C4, el modelo de dominio y los contratos de API están disponibles en la carpeta [`SDD/`](SDD/).

## Estructura del proyecto

```text
nexus_market/
├── SDD/                          # Documentación de arquitectura y diagramas
├── docs/                         # Especificación funcional
├── src/main/java/com/nexusmarket/
│   ├── domain/
│   │   ├── models/               # Entidades y objetos de valor
│   │   ├── ports/in/             # Casos de uso
│   │   ├── ports/out/            # Puertos de persistencia e integraciones
│   │   └── services/             # Servicios de dominio
│   ├── application/services/     # Orquestación de casos de uso
│   ├── adapters/
│   │   ├── rest/                 # Controllers, DTOs, mappers y exception handler
│   │   ├── persistence/jpa/      # Adaptadores MySQL/JPA
│   │   ├── persistence/mongodb/  # Auditoría en MongoDB
│   │   ├── payment/              # Pasarela de pago simulada
│   │   └── security/             # JWT, RBAC y autenticación
│   └── infrastructure/
│       ├── config/               # Wiring de beans y OpenAPI
│       └── seed/                 # Datos iniciales
├── src/test/                     # Tests unitarios y de capa web
├── .env.example                  # Plantilla de variables de entorno
├── docker-compose.yml            # MySQL + MongoDB
├── mvnw / mvnw.cmd               # Maven Wrapper
└── pom.xml
```

## Quick Start

### Requisitos previos

- **JDK 17** o superior.
- **Docker Desktop** con Docker Compose v2.
- **Git**.
- Puertos libres: `8080` (API), `3306` (MySQL) y `27017` (MongoDB).
- No es necesario instalar Maven: el proyecto incluye Maven Wrapper.

### Paso 1: Clonar el repositorio

```bash
git clone https://github.com/your-org/nexus-market.git
cd nexus-market
```

> Reemplaza la URL por la ubicación real del repositorio.

### Paso 2: Crear el archivo `.env`

Copia la plantilla y ajusta las credenciales de las bases de datos si es necesario:

**macOS / Linux:**

```bash
cp .env.example .env
```

**Windows PowerShell:**

```powershell
Copy-Item .env.example .env
```

El archivo `.env` contiene las variables de conexión a MySQL y MongoDB, además de la configuración JWT. Los valores por defecto de `.env.example` funcionan con el `docker-compose.yml` incluido. Para un entorno real, cambia las contraseñas y regenera `JWT_SECRET`.

### Paso 3: Levantar las bases de datos

```bash
docker compose up -d
```

Verifica que los contenedores estén saludables:

```bash
docker compose ps
```

Servicios levantados:

| Servicio | Imagen | Puerto por defecto |
| --- | --- | --- |
| MySQL | `mysql:8.0` | `3306` |
| MongoDB | `mongo:7.0` | `27017` |

### Paso 4: Ejecutar la aplicación

**macOS / Linux:**

```bash
./mvnw spring-boot:run
```

**Windows PowerShell:**

```powershell
.\mvnw.cmd spring-boot:run
```

> En macOS/Linux, si el wrapper no tiene permisos de ejecución, ejecuta `chmod +x mvnw` antes de usarlo.

La API quedará disponible en:

- **API:** <http://localhost:8080>
- **Swagger UI:** <http://localhost:8080/swagger-ui.html>
- **OpenAPI JSON:** <http://localhost:8080/v3/api-docs>
- **Health check:** <http://localhost:8080/actuator/health>

Al arrancar, `DatabaseSeeder` crea el almacén por defecto y el usuario administrador si aún no existen.

## Variables de entorno

| Variable | Descripción | Valor por defecto en `.env.example` |
| --- | --- | --- |
| `DB_URL` | URL JDBC de MySQL | `jdbc:mysql://localhost:3306/nexusmarket?...` |
| `DB_USERNAME` | Usuario de MySQL | `nexusmarket` |
| `DB_PASSWORD` | Contraseña de MySQL | `nexusmarket` |
| `DB_NAME` | Nombre de la base de datos | `nexusmarket` |
| `DB_PORT` | Puerto de MySQL | `3306` |
| `MYSQL_ROOT_PASSWORD` | Contraseña root de MySQL | `root` |
| `MONGODB_URI` | URI de conexión a MongoDB | `mongodb://nexusmarket:nexusmarket@localhost:27017/nexusmarket?authSource=admin` |
| `MONGODB_USERNAME` | Usuario de MongoDB | `nexusmarket` |
| `MONGODB_PASSWORD` | Contraseña de MongoDB | `nexusmarket` |
| `MONGODB_DATABASE` | Base de datos de MongoDB | `nexusmarket` |
| `MONGODB_PORT` | Puerto de MongoDB | `27017` |
| `JWT_SECRET` | Secreto para firmar JWT (el ejemplo usa Base64) | Valor de desarrollo incluido |
| `JWT_EXPIRATION` | Expiración del token en milisegundos | `900000` (15 minutos) |

## Usuarios por defecto

El usuario administrador es creado automáticamente por `DatabaseSeeder` al iniciar la aplicación, siempre que no exista previamente.

| Rol | Email | Contraseña | Notas |
| --- | --- | --- | --- |
| `ADMINISTRATOR` | `admin@nexusmarket.com` | `admin123` | Usuario inicial para administración. Cambiar la contraseña en producción. |

Los compradores y vendedores pueden registrarse públicamente mediante `POST /api/auth/register`. El registro público solo permite los roles `BUYER` y `SELLER`.

## Documentación

- **Swagger UI:** <http://localhost:8080/swagger-ui.html>
- **OpenAPI JSON:** <http://localhost:8080/v3/api-docs>
- **Arquitectura y diagramas C4:** [`SDD/`](SDD/)
  - [01 - Contexto](SDD/01-Contexto.md)
  - [02 - Contenedores](SDD/02-Contenedores.md)
  - [03 - Componentes](SDD/03-Componentes.md)
  - [04 - Modelo de Dominio](SDD/04-Modelo-Dominio.md)
  - [05 - Flujo de Checkout](SDD/05-Flujo-Checkout.md)
  - [06 - Contratos de API](SDD/06-API-Contracts.md)

## Pruebas

El proyecto utiliza **JUnit 5** y **Mockito**. La suite incluye:

- Tests unitarios de modelos y servicios de dominio.
- Tests de aplicación para checkout, rollback y pago rechazado.
- Tests de integración de la capa web con `@WebMvcTest` para validar seguridad, RBAC y códigos HTTP.

Ejecuta todos los tests con:

**macOS / Linux:**

```bash
./mvnw test
```

**Windows PowerShell:**

```powershell
.\mvnw.cmd test
```

También puedes compilar y empaquetar la aplicación con:

```bash
./mvnw clean package
```

**Windows PowerShell:**

```powershell
.\mvnw.cmd clean package
```

## API principal

| Método | Endpoint | Descripción | Rol |
| --- | --- | --- | --- |
| `POST` | `/api/auth/login` | Inicia sesión y devuelve un JWT | Público |
| `POST` | `/api/auth/register` | Registra un comprador o vendedor | Público |
| `POST` | `/api/cart/items` | Añade una variante al carrito | `BUYER` |
| `POST` | `/api/checkout` | Crea una orden por vendedor y reserva stock | `BUYER` |
| `POST` | `/api/orders/{id}/pay` | Procesa el pago de una orden propia | `BUYER` |
| `GET` | `/api/reports/sales` | Reporte de ventas por rango de fechas | `ADMINISTRATOR` |

Consulta el detalle completo de request/response y códigos HTTP en [`SDD/06-API-Contracts.md`](SDD/06-API-Contracts.md) o en Swagger UI.

### Ejemplo rápido de login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@nexusmarket.com",
    "password": "admin123"
  }'
```

Usa el token devuelto en las rutas protegidas. Ten en cuenta el rol: el token del administrador no sirve para `/api/cart`, que requiere `BUYER`.

Ejemplo con un token de comprador:

```bash
curl http://localhost:8080/api/cart \
  -H "Authorization: Bearer <BUYER_TOKEN>"
```

## Seguridad

- Autenticación **stateless** basada en JWT.
- Rutas públicas: `/api/auth/**`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**` y `/swagger-ui.html`.
- El resto de rutas `/api/**` requieren un token válido.
- Autorización por roles con `@PreAuthorize`.
- Roles disponibles: `BUYER`, `SELLER`, `LOGISTICS_OPERATOR`, `SUPERVISOR` y `ADMINISTRATOR`.
- El identificador del usuario autenticado se obtiene del token, nunca del cuerpo de la petición.

## Detener el entorno

Detener los contenedores sin borrar datos:

```bash
docker compose down
```

Detener y eliminar también los volúmenes de MySQL y MongoDB:

```bash
docker compose down -v
```

## Solución de problemas

| Problema | Solución |
| --- | --- |
| `docker compose up -d` falla | Verifica que Docker Desktop esté iniciado. |
| Puerto `3306` u `27017` ocupado | Detén el servicio local que use el puerto o cambia `DB_PORT` / `MONGODB_PORT` en `.env`. |
| La aplicación no conecta a las bases de datos | Ejecuta `docker compose ps` y confirma que los contenedores estén saludables. Revisa que `.env` coincida con las credenciales de Docker Compose. |
| `401 Unauthorized` | Incluye el header `Authorization: Bearer <TOKEN>` en rutas protegidas. |
| `403 Forbidden` | Verifica que el rol del usuario tenga permiso para la operación. |
| `JWT_SECRET` inválido | Usa una cadena de al menos 32 bytes; el ejemplo de `.env.example` usa Base64. Puedes generar uno con `openssl rand -base64 32`. |

---

NexusMarket está diseñado para evolucionar: la separación hexagonal permite sustituir MySQL, MongoDB o la pasarela de pago sin modificar las reglas de negocio del dominio.
