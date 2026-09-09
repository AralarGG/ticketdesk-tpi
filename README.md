# TicketDesk - Sistema de Gestión de Tickets de Soporte

## Trabajo Final Integrador - UTN TUPaD

**Integrantes:**
- Santiago Raúl Salinas
- Ezequiel Sanabria
- Joaquín Del Valle Lietti

**Tutor/a:** Gerardo Adrian Herrera

---

## Problema

Las empresas que brindan soporte a clientes, equipos de atención al usuario en productos de software, videojuegos o plataformas online, suelen manejar los reclamos y consultas de forma desordenada: por planillas sueltas, chats internos o correos electrónicos sin trazabilidad. Esto genera varios problemas concretos:

- **Solicitudes que se pierden** porque no quedan registradas en un único lugar
- **Duplicación de esfuerzos** cuando dos agentes distintos atienden el mismo reclamo sin saberlo
- **Falta de visibilidad** sobre qué tickets están pendientes, quién los está atendiendo y hace cuánto tiempo
- **Imposibilidad de priorizar** correctamente entre consultas urgentes y consultas de baja relevancia
- **Ausencia de historial**, lo que dificulta entender el contexto de un reclamo si cambia el agente asignado

TicketDesk busca resolver esto centralizando todo el ciclo de vida de un ticket de soporte, desde que el usuario lo crea hasta que se resuelve, en un único sistema accesible tanto para usuarios como para agentes, con estados claros y trazabilidad completa.

## Objetivos del Proyecto

- Centralizar la carga y el seguimiento de tickets de soporte en una sola plataforma
- Permitir que los agentes prioricen y distribuyan la carga de trabajo de forma visible
- Reducir el tiempo de respuesta al usuario mediante un flujo de estados claro
- Dejar registro histórico de cada ticket para facilitar auditoría y traspaso entre agentes

## Propuesta de Valor y Enfoque Comercial

Para las organizaciones modernas, la calidad del servicio de soporte define la retención del cliente. TicketDesk transforma la gestión de incidencias operativas mediante:

* **Arquitectura Multi-tenant Adaptable:** Un único ecosistema multi-empresa donde cada cliente opera con sus parámetros y módulos independientes sin comprometer la seguridad ni el aislamiento de datos.
* **Omnicanalidad Inteligente (Creación de Tickets por Voz):** Integración de módulos de entrada por voz para eliminar barreras de fricción en los usuarios finales, permitiendo la apertura de tickets de manera ágil e intuitiva.
* **Trazabilidad e Audibilidad Total:** Garantía de control operativo donde no existe pérdida de información ni modificaciones inconsistentes, protegiendo el historial del cliente y los registros de servicio.

## Alcance del MVP

**Roles de usuario:**
- **Usuario/Cliente:** crea tickets, visualiza el estado de sus propios tickets y puede agregar comentarios o información adicional
- **Agente de soporte:** visualiza todos los tickets, puede tomarlos, reasignarlos, cambiar su estado y responder al usuario
- **Administrador (opcional según tiempo disponible):** gestiona categorías, prioridades y usuarios/agentes del sistema

**Funcionalidades principales:**

| Funcionalidad | Descripción |
|---|---|
| Alta de ticket | El usuario completa título, descripción, categoría (ej. bug, consulta, facturación) y prioridad (baja/media/alta) |
| Asignación | Un agente toma el ticket manualmente, o se asigna automáticamente por categoría |
| Seguimiento de estado | Flujo: `abierto` → `en progreso` → `resuelto` → `cerrado`, con historial de cambios y fecha de cada transición |
| Comentarios | Usuario y agente pueden agregar comentarios dentro del ticket para dar contexto o pedir información |
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

## 🎨 Wireframes y Prototipos de Interfaz (Mockups)

El diseño de la interfaz prioritiza la claridad operativa, reduciendo la carga cognitiva tanto para el cliente como para el agente de soporte.

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

## Stack Tecnológico

- **Backend:** Java 21 + Spring Boot + JPA - se eligió por ser el stack con el que el equipo ya tiene experiencia previa (TPIs de Programación 3 y Programación IV), lo que reduce la curva de aprendizaje y permite enfocar el tiempo en el diseño del producto
- **Frontend:** React + TypeScript - mismo criterio: stack ya trabajado en cursadas anteriores
- **Base de datos:** PostgreSQL - modelo relacional adecuado dado que las entidades (tickets, usuarios, categorías, comentarios) tienen relaciones bien definidas y consultas que se benefician de integridad referencial
- **Despliegue:** Backend en Render/Railway, Frontend en Vercel/Netlify - servicios gratuitos con despliegue continuo desde GitHub, cumpliendo el requisito de tener al menos un componente corriendo en la nube

## Diseño Técnico Previo al Desarrollo

Antes de escribir código, el equipo definió tres aspectos clave de diseño:

**1. Modelo de datos y relaciones (DER)**
Estructura de tablas en PostgreSQL: `usuarios`, `tickets`, `comentarios`, `categorías`, con sus claves foráneas y tipos de datos correspondientes. Se suma una tabla `ticket_history` para registrar cada cambio de estado de un ticket: quién lo cambió, cuándo, y de qué estado a cuál pasó (esto sostiene el requisito de trazabilidad completa mencionado en el problema).

**2. Arquitectura de seguridad y roles (JWT)**
La API se protege con Spring Security usando autenticación stateless mediante JWT (JSON Web Tokens): el frontend en React envía las credenciales de login, recibe un token y lo adjunta en el header de cada request posterior. Se definen dos roles de entrada:
- `ROLE_USER` - accede solo a sus propios tickets
- `ROLE_AGENT` - accede a todos los tickets y puede modificarlos

**3. Especificación de endpoints y contratos de la API REST**
Se define de antemano el diseño de las rutas, los verbos HTTP y la forma exacta del JSON (DTOs) que viaja entre frontend y backend. Esto permite que el equipo de frontend pueda maquetar y mockear datos sin depender de que el backend tenga la lógica terminada, y que el backend sepa con precisión qué datos recibir y devolver en cada endpoint.

## Estructura del Repositorio

```
/backend    → API REST en Spring Boot (controllers, services, repositories, entidades JPA)
/frontend   → Aplicación React + TypeScript (vistas, componentes, consumo de API)
/docs       → Informes, esquemas de base de datos y documentación de avances
```

Todo el desarrollo se centraliza en este único repositorio, según lo requerido por la cátedra.

## Justificación Técnica de Arquitectura e Invariantes

Para cumplir con las exigencias de un entorno productivo empresarial, el diseño del modelo de datos e infraestructura responde a las siguientes decisiones técnicas:

### 1. Manejo Flexibilizado con JSON / JSONB
Para la gestión de parámetros por cliente (ej. `configuracion_empresa.modulos_habilitados`), se adoptó la flexibilidad de estructuras **JSON / JSONB**:
* **Evolución sin Migraciones Disruptivas:** Permite habilitar o deshabilitar módulos operativos por empresa dinámicamente sin alterar el esquema relacional (`ALTER TABLE`) ni generar tiempos de parada (*downtime*).
* **Eficiencia Operativa:** Proporciona indexación eficiente en PostgreSQL manteniendo la velocidad de consulta propia de columnas relacionales tradicionales.

### 2. Canales de Entrada por Voz y Auditoría de IA
El canal de entrada por voz (`canal_origen = 'voz'`) combina usabilidad con auditoría técnica:
* **Inmutabilidad y Auditoría:** Se almacena tanto el archivo de audio original (`audio_url`) como la `transcripcion_original` antes de cualquier procesamiento de palabras clave o asignación automática.
* **Control de Calidad:** Permite verificar la fidelidad de la categorización realizada por el sistema de reconocimiento ante eventuales discrepancias.

### 3. Reglas de Negocio Estrictas y Preservación de Historial
* **Persistencia Cero-Borrado (Soft Delete):** Los usuarios y agentes nunca se eliminan físicamente de la base de datos (`activo = BOOLEAN`). Esto resguarda la integridad de la trazabilidad histórica de los reclamos.
* **Reasignación Obligatoria:** Un agente con tickets abiertos no puede pasar a estado inactivo sin reasignar sus casos pendientes a un agente activo responsable.
* **Trazabilidad de Adjuntos:** La subida de adjuntos/imágenes está restringida exclusivamente al usuario final (`ROLE_USER`) para evitar ambigüedades interpretativas en las respuestas de los agentes, manteniendo la resolución técnica estrictamente registrada como texto auditable.

## Estado del Proyecto

🟡 En desarrollo - Entrega 1: Propuesta y Repositorio

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

El desarrollo de TicketDesk se estructuró en fases incrementales para asegurar un producto funcional desde las primeras etapas, priorizando la estabilidad del núcleo relacional antes de incorporar automatizaciones avanzadas.

* **Fase 1: Núcleo Relacional y Multi-tenant (MVP Base)**  
  * **Enfoque:** Configuración de la estructura de datos principal (`empresas`, `usuarios`, `tickets`, `comentarios`).
  * **Justificación:** Es imprescindible consolidar el aislamiento de datos y el flujo relacional básico antes de permitir integraciones externas o canales alternativos.

* **Fase 2: Flexibilización y Canales de Entrada (Fase Actual)**  
  * **Enfoque:** Implementación de `configuracion_empresa` mediante JSON/JSONB y módulo de creación de tickets por voz.
  * **Justificación:** Se introduce flexibilidad en la configuración de módulos por cliente sin alterar el esquema base, y se suma el canal de voz garantizando la audibilidad de la transcripción original.

* **Fase 3: Métricas Avanzadas, SLAs y Automatización (Próximos Pasos)**  
  * **Enfoque:** Gestión de tiempos de respuesta (SLAs), reportes comparativos entre empresas e integración de modelos de IA para categorización automática.
  * **Justificación:** Una vez garantizada la integridad operativa y la entrada omnicanal, el sistema evoluciona hacia la optimización mediante análisis de datos y automatización avanzada.
