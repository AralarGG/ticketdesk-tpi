# TicketDesk: Flujos de Usuario

## Documento base: Trabajo Final Integrador UTN TUPaD

Este documento describe paso a paso las principales interacciones de cada tipo de usuario con el sistema, desde que ingresa hasta que completa su objetivo. Sirve como base para el diseño de las pantallas (wireframes) y para validar que el modelo de datos y los endpoints cubren cada paso.

---

## 1. Flujo: Cliente crea un ticket por formulario

**Actor:** Usuario/Cliente (`ROLE_USER`)

1. El cliente inicia sesión con su email y contraseña (`POST /auth/login`)
2. Accede a su panel de usuario y hace clic en "Nuevo ticket"
3. Completa el formulario: título, descripción, categoría (seleccionada de un listado fijo) y prioridad
4. Opcionalmente, adjunta una o más fotos como evidencia del problema
5. Envía el formulario (`POST /tickets`, seguido de `POST /tickets/{id}/adjuntos` si hay fotos)
6. El sistema crea el ticket en estado `nuevo` y lo confirma en pantalla con un número de referencia
7. El cliente puede ver su ticket recién creado en su panel, listado junto a los demás

**Puntos de control:** si falta un campo obligatorio, el sistema no permite avanzar y marca el campo faltante. Si la foto supera el tamaño permitido, se avisa antes de intentar subirla.

---

## 2. Flujo: Cliente crea un ticket por voz

**Actor:** Usuario/Cliente (`ROLE_USER`)

1. El cliente inicia sesión
2. En el panel de usuario, en vez de "Nuevo ticket" con formulario, elige la opción "Crear por voz"
3. Graba un mensaje de audio describiendo su problema (ej. "no puedo iniciar sesión en la aplicación")
4. Envía el audio (`POST /tickets/voz`)
5. El sistema procesa el audio: lo transcribe a texto y busca palabras clave para sugerir categoría
6. Se muestra al cliente una pantalla de confirmación con el título/categoría sugeridos y la transcripción completa, para que confirme o corrija antes de crear el ticket definitivamente
7. El cliente confirma, y el ticket se crea en estado `nuevo`, guardando tanto el audio original como la transcripción

**Punto de control clave:** el cliente siempre revisa la transcripción antes de confirmar. Esto evita que un error de reconocimiento de voz genere un ticket mal categorizado sin que nadie lo note.

---

## 3. Flujo: Agente toma y resuelve un ticket

**Actor:** Agente de soporte (`ROLE_AGENT`)

1. El agente inicia sesión y accede a su panel, donde ve el listado de tickets de su empresa
2. Filtra por estado `nuevo` y prioridad `alta` para priorizar los más urgentes (`GET /tickets?estado=nuevo&prioridad=alta`)
3. Abre un ticket para ver el detalle completo (`GET /tickets/{id}`)
4. Se autoasigna el ticket (`PATCH /tickets/{id}/asignar`), lo que cambia el estado a `asignado`
5. Comienza a trabajar en la resolución: cambia el estado a `en_progreso` (`PATCH /tickets/{id}/estado`)
6. Si necesita más información del cliente, agrega un comentario (`POST /tickets/{id}/comentarios`) y cambia el estado a `esperando_cliente`
7. Cuando el cliente responde (nuevo comentario del lado del cliente), el agente retoma el ticket y vuelve a `en_progreso`
8. Una vez resuelto, agrega un comentario final explicando la solución y cambia el estado a `resuelto`

**Restricción aplicada en este flujo:** en el paso 6 y 8, el agente solo puede responder con texto, nunca adjuntando fotos (regla de negocio ya definida en el DER).

---

## 4. Flujo: Ticket se escala a un supervisor

**Actor:** Agente de soporte → Supervisor (`ROLE_SUPERVISOR`)

1. El agente, trabajando un ticket complejo, determina que no puede resolverlo con su nivel de acceso o conocimiento
2. Cambia el estado del ticket a `escalado`, indicando un motivo obligatorio (`PATCH /tickets/{id}/estado`)
3. El sistema reasigna automáticamente el `agente_id` a un usuario con rol `ROLE_SUPERVISOR` disponible (`PATCH /tickets/{id}/asignar`, con motivo "escalado")
4. El supervisor ve el ticket en su propio panel, con acceso al historial completo (`ticket_history`) para entender qué se intentó antes de escalar
5. El supervisor continúa el flujo normal (comentarios, cambio de estado) hasta resolverlo

---

## 5. Flujo: Cliente reabre un ticket resuelto

**Actor:** Usuario/Cliente (`ROLE_USER`)

1. El cliente recibe la notificación (o entra a revisar) de que su ticket pasó a estado `resuelto`
2. Verifica si el problema realmente se solucionó
3. Si el problema persiste, desde el detalle del ticket elige la opción "El problema continúa"
4. El sistema cambia el estado a `reabierto` (`PATCH /tickets/{id}/estado`, iniciado por el cliente pero validado por backend)
5. El ticket vuelve a aparecer en el panel del agente que lo había resuelto, con todo el historial de comentarios previo intacto
6. El agente retoma el caso desde donde había quedado

---

## 6. Flujo: Administrador configura módulos para una nueva empresa cliente

**Actor:** Administrador (`ROLE_ADMIN`)

1. Se da de alta una nueva empresa cliente en el sistema (`empresas`)
2. El administrador accede a la configuración de esa empresa (`GET /empresas/{id}/configuracion`)
3. Decide qué módulos del menú base van a estar visibles para esa empresa (ej. oculta "facturación empresarial" para una farmacia pequeña)
4. Define la ventana de mantenimiento horaria para esa empresa
5. Guarda los cambios (`PATCH /empresas/{id}/configuracion`)
6. A partir de ese momento, los usuarios y agentes de esa empresa ven únicamente los módulos habilitados

---

## 7. Flujo: Agente se da de baja del sistema

**Actor:** Administrador (`ROLE_ADMIN`)

1. El administrador identifica que un agente debe darse de baja (ej. dejó la empresa)
2. Intenta desactivarlo (`PATCH /usuarios/{id}/desactivar`)
3. Si el agente tiene tickets activos asignados, el sistema rechaza la operación y devuelve el listado de tickets pendientes (respuesta 409)
4. El administrador reasigna manualmente esos tickets a otro agente activo (`PATCH /tickets/{id}/asignar` para cada uno)
5. Una vez reasignados todos, reintenta la desactivación, que ahora se completa exitosamente
6. El agente queda con `activo = false`, pero su historial de tickets resueltos anteriormente permanece intacto y visible en `ticket_history`

---

## Resumen: Cobertura de Flujos vs. Modelo de Datos

| Flujo | Entidades involucradas | Endpoints involucrados |
|---|---|---|
| Alta por formulario | tickets, adjuntos | POST /tickets, POST /tickets/{id}/adjuntos |
| Alta por voz | tickets (audio_url, transcripcion_original) | POST /tickets/voz |
| Resolución por agente | tickets, comentarios, ticket_history | GET /tickets, PATCH /tickets/{id}/estado, POST /tickets/{id}/comentarios |
| Escalado | tickets, usuarios (rol supervisor), ticket_history | PATCH /tickets/{id}/estado, PATCH /tickets/{id}/asignar |
| Reapertura | tickets, ticket_history | PATCH /tickets/{id}/estado |
| Configuración por empresa | empresas, configuracion_empresa | GET/PATCH /empresas/{id}/configuracion |
| Baja de agente | usuarios, tickets | PATCH /usuarios/{id}/desactivar, PATCH /tickets/{id}/asignar |

Todos los flujos definidos encuentran soporte completo en el modelo de datos (DER) y en la especificación de endpoints ya documentados, sin requerir campos o rutas adicionales no contemplados.
