# TicketDesk - Sistema de Gestión de Tickets de Soporte

## Trabajo Final Integrador - UTN TUPaD

**Integrantes:**
- Santiago Raúl Salinas
- Ezequiel Sanabria
- Joaquín Del Valle Lietti

**Tutor/a:** Gerardo Adrian Herrera

---

## Problema

Las empresas que brindan soporte a clientes (equipos de atención al usuario en productos de software, videojuegos o plataformas online) suelen manejar los reclamos y consultas de forma desordenada: por planillas sueltas, chats internos o correos electrónicos sin trazabilidad. Esto genera varios problemas concretos:

- **Solicitudes que se pierden** porque no quedan registradas en un único lugar
- **Duplicación de esfuerzos** cuando dos agentes distintos atienden el mismo reclamo sin saberlo
- **Falta de visibilidad** sobre qué tickets están pendientes, quién los está atendiendo y hace cuánto tiempo
- **Imposibilidad de priorizar** correctamente entre consultas urgentes y consultas de baja relevancia
- **Ausencia de historial**, lo que dificulta entender el contexto de un reclamo si cambia el agente asignado

TicketDesk busca resolver esto centralizando todo el ciclo de vida de un ticket de soporte (desde que el usuario lo crea hasta que se resuelve) en un único sistema accesible tanto para usuarios como para agentes, con estados claros y trazabilidad completa.

## Objetivos del Proyecto

- Centralizar la carga y el seguimiento de tickets de soporte en una sola plataforma
- Permitir que los agentes prioricen y distribuyan la carga de trabajo de forma visible
- Reducir el tiempo de respuesta al usuario mediante un flujo de estados claro
- Dejar registro histórico de cada ticket para facilitar auditoría y traspaso entre agentes

## Propuesta de Valor y Enfoque Comercial

Para las organizaciones modernas, la calidad del servicio de soporte define la retención del cliente. TicketDesk transforma la gestión de incidencias operativas mediante:

- **Arquitectura Multi-tenant Adaptable:** un único ecosistema multi-empresa donde cada cliente opera con sus parámetros y módulos independientes sin comprometer la seguridad ni el aislamiento de datos.
- **Omnicanalidad Inteligente (Creación de Tickets por Voz):** integración de módulos de entrada por voz para eliminar barreras de fricción en los usuarios finales, permitiendo la apertura de tickets de manera ágil e intuitiva.
- **Trazabilidad y Auditabilidad Total:** garantía de control operativo donde no existe pérdida de información ni modificaciones inconsistentes, protegiendo el historial del cliente y los registros de servicio.

**Enfoque del producto:** TicketDesk se piensa como un producto genérico, no atado a un rubro específico: cualquier empresa que necesite gestionar reclamos o consultas de clientes (tecnología, salud, comercio, servicios, etc.) puede usarlo, partiendo de un mismo menú base con módulos que se pueden habilitar o deshabilitar según la empresa cliente.

## Alcance del MVP

**Roles de usuario:**
- **Usuario/Cliente:** crea tickets, visualiza el estado de sus propios tickets y puede agregar comentarios o información adicional
- **Agente de soporte:** visualiza todos los tickets, puede tomarlos, reasignarlos, cambiar su estado y responder al usuario
- **Administrador (opcional según tiempo disponible):** gestiona categorías, prioridades y usuarios/agentes del sistema

**Funcionalidades principales:**

| Funcionalidad | Descripción |
|---|---|
| Alta de ticket | El usuario completa título, descripción, categoría (ej. bug, consulta, facturación) y prioridad (baja/media/alta). También puede crearse por voz (ver sección "Apertura de Tickets por Voz") |
| Asignación | Un agente toma el ticket manualmente, o se asigna automáticamente por categoría |
| Seguimiento de estado | Flujo ampliado pensado para un caso de uso real (ver "Flujo de Estados del Ticket"), con historial de cambios y fecha de cada transición |
| Comentarios | Usuario y agente pueden agregar comentarios dentro del ticket para dar contexto o pedir información |
| Adjuntos | El usuario puede adjuntar fotos a un ticket o comentario; los agentes responden únicamente con texto (ver justificación en "Reglas de Negocio") |
| Panel de agente | Listado filtrable por estado, prioridad, categoría y agente asignado |
| Panel de usuario | Vista simplificada con solo los tickets propios y su estado actual |
| Autenticación | Login diferenciado por rol (usuario/agente), con rutas protegidas según permisos |

**Fuera de alcance (para esta primera versión):**
- Notificaciones automáticas por email o push
- Chat en tiempo real entre usuario y agente
- Métricas o reportes avanzados (dashboards, SLA, tiempos promedio de resolución)
- Integraciones con sistemas externos (Slack, Jira, etc.)

Estas exclusiones no se descartan a futuro, pero quedan fuera del MVP para poder entregar un producto funcional y bien probado dentro de los plazos de la cursada.
---

---
## 🎨 Wireframes y Prototipos de Interfaz (Mockups)

El diseño de la interfaz prioriza la claridad operativa, reduciendo la carga cognitiva tanto para el cliente como para el agente de soporte.

### 1. Panel de Creación de Ticket (Vista Cliente - Web y Voz)

```mermaid
graph TD
    subgraph Dashboard_Cliente ["🖥️ Portal del Cliente"]
        A["[ Formulario de Ticket ]"] --> B["Campo: Título y Categoría"]
        B --> C["Campo: Descripción"]
        C --> D["Botón: Adjuntar Captura (Solo Clientes)"]
        
        A --> E["🎙️ Módulo de Voz"]
        E --> F["[ Botón: Grabar Audio ]"]
        F --> G["Visualización: Transcripción Automática previa al envío"]
        
        D --> H["[ Botón: Crear Ticket ]"]
        G --> H
    end
```

### 2. Flujo de Gestión y Cambios de Estado (Vista Agente / Supervisor)

```mermaid
stateDiagram-v2
    [*] --> Nuevo : Cliente crea ticket (Form/Voz)
    Nuevo --> Asignado : Agente toma el ticket
    Asignado --> En_Progreso : Inicio de diagnóstico
    En_Progreso --> Esperando_Cliente : Agente solicita aclaración (Solo Texto)
    Esperando_Cliente --> En_Progreso : Cliente responde
    En_Progreso --> Escalado : Requiere intervención
    Escalado --> Asignado_Supervisor : Reasignación a Supervisor + Motivo obligatorio
    Asignado_Supervisor --> Resuelto : Solución confirmada
    En_Progreso --> Resuelto : Solución confirmada por Agente
    Resuelto --> Cerrado : Confirmación de cierre
    Resuelto --> Reabierto : Cliente reabre el caso
    Reabierto --> En_Progreso : Reasignación
```

## Flujo de Estados del Ticket (Workflow)

Pensando en un caso de uso real (no solo académico), el ciclo de vida de un ticket contempla más estados que un flujo básico de alta/resolución:

```
nuevo → asignado → en progreso → esperando al cliente → resuelto → cerrado
                        ↓
                    escalado
                        ↓
                    reabierto (si el cliente no está conforme con la resolución)
```

- **Nuevo:** el ticket fue creado y aún no fue tomado por ningún agente
- **Asignado:** un agente lo tomó pero todavía no comenzó a trabajarlo
- **En progreso:** el agente está trabajando activamente en la resolución
- **Esperando al cliente:** el agente necesita información adicional del usuario para continuar
- **Escalado:** el ticket requiere intervención de un agente con rol `ROLE_SUPERVISOR`
- **Resuelto:** el agente marcó el problema como solucionado
- **Reabierto:** el cliente indica que el problema persiste tras haber sido marcado como resuelto
- **Cerrado:** estado final, ya no admite reapertura

Cada transición (incluyendo reasignación de agente, cambio de categoría o prioridad, no solo el estado) queda registrada en `ticket_history` con fecha, responsable y motivo cuando aplica.

## Apertura de Tickets por Voz (Voice Speaker)

Como funcionalidad diferencial del producto, el sistema permite que un usuario abra un ticket mediante un **mensaje de voz**, en lugar de completar el formulario manualmente. El sistema reconoce palabras clave dentro del audio (por ejemplo: "no funciona", "error de pago", "no puedo iniciar sesión") para autocompletar categoría y descripción inicial del ticket, mediante una API de reconocimiento de voz a texto (speech-to-text).

Se guarda tanto el **audio original** como su **transcripción textual**, para poder auditar si la categorización automática fue correcta y no depender únicamente de la interpretación del sistema de reconocimiento de voz.

## Disponibilidad y Ventana de Mantenimiento

El sistema contempla una **ventana de mantenimiento programada** (horario configurable por empresa) durante la cual:

- La aplicación muestra un aviso de indisponibilidad temporal a los usuarios
- No se procesan nuevas altas de tickets ni cambios de estado durante ese lapso
- Permite realizar tareas de mantenimiento, actualizaciones o backups sin afectar el uso normal en horario productivo

## Stack Tecnológico

- **Backend:** Java 21 + Spring Boot + JPA - se eligió por ser el stack con el que el equipo ya tiene experiencia previa (TPIs de Programación 3 y Programación IV), lo que reduce la curva de aprendizaje y permite enfocar el tiempo en el diseño del producto
- **Frontend:** React + TypeScript - mismo criterio: stack ya trabajado en cursadas anteriores
- **Base de datos:** PostgreSQL - modelo relacional adecuado dado que las entidades (tickets, usuarios, categorías, comentarios) tienen relaciones bien definidas y consultas que se benefician de integridad referencial
- **Despliegue:** Backend en Render/Railway, Frontend en Vercel/Netlify - servicios gratuitos con despliegue continuo desde GitHub, cumpliendo el requisito de tener al menos un componente corriendo en la nube

## Diseño Técnico Previo al Desarrollo

Antes de escribir código, el equipo definió tres aspectos clave de diseño:

**1. Modelo de datos y relaciones (DER)** Estructura de tablas en PostgreSQL: `usuarios`, `tickets`, `comentarios`, `categorías`, con sus claves foráneas y tipos de datos correspondientes. Se suma una tabla `ticket_history` para registrar cada cambio relevante del ticket (estado, agente asignado, categoría o prioridad): quién lo cambió, cuándo, y de qué valor a cuál pasó (esto sostiene el requisito de trazabilidad completa mencionado en el problema). El detalle completo del modelo de datos, con todos los campos de cada tabla, está documentado en [`/docs/DER-ticketdesk.md`](./docs/DER-ticketdesk.md).

**2. Arquitectura de seguridad y roles (JWT)** La API se protege con Spring Security usando autenticación stateless mediante JWT (JSON Web Tokens): el frontend en React envía las credenciales de login, recibe un token y lo adjunta en el header de cada request posterior. Se definen los siguientes roles:
- `ROLE_USER` - accede solo a sus propios tickets
- `ROLE_AGENT` - accede a todos los tickets y puede modificarlos
- `ROLE_SUPERVISOR` - recibe los tickets escalados

**3. Especificación de endpoints y contratos de la API REST** Se define de antemano el diseño de las rutas, los verbos HTTP y la forma exacta del JSON (DTOs) que viaja entre frontend y backend. Esto permite que el equipo de frontend pueda maquetar y mockear datos sin depender de que el backend tenga la lógica terminada, y que el backend sepa con precisión qué datos recibir y devolver en cada endpoint.

## Justificación Técnica de Arquitectura e Invariantes

Para cumplir con las exigencias de un entorno productivo empresarial, el diseño del modelo de datos e infraestructura responde a las siguientes decisiones técnicas:

### 1. Manejo Flexibilizado con JSON / JSONB

Para la gestión de parámetros por cliente (ej. `configuracion_empresa.modulos_habilitados`), se adoptó la flexibilidad de estructuras JSON / JSONB:

- **Evolución sin Migraciones Disruptivas:** permite habilitar o deshabilitar módulos operativos por empresa dinámicamente sin alterar el esquema relacional (`ALTER TABLE`) ni generar tiempos de parada (*downtime*).
- **Eficiencia Operativa:** proporciona indexación eficiente en PostgreSQL, manteniendo la velocidad de consulta propia de columnas relacionales tradicionales.

### 2. Canales de Entrada por Voz y Auditoría de IA

El canal de entrada por voz (`canal_origen = 'voz'`) combina usabilidad con auditoría técnica:

- **Inmutabilidad y Auditoría:** se almacena tanto el archivo de audio original (`audio_url`) como la `transcripcion_original`, antes de cualquier procesamiento de palabras clave o asignación automática.
- **Control de Calidad:** permite verificar la fidelidad de la categorización realizada por el sistema de reconocimiento ante eventuales discrepancias.

### 3. Reglas de Negocio Estrictas y Preservación de Historial

- **Persistencia Cero-Borrado (Soft Delete):** los usuarios y agentes nunca se eliminan físicamente de la base de datos (`activo = BOOLEAN`). Esto resguarda la integridad de la trazabilidad histórica de los reclamos.
- **Reasignación Obligatoria:** un agente con tickets abiertos no puede pasar a estado inactivo sin reasignar sus casos pendientes a un agente activo responsable.
- **Trazabilidad de Adjuntos:** la subida de adjuntos/imágenes está restringida exclusivamente al usuario final (`ROLE_USER`) para evitar ambigüedades interpretativas en las respuestas de los agentes, manteniendo la resolución técnica estrictamente registrada como texto auditable.

## Estructura del Repositorio

```
/backend    → API REST en Spring Boot (controllers, services, repositories, entidades JPA)
/frontend   → Aplicación React + TypeScript (vistas, componentes, consumo de API)
/docs       → Informes, esquemas de base de datos y documentación de avances
```

Todo el desarrollo se centraliza en este único repositorio, según lo requerido por la cátedra.

## Estado del Proyecto

🟡 En desarrollo: Entrega 1: Propuesta y Repositorio

## Roadmap

- [x] Conformación del equipo y elección de tutor/a
- [ ] Definición de arquitectura y modelo de datos (entidades y relaciones)
  - [ ] Diseño del modelo de datos y relaciones (DER)
  - [ ] Definición de arquitectura de seguridad y roles (JWT)
  - [ ] Especificación de endpoints y contratos de la API REST
- [ ] Desarrollo del backend (API REST)
- [ ] Desarrollo del frontend
- [ ] Despliegue en la nube
- [ ] Informe final y video explicativo
- [ ] Defensa oral

## Roadmap de Evolución del Producto y Justificación

El desarrollo de TicketDesk se estructura en fases incrementales para asegurar un producto funcional desde las primeras etapas, priorizando la estabilidad del núcleo relacional antes de incorporar automatizaciones avanzadas.

- **Fase 1: Núcleo Relacional y Multi-tenant (MVP Base)**
  - **Enfoque:** configuración de la estructura de datos principal (`empresas`, `usuarios`, `tickets`, `comentarios`).
  - **Justificación:** es imprescindible consolidar el aislamiento de datos y el flujo relacional básico antes de permitir integraciones externas o canales alternativos.

- **Fase 2: Flexibilización y Canales de Entrada (Fase Actual)**
  - **Enfoque:** implementación de `configuracion_empresa` mediante JSON/JSONB y módulo de creación de tickets por voz.
  - **Justificación:** se introduce flexibilidad en la configuración de módulos por cliente sin alterar el esquema base, y se suma el canal de voz garantizando la auditabilidad de la transcripción original.

- **Fase 3: Métricas Avanzadas, SLAs y Automatización (Próximos Pasos)**
  - **Enfoque:** gestión de tiempos de respuesta (SLAs), reportes comparativos entre empresas e integración de modelos de IA para categorización automática.
  - **Justificación:** una vez garantizada la integridad operativa y la entrada omnicanal, el sistema evoluciona hacia la optimización mediante análisis de datos y automatización avanzada.
