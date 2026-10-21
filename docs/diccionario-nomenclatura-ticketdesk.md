# TicketDesk: Diccionario de Nomenclatura

## Documento base: Trabajo Final Integrador UTN TUPaD

Este documento fija un único nombre para cada concepto del sistema y muestra cómo se llama en cada capa: la base de datos, la API y las pantallas. Sirve como referencia para que el modelo de datos (`DER-ticketdesk.md`), la especificación de la API (`endpoints-api-ticketdesk.md`), los flujos, las reglas de negocio y los wireframes usen los mismos términos.

---

## 1. Convenciones

| Capa | Convención | Ejemplo |
|---|---|---|
| Base de datos | Tablas en plural y columnas en `snake_case` | `tickets.fecha_creacion` |
| API | Campos en `camelCase` | `fechaCreacion` |
| Valores enumerados | Mayúsculas con guion bajo, igual en la base y en la API | `EN_PROGRESO` |
| Pantallas | Texto en español, con la primera letra en mayúscula y sin guion bajo | "En progreso" |
| Documentos funcionales | Se usa el texto de pantalla en el cuerpo y el valor técnico entre comillas invertidas cuando se habla de datos | el estado `EN_PROGRESO` ("En progreso") |

Regla de uso: en los documentos técnicos (modelo de datos, API, reglas de implementación) se escribe siempre el valor técnico. En los documentos funcionales y de usuario se escribe el texto de pantalla.

## 2. Valores enumerados

### 2.1 Estado del ticket

| Valor técnico | Texto en pantalla | Significado |
|---|---|---|
| `NUEVO` | Nuevo | Creado y sin agente asignado |
| `ASIGNADO` | Asignado | Un agente lo tomó y todavía no empezó a trabajarlo |
| `EN_PROGRESO` | En progreso | El agente trabaja en la resolución |
| `ESPERANDO_CLIENTE` | Esperando al cliente | El agente necesita información del cliente |
| `ESCALADO` | Escalado | Derivado a un supervisor |
| `RESUELTO` | Resuelto | El agente lo resolvió y falta la confirmación del cliente |
| `REABIERTO` | Reabierto | El cliente indicó que el problema continúa |
| `CERRADO` | Cerrado | Estado final |

### 2.2 Prioridad (la elige el cliente)

| Valor técnico | Texto en pantalla |
|---|---|
| `BAJA` | Baja |
| `MEDIA` | Media |
| `ALTA` | Alta |

En el formulario de alta la pantalla la presenta como "Qué tan urgente es para vos".

### 2.3 Nivel de atención (clasificación técnica interna)

| Valor técnico | Texto en pantalla | Tiempo objetivo de resolución |
|---|---|---|
| `NIVEL_1` | Nivel 1 | 24 horas |
| `NIVEL_2` | Nivel 2 | 48 horas |
| `NIVEL_3` | Nivel 3 | 72 horas |
| `CRITICO` | Crítico | 4 horas |

### 2.4 Canal de origen

| Valor técnico | Significado |
|---|---|
| `FORMULARIO` | Ticket creado completando el formulario |
| `VOZ` | Ticket creado por mensaje de voz |

### 2.5 Roles

| Valor técnico | Nombre en los documentos | Texto en pantalla |
|---|---|---|
| `ROLE_USER` | Cliente | Cliente |
| `ROLE_AGENT` | Agente | Agente |
| `ROLE_SUPERVISOR` | Supervisor | Supervisor |
| `ROLE_ADMIN` | Administrador | Administrador |

Se usa el término **personal de soporte** para referirse en conjunto a agentes, supervisores y administradores cuando una regla los alcanza a todos.

### 2.6 Módulos habilitables por empresa

| Clave técnica | Texto en pantalla | Obligatorio |
|---|---|---|
| `tickets` | Tickets | Sí |
| `comentarios` | Comentarios | Sí |
| `adjuntos` | Fotos adjuntas | No |
| `voz` | Tickets por voz | No |
| `facturacion` | Facturación | No |
| `categorias_propias` | Categorías propias | No |

Comentarios y adjuntos son dos módulos independientes: una empresa puede permitir comentarios y no fotos.

La API valida en cada operación que estén habilitados los módulos `comentarios`, `adjuntos`, `voz` y `categorias_propias`. El módulo `facturacion` es un ejemplo de módulo opcional: se puede habilitar o deshabilitar, pero en esta versión no agrega funciones propias.

## 3. Correspondencia por entidad

Cada tabla indica cómo se llama cada dato en la base, en la API y en las pantallas. Una celda con un guion significa que ese dato no viaja por la API o no se muestra.

### 3.1 `empresas`

| Columna | Campo de la API | Texto en pantalla |
|---|---|---|
| `id` | `id` | - |
| `nombre` | `nombre` | Empresa |
| `rubro` | - | - |
| `fecha_alta` | - | - |
| `activa` | - | - |

### 3.2 `configuracion_empresa`

| Columna | Campo de la API | Texto en pantalla |
|---|---|---|
| `empresa_id` | Va en la ruta `/empresas/{id}/configuracion` | - |
| `modulos_habilitados` | `modulosHabilitados` | Módulos habilitados |
| `ventana_mantenimiento_inicio` | `ventanaMantenimientoInicio` | Desde |
| `ventana_mantenimiento_fin` | `ventanaMantenimientoFin` | Hasta |

### 3.3 `usuarios`

| Columna | Campo de la API | Texto en pantalla |
|---|---|---|
| `id` | `id` (en el login, `usuarioId`) | - |
| `empresa_id` | `empresaId` (y `empresaNombre`) | Empresa |
| `nombre` | `nombre` | Nombre y apellido |
| `email` | `email` | Email |
| `password_hash` | Nunca sale por la API. En los pedidos de entrada se llama `password` | Contraseña |
| `rol` | `rol` | Rol |
| `activo` | `activo` | Activo o Desactivado |
| `fecha_alta` | - | - |

### 3.4 `usuario_roles`

| Columna | Campo de la API | Texto en pantalla |
|---|---|---|
| `usuario_id` | Pertenece al usuario | - |
| `rol` | Elemento de la lista `roles` del usuario | Roles |
| `fecha_asignacion` | - | - |

### 3.5 `categorias`

| Columna | Campo de la API | Texto en pantalla |
|---|---|---|
| `id` | `id` | - |
| `empresa_id` | Se expone como `esGenerica`: verdadero cuando es nulo | "General, de todo el sistema" o "Propia de tu empresa" |
| `nombre` | `nombre` | Categoría |
| `descripcion` | `descripcion` | Descripción |

### 3.6 `tickets`

| Columna | Campo de la API | Texto en pantalla |
|---|---|---|
| `id` | `id` | - |
| `empresa_id` | - | - |
| `usuario_id` (el cliente que lo creó) | `clienteId` y `clienteNombre` | Cliente |
| `agente_id` | `agenteId` y `agenteNombre` en el detalle; `agenteAsignado` en el listado | Agente |
| `categoria_id` | `categoriaId`; el nombre viaja como `categoria` | Categoría |
| `titulo` | `titulo` | Título |
| `descripcion` | `descripcion` | Descripción |
| `canal_origen` | `canalOrigen` | "Creado por voz" cuando es `VOZ` |
| `audio_url` | `audioUrl` | Audio original |
| `transcripcion_original` | `transcripcionOriginal` | Transcripción |
| `prioridad` | `prioridad` | Prioridad |
| `nivel_atencion` | `nivelAtencion` | Nivel de atención |
| `estado` | `estado` | Estado |
| `fecha_resuelto` | `fechaResuelto` | Resuelto |
| `reincidente` | `reincidente` | Reincidente |
| `es_reclamo_formal` | `esReclamoFormal` | - |
| `responsable_externo` | `responsableExterno` | - |
| `fecha_creacion` | `fechaCreacion` | Creado |
| `fecha_actualizacion` | `fechaActualizacion` | Última actualización |
| (no es una columna) | `slaHoras` y `vencimientoSla`, calculados | Tiempo objetivo y Vence el |

El tiempo objetivo (SLA) no se guarda: se calcula al responder, a partir del nivel de atención vigente. El vencimiento es la fecha de creación más las horas del nivel.

### 3.7 `comentarios`

| Columna | Campo de la API | Texto en pantalla |
|---|---|---|
| `id` | `id` | - |
| `ticket_id` | Va en la ruta `/tickets/{id}/comentarios` | - |
| `usuario_id` | `usuarioId`, `usuarioNombre` y `usuarioRol` | Nombre y rol del autor ("Cliente" o "Soporte") |
| `contenido` | `contenido` | Comentario |
| `fecha` | `fecha` | Fecha y hora |

### 3.8 `adjuntos`

| Columna | Campo de la API | Texto en pantalla |
|---|---|---|
| `id` | `id` | - |
| `ticket_id` | Va en la ruta `/tickets/{id}/adjuntos` | - |
| `comentario_id` | `comentarioId` | - |
| `url` | `url` | Fotos |
| `tipo` | `tipo` | - |
| `fecha_subida` | `fechaSubida` | Fecha de la foto |

### 3.9 `historial_cambios`

| Columna | Campo de la API | Texto en pantalla |
|---|---|---|
| `id` | `id` | - |
| `entidad` | - (vale `Ticket`, `Usuario` o `ConfiguracionEmpresa`) | - |
| `entidad_id` | Va en la ruta del ticket | - |
| `campo_modificado` | `campoModificado` (en tickets: `estado`, `agente_id` y `nivel_atencion`; en usuarios: `roles` y `activo`; en la configuración: `modulos_habilitados` y `ventana_mantenimiento`) | Estado, Agente y Nivel de atención |
| `valor_anterior` | `valorAnterior` | Valor anterior |
| `valor_nuevo` | `valorNuevo` | Valor nuevo |
| `usuario_id` | `realizadoPor`: nombre de la persona, o "Sistema" si es nulo | Quién lo hizo |
| `motivo` | `motivo` | Motivo |
| `fecha` | `fecha` | Fecha y hora |

## 4. Nombres en los pedidos de la API

Algunos pedidos usan un nombre distinto al del dato que modifican. Se documentan para evitar confusiones.

| Operación | Ruta | Campos del cuerpo |
|---|---|---|
| Iniciar sesión | `POST /auth/login` | `email`, `password`, `empresaId` |
| Registrarse | `POST /auth/register` | `nombre`, `email`, `password`, `empresaId` |
| Crear ticket | `POST /tickets` | `titulo`, `descripcion`, `categoriaId`, `prioridad` |
| Cambiar estado | `PATCH /tickets/{id}/estado` | `estadoNuevo`, `motivo` |
| Asignar | `PATCH /tickets/{id}/asignar` | `agenteId`, `motivo` |
| Cambiar nivel | `PATCH /tickets/{id}/nivel` | `nivel`, `motivo` |
| Sugerencia de voz | `POST /tickets/voz/sugerencia` | `transcripcion` |

## 5. Glosario

| Término | Definición |
|---|---|
| Cliente | Persona que crea tickets y consulta los suyos (`ROLE_USER`). En la documentación anterior también se la llamó "usuario" |
| Agente | Persona del equipo de soporte que toma y resuelve tickets |
| Supervisor | Persona con rol de supervisor: recibe los tickets escalados y puede asignar tickets a otras personas |
| Administrador | Persona que configura la empresa y gestiona categorías propias y usuarios |
| Personal de soporte | Conjunto de agentes, supervisores y administradores |
| Escalar | Pasar un ticket al estado `ESCALADO`, con motivo obligatorio, para que lo atienda un supervisor |
| Resolver | Marcar un ticket como `RESUELTO`. Lo hace el agente |
| Cerrar | Llevar un ticket a `CERRADO`. Lo hace el cliente al confirmar, o el sistema a las 72 horas sin respuesta |
| Reincidente | Ticket que volvió a Nivel 1 tras estancarse 5 días en el nivel Crítico |
| Tiempo objetivo (SLA) | Horas objetivo de resolución según el nivel de atención |
| Ventana de mantenimiento | Franja horaria diaria, definida por empresa, en la que se rechazan las altas y los cambios sobre tickets |
| Módulo | Parte del sistema que cada empresa puede habilitar o deshabilitar |
| Ticket abierto | Ticket que no está `CERRADO`. Incluye los resueltos que esperan la confirmación del cliente |

## 6. Inconsistencias detectadas y decisión tomada

| N° | Inconsistencia | Dónde aparece | Decisión |
|---|---|---|---|
| 1 | El mismo dato se llama `agenteAsignado` en el listado de tickets y `agenteNombre` en el detalle | API | Se documentan ambos nombres. Unificarlos exigiría modificar la API y volver a probar la aplicación, por lo que queda como mejora futura |
| 2 | Los estados se escriben en minúsculas (`en_progreso`) en el README y en los flujos, y en mayúsculas (`EN_PROGRESO`) en la base y la API | Documentos | El valor técnico es siempre en mayúsculas. En prosa se usa el texto de pantalla |
| 3 | El canal de origen se escribe `'voz'` en algunos documentos y es `VOZ` en el sistema | README y DER | El valor técnico es `VOZ` |
| 4 | El rol `ROLE_USER` se llama "usuario" o "usuario/cliente" en algunos documentos | Documentos | Se usa "cliente" |
| 5 | El nivel de atención se abrevia "N1", "N2", "N3" y "CRÍTICO" en los wireframes y se muestra "Nivel 1", "Nivel 2", "Nivel 3" y "Crítico" en las pantallas | Wireframes | El valor técnico es `NIVEL_1` y el texto de pantalla es "Nivel 1" |
| 6 | La tabla de historial tuvo tres nombres a lo largo del proyecto (`ticket_history`, `TicketHistory` e `HistorialCambios`) | Documentos anteriores | Vigentes: tabla `historial_cambios` y clase `HistorialCambios` |
| 7 | El SLA figura como si fuera un dato guardado | Documentos | No es una columna: se calcula con el nivel vigente |
| 8 | Los wireframes identifican los tickets con un número (`#12`), pero el sistema usa un identificador único interno | Wireframes | La aplicación muestra el título y la fecha. El número no existe en esta versión |
| 9 | Los wireframes agrupan "Comentarios y adjuntos" en un solo módulo | Wireframes | Son dos módulos independientes: `comentarios` y `adjuntos` |
| 10 | El cliente elige la "prioridad" y en la pantalla se pregunta "qué tan urgente es" | Pantallas | Son el mismo dato |
| 11 | El campo `usuario_id` de `tickets` es el cliente, no un usuario cualquiera | Base de datos | Se mantiene el nombre por la relación con `usuarios`. En la API se expone como `clienteId` |
| 12 | La contraseña mínima de 8 caracteres se exige en el alta de usuarios por el administrador y en la pantalla de registro, pero la API de registro no la valida | API | Se documenta como escenario de validación. Queda como mejora reforzarlo en la API |
