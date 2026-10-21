# TicketDesk: Matriz de Trazabilidad

## Documento base: Trabajo Final Integrador UTN TUPaD

Esta matriz conecta cada requerimiento funcional con las reglas de negocio que lo gobiernan, los casos de uso que lo realizan, los endpoints que lo implementan, las entidades del modelo de datos que lo sostienen, las pantallas donde se usa y los criterios de aceptación que lo verifican. Permite recorrer el sistema en cualquier dirección: desde un requerimiento hacia su implementación, o desde un endpoint hacia el requerimiento que lo justifica.

Los nombres siguen el `diccionario-nomenclatura-ticketdesk.md`. Los requerimientos (RF y RNF) están en `listado-modulos-ticketdesk.md`, las reglas (RN) en `reglas-negocio-ticketdesk.md`, los casos de uso (UC) en `casos-de-uso-ticketdesk.md` y los criterios (CA) en `criterios-aceptacion-ticketdesk.md`.

## 1. Referencia de pantallas

Las pantallas se identifican con la numeración de `wireframes-pantallas-ticketdesk.md`.

| Código | Pantalla |
|---|---|
| P1 | Login y registro |
| P2 | Panel del cliente (Mis tickets) |
| P3 | Nuevo ticket por formulario |
| P4 | Nuevo ticket por voz |
| P5 | Detalle del ticket, vista del cliente |
| P6 | Panel del personal de soporte (Tickets) |
| P7 | Detalle del ticket, vista del personal de soporte |
| P8 | Administración, pestañas Configuración y Categorías |
| P9 | Administración, pestaña Usuarios |

Los requerimientos funcionales de `listado-modulos-ticketdesk.md` agrupan varias exigencias en una misma viñeta. Para poder referenciarlas, esta matriz las numera con un punto (RF1.1, RF1.2, y así sucesivamente) siguiendo el orden en que aparecen.

## 2. Matriz por requerimiento funcional

| Requerimiento | Reglas | Casos de uso | Endpoints | Entidades | Pantallas | Criterios |
|---|---|---|---|---|---|---|
| RF1.1 Registro de clientes | RN1 | UC-02 | `GET /auth/empresas`, `POST /auth/register` | Usuario | P1 | CA-RF1-01 |
| RF1.2 Alta de agentes, supervisores y administradores | RN1, RN4 | UC-18 | `POST /usuarios` | Usuario, UsuarioRol | P9 | CA-RF1-02 |
| RF1.3 El mismo email en distintas empresas | RN1 | UC-01, UC-02 | `POST /auth/login`, `POST /auth/register` | Usuario, Empresa | P1 | CA-RF1-03 |
| RF1.4 Más de un rol por usuario | RN4 | UC-18 | `POST /usuarios/{id}/roles`, `GET /usuarios/me` | UsuarioRol | P9 | CA-RF1-04, CA-RF1-05 |
| RF1.5 Inicio de sesión y cuentas desactivadas | RN2 | UC-01 | `POST /auth/login` | Usuario | P1 | CA-RF1-06 |
| RF2.1 Crear un ticket por formulario | RN8, RN10 | UC-03 | `POST /tickets`, `GET /categorias` | Ticket, Categoria | P3 | CA-RF2-01, CA-RF2-02 |
| RF2.2 Crear un ticket por voz con confirmación | RN14 | UC-04 | `POST /tickets/voz/sugerencia`, `POST /tickets/voz` | Ticket, Categoria | P4 | CA-RF2-03 a CA-RF2-06 |
| RF2.3 Adjuntar fotos, solo el cliente | RN13 | UC-07 | `POST /tickets/{id}/adjuntos`, `GET /tickets/{id}/adjuntos` | Adjunto | P5 | CA-RF2-07 a CA-RF2-09 |
| Módulo de comunicación: comentarios (sin RF numerado propio) | - | UC-06 | `POST /tickets/{id}/comentarios`, `GET /tickets/{id}/comentarios` | Comentario | P5, P7 | CA-RF3-10 |
| RF3.1 Estados del ticket y transiciones | RN5 | UC-12 | `PATCH /tickets/{id}/estado` | Ticket, HistorialCambios | P7 | CA-RF3-04, CA-RF3-05 |
| RF3.2 Tomar y asignar tickets | RN11 | UC-10, UC-11 | `PATCH /tickets/{id}/asignar`, `GET /usuarios/agentes` | Ticket, HistorialCambios, Usuario | P7 | CA-RF3-01 a CA-RF3-03 |
| RF3.3 Escalar con motivo obligatorio | RN9 | UC-13 | `PATCH /tickets/{id}/estado` | Ticket, HistorialCambios, Usuario, UsuarioRol | P7 | CA-RF3-06, CA-RF3-07 |
| RF3.4 Confirmación o reapertura por el cliente | RN5 | UC-08 | `PATCH /tickets/{id}/estado` | Ticket | P5 | CA-RF3-08, CA-RF3-09 |
| RF3.5 Cierre automático a las 72 horas | RN6, RN12 | UC-19 | Tarea programada (sin endpoint) | Ticket, HistorialCambios | Sin pantalla propia | CA-RF3-11 |
| RF3.6 Rebote por estancamiento en Crítico | RN7, RN12 | UC-20 | Tarea programada (sin endpoint) | Ticket, HistorialCambios | Sin pantalla propia | CA-RF3-12 |
| RF4.1 Prioridad elegida por el cliente | RN8 | UC-03, UC-04 | `POST /tickets`, `POST /tickets/voz` | Ticket | P3, P4 | CA-RF4-01 |
| RF4.2 Nivel de atención y tiempo objetivo | RN8 | UC-13, UC-14 | `PATCH /tickets/{id}/nivel` | Ticket | P6, P7 | CA-RF4-02 a CA-RF4-05 |
| RF4.3 Categorías genéricas y propias | RN10 | UC-03, UC-17 | `GET /categorias`, `POST /categorias` | Categoria | P3, P4, P8 | CA-RF4-06 a CA-RF4-08 |
| RF5.1 Registro de cada cambio relevante | RN11, RN12 | UC-15 | `GET /tickets/{id}/historial` | HistorialCambios | P7 | CA-RF5-01 a CA-RF5-04 |
| RF5.2 Sin borrado físico de usuarios | RN2, RN3 | UC-18 | `PATCH /usuarios/{id}/desactivar`, `PATCH /usuarios/{id}/activar` | Usuario | P9 | CA-RF5-05, CA-RF5-06 |
| RF6.1 Aislamiento entre empresas | RN15 | UC-05, UC-09 | `GET /tickets`, `GET /tickets/{id}` | Todas las entidades llevan la empresa | P2, P6 | CA-RF6-01, CA-RF6-02 |
| RF6.2 Módulos habilitables por empresa | RN16 | UC-16 | `GET /empresas/{id}/configuracion`, `PATCH /empresas/{id}/configuracion` | ConfiguracionEmpresa | P8 | CA-RF6-03, CA-RF6-06 |
| RF6.3 Ventana de mantenimiento | RN17 | UC-16, UC-21 | `PATCH /empresas/{id}/configuracion` y control sobre las rutas de `/tickets` | ConfiguracionEmpresa | P8 | CA-RF6-04, CA-RF6-05 |
| RF7.1 Marcar un reclamo formal | RN18 | Sin caso de uso (no implementado) | Sin endpoint | Ticket (campo `esReclamoFormal`) | Sin pantalla | CA-RF7-01, CA-RF7-02 |
| RF7.2 Registrar un responsable externo | RN18 | Sin caso de uso (no implementado) | Sin endpoint | Ticket (campo `responsableExterno`) | Sin pantalla | CA-RF7-01, CA-RF7-02 |

Lectura de la última fila: el requerimiento RF7 está cubierto por el modelo de datos y por la lectura en la API, pero no tiene operación ni pantalla. Es una implementación parcial, declarada en `reglas-negocio-ticketdesk.md` (RN18) y en `criterios-aceptacion-ticketdesk.md`.

## 3. Matriz por endpoint

Cada endpoint de la API (27 en total) con el requerimiento que lo justifica. Los permisos indican quién puede invocarlo.

| N° | Endpoint | Quién puede | Casos de uso | Requerimiento | Reglas | Entidades | Pantallas |
|---|---|---|---|---|---|---|---|
| 1 | `GET /auth/empresas` | Público | UC-01, UC-02 | RF1.1 | RN1 | Empresa | P1 |
| 2 | `POST /auth/login` | Público | UC-01 | RF1.3, RF1.5 | RN1, RN2 | Usuario, UsuarioRol | P1 |
| 3 | `POST /auth/register` | Público | UC-02 | RF1.1, RF1.3 | RN1 | Usuario | P1 |
| 4 | `POST /tickets` | Cliente | UC-03 | RF2.1, RF4.1 | RN8, RN10 | Ticket, Categoria | P3 |
| 5 | `GET /tickets` | Cliente (los propios) y personal (los de la empresa) | UC-05, UC-09 | RF6.1 | RN15 | Ticket | P2, P6 |
| 6 | `GET /tickets/{id}` | Dueño del ticket y personal de la empresa | UC-05 | RF6.1 | RN15 | Ticket, Comentario, Adjunto | P5, P7 |
| 7 | `GET /tickets/{id}/historial` | Personal de soporte | UC-15 | RF5.1 | RN11, RN12 | HistorialCambios | P7 |
| 8 | `PATCH /tickets/{id}/estado` | Cliente dueño (cierre y reapertura) y agente o supervisor | UC-08, UC-12, UC-13 | RF3.1, RF3.3, RF3.4 | RN5, RN9, RN11 | Ticket, HistorialCambios | P5, P7 |
| 9 | `PATCH /tickets/{id}/asignar` | Agente y supervisor | UC-10, UC-11 | RF3.2 | RN11 | Ticket, HistorialCambios | P7 |
| 10 | `PATCH /tickets/{id}/nivel` | Agente y supervisor | UC-14 | RF4.2 | RN8, RN11 | Ticket, HistorialCambios | P7 |
| 11 | `POST /tickets/voz/sugerencia` | Cliente | UC-04 | RF2.2 | RN14 | Categoria | P4 |
| 12 | `POST /tickets/voz` | Cliente | UC-04 | RF2.2 | RN14 | Ticket, Categoria | P4 |
| 13 | `POST /tickets/{id}/comentarios` | Dueño del ticket y personal | UC-06 | Módulo de comunicación | - | Comentario | P5, P7 |
| 14 | `GET /tickets/{id}/comentarios` | Dueño del ticket y personal | UC-05 | Módulo de comunicación | - | Comentario | P5, P7 |
| 15 | `POST /tickets/{id}/adjuntos` | Cliente | UC-07 | RF2.3 | RN13 | Adjunto | P5 |
| 16 | `GET /tickets/{id}/adjuntos` | Dueño del ticket y personal | UC-05 | RF2.3 | RN13 | Adjunto | P5 |
| 17 | `GET /categorias` | Cualquier usuario autenticado | UC-03, UC-04, UC-09, UC-17 | RF4.3 | RN10 | Categoria | P3, P4, P6, P8 |
| 18 | `POST /categorias` | Administrador | UC-17 | RF4.3 | RN10 | Categoria | P8 |
| 19 | `GET /empresas/{id}/configuracion` | Usuario de esa empresa | UC-16 | RF6.2, RF6.3 | RN16, RN17 | ConfiguracionEmpresa | P8, menú |
| 20 | `PATCH /empresas/{id}/configuracion` | Administrador de esa empresa | UC-16 | RF6.2, RF6.3 | RN16, RN17 | ConfiguracionEmpresa | P8 |
| 21 | `GET /usuarios/me` | Cualquier usuario autenticado | UC-01 | RF1.4 | RN4 | Usuario, UsuarioRol | Menú y sesión |
| 22 | `GET /usuarios/agentes` | Personal de soporte | UC-11 | RF3.2 | RN9 | Usuario | P7 |
| 23 | `GET /usuarios` | Administrador | UC-18 | RF1.2 | RN4 | Usuario | P9 |
| 24 | `POST /usuarios` | Administrador | UC-18 | RF1.2 | RN1, RN4 | Usuario, UsuarioRol | P9 |
| 25 | `POST /usuarios/{id}/roles` | Administrador | UC-18 | RF1.4 | RN4 | UsuarioRol | P9 |
| 26 | `PATCH /usuarios/{id}/desactivar` | Administrador | UC-18 | RF5.2 | RN2, RN3 | Usuario | P9 |
| 27 | `PATCH /usuarios/{id}/activar` | Administrador | UC-18 | RF5.2 | RN2 | Usuario | P9 |

## 4. Matriz de reglas de negocio

Cada regla con los casos de uso que la aplican y los criterios que la verifican.

| Regla | Descripción breve | Casos de uso | Criterios |
|---|---|---|---|
| RN1 | El email es único por empresa | UC-01, UC-02, UC-18 | CA-RF1-01, CA-RF1-03 |
| RN2 | Los usuarios nunca se eliminan | UC-18 | CA-RF1-06, CA-RF5-06 |
| RN3 | Un agente con tickets abiertos no se puede desactivar | UC-18 | CA-RF5-05 |
| RN4 | Un usuario puede tener más de un rol | UC-18 | CA-RF1-04, CA-RF1-05 |
| RN5 | El ticket pasa por Resuelto antes de Cerrado | UC-08, UC-12 | CA-RF3-05, CA-RF3-08 |
| RN6 | Cierre automático a las 72 horas | UC-19 | CA-RF3-11 |
| RN7 | Rebote a Nivel 1 por estancamiento en Crítico | UC-20 | CA-RF3-12 |
| RN8 | Prioridad y nivel de atención son independientes | UC-03, UC-14 | CA-RF4-01 a CA-RF4-05 |
| RN9 | El escalado reasigna a un supervisor, con motivo obligatorio | UC-13 | CA-RF3-06, CA-RF3-07 |
| RN10 | Categorías híbridas | UC-03, UC-17 | CA-RF4-06 a CA-RF4-08 |
| RN11 | Todo cambio relevante queda registrado | UC-10 a UC-15 | CA-RF5-01, CA-RF5-04 |
| RN12 | Los cambios automáticos se distinguen de los de una persona | UC-19, UC-20 | CA-RF5-02 |
| RN13 | Solo el cliente sube fotos | UC-07 | CA-RF2-07 a CA-RF2-09 |
| RN14 | Trazabilidad de los tickets por voz | UC-04 | CA-RF2-03, CA-RF2-04 |
| RN15 | Aislamiento de datos entre empresas | UC-05, UC-09 | CA-RF6-01, CA-RF6-02 |
| RN16 | Configuración flexible de módulos por empresa | UC-16 | CA-RF6-03 |
| RN17 | Ventana de mantenimiento configurable | UC-16, UC-21 | CA-RF6-04, CA-RF6-05 |
| RN18 | Reclamo formal y responsable externo | Sin caso de uso | CA-RF7-01, CA-RF7-02 |

## 5. Matriz de requerimientos no funcionales

| Requerimiento | Cómo se cumple | Dónde verlo |
|---|---|---|
| RNF1 Seguridad | Autenticación con JWT sin sesiones en el servidor, contraseñas con hash BCrypt, validación de rol en cada operación e identificación por empresa y email | UC-01; `reglas-negocio-ticketdesk.md` (RN1); pruebas B13 a B16 y D7 a D10 |
| RNF2 Disponibilidad | Ventana de mantenimiento por empresa que rechaza altas y cambios sobre tickets con un aviso | UC-21; CA-RF6-04 |
| RNF3 Escalabilidad y multi-empresa | Una sola instalación atiende a varias empresas, con configuración en una columna JSONB que se amplía sin migraciones | RF6; `DER-ticketdesk.md` |
| RNF4 Trazabilidad y auditoría | Tabla genérica de historial con filas nuevas por cada cambio y autor vacío para los cambios del sistema | UC-15, UC-19, UC-20; CA-RF5-01 a CA-RF5-04 |
| RNF5 Usabilidad | Canal por voz con confirmación, recorrido de estados, avisos junto a la acción y separación entre prioridad y nivel | UC-04; `wireframes-pantallas-ticketdesk.md` |
| RNF6 Mantenibilidad | Arquitectura por capas (controladores, servicios, repositorios) | `diagramas-uml-ticketdesk.md`, diagrama de componentes |
| RNF7 Rendimiento | Los filtros del listado se resuelven en la base de datos y no en memoria | UC-09; `GET /tickets` |

## 6. Cobertura

| Verificación | Resultado |
|---|---|
| Requerimientos funcionales con al menos un caso de uso | RF1 a RF6 completos. RF7 sin caso de uso porque no tiene operación |
| Requerimientos funcionales con al menos un criterio de aceptación | RF1 a RF7 |
| Endpoints con requerimiento, caso de uso y pantalla identificados | 27 de 27 |
| Reglas de negocio con al menos un criterio | RN1 a RN18 |
| Casos de uso con al menos un requerimiento asociado | 21 de 21 |

La única brecha identificada es RF7 y RN18, que se resolvería con una operación para marcar el reclamo formal y registrar al responsable externo, y su pantalla correspondiente.
