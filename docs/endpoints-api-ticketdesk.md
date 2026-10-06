# TicketDesk: Especificación de Endpoints de la API REST

## Documento base: Trabajo Final Integrador UTN TUPaD

Este documento define el contrato de la API REST: rutas, verbos HTTP, rol requerido y estructura de los datos de entrada y salida. Se basa en el modelo de datos definido en [`DER-ticketdesk.md`](./DER-ticketdesk.md) y refleja la API implementada en el backend.

**Convenciones generales**
- Prefijo base: `/api/v1`
- Formato de intercambio: JSON, con nombres de campo en `camelCase` (`categoriaId`, `estadoNuevo`)
- Todas las rutas, salvo las de `/auth`, requieren el header `Authorization: Bearer {token}` (JWT)
- Los valores de enumeraciones van en mayúsculas: estados (`NUEVO`, `ASIGNADO`, `EN_PROGRESO`, `ESPERANDO_CLIENTE`, `ESCALADO`, `RESUELTO`, `REABIERTO`, `CERRADO`), prioridades (`BAJA`, `MEDIA`, `ALTA`), niveles de atención (`NIVEL_1`, `NIVEL_2`, `NIVEL_3`, `CRITICO`) y roles (`ROLE_USER`, `ROLE_AGENT`, `ROLE_SUPERVISOR`, `ROLE_ADMIN`)
- Los identificadores son UUID
- Todo usuario opera únicamente sobre datos de su propia empresa (regla RN15)

**Formato de los errores**
```json
{ "error": "Not Found", "mensaje": "Ticket no encontrado" }
```

---

## 1. Autenticación (rutas públicas)

### `GET /api/v1/auth/empresas`
Lista las empresas activas, para que la pantalla de login permita elegir a cuál se quiere ingresar.

**Response 200:**
```json
[ { "id": "uuid", "nombre": "Empresa Demo" } ]
```

### `POST /api/v1/auth/login`
Inicia sesión y devuelve un token JWT. Requiere `empresaId` además de email y contraseña: el email es único por empresa (no global), así que el login necesita saber a qué empresa se está entrando, para el caso de una persona con cuentas en más de una empresa usando el mismo correo.

**Request:**
```json
{ "email": "usuario@empresa.com", "password": "********", "empresaId": "uuid" }
```

**Response 200:**
```json
{ "token": "eyJhbGciOiJIUzI1NiIs...", "rol": "ROLE_AGENT", "usuarioId": "uuid", "empresaId": "uuid" }
```

**Errores:** 401 si el email, la contraseña o la empresa no coinciden, o si el usuario está desactivado.

### `POST /api/v1/auth/register`
Alta de un cliente (`ROLE_USER`) en una empresa. El personal de soporte lo da de alta un administrador (ver sección 7).

**Request:**
```json
{ "nombre": "Juan Pérez", "email": "juan@empresa.com", "password": "********", "empresaId": "uuid" }
```

**Response 200:** igual que el login (incluye token).

**Errores:** 409 si ya existe un usuario con ese email en la empresa; 404 si la empresa no existe o está inactiva.

---

## 2. Tickets

### `POST /api/v1/tickets`
Crea un ticket por formulario. Rol requerido: `ROLE_USER`. El ticket nace en estado `NUEVO` y nivel `NIVEL_1`.

**Request:**
```json
{
  "titulo": "No puedo iniciar sesión",
  "descripcion": "Al ingresar mis credenciales, la página se queda cargando",
  "categoriaId": "uuid",
  "prioridad": "MEDIA"
}
```

**Response 201:** detalle del ticket (ver `GET /tickets/{id}`).

**Errores:** 404 si la categoría no existe o pertenece a otra empresa.

### `POST /api/v1/tickets/voz/sugerencia`
Primer paso de la apertura por voz. Recibe la transcripción y devuelve la categoría y el título sugeridos, para que el cliente los revise antes de crear el ticket. Rol requerido: `ROLE_USER`. Requiere el módulo `voz` habilitado en la empresa.

**Request:**
```json
{ "transcripcion": "No puedo iniciar sesión en la aplicación desde ayer" }
```

**Response 200:**
```json
{ "categoriaId": "uuid", "categoriaNombre": "Acceso a la cuenta", "tituloSugerido": "No puedo iniciar sesión en la aplicación desde ayer" }
```
`categoriaId` y `categoriaNombre` pueden venir en `null` si no se reconoció ninguna categoría.

### `POST /api/v1/tickets/voz`
Segundo paso: crea el ticket por voz con el audio, la transcripción ya confirmada y la categoría elegida. Rol requerido: `ROLE_USER`.

**Request:** `multipart/form-data`

| Campo | Tipo | Descripción |
|---|---|---|
| `audio` | archivo | Audio original grabado (tipo `audio/*`, hasta 10MB) |
| `transcripcion` | texto | Texto transcripto por el navegador |
| `categoriaId` | UUID | Categoría confirmada por el cliente |
| `prioridad` | texto | Opcional, por defecto `MEDIA` |

**Response 201:** detalle del ticket, con `canalOrigen: "VOZ"`, `audioUrl` y `transcripcionOriginal` completos (trazabilidad, regla RN14).

### `GET /api/v1/tickets`
Lista tickets. El resultado depende de quién consulta: un cliente ve solo los suyos; el personal de soporte ve todos los de su empresa.

**Query params opcionales (solo personal de soporte):** `estado`, `prioridad`, `categoriaId`, `agenteId`, `nivel`

**Response 200:**
```json
[
  {
    "id": "uuid",
    "titulo": "No puedo iniciar sesión",
    "estado": "EN_PROGRESO",
    "prioridad": "ALTA",
    "nivelAtencion": "NIVEL_1",
    "reincidente": false,
    "categoria": "Acceso a la cuenta",
    "clienteNombre": "Juan Pérez",
    "agenteAsignado": "María López",
    "fechaCreacion": "2026-10-04T10:00:00"
  }
]
```

### `GET /api/v1/tickets/{id}`
Detalle completo, con comentarios y adjuntos. Un cliente solo puede ver sus propios tickets; ningún usuario puede ver tickets de otra empresa (en ambos casos responde 404).

**Response 200:**
```json
{
  "id": "uuid",
  "titulo": "No puedo iniciar sesión",
  "descripcion": "...",
  "estado": "EN_PROGRESO",
  "prioridad": "ALTA",
  "nivelAtencion": "NIVEL_1",
  "slaHoras": 24,
  "vencimientoSla": "2026-10-05T10:00:00",
  "reincidente": false,
  "esReclamoFormal": false,
  "responsableExterno": null,
  "canalOrigen": "FORMULARIO",
  "audioUrl": null,
  "transcripcionOriginal": null,
  "categoriaId": "uuid",
  "categoria": "Acceso a la cuenta",
  "clienteId": "uuid",
  "clienteNombre": "Juan Pérez",
  "agenteId": "uuid",
  "agenteNombre": "María López",
  "fechaCreacion": "2026-10-04T10:00:00",
  "fechaActualizacion": "2026-10-04T11:00:00",
  "fechaResuelto": null,
  "comentarios": [],
  "adjuntos": []
}
```

### `GET /api/v1/tickets/{id}/historial`
Historial completo de cambios del ticket, del más reciente al más antiguo. Roles: agente, supervisor o administrador.

**Response 200:**
```json
[
  {
    "id": "uuid",
    "campoModificado": "estado",
    "valorAnterior": "EN_PROGRESO",
    "valorNuevo": "RESUELTO",
    "realizadoPor": "María López",
    "motivo": "Se restableció la contraseña del usuario",
    "fecha": "2026-10-04T11:00:00"
  }
]
```
`realizadoPor` vale `"Sistema"` cuando el cambio fue automático (cierre por silencio del cliente, rebote por estancamiento).

### `PATCH /api/v1/tickets/{id}/estado`
Cambia el estado. Solo se permiten las transiciones del flujo definido:

| Quién | Transiciones permitidas |
|---|---|
| Agente o supervisor | `NUEVO` → `ASIGNADO` → `EN_PROGRESO` → (`ESPERANDO_CLIENTE` \| `ESCALADO` \| `RESUELTO`); `ESPERANDO_CLIENTE` → `EN_PROGRESO`; `ESCALADO` → (`EN_PROGRESO` \| `RESUELTO`); `REABIERTO` → `EN_PROGRESO` |
| Cliente dueño del ticket | `RESUELTO` → `CERRADO` (confirma) \| `REABIERTO` (el problema continúa) |

El personal de soporte no cierra tickets: el cierre lo confirma el cliente o lo hace el sistema a las 72hs sin respuesta (regla RN6).

**Request:**
```json
{ "estadoNuevo": "RESUELTO", "motivo": "Se restableció la contraseña del usuario" }
```
El `motivo` es obligatorio al pasar a `ESCALADO`. Al escalar, el sistema sube un nivel de atención (hasta `NIVEL_3`) y reasigna el ticket al supervisor con menos carga.

**Response 200:** detalle del ticket.

**Errores:** 409 si la transición no está permitida; 403 si el usuario no puede operar sobre ese ticket; 400 si falta el motivo al escalar.

### `PATCH /api/v1/tickets/{id}/asignar`
Asigna o reasigna un agente. Roles: agente o supervisor. Un agente solo puede tomar el ticket para sí mismo; reasignarlo a otra persona requiere rol de supervisor. Si el ticket estaba en `NUEVO` pasa a `ASIGNADO`.

**Request:**
```json
{ "agenteId": "uuid", "motivo": "Reasignación por carga de trabajo" }
```

**Response 200:** detalle del ticket.

### `PATCH /api/v1/tickets/{id}/nivel`
Cambia el nivel de atención técnico (independiente de la prioridad que eligió el cliente). Roles: agente o supervisor.

**Request:**
```json
{ "nivel": "NIVEL_2", "motivo": "Requiere revisión técnica" }
```

**Response 200:** detalle del ticket.

---

## 3. Comentarios

### `POST /api/v1/tickets/{id}/comentarios`
Agrega un comentario. Cliente y personal de soporte pueden comentar en los tickets a los que tienen acceso. No se admiten comentarios en tickets `CERRADO`. Si el cliente responde con el ticket en `ESPERANDO_CLIENTE`, vuelve automáticamente a `EN_PROGRESO`.

**Request:**
```json
{ "contenido": "¿Podés indicarme qué navegador estás usando?" }
```

**Response 201:**
```json
{
  "id": "uuid",
  "contenido": "¿Podés indicarme qué navegador estás usando?",
  "usuarioId": "uuid",
  "usuarioNombre": "María López",
  "usuarioRol": "ROLE_AGENT",
  "fecha": "2026-10-04T11:00:00"
}
```

### `GET /api/v1/tickets/{id}/comentarios`
Lista los comentarios del ticket en orden cronológico. Devuelve un arreglo con el mismo formato de arriba.

---

## 4. Adjuntos

### `POST /api/v1/tickets/{id}/adjuntos`
Sube una imagen asociada al ticket o a un comentario. **Solo el cliente (`ROLE_USER`) puede subir adjuntos** (regla RN13): un agente o supervisor recibe 403. Requiere el módulo `adjuntos` habilitado.

**Request:** `multipart/form-data`

| Campo | Tipo | Descripción |
|---|---|---|
| `archivo` | archivo | Imagen (`image/*`, hasta 10MB) |
| `comentarioId` | UUID | Opcional, comentario al que pertenece la imagen |

**Response 201:**
```json
{
  "id": "uuid",
  "url": "https://res.cloudinary.com/.../imagen123.jpg",
  "tipo": "image/jpeg",
  "comentarioId": null,
  "fechaSubida": "2026-10-04T11:05:00"
}
```
El archivo se guarda en Cloudinary; en la base de datos solo queda la URL.

### `GET /api/v1/tickets/{id}/adjuntos`
Lista los adjuntos del ticket. Devuelve un arreglo con el mismo formato de arriba.

---

## 5. Categorías

### `GET /api/v1/categorias`
Lista las categorías visibles para la empresa del usuario: las genéricas (compartidas por todo el sistema) más las propias de su empresa.

**Response 200:**
```json
[
  { "id": "uuid", "nombre": "Bug", "descripcion": null, "esGenerica": true },
  { "id": "uuid", "nombre": "Consulta sobre receta", "descripcion": "...", "esGenerica": false }
]
```

### `POST /api/v1/categorias`
Crea una categoría propia de la empresa. Rol requerido: `ROLE_ADMIN`. Requiere el módulo `categorias_propias` habilitado.

**Request:**
```json
{ "nombre": "Consulta sobre receta", "descripcion": "Dudas sobre recetas médicas" }
```

**Response 201:** la categoría creada. **Errores:** 409 si el nombre repite el de una categoría propia o genérica.

---

## 6. Configuración de Empresa

### `GET /api/v1/empresas/{id}/configuracion`
Devuelve los módulos habilitados y la ventana de mantenimiento de la empresa. Cualquier usuario puede consultarla, pero solo la de su propia empresa (el frontend la necesita para armar el menú según los módulos habilitados).

**Response 200:**
```json
{
  "modulosHabilitados": ["tickets", "comentarios", "adjuntos", "voz", "facturacion", "categorias_propias"],
  "ventanaMantenimientoInicio": "03:00:00",
  "ventanaMantenimientoFin": "04:00:00"
}
```

### `PATCH /api/v1/empresas/{id}/configuracion`
Actualiza la configuración. Rol requerido: `ROLE_ADMIN`. Todos los campos son opcionales; solo se modifican los enviados.

**Request:**
```json
{ "modulosHabilitados": ["tickets", "comentarios", "voz"], "ventanaMantenimientoInicio": "02:00", "ventanaMantenimientoFin": "03:00" }
```

**Módulos válidos del menú base:** `tickets`, `comentarios`, `adjuntos`, `voz`, `facturacion`, `categorias_propias`. Los módulos `tickets` y `comentarios` son obligatorios y no se pueden deshabilitar.

**Ventana de mantenimiento (regla RN17):** durante esa franja, las escrituras sobre tickets (altas, cambios de estado, comentarios, adjuntos) responden **503** con un aviso de mantenimiento. Las consultas siguen funcionando. La ventana puede cruzar la medianoche (por ejemplo 23:00 a 02:00).

**Errores:** 400 si un módulo no existe o si la ventana está incompleta o tiene inicio y fin iguales.

---

## 7. Usuarios

### `GET /api/v1/usuarios/me`
Datos del usuario autenticado.

**Response 200:**
```json
{
  "id": "uuid",
  "nombre": "María López",
  "email": "maria@empresa.com",
  "rol": "ROLE_AGENT",
  "roles": ["ROLE_AGENT", "ROLE_SUPERVISOR"],
  "activo": true,
  "empresaId": "uuid",
  "empresaNombre": "Empresa Demo"
}
```
`rol` es el rol principal; `roles` lista todos los habilitados (regla RN4).

### `GET /api/v1/usuarios/agentes`
Agentes y supervisores activos de la empresa, para armar la lista de asignación. Roles: agente, supervisor o administrador. Devuelve un arreglo con el formato de arriba.

### `GET /api/v1/usuarios`
Todos los usuarios de la empresa. Rol requerido: `ROLE_ADMIN`.

### `POST /api/v1/usuarios`
Alta de un usuario de la empresa (por ejemplo, un agente). Rol requerido: `ROLE_ADMIN`.

**Request:**
```json
{ "nombre": "Pedro Gómez", "email": "pedro@empresa.com", "password": "********", "rol": "ROLE_AGENT", "rolesAdicionales": ["ROLE_SUPERVISOR"] }
```
La contraseña debe tener al menos 8 caracteres. **Response 201:** el usuario creado. **Errores:** 409 si el email ya existe en la empresa.

### `POST /api/v1/usuarios/{id}/roles`
Suma un rol a un usuario existente. Rol requerido: `ROLE_ADMIN`. Antes de asignarlo se verifica que el usuario no lo tenga ya (checking de rol previo).

**Request:** `{ "rol": "ROLE_SUPERVISOR" }` **Errores:** 409 si ya tiene ese rol.

### `PATCH /api/v1/usuarios/{id}/desactivar`
Desactiva un usuario; nunca se elimina (regla RN2). Rol requerido: `ROLE_ADMIN`. Un administrador no puede desactivar su propia cuenta.

**Errores:** 409 si el agente tiene tickets abiertos a su cargo (regla RN3):
```json
{ "error": "Conflict", "mensaje": "El agente tiene 3 ticket(s) abierto(s) a su cargo. Reasignalos antes de desactivarlo." }
```

### `PATCH /api/v1/usuarios/{id}/activar`
Reactiva un usuario desactivado. Rol requerido: `ROLE_ADMIN`.

---

## 8. Códigos de Estado HTTP Utilizados

| Código | Uso |
|---|---|
| 200 | Operación exitosa (lectura o actualización) |
| 201 | Recurso creado exitosamente |
| 400 | Datos de entrada inválidos o incompletos |
| 401 | No autenticado: token ausente, vencido o inválido; credenciales incorrectas |
| 403 | Autenticado pero sin permisos para la acción (rol incorrecto, módulo deshabilitado) |
| 404 | Recurso no encontrado, o que pertenece a otra empresa |
| 409 | Conflicto con el estado actual (transición no permitida, duplicado, agente con tickets abiertos) |
| 413 | Archivo mayor al máximo permitido (10MB) |
| 503 | Escritura rechazada por ventana de mantenimiento |

---

## Convención de Identificadores de Recurso

Todos los endpoints que operan sobre un recurso específico llevan su identificador explícito en la ruta (`{id}`), por ejemplo `GET /tickets/{id}`, `PATCH /usuarios/{id}/desactivar`. Los únicos endpoints sin `{id}` son operaciones a nivel de colección, donde no aplica: `POST /tickets` (crear, todavía no existe un id) y `GET /tickets` (listar todos). Ningún endpoint de este documento queda con la ruta incompleta o sin definir su identificador cuando corresponde.

## Fuera de Alcance

- Endpoints de reportes y métricas (Fase 3 del roadmap)
- Endpoint de notificaciones
