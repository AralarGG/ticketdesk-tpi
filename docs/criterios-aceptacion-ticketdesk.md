# TicketDesk: Criterios de Aceptación

## Documento base: Trabajo Final Integrador UTN TUPaD

Este documento define, para cada requerimiento funcional (RF1 a RF7 de `listado-modulos-ticketdesk.md`), los criterios que permiten verificar objetivamente cuándo se considera cumplido. Cada criterio se escribe en el formato **dado, cuando, entonces** y se vincula con el caso de prueba que lo verifica en `pruebas-ticketdesk.md`. Los términos siguen el `diccionario-nomenclatura-ticketdesk.md` y los casos de uso están en `casos-de-uso-ticketdesk.md`.

## Cómo leer las tablas

| Estado | Significado |
|---|---|
| Verificado | Se ejecutó al menos una prueba que lo confirma. Entre paréntesis figuran los códigos de `pruebas-ticketdesk.md` (B: API local, F: aplicación web local, D: verificación automática en la nube, M: pruebas manuales en la nube) |
| Verificado en parte | Se probó una parte del criterio. La nota indica qué falta |
| Pendiente de prueba | El sistema lo implementa, pero todavía no hay un caso de prueba que lo confirme |
| No implementado | El criterio describe algo que esta versión no ofrece |

---

## RF1: Gestión de identidad

| Código | Dado | Cuando | Entonces | Estado |
|---|---|---|---|---|
| CA-RF1-01 | un visitante que eligió su empresa | se registra con nombre, email y contraseña | se crea una cuenta de cliente activa en esa empresa y se inicia la sesión | Verificado (B15) |
| CA-RF1-02 | un administrador autenticado | agrega una persona con nombre, email, contraseña de al menos 8 caracteres y rol principal | la persona queda en la empresa del administrador y puede iniciar sesión | Pendiente de prueba |
| CA-RF1-03 | un email ya registrado en una empresa | se registra el mismo email en otra empresa | se crea una cuenta independiente con su propia contraseña. Si se repite dentro de la misma empresa, el sistema responde 409 | Pendiente de prueba (la empresa de demostración es una sola) |
| CA-RF1-04 | un usuario con un rol | el administrador le suma otro rol | el usuario conserva ambos roles. Si ya tenía ese rol, el sistema responde 409 | Verificado (F16) |
| CA-RF1-05 | un usuario con los roles de agente y supervisor | inicia sesión | puede realizar las acciones de ambos roles | Verificado (D12, M4) |
| CA-RF1-06 | una cuenta desactivada | la persona intenta iniciar sesión | el sistema responde 401 "El usuario está desactivado" | Pendiente de prueba |

Nota: la contraseña mínima de 8 caracteres se exige en el alta de usuarios por el administrador y en la pantalla de registro. La API de registro no la valida; queda como mejora.

## RF2: Creación de tickets

| Código | Dado | Cuando | Entonces | Estado |
|---|---|---|---|---|
| CA-RF2-01 | un cliente con sesión iniciada | completa título, descripción, categoría y prioridad y confirma | se crea un ticket `NUEVO`, con nivel `NIVEL_1`, canal `FORMULARIO` y sin agente | Verificado (B4, F3, D3) |
| CA-RF2-02 | un cliente que crea un ticket | omite el título, la descripción, la categoría o la prioridad | el sistema responde 400 y no crea el ticket | Pendiente de prueba |
| CA-RF2-03 | un cliente con el módulo de voz habilitado | graba un mensaje y termina | se muestra el texto transcripto y se sugieren una categoría y un título. El ticket todavía no se crea | Verificado (B20, F6, D18, M2) |
| CA-RF2-04 | una sugerencia de voz mostrada | el cliente confirma | se crea un ticket con canal `VOZ`, con el audio original y la transcripción guardados | Verificado (B21, F6, D19, M2) |
| CA-RF2-05 | una transcripción sin palabras clave reconocidas | el cliente termina la grabación | el sistema sugiere la categoría Consulta por defecto y el cliente puede cambiarla antes de crear | Pendiente de prueba (N16) |
| CA-RF2-06 | el módulo de voz deshabilitado en la empresa | un cliente entra a la aplicación | la opción de voz no aparece y la API responde 403 si se la invoca | Verificado en parte (F12). Falta probar la respuesta 403 de la API |
| CA-RF2-07 | un cliente dueño de un ticket no cerrado | sube una imagen | la foto queda asociada al ticket y se muestra como miniatura | Verificado (B17, F5, D17, M3) |
| CA-RF2-08 | un agente o un supervisor | intenta subir una foto | el sistema responde 403 | Verificado (B16, D16) |
| CA-RF2-09 | un cliente | sube un archivo que no es una imagen, o uno de más de 10 MB | el sistema responde 400 o 413 respectivamente | Pendiente de prueba |

## RF3: Ciclo de vida del ticket

| Código | Dado | Cuando | Entonces | Estado |
|---|---|---|---|---|
| CA-RF3-01 | un ticket `NUEVO` | un agente lo toma | el ticket pasa a `ASIGNADO`, queda a cargo del agente y se registran el cambio de agente y el de estado | Verificado (B6, D4, F8) |
| CA-RF3-02 | un ticket cualquiera no cerrado | un supervisor lo asigna a un agente | el ticket queda a cargo del agente elegido | Verificado (D12, M4) |
| CA-RF3-03 | un agente | intenta asignar un ticket a otra persona | el sistema responde 403 | Pendiente de prueba |
| CA-RF3-04 | un ticket asignado | el personal lo avanza por las transiciones permitidas | el estado cambia y el cambio queda en el historial | Verificado (B7, B9, D5, D6, F8) |
| CA-RF3-05 | un ticket `RESUELTO` | el agente intenta cerrarlo | el sistema responde 409 | Verificado (B10, D7) |
| CA-RF3-06 | un ticket `EN_PROGRESO` | se intenta escalar sin motivo | el sistema responde 400 | Verificado (B18, D13, F10) |
| CA-RF3-07 | un ticket `EN_PROGRESO` | se escala con motivo | el ticket pasa a `ESCALADO`, sube un nivel de atención (hasta `NIVEL_3`) y se reasigna al supervisor con menos tickets abiertos. Se registran tres cambios | Verificado (B19, D14, D15, F11) |
| CA-RF3-08 | un ticket `RESUELTO` | el cliente confirma el cierre | el ticket pasa a `CERRADO` y ya no admite comentarios ni fotos | Verificado (B11, D8, F9) |
| CA-RF3-09 | un ticket `RESUELTO` | el cliente indica que el problema continúa | el ticket pasa a `REABIERTO`, se borra su fecha de resolución y vuelve al panel del personal | Pendiente de prueba |
| CA-RF3-10 | un ticket `ESPERANDO_CLIENTE` | el cliente dueño agrega un comentario | el ticket vuelve solo a `EN_PROGRESO` y el cambio queda registrado | Pendiente de prueba |
| CA-RF3-11 | un ticket `RESUELTO` hace más de 72 horas | se ejecuta la tarea programada | el ticket pasa a `CERRADO` y el historial muestra "Sistema" como autor | Pendiente de prueba (requiere simular el paso del tiempo) |
| CA-RF3-12 | un ticket en `CRITICO`, sin cambios de estado, agente o nivel hace más de 5 días y no resuelto | se ejecuta la tarea programada | el ticket vuelve a `NIVEL_1`, queda marcado como reincidente y el cambio se registra sin autor | Pendiente de prueba (requiere simular el paso del tiempo) |

## RF4: Clasificación de tickets

| Código | Dado | Cuando | Entonces | Estado |
|---|---|---|---|---|
| CA-RF4-01 | un cliente que crea un ticket | elige una prioridad | la prioridad queda guardada tal como la eligió, es independiente del nivel de atención y el cliente no ve el nivel | Verificado (B4, F2) |
| CA-RF4-02 | un ticket recién creado | se consulta | tiene nivel `NIVEL_1`, tiempo objetivo de 24 horas y vencimiento igual a la fecha de creación más 24 horas | Verificado (B4, D3) |
| CA-RF4-03 | un ticket escalado a `NIVEL_2` | se consulta | el tiempo objetivo es de 48 horas | Verificado (F11) |
| CA-RF4-04 | un agente o un supervisor | cambia el nivel de atención de un ticket no cerrado | el ticket tiene el nuevo nivel y su tiempo objetivo, y el cambio queda en el historial con la persona y el motivo | Pendiente de prueba |
| CA-RF4-05 | un cliente | intenta cambiar el nivel de atención | el sistema responde 403 | Verificado (B13, D9) |
| CA-RF4-06 | un cliente que crea un ticket | elige una categoría | puede elegir las genéricas y las propias de su empresa, y no las de otra empresa | Verificado en parte (B3, D2, F13). Falta probar la restricción con una segunda empresa |
| CA-RF4-07 | el módulo de categorías propias habilitado | el administrador crea una categoría | la categoría es visible para los clientes de su empresa | Verificado (F13) |
| CA-RF4-08 | una categoría propia existente | el administrador crea otra con el mismo nombre | el sistema responde 409 | Pendiente de prueba |

## RF5: Trazabilidad

| Código | Dado | Cuando | Entonces | Estado |
|---|---|---|---|---|
| CA-RF5-01 | un cambio de estado, de agente o de nivel de atención en un ticket | el cambio se produce | se registra el valor anterior, el nuevo, quién lo hizo, cuándo y el motivo | Verificado (B12, D11, F8, F11) |
| CA-RF5-02 | un cambio hecho por el sistema | el cambio se produce | el registro no tiene autor y el historial muestra "Sistema" | Pendiente de prueba |
| CA-RF5-03 | un cliente | solicita el historial de un ticket | el sistema responde 403 | Verificado (B14, D10) |
| CA-RF5-04 | una persona del personal de soporte | consulta el historial | ve los cambios del más reciente al más antiguo | Verificado (B12, F8) |
| CA-RF5-05 | un agente con tickets abiertos a su cargo | el administrador intenta desactivarlo | el sistema responde 409 e indica cuántos tickets tiene | Verificado (F14) |
| CA-RF5-06 | un usuario sin tickets abiertos | el administrador lo desactiva | la cuenta queda desactivada, su historial permanece y puede reactivarse | Verificado (F15) |

Alcance de la trazabilidad en esta versión: la categoría y la prioridad se fijan únicamente al crear el ticket y no existe una operación que las modifique, por lo que no generan registros de cambio. Los cambios que sí se registran en los tickets son los de estado, agente y nivel de atención. El sistema registra además, en la misma tabla, los cambios de roles y de activación de los usuarios y los de la configuración de la empresa, aunque la interfaz solo muestra el historial de los tickets.

## RF6: Multi-empresa

| Código | Dado | Cuando | Entonces | Estado |
|---|---|---|---|---|
| CA-RF6-01 | un cliente | intenta abrir un ticket de otro cliente | el sistema responde 404 | Verificado (B15) |
| CA-RF6-02 | el personal de soporte de una empresa | consulta el listado de tickets | ve solamente los tickets de su empresa | Verificado en parte (F7). Falta probar con una segunda empresa |
| CA-RF6-03 | un administrador | deshabilita un módulo opcional | la opción desaparece del menú de la empresa y la API rechaza su uso con 403 | Verificado en parte (F12). Falta probar el 403 de la API |
| CA-RF6-04 | una ventana de mantenimiento vigente | se intenta crear o modificar un ticket | el sistema responde 503. Las consultas siguen funcionando | Verificado (B22, B23, B24) |
| CA-RF6-05 | un administrador | guarda una ventana con una sola hora, o con la misma hora de inicio y de fin | el sistema responde 400 | Pendiente de prueba |
| CA-RF6-06 | un administrador de una empresa | consulta o modifica la configuración de otra | el sistema responde 403 | Pendiente de prueba |

## RF7: Casos fuera del alcance del software

| Código | Dado | Cuando | Entonces | Estado |
|---|---|---|---|---|
| CA-RF7-01 | cualquier ticket | se consulta | la respuesta incluye si es reclamo formal (falso por defecto) y el responsable externo (vacío por defecto) | Verificado (B21) |
| CA-RF7-02 | una persona del personal de soporte | marca un ticket como reclamo formal o registra un responsable externo | el dato queda guardado y visible en el ticket | No implementado: esta versión incluye los campos en el modelo y en la respuesta, pero no una operación ni una pantalla para completarlos |

---

## Resumen

| Estado | Cantidad |
|---|---|
| Verificado | 28 |
| Verificado en parte | 4 |
| Pendiente de prueba | 16 |
| No implementado | 1 |
| **Total de criterios** | **49** |

Los criterios pendientes de prueba corresponden a funciones ya implementadas en el sistema que todavía no tienen un caso en `pruebas-ticketdesk.md`. Se incorporan como casos nuevos en la estrategia de pruebas, junto con los escenarios de error.
