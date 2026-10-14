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

## 4. Pruebas pendientes tras el despliegue

Una vez desplegado el sistema se repiten en el entorno en línea los casos principales: inicio de sesión de los cuatro roles, ciclo completo de un ticket, creación por voz, subida de una foto, escalado y panel de administración. Los resultados se registran en esta sección.

| Caso | Resultado | Fecha |
|---|---|---|
| Inicio de sesión de cada rol | [COMPLETAR] | |
| Ciclo completo de un ticket | [COMPLETAR] | |
| Creación por voz | [COMPLETAR] | |
| Subida de una foto | [COMPLETAR] | |
| Escalado con motivo | [COMPLETAR] | |
| Panel de administración | [COMPLETAR] | |

## 5. Alcance de las pruebas

Las pruebas fueron manuales y funcionales. No se desarrollaron pruebas automatizadas, lo que se registra como una mejora futura. No se probaron en tiempo real el cierre automático a las 72 horas (el planificador se ejecuta al iniciar el servidor y luego cada hora) ni la asignación de un ticket a otra persona desde la sesión de un supervisor.
