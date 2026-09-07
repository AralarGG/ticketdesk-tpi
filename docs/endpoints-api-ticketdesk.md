# TicketDesk: Especificación de Endpoints de la API REST

## Documento base: Trabajo Final Integrador UTN TUPaD

Este documento define el contrato de la API REST: rutas, verbos HTTP, autenticación requerida y estructura de los datos de entrada/salida (DTOs). Se basa en el modelo de datos definido en [`DER-ticketdesk.md`](./DER-ticketdesk.md).

**Convenciones generales:**
- Todas las rutas (excepto login/registro) requieren header `Authorization: Bearer {token}` (JWT)
- Formato de intercambio: JSON
- Prefijo base: `/api/v1`

---

## 1. Autenticación

### `POST /api/v1/auth/login`
Inicia sesión y devuelve un token JWT.

**Request:**
```json
{
  "email": "usuario@empresa.com",
  "password": "********"
}
```

**Response 200:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "rol": "ROLE_AGENT",
  "usuario_id": "uuid",
  "empresa_id": "uuid"
}
```

**Response 401:** credenciales inválidas

### `POST /api/v1/auth/register`
Alta de un nuevo usuario (cliente). El alta de agentes/supervisores la realiza un administrador desde un endpoint separado.

**Request:**
```json
{
  "nombre": "Juan Pérez",
  "email": "juan@empresa.com",
  "password": "********",
  "empresa_id": "uuid"
}
```

**Response 201:** usuario creado, mismo formato que login (token incluido)

---

## 2. Tickets

### `POST /api/v1/tickets`
Crea un nuevo ticket. Rol requerido: `ROLE_USER`.

**Request:**
```json
{
  "titulo": "No puedo iniciar sesión",
  "descripcion": "Al ingresar mis credenciales, la página se queda cargando",
  "categoria_id": "uuid",
  "prioridad": "media",
  "canal_origen": "formulario"
}
```

**Response 201:**
```json
{
  "id": "uuid",
  "titulo": "No puedo iniciar sesión",
  "estado": "nuevo",
  "fecha_creacion": "2026-09-07T10:00:00Z"
}
```

### `POST /api/v1/tickets/voz`
Crea un ticket a partir de un audio. Rol requerido: `ROLE_USER`.

**Request:** `multipart/form-data` con el archivo de audio

**Response 201:**
```json
{
  "id": "uuid",
  "titulo": "(autogenerado desde transcripción)",
  "transcripcion_original": "no puedo iniciar sesión en la app",
  "categoria_id": "uuid",
  "estado": "nuevo",
  "canal_origen": "voz"
}
```

### `GET /api/v1/tickets`
Lista tickets. El resultado varía según el rol:
- `ROLE_USER`: solo sus propios tickets
- `ROLE_AGENT` / `ROLE_SUPERVISOR`: todos los tickets de su empresa

**Query params opcionales:** `?estado=abierto&prioridad=alta&categoria_id=uuid&agente_id=uuid`

**Response 200:**
```json
{
  "total": 42,
  "tickets": [
    {
      "id": "uuid",
      "titulo": "No puedo iniciar sesión",
      "estado": "en_progreso",
      "prioridad": "alta",
      "categoria": "Bug",
      "agente_asignado": "María López",
      "fecha_creacion": "2026-09-07T10:00:00Z"
    }
  ]
}
```

### `GET /api/v1/tickets/{id}`
Detalle completo de un ticket, incluyendo comentarios y adjuntos asociados.

**Response 200:**
```json
{
  "id": "uuid",
  "titulo": "No puedo iniciar sesión",
  "descripcion": "...",
  "estado": "en_progreso",
  "prioridad": "alta",
  "categoria": "Bug",
  "usuario": { "id": "uuid", "nombre": "Juan Pérez" },
  "agente_asignado": { "id": "uuid", "nombre": "María López" },
  "comentarios": [ ],
  "adjuntos": [ ],
  "fecha_creacion": "2026-09-07T10:00:00Z"
}
```

**Response 403:** si un `ROLE_USER` intenta ver un ticket que no es suyo

### `PATCH /api/v1/tickets/{id}/estado`
Cambia el estado de un ticket. Rol requerido: `ROLE_AGENT` o `ROLE_SUPERVISOR`.

**Request:**
```json
{
  "estado_nuevo": "resuelto",
  "motivo": "Se restableció la contraseña del usuario"
}
```

**Response 200:** ticket actualizado (genera automáticamente una fila en `ticket_history`)

### `PATCH /api/v1/tickets/{id}/asignar`
Asigna o reasigna un agente al ticket. Rol requerido: `ROLE_AGENT` (autoasignación) o `ROLE_SUPERVISOR` (reasignar a cualquiera).

**Request:**
```json
{
  "agente_id": "uuid",
  "motivo": "Reasignación por escalado"
}
```

**Response 200:** ticket actualizado

---

## 3. Comentarios

### `POST /api/v1/tickets/{id}/comentarios`
Agrega un comentario a un ticket.

**Request:**
```json
{
  "contenido": "¿Podés indicarme qué navegador estás usando?"
}
```

**Response 201:**
```json
{
  "id": "uuid",
  "contenido": "¿Podés indicarme qué navegador estás usando?",
  "usuario": { "id": "uuid", "nombre": "María López", "rol": "ROLE_AGENT" },
  "fecha": "2026-09-07T11:00:00Z"
}
```

### `GET /api/v1/tickets/{id}/comentarios`
Lista todos los comentarios de un ticket, ordenados por fecha.

---

## 4. Adjuntos

### `POST /api/v1/tickets/{id}/adjuntos`
Sube una foto asociada al ticket o a un comentario. **Restringido a `ROLE_USER`** (ver justificación en README, sección "Reglas de Negocio").

**Request:** `multipart/form-data` con la imagen, más `comentario_id` opcional

**Response 201:**
```json
{
  "id": "uuid",
  "url": "https://res.cloudinary.com/.../imagen123.jpg",
  "tipo": "imagen",
  "fecha_subida": "2026-09-07T11:05:00Z"
}
```

**Response 403:** si quien intenta subir el adjunto tiene rol `ROLE_AGENT` o `ROLE_SUPERVISOR`

---

## 5. Categorías

### `GET /api/v1/categorias`
Lista el catálogo único de categorías (compartido por todas las empresas).

**Response 200:**
```json
[
  { "id": "uuid", "nombre": "Bug", "descripcion": "..." },
  { "id": "uuid", "nombre": "Consulta", "descripcion": "..." }
]
```

---

## 6. Configuración de Empresa

### `GET /api/v1/empresas/{id}/configuracion`
Obtiene la configuración de módulos habilitados y ventana de mantenimiento de una empresa. Rol requerido: `ROLE_SUPERVISOR` o `ROLE_ADMIN`.

**Response 200:**
```json
{
  "modulos_habilitados": ["facturacion", "categorias_avanzadas"],
  "ventana_mantenimiento_inicio": "03:00",
  "ventana_mantenimiento_fin": "04:00"
}
```

### `PATCH /api/v1/empresas/{id}/configuracion`
Actualiza la configuración. Rol requerido: `ROLE_ADMIN`.

**Request:**
```json
{
  "modulos_habilitados": ["facturacion"],
  "ventana_mantenimiento_inicio": "02:00",
  "ventana_mantenimiento_fin": "03:00"
}
```

---

## 7. Usuarios / Agentes

### `GET /api/v1/usuarios/agentes`
Lista los agentes activos de la empresa (para asignación manual). Rol requerido: `ROLE_AGENT` o `ROLE_SUPERVISOR`.

### `PATCH /api/v1/usuarios/{id}/desactivar`
Desactiva un usuario (soft delete). Si es un agente con tickets asignados, requiere reasignación previa obligatoria (ver regla de negocio en el DER). Rol requerido: `ROLE_ADMIN`.

**Response 409:** si el agente tiene tickets activos sin reasignar
```json
{
  "error": "El agente tiene 3 tickets activos. Reasigne los tickets antes de desactivar."
}
```

---

## 8. Códigos de Estado HTTP Utilizados

| Código | Uso |
|---|---|
| 200 | Operación exitosa (lectura o actualización) |
| 201 | Recurso creado exitosamente |
| 400 | Datos de entrada inválidos |
| 401 | No autenticado (token ausente o inválido) |
| 403 | Autenticado pero sin permisos para la acción (rol incorrecto) |
| 404 | Recurso no encontrado |
| 409 | Conflicto con el estado actual del recurso (ej. desactivar agente con tickets pendientes) |

---

## Pendiente de Definición

- Endpoints de reportes/métricas (Fase 3 del roadmap, fuera del MVP actual)
- Endpoint de notificaciones (fuera de alcance del MVP)
