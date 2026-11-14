# SPEC: Proteger la API de Bloom con la sesión del usuario

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

<!-- Qué necesidad resolvemos, quién tiene esa necesidad y qué podrá hacer.
Describe el objetivo en lenguaje de producto. -->

El chat de Bloom (pantalla de recomendaciones) llama a una API propia que a su vez usa Groq. Hoy esa API es pública: cualquiera con la URL puede usarla y gastar la cuota de Groq de la propietaria. Queremos que solo la puedan usar personas con sesión iniciada en la app, y poner un tope al gasto en AWS, sin cambiar la experiencia de quien usa la app con normalidad.

## Situación actual

<!-- Comportamiento actual relevante, limitación que queremos resolver y
comportamientos existentes que deben conservarse. No describas la arquitectura. -->

- La API (`bloom-api`) acepta `POST /api/chat` sin ninguna identificación. Se está migrando de Render a AWS Lambda con una URL de función pública.
- La URL va dentro del APK publicado, así que es fácil de obtener.
- La app solo deja usar Bloom con sesión iniciada en Supabase.
- Se conserva: el formato de petición y respuesta del chat, los mensajes de error actuales, `/health` público y el uso local con `uvicorn`.

## Dentro del alcance

<!-- Requisitos concretos, con identificadores estables para vincularlos a
criterios, decisiones del plan y tareas. -->

- **RF-01:** La API rechaza `POST /api/chat` con 401 si la petición no trae un token de sesión de Supabase válido, vigente y emitido por el proyecto de la app. En ese caso no llama a Groq.
- **RF-02:** La app envía el token de la sesión actual en cada petición al chat de Bloom.
- **RF-03:** Si la API responde 401, la app refresca la sesión y reintenta una sola vez. Si vuelve a fallar, muestra un mensaje que pide volver a iniciar sesión.
- **RF-04:** Si no hay sesión al enviar un mensaje, la app no hace la petición y muestra ese mismo mensaje.
- **RF-05:** `/health` sigue siendo público y no requiere token.
- **RF-06:** El despliegue en AWS limita el gasto: un tope de ejecuciones simultáneas de la función y una alerta de presupuesto mensual que avisa por correo.
- **RF-07:** El token y la clave de Groq no aparecen nunca en logs de la app ni de la API.

## Fuera de alcance

<!-- Exclusiones acordadas, no deducidas por el agente. Si no hay exclusiones
adicionales, indícalo tras revisarlo con la persona. -->

- Límite de mensajes por usuario (se hará después).
- Play Integrity / Firebase App Check.
- Cerrar la sesión de la app automáticamente al recibir un 401.
- Cambios en el contenido de las respuestas de Bloom o en la interfaz del chat, salvo el nuevo mensaje de error.
- Despliegue automático de `bloom-api` desde GitHub Actions.

## Flujo de usuario

<!-- Cómo se inicia, qué hace el usuario y qué resultado obtiene.
Incluye pantallas afectadas, navegación y alternativas relevantes. -->

1. La persona, con sesión iniciada, abre el chat de Bloom y escribe un mensaje.
2. La app envía el mensaje junto con su token de sesión y recibe la respuesta como hasta ahora.
3. Si el token había caducado, la app lo renueva y reintenta sin que la persona note nada.
4. Si la sesión no se puede renovar, el chat muestra el error de sesión. La persona puede ir a Perfil, cerrar sesión y volver a entrar.

## Datos y reglas de negocio

<!-- Información que necesita el usuario, campos obligatorios, validaciones,
límites y reglas como duplicados u orden de presentación. Describe significado
y comportamiento, sin diseñar tablas, DTO, DAO ni almacenamiento. -->

- Solo valen los tokens de usuario autenticado de Supabase del proyecto de la app. No valen la clave publicable, tokens de otro proyecto, tokens caducados ni tokens anónimos.
- La API no guarda tokens ni datos del usuario.
- Tope de ejecuciones simultáneas: 5. Alerta de presupuesto: 5 USD/mes. Ambos se pueden cambiar al desplegar.

## Comportamiento mobile y casos alternativos

<!-- Adapta la tabla usando MOBILE_GUIDELINES.md. Añade escenarios relevantes.
Marca No aplica con su motivo cuando corresponda. No presupongas soporte offline
ni conservación de todo el estado. Expresa resultados, no mecanismos técnicos. -->

| Situación | Comportamiento esperado |
| --- | --- |
| Carga o acción en curso | Igual que hoy. El posible reintento tras refrescar la sesión ocurre dentro de la misma espera. |
| Sin datos | No aplica: no cambia. |
| Entrada inválida | Igual que hoy (400/422 → mensaje actual). |
| Error o espera excesiva | Igual que hoy. Nuevo: sesión no válida tras reintentar → mensaje de sesión. |
| Sin conexión o conexión interrumpida | Igual que hoy. No refrescar la sesión sin red no debe mostrar el error de sesión, sino el de no disponible. |
| Cancelar o volver atrás | Igual que hoy. |
| Pasar a segundo plano y regresar | Igual que hoy. Si el token caducó en segundo plano, se renueva al enviar (RF-03). |
| Recrear la pantalla | Igual que hoy. |
| Reabrir después de terminarse el proceso | La sesión guardada se carga antes de enviar. Si no hay sesión, se aplica RF-04. |
| Privacidad y seguridad | RF-07. Peticiones solo por HTTPS. |
| Idiomas | El nuevo mensaje existe en inglés y español. |

**Puntos de la guía no aplicables y motivo:** permisos, trabajo en segundo plano, adaptación visual y accesibilidad. No cambia la interfaz ni el uso del dispositivo; el nuevo error usa el componente que ya existe.

## Restricciones del pedido

<!-- Condiciones ya impuestas: compatibilidad, límites de alcance, requisitos
de accesibilidad o rendimiento medibles, o una tecnología expresamente exigida.
Ejemplo: Usar Room puede ser una restricción; el diseño de entidades va en PLAN.md.
No conviertas una preferencia del agente en una restricción. -->

- La autenticación usa la sesión de Supabase que ya tiene la app.
- La API corre en AWS Lambda (migración en curso).
- No se añaden secretos nuevos al APK.

## Criterios de aceptación

<!-- Resultados observables que permitan decidir si se cumple cada requisito.
Incluye los casos alternativos acordados. No uses Funciona correctamente.
Repite el formato según sea necesario. -->

- **CA-01 · RF-01:** Dada la API desplegada, cuando se llama a `/api/chat` sin cabecera de autorización, con un token mal formado, firmado por otro emisor o caducado, entonces responde 401 y no se llama a Groq.
- **CA-02 · RF-01:** Dado un token válido de un usuario del proyecto, cuando se llama a `/api/chat`, entonces la respuesta es la misma que antes del cambio.
- **CA-03 · RF-02:** Dada una sesión iniciada, cuando la app envía un mensaje a Bloom, entonces la petición incluye el token de esa sesión.
- **CA-04 · RF-03:** Dada una primera respuesta 401, cuando el refresco de sesión funciona, entonces la app reintenta una vez y muestra la respuesta.
- **CA-05 · RF-03:** Dado que el reintento vuelve a dar 401, entonces el chat muestra el mensaje de sesión y no hay más reintentos.
- **CA-06 · RF-04:** Sin sesión, cuando se envía un mensaje, entonces no se hace ninguna petición y se muestra el mensaje de sesión.
- **CA-07 · RF-05:** Cuando se llama a `/health` sin token, entonces responde 200.
- **CA-08 · RF-06:** Tras desplegar, la función tiene un tope de ejecuciones simultáneas configurado y existe una alerta de presupuesto con correo.
- **CA-09 · RF-07:** En los logs de la app y de la API no aparece el token ni la clave de Groq en ninguno de los casos anteriores.

## Cómo se comprueba el comportamiento

<!-- Una fila por criterio: escenario y resultado que debemos comprobar.
La selección de tests, herramientas, comandos y evidencias se desarrolla en PLAN.md.
No marques los criterios como superados durante la especificación. -->

| Criterio | Condiciones y pasos | Resultado esperado |
| --- | --- | --- |
| CA-01 | Llamar a la API sin token / con token inválido / caducado / de otro emisor | 401; Groq no recibe llamada |
| CA-02 | Llamar con un token válido | 200 y respuesta de Bloom |
| CA-03 | Enviar un mensaje desde la app con sesión | La petición lleva el token |
| CA-04 | Simular 401 y luego éxito | Un reintento, respuesta mostrada |
| CA-05 | Simular 401 dos veces | Mensaje de sesión, sin más peticiones |
| CA-06 | Enviar sin sesión | Sin petición, mensaje de sesión |
| CA-07 | `GET /health` sin token | 200 |
| CA-08 | Revisar el despliegue en AWS | Tope de concurrencia y alerta de presupuesto presentes |
| CA-09 | Revisar los logs de los casos anteriores | No aparecen secretos |

## Decisiones pendientes

<!-- Al resolverlas, actualiza las secciones afectadas. Escribe Ninguna cuando
no queden pendientes funcionales ni restricciones por decidir. -->

Ninguna. Resueltas: alerta de 5 USD/mes y tope de 5 ejecuciones simultáneas. Si la cuenta AWS no permite reservar concurrencia, se deja solo la alerta y se documenta.
