# TicketDesk: Plan de Desarrollo Técnico por Etapas

## Documento base: Trabajo Final Integrador UTN TUPaD

Este documento define el orden concreto de implementación, a nivel de código, una vez cerrada la etapa de diseño (DER, endpoints, flujos de usuario y wireframes ya documentados). El objetivo es evitar bloqueos entre backend y frontend, y priorizar lo que sostiene al resto del sistema.

---

## Criterio de priorización

Se desarrolla primero lo que otras partes del sistema necesitan para funcionar, dejando para el final lo que depende de todo lo anterior (funcionalidades diferenciales como el ingreso por voz) y lo que es más cosmético que funcional.

---

## Etapa 1: Base del Backend (sin lógica de negocio todavía)

**Objetivo:** tener el proyecto Spring Boot corriendo, conectado a PostgreSQL, con las tablas creadas.

1. Inicializar proyecto Spring Boot (Spring Initializr: Web, JPA, Security, PostgreSQL Driver)
2. Configurar conexión a PostgreSQL (local primero, luego a la instancia en la nube)
3. Crear las entidades JPA según el DER: `Empresa`, `Usuario`, `Categoria`, `Ticket`, `Comentario`, `Adjunto`, `TicketHistory`, `ConfiguracionEmpresa`
4. Generar las tablas (vía JPA `ddl-auto` en desarrollo, o migraciones si el equipo prefiere Flyway/Liquibase)
5. Cargar datos de prueba (seed) para las categorías genéricas y una empresa de ejemplo

**Sin esto no se puede avanzar con nada más**, porque todo el resto depende de que las tablas y entidades existan.

---

## Etapa 2: Autenticación y Roles

**Objetivo:** tener login funcionando con JWT y control de acceso por rol.

1. Implementar registro de usuario (`POST /auth/register`)
2. Implementar login con JWT (`POST /auth/login`)
3. Configurar Spring Security: filtro de validación de JWT en cada request
4. Definir los roles (`ROLE_USER`, `ROLE_AGENT`, `ROLE_SUPERVISOR`, `ROLE_ADMIN`) y las reglas de acceso por endpoint
5. Probar con Postman/Insomnia que cada rol accede solo a lo que le corresponde

**Depende de:** Etapa 1 (necesita la entidad `Usuario` ya creada).
**Habilita:** todo lo que sigue, porque cada endpoint futuro va a requerir un rol autenticado.

---

## Etapa 3: CRUD de Tickets (núcleo del sistema)

**Objetivo:** poder crear, ver, listar y cambiar el estado de un ticket, vía formulario (no por voz todavía).

1. Endpoint de alta de ticket (`POST /tickets`)
2. Endpoint de listado con filtros (`GET /tickets`)
3. Endpoint de detalle (`GET /tickets/{id}`)
4. Endpoint de cambio de estado (`PATCH /tickets/{id}/estado`), generando automáticamente la fila en `ticket_history`
5. Endpoint de asignación de agente (`PATCH /tickets/{id}/asignar`)
6. Validar las reglas de negocio: un ticket nuevo no puede pasar directo a "resuelto" sin pasar por los estados intermedios

**Depende de:** Etapa 2 (necesita saber qué usuario está haciendo la acción y con qué rol).
**Es el corazón del sistema**: sin esto, ninguna otra funcionalidad tiene sentido.

---

## Etapa 4: Comentarios y Adjuntos

**Objetivo:** poder conversar dentro de un ticket, con la restricción de fotos solo para clientes.

1. Endpoint de alta de comentario (`POST /tickets/{id}/comentarios`)
2. Endpoint de listado de comentarios (`GET /tickets/{id}/comentarios`)
3. Integración con Cloudinary para subida de imágenes
4. Endpoint de alta de adjunto (`POST /tickets/{id}/adjuntos`), con la validación de rol (`ROLE_USER` únicamente)

**Depende de:** Etapa 3 (los comentarios y adjuntos cuelgan de un ticket ya existente).

---

## Etapa 5: Frontend, Primeras Pantallas

**Objetivo:** tener una interfaz mínima usable, consumiendo lo ya construido en el backend (Etapas 1 a 4).

1. Configurar proyecto React + TypeScript, estructura de carpetas y ruteo
2. Pantalla de Login
3. Panel de Usuario/Cliente (listado de tickets propios)
4. Pantalla de Nuevo Ticket (formulario)
5. Pantalla de Detalle de Ticket (cliente): ver comentarios, adjuntar fotos, responder
6. Panel de Agente (listado con filtros)
7. Pantalla de Detalle de Ticket (agente): cambiar estado, asignarse, comentar

**Depende de:** Etapas 2, 3 y 4 (necesita que los endpoints ya existan y funcionen para poder consumirlos).

**Nota:** esta etapa puede arrancar en paralelo con la Etapa 4 si el equipo se divide el trabajo, ya que el frontend de login y panel de cliente no depende de que los adjuntos estén terminados.

---

## Etapa 6: Escalado y Roles Avanzados

**Objetivo:** sumar el flujo de supervisor y reasignación de agentes.

1. Backend: endpoint de escalado (cambio de estado a `escalado` + reasignación automática a `ROLE_SUPERVISOR`)
2. Backend: endpoint de baja de agente con validación de tickets pendientes (`PATCH /usuarios/{id}/desactivar`)
3. Frontend: botón de "Escalar a Supervisor" en la vista de agente
4. Frontend: panel de administración para gestión de agentes

**Depende de:** Etapas 3 y 5 (ya debe existir el flujo básico de tickets y las pantallas de agente).

---

## Etapa 7: Configuración por Empresa (JSONB)

**Objetivo:** permitir que cada empresa habilite/deshabilite módulos y configure su ventana de mantenimiento.

1. Backend: endpoint de lectura/escritura de configuración (`GET` / `PATCH /empresas/{id}/configuracion`)
2. Backend: middleware que verifique la ventana de mantenimiento antes de procesar altas/cambios de estado
3. Frontend: panel de administración de configuración de empresa

**Depende de:** Etapa 1 (la tabla `configuracion_empresa` ya debe existir) y Etapa 6 (reutiliza el panel de administración).

---

## Etapa 8: Apertura de Tickets por Voz (Funcionalidad Diferencial)

**Objetivo:** sumar el canal de voz como forma alternativa de creación de tickets.

1. Investigar e integrar una API de speech-to-text (a definir: Google Speech-to-Text, Whisper de OpenAI, u otra)
2. Backend: endpoint `POST /tickets/voz`, que recibe el audio, lo transcribe, sugiere categoría por palabras clave y guarda `audio_url` + `transcripcion_original`
3. Frontend: pantalla de grabación + pantalla de confirmación de la transcripción antes de crear el ticket

**Depende de:** Etapa 3 (reutiliza toda la lógica de creación de ticket ya construida, solo cambia el origen de los datos).

**Se deja para el final** porque es la funcionalidad diferencial, no crítica para que el sistema funcione, y depende de una integración externa (API de voz) que puede llevar tiempo de investigación.

---

## Etapa 9: Despliegue en la Nube

**Objetivo:** cumplir con el requisito de tener al menos un componente corriendo en la nube.

1. Desplegar backend en Render o Railway
2. Desplegar frontend en Vercel o Netlify
3. Migrar la base de datos a una instancia en la nube (Supabase o similar)
4. Configurar variables de entorno (credenciales de base de datos, claves de API de voz, Cloudinary)

**Puede arrancar en paralelo** desde etapas tempranas (por ejemplo, desplegar una versión mínima ya en la Etapa 3), para no dejar todo el trabajo de infraestructura para el final.

---

## Resumen Visual del Orden de Dependencias

```
Etapa 1 (Base Backend)
    │
    ▼
Etapa 2 (Auth y Roles)
    │
    ▼
Etapa 3 (CRUD Tickets) ──────┐
    │                        │
    ▼                        ▼
Etapa 4 (Comentarios/    Etapa 5 (Frontend
         Adjuntos)             básico)
    │                        │
    └───────────┬────────────┘
                ▼
        Etapa 6 (Escalado y Roles Avanzados)
                │
                ▼
        Etapa 7 (Config. por Empresa)
                │
                ▼
        Etapa 8 (Voz, funcionalidad diferencial)

Etapa 9 (Despliegue): en paralelo, desde Etapa 3 en adelante
```

## Nota sobre Repartición de Trabajo en Equipo

Dado que el equipo son tres integrantes, una posible división (a confirmar entre todos) es:

- Una persona enfocada en backend (Etapas 1 a 4, 6, 7, 8 del lado servidor)
- Una persona enfocada en frontend (Etapa 5 en adelante, del lado cliente)
- Una persona rotando entre ambos según necesidad, y llevando adelante el despliegue (Etapa 9) y la documentación

Esta repartición es una propuesta inicial: se ajustará según disponibilidad real de cada integrante durante las próximas semanas.
