# NexusMarket — Especificación funcional completa

| Campo | Valor |
|---|---|
| Versión | 2.0 |
| Fecha | 26/10/2026 |
| Estado | Aprobada para diseño |
| Autor | Análisis de negocio senior |
| Sustituye | Especificación funcional v1, supuestos del README y comportamiento implícito del código actual |
| Ámbito | Marketplace multi-vendedor de productos físicos y digitales |
| Moneda | Configurable por marketplace; valor por defecto EUR |
| Idioma | Español |

> **Nomenclatura.** *NexusMarket* es el nombre funcional del marketplace. El repositorio de referencia se denomina *TradeHub*. Esta especificación es la fuente de verdad para el diseño y prevalece sobre el README y sobre cualquier comportamiento actual del código que la contradiga. Las brechas entre esta especificación y la implementación existente se listan en el Anexo A.

## Convenciones

- **DEB-xx**: decisión de negocio.
- **RN-xx**: regla de negocio.
- **EST-xx**: estado.
- **CAP-xx**: capacidad de permiso.
- **Debe**: requisito obligatorio. **Debería**: requisito recomendado, salvo justificación aprobada.
- Los estados se identifican con códigos en inglés para trazabilidad con el diseño; la etiqueta funcional se muestra en español.
- Los importes se expresan en unidades monetarias con dos decimales, salvo que la moneda exija otra escala.

---

## 1. Resumen ejecutivo

NexusMarket es un marketplace en el que varios vendedores publican productos físicos y digitales, un comprador puede adquirir productos de varios vendedores en una sola compra y la plataforma coordina pago, inventario, logística, facturación, devoluciones y reportes.

Las decisiones estructurales de esta versión son:

1. **El precio vive en la variante/SKU**, no en el producto genérico. El producto muestra un precio “desde” derivado.
2. **El pago se procesa mediante un PSP externo integrado** (checkout alojado y webhooks). La pasarela simulada solo existe en desarrollo y pruebas.
3. **Una compra con productos de varios vendedores genera una compra y un pago, pero N pedidos**, uno por vendedor.
4. **El inventario se reserva al confirmar el checkout y se descuenta al confirmar el pago.** La reserva caduca a los 15 minutos. La concurrencia por defecto es optimista, con bloqueo pesimista controlado para SKU de alta contención.
5. **Se crean los dominios de Facturación, Logística, Devoluciones y Reembolsos, y Reportes**, cada uno con objetivo, participantes, atributos y reglas.
6. **Se mantiene el rol Supervisor**, con perfil de control y aprobación de cuatro ojos, sin poderes de administración ni de configuración.
7. **El documento de identidad se incorpora formalmente a la tabla de Usuarios**, con tipo, número, país, verificación y protección de datos.
8. **Los productos digitales se entregan mediante derechos digitales** (descarga firmada, licencia o acceso), emitidos al confirmar el pago y revocados al reembolsar.
9. **El pedido tiene atributos explícitos**: dirección de entrega congelada, líneas, precio congelado, importes, impuestos, estado de pago, envío, factura, devolución y auditoría.
10. **La máquina de estados incorpora CANCELADO, DEVUELTO y REEMBOLSADO**, además de los estados intermedios necesarios, con transiciones, actores, guardas y efectos laterales definidos.

El resultado es una especificación apta para derivar el modelo de dominio, los casos de uso, las APIs, las tablas y los criterios de aceptación.

---

## 2. Decisiones de negocio

| ID | Punto | Decisión | Justificación | Sección |
|---|---|---|---|---|
| DEB-01 | Precio | El precio autoritativo está en la **variante/SKU**. El producto solo expone un precio “desde” derivado de sus variantes publicadas. | El precio depende de talla, color, formato, licencia, duración o capacidad. La línea de pedido debe apuntar a un SKU exacto para congelar precio, impuesto e inventario. Un precio a nivel de producto obliga a excepciones y permite inconsistencias entre lo mostrado y lo cobrado. | 4.2 |
| DEB-02 | Pasarela de pago | **Externa e integrada mediante PSP** con checkout alojado, tokenización y webhooks. **Simulada solo en desarrollo/pruebas**. | Reduce el alcance PCI DSS, aporta 3DS/SCA, métodos locales, antifraude y liquidación. La pasarela simulada no es aceptable en producción porque no mueve dinero real ni cumple obligaciones financieras. | 4.6 |
| DEB-03 | Multi-vendedor | Un carrito y un checkout generan **una compra (CheckoutGroup) y N pedidos**, uno por vendedor. Hay **un solo pago** y **N facturas, envíos, devoluciones y reembolsos**. | Cada vendedor tiene su propio inventario, obligaciones fiscales, SLA, logística y responsabilidad. Un pedido único multi-vendedor impide cancelar, devolver, facturar o enviar por separado y acopla la reputación de varios vendedores. | 4.4 |
| DEB-04 | Inventario | **Reserva al confirmar el checkout** (PENDING_PAYMENT) con TTL de 15 minutos. **Descuento definitivo al confirmar el pago**. Liberación al fallar, caducar o cancelar. **Bloqueo optimista por defecto** y pesimista para SKU de alta contención. | Reservar al añadir al carrito bloquea stock por carritos abandonados. Descontar solo al enviar permite sobreventa. Reservar en checkout y confirmar en pago equilibra conversión y exactitud. El optimista escala mejor en un marketplace de lectura intensiva; el pesimista evita contención extrema en lanzamientos. | 4.3 |
| DEB-05 | Dominios faltantes | Se crean **Facturación, Logística, Devoluciones y Reembolsos, y Reportes**, con objetivos, participantes, atributos y reglas. | Sin estos dominios el marketplace no puede cerrar el ciclo económico, entregar, revertir una venta ni medir la operación. Son dominios de negocio, no simples módulos técnicos. | 4.8–4.11 |
| DEB-06 | Supervisor | **Se mantiene el rol**. Es un rol de supervisión, lectura global y aprobación limitada; no administra usuarios, configuración, precios ni pagos. | Permite aplicar el principio de cuatro ojos en devoluciones, reembolsos, fraude y suspensiones temporales, y evita que el Administrador sea el único aprobador. Su límite económico y temporal impide que se convierta en un “superadministrador”. | 7 |
| DEB-07 | Documento de identidad | Se añaden a Usuarios: **tipo, número, país, hash de unicidad, estado de verificación, fecha, verificador y referencia KYC**. | La identidad es necesaria para KYC de vendedores, prevención de fraude, obligaciones fiscales y soporte. Un único campo de texto libre no permite unicidad, verificación, enmascaramiento ni auditoría. | 4.1 |
| DEB-08 | Producto digital | Entrega mediante **derecho digital**: descarga firmada, clave de licencia o acceso a contenido, disponible en “Mi biblioteca” y emitido al confirmar el pago. | No hay envío físico. El derecho digital debe ser verificable, revocable y auditable; además permite aplicar la política de devolución de digitales y revocar accesos tras un reembolso. | 4.7 |
| DEB-09 | Atributos del pedido | Se definen **cabecera, líneas, direcciones congeladas, importes, impuestos, estados, referencias de pago, envío, factura, devolución y auditoría**. | Un pedido es un contrato comercial y un documento financiero. Sin precio congelado y direcciones snapshot, un cambio posterior de catálogo o libreta de direcciones altera la obligación de las partes. | 4.5 |
| DEB-10 | Estados | La máquina de estados incorpora **PAYMENT_FAILED, ON_HOLD, CANCELLED, RETURN_REQUESTED, RETURN_IN_TRANSIT, RETURN_RECEIVED, RETURNED, REFUND_PENDING y REFUNDED**, con transiciones y guardas. | El ciclo real incluye fallos de pago, revisión antifraude, cancelaciones, devoluciones y reembolsos. Ocultarlos en un estado genérico impide operar, auditar y conciliar. | 6 |

---

## 3. Alcance, actores y roles

### 3.1 Alcance funcional

**Incluido:**

- Registro, autenticación y gestión de usuarios con un rol por cuenta.
- Alta y verificación de vendedores por parte del Administrador.
- Catálogo de productos físicos y digitales con variantes/SKU, precios, medios y categorías.
- Inventario distribuido por almacén, con reservas, descuento, liberación, ajustes y devoluciones.
- Carrito multi-vendedor y checkout con una compra y N pedidos.
- Pago con PSP externo, incluyendo fallos, reintentos, conciliación y contracargos.
- Preparación, envío, seguimiento, entrega e incidencias logísticas.
- Entrega digital mediante derechos digitales.
- Facturación, notas de crédito y liquidación a vendedores.
- Devoluciones, inspección, reembolsos y disputas.
- Reportes operativos, comerciales y financieros.
- Auditoría, notificaciones transaccionales y trazabilidad.

**Excluido de esta versión (salvo decisión posterior):**

- Publicidad, subastas, suscripciones recurrentes y marketplace de servicios.
- Compras como invitado sin cuenta.
- Programa de puntos, fidelización y crédito al comprador.
- Logística propia con flota; se integra con transportistas.
- Facturación electrónica de países no soportados por el motor fiscal.
- Reseñas y reputación detalladas; se dejan como extensión, aunque los reportes pueden consumir métricas de satisfacción si existen.

### 3.2 Actores

| Actor | Descripción | Interés principal |
|---|---|---|
| Visitante | Persona no autenticada. | Explorar catálogo, precios y disponibilidad; registrarse o iniciar sesión. |
| Comprador | Usuario con rol BUYER y estado comercial activo. | Comprar, pagar, recibir, descargar, devolver y consultar sus documentos. |
| Vendedor | Usuario con rol SELLER, aprobado por el Administrador. | Publicar catálogo, gestionar stock y pedidos propios, atender devoluciones y cobrar. |
| Operador logístico | Usuario con rol LOGISTICS_OPERATOR. | Recibir, preparar, despachar, entregar y gestionar incidencias y devoluciones físicas. |
| Supervisor | Usuario con rol SUPERVISOR. | Supervisar la operación, aprobar excepciones y actuar como segundo par de ojos. |
| Administrador | Usuario con rol ADMINISTRATOR. | Configurar la plataforma, gestionar usuarios y vendedores, y asumir las decisiones de mayor impacto. |
| Sistema | Jobs, webhooks y automatizaciones. | Ejecutar caducidades, conciliaciones, reintentos, emisión de facturas y reportes. |
| PSP | Proveedor de servicios de pago. | Autorizar, capturar, reembolsar y reportar disputas. |
| Transportista | Proveedor logístico. | Recoger, transportar, entregar y reportar eventos. |
| Proveedor KYC | Servicio de verificación de identidad. | Verificar documentos y listas de sanciones. |
| Autoridad fiscal | Organismo tributario. | Recibir facturas electrónicas cuando la jurisdicción lo exija. |

### 3.3 Roles y principio de asignación

- Cada usuario tiene **exactamente un rol** en esta versión. Si una persona necesita dos roles, se usan cuentas separadas o una composición de roles futura; no se permite autoasignarse permisos.
- Los permisos son **atómicos y denegados por defecto**. El rol concede un conjunto de capacidades; además se aplican **alcance** (propio, asignado o global) y **condiciones** (umbral, estado, país, categoría).
- Toda operación sensible se audita con actor, fecha, IP, correlación, valor anterior y valor nuevo.
- Las operaciones de cuatro ojos requieren que quien aprueba sea distinto de quien solicita y de quien ejecuta.
- “Finanzas” no es un rol separado: es un **perfil funcional** del Administrador con las capacidades financieras (CAP-14, CAP-15, CAP-16, CAP-25). Esto evita ampliar el enum de roles sin necesidad.

### 3.4 Reglas generales de autorización

- **RN-AUT-01:** Toda operación requiere autenticación, salvo catálogo público, registro y login.
- **RN-AUT-02:** Un usuario solo puede operar sobre sus propios recursos, salvo que su capacidad indique alcance global o asignado.
- **RN-AUT-03:** Un vendedor no puede comprar sus propios productos ni aprobar devoluciones en las que sea comprador.
- **RN-AUT-04:** Un usuario bloqueado no puede iniciar sesión; un comprador suspendido puede consultar y devolver, pero no comprar.
- **RN-AUT-05:** Los roles privilegiados (Administrador, Supervisor, Operador logístico) deben usar MFA.
- **RN-AUT-06:** El Administrador no puede eliminar registros de auditoría, saltarse el PSP, cambiar su propio rol ni aprobar su propio reembolso.
- **RN-AUT-07:** El Supervisor no puede modificar catálogo, precios, impuestos, comisiones, usuarios ni configuración de pagos.

---

## 4. Modelo de dominio

### 4.1 Usuarios y documento de identidad

**Objetivo:** identificar de forma única y verificable a cada persona, habilitar el acceso según su rol y cumplir obligaciones de KYC, antifraude y fiscales.

#### Tabla `users`

| Atributo | Tipo | Obligatorio | Regla |
|---|---|---|---|
| `id` | UUID | Sí | Clave primaria. |
| `full_name` | Texto 150 | Sí | Nombre completo, tal como figura en el documento. |
| `email` | Texto 254 | Sí | Único, normalizado a minúsculas; usado para login. |
| `password_hash` | Texto | Sí | Argon2id o bcrypt con coste adecuado; nunca se devuelve. |
| `role` | Enum | Sí | BUYER, SELLER, LOGISTICS_OPERATOR, SUPERVISOR, ADMINISTRATOR. |
| `status` | Enum | Sí | ACTIVE, BLOCKED, INACTIVE. Por defecto ACTIVE. |
| `identity_document_type` | Enum | Condicional | DNI, NIE, PASSPORT, TAX_ID, OTHER. Obligatorio para vendedores, logística, supervisores y administradores. |
| `identity_document_number` | Texto cifrado | Condicional | Número completo; cifrado en reposo; nunca se muestra completo salvo CAP-22. |
| `identity_document_country` | ISO 3166-1 alpha-2 | Condicional | País emisor. |
| `identity_document_hash` | Texto | Condicional | HMAC del tipo + país + número, para unicidad y búsqueda sin exponer el número. |
| `identity_verified` | Booleano | Sí | Por defecto falso. |
| `identity_verified_at` | Timestamp | No | Fecha de verificación. |
| `identity_verified_by` | UUID | No | Usuario o proveedor que verificó. |
| `identity_verification_ref` | Texto | No | Referencia del proveedor KYC. |
| `phone` | Texto 30 | No | Verificado para operaciones sensibles. |
| `locale` | Texto 10 | Sí | Por defecto `es-ES`. |
| `timezone` | Texto 50 | Sí | Por defecto `Europe/Madrid` o la del marketplace. |
| `mfa_enabled` | Booleano | Sí | Obligatorio para roles privilegiados. |
| `last_login_at` | Timestamp | No | Auditable. |
| `failed_login_attempts` | Entero | Sí | Se reinicia al autenticar correctamente. |
| `locked_until` | Timestamp | No | Bloqueo temporal por intentos. |
| `created_at` | Timestamp | Sí | UTC. |
| `updated_at` | Timestamp | Sí | UTC. |
| `deleted_at` | Timestamp | No | Borrado lógico o anonimización; no borra auditoría. |
| `version` | Entero | Sí | Control de concurrencia optimista. |

#### Reglas del documento de identidad

- **RN-USR-01:** El documento es **único por tipo + país + número**. La unicidad se valida con `identity_document_hash`, no con el texto cifrado.
- **RN-USR-02:** Es obligatorio para vendedores, operadores logísticos, supervisores y administradores. Para compradores es obligatorio cuando solicite factura con datos fiscales, compre artículos con edad mínima o supere el umbral antifraude; en los demás casos es opcional.
- **RN-USR-03:** El vendedor requiere además KYC de empresa, titular real, domicilio fiscal y cuenta de liquidación. La aprobación es del Administrador; el Supervisor puede revisar y recomendar, no aprobar.
- **RN-USR-04:** Un documento verificado no puede modificarse por autoservicio. Un cambio exige re-verificación y deja historial.
- **RN-USR-05:** El número se muestra enmascarado (por ejemplo, `****1234`). Solo el Administrador con CAP-22 y el propio usuario pueden ver el valor completo; el Supervisor ve la versión enmascarada.
- **RN-USR-06:** Todo acceso al documento completo se registra en auditoría con motivo.
- **RN-USR-07:** No se permite reutilizar el documento de una cuenta bloqueada o anonimizada para crear otra cuenta sin revisión antifraude.
- **RN-USR-08:** Los documentos se cifran en reposo y se transmiten por canal seguro. No se incluyen en reportes, logs ni correos.
- **RN-USR-09:** La retención del documento se limita al plazo legal aplicable; después se anonimiza o se elimina, conservando el hash y el resultado de la verificación cuando la ley lo permita.
- **RN-USR-10:** El `tax_id` del vendedor es un dato fiscal distinto del documento de identidad y se almacena en `sellers`.

#### Atributos específicos del comprador (`buyers`)

`id`, `user_id`, `commercial_status` (ACTIVE, SUSPENDED, RESTRICTED), `default_address_id`, `preferred_currency`, `accepts_marketing`, `created_at`, `updated_at`, `version`.

- **RN-BUY-01:** Solo un comprador ACTIVE puede iniciar checkout.
- **RN-BUY-02:** SUSPENDED puede consultar pedidos, descargar facturas y gestionar devoluciones; RESTRICTED requiere revisión para comprar.
- **RN-BUY-03:** Las direcciones se validan y se guardan como snapshot al confirmar el checkout.

#### Atributos específicos del vendedor (`sellers`)

`id`, `user_id`, `business_name`, `legal_name`, `tax_id`, `tax_regime`, `fiscal_address`, `contact_phone`, `status` (PENDING_APPROVAL, ACTIVE, SUSPENDED, REJECTED), `commission_rate`, `payout_account_token`, `rating`, `approved_at`, `approved_by`, `created_at`, `updated_at`, `version`.

- **RN-SEL-01:** El alta la realiza exclusivamente el Administrador; el vendedor no se autorregistra.
- **RN-SEL-02:** Sin KYC aprobado y cuenta de liquidación validada no se puede publicar catálogo ni recibir pagos.
- **RN-SEL-03:** La suspensión impide nuevas ventas y publicaciones, pero no cancela pedidos ya pagados; estos deben honrarse o cancelarse con reembolso.
- **RN-SEL-04:** La comisión es configurable por vendedor y categoría; se congela por pedido en el momento del pago.

### 4.2 Producto, variante y precio

#### Decisión: el precio va en la variante

El **producto** es la unidad comercial y de contenido. La **variante/SKU** es la unidad vendible, inventariable y facturable. El precio autoritativo, la moneda, el impuesto, el peso y las dimensiones viven en la variante.

**Por qué:**

- Un mismo producto puede tener tallas, colores, capacidades, formatos o licencias con precios distintos.
- El inventario y el código de barras existen por SKU, no por producto.
- La línea de pedido debe identificar exactamente qué se compró y a qué precio.
- La fiscalidad puede variar por variante (por ejemplo, libro digital frente a libro físico).
- Las promociones y listas de precios se aplican por SKU.
- Un precio único en el producto obliga a duplicar productos o a introducir reglas ad hoc, y facilita cobrar un precio distinto al mostrado.

Para productos simples se crea automáticamente una **variante por defecto**. El producto expone `display_price_from` como el mínimo de sus variantes publicadas; es un dato derivado de catálogo, no el precio de venta.

#### Tabla `products`

`id`, `seller_id`, `name`, `description`, `category_id`, `brand`, `type` (PHYSICAL, DIGITAL), `status` (DRAFT, PUBLISHED, SUSPENDED, DISCONTINUED), `attributes`, `media`, `default_variant_id`, `display_price_from`, `rating_average`, `rating_count`, `created_at`, `updated_at`, `version`.

#### Tabla `variants`

| Atributo | Tipo | Obligatorio | Regla |
|---|---|---|---|
| `id` | UUID | Sí | Clave primaria. |
| `product_id` | UUID | Sí | Producto padre. |
| `sku` | Texto 64 | Sí | Único global o por vendedor según configuración; inmutable tras la primera venta. |
| `barcode` | Texto 64 | No | EAN/UPC/ISBN si aplica. |
| `attribute_values` | JSON | No | Talla, color, formato, licencia, duración. |
| `price` | Decimal 19,2 | Sí | Mayor que cero; moneda de la variante. |
| `compare_at_price` | Decimal 19,2 | No | Precio de referencia; debe ser mayor o igual al precio. |
| `currency` | ISO 4217 | Sí | Una moneda por checkout en esta versión. |
| `tax_class_id` | UUID | Sí | Clase fiscal aplicable. |
| `weight_grams` | Entero | Condicional | Obligatorio para físicos. |
| `dimensions` | JSON | Condicional | Obligatorio para físicos. |
| `status` | Enum | Sí | ACTIVE, INACTIVE, OUT_OF_STOCK. |
| `digital_license_limit` | Entero | No | Si es finito, se reserva licencia como stock digital. |
| `created_at` | Timestamp | Sí | UTC. |
| `updated_at` | Timestamp | Sí | UTC. |
| `version` | Entero | Sí | Concurrencia optimista. |

#### Reglas de precio

- **RN-PRE-01:** El precio de venta es `variants.price`; el precio del producto es solo referencia de catálogo.
- **RN-PRE-02:** Todo precio debe ser mayor que cero y tener dos decimales. No se permiten precios negativos ni `null`.
- **RN-PRE-03:** La moneda del checkout es única. Si el comprador cambia de moneda, se recotiza el carrito; no se mezclan monedas.
- **RN-PRE-04:** El precio se congela al confirmar el checkout (`price_frozen_at`). Después del pago es inmutable.
- **RN-PRE-05:** Si el precio cambia entre añadir al carrito y confirmar el checkout, se muestra el nuevo precio y se exige aceptación si el total sube. Si baja, se aplica el menor.
- **RN-PRE-06:** Una compra ya pagada no se recalcula por cambios de catálogo, promociones o tipo de cambio.
- **RN-PRE-07:** Los descuentos se registran como líneas de descuento, no modificando el precio unitario congelado. Esto preserva la trazabilidad fiscal.
- **RN-PRE-08:** El precio mostrado debe incluir o desglosar impuestos según la jurisdicción; la línea de pedido guarda base, tasa, impuesto y total.
- **RN-PRE-09:** El precio de una variante suspendida no se muestra ni se puede comprar; las compras existentes se respetan.
- **RN-PRE-10:** El vendedor puede cambiar precios de variantes sin afectar pedidos en curso.

### 4.3 Inventario

#### Entidades

**`inventory_items`**: `id`, `variant_id`, `warehouse_id`, `quantity_on_hand`, `quantity_reserved`, `quantity_available`, `quantity_damaged`, `reorder_point`, `version`, `updated_at`.

**`inventory_movements`**: `id`, `inventory_item_id`, `type` (INBOUND, RESERVATION, RELEASE, SALE, CANCELLATION_RESTOCK, RETURN_RESTOCK, DAMAGE, ADJUSTMENT, TRANSFER), `quantity`, `reference_type`, `reference_id`, `actor_id`, `balance_after`, `created_at`, `reason`.

**`reservations`**: `id`, `order_line_id` (único), `inventory_item_id`, `quantity`, `status` (ACTIVE, CONFIRMED, RELEASED, EXPIRED), `expires_at`, `created_at`, `confirmed_at`, `released_at`, `version`.

#### Reglas de inventario

- **RN-INV-01:** `quantity_available = quantity_on_hand - quantity_reserved - quantity_damaged`. Nunca puede ser negativa.
- **RN-INV-02:** No se permite stock negativo en ninguna operación, ni por concurrencia, ni por ajuste, ni por devolución.
- **RN-INV-03:** El inventario se lleva por **variante + almacén**, no por producto.
- **RN-INV-04:** Un producto digital no consume inventario físico. Si tiene límite de licencias, se reserva una licencia al checkout y se consolida al pago.
- **RN-INV-05:** La reserva se crea al confirmar el checkout, no al añadir al carrito.
- **RN-INV-06:** La reserva tiene TTL de 15 minutos, configurable entre 10 y 30. Un job la expira cada minuto.
- **RN-INV-07:** Al confirmar el pago, la reserva pasa a CONFIRMED y el stock se descuenta: `quantity_reserved` baja y `quantity_on_hand` baja en la misma cantidad.
- **RN-INV-08:** Al fallar el pago de forma definitiva, caducar la reserva o cancelar antes del pago, la reserva pasa a RELEASED o EXPIRED y el stock vuelve a disponible.
- **RN-INV-09:** Si el pedido se cancela después del pago y antes del envío, se repone el stock con movimiento CANCELLATION_RESTOCK.
- **RN-INV-10:** Una devolución se repone solo después de la inspección. Si es vendible, vuelve a disponible; si está dañada, va a cuarentena o dañado.
- **RN-INV-11:** Los ajustes exigen motivo, actor y evidencia; no pueden dejar saldo negativo.
- **RN-INV-12:** El stock se asigna a un almacén concreto. Si no hay suficiente en el almacén elegido, el checkout falla y ofrece alternativas; no se parte una misma línea entre almacenes en esta versión.
- **RN-INV-13:** La reserva es idempotente por `order_line_id`; un reintento no duplica cantidades.
- **RN-INV-14:** Nunca se mantiene una transacción de base de datos abierta mientras se llama al PSP o al transportista.

#### Momento exacto de reserva y descuento

| Momento | Acción | Efecto en inventario |
|---|---|---|
| Añadir al carrito | Verificación suave de disponibilidad | Ninguno. No se reserva. |
| Confirmar checkout | Validar carrito, congelar precios, crear compra y pedidos, crear reservas | `available` baja, `reserved` sube; reserva ACTIVE con TTL. |
| Pago aprobado | Confirmar reserva y emitir factura | `reserved` baja, `on_hand` baja; reserva CONFIRMED. |
| Pago rechazado | Mantener reserva hasta reintento o TTL | Sin cambio mientras esté ACTIVE. |
| Pago fallido definitivo, caducidad o cancelación previa al pago | Liberar reserva | `reserved` baja, `available` sube; reserva RELEASED/EXPIRED. |
| Cancelación pagada antes de envío | Reponer stock | `on_hand` sube, `available` sube; movimiento CANCELLATION_RESTOCK. |
| Devolución recibida y aceptada | Reponer o marcar dañado | `available` sube o `damaged` sube; movimiento RETURN_RESTOCK o DAMAGE. |
| Envío | Salida física | Sin cambio de stock: ya se descontó al pagar. Solo cambia el estado logístico. |

#### Concurrencia: optimista por defecto, pesimista por excepción

**Decisión:** bloqueo **optimista** como mecanismo por defecto, con **bloqueo pesimista** para SKU de alta contención.

**Optimista:**

- Cada fila de inventario y cada reserva tiene `version`.
- La reserva se ejecuta con una actualización condicional atómica: solo descuenta si `quantity_available >= cantidad` y la versión coincide.
- Si la actualización afecta a cero filas, hay conflicto: se reintenta hasta tres veces con espera exponencial y jitter.
- Si tras los reintentos no hay stock, se informa “el stock cambió” y se invita a revisar el carrito.
- Se usa aislamiento READ COMMITTED y transacciones cortas.
- Restricciones de base de datos: `available >= 0`, `reserved >= 0`, unicidad de reserva por línea.

**Pesimista:**

- Se usa `SELECT ... FOR UPDATE` sobre las filas de inventario, ordenadas por `variant_id` ascendente para evitar interbloqueos.
- Se aplica cuando la tasa de conflicto de un SKU supera el 5%, en lanzamientos limitados o cuando una compra requiere bloquear varias líneas de forma atómica.
- La transacción debe ser muy corta y nunca incluir llamadas externas.
- Alternativa para picos extremos: reserva atómica en Redis con TTL y reconciliación asíncrona con la base de datos; se adopta solo si la operación lo justifica.

**Por qué:** el marketplace es de lectura intensiva y los conflictos suelen estar localizados en pocos SKU. El optimista evita bloqueos innecesarios y escala mejor. El pesimista se reserva para los casos donde la contención es real y el coste de reintentar es mayor que el de bloquear.

#### Idempotencia y recuperación

- **RN-INV-15:** La confirmación de checkout usa una clave de idempotencia por comprador y carrito. Repetir la petición devuelve la misma compra, no crea otra.
- **RN-INV-16:** La confirmación de pago es idempotente por `payment_id`; un webhook duplicado no descuenta dos veces.
- **RN-INV-17:** Un job de reconciliación compara reservas ACTIVE con pagos y pedidos, y libera o confirma las que quedaron inconsistentes.
- **RN-INV-18:** Se alerta si una reserva ACTIVE supera el TTL en más de cinco minutos o si se detecta un intento de stock negativo.

### 4.4 Carrito, checkout y compra multi-vendedor

#### 4.4.1 Carrito

El carrito es un agregado **previo al pedido**. No es un pedido y no reserva inventario.

**`carts`**: `id`, `buyer_id`, `currency`, `status` (ACTIVE, CHECKED_OUT, ABANDONED, MERGED), `created_at`, `updated_at`, `expires_at`, `version`.

**`cart_lines`**: `id`, `cart_id`, `variant_id`, `seller_id`, `quantity`, `unit_price_at_add`, `current_unit_price`, `warehouse_id`, `added_at`, `updated_at`, `version`.

**Reglas:**

- **RN-CAR-01:** Un comprador tiene como máximo un carrito ACTIVE.
- **RN-CAR-02:** Añadir al carrito solo verifica disponibilidad de forma suave; no crea reserva.
- **RN-CAR-03:** El carrito muestra el precio actual de la variante. Si cambió desde que se añadió, se informa al comprador.
- **RN-CAR-04:** La cantidad por línea es un entero positivo, máximo 99 por defecto y nunca superior al stock disponible en el almacén elegido.
- **RN-CAR-05:** El carrito puede contener productos de varios vendedores; la interfaz los agrupa por vendedor y muestra envío y plazo estimado por separado.
- **RN-CAR-06:** El carrito caduca a los 30 días de inactividad. Un carrito caducado no se puede pagar; se puede recuperar como nuevo.
- **RN-CAR-07:** Al iniciar sesión se fusiona el carrito anónimo con el del comprador, sumando cantidades y respetando el máximo.
- **RN-CAR-08:** Un vendedor no puede añadir sus propios productos.
- **RN-CAR-09:** El comprador debe aceptar los términos, la política de devoluciones y, si aplica, el consentimiento de datos en el checkout.

#### 4.4.2 Checkout

**`checkout_groups`** (la “compra”): `id`, `group_number`, `buyer_id`, `currency`, `status` (PENDING_PAYMENT, PAID, PARTIALLY_FULFILLED, COMPLETED, PARTIALLY_CANCELLED, CANCELLED, REFUNDED), `payment_id`, `subtotal`, `discount_total`, `shipping_total`, `tax_total`, `grand_total`, `shipping_address_id`, `billing_address_id`, `created_at`, `paid_at`, `completed_at`, `version`.

**Reglas del checkout:**

- **RN-CHK-01:** El checkout es una operación **todo o nada** en su creación: si una línea no puede reservarse, no se crea ningún pedido ni se inicia el pago.
- **RN-CHK-02:** Se congela el precio de cada línea, la moneda, la dirección de entrega, la dirección de facturación, los impuestos, los descuentos y el coste de envío.
- **RN-CHK-03:** Se crea **un pedido por vendedor**. Si un vendedor tiene líneas físicas y digitales, se mantiene un solo pedido con líneas de distinto tipo; la logística solo aplica a las físicas.
- **RN-CHK-04:** Se crea **un solo pago** por el total de la compra.
- **RN-CHK-05:** El total de la compra debe ser igual a la suma de los totales de los pedidos, sin diferencias de redondeo.
- **RN-CHK-06:** Si el comprador abandona antes de pagar, la compra queda PENDING_PAYMENT hasta el TTL; luego se cancela y se liberan las reservas.
- **RN-CHK-07:** El checkout exige dirección de entrega para líneas físicas. Si todas las líneas son digitales, no exige dirección de entrega, pero sí datos de facturación si el comprador los solicita.
- **RN-CHK-08:** El checkout valida que el comprador esté activo, que los vendedores estén activos, que las variantes estén publicadas y que el stock esté disponible.
- **RN-CHK-09:** El checkout no permite mezclar monedas. Si el comprador cambia de moneda, se recotiza todo el carrito.
- **RN-CHK-10:** El checkout genera un `group_number` legible y un `order_number` por pedido, con secuencia correlativa y sin huecos dentro de cada serie.

#### 4.4.3 Decisión multi-vendedor: una compra, N pedidos

**Decisión:** un carrito con productos de varios vendedores genera **un CheckoutGroup y N pedidos**, uno por vendedor, con **un solo pago**.

**Justificación:**

1. **Cumplimiento y facturación:** cada vendedor es responsable de su propia factura, impuestos y datos fiscales. Un pedido único obligaría a facturas multi-emisor o a una ficticia “factura única” que no refleja la realidad.
2. **Logística:** cada vendedor puede tener almacén, transportista, plazo y coste distintos. Un pedido único no puede tener un solo estado de envío cuando hay varios orígenes.
3. **Devoluciones y reembolsos:** el comprador puede devolver solo un producto de un vendedor. Con pedidos separados, la devolución y el reembolso parcial son naturales; con un pedido único, se contamina el resto.
4. **Ciclo de vida:** un vendedor puede aceptar y otro rechazar; uno puede enviar y otro retrasarse. Los pedidos separados permiten estados independientes y un grupo que agrega el resultado.
5. **Liquidación:** la plataforma liquida a cada vendedor por separado, descontando comisiones, reembolsos y contracargos de su propio pedido.
6. **Reputación y SLA:** las métricas de cumplimiento se atribuyen al vendedor correcto, no al conjunto.
7. **Operación:** cancelar, reprogramar o reenviar un pedido no afecta a los demás.

**Alternativas descartadas:**

- **Un pedido único con líneas multi-vendedor:** imposible de facturar y enviar correctamente sin crear sub-pedidos; en la práctica es el mismo modelo con más complejidad.
- **N pagos separados:** peor experiencia, más comisiones, más fallos parciales y más contracargos. Se prefiere un pago y una distribución interna.

**Reglas de la compra multi-vendedor:**

- **RN-MUL-01:** El comprador ve una sola compra y un solo cobro, pero puede seguir cada pedido por separado.
- **RN-MUL-02:** Cada pedido tiene su propio estado, factura, envío, devolución y reembolso.
- **RN-MUL-03:** Si un vendedor rechaza o cancela su pedido después del pago, se reembolsa la parte proporcional de ese pedido; los demás continúan. La compra pasa a PARTIALLY_CANCELLED.
- **RN-MUL-04:** El coste de envío se calcula por vendedor y por almacén de origen. No se suma un envío único si los productos salen de orígenes distintos.
- **RN-MUL-05:** Un cupón de plataforma se reparte entre los pedidos en proporción al subtotal de cada uno; el redondeo se asigna por mayor resto para que la suma coincida exactamente. Un cupón de vendedor solo aplica a su pedido.
- **RN-MUL-06:** La comisión de plataforma se calcula por pedido, no sobre el total de la compra, para permitir reembolsos y liquidaciones independientes.
- **RN-MUL-07:** Si un pedido se cancela total o parcialmente, el comprador recibe el reembolso de su parte, incluida la parte proporcional de envío y descuentos.
- **RN-MUL-08:** El grupo se considera COMPLETED cuando todos los pedidos están FINALIZED o CANCELLED sin saldo pendiente; REFUNDED cuando todos están reembolsados.
- **RN-MUL-09:** El comprador recibe un recibo de la compra y una factura por cada pedido.
- **RN-MUL-10:** Si la creación de un pedido falla después de reservar otros, se compensan todas las reservas y no se cobra.

#### 4.4.4 Casos límite del checkout

| Caso | Comportamiento |
|---|---|
| Un vendedor se suspende entre el carrito y el checkout | El checkout falla para sus líneas y ofrece eliminarlas o guardar el resto. |
| Una variante se despublica | Se elimina del checkout con aviso; no se cobra. |
| El precio sube antes de confirmar | Se muestra el nuevo total y se exige aceptación; si no acepta, no se crea la compra. |
| El stock se agota durante el checkout | Falla la reserva de esa línea; no se crea ningún pedido ni pago. |
| El comprador cambia la dirección después de pagar | No se permite si el pedido ya está en preparación; antes de preparar, se puede solicitar cambio y el vendedor decide. |
| El pago se aprueba pero falla la creación de la factura | El pedido queda PAID con factura en estado PENDING y alerta; no se pierde el cobro. |
| El pago se aprueba pero falla la entrega digital | El pedido queda PAID con fulfillment FAILED y reintento; si no se resuelve en 24 h, se ofrece reembolso. |

### 4.5 Pedido, líneas, direcciones y precio congelado

#### 4.5.1 Cabecera del pedido (`orders`)

| Atributo | Tipo | Obligatorio | Regla |
|---|---|---|---|
| `id` | UUID | Sí | Clave primaria. |
| `order_number` | Texto 32 | Sí | Único y legible; no se reutiliza. |
| `checkout_group_id` | UUID | Sí | Compra a la que pertenece. |
| `buyer_id` | UUID | Sí | Comprador. |
| `seller_id` | UUID | Sí | Vendedor del pedido. |
| `status` | Enum | Sí | Estado comercial de la sección 6. |
| `payment_status` | Enum | Sí | CREATED, REQUIRES_ACTION, UNKNOWN, AUTHORIZED, CAPTURED, FAILED, PARTIALLY_REFUNDED, REFUNDED, DISPUTED. Proyección del pago asignado. |
| `fulfillment_status` | Enum | Sí | UNFULFILLED, PARTIALLY_FULFILLED, FULFILLED, FAILED. |
| `return_status` | Enum | Sí | NONE, REQUESTED, IN_TRANSIT, RECEIVED, PARTIAL, RETURNED, REJECTED. |
| `refund_status` | Enum | Sí | NONE, PENDING, PARTIALLY_REFUNDED, REFUNDED, FAILED. |
| `currency` | ISO 4217 | Sí | Moneda del pedido. |
| `subtotal` | Decimal 19,2 | Sí | Suma de líneas antes de descuentos e impuestos. |
| `discount_total` | Decimal 19,2 | Sí | Descuentos del vendedor y parte del cupón de plataforma. |
| `shipping_total` | Decimal 19,2 | Sí | Coste de envío del pedido. |
| `tax_total` | Decimal 19,2 | Sí | Suma de impuestos por línea. |
| `grand_total` | Decimal 19,2 | Sí | `subtotal - discount_total + shipping_total + tax_total`. |
| `paid_total` | Decimal 19,2 | Sí | Importe capturado asignado. |
| `refunded_total` | Decimal 19,2 | Sí | Importe reembolsado acumulado. |
| `shipping_address` | Snapshot | Condicional | Obligatorio si hay líneas físicas. |
| `billing_address` | Snapshot | Condicional | Obligatorio si se solicita factura con datos fiscales. |
| `contact_email` | Texto 254 | Sí | Snapshot para notificaciones. |
| `contact_phone` | Texto 30 | Condicional | Obligatorio para envíos. |
| `delivery_method` | Enum | Sí | STANDARD, EXPRESS, DIGITAL, PICKUP. |
| `cancellation_status` | Enum | Sí | NONE, PARTIAL, FULL. |
| `price_frozen_at` | Timestamp | Sí | Momento de congelación. |
| `pricing_snapshot` | JSON | Sí | Versión de lista de precios, promoción, moneda y reglas aplicadas. |
| `payment_id` | UUID | No | Pago asignado. |
| `payment_reference` | Texto | No | Referencia del PSP. |
| `invoice_id` | UUID | No | Factura emitida. |
| `cancellation_reason` | Texto | No | Obligatorio si está CANCELLED. |
| `internal_notes` | Texto | No | Solo operadores autorizados. |
| `created_at` | Timestamp | Sí | UTC. |
| `submitted_at` | Timestamp | Sí | Confirmación del checkout. |
| `paid_at` | Timestamp | No | Pago confirmado. |
| `accepted_at` | Timestamp | No | Vendedor aceptó. |
| `dispatched_at` | Timestamp | No | Envío. |
| `delivered_at` | Timestamp | No | Entrega. |
| `finalized_at` | Timestamp | No | Cierre. |
| `return_window_ends_at` | Timestamp | No | Calculado desde la entrega. |
| `updated_at` | Timestamp | Sí | UTC. |
| `version` | Entero | Sí | Concurrencia optimista. |

#### 4.5.2 Líneas de pedido (`order_lines`)

| Atributo | Tipo | Obligatorio | Regla |
|---|---|---|---|
| `id` | UUID | Sí | Clave primaria. |
| `order_id` | UUID | Sí | Pedido. |
| `line_number` | Entero | Sí | Correlativo dentro del pedido. |
| `product_id` | UUID | Sí | Producto. |
| `variant_id` | UUID | Sí | Variante/SKU vendida. |
| `sku` | Texto 64 | Sí | Snapshot del SKU. |
| `product_name` | Texto 200 | Sí | Snapshot del nombre. |
| `variant_attributes` | JSON | No | Snapshot de talla, color, formato, licencia. |
| `quantity` | Entero | Sí | Mayor que cero. |
| `unit_price` | Decimal 19,2 | Sí | Precio congelado. |
| `unit_price_frozen_at` | Timestamp | Sí | Momento de congelación. |
| `discount_amount` | Decimal 19,2 | Sí | Descuento asignado a la línea. |
| `tax_rate` | Decimal 5,4 | Sí | Tasa aplicada. |
| `tax_amount` | Decimal 19,2 | Sí | Impuesto de la línea. |
| `line_total` | Decimal 19,2 | Sí | `quantity * unit_price - discount + tax`. |
| `currency` | ISO 4217 | Sí | Moneda. |
| `warehouse_id` | UUID | Condicional | Obligatorio para físicos. |
| `fulfillment_type` | Enum | Sí | PHYSICAL, DIGITAL, PICKUP. |
| `shipment_id` | UUID | No | Envío asociado. |
| `entitlement_id` | UUID | No | Derecho digital asociado. |
| `line_status` | Enum | Sí | PENDING, RESERVED, PAID, PREPARING, SHIPPED, DELIVERED, CANCELLED, RETURNED, REFUNDED. |
| `returned_quantity` | Entero | Sí | Acumulado devuelto; no supera `quantity`. |
| `refunded_amount` | Decimal 19,2 | Sí | Acumulado reembolsado de la línea. |
| `created_at` | Timestamp | Sí | UTC. |
| `updated_at` | Timestamp | Sí | UTC. |
| `version` | Entero | Sí | Concurrencia optimista. |

#### 4.5.3 Dirección de entrega congelada

La dirección no se guarda como una referencia viva a la libreta del comprador. Se guarda un **snapshot** en el pedido.

| Atributo | Tipo | Obligatorio | Regla |
|---|---|---|---|
| `recipient_name` | Texto 150 | Sí | Persona que recibe; puede diferir del comprador. |
| `phone` | Texto 30 | Sí | Para incidencias y avisos del transportista. |
| `street` | Texto 200 | Sí | Vía principal. |
| `number` | Texto 20 | Sí | Número o identificador. |
| `unit` | Texto 20 | No | Piso, puerta, apartamento. |
| `complement` | Texto 200 | No | Referencia adicional. |
| `city` | Texto 100 | Sí | Ciudad o municipio. |
| `state` | Texto 100 | Condicional | Obligatorio según país. |
| `postal_code` | Texto 20 | Sí | Validado contra el país. |
| `country` | ISO 3166-1 alpha-2 | Sí | País de entrega. |
| `delivery_instructions` | Texto 500 | No | Indicaciones para el transportista. |
| `latitude` | Decimal | No | Geocodificación. |
| `longitude` | Decimal | No | Geocodificación. |
| `validation_status` | Enum | Sí | VALID, PARTIAL, INVALID, UNVERIFIED. |
| `validation_provider` | Texto | No | Proveedor de validación. |
| `validated_at` | Timestamp | No | Fecha de validación. |

**Reglas:**

- **RN-DIR-01:** La dirección se valida al confirmar el checkout. Si no se puede validar, se pide corrección; no se bloquea por una validación dudosa si el comprador confirma.
- **RN-DIR-02:** El snapshot es inmutable después del pago. Un cambio posterior se gestiona como incidencia logística y requiere aceptación del vendedor y del transportista.
- **RN-DIR-03:** No se aceptan apartados postales para productos voluminosos o de alto valor.
- **RN-DIR-04:** La dirección de facturación puede ser distinta de la de entrega.
- **RN-DIR-05:** Los pedidos solo digitales no requieren dirección de entrega.
- **RN-DIR-06:** Los datos de dirección se enmascaran en reportes y se conservan según la política de privacidad.

#### 4.5.4 Precio congelado

- **RN-CON-01:** Al confirmar el checkout se copian a la línea: `unit_price`, `currency`, `tax_rate`, `discount_amount` y `price_frozen_at`.
- **RN-CON-02:** El precio congelado es inmutable después del pago. Ni el vendedor ni el Administrador pueden cambiarlo.
- **RN-CON-03:** La suma de líneas, descuentos, envío e impuestos debe coincidir con `grand_total` y con el importe autorizado en el PSP.
- **RN-CON-04:** Si el pago se reintenta dentro del TTL, se mantiene el precio congelado. Si el TTL expira, la compra se cancela y el comprador debe reiniciar el checkout con precios actuales.
- **RN-CON-05:** Un cambio de precio del catálogo nunca altera un pedido pagado. Para corregir un error de precio se usa una nota de crédito o un reembolso, no una edición.
- **RN-CON-06:** Los impuestos se calculan con la tasa vigente en el momento de la congelación y se guardan por línea.
- **RN-CON-07:** Los redondeos se aplican por línea y se ajustan en la última línea o en el total según la regla fiscal; la suma debe cuadrar al céntimo.
- **RN-CON-08:** El pedido guarda `pricing_snapshot` con la versión de la lista de precios, la promoción y la moneda para auditoría.

#### 4.5.5 Inmutabilidad y auditoría del pedido

- Una vez pagado, solo pueden cambiar los campos de estado, fulfillment, devolución, reembolso, notas internas y referencias externas.
- Los cambios de estado se registran en `order_status_history` (`id`, `order_id`, `from_status`, `to_status`, `event`, `actor_id`, `reason`, `correlation_id`, `created_at`) con estado anterior, nuevo, actor, motivo, timestamp y correlación.
- Las correcciones de importes se hacen con movimientos financieros (reembolso, nota de crédito, ajuste), nunca sobrescribiendo el importe original.
- El pedido debe ser reconstruible: qué se compró, a qué precio, con qué impuestos, a qué dirección, con qué pago y con qué documentos.

### 4.6 Pasarela de pago

#### 4.6.1 Decisión: PSP externo integrado

**Decisión:** NexusMarket usa un **PSP externo** (por ejemplo, Stripe, Adyen, Mercado Pago o Redsys, según país) mediante:

- **Checkout alojado o componentes tokenizados** del PSP.
- **Payment Intent** o equivalente creado por el backend.
- **Webhooks firmados** para confirmar autorización, captura, reembolso y disputa.
- **Conciliación programada** contra la API del PSP.
- **Nunca** se almacenan números de tarjeta, CVV ni datos de banda magnética.

**No se usa una pasarela “simulada” en producción.** La pasarela simulada existe solo en los perfiles de desarrollo y prueba, con resultados deterministas (aprobado, rechazado, timeout, error) y webhooks simulados. Sirve para pruebas automatizadas y demos, no para cobrar.

**Por qué no una pasarela integrada propia:**

- Aumenta el alcance PCI DSS y el riesgo de seguridad.
- Exige cumplir 3DS/SCA, antifraude, chargebacks y normativa local.
- Requiere operación 24/7, conciliación y soporte de adquiriente.
- El PSP ya ofrece métodos locales, tokenización, reintentos y liquidación.

**Por qué no una pasarela puramente simulada:**

- No mueve dinero real, no cumple obligaciones financieras y no protege contra fraude.
- Impide conciliar, reembolsar y gestionar contracargos.
- Solo es aceptable como adaptador de desarrollo.

#### 4.6.2 Flujo de pago

1. El comprador confirma el checkout.
2. El backend crea la compra, los pedidos y las reservas.
3. El backend crea un pago en estado CREATED con una **clave de idempotencia** y solicita al PSP una sesión de pago por el total exacto.
4. El PSP devuelve una URL o token de checkout. El comprador es redirigido.
5. El comprador completa el pago, incluido 3DS/SCA si aplica.
6. El PSP envía un webhook firmado. El backend valida firma, importe, moneda, referencia e idempotencia.
7. Si el pago se captura, el pedido pasa a PAID, se confirman las reservas, se emite la factura y se notifica al vendedor. Si hay digitales, se emiten los derechos.
8. Si el pago falla, el pedido pasa a PAYMENT_FAILED y se permite reintento dentro del TTL.
9. Si el webhook no llega, la conciliación consulta el estado por referencia y corrige.
10. Si el pago queda en estado desconocido, no se marca PAID ni CANCELLED hasta reconciliar.

#### 4.6.3 Entidades de pago

**`payments`**:

`id`, `checkout_group_id`, `psp`, `psp_payment_id`, `idempotency_key`, `amount`, `currency`, `status`, `method`, `authorization_code`, `three_ds_status`, `captured_amount`, `refunded_amount`, `failure_code`, `failure_message`, `created_at`, `authorized_at`, `captured_at`, `updated_at`, `version`.

**`payment_events`**:

`id`, `payment_id`, `event_type`, `psp_event_id` (único), `payload_hash`, `processed_at`, `result`, `created_at`.

**`refunds`**:

`id`, `refund_number`, `payment_id`, `order_id`, `return_id`, `amount`, `currency`, `type` (FULL, PARTIAL), `method` (ORIGINAL, STORE_CREDIT), `status` (REQUESTED, APPROVED, PROCESSING, SUCCEEDED, FAILED, CANCELLED), `psp_refund_id`, `reason`, `requested_by`, `approved_by`, `approved_at`, `processed_at`, `failure_code`, `idempotency_key`, `created_at`, `updated_at`.

**`disputes`**:

`id`, `payment_id`, `psp_dispute_id`, `amount`, `currency`, `reason`, `status` (OPEN, UNDER_REVIEW, WON, LOST, CLOSED), `evidence_due_at`, `outcome`, `created_at`, `updated_at`.

**`dispute_allocations`**:

`id`, `dispute_id`, `order_id`, `seller_id`, `amount`, `status`. Permite repartir un contracargo de un pago multi-vendedor entre los pedidos afectados.

#### 4.6.4 Estados del pago

| Código | Descripción | Terminal |
|---|---|---|
| CREATED | Pago creado, aún sin interacción del comprador. | No |
| REQUIRES_ACTION | Requiere 3DS, redirección o acción del comprador. | No |
| UNKNOWN | Estado no confirmado por el PSP; requiere conciliación antes de decidir. | No |
| AUTHORIZED | Autorizado, pendiente de captura. | No |
| CAPTURED | Cobrado. | No |
| FAILED | Rechazado o error definitivo. | No |
| CANCELLED | Anulado antes de capturar. | Sí |
| PARTIALLY_REFUNDED | Reembolso parcial confirmado. | No |
| REFUNDED | Reembolso total confirmado. | Sí |
| DISPUTED | Contracargo o disputa abierta. | No |

**Transiciones:** CREATED → REQUIRES_ACTION → AUTHORIZED → CAPTURED; CREATED/REQUIRES_ACTION/AUTHORIZED → FAILED, CANCELLED o UNKNOWN; UNKNOWN → CAPTURED, FAILED o CANCELLED tras conciliación; CAPTURED → PARTIALLY_REFUNDED → REFUNDED; CAPTURED/PARTIALLY_REFUNDED → DISPUTED. Un pago FAILED puede reintentarse con un **nuevo** pago, no reutilizando el mismo `psp_payment_id`.

#### 4.6.5 Qué pasa si falla

| Escenario | Detección | Estado del pedido | Inventario | Experiencia del comprador | Recuperación |
|---|---|---|---|---|---|
| Tarjeta rechazada | Webhook o respuesta síncrona con código de rechazo | PAYMENT_FAILED | Reserva ACTIVE hasta TTL | Mensaje claro; puede reintentar con otro método | Hasta 3 intentos; luego CANCELLED y liberación |
| 3DS abandonado o fallido | Estado REQUIRES_ACTION sin completar | PENDING_PAYMENT o PAYMENT_FAILED según PSP | Reserva ACTIVE | Se le invita a completar o reintentar | Reintento dentro del TTL |
| Timeout o estado desconocido | Sin webhook y consulta con timeout | PENDING_PAYMENT con pago UNKNOWN | Reserva ACTIVE | “Estamos verificando tu pago”; no se le cobra dos veces | Conciliación cada 5 min; si no se confirma, se cancela y libera |
| PSP caído o error 5xx | Error al crear o consultar el pago | PENDING_PAYMENT | Reserva ACTIVE hasta TTL | Checkout no disponible o pago en espera | Circuit breaker, reintento y aviso; no se marca PAID sin confirmación |
| Webhook perdido | Pago sin evento en 2 min | PENDING_PAYMENT | Reserva ACTIVE | Sin cambio visible | Job de conciliación consulta el PSP por referencia |
| Webhook duplicado | Mismo `psp_event_id` | Sin cambio | Sin cambio | Sin cambio | Idempotencia por evento; se ignora el duplicado |
| Webhook con importe o moneda distintos | Validación de firma y datos | ON_HOLD | Reserva ACTIVE | “Pago en revisión” | Revisión manual; no se confirma hasta aclarar |
| Captura falla tras autorización | Error del PSP | PAYMENT_FAILED | Reserva ACTIVE hasta TTL | Se le pide otro método | Nuevo pago; la autorización se anula si es posible |
| Pago marcado como fraude | Regla antifraude o PSP | ON_HOLD | Reserva ACTIVE | “En revisión” | Supervisor/Administrador decide; si se rechaza, CANCELLED y liberación |
| Reembolso falla | Error del PSP | REFUND_PENDING | Sin cambio | “Reembolso en proceso” | Reintento exponencial; tarea manual; nunca se marca REFUNDED sin confirmación |
| Contracargo | Notificación del PSP | Estado comercial sin cambio; pago DISPUTED | Sin cambio | Notificación y solicitud de evidencia | Responder en plazo; si se pierde, débito al vendedor y registro financiero |

**Reglas de fallo:**

- **RN-PAY-01:** Un pago solo se considera aprobado con confirmación del PSP, nunca por una respuesta del navegador.
- **RN-PAY-02:** Un pedido nunca pasa a PAID si el importe o la moneda no coinciden exactamente con el total congelado.
- **RN-PAY-03:** Un pago en estado desconocido no se marca como fallido ni como aprobado hasta conciliar.
- **RN-PAY-04:** Un reintento crea un nuevo pago; no se reutiliza el identificador del anterior.
- **RN-PAY-05:** El comprador puede reintentar hasta 3 veces dentro de los 15 minutos. Después, el pedido se cancela y se liberan las reservas.
- **RN-PAY-06:** Si el PSP no responde, se aplica timeout, circuit breaker y reintento; no se bloquea el hilo de la petición.
- **RN-PAY-07:** Los webhooks se validan por firma, IP o secreto compartido, y se procesan de forma idempotente.
- **RN-PAY-08:** Los eventos de pago se guardan con hash del payload para auditoría y reproducción.
- **RN-PAY-09:** Un reembolso no se marca como completado hasta que el PSP lo confirme. Si falla, queda REFUND_PENDING y se alerta.
- **RN-PAY-10:** Los contracargos se gestionan como disputa financiera; no cambian automáticamente el estado comercial del pedido.

#### 4.6.6 Captura, moneda y métodos

- **Captura:** automática al autorizar, para simplificar la operación. Si el negocio requiere captura diferida, se permite authorize + capture dentro de 48 h, pero el pedido no pasa a PAID hasta la captura.
- **Moneda:** una por checkout. El PSP debe soportar la moneda; si no, se rechaza el checkout antes de crear el pago.
- **Métodos:** tarjeta, wallets y métodos locales del PSP. Efectivo contra reembolso y transferencia manual quedan fuera del MVP.
- **3DS/SCA:** obligatorio cuando la jurisdicción o el PSP lo exijan. Un fallo de 3DS no es un fraude confirmado; es un intento fallido.
- **Tokenización:** el PSP devuelve un token; NexusMarket guarda solo el token y los últimos cuatro dígitos, nunca el PAN.
- **PCI DSS:** alcance SAQ A o equivalente; el checkout alojado reduce el alcance.
- **Liquidación:** el PSP liquida a la cuenta de la plataforma; la plataforma distribuye a vendedores según el ledger de liquidación.

#### 4.6.7 Conciliación

- **RN-PAY-11:** Diariamente se concilia el ledger de pagos contra el reporte del PSP: cobros, reembolsos, comisiones, contracargos y liquidaciones.
- **RN-PAY-12:** Las diferencias se registran como incidencias con importe, referencia y responsable; no se ajustan silenciosamente.
- **RN-PAY-13:** La conciliación es idempotente y se puede repetir sin duplicar movimientos.
- **RN-PAY-14:** Un pedido PAID sin pago CAPTURED, o un pago CAPTURED sin pedido, es una excepción crítica y se alerta.

### 4.7 Entrega de productos digitales

#### 4.7.1 Objetivo

Entregar productos digitales de forma inmediata, segura, verificable y revocable, sin dirección física ni transportista.

#### 4.7.2 Entidad `entitlements` (derecho digital)

| Atributo | Tipo | Obligatorio | Regla |
|---|---|---|---|
| `id` | UUID | Sí | Clave primaria. |
| `order_line_id` | UUID | Sí | Único: un derecho por línea digital. |
| `buyer_id` | UUID | Sí | Titular. |
| `product_id` | UUID | Sí | Producto. |
| `variant_id` | UUID | Sí | Variante/licencia. |
| `type` | Enum | Sí | DOWNLOAD, LICENSE_KEY, STREAMING_ACCESS, COURSE_ACCESS. |
| `status` | Enum | Sí | PENDING, ISSUED, ACCESSED, REVOKED, EXPIRED. |
| `license_key_encrypted` | Texto | No | Solo para LICENSE_KEY; cifrado. |
| `download_url_token` | Texto | No | Token firmado, no URL permanente. |
| `max_downloads` | Entero | No | Por defecto 5; configurable. |
| `download_count` | Entero | Sí | Se incrementa de forma atómica. |
| `access_expires_at` | Timestamp | No | Nulo si es perpetuo. |
| `issued_at` | Timestamp | No | Emisión. |
| `first_accessed_at` | Timestamp | No | Para política de devolución. |
| `revoked_at` | Timestamp | No | Revocación. |
| `revocation_reason` | Texto | No | Reembolso, fraude, orden judicial. |
| `created_at` | Timestamp | Sí | UTC. |
| `updated_at` | Timestamp | Sí | UTC. |
| `version` | Entero | Sí | Concurrencia optimista. |

#### 4.7.3 Flujo de entrega

1. El pago se confirma y el pedido pasa a PAID.
2. Por cada línea digital se crea un derecho en estado PENDING.
3. El sistema genera el recurso: clave de licencia, token de descarga o acceso al contenido.
4. El derecho pasa a ISSUED y la línea a DELIVERED.
5. El comprador recibe un correo con un enlace a “Mi biblioteca”; **no se envían claves ni archivos adjuntos**.
6. En “Mi biblioteca” el comprador ve el producto, el estado, la fecha de emisión, las descargas restantes y las condiciones.
7. Al descargar o acceder, se valida el token, se incrementa el contador y se registra el evento.
8. Si el pedido se reembolsa, el derecho se revoca y el acceso se bloquea.

#### 4.7.4 Reglas de entrega digital

- **RN-DIG-01:** La entrega digital se emite **solo después de la confirmación del pago**, nunca antes.
- **RN-DIG-02:** No se exige dirección de entrega para pedidos solo digitales.
- **RN-DIG-03:** El enlace de descarga es firmado, de vida corta (15 minutos) y de un solo uso por solicitud; se puede regenerar desde la biblioteca.
- **RN-DIG-04:** El número máximo de descargas es configurable por variante. Agotado el límite, el comprador puede solicitar regeneración con revisión antifraude.
- **RN-DIG-05:** Las claves de licencia se muestran una vez y se guardan cifradas; no se envían por correo en texto plano.
- **RN-DIG-06:** El acceso puede tener caducidad (curso, suscripción) o ser perpetuo. La fecha se muestra antes de comprar.
- **RN-DIG-07:** El derecho es personal e intransferible salvo que el vendedor permita regalo; en el MVP no se permite transferencia.
- **RN-DIG-08:** Se registran accesos, IP, user agent y fecha para auditoría y antifraude, con minimización de datos.
- **RN-DIG-09:** Si la emisión falla, el pedido queda PAID con `fulfillment_status = FAILED`, se reintenta y se alerta. No se marca DELIVERED.
- **RN-DIG-10:** Si la emisión no se resuelve en 24 h, se ofrece reembolso o entrega manual; el comprador no queda sin producto ni sin dinero.
- **RN-DIG-11:** Un pedido mixto (físico + digital) entrega el digital al pagar y mantiene el estado del pedido en PREPARING hasta que el físico se entregue. La línea digital sí queda DELIVERED.
- **RN-DIG-12:** Si todas las líneas son digitales y se emiten correctamente, el pedido pasa automáticamente a DELIVERED.
- **RN-DIG-13:** La ventana de devolución digital es de 14 días desde la entrega y solo aplica si no se ha accedido al contenido o si es defectuoso/no conforme. La ley aplicable prevalece.
- **RN-DIG-14:** Al reembolsar, el derecho se revoca y se desactiva la licencia. Si el comprador ya descargó, se registra la revocación y se aplica la política de abuso.
- **RN-DIG-15:** No se permite comprar dos veces la misma licencia en la misma compra sin advertencia; el comprador debe confirmar.
- **RN-DIG-16:** Los productos digitales tienen tratamiento fiscal propio y se facturan con la categoría correcta.

#### 4.7.5 Estados del derecho digital

PENDING → ISSUED → ACCESSED → EXPIRED; ISSUED/ACCESSED → REVOKED. EXPIRED y REVOKED son terminales. Una revocación por reembolso es irreversible salvo nuevo pago y nueva emisión.

#### 4.7.6 Seguridad de la entrega

- URLs firmadas con expiración y alcance por usuario y línea.
- Validación de sesión y titularidad en cada acceso.
- Límite de intentos y rate limiting.
- Almacenamiento en object storage privado; nunca en el servidor web público.
- Cifrado en reposo y en tránsito.
- Registro de accesos y alertas por descargas anómalas.
- DRM o marca de agua opcional para contenido de alto valor.

### 4.8 Facturación

#### 4.8.1 Objetivo

Emitir, entregar, rectificar y conservar los documentos fiscales de cada venta, conciliar los cobros y liquidar a los vendedores, cumpliendo la normativa aplicable.

#### 4.8.2 Participantes

| Participante | Responsabilidad |
|---|---|
| Comprador | Proporcionar datos fiscales correctos y descargar sus documentos. |
| Vendedor | Ser el sujeto fiscal de sus ventas; revisar sus facturas y liquidaciones. |
| Administrador (perfil Finanzas) | Configurar impuestos, comisiones, series y datos legales; rectificar y liquidar. |
| Supervisor | Consultar en modo lectura y aprobar excepciones dentro de umbral. |
| PSP | Cobrar, reembolsar y entregar el reporte de liquidación. |
| Autoridad fiscal | Recibir facturas electrónicas cuando aplique. |
| Sistema | Emitir, numerar, firmar, enviar y conciliar automáticamente. |

#### 4.8.3 Entidades

**`invoices`**: `id`, `invoice_number`, `series`, `country`, `tax_regime`, `order_id`, `seller_id`, `buyer_id`, `buyer_legal_name`, `buyer_tax_id`, `buyer_address`, `issue_date`, `currency`, `exchange_rate`, `subtotal`, `discount_total`, `tax_total`, `total`, `status` (DRAFT, ISSUED, CREDITED), `pdf_url`, `xml_url`, `hash`, `external_id`, `issued_at`, `created_at`.

**`invoice_lines`**: `id`, `invoice_id`, `order_line_id`, `description`, `quantity`, `unit_price`, `discount`, `tax_rate`, `tax_amount`, `line_total`, `tax_class`.

**`credit_notes`**: `id`, `credit_note_number`, `invoice_id`, `reason`, `subtotal`, `tax_total`, `total`, `status`, `issued_at`, `psp_refund_id`, `created_at`.

**`settlements`**: `id`, `seller_id`, `period_start`, `period_end`, `gross_sales`, `commission_total`, `payment_fees`, `shipping_collected`, `refunds`, `chargebacks`, `adjustments`, `net_payable`, `currency`, `status` (PENDING, APPROVED, PAID, FAILED), `payout_ref`, `paid_at`, `created_at`.

**`tax_rates`**: `id`, `country`, `region`, `tax_class`, `rate`, `effective_from`, `effective_to`, `legal_reference`.

#### 4.8.4 Reglas de facturación

- **RN-FAC-01:** Se emite **una factura por pedido de vendedor**, no una por compra multi-vendedor. El comprador recibe además un recibo de la compra.
- **RN-FAC-02:** La factura se emite al confirmar el pago. Un pedido no pagado no genera factura.
- **RN-FAC-03:** La numeración es correlativa por vendedor, serie y ejercicio; no puede tener huecos ni reutilizar números. La generación debe ser segura ante concurrencia.
- **RN-FAC-04:** La factura es inmutable una vez emitida. Cualquier corrección se hace con **nota de crédito**, no editando la factura.
- **RN-FAC-05:** El total de la factura debe coincidir con el total del pedido. Si no coincide, es un error crítico y no se emite.
- **RN-FAC-06:** Los impuestos se calculan por línea según país, región, clase fiscal y vigencia en la fecha de la operación. Se guarda el desglose.
- **RN-FAC-07:** La factura incluye datos fiscales del comprador y del vendedor, dirección, concepto, cantidad, precio unitario, descuentos, base, tasa, impuesto y total.
- **RN-FAC-08:** Una cancelación antes de la factura no genera factura. Una cancelación después de la factura genera nota de crédito.
- **RN-FAC-09:** Un reembolso total genera nota de crédito por el total; uno parcial, por la parte reembolsada. La nota de crédito referencia la factura original.
- **RN-FAC-10:** La factura se envía al comprador y se pone a disposición del vendedor. El comprador solo ve sus facturas; el vendedor, las suyas; el Supervisor y el Administrador, todas según alcance.
- **RN-FAC-11:** Los documentos se conservan durante el plazo legal (por defecto 10 años) y no se eliminan por baja de la cuenta.
- **RN-FAC-12:** En jurisdicciones con factura electrónica, se genera XML/UBL/CFDI según corresponda, se firma y se envía a la autoridad. Si el envío falla, se reintenta y se alerta; la factura no se considera válida hasta la aceptación.
- **RN-FAC-13:** El motor fiscal debe soportar operaciones exentas, inversión del sujeto pasivo y tipos reducidos. Si no sabe calcular, bloquea la venta en esa jurisdicción.
- **RN-FAC-14:** Los redondeos se aplican con precisión decimal y se documentan; la suma de líneas debe cuadrar con el total.
- **RN-FAC-15:** La comisión de plataforma se factura al vendedor según el contrato y se descuenta en la liquidación.
- **RN-FAC-16:** La liquidación al vendedor se calcula por pedido y por periodo: ventas brutas menos comisión, costes de pago, envíos cobrados, reembolsos, contracargos y ajustes.
- **RN-FAC-17:** La liquidación se paga después de la entrega y del periodo de retención (por defecto 7 días), con una reserva de garantía del 10% hasta el cierre de la ventana de devolución. La reserva cubre devoluciones y contracargos.
- **RN-FAC-18:** Un saldo negativo por reembolsos o contracargos se compensa con liquidaciones futuras; no se exige pago inmediato salvo cierre de cuenta.
- **RN-FAC-19:** La liquidación es auditable y conciliable con pagos, facturas y notas de crédito.
- **RN-FAC-20:** El Administrador puede anular una factura solo mediante nota de crédito y con motivo; el Supervisor no puede anular.

### 4.9 Logística

#### 4.9.1 Objetivo

Entregar los productos físicos en el plazo y condiciones prometidos, con trazabilidad, prueba de entrega, gestión de incidencias y logística inversa.

#### 4.9.2 Participantes

| Participante | Responsabilidad |
|---|---|
| Comprador | Proporcionar dirección válida, elegir servicio y recibir. |
| Vendedor | Preparar y embalar; elegir transportista y servicio; asumir el coste. |
| Operador logístico | Recibir, preparar, etiquetar, despachar, entregar y registrar incidencias. |
| Transportista | Recoger, transportar, entregar y reportar eventos. |
| Supervisor | Autorizar excepciones, reenvíos e incidencias dentro de umbral. |
| Administrador | Configurar transportistas, tarifas, almacenes y políticas. |
| Sistema | Calcular tarifas, generar etiquetas, consumir webhooks y actualizar estados. |

#### 4.9.3 Entidades

**`shipments`**: `id`, `shipment_number`, `order_id`, `seller_id`, `warehouse_id`, `carrier_id`, `service_level`, `tracking_number`, `label_url`, `status`, `package_count`, `weight_grams`, `dimensions`, `shipping_cost`, `insured_value`, `pickup_at`, `dispatched_at`, `estimated_delivery_at`, `delivered_at`, `attempts`, `recipient_name`, `pod_type`, `pod_url`, `otp_verified`, `incident_reason`, `sla_due_at`, `created_at`, `updated_at`, `version`.

**`shipment_events`**: `id`, `shipment_id`, `status`, `occurred_at`, `location`, `description`, `source`, `raw_payload_hash`, `created_at`.

**`carriers`**: `id`, `name`, `code`, `services`, `tracking_url`, `api_config_ref`, `sla_days`, `active`.

**`shipping_rates`**: `id`, `carrier_id`, `origin_zone`, `destination_zone`, `weight_from`, `weight_to`, `service_level`, `price`, `currency`, `valid_from`, `valid_to`.

**`shipment_incidents`**: `id`, `shipment_id`, `type` (LOST, DAMAGED, DELAYED, ADDRESS, REFUSED, CUSTOMS), `description`, `status`, `resolution`, `claim_amount`, `opened_at`, `resolved_at`.

#### 4.9.4 Estados del envío

CREATED → LABEL_READY → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED. Desde PICKED_UP/IN_TRANSIT/OUT_FOR_DELIVERY: DELIVERY_FAILED → reschedule o RETURNED_TO_SENDER. Desde cualquier estado abierto: LOST o DAMAGED. DELIVERED, RETURNED_TO_SENDER y LOST son terminales salvo reenvío con nuevo envío.

#### 4.9.5 Reglas logísticas

- **RN-LOG-01:** Solo los pedidos con líneas físicas generan envío.
- **RN-LOG-02:** El envío se crea al pasar el pedido a PREPARING, no antes de pagar.
- **RN-LOG-03:** Un pedido puede tener varios envíos si hay varios almacenes o paquetes; cada envío tiene su tracking.
- **RN-LOG-04:** No se puede despachar sin etiqueta, tracking y transportista asignado.
- **RN-LOG-05:** El coste de envío se congela en el checkout. Un cambio de tarifa posterior no altera el pedido.
- **RN-LOG-06:** La dirección se valida antes de generar la etiqueta; si el transportista la rechaza, se pide corrección antes del despacho.
- **RN-LOG-07:** La entrega se confirma con prueba: firma, foto, OTP o confirmación del transportista. Para importes superiores a 200 € se exige OTP o firma.
- **RN-LOG-08:** El comprador recibe el tracking y las notificaciones de cambio de estado.
- **RN-LOG-09:** Si hay un intento fallido, se registra y se reprograma. Tras 3 intentos, el paquete vuelve al almacén y se contacta al comprador.
- **RN-LOG-10:** Si el paquete se pierde o daña en tránsito, se abre incidencia, se investiga con el transportista y se resuelve con reenvío o reembolso.
- **RN-LOG-11:** El operador logístico no puede modificar precios, facturas ni datos del comprador; solo datos de envío y estado.
- **RN-LOG-12:** Los webhooks del transportista son idempotentes y se validan por firma o secreto.
- **RN-LOG-13:** La entrega marca el pedido como DELIVERED y activa la ventana de devolución.
- **RN-LOG-14:** Un envío devuelto al remitente no se considera una devolución voluntaria; se gestiona como incidencia y puede terminar en reembolso.
- **RN-LOG-15:** Los productos restringidos, perecederos o de gran volumen requieren transportista y embalaje específicos; el checkout no los ofrece si no hay servicio.
- **RN-LOG-16:** La logística inversa usa el mismo transportista o uno acordado; la etiqueta de devolución la genera la plataforma.
- **RN-LOG-17:** El SLA de preparación es de 48 h desde la aceptación; el de entrega, el comprometido en el checkout. Los incumplimientos se registran y afectan la reputación del vendedor.
- **RN-LOG-18:** Un envío no puede modificarse después de la entrega; las correcciones se hacen con eventos e incidencias.

### 4.10 Devoluciones y reembolsos

#### 4.10.1 Objetivo

Gestionar solicitudes de devolución, inspección, reposición de stock y reembolso de forma consistente, trazable y conforme a la ley, incluyendo disputas y contracargos.

#### 4.10.2 Participantes

| Participante | Responsabilidad |
|---|---|
| Comprador | Solicitar la devolución, aportar evidencia y enviar el producto. |
| Vendedor | Aprobar o rechazar según política; inspeccionar y reponer. |
| Operador logístico | Recibir, registrar y clasificar el estado del producto. |
| Supervisor | Aprobar excepciones y reembolsos dentro de su umbral. |
| Administrador (Finanzas) | Aprobar importes superiores, configurar política y gestionar contracargos. |
| PSP | Ejecutar el reembolso y confirmar el resultado. |
| Sistema | Calcular importes, generar etiquetas, emitir notas de crédito y conciliar. |

#### 4.10.3 Entidades

**`return_requests`**: `id`, `rma_number`, `order_id`, `order_line_id`, `buyer_id`, `seller_id`, `quantity`, `reason_code`, `description`, `evidence_urls`, `status`, `requested_at`, `sla_due_at`, `approved_at`, `approved_by`, `rejection_reason`, `pickup_shipment_id`, `received_at`, `inspected_at`, `inspected_by`, `inspection_result`, `restock_quantity`, `damaged_quantity`, `refund_id`, `created_at`, `updated_at`, `version`.

**`return_events`**: `id`, `return_id`, `status`, `actor_id`, `notes`, `created_at`.

**`refunds`**: ver 4.6.3.

**`disputes`**: ver 4.6.3.

#### 4.10.4 Estados de la devolución

REQUESTED → UNDER_REVIEW → APPROVED o REJECTED; APPROVED → LABEL_ISSUED → IN_TRANSIT → RECEIVED → INSPECTED → ACCEPTED o REJECTED_INSPECTION; ACCEPTED → REFUNDED o CLOSED. REJECTED y CLOSED son terminales. Una devolución rechazada en inspección puede reenviarse al comprador y cerrarse.

#### 4.10.5 Reglas de devolución y reembolso

- **RN-DEV-01:** El plazo general es de **30 días naturales desde la entrega**. Para digitales, 14 días desde la entrega y solo si no se accedió o si hay defecto. La ley aplicable prevalece.
- **RN-DEV-02:** Solo se puede devolver cantidad no devuelta previamente y no superior a la comprada.
- **RN-DEV-03:** El producto debe estar en condiciones razonables, con accesorios y embalaje cuando sea exigible. La ley permite probar el producto; no exige que esté precintado salvo excepciones legales.
- **RN-DEV-04:** No son devolvibles, salvo defecto: productos personalizados, perecederos, higiene abiertos, contenido digital accedido, lotería y servicios ya prestados. Se informa antes de comprar.
- **RN-DEV-05:** La solicitud requiere motivo y, para daños o error, evidencia fotográfica.
- **RN-DEV-06:** El vendedor revisa en 48 h. Si no responde, se autoaprueba para no perjudicar al comprador.
- **RN-DEV-07:** El comprador paga el envío de devolución salvo defecto, producto erróneo o daño imputable al vendedor. La plataforma puede ofrecer etiqueta y cobrarla o descontarla.
- **RN-DEV-08:** La inspección se realiza en 3 días hábiles desde la recepción. Si es vendible, se repone; si no, se marca dañado y se informa.
- **RN-DEV-09:** El reembolso se solicita en 3 días hábiles desde la aprobación o inspección. El PSP puede tardar 5–10 días hábiles en reflejarlo.
- **RN-DEV-10:** El reembolso se hace por el método original. El vale de compra es opcional y requiere consentimiento; puede incluir un incentivo.
- **RN-DEV-11:** El importe reembolsado no puede superar lo capturado menos lo ya reembolsado. Se calcula por línea, con su parte proporcional de descuento e impuesto.
- **RN-DEV-12:** El envío se reembolsa si el producto es defectuoso, erróneo o no conforme; en devoluciones por cambio de opinión, solo si la política lo indica.
- **RN-DEV-13:** Una comisión de reposición solo se aplica si está informada antes de la compra, no supera el 10% y no aplica a defectuosos. Por defecto es 0.
- **RN-DEV-14:** Un reembolso parcial no cierra el pedido como REFUNDED; queda RETURNED o DELIVERED con `refund_status = PARTIALLY_REFUNDED`. Solo el reembolso total lleva a REFUNDED.
- **RN-DEV-15:** Todo reembolso genera nota de crédito si la factura ya fue emitida.
- **RN-DEV-16:** La devolución de un digital revoca el derecho y desactiva la licencia; si ya se accedió, se aplica la política de abuso.
- **RN-DEV-17:** Los reembolsos son idempotentes por `idempotency_key` y por referencia del PSP. Un reintento no duplica el dinero.
- **RN-DEV-18:** Si el PSP falla, el reembolso queda REFUND_PENDING, se reintenta y se alerta; no se marca REFUNDED hasta la confirmación.
- **RN-DEV-19:** Los contracargos se responden con evidencia (entrega, términos, comunicaciones). Si se pierden, se debitan del ledger del vendedor y se registran.
- **RN-DEV-20:** El abuso reiterado de devoluciones puede limitar la cuenta tras revisión del Supervisor; nunca se aplica automáticamente sin evidencia.
- **RN-DEV-21:** El stock se repone solo después de la inspección; nunca al aprobar la solicitud.
- **RN-DEV-22:** La devolución de un pedido multi-vendedor es independiente por pedido; no afecta a los demás.

#### 4.10.6 Matriz de aprobación de reembolsos

| Importe acumulado del pedido | Aprueba | Ejecuta |
|---|---|---|
| Hasta 500 € | Vendedor, dentro de política | Sistema, tras aprobación |
| 500,01 € a 2.000 € | Supervisor | Sistema, tras aprobación |
| Más de 2.000 € | Administrador (Finanzas) | Sistema, tras aprobación |
| Excepción, fraude o disputa | Supervisor y Administrador (cuatro ojos) | Sistema o Finanzas |
| Reembolso manual por fallo del PSP | Administrador (Finanzas) | Administrador (Finanzas) |

Los umbrales son configurables y se expresan en la moneda del pedido convertida a la moneda de control.

### 4.11 Reportes

#### 4.11.1 Objetivo

Proporcionar información confiable, oportuna y con alcance adecuado para operar, vender, liquidar, detectar fraude y cumplir obligaciones.

#### 4.11.2 Participantes

| Participante | Responsabilidad |
|---|---|
| Comprador | Consultar solo su historial y documentos; no accede a reportes globales. |
| Vendedor | Ver sus ventas, stock, devoluciones, SLA y liquidaciones. |
| Operador logístico | Ver envíos, incidencias y SLA de su ámbito. |
| Supervisor | Ver reportes globales y aprobar excepciones. |
| Administrador | Ver todo, configurar definiciones y exportar. |
| Sistema | Calcular, cachear, versionar y auditar ejecuciones. |

#### 4.11.3 Entidades

**`report_definitions`**: `id`, `code`, `name`, `domain`, `description`, `metrics`, `dimensions`, `filters`, `granularity`, `scope`, `freshness`, `owner`, `version`, `status`.

**`report_runs`**: `id`, `definition_id`, `parameters`, `requested_by`, `requested_at`, `status` (QUEUED, RUNNING, COMPLETED, FAILED, EXPIRED), `format`, `row_count`, `file_url`, `hash`, `expires_at`, `completed_at`.

**`metrics`**: `code`, `name`, `formula`, `source`, `owner`, `version`.

**`dashboards`**: `id`, `name`, `audience`, `widgets`, `scope`, `updated_at`.

#### 4.11.4 Reglas de reportes

- **RN-REP-01:** Todo reporte respeta rol, alcance y ownership. Un vendedor solo ve sus datos; un operador logístico, los envíos asignados; el Supervisor y el Administrador, el alcance global.
- **RN-REP-02:** Se aplica minimización de datos: no se muestran documentos de identidad, PAN, direcciones completas ni datos personales innecesarios.
- **RN-REP-03:** Los agregados con menos de 5 compradores o pedidos se suprimen o agrupan para evitar reidentificación.
- **RN-REP-04:** Las definiciones de métricas son únicas y versionadas. Cambiar una fórmula exige nueva versión y fecha de vigencia.
- **RN-REP-05:** Las fechas se almacenan en UTC y se presentan en la zona del usuario; los cierres financieros usan la zona del marketplace.
- **RN-REP-06:** Los reportes operativos tienen frescura de 5–15 minutos; los financieros, de T+1 tras el cierre. La frescura se muestra en la interfaz.
- **RN-REP-07:** Los reportes financieros deben reconciliar con facturas, pagos, reembolsos y liquidaciones. Una diferencia se marca, no se oculta.
- **RN-REP-08:** Las exportaciones se auditan, se marcan con el usuario y la fecha, y usan enlaces que expiran.
- **RN-REP-09:** No se permite consulta SQL directa de usuarios de negocio; solo definiciones aprobadas.
- **RN-REP-10:** Los reportes no modifican datos de origen.
- **RN-REP-11:** Los datos tardíos se reflejan con una marca de reexpresión; no se reescribe silenciosamente el pasado.
- **RN-REP-12:** La retención de dashboards es de 24 meses; la de reportes financieros, la legal aplicable.

#### 4.11.5 Catálogo mínimo de métricas

| Métrica | Fórmula | Dominio |
|---|---|---|
| GMV | Suma de ventas brutas de pedidos pagados, antes de comisiones y reembolsos. | Comercial |
| Ingreso neto | GMV menos reembolsos, contracargos y comisiones de pago. | Finanzas |
| Take rate | Comisión de plataforma / GMV. | Finanzas |
| Pedidos pagados | Conteo de pedidos con pago capturado. | Comercial |
| Ticket medio | GMV / pedidos pagados. | Comercial |
| Tasa de conversión | Checkouts pagados / sesiones o carritos iniciados. | Comercial |
| Tasa de cancelación | Pedidos cancelados / pedidos creados. | Operación |
| Tasa de devolución | Unidades devueltas / unidades entregadas. | Postventa |
| Tasa de reembolso | Importe reembolsado / GMV. | Finanzas |
| Cumplimiento de despacho | Pedidos despachados dentro del SLA / pedidos pagados. | Logística |
| Cumplimiento de entrega | Entregas a tiempo / entregas totales. | Logística |
| Stockout | SKU sin disponible / SKU publicados. | Inventario |
| Sell-through | Unidades vendidas / unidades disponibles en el periodo. | Inventario |
| Éxito de pago | Pagos capturados / intentos de pago. | Pagos |
| Contracargos | Importe de contracargos / GMV. | Riesgo |
| Días de cobro | Cuentas por cobrar / ventas diarias. | Finanzas |
| Precisión de liquidación | Liquidaciones conciliadas sin diferencia / total. | Finanzas |

---

## 5. Reglas transversales

### 5.1 Dinero, impuestos y redondeo

- **RN-GEN-01:** Los importes se almacenan en decimal con precisión 19,2 (o la escala de la moneda) y se calculan con aritmética decimal, nunca con coma flotante.
- **RN-GEN-02:** La moneda se guarda en cada importe relevante; no se asume la moneda del servidor.
- **RN-GEN-03:** El redondeo es half-up a la escala de la moneda, aplicado por línea y ajustado en el total para que la suma cuadre.
- **RN-GEN-04:** El importe cobrado debe ser exactamente igual al total congelado. Una diferencia de un céntimo bloquea el pago.
- **RN-GEN-05:** Los impuestos se calculan con la legislación y fecha de la operación; un cambio posterior de tasa no altera pedidos pagados.
- **RN-GEN-06:** Las comisiones y liquidaciones se calculan por pedido, con su propia moneda y tipo de cambio registrado.

### 5.2 Tiempo

- **RN-GEN-07:** Todas las fechas se almacenan en UTC. La presentación usa la zona del usuario; los cierres financieros usan la zona del marketplace.
- **RN-GEN-08:** Los plazos legales se calculan en días naturales; los operativos, en días hábiles, salvo indicación contraria.
- **RN-GEN-09:** Las ventanas (TTL de reserva, reintentos, aceptación del vendedor, devolución) se configuran por marketplace y se documentan en la interfaz.

### 5.3 Idempotencia y eventos

- **RN-GEN-10:** Checkout, pago, webhook, reembolso, emisión de factura y emisión de derecho digital son idempotentes por clave o por identificador externo.
- **RN-GEN-11:** Los cambios de estado publican eventos de dominio con `event_id`, `aggregate_id`, `occurred_at` y correlación. Los consumidores son idempotentes.
- **RN-GEN-12:** Se usa patrón outbox o equivalente para no perder eventos entre la transacción y la publicación.
- **RN-GEN-13:** Los webhooks y eventos se guardan con hash del payload y resultado de procesamiento.

### 5.4 Auditoría

- **RN-GEN-14:** Toda operación sensible (pago, reembolso, cambio de estado, precio, permiso, documento, factura) deja un registro append-only con actor, fecha, IP, correlación, valor anterior y valor nuevo.
- **RN-GEN-15:** La auditoría no se puede editar ni eliminar desde la aplicación; solo se archiva según retención legal.
- **RN-GEN-16:** Los logs no contienen PAN, CVV, documentos completos, contraseñas ni tokens de descarga.

### 5.5 Seguridad y privacidad

- **RN-GEN-17:** Autenticación con JWT de vida corta y refresh token rotatorio; revocación por usuario y dispositivo.
- **RN-GEN-18:** MFA obligatorio para Administrador, Supervisor y Operador logístico.
- **RN-GEN-19:** Cifrado en tránsito (TLS) y en reposo para datos sensibles; secretos en gestor de secretos.
- **RN-GEN-20:** Principio de mínimo privilegio y separación de funciones.
- **RN-GEN-21:** Rate limiting y bloqueo progresivo ante intentos de login, pago o descarga anómalos.
- **RN-GEN-22:** Privacidad por diseño: minimización, finalidad, consentimiento, retención y derechos del titular (acceso, rectificación, supresión, portabilidad, oposición).
- **RN-GEN-23:** Los datos personales no se usan para reportes agregados que permitan reidentificación; se aplica k-anonimato mínimo de 5.
- **RN-GEN-24:** Los encargados de tratamiento (PSP, transportista, KYC, email) se documentan con contrato y garantías.

### 5.6 Consistencia y disponibilidad

- **RN-GEN-25:** Inventario y pagos requieren consistencia fuerte; reportes, búsqueda y notificaciones pueden ser eventuales.
- **RN-GEN-26:** Un pedido no se considera pagado sin confirmación del PSP; un reembolso no se considera completado sin confirmación del PSP.
- **RN-GEN-27:** La indisponibilidad de un proveedor externo no debe corromper el estado; se aplica timeout, reintento, circuit breaker y compensación.
- **RN-GEN-28:** Los jobs de recuperación son idempotentes y auditables.

---

## 6. Máquina de estados del pedido

### 6.1 Principios

1. El **estado del pedido** representa el ciclo comercial y de fulfillment. Los estados financieros, de envío, de devolución y de derecho digital se gestionan en sub-máquinas y se reflejan en campos específicos.
2. No se permite saltar estados ni retroceder, salvo las transiciones explícitas de rechazo, reintento o corrección.
3. Toda transición tiene **actor autorizado, guarda, efecto lateral y registro de auditoría**.
4. Un pedido pagado es inmutable en precios y datos comerciales; solo cambian estados, referencias y datos operativos permitidos.
5. Las transiciones parciales se resuelven a nivel de línea; el estado del pedido se actualiza cuando se cumple la condición agregada.
6. Los estados terminales no admiten nuevas transiciones; una reclamación posterior se gestiona como caso de postventa o garantía, fuera de esta máquina.

### 6.2 Catálogo de estados

| Código | Etiqueta | Descripción | Terminal |
|---|---|---|---|
| PENDING_PAYMENT | Pendiente de pago | Checkout confirmado, reservas activas, pago iniciado. | No |
| PAYMENT_FAILED | Pago rechazado | El PSP rechazó o no completó el pago; se puede reintentar. | No |
| ON_HOLD | En revisión | Retenido por antifraude, discrepancia o revisión manual. | No |
| PAID | Pagado | Pago capturado; pedido confirmado. | No |
| PREPARING | En preparación | Vendedor aceptó y prepara el pedido. | No |
| DISPATCHED | Enviado | Entregado al transportista con tracking. | No |
| DELIVERED | Entregado | Prueba de entrega registrada. | No |
| FINALIZED | Finalizado | Ventana de devolución cerrada y pedido cerrado. | Sí |
| CANCELLED | Cancelado | Cancelación comercial; si hubo cobro, continúa a reembolso. | Condicional |
| RETURN_REQUESTED | Devolución solicitada | El comprador solicitó devolución. | No |
| RETURN_IN_TRANSIT | Devolución en tránsito | Devolución aprobada y en camino. | No |
| RETURN_RECEIVED | Devolución recibida | El almacén recibió el producto; pendiente de inspección. | No |
| RETURNED | Devuelto | Inspección aceptada; producto devuelto. | Condicional |
| REFUND_PENDING | Reembolso pendiente | Reembolso aprobado, pendiente de confirmación del PSP. | No |
| REFUNDED | Reembolsado | Reembolso total confirmado; pedido cerrado. | Sí |

**Estados terminales:** FINALIZED, REFUNDED y CANCELLED cuando no hay importe capturado. RETURNED es terminal si no hay reembolso asociado (por ejemplo, cambio o reposición); en el MVP toda devolución aceptada genera reembolso, por lo que normalmente pasa a REFUND_PENDING.

### 6.3 Transiciones válidas

| # | Desde | Hasta | Evento / disparador | Actor | Guardas | Efectos laterales |
|---|---|---|---|---|---|---|
| T01 | — | PENDING_PAYMENT | Checkout confirmado | Comprador | Carrito válido, stock reservado, pago creado | Crea compra, N pedidos, reservas y pago; congela precios y direcciones; `available` baja y `reserved` sube. |
| T02 | PENDING_PAYMENT | PAYMENT_FAILED | Pago rechazado | PSP | Código de rechazo recibido | Marca pago FAILED; notifica; conserva reserva hasta TTL. |
| T03 | PAYMENT_FAILED | PENDING_PAYMENT | Reintento de pago | Comprador | Intentos < 3 y TTL vigente | Crea nuevo pago; mantiene reserva y precio congelado. |
| T04 | PENDING_PAYMENT | CANCELLED | Cancelación del comprador o timeout | Comprador / Sistema | Sin captura; TTL vencido o cancelación explícita | Anula pago; libera reservas; cierra pedido si no hay cobro. |
| T05 | PAYMENT_FAILED | CANCELLED | Reintentos agotados, timeout o cancelación | Sistema / Comprador | Sin captura; TTL vencido o 3 fallos | Libera reservas; notifica. |
| T06 | PENDING_PAYMENT | ON_HOLD | Revisión antifraude | Sistema | Riesgo alto o discrepancia | Retiene reserva; no captura; solicita revisión. |
| T07 | ON_HOLD | PENDING_PAYMENT | Revisión superada | Supervisor / Administrador | Revisión favorable | Reanuda pago o captura. |
| T08 | ON_HOLD | CANCELLED | Revisión rechazada | Supervisor / Administrador | Rechazo documentado | Anula o reembolsa; libera reservas. |
| T09 | PENDING_PAYMENT | PAID | Pago capturado | PSP | Firma válida; importe y moneda exactos; reserva activa | Confirma reservas: `reserved` baja y `on_hand` baja; emite factura; notifica vendedor; emite derechos digitales. |
| T09A | PENDING_PAYMENT | ON_HOLD | Pago capturado sin reserva válida o con discrepancia | Sistema | Captura confirmada, pero reserva vencida, stock insuficiente, importe o moneda distintos | No confirma fulfillment; alerta; revisión manual; si no se resuelve, reembolso. |
| T10 | PAID | ON_HOLD | Revisión posterior al cobro | Sistema / Supervisor | Fraude, contracargo o discrepancia | Congela fulfillment; no cancela automáticamente. |
| T11 | ON_HOLD | PAID | Revisión posterior superada | Supervisor / Administrador | Revisión favorable | Reanuda fulfillment. |
| T12 | ON_HOLD | CANCELLED | Revisión posterior rechazada | Supervisor / Administrador | Rechazo documentado | Cancela; si hay captura, abre reembolso; repone stock si no se envió. |
| T13 | PAID | PREPARING | Vendedor acepta | Vendedor | Dentro de 48 h; stock disponible | Crea borrador de envío; notifica al comprador. |
| T14 | PAID | CANCELLED | Cancelación dentro de gracia o rechazo del vendedor | Comprador / Vendedor | Antes de PREPARING; dentro de 1 h o rechazo justificado | Si hay captura, abre reembolso; repone stock si no se envió; aplica penalización si corresponde. |
| T15 | PREPARING | CANCELLED | Vendedor no puede cumplir o acepta cancelación del comprador | Vendedor / Supervisor | Antes de DISPATCHED; motivo; afecta a todas las líneas o se cancela la línea parcial | Reembolso total o parcial; reposición de stock; penalización por incumplimiento. |
| T16 | PREPARING | DISPATCHED | Envío despachado | Operador logístico | Etiqueta, tracking y transportista; paquete recogido | Registra tracking; notifica; no cambia stock. |
| T17 | DISPATCHED | DELIVERED | Entrega confirmada | Transportista / Operador | Prueba de entrega válida | Activa ventana de devolución; programa liquidación. |
| T18 | DISPATCHED | CANCELLED | Paquete perdido o devuelto al remitente sin reenvío | Operador / Supervisor | Incidencia documentada; afecta a todas las líneas del pedido; no se puede reenviar | Reembolso; reposición si el paquete vuelve; cierra incidencia. Si es parcial, se cierra la línea y el pedido sigue. |
| T19 | DELIVERED | FINALIZED | Ventana de devolución vencida | Sistema | Sin devolución abierta; sin reembolso pendiente | Cierra pedido; libera la reserva de garantía y la liquidación al vendedor. |
| T20 | DELIVERED | RETURN_REQUESTED | Solicitud de devolución | Comprador | Dentro de 30 días (14 digital); elegible; cantidad disponible | Crea RMA; notifica al vendedor. Puede ser parcial; la agregación se resuelve en 6.6. |
| T21 | RETURN_REQUESTED | RETURN_IN_TRANSIT | Devolución aprobada | Vendedor / Supervisor | Aprobación dentro de 48 h o autoaprobación | Genera etiqueta; espera recepción. |
| T22 | RETURN_REQUESTED | DELIVERED | Devolución rechazada | Vendedor / Supervisor | Motivo válido y notificado | Cierra RMA; el pedido sigue entregado. |
| T23 | RETURN_IN_TRANSIT | RETURN_RECEIVED | Paquete recibido en almacén | Operador logístico | Recepción registrada | Inicia inspección. |
| T24 | RETURN_RECEIVED | RETURNED | Inspección aceptada y devolución total | Operador / Vendedor | Todas las unidades del pedido devueltas y aceptadas | Repone stock o marca dañado; abre reembolso total. |
| T24A | RETURN_RECEIVED | DELIVERED | Inspección aceptada y devolución parcial | Operador / Vendedor | Solo parte de las unidades devueltas y aceptadas | Repone la parte devuelta; actualiza `return_status = PARTIAL`; el pedido vuelve a DELIVERED y se reembolsa la parte proporcional. |
| T25 | RETURN_RECEIVED | DELIVERED | Inspección rechazada | Vendedor / Supervisor | Evidencia de no conformidad | Reenvía al comprador; cierra RMA. |
| T26 | RETURNED | REFUND_PENDING | Reembolso total aprobado | Vendedor / Supervisor / Administrador | Reembolso total del pedido; importe = capturado - reembolsado; aprobación según umbral | Crea reembolso; emite nota de crédito si aplica; espera PSP. |
| T27 | CANCELLED | REFUND_PENDING | Reembolso requerido por cancelación total | Sistema | `paid_total > 0`; todas las líneas canceladas | Crea reembolso; emite nota de crédito; espera PSP. |
| T28 | DELIVERED | REFUND_PENDING | Reembolso total por defecto o gesto comercial sin devolución | Supervisor / Administrador | Reembolso total; caso justificado; importe ≤ capturado | Crea reembolso; no repone stock; emite nota de crédito. |
| T29 | REFUND_PENDING | REFUNDED | Reembolso confirmado | PSP | Confirmación de reembolso total | Cierra pedido; revoca derechos; actualiza ledger; notifica. |
| T30 | REFUND_PENDING | REFUND_PENDING | Reembolso fallido y reintentado | Sistema / PSP | Reintentos < límite | Mantiene estado; registra fallo; alerta; no cambia importe. |
| T31 | PAID | DELIVERED | Entrega digital automática | Sistema | Todas las líneas son digitales; derechos emitidos | Marca líneas digitales DELIVERED; inicia ventana de 14 días. |

**Nota sobre T31:** en un pedido mixto, la línea digital pasa a DELIVERED al pagar, pero el pedido permanece en PREPARING hasta que el físico se entregue. La agregación se explica en 6.6.

### 6.4 Caminos principales

- **Físico feliz:** PENDING_PAYMENT → PAID → PREPARING → DISPATCHED → DELIVERED → FINALIZED.
- **Digital feliz:** PENDING_PAYMENT → PAID → DELIVERED → FINALIZED.
- **Pago rechazado con reintento:** PENDING_PAYMENT → PAYMENT_FAILED → PENDING_PAYMENT → PAID.
- **Pago abandonado:** PENDING_PAYMENT → CANCELLED.
- **Revisión antifraude:** PENDING_PAYMENT → ON_HOLD → PAID o CANCELLED.
- **Cancelación pagada:** PAID → CANCELLED → REFUND_PENDING → REFUNDED.
- **Devolución:** DELIVERED → RETURN_REQUESTED → RETURN_IN_TRANSIT → RETURN_RECEIVED → RETURNED → REFUND_PENDING → REFUNDED.
- **Devolución rechazada:** DELIVERED → RETURN_REQUESTED → DELIVERED.
- **Pérdida logística:** DISPATCHED → CANCELLED → REFUND_PENDING → REFUNDED.
- **Reembolso por defecto sin devolución:** DELIVERED → REFUND_PENDING → REFUNDED.

### 6.5 Transiciones prohibidas

| Transición prohibida | Motivo |
|---|---|
| PENDING_PAYMENT → PAID sin confirmación del PSP | El pago no se presume; se confirma. |
| PAID → DISPATCHED saltando PREPARING (físico) | El vendedor debe aceptar y preparar; solo el digital se entrega automáticamente. |
| DISPATCHED → CANCELLED por decisión del comprador | Una vez enviado, el camino es la devolución; solo una incidencia logística permite el cierre. |
| DELIVERED → CANCELLED | Lo entregado se devuelve, no se cancela. |
| FINALIZED → cualquier estado | Es terminal; una garantía se gestiona fuera de esta máquina. |
| REFUNDED → cualquier estado | Es terminal; un nuevo cobro exige nuevo pedido. |
| RETURNED → PAID o DELIVERED | El hecho comercial de la devolución no se deshace. |
| CANCELLED → PAID | Un pedido cancelado no se resucita; se crea otro. |
| Cualquier estado → REFUNDED sin confirmación del PSP | El dinero no se presume reembolsado. |
| Cualquier estado → FINALIZED sin entrega | No se cierra lo que no se entregó. |
| Devolución sin pedido DELIVERED | No se devuelve lo no entregado. |
| Reembolso total con devolución parcial | Un reembolso total exige devolución total o decisión expresa de no devolución. |
| Cambio de precio después de PAID | El precio congelado es inmutable. |
| Reserva de stock sin pedido | Toda reserva pertenece a una línea de pedido. |
| Cancelación de una línea ya enviada | Se gestiona como devolución. |

### 6.6 Agregación de líneas y pedidos mixtos

- **RN-EST-01:** Cada línea tiene su propio estado. El estado del pedido se actualiza cuando **todas las líneas activas** alcanzan la condición.
- **RN-EST-02:** Si se cancela una línea y quedan otras activas, el pedido permanece en su estado y `cancellation_status = PARTIAL`; se reembolsa la parte proporcional. Si se cancelan todas, el pedido pasa a CANCELLED.
- **RN-EST-03:** Si se devuelve parte de las unidades, el pedido permanece DELIVERED o RETURNED con `return_status = PARTIAL`; si se devuelven todas las unidades, pasa a RETURNED.
- **RN-EST-04:** Un reembolso parcial no cambia el estado del pedido a REFUNDED; actualiza `refund_status = PARTIALLY_REFUNDED`. Solo el reembolso total lleva a REFUNDED.
- **RN-EST-05:** En un pedido mixto, la línea digital se entrega al pagar y la física sigue su ciclo. El pedido no pasa a DELIVERED hasta que la física se entrega; si todas las líneas son digitales, pasa a DELIVERED al emitir los derechos.
- **RN-EST-06:** Si una línea física se pierde y se reembolsa, pero otras se entregan, el pedido no se cancela completo; se cierra la línea y el pedido sigue hasta FINALIZED con la parte reembolsada.
- **RN-EST-07:** El estado del pedido nunca se deriva solo del pago; requiere el fulfillment correspondiente.
- **RN-EST-08:** La historia de estados es append-only y permite reconstruir el pedido.

### 6.7 Sub-máquinas

#### Pago

CREATED → REQUIRES_ACTION → AUTHORIZED → CAPTURED; CREATED/REQUIRES_ACTION/AUTHORIZED → FAILED, CANCELLED o UNKNOWN; UNKNOWN → CAPTURED, FAILED o CANCELLED; CAPTURED → PARTIALLY_REFUNDED → REFUNDED; CAPTURED/PARTIALLY_REFUNDED → DISPUTED. Un pago FAILED no se reutiliza: se crea uno nuevo.

#### Envío

CREATED → LABEL_READY → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED. Desde estados abiertos: DELIVERY_FAILED, LOST, DAMAGED. DELIVERY_FAILED → reprogramación o RETURNED_TO_SENDER. DELIVERED, RETURNED_TO_SENDER y LOST son terminales; un reenvío crea un nuevo envío.

#### Devolución

REQUESTED → UNDER_REVIEW → APPROVED o REJECTED; APPROVED → LABEL_ISSUED → IN_TRANSIT → RECEIVED → INSPECTED → ACCEPTED o REJECTED_INSPECTION; ACCEPTED → REFUNDED o CLOSED. REJECTED y CLOSED son terminales; un rechazo de inspección puede reenviar al comprador y cerrar.

#### Reembolso

REQUESTED → APPROVED → PROCESSING → SUCCEEDED o FAILED; FAILED → PROCESSING (reintento) o CANCELLED. SUCCEEDED y CANCELLED son terminales. El estado del pedido solo pasa a REFUNDED con SUCCEEDED total.

#### Derecho digital

PENDING → ISSUED → ACCESSED → EXPIRED; ISSUED/ACCESSED → REVOKED. EXPIRED y REVOKED son terminales.

#### Carrito

ACTIVE → CHECKED_OUT, ABANDONED o MERGED. CHECKED_OUT, ABANDONED y MERGED son terminales; un carrito nuevo se crea para la siguiente compra.

#### Compra (CheckoutGroup)

PENDING_PAYMENT → PAID → PARTIALLY_FULFILLED → COMPLETED; PENDING_PAYMENT → CANCELLED; PAID → PARTIALLY_CANCELLED → COMPLETED o CANCELLED; cualquier estado con reembolsos totales → REFUNDED. El grupo es un agregado de conveniencia; la fuente de verdad son los pedidos.

### 6.8 Jobs y temporizadores

| Job | Frecuencia | Acción |
|---|---|---|
| Expiración de reservas | Cada minuto | Libera reservas ACTIVE con `expires_at` vencido y cancela pedidos PENDING_PAYMENT. |
| Expiración de pagos | Cada minuto | Cancela pagos CREATED/REQUIRES_ACTION vencidos y libera reservas. |
| Reconciliación de pagos | Cada 5 minutos | Consulta pagos sin webhook y corrige estados. |
| Aceptación del vendedor | Cada 15 minutos | Cancela y reembolsa pedidos PAID sin aceptación en 48 h. |
| Autoaprobación de devoluciones | Cada hora | Aprueba devoluciones sin respuesta del vendedor en 48 h. |
| Finalización de pedidos | Diaria | Pasa DELIVERED a FINALIZED al vencer la ventana de devolución. |
| Reintento de facturas | Cada 15 minutos | Reintenta facturas en error y alerta tras 3 fallos. |
| Reintento de derechos digitales | Cada 5 minutos | Reintenta emisiones fallidas y alerta a las 24 h. |
| Reintento de reembolsos | Exponencial | Reintenta reembolsos FAILED y alerta. |
| Liquidación a vendedores | Diaria | Calcula y programa pagos T+7 tras entrega. |
| Conciliación financiera | Diaria | Compara PSP, facturas, reembolsos y liquidaciones. |

---

## 7. Permisos y matriz de responsabilidades

### 7.1 Modelo de permisos

- **RBAC + alcance + condiciones.** El rol concede capacidades; el alcance limita a propio, asignado o global; las condiciones aplican umbrales y estados.
- **Denegación por defecto.** Lo no concedido se deniega.
- **Separación de funciones.** Quien solicita no aprueba; quien aprueba no ejecuta manualmente el pago; quien configura no opera.
- **Cuatro ojos.** Reembolsos altos, fraude, cambios de datos fiscales y suspensiones permanentes requieren dos personas.
- **Auditoría obligatoria** en todas las capacidades sensibles.

### 7.2 Rol Supervisor: decisión y límites

**Se mantiene el rol Supervisor.** No es un “administrador junior”. Es un rol de **control, supervisión y aprobación limitada** que:

- Aplica el principio de cuatro ojos en devoluciones, reembolsos, fraude y suspensiones temporales.
- Evita que el Administrador sea el único aprobador y el único punto de fallo.
- Puede leer toda la operación sin modificarla.
- Tiene límites económicos y temporales estrictos.

**Puede:**

- Ver todos los pedidos, reportes, liquidaciones y auditoría en modo lectura.
- Aprobar devoluciones y reembolsos entre 500,01 € y 2.000 €.
- Aprobar excepciones de devolución y reembolso dentro de su umbral.
- Suspender temporalmente un producto, vendedor o cuenta durante 72 h, con motivo y revisión posterior del Administrador.
- Autorizar reenvíos, incidencias logísticas y gestos comerciales dentro de su umbral.
- Revisar KYC y recomendar aprobación; no aprobar.
- Exportar reportes con marca de agua y auditoría.

**No puede:**

- Crear, editar o eliminar usuarios, roles o permisos.
- Aprobar vendedores, cambiar datos fiscales o cuentas de liquidación.
- Crear, editar o despublicar catálogo, precios, impuestos o comisiones.
- Configurar el PSP, las comisiones, las series de facturación o las reglas de negocio.
- Ejecutar reembolsos manualmente ni alterar importes.
- Ver números completos de tarjeta (nunca) ni documentos de identidad completos (solo enmascarados).
- Eliminar registros de auditoría o datos personales.
- Aprobar su propio reembolso o el de una cuenta vinculada.
- Cambiar su propio rol o elevar sus permisos.

**Justificación:** el Supervisor aporta control sin concentrar poder. Si la organización no necesita el rol, puede desactivarse por configuración, pero la especificación lo mantiene porque los dominios de devoluciones, fraude y reportes requieren un aprobador intermedio.

### 7.3 Matriz de capacidades

Leyenda: **✓** completo; **P** propio con ownership; **A** asignado; **L** lectura; **U** dentro de umbral; **—** sin acceso.

| ID | Capacidad | Comprador | Vendedor | Logístico | Supervisor | Administrador |
|---|---|---|---|---|---|---|
| CAP-01 | Registro y perfil propio | ✓ | ✓ | ✓ | ✓ | ✓ |
| CAP-02 | Carrito y compra | ✓ | — | — | — | — |
| CAP-03 | Catálogo público | ✓ | ✓ | ✓ | ✓ | ✓ |
| CAP-04 | Crear y editar productos y precios | — | P | — | — | ✓ |
| CAP-05 | Publicar, despublicar y suspender catálogo | — | P | — | U temporal 72 h | ✓ |
| CAP-06 | Gestionar inventario y almacenes | — | P | ✓ | L | ✓ |
| CAP-07 | Recepción, preparación y etiquetado | — | P | ✓ | L | ✓ |
| CAP-08 | Ver pedidos | P | P | A | L global | ✓ |
| CAP-09 | Aceptar y preparar pedidos | — | P | — | — | ✓ |
| CAP-10 | Crear envíos y actualizar tracking | L | L | ✓ | L | ✓ |
| CAP-11 | Confirmar entrega | — | — | ✓ | L | ✓ |
| CAP-12 | Solicitar devolución | P | — | — | — | — |
| CAP-13 | Aprobar devolución y reembolso | — | P hasta 500 € | — | U 500,01–2.000 € | ✓ más de 2.000 € |
| CAP-14 | Ejecutar o reintentar reembolso manual | — | — | — | — | ✓ Finanzas |
| CAP-15 | Ver, emitir y rectificar facturas | L propias | L propias | — | L global | ✓ |
| CAP-16 | Ver y gestionar liquidaciones | — | L propias | — | L global | ✓ |
| CAP-17 | Ver reportes | — | P | A | L global | ✓ |
| CAP-18 | Exportar reportes | — | P | A | ✓ | ✓ |
| CAP-19 | Gestionar usuarios y roles | — | — | — | — | ✓ |
| CAP-20 | Alta y aprobación de vendedores | — | — | — | L y recomendación | ✓ |
| CAP-21 | Suspender producto, vendedor o cuenta | — | — | — | U temporal 72 h | ✓ permanente |
| CAP-22 | Ver documento de identidad | P enmascarado | P enmascarado | P enmascarado | L enmascarado | ✓ completo auditado |
| CAP-23 | Ver auditoría | — | — | — | L | ✓ |
| CAP-24 | Configurar pagos, impuestos y comisiones | — | — | — | — | ✓ |
| CAP-25 | Gestionar disputas y contracargos | — | L propias | — | L | ✓ |
| CAP-26 | Crear cupones y campañas | — | P en sus productos | — | — | ✓ |
| CAP-27 | Ver datos personales | P | P del pedido | P del envío | L enmascarado | ✓ auditado |
| CAP-28 | Eliminar o anonimizar datos | — | — | — | — | — |
| CAP-29 | Configurar reglas de negocio | — | — | — | — | ✓ |
| CAP-30 | Ver y operar almacenes | — | P | ✓ | L | ✓ |

**Notas:**

- El Administrador no puede borrar auditoría, saltarse el PSP, cambiar su propio rol ni aprobar su propio reembolso.
- El Vendedor no puede comprar sus productos ni aprobar devoluciones en las que sea comprador.
- El Operador logístico ve datos de envío, no importes de artículos ni datos fiscales.
- El Supervisor ve documentos enmascarados; el Administrador ve el completo solo con motivo y auditoría.
- Ningún rol elimina datos manualmente: la supresión y la anonimización las ejecuta únicamente el proceso de retención, con auditoría.
- Los umbrales se expresan en moneda de control y se convierten con el tipo de cambio del día.

### 7.4 Matriz RACI por dominio

**R** responsable de ejecutar; **A** aprueba; **C** consultado; **I** informado.

| Proceso | Comprador | Vendedor | Logístico | Supervisor | Administrador | PSP / Transportista |
|---|---|---|---|---|---|---|
| Registro y autenticación | R | R | R | R | A | I |
| Alta y KYC de vendedor | I | R | I | C | A | I |
| Catálogo y precios | I | R | I | C | A | — |
| Inventario y almacenes | I | R | R | C | A | — |
| Checkout y pago | R | I | I | I | A | R |
| Preparación y envío | I | R | R | C | A | R |
| Entrega e incidencias | I | C | R | A | I | R |
| Devoluciones | R | R | R | A (≤ 2.000 €) | A (> 2.000 €) | I |
| Reembolsos | I | R | I | A (≤ 2.000 €) | A (> 2.000 €) | R |
| Facturación | I | C | I | C | A | I |
| Liquidaciones | — | I | — | C | A | R |
| Reportes | I | R | R | A | A | I |
| Auditoría y seguridad | I | I | I | C | A | I |

**Regla RACI:** en cada instancia solo hay un “A”. Cuando Supervisor y Administrador comparten la aprobación, se aplica el umbral: Supervisor hasta 2.000 € y Administrador por encima. Para cambios de configuración, solo el Administrador es A.

### 7.5 Acciones sensibles y cuatro ojos

| Acción | Solicita | Aprueba | Ejecuta | Registro |
|---|---|---|---|---|
| Reembolso ≤ 500 € | Vendedor | Vendedor | Sistema | Auditoría |
| Reembolso 500,01–2.000 € | Vendedor | Supervisor | Sistema | Auditoría + motivo |
| Reembolso > 2.000 € | Vendedor / Supervisor | Administrador | Sistema / Finanzas | Auditoría + cuatro ojos |
| Suspensión temporal | Supervisor | Supervisor | Sistema | Auditoría + revisión 72 h |
| Suspensión permanente | Supervisor / Administrador | Administrador | Sistema | Auditoría + cuatro ojos |
| Cambio de datos fiscales | Vendedor | Administrador | Administrador | Auditoría + evidencia |
| Anulación de factura | Administrador | Administrador | Administrador | Nota de crédito + auditoría |
| Cambio de comisión | Administrador | Administrador | Sistema | Auditoría + vigencia |
| Acceso a documento completo | Administrador | Administrador | Sistema | Auditoría con motivo |

---

## 8. Flujos principales

### 8.1 Checkout y pago exitoso

1. El comprador añade variantes de uno o varios vendedores al carrito.
2. El sistema verifica disponibilidad y muestra precios actuales, envío y plazos por vendedor.
3. El comprador elige dirección, método de envío, facturación y cupón.
4. El sistema valida comprador, vendedores, variantes, stock, moneda y dirección.
5. El comprador confirma el checkout con clave de idempotencia.
6. El sistema congela precios, crea la compra y N pedidos, reserva stock y crea el pago.
7. El comprador es redirigido al PSP y completa el pago, incluido 3DS si aplica.
8. El PSP envía webhook firmado; el sistema valida y confirma la captura.
9. El sistema confirma reservas, descuenta stock, emite factura, emite derechos digitales y notifica a vendedores.
10. Los pedidos pasan a PAID; los digitales a DELIVERED; los físicos esperan aceptación.
11. El comprador ve la compra, los pedidos, las facturas y la biblioteca digital.

### 8.2 Pago rechazado y reintento

1. El PSP rechaza el pago y envía el código.
2. El pedido pasa a PAYMENT_FAILED; la reserva sigue activa.
3. El comprador ve el motivo y puede reintentar con otro método.
4. Cada reintento crea un nuevo pago, mantiene el precio congelado y no duplica la reserva.
5. Si el pago se aprueba, sigue el flujo 8.1.
6. Si se agotan los 3 intentos o vence el TTL, el pedido pasa a CANCELLED y se liberan las reservas.
7. Si el estado es desconocido, no se cancela ni se aprueba hasta conciliar.

### 8.3 Cancelación parcial multi-vendedor

1. El comprador paga una compra con pedidos de dos vendedores.
2. Un vendedor rechaza su pedido antes de preparar.
3. Ese pedido pasa a CANCELLED y se abre un reembolso por su parte proporcional.
4. El otro pedido continúa su ciclo.
5. La compra pasa a PARTIALLY_CANCELLED.
6. El comprador recibe la factura del pedido que sigue y la nota de crédito del cancelado.
7. El reembolso se confirma por el PSP y el pedido cancelado pasa a REFUNDED.

### 8.4 Fulfillment físico

1. El pedido está PAID y el vendedor lo acepta en 48 h.
2. El pedido pasa a PREPARING; se crea el borrador de envío.
3. El operador valida dirección, embala, genera etiqueta y asigna transportista.
4. El transportista recoge; el pedido pasa a DISPATCHED con tracking.
5. El comprador recibe notificaciones y puede seguir el envío.
6. El transportista entrega con POD; el pedido pasa a DELIVERED.
7. Se activa la ventana de devolución y se programa la liquidación.
8. Al vencer la ventana sin devolución, el pedido pasa a FINALIZED y se libera la liquidación.

### 8.5 Devolución y reembolso

1. El comprador solicita devolución dentro de 30 días, con motivo y evidencia.
2. El pedido pasa a RETURN_REQUESTED; se crea la RMA.
3. El vendedor aprueba o rechaza en 48 h; si no responde, se autoaprueba.
4. Si se aprueba, se genera etiqueta y el pedido pasa a RETURN_IN_TRANSIT.
5. El almacén recibe y registra; el pedido pasa a RETURN_RECEIVED.
6. Se inspecciona en 3 días hábiles; si se acepta, el pedido pasa a RETURNED y se repone o marca dañado.
7. Se aprueba el reembolso según umbral; el pedido pasa a REFUND_PENDING.
8. El PSP confirma; se emite nota de crédito, se revoca el derecho digital si aplica y el pedido pasa a REFUNDED.
9. Si el PSP falla, se reintenta y se alerta; no se marca REFUNDED hasta la confirmación.

### 8.6 Entrega digital

1. El pedido con líneas digitales se paga.
2. Por cada línea se crea un derecho PENDING.
3. El sistema genera la clave, el token o el acceso.
4. El derecho pasa a ISSUED y la línea a DELIVERED.
5. El comprador recibe un correo con enlace a “Mi biblioteca”; no se envían claves adjuntas.
6. Al acceder, se valida titularidad, se registra el acceso y se incrementa el contador.
7. Si el pedido se reembolsa, el derecho pasa a REVOKED y el acceso se bloquea.
8. Si la emisión falla, se reintenta; a las 24 h se ofrece reembolso o entrega manual.

### 8.7 Liquidación al vendedor

1. El pedido se entrega y se cumple el periodo de retención; se calcula la liquidación con reserva de garantía.
2. El sistema calcula ventas, comisión, costes de pago, envíos, reembolsos y contracargos.
3. Genera la liquidación del periodo con estado PENDING.
4. El Administrador (Finanzas) la revisa y aprueba.
5. Se ejecuta el pago al vendedor y se registra la referencia.
6. La liquidación pasa a PAID y se concilia con el PSP y las facturas.
7. Un saldo negativo se compensa con la siguiente liquidación.
8. Al cerrar la ventana de devolución sin incidencias, se libera la reserva de garantía.

---

## 9. Requisitos no funcionales

| Categoría | Requisito | Objetivo |
|---|---|---|
| Disponibilidad | Checkout y pago | 99,9% mensual. |
| Disponibilidad | Catálogo | 99,5% mensual. |
| Rendimiento | Búsqueda y ficha de producto | p95 < 500 ms. |
| Rendimiento | Checkout | p95 < 1,5 s sin contar PSP. |
| Rendimiento | Webhook de pago | p95 < 300 ms. |
| Escalabilidad | Picos de campaña | Soportar 10× el tráfico medio con reservas y colas. |
| Consistencia | Inventario y pagos | Fuerte; sin sobreventa ni doble cobro. |
| Consistencia | Reportes y búsqueda | Eventual; frescura declarada. |
| Seguridad | PCI DSS | SAQ A o equivalente; sin almacenar PAN/CVV. |
| Seguridad | Autenticación | JWT corto, refresh rotatorio, MFA privilegiado. |
| Privacidad | Datos personales | GDPR/LFPDPPP y normativa local; minimización y retención. |
| Auditoría | Operaciones sensibles | 100% registradas y consultables. |
| Recuperación | RPO | ≤ 15 minutos para pagos y pedidos. |
| Recuperación | RTO | ≤ 4 horas para checkout; ≤ 8 horas para reportes. |
| Observabilidad | Logs, métricas, trazas | Correlación por compra, pedido, pago y envío. |
| Accesibilidad | Interfaz | WCAG 2.1 AA en flujos críticos. |
| Localización | Idioma, moneda, impuestos | Configurable por marketplace. |
| Mantenibilidad | Dominios | Puertos y adaptadores; PSP y transportistas intercambiables. |
| Legal | Facturación y devoluciones | Motor fiscal y políticas configurables por país. |

**SLO de jobs:** la expiración de reservas no debe acumular más de 1 minuto de retraso; la conciliación de pagos, más de 10 minutos; la emisión de facturas, más de 15 minutos.

---

## 10. Glosario

| Término | Definición |
|---|---|
| **SKU / Variante** | Unidad vendible e inventariable de un producto, con precio, atributos y stock propios. |
| **Producto** | Entidad comercial que agrupa variantes y contenido; no es la unidad de venta. |
| **CheckoutGroup / Compra** | Agrupación de los pedidos generados por un mismo checkout y un mismo pago. |
| **Pedido** | Unidad de fulfillment, facturación, devolución y liquidación de un vendedor. |
| **Línea de pedido** | Producto/variante concreta comprada, con cantidad, precio congelado e impuestos. |
| **Precio congelado** | Copia inmutable del precio y condiciones en el momento del checkout. |
| **Reserva** | Apartado temporal de stock para una línea, con vencimiento. |
| **Confirmación de venta** | Descuento definitivo del stock al confirmar el pago. |
| **PSP** | Proveedor de servicios de pago que autoriza, captura y reembolsa. |
| **Tokenización** | Sustitución de datos de tarjeta por un token del PSP. |
| **3DS/SCA** | Autenticación reforzada del comprador exigida por la normativa. |
| **Captura** | Cobro efectivo de una autorización. |
| **Contracargo** | Disputa del comprador ante su emisor; el PSP retira fondos. |
| **RMA** | Autorización de devolución de mercancía. |
| **POD** | Prueba de entrega (firma, foto, OTP). |
| **SLA** | Compromiso de nivel de servicio. |
| **GMV** | Valor bruto de la mercancía vendida. |
| **Take rate** | Porcentaje que la plataforma retiene sobre el GMV. |
| **Liquidación** | Pago neto al vendedor tras comisiones, reembolsos y ajustes. |
| **Nota de crédito** | Documento fiscal que rectifica total o parcialmente una factura. |
| **Derecho digital** | Autorización emitida al comprador para descargar, licenciar o acceder a un producto digital. |
| **KYC** | Conocimiento del cliente; verificación de identidad y riesgo. |
| **Cuatro ojos** | Regla por la que una acción requiere aprobación de una segunda persona. |
| **Idempotencia** | Propiedad por la que repetir una operación no duplica su efecto. |
| **Outbox** | Patrón para publicar eventos de forma fiable junto con la transacción. |
| **k-anonimato** | Técnica que exige al menos k individuos en un agregado para evitar reidentificación. |

---

## 11. Anexos

### Anexo A. Brechas entre esta especificación y la implementación actual

La implementación existente es un punto de partida válido, pero no cumple todos los puntos de esta especificación. El delta debe resolverse en diseño e implementación.

| Brecha | Situación actual | Objetivo de esta especificación |
|---|---|---|
| Precio | `Product` tiene `price`; las variantes son una lista de textos. | Precio en `Variant/SKU`; variante como entidad; producto con precio “desde”. |
| Inventario | Se reserva al añadir al carrito. | Se reserva al confirmar el checkout y se descuenta al pagar. |
| Carrito | El carrito es un `Order` en estado `CART`. | Carrito separado; el pedido nace en PENDING_PAYMENT. |
| Multi-vendedor | Un pedido por carrito, sin agrupación por vendedor. | Un CheckoutGroup y N pedidos, uno por vendedor. |
| Pago | El comprador llama a `confirmPayment` con una referencia; no hay PSP. | PSP externo, webhook, conciliación y estados financieros. |
| Identidad | Un único campo `identityDocument` de texto. | Tipo, número cifrado, país, hash, verificación y auditoría. |
| Supervisor | Existe en el enum, pero no tiene permisos ni rutas. | Rol definido con capacidades, umbrales y cuatro ojos. |
| Estados | Faltan devolución, reembolso, pago fallido y revisión. | Máquina completa de la sección 6. |
| Facturación | No existe. | Facturas, notas de crédito y liquidaciones. |
| Logística | Solo cambios de estado; sin envíos, tracking ni incidencias. | Dominio logístico completo. |
| Devoluciones | No existen. | RMA, inspección, reposición y reembolso. |
| Reportes | No existen. | Definiciones, ejecuciones, métricas y permisos. |
| Dirección | `shippingAddress` es un texto libre. | Snapshot estructurado y validado. |
| Digital | No hay entrega ni derechos. | Entitlements, biblioteca y revocación. |
| Concurrencia | Sin versión ni reservas atómicas. | Optimista por defecto y pesimista controlado. |
| Idempotencia | Sin claves ni eventos. | Checkout, pago, webhook y reembolso idempotentes. |

### Anexo B. Trazabilidad con el encargo

| Punto solicitado | Decisión | Sección |
|---|---|---|
| Precio en producto o variante | DEB-01 | 4.2 |
| Pasarela de pago y fallos | DEB-02 | 4.6 |
| Regla multi-vendedor | DEB-03 | 4.4 |
| Reserva y descuento de inventario, concurrencia | DEB-04 | 4.3 |
| Dominios de Facturación, Logística, Devoluciones y Reembolsos, Reportes | DEB-05 | 4.8–4.11 |
| Permisos del Supervisor | DEB-06 | 7 |
| Documento de identidad en Usuarios | DEB-07 | 4.1 |
| Entrega de productos digitales | DEB-08 | 4.7 |
| Atributos del pedido | DEB-09 | 4.5 |
| Máquina de estados completa | DEB-10 | 6 |

### Anexo C. Parámetros por defecto

| Parámetro | Valor por defecto | Configurable |
|---|---|---|
| Moneda | EUR | Sí, por marketplace |
| Impuesto | IVA/VAT según país y categoría | Sí |
| Comisión de plataforma | 15% | Sí, por vendedor/categoría |
| TTL de reserva | 15 minutos | Sí, 10–30 |
| Reintentos de pago | 3 | Sí |
| Ventana de pago | 15 minutos | Sí |
| Aceptación del vendedor | 48 horas | Sí |
| SLA de despacho | 48 horas | Sí |
| Entrega estándar | 2–5 días hábiles | Sí |
| Devolución física | 30 días naturales | Sí, ≥ mínimo legal |
| Devolución digital | 14 días, sin acceso | Sí, ≥ mínimo legal |
| Inspección de devolución | 3 días hábiles | Sí |
| Reembolso tras aprobación | 3 días hábiles | Sí |
| Umbral Vendedor | 500 € | Sí |
| Umbral Supervisor | 2.000 € | Sí |
| Retención de liquidación | 7 días tras entrega + reserva del 10% hasta el cierre de devoluciones | Sí |
| Retención de facturas | 10 años | Según ley |
| Caducidad de carrito | 30 días | Sí |
| Máximo por línea | 99 | Sí |
| OTP/firma | > 200 € | Sí |
| k-anonimato | 5 | Sí |
| Frescura operativa | 15 minutos | Sí |
| Frescura financiera | T+1 tras cierre | Sí |

### Anexo D. Invariantes y criterios de aceptación

1. **No sobreventa:** `quantity_available >= 0` en todo momento; la suma de reservas activas no supera el stock.
2. **Pago exacto:** el importe capturado es igual al total congelado y a la suma de los pedidos.
3. **Un pago, N pedidos:** la compra tiene un pago y cada pedido tiene su factura, envío y devolución.
4. **Precio inmutable:** ningún pedido pagado cambia de precio; las correcciones son financieras.
5. **Factura coherente:** toda factura cuadra con su pedido; toda rectificación tiene nota de crédito.
6. **Reembolso seguro:** ningún reembolso supera lo capturado menos lo reembolsado; es idempotente y confirmado por el PSP.
7. **Estado válido:** toda transición pertenece a la tabla de la sección 6 y queda auditada.
8. **Digital revocable:** todo derecho digital emitido se puede revocar y el reembolso lo desactiva.
9. **Identidad única:** no hay dos usuarios con el mismo documento verificado.
10. **Permisos:** ninguna capacidad se concede fuera de la matriz; el Supervisor no excede su umbral.
11. **Inventario y devolución:** el stock solo se repone tras inspección; nunca al aprobar la solicitud.
12. **Reportes conciliados:** los reportes financieros cuadran con facturas, pagos y liquidaciones o marcan la diferencia.
13. **Trazabilidad:** todo pedido se reconstruye desde sus eventos, pagos, facturas, envíos y devoluciones.
14. **Privacidad:** no se exponen documentos completos, PAN, CVV ni datos personales innecesarios.

### Anexo E. Registro de cambios

| Versión | Fecha | Cambio |
|---|---|---|
| 1.0 | — | Especificación base del marketplace, sin precios por variante, multi-vendedor, devoluciones, facturación ni reportes. |
| 2.0 | 26/10/2026 | Se completan los diez puntos del encargo, se añaden los cuatro dominios, se define el rol Supervisor, el documento de identidad, la entrega digital, los atributos del pedido y la máquina de estados completa. |

---

**Fin de la especificación.** Esta versión se considera la fuente de verdad para el diseño de NexusMarket. Cualquier cambio posterior debe registrarse como una nueva versión, con impacto en reglas, estados, permisos y criterios de aceptación.

