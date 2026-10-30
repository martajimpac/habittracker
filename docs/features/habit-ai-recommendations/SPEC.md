# SPEC: Chat de hábitos con bloom-api

**Estado:** Aprobada <!-- Borrador | En revisión | Aprobada -->

<!-- PARA LA PERSONA
Copia esta plantilla como SPEC.md en una carpeta de la funcionalidad.
Pide al agente que la complete contigo usando MOBILE_GUIDELINES.md.
SPEC.md define qué debe cumplirse; PLAN.md desarrolla cómo implementarlo;
TASKS.md organiza los pasos de ejecución.
-->

<!-- PARA EL AGENTE
- Lee las instrucciones del proyecto y MOBILE_GUIDELINES.md. Inspecciona el
  repositorio para comprobar el comportamiento actual. Si falta la guía, pide su ubicación.
- Completa esta spec con la persona: investiga lo comprobable y consulta las
  decisiones pendientes. Haz pocas preguntas por vez y actualiza las respuestas.
- No inventes requisitos ni exclusiones. Distingue propuestas de decisiones
  confirmadas y marca como PENDIENTE lo que aún no esté resuelto.
- Aplica las consideraciones mobile relevantes sin ampliar el alcance automáticamente.
- No incluyas diseño de clases, tablas, componentes, archivos o algoritmos:
  esos detalles pertenecen a PLAN.md. Sí registra restricciones explícitas del pedido.
- Mantén el documento breve y proporcional a la funcionalidad. Conserva los comentarios.
- Un documento completo no está aprobado automáticamente. Solicita aprobación
  antes de marcarlo como Aprobada. No implementes durante esta etapa.
-->

Esta revisión sustituye el comportamiento de recomendación de un solo uso aprobado el 2026-10-28. Quedó aprobada el 2026-10-30, cuando se pidió cerrar el flujo con las acciones recomendadas y autorizar la implementación.

## Qué construimos y para quién

La persona que usa Bloom, con la sesión iniciada, puede escribir a un asistente sobre sus hábitos y leer la respuesta en la misma pantalla. Cada mensaje sale con los contadores que la app ya calcula. La respuesta puede usar esos contadores y no añade otras cifras. El hilo dura solo mientras esa pantalla sigue abierta.

**Confirmado — Chat-D1.** Hay un hilo: la persona escribe, la app envía el texto con los contadores y la respuesta aparece como mensaje.

**Confirmado — Chat-D8.** La app no llama a Ollama. Llama a un endpoint nuevo de chat en `bloom-api`, distinto de `POST /api/recommendations`. Cada envío lleva los contadores, el idioma de la app y los mensajes de la visita. La respuesta útil es un solo texto del asistente. El teléfono conserva el hilo en memoria durante la visita.

## Situación actual

Bloom guarda los hábitos de la cuenta y sus registros. Leerlos no exige internet. Crear, editar, completar o borrar un hábito sigue exigiendo red.

Estadísticas abre una pantalla de recomendación. Muestra, por hábito, el nombre, los días completados y el objetivo de la semana lunes–domingo que contiene hoy, y un botón que pide un único texto. No hay campo para escribir ni hilo. Pedir otra vez sustituye el texto anterior. Sin hábitos, esa pantalla no llama a la API.

`bloom-api` está en `C:\Users\marta\apis\bloom-api`. Hoy expone `GET /health` y `POST /api/recommendations`. El cuerpo de recomendaciones es una lista `habits` con `name`, `completed_days` y `total_days`. La respuesta útil es `recommendations`. Una lista vacía recibe 400. Si los días completados superan el total, también. La API espera a Ollama como máximo 90 segundos, con el modelo `qwen2.5-coder:7b`, en `{OLLAMA_URL}/api/chat` y con `stream: false`. No tiene autenticación. La dirección base de la app es configuración de entorno: en desarrollo, `http://10.0.2.2:8000` en el emulador estándar y `http://<IP privada del ordenador>:8000` en un móvil de la misma Wi-Fi.

Esas reglas de contadores, de dirección y de acceso a Ollama se mantienen. Esta revisión no cambia rachas, metas, calendarios ni gráficos.

## Dentro del alcance

- **RF-01:** Quien haya iniciado sesión y tenga hábitos abre desde Estadísticas un hilo vacío y puede escribir un mensaje.
- **RF-02:** Cada envío lleva los contadores actuales de la semana lunes–domingo que contiene hoy, el idioma de la app y los mensajes de esta visita, en orden. La respuesta es un mensaje del asistente.
- **RF-03:** La respuesta puede usar los contadores enviados y no introduce otras cifras. Esta pantalla no presenta cifras como dato calculado por la app.
- **RF-04:** El texto del asistente llega en el idioma de la app, inglés o español. Los rótulos fijos salen de los recursos de ese idioma.
- **RF-05:** Sin hábitos no hay llamada. La pantalla explica que hace falta un hábito y no permite enviar.
- **RF-06:** Un fallo, un tiempo de espera agotado o una respuesta vacía no añade un mensaje del asistente. El mensaje de la persona permanece y se puede reintentar ese envío sin duplicarlo.
- **RF-07:** Mientras llega la respuesta, la pantalla sigue usable y no sale otra llamada.
- **RF-08:** El hilo y el texto aún no enviado duran mientras la pantalla sigue abierta, también si se recrea. Volver a Estadísticas o terminarse el proceso deja la siguiente apertura vacía. No se publica una notificación.

## Fuera de alcance

Estas exclusiones salen del pedido o de acuerdos anteriores que esta revisión no reabre:

- Respuestas en streaming. Cada respuesta llega entera.
- Guardar el hilo en el dispositivo o en `bloom-api` después de la visita.
- Mostrar los contadores como un bloque propio de la pantalla.
- Enviar un mensaje sin hábitos.
- Quitar `POST /api/recommendations` de `bloom-api`.
- Incluir amigos, retos o protecciones de racha en el envío. El acuerdo anterior los dejó fuera y esta revisión no los añade.
- Cambiar el cálculo de rachas, metas, calendarios o gráficos.
- Cambiar el modelo `qwen2.5-coder:7b` o hacer que la app llame a Ollama.

## Flujo de usuario

1. La persona abre la pantalla desde Estadísticas. Con hábitos, ve el hilo vacío y puede escribir. Todavía no hay llamada.
2. Sin hábitos, ve la explicación y no puede enviar.
3. Escribe y envía. Su mensaje aparece en el hilo y ve que la respuesta está en curso.
4. Si la respuesta sirve, aparece debajo como mensaje del asistente, en el idioma de la app.
5. Otro mensaje continúa el mismo hilo. El envío incluye los mensajes anteriores y los contadores actuales.
6. Si falla, el mensaje de la persona sigue en el hilo, no aparece una respuesta del asistente y puede reintentar ese envío.
7. Volver atrás abandona la petición de esa visita. La próxima apertura empieza vacía.

## Datos y reglas de negocio

Los contadores salen de los hábitos y registros ya guardados. No se crean cifras nuevas.

Por cada hábito se envían el nombre, los días completados y el objetivo semanal. El nombre va sin espacios sobrantes; un nombre vacío no se envía. Si después de eso no queda ningún hábito, se trata como el caso sin hábitos.

**Heredado de la spec del 2026-10-28.** El periodo es la semana lunes–domingo que contiene hoy.

- El objetivo semanal es el que Bloom ya usa: si el hábito pide menos de 7 días, ese número; si pide 7 y tiene entre 1 y 6 días configurados, esos días; en otro caso, 7.
- Los días completados son los días distintos de esa semana con un registro completado.

La app envía esos valores reales y no los recorta. Si los días completados superan el objetivo, el servicio rechaza el envío y la pantalla muestra el error recuperable. Cada envío usa los contadores de ese momento.

El idioma enviado es el de la app, inglés o español. Los mensajes van en el orden de la visita: lo escrito por la persona y lo respondido por el asistente. No se envían correos, contraseñas ni tokens.

Un texto vacío o formado solo por espacios no se envía y no llama al servicio. Reintentar un envío fallido no añade otra copia del mismo mensaje de la persona.

El hilo no se guarda. Salir de la pantalla lo abandona.

## Comportamiento mobile y casos alternativos

| Situación | Comportamiento esperado |
| --- | --- |
| Carga o acción en curso | Se indica que la respuesta está en curso. No sale otra llamada hasta que termine o falle. El resto de la app sigue respondiendo. |
| Sin datos | Sin hábitos, o si ninguno tiene nombre, hay explicación y ninguna llamada. Con hábitos y sin mensajes, el hilo está vacío y se puede escribir. |
| Entrada inválida | Un texto vacío o solo con espacios no se envía. |
| Error o espera excesiva | Error visible y reintento de ese envío. No aparece un mensaje vacío ni inventado del asistente. El servicio espera como máximo 90 segundos. |
| Sin conexión o conexión interrumpida | Leer los hábitos no exige internet. Si `bloom-api` no responde o se corta el envío, el resultado es el error recuperable. No se muestra el aviso de «sin internet» que usa guardar un hábito. |
| Cancelar o volver atrás | Salir abandona la petición de esta visita y no publica una notificación. La próxima apertura está vacía. |
| Pasar a segundo plano y regresar | Si el proceso sigue vivo, el hilo permanece. Si la petición termina fuera de la pantalla, al volver se ve la respuesta o el error. No se lanza otra petición sola. |
| Recrear la pantalla | Se conservan los mensajes, el texto aún no enviado y el envío en curso, su respuesta o su error. |
| Reabrir después de terminarse el proceso | No queda hilo ni borrador. Hay que escribir de nuevo. |
| Pulsaciones repetidas | Una sola llamada a la vez. |
| Texto largo, tamaño de letra, teclado y pantallas pequeñas | Los mensajes se pueden recorrer. El campo y la acción de enviar no quedan tapados por el teclado ni por las barras del sistema. |
| Accesibilidad | La carga y el error no dependen solo del color. Enviar, el campo y los mensajes tienen nombre para el lector de pantalla. |
| Idioma | Los rótulos y la respuesta del asistente siguen el inglés o el español de la app. |

**Puntos de la guía no aplicables y motivo:** permisos nuevos, porque no se pide ninguno; trabajo en segundo plano con notificación, porque el resultado no sale de la pantalla; enlaces externos, porque no forman parte del pedido; guardar el hilo, porque Chat-D3 lo limita a la visita.

## Restricciones del pedido

- Jetpack Compose y los hábitos ya guardados. Reutilizar el cálculo de contadores; no crear otro criterio de racha o de meta.
- La app llama al endpoint nuevo de chat en `bloom-api`. No llama a Ollama ni usa `POST /api/recommendations` para este hilo.
- El modelo sigue siendo `qwen2.5-coder:7b`. `bloom-api` llama a `{OLLAMA_URL}/api/chat` con `stream: false`. La espera máxima sigue siendo 90 segundos.
- La dirección base de `bloom-api` es configuración de entorno y no se versiona como secreto. En desarrollo la API se arranca con `python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload`. El emulador estándar usa `http://10.0.2.2:8000` y un móvil en la misma Wi-Fi usa `http://<IP privada del ordenador>:8000`.
- El hilo vive solo en la visita a esta pantalla.

## Criterios de aceptación

- **CA-01 · RF-01:** Dado un usuario con hábitos, cuando abre la pantalla desde Estadísticas, entonces ve un hilo vacío y puede escribir.
- **CA-02 · RF-02:** Dado un hilo con una pregunta y una respuesta, cuando envía otra pregunta, entonces la llamada lleva los contadores actuales, el idioma de la app y los tres mensajes en orden, y la nueva respuesta aparece al final.
- **CA-03 · RF-03:** Dado un resultado correcto, cuando se mira la pantalla, entonces no hay un bloque de cifras de la app y el mensaje del asistente no afirma una cifra que no estuviera entre los contadores enviados.
- **CA-04 · RF-04:** Dada la app en inglés, cuando llega la respuesta, entonces ese texto está en inglés. Dada la app en español, cuando llega, entonces está en español. Los rótulos coinciden con ese idioma.
- **CA-05 · RF-05:** Dado un usuario sin hábitos, cuando abre la pantalla, entonces ve la explicación y no hay llamada.
- **CA-06 · RF-06:** Dado que el envío falla, se agota la espera o la respuesta llega vacía, cuando termina, entonces su mensaje sigue en el hilo, no hay mensaje del asistente y puede reintentar sin duplicar el suyo.
- **CA-07 · RF-07:** Dado un envío en curso, cuando intenta enviar otra vez, entonces no sale una segunda llamada.
- **CA-08 · RF-08:** Dado un hilo visible, cuando gira el teléfono, entonces los mensajes y el borrador siguen. Cuando vuelve a Estadísticas y abre de nuevo, o cuando el proceso ha terminado, entonces el hilo está vacío.
- **CA-09 · RF-02:** Dado el emulador estándar, cuando envía un mensaje, entonces llama a `http://10.0.2.2:8000` en el endpoint nuevo de chat. Dado un móvil en la misma Wi-Fi, cuando envía, entonces llama a `http://<IP privada del ordenador>:8000` en ese mismo endpoint. La app no abre una conexión con Ollama.

## Cómo se comprueba el comportamiento

<!-- Una fila por criterio: escenario y resultado que debemos comprobar.
La selección de tests, herramientas, comandos y evidencias se desarrolla en PLAN.md.
No marques los criterios como superados durante la especificación. -->

| Criterio | Condiciones y pasos | Resultado esperado |
| --- | --- | --- |
| CA-01 | Cuenta con hábitos. Abrir la pantalla desde Estadísticas. | Hilo vacío y se puede escribir. Ninguna llamada todavía. |
| CA-02 | Enviar una pregunta, recibir respuesta y enviar otra. Observar la segunda llamada. | Lleva los contadores de ese momento, el idioma y los mensajes previos más el nuevo, en orden. La respuesta nueva queda al final. |
| CA-03 | Respuesta válida. Revisar el mensaje del asistente frente a los contadores enviados. | No hay un bloque de cifras. El mensaje no afirma una cifra ausente de esos contadores. |
| CA-04 | Abrir la app en inglés y en español y recibir una respuesta en cada idioma. | Rótulos y respuesta coinciden con el idioma de la app. |
| CA-05 | Cuenta sin hábitos. Abrir la pantalla. | Explicación y ninguna llamada. |
| CA-06 | `bloom-api` apagada, respuesta de error, espera agotada o cuerpo sin texto. Reintentar. | El mensaje de la persona sigue una sola vez, no hay respuesta del asistente y el reintento vuelve a enviar ese mensaje. |
| CA-07 | Intentar enviar otra vez durante la espera. | Una sola llamada. |
| CA-08 | Con mensajes y texto sin enviar, girar el teléfono. Salir a Estadísticas y volver a entrar. Repetir tras terminar el proceso. | El giro conserva hilo y borrador. Las otras dos aperturas están vacías. |
| CA-09 | Enviar un mensaje en el emulador estándar y en un móvil de la misma Wi-Fi. | El emulador llama a `http://10.0.2.2:8000`. El móvil llama a `http://<IP privada>:8000`. No hay conexión de la app con Ollama. |

Ningún criterio de esta revisión está superado: el chat no está implementado.

## Decisiones pendientes

<!-- Al resolverlas, actualiza las secciones afectadas. Escribe Ninguna cuando
no queden pendientes funcionales ni restricciones por decidir. -->

Ninguna.

Cerradas en esta revisión: Chat-D1, hilo con mensaje escrito por la persona; Chat-D2, cada envío incluye los mensajes anteriores y los contadores actuales; Chat-D3, el hilo dura solo con la pantalla abierta; Chat-D4, la pantalla es el hilo y el campo, sin bloque de contadores; Chat-D5, sin hábitos no se envía; Chat-D6, la respuesta puede usar los contadores enviados y no otras cifras; Chat-D7, la respuesta usa el idioma de la app; Chat-D8, endpoint nuevo de chat en `bloom-api`, con el hilo en memoria durante la visita.

Se mantienen de la spec del 2026-10-28: semana lunes–domingo actual, contadores reales sin recortar, dirección base por entorno, modelo `qwen2.5-coder:7b`, espera máxima de 90 segundos y la app sin llamada directa a Ollama.

<!-- ANTES DE SOLICITAR APROBACIÓN
Comprueba que el alcance está acordado, los flujos son coherentes, los puntos
mobile relevantes están cubiertos y cada requisito tiene criterios comprobables.
Resuelve las dudas y los marcadores pendientes. Mantén el diseño técnico en PLAN.md.
-->
