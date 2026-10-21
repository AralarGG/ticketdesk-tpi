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
| #08  Consulta de facturación     [escalado]       |
| #05  Error al subir foto         [resuelto]       |
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
|          Nivel de Atención [Todos v]              |
+--------------------------------------------------+
| Tickets (8 de 42)                                 |
|----------------------------------------------------|
| #12  No puedo iniciar sesión  [nuevo]  [Alta] [N1] |
| #15  Error de pago         [nuevo] [Alta] [CRÍTICO]|
| #18  Consulta general    [asignado] [Media]  [N2]  |
+--------------------------------------------------+
```

**Nota:** la etiqueta de Nivel de Atención (N1, N2, N3, CRÍTICO) se muestra junto a la prioridad, pero son campos independientes (ver justificación en README, sección "Prioridad vs. Nivel de Atención"). El nivel CRÍTICO se destaca visualmente (ej. color rojo) por su SLA de 4hs.

---

## 7. Detalle de Ticket (vista Agente)

Corresponde al flujo 3 (pasos 3 a 8) y flujo 4 (escalado).

```
+--------------------------------------------------+
| < Volver     Ticket #12       [nuevo]             |
+--------------------------------------------------+
| No puedo iniciar sesión                           |
| Cliente: Juan Pérez | Categoría: Bug | Alta        |
| Nivel de Atención: [ N1        v ]  (SLA: 24hs)   |
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

---

## Cambios entre el diseño original y la implementación final

Los wireframes anteriores son el diseño inicial. Al implementar el frontend se ajustaron los siguientes puntos, que reflejan lo que efectivamente funciona en la aplicación:

1. **Login (pantalla 1):** se agregó el selector de **Empresa**, porque el login requiere email, contraseña y `empresaId` (regla RN1, el email es único por empresa). El registro de clientes se hace en la misma pantalla.
2. **Nuevo ticket por formulario (pantalla 3):** las fotos no se adjuntan en el formulario sino desde el detalle del ticket, una vez creado. El botón se llama "Crear ticket".
3. **Nuevo ticket por voz (pantalla 4):** la grabación y la transcripción en vivo ocurren en la misma pantalla. Al terminar se muestran el audio grabado, el texto editable, la categoría sugerida (editable) y la prioridad, con los botones "Crear ticket" y "Grabar de nuevo".
4. **Detalle de ticket (pantallas 5 y 7):** se agregó el **recorrido de estados** de cinco pasos. Para el personal de soporte se suman la barra de **tiempo objetivo (SLA)** y el **historial de cambios**. Los tickets se identifican por título y fecha, no por número.
5. **Detalle de ticket, vista agente (pantalla 7):** en lugar de un selector de estado, se muestran solo los botones de las acciones válidas según el estado actual (Tomar este ticket, Empezar a trabajar, Pedir información al cliente, Escalar a un supervisor, Marcar como resuelto, Retomar). El motivo es obligatorio únicamente al escalar. Los supervisores ven además un selector "Asignar a".
6. **Detalle de ticket, vista cliente (pantalla 5):** cuando el ticket está resuelto aparecen los botones "Sí, cerrar el ticket" y "El problema continúa".
7. **Panel de agente (pantalla 6):** además de estado, prioridad y nivel, se puede filtrar por categoría y por "Solo los míos". El contador muestra la cantidad de tickets de la búsqueda.
8. **Gestión de agentes (pantalla 9):** se implementó como la pestaña **Usuarios** del panel de administración, con las acciones de sumar un rol, activar y desactivar. Si el agente tiene tickets abiertos, el sistema rechaza la baja con un mensaje que indica cuántos son, y la reasignación se hace desde cada ticket. El botón "Reasignar todos y desactivar" queda como mejora futura.
9. **Panel de administración (pantalla 8):** además de la configuración de módulos y la ventana de mantenimiento, incluye las pestañas **Categorías** (alta de categorías propias de la empresa) y **Usuarios**.

## Decisiones de interfaz y su respaldo en los artefactos funcionales

Para que los documentos funcionales sean la única fuente de verdad, cada decisión de interfaz tomada al implementar las pantallas está respaldada en los requerimientos, las reglas, los casos de uso o los criterios de aceptación. Si una pantalla cambia, el cambio se refleja en estos artefactos antes de darse por cerrado.

| N° | Decisión de interfaz | Dónde está documentada |
|---|---|---|
| 1 | El login pide la empresa además del email y la contraseña | RN1, UC-01, UC-02, CA-RF1-03 |
| 2 | Las fotos se suben desde el detalle del ticket, después de crearlo | UC-03 (paso 5), UC-07, flujo 1, CA-RF2-07 |
| 3 | La voz se resuelve en una sola pantalla, con transcripción en vivo y categoría sugerida editable | UC-04, RN14, flujo 2, CA-RF2-03 a CA-RF2-06 |
| 4 | El detalle muestra un recorrido de cinco pasos, la barra de tiempo objetivo y, solo al personal, el historial | RN5, RN8, UC-05, UC-15, CA-RF4-02, CA-RF5-03 |
| 5 | Los botones de acción muestran solo las transiciones válidas del estado actual | UC-12 (tabla de transiciones), RN5, CA-RF3-04 |
| 6 | El cliente confirma el cierre o indica que el problema continúa desde el detalle | UC-08, flujo 5, RN5, CA-RF3-08, CA-RF3-09 |
| 7 | El panel del personal filtra por estado, prioridad, nivel, categoría y "Solo los míos" | UC-09, RNF7 |
| 8 | La gestión de agentes es la pestaña Usuarios del panel de administración | UC-18, RN3, flujo 7, CA-RF5-05 |
| 9 | El panel de administración incluye una pestaña de categorías propias | UC-17, RN10, CA-RF4-07 |
| 10 | El aviso del motivo obligatorio aparece junto a los botones y el cursor pasa al cuadro | UC-13 (flujo alternativo A1), RN9, CA-RF3-06 |
| 11 | El selector "Asignar a" aparece solo para los supervisores | UC-11, RN9, CA-RF3-02, CA-RF3-03 |
| 12 | El cliente no ve el nivel de atención ni el tiempo objetivo | RN8, CA-RF4-01 |
| 13 | El menú oculta las opciones de los módulos deshabilitados | RN16, UC-16, CA-RF6-03 |
| 14 | Durante la ventana de mantenimiento se rechazan las altas y los cambios con un aviso | RN17, UC-21, CA-RF6-04 |
