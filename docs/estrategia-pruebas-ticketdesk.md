# TicketDesk: Estrategia de Pruebas

## Documento base: Trabajo Final Integrador UTN TUPaD

Este documento describe cómo se prueba TicketDesk: qué se verifica, con qué técnicas, en qué entornos, qué validaciones de entrada aplica el sistema y qué escenarios de error son relevantes. Los resultados de las pruebas ya ejecutadas están en `pruebas-ticketdesk.md`, y los criterios que las motivan en `criterios-aceptacion-ticketdesk.md`.

---

## 1. Objetivos

- Verificar que cada criterio de aceptación se cumple y dejar constancia de qué prueba lo confirma.
- Comprobar que las restricciones por rol y por empresa se respetan en el servidor y no solo en la interfaz.
- Confirmar que el sistema responde con un código y un mensaje claros ante datos inválidos, operaciones no permitidas y fallas de disponibilidad.
- Detectar defectos de usabilidad antes de la entrega.

## 2. Alcance

| Incluido | No incluido en esta versión |
|---|---|
| Operaciones de la API y sus permisos | Pruebas unitarias automatizadas del código |
| Flujos completos en la aplicación web con cada rol | Pruebas de carga y de rendimiento |
| Validación de datos de entrada y mensajes de error | Pruebas de compatibilidad con todos los navegadores (la voz se prueba en Chrome y Edge) |
| Sistema desplegado en la nube | Pruebas de accesibilidad |

La ausencia de pruebas unitarias automatizadas es una limitación reconocida. Se compensa con una verificación automática desde afuera del sistema (ver sección 3) y queda registrada como mejora futura.

## 3. Niveles y técnicas de prueba

| Nivel | Técnica | Herramienta | Qué cubre |
|---|---|---|---|
| Funcional de la API | Peticiones HTTP directas con una persona de cada rol | PowerShell | Operaciones, permisos y códigos de respuesta (casos B de `pruebas-ticketdesk.md`) |
| Funcional de la interfaz | Recorridos manuales en el navegador con cada rol | Chrome | Pantallas, avisos, recorrido de estados y formularios (casos F y M) |
| Verificación automática | Script que recorre los casos principales y marca cada uno como aprobado o con fallo | `smoke-test.ps1` | 25 verificaciones sobre el sistema desplegado (casos D) |
| Verificación automática ampliada | Segundo script con los casos de validación, permisos y errores que faltaban | `smoke-test-ampliado.ps1` | Casos N01 a N13 y N16 de la sección 8 |
| Con manipulación de tiempo | Se modifica la fecha de un ticket en la base para provocar la regla y se reinicia el servidor | Panel de SQL de Supabase | Cierre automático y rebote desde Crítico (casos N14 y N15) |
| Seguridad por rol | Cada operación se prueba con un rol permitido y con uno no permitido | PowerShell y navegador | Códigos 401, 403 y 404 |

## 4. Entornos y datos

| Entorno | Componentes | Uso |
|---|---|---|
| Local | Backend en el entorno de desarrollo, PostgreSQL local, aplicación web en modo de desarrollo y Cloudinary real | Desarrollo y primeras pruebas |
| En línea | Frontend en Vercel, backend en Render, base en Supabase y Cloudinary real | Verificación final |

Datos de prueba: una empresa ("Empresa Demo"), cinco categorías genéricas y cuatro usuarios, uno por rol (el supervisor también es agente). Se cargan automáticamente cuando la base está vacía. Las pruebas automáticas crean tickets con el prefijo `[smoke]` y los casos ampliados crean datos con el prefijo `QA`, para distinguirlos de los de la demostración.

## 5. Enfoque

1. **Basado en criterios.** Cada criterio de aceptación debe tener al menos un caso de prueba. Los que todavía no lo tienen figuran como "pendiente de prueba" y se cubren con los casos de la sección 8.
2. **Positivo y negativo.** Para cada operación se prueba el camino correcto y, al menos, un camino de error.
3. **Por rol.** Las operaciones restringidas se prueban con un rol permitido y con uno que no lo es.
4. **Regresión.** Después de cada despliegue se vuelve a ejecutar la verificación automática.
5. **Defectos.** Cada defecto encontrado se registra con su corrección en `pruebas-ticketdesk.md`.

Criterio de entrada: el sistema compila y arranca, y la base tiene los datos de prueba.
Criterio de salida: todos los criterios están verificados, salvo los que dependan de una función no implementada, que quedan declarados como tales.

## 6. Validaciones de entrada

La API valida los datos antes de procesarlos. Cuando una validación falla, responde con un código de error y un mensaje. Los errores de validación de datos (400) tienen el formato "Datos inválidos. campo: motivo". Los errores de permisos (403) responden siempre "No tenés permisos para realizar esta acción", sin detallar la causa. El resto de los errores devuelve el mensaje indicado en la tabla.

| Operación | Dato | Regla | Respuesta si no se cumple |
|---|---|---|---|
| Registro | `nombre`, `email`, `password`, `empresaId` | No pueden estar vacíos, y el email debe tener formato válido | 400 |
| Registro | `email` | No puede repetirse dentro de la empresa | 409 "Ya existe un usuario con ese email en la empresa" |
| Registro | `empresaId` | Debe ser una empresa existente y activa | 404 "Empresa no encontrada" |
| Alta de usuario por el administrador | `password` | Al menos 8 caracteres | 400 "Datos inválidos. password: debe tener al menos 8 caracteres" |
| Alta de usuario por el administrador | `rol` | Obligatorio | 400 |
| Crear ticket | `titulo`, `descripcion` | No pueden estar vacíos | 400 |
| Crear ticket | `categoriaId`, `prioridad` | Obligatorios. La categoría debe ser genérica o de la empresa | 400, o 404 "Categoría no encontrada" |
| Comentar | `contenido` | No puede estar vacío | 400 |
| Comentar o adjuntar | Estado del ticket | El ticket no puede estar cerrado | 409 "El ticket está cerrado y no admite nuevos comentarios" o "...nuevos adjuntos" |
| Adjuntar | Archivo | Debe ser una imagen, no estar vacío y pesar hasta 10 MB | 400 "Solo se aceptan imágenes (jpg, png, etc.)", 400 "El archivo está vacío" o 413 |
| Adjuntar | Rol | Solo el cliente | 403 |
| Voz | `transcripcion` | No puede estar vacía | 400 "La transcripción no puede estar vacía" |
| Voz | Archivo de audio | Debe existir y ser un audio | 400 "Falta el audio original" o "El archivo enviado no es un audio" |
| Cambiar estado | `estadoNuevo` | Debe ser una transición permitida para el rol | 409 "No se puede pasar un ticket de X a Y" |
| Cambiar estado | `motivo` | Obligatorio al escalar | 400 "Para escalar un ticket el motivo es obligatorio" |
| Asignar | Persona elegida | Debe estar activa, ser de la empresa y tener rol de agente o supervisor | 404 "Agente no encontrado" o 400 |
| Asignar | Rol de quien asigna | Un agente solo puede tomar tickets para sí mismo | 403 |
| Cambiar nivel | Estado del ticket | No puede estar cerrado | 409 "El ticket está cerrado y no admite cambios" |
| Crear categoría | `nombre` | No puede repetirse dentro de la empresa | 409 "Ya existe una categoría llamada ..." |
| Configuración | Módulos | Deben ser módulos conocidos | 400 con la lista de módulos válidos |
| Configuración | Ventana de mantenimiento | Debe tener inicio y fin, y no pueden ser iguales | 400 |
| Configuración | Empresa | Solo la propia | 403 |
| Desactivar usuario | Tickets abiertos | Un agente con tickets abiertos no se puede desactivar | 409 "El agente tiene N ticket(s) abierto(s) a su cargo..." |
| Desactivar usuario | Persona | No puede desactivarse la propia cuenta | 400 "No podés desactivar tu propia cuenta" |

La interfaz agrega controles propios antes de enviar: contraseña de al menos 8 caracteres en el registro, límite de 200 caracteres en el título, botón de creación inactivo hasta tener categoría y texto, y aviso junto a los botones cuando falta el motivo al escalar. La API de registro no vuelve a validar la longitud de la contraseña; queda como mejora.

## 7. Escenarios de error relevantes

| Código | Cuándo ocurre | Ejemplo | Cobertura |
|---|---|---|---|
| 400 | Dato faltante, con formato inválido o regla no cumplida | Escalar sin motivo | Verificado (B18, D13, F10) y casos N04, N05, N11 |
| 401 | Credenciales incorrectas, cuenta desactivada, o token vencido o ausente | Iniciar sesión con una contraseña errónea | Caso N03 |
| 403 | La persona no tiene permiso, o el módulo está deshabilitado | Un cliente cambia el nivel de atención | Verificado (B13, B14, B16, D9, D10, D16, D22) y casos N06, N12, N13 |
| 404 | El recurso no existe o no pertenece a la persona o a su empresa | Un cliente abre un ticket ajeno | Verificado (B15) |
| 409 | La operación entra en conflicto con el estado actual o con una regla | El agente intenta cerrar un ticket resuelto | Verificado (B10, D7, F14) y casos N02, N10 |
| 413 | El archivo supera los 10 MB. Según el servidor, también puede manifestarse como un corte de la conexión | Subir una foto de 11 MB | Caso N05 (si la conexión se corta, el script lo marca para revisar a mano) |
| 503 | La empresa está dentro de su ventana de mantenimiento | Crear un ticket durante la ventana | Verificado (B22) |

Otros escenarios que la interfaz resuelve de forma visible:

| Escenario | Comportamiento esperado |
|---|---|
| El servidor tarda en responder tras un período sin uso | Aparece un mensaje de conexión; al esperar y recargar, la aplicación carga (entre 2 y 3 minutos en el plan gratuito) |
| El token de sesión vence | La aplicación vuelve a la pantalla de ingreso |
| El navegador no concede el micrófono | La pantalla de voz informa que no pudo acceder y no permite grabar |
| El navegador no transcribe voz | La pantalla avisa y permite escribir el texto a mano |
| La sugerencia de categoría no reconoce palabras clave | El sistema sugiere la categoría Consulta por defecto y el cliente puede cambiarla (caso N16) |

## 8. Casos de prueba nuevos

Estos casos cubren los criterios de aceptación que todavía figuran como pendientes. Los casos N01 a N13 y N16 los ejecuta `smoke-test-ampliado.ps1` y los casos N14 y N15 se hacen a mano.

| Caso | Criterio | Pasos | Resultado esperado |
|---|---|---|---|
| N01 | CA-RF1-02 | El administrador crea un usuario agente con una contraseña de 10 caracteres. Se inicia sesión con ese usuario | La creación funciona y el inicio de sesión devuelve el rol de agente |
| N02 | CA-RF1-03 | Se registra dos veces el mismo email de cliente en la misma empresa | El segundo intento responde 409 |
| N03 | CA-RF1-06 | El administrador desactiva el usuario creado en N01 y esa persona intenta iniciar sesión | Responde 401 "El usuario está desactivado" |
| N04 | CA-RF2-02 | Un cliente crea un ticket sin título | Responde 400 |
| N05 | CA-RF2-09 | Un cliente sube un archivo de texto como si fuera una imagen, y luego un archivo de 11 MB | Responden 400 y 413 respectivamente |
| N06 | CA-RF3-03 | Un agente intenta asignar un ticket al supervisor | Responde 403 |
| N07 | CA-RF3-09 | Un cliente reabre un ticket resuelto | El estado pasa a `REABIERTO` |
| N08 | CA-RF3-10 | Un ticket en `ESPERANDO_CLIENTE` recibe un comentario del cliente | El estado vuelve a `EN_PROGRESO` |
| N09 | CA-RF4-04 | Un agente cambia el nivel de un ticket a `NIVEL_2` | El ticket muestra `NIVEL_2` |
| N10 | CA-RF4-08 | El administrador crea dos categorías con el mismo nombre | La segunda responde 409 |
| N11 | CA-RF6-05 | El administrador guarda una ventana con la misma hora de inicio y de fin | Responde 400 |
| N12 | CA-RF6-06 | El administrador consulta la configuración de otra empresa | Responde 403 |
| N13 | CA-RF2-06, CA-RF6-03 | Se deshabilita el módulo de voz y un cliente pide una sugerencia de voz. Luego se vuelve a habilitar | La petición responde 403 y la configuración queda restaurada |
| N14 | CA-RF3-11, CA-RF5-02 | Ver el procedimiento de la sección 9 | El ticket queda `CERRADO` y el historial muestra "Sistema" |
| N15 | CA-RF3-12, CA-RF5-02 | Ver el procedimiento de la sección 9 | El ticket vuelve a `NIVEL_1`, marcado como reincidente, con autor "Sistema" |
| N16 | CA-RF2-05 | Se pide una sugerencia de voz con un texto sin palabras clave reconocidas | La categoría sugerida es Consulta |

El criterio CA-RF6-02 (aislamiento con una segunda empresa) no tiene caso porque el sistema no ofrece una operación para crear empresas y la demostración usa una sola. Queda como limitación declarada.

## 9. Procedimiento para las reglas con tiempo (N14 y N15)

Las reglas de cierre automático y de rebote dependen del paso del tiempo. Para probarlas sin esperar días se adelanta la fecha de un ticket de prueba directamente en la base y se reinicia el servidor, ya que la tarea programada también se ejecuta al iniciar.

1. Crear con la aplicación dos tickets de prueba y llevar uno hasta `RESUELTO`. El otro se deja en `EN_PROGRESO` y se le asigna el nivel `CRITICO`.
2. En el panel de SQL de Supabase, adelantar las fechas de esos dos tickets (reemplazando los identificadores por los de los tickets de prueba):

```sql
UPDATE tickets SET fecha_resuelto = now() - interval '73 hours' WHERE id = 'ID_DEL_TICKET_RESUELTO';
UPDATE tickets SET fecha_actualizacion = now() - interval '6 days' WHERE id = 'ID_DEL_TICKET_CRITICO';
```

3. En Render, reiniciar el servicio (Manual Deploy o Restart) y esperar a que figure como activo.
4. Abrir los dos tickets. El primero debe estar `CERRADO` y el segundo en `NIVEL_1` con la marca de reincidente. En el historial de ambos, el cambio figura hecho por "Sistema".
5. Registrar los resultados en `pruebas-ticketdesk.md`.

Para el rebote cuenta la fecha de la última modificación del ticket (cambios de estado, de agente o de nivel). Agregar un comentario no la modifica.

Esta prueba modifica datos de la base en línea. Se hace sobre tickets creados para la prueba, nunca sobre los de la demostración.

## 10. Registro y seguimiento

- Los resultados de cada ejecución se agregan a `pruebas-ticketdesk.md`, con la fecha y el entorno.
- Cuando un criterio pasa a verificado, se actualiza su estado en `criterios-aceptacion-ticketdesk.md` y el resumen de la matriz.
- Los defectos se anotan con su corrección y se vuelve a ejecutar la verificación automática.

## 11. Riesgos y limitaciones

| Riesgo o limitación | Mitigación |
|---|---|
| No hay pruebas unitarias automatizadas | La verificación automática y los casos ampliados recorren el sistema completo. Se declara como mejora futura |
| El servidor gratuito tarda en despertar | Las pruebas empiezan con una petición de calentamiento y se abre la aplicación antes de cualquier demostración |
| Las pruebas modifican datos en línea | Se identifican con prefijos y se hacen con datos propios de la prueba. El script que cambia la configuración la restaura al terminar |
| La voz depende del navegador | Se prueba en Chrome y Edge, y la interfaz ofrece el texto manual como alternativa |
| Solo hay una empresa de demostración | El aislamiento entre empresas se verifica con clientes distintos y queda declarada la falta de una prueba con dos empresas |
