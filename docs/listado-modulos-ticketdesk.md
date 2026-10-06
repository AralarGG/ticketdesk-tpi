# TicketDesk: Definición de Módulos y Requerimientos del Sistema

## Documento base: Trabajo Final Integrador UTN TUPaD

Este documento define los módulos funcionales de TicketDesk y los requerimientos (funcionales y no funcionales) que el sistema debe cumplir. Se corresponde con el modelo de datos (`DER-ticketdesk.md`), la especificación de la API (`endpoints-api-ticketdesk.md`) y el plan de desarrollo (`plan-desarrollo-ticketdesk.md`).

---

# Parte 1: Módulos del Sistema

## 1. Módulo de Autenticación y Usuarios

**Responsabilidad:** login, registro, y gestión de identidad de usuarios (clientes, agentes, supervisores, administradores).

- Login con JWT, identificando siempre empresa + email (no email solo, ver Requerimiento No Funcional 1)
- Registro de clientes
- Gestión de roles (`ROLE_USER`, `ROLE_AGENT`, `ROLE_SUPERVISOR`, `ROLE_ADMIN`)
- Soporte para que un mismo usuario tenga más de un rol habilitado a la vez (empresas grandes)
- Desactivación de usuarios (soft delete), con reasignación obligatoria de tickets activos si es agente

**Entidades relacionadas:** `Usuario`, `UsuarioRol`

**Estado:** implementado (Etapa 1 y 2 del plan de desarrollo). Login y registro funcionando con JWT, probado localmente.

---

## 2. Módulo de Gestión de Tickets (núcleo del sistema)

**Responsabilidad:** todo el ciclo de vida de un ticket, desde su creación hasta su cierre.

- Alta de ticket (por formulario o por voz)
- Listado y filtrado de tickets (por estado, prioridad, categoría, agente)
- Cambio de estado, siguiendo el flujo definido (ver Requerimiento Funcional 3)
- Asignación y reasignación de agentes
- Escalado a supervisor
- Clasificación por nivel de atención (`NIVEL_1` a `CRITICO`), independiente de la prioridad percibida por el cliente
- Cierre automático por silencio del cliente y rebote por estancamiento en nivel máximo

**Entidades relacionadas:** `Ticket`, `HistorialCambios`

**Estado:** implementado (Etapa 3 del plan de desarrollo). CRUD funcionando, probado localmente junto con la creación automática de tablas.

---

## 3. Módulo de Comunicación (Comentarios y Adjuntos)

**Responsabilidad:** el intercambio de información dentro de un ticket entre cliente y agente.

- Alta y listado de comentarios
- Subida de adjuntos, restringida exclusivamente a clientes (los agentes responden solo con texto)
- Integración con Cloudinary para almacenamiento de imágenes (nunca se guarda el archivo en la base de datos)

**Entidades relacionadas:** `Comentario`, `Adjunto`

**Estado:** implementado (Etapa 4 del plan de desarrollo). Comentarios y adjuntos con control de acceso por empresa y dueño del ticket, y subida de imágenes a Cloudinary.

---

## 4. Módulo de Categorías

**Responsabilidad:** catálogo de categorías de tickets, con un componente genérico compartido y otro propio de cada empresa.

- Listado de categorías visibles para una empresa (genéricas + propias)
- Alta de categorías propias por empresa, desde el panel de administración

**Entidades relacionadas:** `Categoria`

**Estado:** implementado. Listado de categorías visibles por empresa y alta de categorías propias por el administrador.

---

## 5. Módulo de Configuración por Empresa (Multi-tenant)

**Responsabilidad:** permitir que cada empresa cliente personalice qué módulos ve habilitados, y configure su ventana de mantenimiento.

- Habilitar/deshabilitar módulos del menú base por empresa
- Configuración de ventana de mantenimiento (horario en que el sistema no procesa altas ni cambios)

**Entidades relacionadas:** `Empresa`, `ConfiguracionEmpresa`

**Estado:** implementado (Etapa 7 del plan de desarrollo). Endpoints de lectura y modificación de la configuración, validación de módulos habilitados en la API y ventana de mantenimiento aplicada por un filtro.

---

## 6. Módulo de Voz (Voice Speaker, funcionalidad diferencial)

**Responsabilidad:** permitir la creación de tickets mediante mensajes de voz.

- Recepción y almacenamiento del audio original
- Transcripción de voz a texto, que realiza el navegador del cliente (Web Speech API)
- Sugerencia automática de categoría por palabras clave, con confirmación del usuario antes de crear el ticket

**Entidades relacionadas:** `Ticket` (campos `canal_origen`, `audio_url`, `transcripcion_original`)

**Estado:** backend implementado (Etapa 8 del plan de desarrollo): sugerencia de categoría por palabras clave y alta de ticket con audio y transcripción. La captura y transcripción del audio en el navegador se resuelve en el frontend.

---

## Relación entre Módulos y Arquitectura General

```
┌─────────────────────────────────────────────────────┐
│                     Frontend (React)                  │
└───────────────────────┬───────────────────────────────┘
                         │ HTTP (JSON) + JWT
┌───────────────────────▼───────────────────────────────┐
│                  Backend (Spring Boot)                 │
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Autenticación│  │   Tickets    │  │ Comunicación │  │
│  │  y Usuarios  │  │  (núcleo)    │  │ (Comentarios │  │
│  │              │  │              │  │  y Adjuntos) │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                                                         │
│  ┌──────────────┐  ┌──────────────┐                    │
│  │  Categorías  │  │Config. Empresa│                   │
│  │  (híbridas)  │  │(Multi-tenant) │                   │
│  └──────────────┘  └──────────────┘                    │
│                                                         │
│  ┌──────────────────────────────────┐                  │
│  │       Módulo de Voz (Voice)      │                  │
│  └──────────────────────────────────┘                  │
└───────────────────────┬───────────────────────────────┘
                         │ JDBC
┌───────────────────────▼───────────────────────────────┐
│              PostgreSQL (esquema relacional            │
│              + columnas JSONB para config flexible)    │
└─────────────────────────────────────────────────────────┘
```

## Estado de Implementación

| Módulo | Estado |
|---|---|
| Autenticación y Usuarios | Implementado y probado (login/registro con JWT, multi-rol, empresa+email) |
| Gestión de Tickets | Implementado y probado (CRUD completo, nivel de atención, cierre automático) |
| Comunicación | Implementado |
| Categorías | Implementado (modelo híbrido) |
| Configuración por Empresa | Implementado (módulos y ventana de mantenimiento) |
| Voz | Backend implementado; captura de audio en el frontend |

---

# Parte 2: Requerimientos del Sistema

## Requerimientos Funcionales

**RF1: Gestión de identidad**
- El sistema debe permitir que un cliente se registre y un agente/supervisor/administrador sea dado de alta dentro de una empresa.
- El sistema debe permitir que una misma persona (mismo email) tenga cuentas independientes en distintas empresas.
- El sistema debe permitir que un usuario tenga más de un rol habilitado a la vez.

**RF2: Creación de tickets**
- El sistema debe permitir crear un ticket completando un formulario (título, descripción, categoría, prioridad).
- El sistema debe permitir crear un ticket mediante un mensaje de voz, mostrando la transcripción para confirmación antes de crear el ticket definitivamente.
- El sistema debe permitir adjuntar fotos a un ticket o comentario, exclusivamente por parte del cliente.

**RF3: Ciclo de vida del ticket**
- El sistema debe llevar el ticket a través de los estados: nuevo, asignado, en progreso, esperando al cliente, escalado, resuelto, reabierto, cerrado.
- El sistema debe permitir que un agente se autoasigne un ticket, o que se le asigne manualmente.
- El sistema debe permitir escalar un ticket a un supervisor, con motivo obligatorio.
- El sistema debe cerrar automáticamente un ticket en estado "resuelto" si el cliente no responde ni reabre dentro de las 72 horas.
- El sistema debe hacer rebotar a Nivel 1, marcado como "reincidente", un ticket que queda estancado 5 días en el nivel de atención máximo (Crítico) sin resolverse.

**RF4: Clasificación de tickets**
- El sistema debe permitir que el cliente asigne una prioridad (baja, media, alta) a su propio ticket, independiente de la clasificación técnica interna.
- El sistema debe clasificar cada ticket en un nivel de atención (Nivel 1, Nivel 2, Nivel 3, Crítico), cada uno con un tiempo objetivo de resolución (SLA) propio.
- El sistema debe permitir categorizar un ticket con una categoría genérica del sistema o una propia de la empresa.

**RF5: Trazabilidad**
- El sistema debe registrar cada cambio relevante de un ticket (estado, agente asignado, categoría, prioridad, nivel de atención), incluyendo quién lo hizo (o que fue automático), cuándo, y por qué.
- El sistema no debe eliminar físicamente usuarios ni tickets: los usuarios se desactivan, preservando el historial.

**RF6: Multi-empresa (multi-tenant)**
- El sistema debe aislar completamente los datos de cada empresa cliente entre sí.
- El sistema debe permitir que cada empresa habilite o deshabilite módulos del menú base según sus necesidades.
- El sistema debe permitir que cada empresa configure su propia ventana de mantenimiento.

**RF7: Casos fuera del alcance del software**
- El sistema debe permitir marcar un ticket como reclamo formal, sujeto a normativa de protección al consumidor.
- El sistema debe permitir registrar los datos de contacto de un responsable externo al software, para casos que requieren intervención humana directa.

## Requerimientos No Funcionales

**RNF1: Seguridad**
- El sistema debe autenticar cada request mediante JWT, sin mantener sesiones en el servidor (stateless).
- El sistema debe identificar sin ambigüedad a cada usuario autenticado, incluso si su email coincide con el de otra cuenta en otra empresa (login por combinación empresa+email).
- El sistema debe restringir cada operación según el rol del usuario autenticado.
- Las contraseñas deben almacenarse siempre encriptadas (hash), nunca en texto plano.

**RNF2: Disponibilidad**
- El sistema debe permitir configurar una ventana de mantenimiento por empresa, durante la cual se informa la indisponibilidad temporal y no se procesan altas ni cambios.

**RNF3: Escalabilidad y multi-tenant**
- El sistema debe soportar múltiples empresas operando simultáneamente sobre la misma infraestructura, sin necesidad de instancias separadas.
- El esquema de configuración por empresa debe poder ampliarse (nuevos módulos, nuevos parámetros) sin requerir migraciones disruptivas del esquema relacional (de ahí el uso de columnas JSONB para configuración flexible).

**RNF4: Trazabilidad y auditoría**
- Toda modificación relevante debe quedar registrada de forma inmutable (no se sobrescribe el historial, se agregan nuevas filas).
- El sistema debe distinguir entre cambios hechos por una persona y cambios hechos automáticamente por el sistema (ej. cierre por vencimiento, rebote por estancamiento).

**RNF5: Usabilidad**
- El sistema debe minimizar la fricción para crear un ticket, ofreciendo un canal alternativo por voz además del formulario tradicional.
- El sistema no debe hacer que el cliente sienta que se subestima la urgencia de su reclamo: por eso la prioridad percibida por el cliente y el nivel de atención técnico interno se mantienen como conceptos separados.

**RNF6: Mantenibilidad**
- El código del backend debe seguir una arquitectura por capas (controller, service, repository) que separe responsabilidades y facilite el mantenimiento y las pruebas.
- Las decisiones de diseño no triviales deben quedar documentadas junto a la entidad o clase correspondiente, no solo en documentos externos.

**RNF7: Rendimiento**
- Las consultas de listado de tickets deben soportar filtros (estado, prioridad, categoría, agente) resueltos a nivel de base de datos, no en memoria, para mantener buen desempeño con volumen alto de tickets.
