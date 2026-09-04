# TicketDesk — Sistema de Gestión de Tickets de Soporte

## Trabajo Final Integrador — UTN TUPaD

**Integrantes:**
- Santiago Raúl Salinas
- Ezequiel Sanabria
- Joaquín Del Valle Lietti

**Tutor/a:** Gerardo Adrian Herrera

---

## Problema

Las empresas que brindan soporte a clientes —equipos de atención al usuario en productos de software, videojuegos o plataformas online— suelen manejar los reclamos y consultas de forma desordenada: por planillas sueltas, chats internos o correos electrónicos sin trazabilidad. Esto genera varios problemas concretos:

- **Solicitudes que se pierden** porque no quedan registradas en un único lugar
- **Duplicación de esfuerzos** cuando dos agentes distintos atienden el mismo reclamo sin saberlo
- **Falta de visibilidad** sobre qué tickets están pendientes, quién los está atendiendo y hace cuánto tiempo
- **Imposibilidad de priorizar** correctamente entre consultas urgentes y consultas de baja relevancia
- **Ausencia de historial**, lo que dificulta entender el contexto de un reclamo si cambia el agente asignado

TicketDesk busca resolver esto centralizando todo el ciclo de vida de un ticket de soporte —desde que el usuario lo crea hasta que se resuelve— en un único sistema accesible tanto para usuarios como para agentes, con estados claros y trazabilidad completa.

## Objetivos del Proyecto

- Centralizar la carga y el seguimiento de tickets de soporte en una sola plataforma
- Permitir que los agentes prioricen y distribuyan la carga de trabajo de forma visible
- Reducir el tiempo de respuesta al usuario mediante un flujo de estados claro
- Dejar registro histórico de cada ticket para facilitar auditoría y traspaso entre agentes

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

## Stack Tecnológico

- **Backend:** Java 21 + Spring Boot + JPA — se eligió por ser el stack con el que el equipo ya tiene experiencia previa (TPIs de Programación 3 y Programación IV), lo que reduce la curva de aprendizaje y permite enfocar el tiempo en el diseño del producto
- **Frontend:** React + TypeScript — mismo criterio: stack ya trabajado en cursadas anteriores
- **Base de datos:** PostgreSQL — modelo relacional adecuado dado que las entidades (tickets, usuarios, categorías, comentarios) tienen relaciones bien definidas y consultas que se benefician de integridad referencial
- **Despliegue:** Backend en Render/Railway, Frontend en Vercel/Netlify — servicios gratuitos con despliegue continuo desde GitHub, cumpliendo el requisito de tener al menos un componente corriendo en la nube

## Estructura del Repositorio

```
/backend    → API REST en Spring Boot (controllers, services, repositories, entidades JPA)
/frontend   → Aplicación React + TypeScript (vistas, componentes, consumo de API)
/docs       → Informes, esquemas de base de datos y documentación de avances
```

Todo el desarrollo se centraliza en este único repositorio, según lo requerido por la cátedra.

## Estado del Proyecto

🟡 En desarrollo — Entrega 1: Propuesta y Repositorio

## Roadmap

- [x] Conformación del equipo y elección de tutor/a
- [ ] Definición de arquitectura y modelo de datos (entidades y relaciones)
- [ ] Desarrollo del backend (API REST)
- [ ] Desarrollo del frontend
- [ ] Despliegue en la nube
- [ ] Informe final y video explicativo
- [ ] Defensa oral
