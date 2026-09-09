# TicketDesk: Wireframes Detallados por Pantalla

## Documento base: Trabajo Final Integrador UTN TUPaD

Este documento complementa la sección "Wireframes y Prototipos de Interfaz" del README principal, donde se documentan los diagramas de flujo (creación de ticket y cambios de estado) en formato Mermaid. Acá se detalla, pantalla por pantalla, cómo se distribuye la información en cada una de las vistas principales del sistema, cubriendo todos los roles de usuario (cliente, agente, administrador). Cada pantalla se corresponde con uno o más pasos de los flujos de usuario ya documentados en `flujos-usuario-ticketdesk.md`.

---

## 1. Login

```
+--------------------------------------------------+
|                    TicketDesk                     |
|                                                    |
|   Email:     [_____________________________]      |
|   Contraseña:[_____________________________]      |
|                                                    |
|              [   Iniciar Sesión   ]               |
|                                                    |
|          ¿No tenés cuenta? Registrate             |
+--------------------------------------------------+
```

---

## 2. Panel de Usuario/Cliente

Corresponde al flujo 1 y 5 (creación y seguimiento de tickets propios).

```
+--------------------------------------------------+
| TicketDesk          Hola, Juan Pérez   [Salir]    |
+--------------------------------------------------+
| [+ Nuevo Ticket]   [+ Nuevo Ticket por Voz]       |
+--------------------------------------------------+
| Mis Tickets                                       |
|----------------------------------------------------|
| #12  No puedo iniciar sesión     [en_progreso]    |
| #08  Consulta de facturación     [resuelto]       |
| #05  Error al subir foto         [cerrado]        |
+--------------------------------------------------+
```

Cada fila es clickeable y lleva al detalle del ticket (pantalla 4).

---

## 3. Nuevo Ticket (formulario)

Corresponde al flujo 1.

```
+--------------------------------------------------+
| < Volver              Nuevo Ticket                |
+--------------------------------------------------+
| Título:      [____________________________]      |
| Descripción: [____________________________]      |
|              [____________________________]      |
| Categoría:   [ Bug            v ]                 |
| Prioridad:   ( ) Baja  (•) Media  ( ) Alta         |
|                                                    |
| Adjuntar foto:  [ Seleccionar archivo ]           |
|                                                    |
|              [   Enviar Ticket   ]                |
+--------------------------------------------------+
```

---

## 4. Nuevo Ticket por Voz

Corresponde al flujo 2, incluyendo el paso de confirmación de la transcripción.

```
Paso 1: Grabación
+--------------------------------------------------+
| < Volver          Crear Ticket por Voz             |
+--------------------------------------------------+
|                                                    |
|              (  🎤  Grabar mensaje  )              |
|                                                    |
|         Contanos qué problema tenés               |
+--------------------------------------------------+

Paso 2: Confirmación (tras procesar el audio)
+--------------------------------------------------+
| Revisá antes de confirmar                         |
+--------------------------------------------------+
| Transcripción:                                    |
| "no puedo iniciar sesión en la app"               |
|                                                    |
| Categoría sugerida: [ Bug          v ]  (editable)|
| Título sugerido:    [_____________________]       |
|                                                    |
|      [ Confirmar y Crear Ticket ]  [ Editar ]      |
+--------------------------------------------------+
```

---

## 5. Detalle de Ticket (vista Cliente)

Corresponde a los flujos 1, 2 y 5.

```
+--------------------------------------------------+
| < Volver     Ticket #12       [en_progreso]       |
+--------------------------------------------------+
| No puedo iniciar sesión                           |
| Categoría: Bug | Prioridad: Media                 |
| Creado: 07/09/2026                                |
+--------------------------------------------------+
| Comentarios                                       |
|----------------------------------------------------|
| Agente María López (07/09 11:00):                 |
| "¿Podés indicarme qué navegador estás usando?"    |
|                                                    |
| Vos (07/09 11:15):                                |
| "Chrome, versión actualizada"  [foto adjunta]     |
+--------------------------------------------------+
| [Escribir un comentario...]      [Adjuntar foto]  |
|                          [ Enviar ]               |
+--------------------------------------------------+
| Si el ticket está resuelto:                       |
|            [ El problema continúa ]               |
+--------------------------------------------------+
```

---

## 6. Panel de Agente

Corresponde al flujo 3 (paso 1 y 2: listado y filtrado).

```
+--------------------------------------------------+
| TicketDesk        Hola, María López    [Salir]    |
+--------------------------------------------------+
| Filtros: Estado [Nuevo v]  Prioridad [Alta v]     |
+--------------------------------------------------+
| Tickets (8 de 42)                                 |
|----------------------------------------------------|
| #12  No puedo iniciar sesión   [nuevo]    [Alta]  |
| #15  Error de pago              [nuevo]    [Alta] |
| #18  Consulta general           [asignado][Media] |
+--------------------------------------------------+
```

---

## 7. Detalle de Ticket (vista Agente)

Corresponde al flujo 3 (pasos 3 a 8) y flujo 4 (escalado).

```
+--------------------------------------------------+
| < Volver     Ticket #12       [nuevo]             |
+--------------------------------------------------+
| No puedo iniciar sesión                           |
| Cliente: Juan Pérez | Categoría: Bug | Alta        |
+--------------------------------------------------+
| [ Asignarme este ticket ]                         |
+--------------------------------------------------+
| Cambiar estado: [ En progreso        v ]          |
| Motivo (si aplica): [_______________________]     |
|                          [ Guardar ]              |
+--------------------------------------------------+
| Comentarios                                       |
| (igual que vista cliente, sin opción de adjuntar) |
+--------------------------------------------------+
| [ Escalar a Supervisor ]                          |
+--------------------------------------------------+
```

---

## 8. Panel de Administración: Configuración de Empresa

Corresponde al flujo 6.

```
+--------------------------------------------------+
| Configuración: Farmacia del Centro                |
+--------------------------------------------------+
| Módulos habilitados:                              |
|   [x] Gestión de tickets básica                   |
|   [x] Comentarios y adjuntos                       |
|   [ ] Facturación empresarial                     |
|   [x] Apertura de tickets por voz                 |
+--------------------------------------------------+
| Ventana de mantenimiento:                         |
|   Desde: [ 03:00 ]   Hasta: [ 04:00 ]             |
+--------------------------------------------------+
|              [   Guardar Configuración   ]        |
+--------------------------------------------------+
```

---

## 9. Panel de Administración: Gestión de Agentes

Corresponde al flujo 7 (baja de agente con reasignación obligatoria).

```
+--------------------------------------------------+
| Agentes de la empresa                             |
+--------------------------------------------------+
| María López      Activo    [ Desactivar ]         |
| Pedro Gómez      Activo    [ Desactivar ]         |
+--------------------------------------------------+

Al intentar desactivar con tickets activos:
+--------------------------------------------------+
| No se puede desactivar a Pedro Gómez               |
| Tiene 3 tickets activos sin reasignar:            |
|   #22  #27  #31                                   |
|                                                    |
| Reasignar a: [ María López       v ]              |
|              [ Reasignar todos y desactivar ]     |
+--------------------------------------------------+
```

---

## Cobertura de Pantallas vs. Flujos de Usuario

| Pantalla | Flujo relacionado |
|---|---|
| Login | Todos |
| Panel de Usuario/Cliente | Flujo 1, 5 |
| Nuevo Ticket (formulario) | Flujo 1 |
| Nuevo Ticket por Voz | Flujo 2 |
| Detalle de Ticket (Cliente) | Flujo 1, 2, 5 |
| Panel de Agente | Flujo 3 |
| Detalle de Ticket (Agente) | Flujo 3, 4 |
| Config. de Empresa | Flujo 6 |
| Gestión de Agentes | Flujo 7 |

Todas las pantallas necesarias para cubrir los flujos documentados están contempladas. El diseño visual definitivo (colores, tipografías, componentes) se desarrollará durante la etapa de implementación del frontend.
