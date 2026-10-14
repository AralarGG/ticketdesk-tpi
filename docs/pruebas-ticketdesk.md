# TicketDesk: Pruebas Realizadas

## Documento base: Trabajo Final Integrador UTN TUPaD

Este documento registra las pruebas ejecutadas sobre el sistema y sus resultados. Las pruebas de la API se realizaron enviando peticiones HTTP directas a un servidor local, y las de la aplicación web se hicieron a mano en el navegador Chrome contra ese mismo servidor, con una empresa de prueba y usuarios de los distintos roles.

**Fecha de ejecución:** 6 de octubre de 2026
**Entorno:** backend en ejecución local con PostgreSQL, frontend en modo de desarrollo, almacenamiento real de archivos en Cloudinary.

---

## 1. Pruebas de la API

| N° | Caso | Resultado esperado | Resultado |
|---|---|---|---|
| B1 | Listar empresas desde una ruta pública | 200, con la empresa de prueba | Aprobado |
| B2 | Iniciar sesión como cliente indicando la empresa | Devuelve un token | Aprobado |
| B3 | Listar las categorías visibles | Las 5 categorías genéricas | Aprobado |
| B4 | Crear un ticket como cliente | Estado nuevo, Nivel 1, tiempo objetivo de 24 horas, cliente correcto | Aprobado |
| B5 | El agente consulta el listado | Ve el ticket de la empresa | Aprobado |
| B6 | El agente toma el ticket | Pasa a asignado con su nombre | Aprobado |
| B7 | El agente lo pasa a en progreso | Estado en progreso | Aprobado |
| B8 | El agente comenta | Comentario con rol de agente | Aprobado |
| B9 | El agente lo marca como resuelto | Estado resuelto, con fecha de resolución | Aprobado |
| B10 | El agente intenta cerrar el ticket | Error 409: solo el cliente o el sistema cierran | Aprobado |
| B11 | El cliente confirma el cierre | Estado cerrado | Aprobado |
| B12 | Consultar el historial del ticket | Cinco registros, cada uno con autor y motivo | Aprobado |
| B13 | El cliente intenta cambiar el nivel de atención | Error 403 | Aprobado |
| B14 | El cliente intenta ver el historial | Error 403 | Aprobado |
| B15 | Otro cliente intenta ver un ticket ajeno | Error 404 | Aprobado |
| B16 | El agente intenta subir una foto | Error 403 | Aprobado |
| B17 | El cliente sube una foto | 201, con una dirección de Cloudinary | Aprobado |
| B18 | Escalar un ticket sin motivo | Error 400 | Aprobado |
| B19 | Escalar con motivo | Estado escalado, Nivel 2, reasignado al supervisor | Aprobado |
| B20 | Pedir la sugerencia de categoría por voz | Categoría Facturación para un texto sobre un cobro duplicado | Aprobado |
| B21 | Crear un ticket por voz con audio | 201, canal de origen voz, dirección del audio y transcripción completa | Aprobado |
| B22 | Crear un ticket dentro de la ventana de mantenimiento | Error 503 | Aprobado |
| B23 | Consultar tickets dentro de la ventana de mantenimiento | La consulta funciona | Aprobado |
| B24 | Restaurar la ventana y crear un ticket | Estado nuevo | Aprobado |

## 2. Pruebas de la aplicación web

| N° | Rol | Caso | Resultado esperado | Resultado |
|---|---|---|---|---|
| F1 | Cliente | Iniciar sesión eligiendo la empresa | Ingresa a su panel | Aprobado |
| F2 | Cliente | Ver el listado propio | Solo sus tickets, sin las columnas internas de nivel y agente | Aprobado |
| F3 | Cliente | Crear un ticket por formulario | Redirige al detalle, estado nuevo | Aprobado |
| F4 | Cliente | Agregar un comentario | Aparece con su nombre, rol y hora | Aprobado |
| F5 | Cliente | Subir una foto | Aparece como miniatura | Aprobado |
| F6 | Cliente | Crear un ticket por voz | Transcripción en vivo, categoría Facturación sugerida y editable, ticket con audio y transcripción visibles | Aprobado |
| F7 | Agente | Ver el listado | Tickets de toda la empresa, con cliente, nivel, agente y filtros | Aprobado |
| F8 | Agente | Tomar, trabajar y resolver un ticket | El recorrido avanza, el historial suma cada cambio, el tiempo objetivo indica que se cumplió | Aprobado |
| F9 | Cliente | Confirmar el cierre de un ticket resuelto | Estado cerrado, sin comentarios ni fotos nuevas | Aprobado |
| F10 | Agente | Escalar sin motivo | Aviso junto a los botones y cursor en el cuadro de motivo | Aprobado |
| F11 | Agente | Escalar con motivo | Estado escalado, Nivel 2, tiempo objetivo de 48 horas, supervisor asignado, tres registros nuevos en el historial | Aprobado |
| F12 | Administrador | Desactivar el módulo de voz | La opción desaparece del menú del cliente | Aprobado |
| F13 | Administrador | Crear una categoría propia | Aparece en la lista como propia y en el formulario de los clientes | Aprobado |
| F14 | Administrador | Desactivar a un agente con tickets abiertos | Se rechaza con un mensaje que indica la cantidad | Aprobado |
| F15 | Administrador | Desactivar a un agente sin tickets abiertos | Se permite, y puede reactivarse | Aprobado |
| F16 | Administrador | Sumar un rol a un usuario | Aviso de rol agregado | Aprobado |

## 3. Defectos detectados durante las pruebas

| N° | Defecto | Corrección |
|---|---|---|
| D1 | La fecha de vencimiento terminaba con dos puntos | Se unificó el formato del texto |
| D2 | El cuadro de motivo decía que era obligatorio sin aclarar que solo lo es al escalar | Se renombró como opcional y la ayuda aclara cuándo es obligatorio |
| D3 | El aviso tras cambiar de estado repetía el texto del botón ("pasó a marcar como resuelto") | Se redactó a partir del nombre del estado |
| D4 | Al escalar sin motivo, el aviso aparecía arriba de la página, lejos del botón, y parecía que no pasaba nada | El aviso se muestra junto a los botones y el cursor pasa al cuadro de motivo |
| D5 | El modelo de datos publicado en el repositorio conservaba la tabla anterior de historial | Se reemplazó por la versión alineada con el código |
| D6 | Varios documentos describían comportamientos previos al código final | Se revisaron y actualizaron contra la implementación |

## 4. Pruebas del sistema desplegado

**Fecha de ejecución:** 14 de octubre de 2026
**Entorno:** frontend en Vercel, backend en Render, base de datos en Supabase y archivos en Cloudinary.

### 4.1 Verificación automática

Se ejecutó el script `docs/smoke-test.ps1` contra la dirección pública del backend. El script inicia sesión con los cuatro roles y recorre los casos principales. **Resultado: 25 pruebas aprobadas y 0 con fallo.**

| N° | Caso verificado | Resultado |
|---|---|---|
| D1 | Inicio de sesión como cliente, agente, supervisor y administrador | Aprobado |
| D2 | Hay al menos 5 categorías visibles | Aprobado |
| D3 | El ticket nace en estado nuevo, Nivel 1 | Aprobado |
| D4 | El agente toma el ticket (asignado) | Aprobado |
| D5 | Pasa a en progreso | Aprobado |
| D6 | Pasa a resuelto | Aprobado |
| D7 | El agente no puede cerrar el ticket (409) | Aprobado |
| D8 | El cliente confirma el cierre (cerrado) | Aprobado |
| D9 | El cliente no puede cambiar el nivel de atención (403) | Aprobado |
| D10 | El cliente no puede ver el historial (403) | Aprobado |
| D11 | El historial registra al menos 5 cambios | Aprobado |
| D12 | El supervisor asigna el ticket a un agente | Aprobado |
| D13 | Escalar sin motivo devuelve error (400) | Aprobado |
| D14 | Escalar con motivo: escalado, Nivel 2 | Aprobado |
| D15 | El ticket escalado se reasigna a otra persona (supervisor) | Aprobado |
| D16 | El agente no puede subir fotos (403) | Aprobado |
| D17 | El cliente sube una foto a Cloudinary (201) | Aprobado |
| D18 | La sugerencia por voz propone la categoría Facturación | Aprobado |
| D19 | Se crea un ticket por voz con audio (201) | Aprobado |
| D20 | El administrador lee la configuración de la empresa | Aprobado |
| D21 | El administrador lista los usuarios | Aprobado |
| D22 | El cliente no puede listar usuarios (403) | Aprobado |

La fila D1 agrupa los cuatro inicios de sesión (uno por rol) y cada una de las otras 21 filas es una verificación individual, lo que da 25 en total.

### 4.2 Pruebas manuales en el navegador

| N° | Rol | Caso | Resultado esperado | Resultado |
|---|---|---|---|---|
| M1 | Cliente | Iniciar sesión desde la dirección pública | Ingresa a su panel y ve sus tickets | Aprobado |
| M2 | Cliente | Crear un ticket por voz con el micrófono | Transcripción en vivo, ticket con la etiqueta de voz y audio que se reproduce | Aprobado |
| M3 | Cliente | Subir una foto a un ticket | Aparece en la sección Fotos | Aprobado |
| M4 | Supervisor | Asignar un ticket a un agente desde el selector | Estado asignado, agente actualizado y dos registros nuevos en el historial | Aprobado |
| M5 | Cliente y personal | Verificar la hora de los tickets | Coincide con la hora de Argentina | Aprobado |
| M6 | Administrador | Recorrer las pestañas de configuración, categorías y usuarios | Las tres pestañas cargan sus datos | [COMPLETAR] |

### 4.3 Observaciones del despliegue

- La primera consulta tras un período sin uso tarda entre 2 y 3 minutos, porque el plan gratuito del servidor se suspende por inactividad y el arranque de la aplicación demora cerca de 2 minutos.
- Una contraseña de base de datos con el símbolo `#` se cortó al cargarla con la opción Add from .env de Render, lo que produjo un error de autenticación. Se resolvió volviendo a cargarla a mano. Quedó documentado en la guía de despliegue.

## 5. Alcance de las pruebas

Las pruebas fueron manuales y funcionales. No se desarrollaron pruebas automatizadas del código, lo que se registra como una mejora futura: el script de verificación recorre el sistema desde afuera, pero no reemplaza a las pruebas unitarias. No se probó en tiempo real el cierre automático a las 72 horas: el planificador se ejecuta al iniciar el servidor y luego cada hora.
