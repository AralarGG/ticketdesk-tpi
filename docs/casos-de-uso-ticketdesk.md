# TicketDesk: Casos de Uso

## Documento base: Trabajo Final Integrador UTN TUPaD

Este documento formaliza los casos de uso del sistema. Cada caso indica quién lo realiza, qué necesita para empezar, cómo transcurre, qué errores pueden ocurrir y qué queda como resultado. Complementa los flujos de usuario (`flujos-usuario-ticketdesk.md`), que describen las interacciones desde el punto de vista de cada persona. Los nombres de estados, roles y campos siguen el `diccionario-nomenclatura-ticketdesk.md`.

---

## 1. Actores

| Actor | Descripción | Rol técnico |
|---|---|---|
| Visitante | Persona que todavía no inició sesión | Sin rol |
| Cliente | Persona que crea tickets y consulta los suyos | `ROLE_USER` |
| Agente | Persona del equipo de soporte que toma y resuelve tickets | `ROLE_AGENT` |
| Supervisor | Recibe los tickets escalados y puede asignar tickets a otras personas. Hace todo lo que hace un agente | `ROLE_SUPERVISOR` |
| Administrador | Configura la empresa y gestiona categorías propias y usuarios | `ROLE_ADMIN` |
| Sistema | Reglas automáticas del servidor (tareas programadas y control de la ventana de mantenimiento) | - |

Una misma persona puede tener más de un rol (por ejemplo, agente y supervisor). Se llama **personal de soporte** al conjunto de agentes, supervisores y administradores.

## 2. Lista de casos de uso

| Código | Caso de uso | Actor principal | Requerimiento | Reglas de negocio |
|---|---|---|---|---|
| UC-01 | Iniciar sesión | Visitante | RF1 | RN1, RN2 |
| UC-02 | Registrarse como cliente | Visitante | RF1 | RN1 |
| UC-03 | Crear un ticket por formulario | Cliente | RF2, RF4 | RN8, RN10 |
| UC-04 | Crear un ticket por voz | Cliente | RF2 | RN14 |
| UC-05 | Consultar mis tickets | Cliente | RF2, RF6 | RN15 |
| UC-06 | Comentar un ticket | Cliente, agente, supervisor | RF2 | - |
| UC-07 | Adjuntar una foto | Cliente | RF2 | RN13 |
| UC-08 | Confirmar el cierre o reabrir un ticket | Cliente | RF3 | RN5, RN6 |
| UC-09 | Listar y filtrar los tickets de la empresa | Personal de soporte | RF3 | RN15 |
| UC-10 | Tomar un ticket | Agente, supervisor | RF3 | RN11 |
| UC-11 | Asignar un ticket a otra persona | Supervisor | RF3 | RN9, RN11 |
| UC-12 | Trabajar un ticket | Agente, supervisor | RF3 | RN5, RN11 |
| UC-13 | Escalar un ticket a un supervisor | Agente, supervisor | RF3 | RN9, RN11 |
| UC-14 | Cambiar el nivel de atención | Agente, supervisor | RF4 | RN8, RN11 |
| UC-15 | Consultar el historial de un ticket | Personal de soporte | RF5 | RN11, RN12 |
| UC-16 | Configurar módulos y ventana de mantenimiento | Administrador | RF6 | RN16, RN17 |
| UC-17 | Gestionar categorías propias | Administrador | RF4 | RN10 |
| UC-18 | Gestionar usuarios y roles | Administrador | RF1, RF5 | RN1, RN2, RN3, RN4 |
| UC-19 | Cerrar automáticamente un ticket resuelto | Sistema | RF3 | RN6, RN12 |
| UC-20 | Devolver a Nivel 1 un ticket estancado en Crítico | Sistema | RF3 | RN7, RN12 |
| UC-21 | Aplicar la ventana de mantenimiento | Sistema | RF6 | RN17 |

Todos los casos que actúan sobre un ticket, salvo los del sistema, tienen como precondición común que la persona haya iniciado sesión y que el ticket pertenezca a su empresa. Si el ticket es de otra empresa, o es de otro cliente, el sistema responde que no existe (404), para no revelar su existencia.

---

## 3. Especificación

### UC-01: Iniciar sesión

- **Actor:** Visitante.
- **Objetivo:** ingresar al sistema con una cuenta de una empresa.
- **Precondiciones:** la persona tiene una cuenta activa en una empresa.
- **Flujo principal:**
  1. La persona elige su empresa de la lista pública de empresas.
  2. Ingresa su email y su contraseña.
  3. El sistema valida las credenciales dentro de esa empresa.
  4. El sistema devuelve un token de sesión con vigencia de 24 horas y el rol de la persona.
  5. La aplicación muestra el panel y el menú que corresponden al rol y a los módulos habilitados de la empresa.
- **Flujos alternativos y errores:**
  - A1. Email, contraseña o empresa incorrectos: el sistema responde 401 "Email, contraseña o empresa incorrectos".
  - A2. La cuenta está desactivada: el sistema responde 401 "El usuario está desactivado".
  - A3. Si el token vence, la aplicación vuelve a la pantalla de ingreso.
- **Postcondiciones:** la persona queda autenticada. No se guarda ninguna sesión en el servidor.
- **Referencias:** `GET /auth/empresas`, `POST /auth/login`.

### UC-02: Registrarse como cliente

- **Actor:** Visitante.
- **Objetivo:** crear una cuenta de cliente en una empresa.
- **Precondiciones:** la empresa existe.
- **Flujo principal:**
  1. La persona elige "Registrate", elige la empresa e ingresa nombre, email y contraseña.
  2. El sistema verifica que el email no esté usado en esa empresa.
  3. El sistema crea una cuenta activa con rol de cliente.
  4. El sistema inicia la sesión automáticamente.
- **Flujos alternativos y errores:**
  - A1. El email ya existe en esa empresa: 409 "Ya existe un usuario con ese email en la empresa". El mismo email sí puede existir en otra empresa.
  - A2. Falta un dato o el email es inválido: 400.
  - A3. La pantalla exige una contraseña de al menos 8 caracteres.
- **Postcondiciones:** existe un nuevo usuario con rol `ROLE_USER`.
- **Referencias:** `POST /auth/register`.

### UC-03: Crear un ticket por formulario

- **Actor:** Cliente.
- **Objetivo:** registrar un reclamo o consulta.
- **Precondiciones:** sesión iniciada como cliente.
- **Flujo principal:**
  1. El cliente elige "Nuevo ticket".
  2. Completa título, categoría, prioridad y descripción.
  3. Confirma con "Crear ticket".
  4. El sistema crea el ticket en estado `NUEVO`, con nivel de atención `NIVEL_1`, canal `FORMULARIO` y sin agente.
  5. La aplicación muestra el detalle del ticket creado.
- **Flujos alternativos y errores:**
  - A1. Falta el título, la descripción, la categoría o la prioridad: 400, y la pantalla marca el campo.
  - A2. La categoría no es genérica ni de su empresa: 404 "Categoría no encontrada".
  - A3. La empresa está dentro de su ventana de mantenimiento: 503.
- **Postcondiciones:** existe un ticket nuevo visible en "Mis tickets". Las fotos se agregan después (UC-07).
- **Referencias:** `GET /categorias`, `POST /tickets`.

### UC-04: Crear un ticket por voz

- **Actor:** Cliente.
- **Objetivo:** crear un ticket hablando, con confirmación antes de crearlo.
- **Precondiciones:** sesión de cliente, módulo de voz habilitado en la empresa y permiso de micrófono en el navegador.
- **Flujo principal:**
  1. El cliente elige "Nuevo ticket por voz" y "Empezar a grabar".
  2. El navegador graba el audio y transcribe lo que se dice mientras se habla.
  3. El cliente toca "Terminar".
  4. La aplicación envía el texto al sistema, que busca palabras clave y devuelve una categoría y un título sugeridos.
  5. El cliente revisa el audio, corrige el texto si hace falta, confirma o cambia la categoría y elige la prioridad.
  6. Confirma con "Crear ticket". La aplicación envía el audio, el texto, la categoría y la prioridad.
  7. El sistema sube el audio, crea el ticket con canal `VOZ` y guarda el audio original y la transcripción.
- **Flujos alternativos y errores:**
  - A1. No se otorga el permiso de micrófono: la aplicación lo informa y no permite grabar.
  - A2. El navegador no transcribe: el cliente escribe el texto a mano.
  - A3. Ninguna palabra clave coincide: la categoría queda sin elegir y el cliente debe elegirla.
  - A4. El módulo de voz está deshabilitado: la opción no aparece en el menú y la API responde 403.
  - A5. La transcripción está vacía o el archivo no es un audio: 400.
- **Postcondiciones:** existe un ticket con el audio original y la transcripción para su auditoría.
- **Referencias:** `POST /tickets/voz/sugerencia`, `POST /tickets/voz`.

### UC-05: Consultar mis tickets

- **Actor:** Cliente.
- **Objetivo:** ver el estado y la conversación de sus tickets.
- **Precondiciones:** sesión de cliente.
- **Flujo principal:**
  1. El cliente entra a "Mis tickets" y ve únicamente sus tickets, del más nuevo al más antiguo.
  2. Abre uno.
  3. Ve el recorrido de estados, la descripción, el audio (si es por voz), los comentarios, las fotos y los datos del ticket.
- **Flujos alternativos y errores:**
  - A1. Intenta abrir un ticket de otro cliente: 404.
- **Postcondiciones:** ninguna.
- **Referencias:** `GET /tickets`, `GET /tickets/{id}`.
- **Nota:** el cliente no ve el nivel de atención, el tiempo objetivo ni el historial.

### UC-06: Comentar un ticket

- **Actor:** Cliente dueño del ticket, agente o supervisor.
- **Objetivo:** agregar información a la conversación del ticket.
- **Precondiciones:** el ticket es accesible para la persona y no está cerrado; el módulo de comentarios está habilitado.
- **Flujo principal:**
  1. La persona escribe el comentario y lo envía.
  2. El sistema lo guarda con el autor, su rol y la fecha.
  3. El comentario aparece en la conversación.
- **Flujos alternativos y errores:**
  - A1. El ticket está cerrado: 409 "El ticket está cerrado y no admite nuevos comentarios".
  - A2. El comentario está vacío: 400.
  - A3. Si el ticket estaba en `ESPERANDO_CLIENTE` y comenta el cliente dueño, el ticket vuelve solo a `EN_PROGRESO` y el cambio queda registrado con el motivo "El cliente respondió".
- **Postcondiciones:** el comentario forma parte del ticket.
- **Referencias:** `POST /tickets/{id}/comentarios`, `GET /tickets/{id}/comentarios`.

### UC-07: Adjuntar una foto

- **Actor:** Cliente.
- **Objetivo:** aportar evidencia visual del problema.
- **Precondiciones:** el ticket es suyo y no está cerrado; el módulo de fotos adjuntas está habilitado.
- **Flujo principal:**
  1. El cliente elige "Subir una foto o captura" en el detalle del ticket y selecciona una imagen.
  2. El sistema sube la imagen al servicio de archivos y guarda su dirección.
  3. La foto aparece como miniatura en la sección de fotos.
- **Flujos alternativos y errores:**
  - A1. La persona no es cliente: 403. Los agentes responden solo con texto.
  - A2. El archivo no es una imagen o está vacío: 400.
  - A3. El archivo supera los 10 MB: 413.
  - A4. El ticket está cerrado: 409. El módulo está deshabilitado: 403.
- **Postcondiciones:** el ticket tiene un adjunto más.
- **Referencias:** `POST /tickets/{id}/adjuntos`, `GET /tickets/{id}/adjuntos`.

### UC-08: Confirmar el cierre o reabrir un ticket

- **Actor:** Cliente dueño del ticket.
- **Objetivo:** indicar si el problema quedó resuelto.
- **Precondiciones:** el ticket está en `RESUELTO`.
- **Flujo principal (cierre):**
  1. El cliente ve la pregunta "¿Se solucionó tu problema?" y elige "Sí, cerrar el ticket".
  2. El sistema pasa el ticket a `CERRADO` y lo registra en el historial.
  3. Ya no se aceptan comentarios ni fotos.
- **Flujo alternativo (reapertura):**
  1. El cliente elige "El problema continúa".
  2. El sistema pasa el ticket a `REABIERTO` y borra la fecha de resolución.
  3. El ticket vuelve al panel del personal de soporte, con su historial intacto.
- **Flujos alternativos y errores:**
  - A1. El ticket no está en `RESUELTO`: 409.
  - A2. Otro cliente intenta confirmar o reabrir un ticket que no es suyo: 404.
- **Postcondiciones:** el ticket queda `CERRADO` o `REABIERTO`.
- **Referencias:** `PATCH /tickets/{id}/estado`.

### UC-09: Listar y filtrar los tickets de la empresa

- **Actor:** Personal de soporte.
- **Objetivo:** ver y priorizar el trabajo pendiente.
- **Precondiciones:** sesión de personal de soporte.
- **Flujo principal:**
  1. La persona entra a "Tickets" y ve todos los tickets de su empresa, del más nuevo al más antiguo.
  2. Aplica filtros por estado, prioridad, nivel de atención, categoría o "Solo los míos".
  3. El sistema resuelve los filtros en la base de datos y actualiza el listado.
  4. Abre el ticket que le interesa.
- **Flujos alternativos y errores:**
  - A1. Ningún ticket coincide: la pantalla lo informa y ofrece quitar los filtros.
- **Postcondiciones:** ninguna.
- **Referencias:** `GET /tickets` con los parámetros `estado`, `prioridad`, `nivel`, `categoriaId` y `agenteId`.

### UC-10: Tomar un ticket

- **Actor:** Agente o supervisor.
- **Objetivo:** hacerse cargo de un ticket.
- **Precondiciones:** el ticket no está cerrado.
- **Flujo principal:**
  1. La persona abre un ticket y elige "Tomar este ticket".
  2. El sistema lo asigna a quien lo toma.
  3. Si el ticket estaba `NUEVO`, pasa a `ASIGNADO`.
  4. El sistema registra el cambio de agente y el de estado, con la persona y la fecha.
- **Flujos alternativos y errores:**
  - A1. El ticket está cerrado: 409 "El ticket está cerrado y no admite cambios".
  - A2. Los supervisores usan el selector "Asignar a" (UC-11), donde pueden elegirse a sí mismos.
- **Postcondiciones:** el ticket tiene agente.
- **Referencias:** `PATCH /tickets/{id}/asignar`.

### UC-11: Asignar un ticket a otra persona

- **Actor:** Supervisor.
- **Objetivo:** repartir o reasignar el trabajo.
- **Precondiciones:** el ticket no está cerrado.
- **Flujo principal:**
  1. El supervisor abre el ticket, elige una persona en "Asignar a" y toca "Asignar".
  2. El sistema verifica que la persona esté activa, sea de la misma empresa y tenga rol de agente o supervisor.
  3. Asigna el ticket, y si estaba `NUEVO` lo pasa a `ASIGNADO`.
  4. Registra los cambios con el motivo, si lo hubo.
- **Flujos alternativos y errores:**
  - A1. Un agente intenta asignar a otra persona: 403 "Un agente solo puede tomar tickets para sí mismo. Reasignar a otra persona requiere rol de supervisor".
  - A2. La persona elegida no es agente ni supervisor: 400.
  - A3. La persona no existe, está inactiva o es de otra empresa: 404 "Agente no encontrado".
- **Postcondiciones:** el ticket queda a cargo de la persona elegida.
- **Referencias:** `PATCH /tickets/{id}/asignar`, `GET /usuarios/agentes`.

### UC-12: Trabajar un ticket

- **Actor:** Agente o supervisor.
- **Objetivo:** llevar el ticket desde la asignación hasta la resolución.
- **Precondiciones:** el ticket está asignado y no cerrado.
- **Flujo principal:**
  1. La persona elige, en el detalle, una de las acciones válidas para el estado actual.
  2. Opcionalmente escribe un motivo o detalle.
  3. El sistema cambia el estado y lo registra en el historial.
  4. Si el nuevo estado es `RESUELTO`, comienza a contar el plazo de 72 horas para la confirmación del cliente.
- **Transiciones permitidas al personal de soporte:**

| Desde | Hacia | Acción en pantalla |
|---|---|---|
| `NUEVO` | `ASIGNADO` | Tomar este ticket (UC-10) |
| `ASIGNADO` | `EN_PROGRESO` | Empezar a trabajar |
| `EN_PROGRESO` | `ESPERANDO_CLIENTE` | Pedir información al cliente |
| `EN_PROGRESO` | `ESCALADO` | Escalar a un supervisor (UC-13) |
| `EN_PROGRESO` | `RESUELTO` | Marcar como resuelto |
| `ESPERANDO_CLIENTE` | `EN_PROGRESO` | Retomar |
| `ESCALADO` | `EN_PROGRESO` | Retomar |
| `ESCALADO` | `RESUELTO` | Marcar como resuelto |
| `REABIERTO` | `EN_PROGRESO` | Retomar |

- **Flujos alternativos y errores:**
  - A1. Transición no permitida: 409 "No se puede pasar un ticket de X a Y".
  - A2. El personal intenta cerrar un ticket resuelto: 409 "El ticket está resuelto: lo cierra el cliente al confirmar, o el sistema a las 72hs sin respuesta".
  - A3. Un administrador sin rol de agente ni de supervisor no puede cambiar estados: 403.
- **Postcondiciones:** el ticket tiene el nuevo estado y un registro en el historial.
- **Referencias:** `PATCH /tickets/{id}/estado`.

### UC-13: Escalar un ticket a un supervisor

- **Actor:** Agente o supervisor.
- **Objetivo:** derivar un caso que requiere intervención de mayor jerarquía.
- **Precondiciones:** el ticket está en `EN_PROGRESO`, que es el único estado desde el cual se puede escalar.
- **Flujo principal:**
  1. La persona escribe el motivo y elige "Escalar a un supervisor".
  2. El sistema pasa el ticket a `ESCALADO`.
  3. Sube el nivel de atención un escalón, hasta `NIVEL_3` (Crítico no se asigna por esta vía).
  4. Reasigna el ticket al supervisor activo con menos tickets abiertos, distinto del agente actual.
  5. Registra el cambio de estado, el de nivel y el de agente, con el motivo.
- **Flujos alternativos y errores:**
  - A1. Sin motivo: 400 "Para escalar un ticket el motivo es obligatorio". La pantalla muestra el aviso junto a los botones y lleva el cursor al cuadro del motivo.
  - A2. No hay otro supervisor disponible: el ticket queda escalado, con su nivel actualizado, sin cambiar de agente.
- **Postcondiciones:** el ticket está `ESCALADO`, con un nivel más alto (si correspondía) y a cargo de un supervisor.
- **Referencias:** `PATCH /tickets/{id}/estado`, regla RN9.

### UC-14: Cambiar el nivel de atención

- **Actor:** Agente o supervisor.
- **Objetivo:** reclasificar técnicamente un ticket.
- **Precondiciones:** el ticket no está cerrado.
- **Flujo principal:**
  1. La persona elige un nivel en el selector "Nivel de atención" y toca "Guardar".
  2. El sistema actualiza el nivel y, por lo tanto, el tiempo objetivo.
  3. Registra el cambio con la persona y el motivo, si lo hubo.
- **Flujos alternativos y errores:**
  - A1. Un cliente intenta cambiarlo: 403.
  - A2. El ticket está cerrado: 409.
- **Postcondiciones:** el ticket tiene el nuevo nivel.
- **Referencias:** `PATCH /tickets/{id}/nivel`. Las transiciones y las reglas del nivel Crítico están en `reglas-negocio-ticketdesk.md`, sección "Transiciones del nivel de atención".

### UC-15: Consultar el historial de un ticket

- **Actor:** Personal de soporte.
- **Objetivo:** entender qué se hizo antes sobre un ticket.
- **Precondiciones:** el ticket pertenece a su empresa.
- **Flujo principal:**
  1. En el detalle del ticket, la persona ve el panel "Historial de cambios".
  2. Cada línea indica qué cambió (estado, agente o nivel), de qué valor a cuál, quién lo hizo, cuándo y con qué motivo, del más reciente al más antiguo.
- **Flujos alternativos y errores:**
  - A1. Un cliente solicita el historial: 403.
  - A2. Los cambios hechos por el sistema muestran "Sistema" como autor.
- **Postcondiciones:** ninguna. El historial no se modifica.
- **Referencias:** `GET /tickets/{id}/historial`.

### UC-16: Configurar módulos y ventana de mantenimiento

- **Actor:** Administrador.
- **Objetivo:** adaptar el sistema a su empresa.
- **Precondiciones:** sesión de administrador.
- **Flujo principal:**
  1. El administrador entra a "Administración" y a la pestaña "Configuración".
  2. Tilda o destilda los módulos opcionales. Tickets y Comentarios siempre están activos.
  3. Define la hora de inicio y de fin de la ventana de mantenimiento.
  4. Toca "Guardar cambios".
  5. El sistema valida, guarda y registra el cambio. El menú de la empresa se adapta.
- **Flujos alternativos y errores:**
  - A1. Un módulo que no existe: 400 con la lista de módulos válidos.
  - A2. La ventana tiene solo una de las dos horas, o ambas son iguales: 400.
  - A3. Consulta o modifica la configuración de otra empresa: 403 "No podés acceder a la configuración de otra empresa".
- **Postcondiciones:** la configuración de la empresa queda actualizada.
- **Referencias:** `GET /empresas/{id}/configuracion`, `PATCH /empresas/{id}/configuracion`.

### UC-17: Gestionar categorías propias

- **Actor:** Administrador.
- **Objetivo:** sumar categorías específicas del rubro de la empresa.
- **Precondiciones:** el módulo de categorías propias está habilitado.
- **Flujo principal:**
  1. En la pestaña "Categorías" el administrador ve las generales y las propias.
  2. Completa el nombre (y, si quiere, la descripción) y toca "Crear categoría".
  3. La nueva categoría queda disponible para los clientes de su empresa.
- **Flujos alternativos y errores:**
  - A1. Ya existe una categoría con ese nombre: 409.
  - A2. El módulo está deshabilitado: 403, y la pantalla explica cómo activarlo.
- **Postcondiciones:** existe una categoría propia visible solo en esa empresa.
- **Referencias:** `GET /categorias`, `POST /categorias`.

### UC-18: Gestionar usuarios y roles

- **Actor:** Administrador.
- **Objetivo:** dar de alta personas, asignarles roles y activarlas o desactivarlas.
- **Precondiciones:** sesión de administrador.
- **Flujo principal:**
  1. En la pestaña "Usuarios" el administrador ve las personas de su empresa con sus roles y su estado.
  2. Puede agregar una persona (nombre, email, contraseña inicial de al menos 8 caracteres y rol principal).
  3. Puede sumarle un rol a una persona existente.
  4. Puede desactivar o activar una cuenta. Las cuentas nunca se eliminan.
- **Flujos alternativos y errores:**
  - A1. El email ya existe en la empresa: 409.
  - A2. La persona ya tiene ese rol: 409 "El usuario ya tiene el rol ...".
  - A3. Intenta desactivar su propia cuenta: 400 "No podés desactivar tu propia cuenta".
  - A4. Intenta desactivar a un agente con tickets abiertos: 409 "El agente tiene N ticket(s) abierto(s) a su cargo. Reasignalos antes de desactivarlo".
  - A5. La cuenta ya estaba en ese estado: 409.
- **Postcondiciones:** los usuarios reflejan los cambios. El historial de tickets de una persona desactivada permanece intacto.
- **Referencias:** `GET /usuarios`, `POST /usuarios`, `POST /usuarios/{id}/roles`, `PATCH /usuarios/{id}/desactivar`, `PATCH /usuarios/{id}/activar`.

### UC-19: Cerrar automáticamente un ticket resuelto

- **Actor:** Sistema.
- **Disparador:** una tarea programada que se ejecuta cada hora.
- **Condición:** el ticket está `RESUELTO` y su fecha de resolución es anterior a 72 horas.
- **Flujo principal:**
  1. El sistema busca los tickets que cumplen la condición.
  2. Los pasa a `CERRADO`.
  3. Registra el cambio sin autor, de modo que el historial muestre "Sistema".
- **Postcondiciones:** el silencio del cliente se interpreta como conformidad y el ticket queda cerrado.
- **Referencias:** RN6, RN12.

### UC-20: Devolver a Nivel 1 un ticket estancado en Crítico

- **Actor:** Sistema.
- **Disparador:** la misma tarea programada de cada hora.
- **Condición:** el ticket está en `CRITICO`, no está resuelto ni cerrado y pasaron más de 5 días sin actualizaciones.
- **Flujo principal:**
  1. El sistema busca los tickets que cumplen la condición.
  2. Los pasa a `NIVEL_1` y los marca como reincidentes.
  3. Registra el cambio sin autor, con el motivo de que superó los 5 días en el nivel máximo.
- **Postcondiciones:** el caso se retoma desde el nivel inicial, con la alerta de que ya falló en el nivel máximo.
- **Referencias:** RN7, RN12.

### UC-21: Aplicar la ventana de mantenimiento

- **Actor:** Sistema.
- **Disparador:** cada petición de escritura sobre las rutas de tickets (`POST`, `PUT`, `PATCH` y `DELETE` bajo `/api/v1/tickets`).
- **Condición:** la hora actual, en la zona horaria de Argentina, está dentro de la ventana configurada por la empresa del usuario. La ventana puede cruzar la medianoche.
- **Flujo principal:**
  1. El sistema detecta que la empresa está en mantenimiento.
  2. Rechaza la petición con 503 y un aviso de indisponibilidad temporal.
- **Flujos alternativos:**
  - A1. Las consultas (`GET`) siguen funcionando.
  - A2. Los cambios de configuración, de categorías y de usuarios no se bloquean.
- **Postcondiciones:** no se procesan altas ni cambios sobre tickets durante la ventana.
- **Referencias:** RN17.
