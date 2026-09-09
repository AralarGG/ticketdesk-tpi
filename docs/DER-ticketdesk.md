# TicketDesk: Diagrama de Entidad-Relación (DER)

## Documento base: Trabajo Final Integrador UTN TUPaD

Este documento define el modelo de datos base del sistema. Al tratarse de una primera versión, se espera que evolucione a medida que el equipo profundice en funcionalidades adicionales (por ejemplo, subcategorías, etiquetas, SLAs, etc.).

---

## 1. Entidades y Campos

### `empresas`
Representa a cada cliente que contrata TicketDesk (arquitectura multi-tenant).

| Campo | Tipo | Descripción |
|---|---|---|
| id | UUID / SERIAL (PK) | Identificador único de la empresa |
| nombre | VARCHAR(150) | Nombre de la empresa cliente |
| rubro | VARCHAR(100) | Rubro/sector (informativo, no restrictivo) |
| fecha_alta | TIMESTAMP | Fecha de alta en el sistema |
| activa | BOOLEAN | Si la cuenta está activa o suspendida |

### `configuracion_empresa`
Guarda qué módulos/opciones del menú base tiene habilitados cada empresa.

| Campo | Tipo | Descripción |
|---|---|---|
| id | UUID / SERIAL (PK) | Identificador |
| empresa_id | FK → empresas.id | A qué empresa pertenece esta configuración |
| modulos_habilitados | JSON/JSONB | Lista de módulos activos, ej: `["facturacion", "categorias_avanzadas"]` |
| ventana_mantenimiento_inicio | TIME | Hora de inicio de la ventana de mantenimiento diaria |
| ventana_mantenimiento_fin | TIME | Hora de fin de la ventana de mantenimiento |

### `usuarios`
Representa tanto a clientes como a agentes (diferenciados por rol).

**Regla de negocio:** los usuarios nunca se eliminan físicamente de la base de datos (para no perder la trazabilidad de tickets, comentarios e historial ya asociados a ellos). En su lugar, se desactivan mediante el campo `activo`. Si un agente con tickets asignados se desactiva, esos tickets deben reasignarse obligatoriamente a otro agente activo antes de completar la baja: el sistema no permite dejar tickets abiertos sin un agente activo responsable.

| Campo | Tipo | Descripción |
|---|---|---|
| id | UUID / SERIAL (PK) | Identificador único |
| empresa_id | FK → empresas.id | Empresa a la que pertenece el usuario |
| nombre | VARCHAR(150) | Nombre completo |
| email | VARCHAR(150) UNIQUE | Usado para login |
| password_hash | VARCHAR(255) | Contraseña encriptada |
| rol | ENUM('ROLE_USER', 'ROLE_AGENT', 'ROLE_SUPERVISOR', 'ROLE_ADMIN') | Rol dentro del sistema |
| activo | BOOLEAN | Si el usuario está activo; los agentes inactivos no pueden recibir nuevas asignaciones |
| fecha_alta | TIMESTAMP | Fecha de registro |

### `categorias`
Categorías de tickets. Catálogo único y genérico, compartido por todas las empresas: no personalizable por cliente, para mantener consistencia en reportes y comparabilidad entre empresas que usan el sistema.

| Campo | Tipo | Descripción |
|---|---|---|
| id | UUID / SERIAL (PK) | Identificador único |
| nombre | VARCHAR(100) | Ej: "Bug", "Consulta", "Facturación", "Reclamo de servicio" |
| descripcion | TEXT | Detalle opcional de la categoría |

### `tickets`
Entidad central del sistema.

| Campo | Tipo | Descripción |
|---|---|---|
| id | UUID / SERIAL (PK) | Identificador único |
| empresa_id | FK → empresas.id | Empresa a la que pertenece el ticket |
| usuario_id | FK → usuarios.id | Usuario que creó el ticket |
| agente_id | FK → usuarios.id (nullable) | Agente asignado (nulo hasta que se asigna) |
| categoria_id | FK → categorias.id | Categoría del ticket |
| titulo | VARCHAR(200) | Título breve del reclamo |
| descripcion | TEXT | Detalle completo del problema |
| canal_origen | ENUM('formulario', 'voz') | Cómo se creó el ticket: manualmente o por mensaje de voz |
| audio_url | VARCHAR(500) (nullable) | Si `canal_origen = 'voz'`, URL del audio original guardado (auditable) |
| transcripcion_original | TEXT (nullable) | Texto exacto transcripto del audio, antes de cualquier interpretación de palabras clave: permite auditar si el sistema categorizó correctamente |
| prioridad | ENUM('baja', 'media', 'alta') | Prioridad del ticket |
| estado | ENUM('nuevo', 'asignado', 'en_progreso', 'esperando_cliente', 'escalado', 'resuelto', 'reabierto', 'cerrado') | Estado actual |
| fecha_creacion | TIMESTAMP | Fecha de creación |
| fecha_actualizacion | TIMESTAMP | Última modificación |

### `comentarios`
Intercambio de mensajes dentro de un ticket.

| Campo | Tipo | Descripción |
|---|---|---|
| id | UUID / SERIAL (PK) | Identificador único |
| ticket_id | FK → tickets.id | Ticket al que pertenece |
| usuario_id | FK → usuarios.id | Autor del comentario (cliente o agente) |
| contenido | TEXT | Texto del comentario |
| fecha | TIMESTAMP | Fecha del comentario |

### `adjuntos`
Fotos u otros archivos asociados a un ticket o comentario.

**Regla de negocio:** los adjuntos solo pueden ser subidos por usuarios con rol `ROLE_USER` (clientes). Los agentes no pueden adjuntar fotos en sus respuestas: toda resolución, diagnóstico o instrucción del agente debe quedar registrada como texto en `comentarios`. Esto se debe a que una imagen puede contener información ambigua o sujeta a interpretación subjetiva de quien la ve después, mientras que el objetivo del sistema es mantener control total y trazabilidad verificable de cada paso de la resolución. Esta validación se aplica a nivel de backend antes de aceptar la carga de un archivo.

| Campo | Tipo | Descripción |
|---|---|---|
| id | UUID / SERIAL (PK) | Identificador único |
| ticket_id | FK → tickets.id | Ticket al que pertenece |
| comentario_id | FK → comentarios.id (nullable) | Comentario asociado, si corresponde |
| url | VARCHAR(500) | URL del archivo en el servicio de almacenamiento (Cloudinary) |
| tipo | VARCHAR(50) | Tipo de archivo (imagen, etc.) |
| fecha_subida | TIMESTAMP | Fecha de carga |
| subido_por_rol | ENUM('ROLE_USER') | Restringido a clientes; se guarda explícitamente para auditoría, aunque el rol ya se puede inferir vía `usuario_id` del comentario |

### `ticket_history`
Historial de cambios relevantes de cada ticket (trazabilidad completa: no solo cambios de estado, sino también reasignación de agente, cambio de categoría o de prioridad).

| Campo | Tipo | Descripción |
|---|---|---|
| id | UUID / SERIAL (PK) | Identificador único |
| ticket_id | FK → tickets.id | Ticket al que pertenece el cambio |
| campo_modificado | ENUM('estado', 'agente_id', 'categoria_id', 'prioridad') | Qué campo del ticket cambió |
| valor_anterior | VARCHAR(100) | Valor previo al cambio |
| valor_nuevo | VARCHAR(100) | Valor posterior al cambio |
| usuario_id | FK → usuarios.id | Quién hizo el cambio |
| motivo | TEXT (nullable) | Motivo del cambio, relevante en escalado, reasignación o reapertura |
| fecha | TIMESTAMP | Fecha y hora exacta del cambio |

---

## 2. Relaciones entre Entidades

```
empresas (1) ────< (N) usuarios
empresas (1) ────< (N) tickets
empresas (1) ──── (1) configuracion_empresa

usuarios (1) ────< (N) tickets            [como creador]
usuarios (1) ────< (N) tickets            [como agente asignado]
usuarios (1) ────< (N) comentarios
usuarios (1) ────< (N) ticket_history     [quién hizo el cambio]

categorias (1) ────< (N) tickets          [catálogo único, no depende de empresas]

tickets (1) ────< (N) comentarios
tickets (1) ────< (N) adjuntos
tickets (1) ────< (N) ticket_history

comentarios (1) ────< (N) adjuntos        [opcional, si la foto va en un comentario]
```

---

## 3. Notas de Diseño

- **Multi-tenant (multi-empresa):** casi todas las entidades principales llevan `empresa_id`, lo que permite que múltiples empresas usen el mismo sistema sin mezclar sus datos.
- **Categorías genéricas y únicas:** a diferencia del menú de módulos (que sí varía por empresa), las categorías de tickets son un catálogo fijo y compartido por todas las empresas del sistema. Esto simplifica el modelo y mantiene consistencia si en el futuro se quieren generar reportes comparativos entre distintos clientes.
- **Fotos/adjuntos:** solo se guarda la URL (Cloudinary u otro servicio), nunca el archivo binario en la base de datos.
- **Escalado y supervisión:** cuando un ticket pasa a estado `escalado`, se reasigna (campo `agente_id`) a un usuario con rol `ROLE_SUPERVISOR` en lugar de otro `ROLE_AGENT` común. Esto queda registrado en `ticket_history` como un cambio de `agente_id`, con el motivo "escalado" explicitado.
- **Trazabilidad de creación por voz:** siguiendo el mismo criterio que con las fotos de agentes, el sistema guarda tanto el audio original como su transcripción textual. Esto permite auditar si la categorización automática por palabras clave fue correcta, sin depender únicamente de la interpretación del sistema de reconocimiento de voz.
- **Extensibilidad:** este DER es una base. Es esperable sumar entidades como `etiquetas`, `sla_configuracion`, `notificaciones`, o `roles_personalizados` a medida que el proyecto crezca: la arquitectura multi-tenant y la separación en tablas independientes está pensada para facilitar esas ampliaciones sin romper lo existente.

---

## 4. Decisión de Almacenamiento (Cerrada)

**Enfoque adoptado: PostgreSQL relacional + columnas JSONB para configuración flexible.**

Todas las entidades del núcleo del negocio (`empresas`, `usuarios`, `tickets`, `comentarios`, `categorias`, `adjuntos`, `ticket_history`) se implementan como tablas relacionales tradicionales en PostgreSQL, dado que sus relaciones son estables y requieren integridad referencial (claves foráneas, transacciones).

El campo `configuracion_empresa.modulos_habilitados` se implementa como columna `JSONB` dentro de PostgreSQL (no como una base NoSQL separada). Esto permite:

- Modificar qué módulos tiene habilitados cada empresa sin necesidad de `ALTER TABLE` ni migraciones
- Mantener la ventaja de indexación y performance de consulta que ofrece PostgreSQL sobre columnas JSONB
- Evitar la complejidad de mantener dos motores de base de datos distintos (uno relacional y uno NoSQL) para un proyecto de este tamaño y plazo

Esta decisión no se toma de forma aislada: coincide con el análisis técnico realizado de forma independiente por Ezequiel Sanabria (ver sección "Justificación Técnica de Arquitectura e Invariantes" en el README principal), lo cual refuerza que es la solución adecuada para el proyecto.
