# SPEC: Recomendación de hábitos con Ollama

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

## Qué construimos y para quién

La persona que usa Bloom puede pedir **una** recomendación sobre sus hábitos. La app calcula antes los números con los datos que ya tiene guardados y se los enseña como hechos. El texto del modelo solo es una sugerencia y no añade cifras.

**Confirmado por el pedido:** no es un chat. No hay hilo de mensajes, ni preguntas de seguimiento, ni un modelo que decida las estadísticas. La app Android no llama a Ollama. Llama a la API intermedia que ya existe. Esa API es quien habla con Ollama.

**Alcance que permite la API actual.** `bloom-api` es una aplicación FastAPI. Quien puede usarla es cualquier cliente que alcance el proceso mientras está en marcha. El código no fija el host ni el puerto, no tiene autenticación y no trae una dirección pública. Ollama lo llama la propia API, por defecto en `http://127.0.0.1:11434`, configurable con `OLLAMA_URL`. Eso encaja con Ollama instalado en el mismo ordenador que `bloom-api`. No convierte el asistente en una función disponible para cualquier instalación de Bloom si ese proceso no es alcanzable desde esos teléfonos.

## Situación actual

Bloom ya guarda los hábitos de la cuenta y sus registros de cumplimiento, y los lee en local aunque no haya internet para modificarlos. Las mutaciones de hábitos siguen exigiendo red.

Hoy no hay asistente en la app Android. El acceso remoto de Bloom sigue siendo Supabase. La API intermedia está fuera de este repositorio, en `C:\Users\marta\bloom-api`, y solo contiene `main.py`. Estadísticas, perfil, la meta semanal y el detalle ya muestran cifras calculadas por la app, entre ellas:

- cuántos hábitos hay y cuántos registros están completados;
- la racha de días programados cumplidos;
- la tarjeta de racha de Estadísticas, si llega al mínimo actual de 3 días;
- la meta de 5 días en la semana lunes–domingo que contiene hoy;
- el resumen de esa semana, con programados y completados por día;
- el objetivo semanal de cada hábito y los días ya cumplidos en esa semana.

Esas reglas siguen igual. Esta función no las cambia ni las sustituye.

## Dentro del alcance

- **RF-01:** Quien haya iniciado sesión puede pedir una recomendación a partir de sus hábitos guardados.
- **RF-02:** Las cifras visibles las calcula la app con las reglas ya existentes. El texto del modelo no es una fuente de cifras.
- **RF-03:** La app pide la recomendación con `POST /api/recommendations` a `bloom-api`. No llama a Ollama. El cuerpo es una lista `habits` con `name`, `completed_days` y `total_days`. La respuesta útil es `recommendations`, un texto.
- **RF-04:** Si no hay hábitos, no se llama a la API y se muestra el estado vacío. La API también rechaza una lista vacía.
- **RF-05:** Un fallo, un tiempo de espera agotado o una respuesta vacía no muestra una recomendación. Se puede reintentar.
- **RF-06:** Mientras llega la respuesta, la interfaz sigue usable y un segundo toque no lanza otra petición.
- **RF-07:** Los textos fijos de la pantalla salen de recursos, en el inglés y el español que ya tiene la app. El texto generado llega en español, porque así lo pide el prompt de `bloom-api`.
- **RF-08:** La dirección base de `bloom-api` se configura por entorno y no forma parte de la lógica que calcula o presenta la recomendación. En desarrollo, el emulador estándar usa `http://10.0.2.2:8000` y un móvil en la misma Wi-Fi usa `http://<IP privada del ordenador>:8000`.

## Fuera de alcance

Estas exclusiones salen del pedido. No son una lista cerrada de producto:

- Un chatbot, un historial de conversación o respuestas en streaming.
- Que el modelo invente o corrija estadísticas.
- Cambiar el cálculo de rachas, metas, calendarios o gráficos que ya existen.

Amigos, retos y protecciones de racha no forman parte del contrato actual: `bloom-api` solo acepta, por hábito, el nombre y dos contadores. Incluirlos exigiría cambiar esa API. No se propone ese cambio.

## Flujo de usuario

**Confirmado — D3.** Un botón en Estadísticas abre una pantalla nueva con los contadores y el texto. La pestaña Estadísticas se queda como está.

1. La persona abre esa pantalla y ve la acción para pedir la recomendación. Todavía no hay sugerencia.
2. Si no tiene hábitos, ve el estado vacío y no puede pedirla.
3. Si tiene hábitos, pide la recomendación. Ve que está en curso y las cifras que la app ya calculó.
4. Si la respuesta sirve, ve esas mismas cifras y, aparte, el texto `recommendations`. Ese texto pide entre tres y cinco recomendaciones y va marcado como sugerencia.
5. Pedir otra vez sustituye la sugerencia anterior. No acumula mensajes.
6. Si falla, ve el error y puede reintentar. Las cifras calculadas pueden seguir visibles.

## Datos y reglas de negocio

Los hechos salen de los hábitos y registros ya guardados de la cuenta. No se crean cifras nuevas para esta pantalla.

La API acepta y la pantalla muestra, por cada hábito, el nombre, `completed_days` y `total_days`. El nombre es obligatorio, de 1 a 100 caracteres. Los dos contadores son enteros de cero o más, y los días completados no pueden superar el total. No se envían correos, contraseñas ni tokens.

**Confirmado — D5.** El periodo es la semana lunes–domingo que contiene hoy.

- `total_days` es el objetivo semanal del hábito, el mismo que ya usa Bloom: si pide menos de 7 días, ese número; si no, los días de la semana configurados cuando son entre 1 y 6; en otro caso, 7.
- `completed_days` es cuántos días distintos de esa semana tienen un registro completado.

La app envía esos valores reales. No los recorta para que quepan en el objetivo. Si un hábito tiene más días completados que su objetivo, `bloom-api` responde 400 y la pantalla muestra el error recuperable. Rachas, la meta de 5 días y los resúmenes por día no entran en este cuerpo.

La sugerencia se presenta separada de las cifras. Una cifra que solo aparezca en el párrafo del modelo no se muestra como dato de la app.

**Confirmado — D7.** La sugerencia no se guarda. Volver atrás abandona la petición de esa visita y no publica una notificación. Si el sistema termina el proceso, la pantalla vuelve a estar en reposo y hay que pedirla otra vez. Mientras la pantalla sigue abierta y el proceso vive, se conserva el estado de esa visita.

## Comportamiento mobile y casos alternativos

| Situación | Comportamiento esperado |
| --- | --- |
| Carga o acción en curso | Se indica que la recomendación está en curso. La acción no se puede repetir hasta que termine o falle. El resto de la app sigue respondiendo. |
| Sin datos | Sin hábitos no hay llamada a `bloom-api` ni sugerencia. |
| Entrada inválida | No aplica: no hay formulario. La persona solo pide la recomendación. |
| Error o espera excesiva | Mensaje de error y opción de reintentar. No aparece una sugerencia vacía ni inventada. `bloom-api` espera a Ollama como máximo 90 segundos y responde 504 si se agota, 502 si Ollama falla o devuelve un texto vacío, y 400 si la lista no es válida. |
| Sin conexión o conexión interrumpida | Leer los hábitos no exige internet. Si la API intermedia no responde o se corta a mitad, el resultado es el error recuperable. No se equipara, sin decidirlo, al aviso de «sin internet» de guardar un hábito. |
| Cancelar o volver atrás | Salir de la pantalla abandona la petición de esta visita y no publica la sugerencia en una notificación. |
| Pasar a segundo plano y regresar | Si la petición termina mientras la pantalla no está delante, al volver se ve el resultado o el error de esa visita. No se lanza una segunda petición sola. |
| Recrear la pantalla | Se conserva el estado de esta visita: en curso, resultado o error. |
| Reabrir después de terminarse el proceso | No queda una sugerencia guardada. La pantalla vuelve a estar en reposo. |
| Pulsaciones repetidas | Una sola petición a la vez. |
| Texto largo, tamaño de letra y pantallas pequeñas | Las cifras y la sugerencia se pueden desplazar. No quedan tapadas por las barras del sistema ni por el teclado, porque no hay campo de texto. |
| Accesibilidad | El estado de carga y el de error no dependen solo del color. La acción y el estado tienen nombre para el lector de pantalla. |
| Idioma | Los rótulos siguen el idioma de la app (inglés y español). El texto generado llega en español. |

**Puntos de la guía no aplicables y motivo:** permisos nuevos, porque no se pide ninguno; trabajo en segundo plano con notificación, porque la propuesta no publica el resultado fuera de la pantalla; enlaces externos, porque no forman parte del pedido.

## Restricciones del pedido

- Jetpack Compose y los hábitos ya guardados en Room. Reutilizar los cálculos existentes; no crear otro criterio de racha o de meta.
- La app usa la API intermedia existente. No se crea otra ni se sustituye sin una justificación técnica y tu aprobación.
- El modelo sigue siendo `qwen2.5-coder:7b`. `bloom-api` llama a `{OLLAMA_URL}/api/chat` con `stream: false`. La app no hace esa llamada.
- **Confirmado — D9.** La app no llama a Ollama. La dirección base de `bloom-api` es configuración de entorno, separada de la recomendación, y no se versiona como secreto. Durante el desarrollo la API se arranca con `python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload`. Esas direcciones de desarrollo son HTTP.
- El modelo `qwen2.5-coder:7b` está elegido aunque esté orientado a código. Cambiarlo queda fuera de este pedido.
- No se añade un chatbot ni una dependencia nueva salvo que el plan posterior demuestre que la pila actual no puede hacer esta llamada.

## Criterios de aceptación

- **CA-01 · RF-01:** Dado un usuario con hábitos guardados, cuando pide la recomendación, entonces ve un solo texto de respuesta, sin hilo de chat.
- **CA-02 · RF-02:** Dado un conjunto conocido de hábitos y registros, cuando se muestra el resultado, entonces cada hábito muestra el nombre, los días completados de la semana lunes–domingo actual y su objetivo semanal, calculados por la app, y no cifras añadidas por el modelo.
- **CA-03 · RF-03:** Dada `bloom-api` en marcha, cuando la app pide la recomendación, entonces hace `POST /api/recommendations` y no llama a `/api/chat`.
- **CA-04 · RF-04:** Dado un usuario sin hábitos, cuando abre la pantalla, entonces no hay llamada al modelo y ve el estado vacío.
- **CA-05 · RF-05:** Dado que la API intermedia no responde, tarda más del límite o devuelve un cuerpo vacío, cuando termina la espera, entonces ve un error con reintento y ninguna sugerencia.
- **CA-06 · RF-06:** Dado que una petición está en curso, cuando la persona pulsa otra vez, entonces no sale una segunda llamada.
- **CA-07 · RF-02:** Dado un resultado correcto, cuando se mira la pantalla, entonces las cifras y la sugerencia están separadas y la sugerencia no se presenta como un dato calculado.
- **CA-08 · RF-07:** Dado el idioma inglés o español del dispositivo, cuando se ve la pantalla, entonces los rótulos fijos salen de los recursos de ese idioma.
- **CA-09 · RF-08:** Dado el emulador estándar, cuando la app pide la recomendación, entonces llama a `http://10.0.2.2:8000`. Dado un móvil en la misma Wi-Fi, cuando pide la recomendación, entonces llama a `http://<IP privada del ordenador>:8000`. Cambiar esa dirección no exige cambiar el cálculo de los contadores.

## Cómo se comprueba el comportamiento

<!-- Una fila por criterio: escenario y resultado que debemos comprobar.
La selección de tests, herramientas, comandos y evidencias se desarrolla en PLAN.md.
No marques los criterios como superados durante la especificación. -->

| Criterio | Condiciones y pasos | Resultado esperado |
| --- | --- | --- |
| CA-01 | Cuenta con hábitos. Pedir una recomendación. | Un texto `recommendations`, sin hilo de chat. |
| CA-02 | Hábitos y registros fijados de antemano. Comparar nombre, días completados de la semana actual y objetivo semanal. | Coinciden con el cálculo ya existente. El texto del modelo no crea otra cifra oficial. |
| CA-03 | `bloom-api` en marcha. Pedir la recomendación y observar la petición de la app. | `POST /api/recommendations`. No aparece una conexión de la app a Ollama. |
| CA-04 | Cuenta sin hábitos. Abrir la pantalla. | Estado vacío y ninguna petición. |
| CA-05 | `bloom-api` apagada, respuesta 502 o 504, o cuerpo sin texto. | Error, reintento y ninguna sugerencia. |
| CA-06 | Pulsar repetidamente durante la carga. | Una sola petición. |
| CA-07 | Respuesta válida del modelo, aunque mencione un número que no estaba en el resumen. | Ese número no aparece como estadística de la app. |
| CA-08 | Abrir la pantalla en inglés y en español. | Los rótulos cambian con el idioma. El texto generado sigue en español. |
| CA-09 | Pedir la recomendación en el emulador estándar y en un móvil de la misma Wi-Fi. | El emulador llama a `http://10.0.2.2:8000`. El móvil llama a `http://<IP privada>:8000`. El cálculo de los contadores no depende de esa dirección. |

Ningún criterio está superado: no hay implementación.

## Decisiones pendientes

<!-- Al resolverlas, actualiza las secciones afectadas. Escribe Ninguna cuando
no queden pendientes funcionales ni restricciones por decidir. -->

Ninguna.

Cerradas: D1 queda descrito por el alcance de `bloom-api`; D2, amigos y retos no entran en el contrato; D3, botón en Estadísticas y pantalla nueva; D4, el texto generado es español; D5, semana lunes–domingo actual, días completados frente al objetivo semanal; D6, el nombre del hábito se envía; D7, la sugerencia no se guarda; D8, la API espera como máximo 90 segundos; D9, dirección base configurable por entorno, con `http://10.0.2.2:8000` en el emulador y `http://<IP privada>:8000` en un móvil de la misma Wi-Fi; D10, la API está localizada.

<!-- ANTES DE SOLICITAR APROBACIÓN
Comprueba que el alcance está acordado, los flujos son coherentes, los puntos
mobile relevantes están cubiertos y cada requisito tiene criterios comprobables.
Resuelve las dudas y los marcadores pendientes. Mantén el diseño técnico en PLAN.md.
-->
