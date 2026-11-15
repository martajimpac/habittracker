# SPEC: Órdenes en el chat de Bloom: crear hábitos y marcar días

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

Quien usa Bloom quiere dar órdenes al asistente, no solo pedirle consejo. Con esta versión podrá escribir en el chat cosas como «crea un hábito de leer 20 minutos cada noche» o «marca leer como hecho ayer». Bloom propondrá la acción en una tarjeta dentro de la conversación, y la acción se hará cuando la persona la confirme. También podrá crear así su primer hábito, aunque todavía no tenga ninguno.

## Situación actual

<!-- Comportamiento actual relevante, limitación que queremos resolver y
comportamientos existentes que deben conservarse. No describas la arquitectura. -->

- **Chat de Bloom:** la persona escribe y Bloom responde solo con texto. Bloom recibe los recuentos de la semana de cada hábito (nombre, días hechos y días totales) y da consejos. No puede crear, marcar, cambiar ni borrar nada.
- **Sin hábitos:** el chat no deja escribir. Muestra «No habits yet / Create a habit before sending a message.». La API también rechaza una petición sin hábitos.
- **Crear un hábito:** hoy solo se puede desde el formulario Crear hábito.
- **Marcar un día:** hoy solo se puede desde Inicio, para hoy y los días ya pasados de la semana actual (de lunes a domingo), y solo en los hábitos que aparecen ese día. Los días futuros no se pueden marcar.
- Crear y marcar son online-first: sin red no se guarda nada y se muestra el error.
- **Se conserva:**
  - las respuestas de consejo de Bloom, su tono y sus reglas (no juzgar, no diagnosticar, no inventar cifras);
  - el idioma de la respuesta (inglés o español, según el del móvil);
  - la sesión obligatoria, el reintento tras refrescarla y los mensajes de error actuales del chat;
  - el formulario Crear hábito y la forma de marcar en Inicio, sin cambios;
  - las reglas de creación (privado y recordatorio apagado por defecto) y el recálculo automático de rachas, protecciones, estadísticas y widgets al marcar.

## Dentro del alcance

<!-- Requisitos concretos, con identificadores estables para vincularlos a
criterios, decisiones del plan y tareas. -->

**Comunes a las dos órdenes**

- **RF-01:** Bloom solo propone una acción cuando la persona la pide. En el resto de mensajes responde como ahora, solo con texto.
- **RF-02:** Cada acción se propone con una tarjeta dentro de la conversación, con un botón para confirmar y otro para cancelar. Nada se guarda sin pulsar el botón de confirmar.
- **RF-03:** Al confirmar, la acción se guarda igual que desde la app, y la tarjeta pasa a mostrar que está hecha y ya no tiene botones.
- **RF-04:** Al cancelar, no se hace nada, y la tarjeta pasa a mostrar que se canceló y ya no tiene botones.
- **RF-05:** Mientras se guarda, los botones de la tarjeta no responden. Un segundo toque no repite la acción.
- **RF-06:** Si el guardado falla, no cambia nada, se muestra el error y la tarjeta conserva sus botones para reintentar.
- **RF-07:** Si Bloom propone una versión corregida, la tarjeta anterior conserva sus botones.
- **RF-08:** En los mensajes siguientes, Bloom sabe si cada propuesta anterior se hizo o se canceló.
- **RF-09:** Una sola propuesta por respuesta. Si la persona pide varias acciones a la vez, Bloom propone una y ofrece seguir con la siguiente.

**Crear un hábito**

- **RF-10:** Cuando la persona pide crear un hábito, la tarjeta muestra el nombre, la descripción (si la hay), el icono, el color y la frecuencia, con los botones Crear y Cancelar.
- **RF-11:** Lo que la persona no indica lo completa Bloom: elige un icono y un color de los que ofrece el formulario, y la frecuencia diaria. El recordatorio queda apagado y el hábito, privado.
- **RF-12:** Si la orden no deja claro qué hábito crear (por ejemplo, solo «crea un hábito»), Bloom pregunta cuál en vez de proponer uno.
- **RF-13:** Si la persona pide un recordatorio, Bloom propone el hábito sin él y le dice que puede activarlo desde Editar hábito.
- **RF-14:** El hábito creado aparece en Inicio, Estadísticas y los widgets.

**Marcar o desmarcar un día**

- **RF-15:** Cuando la persona pide marcar un hábito como hecho, o quitarlo, en un día, la tarjeta muestra el hábito (nombre e icono), el día y la acción («Marcar como hecho» o «Quitar hecho»), con un botón que confirma esa acción y Cancelar.
- **RF-16:** Solo se puede proponer para hoy o un día ya pasado de la semana actual, y en un hábito que aparece en Inicio ese día. Si la persona pide un día futuro, uno de otra semana o un día en que el hábito no toca, Bloom lo explica y no muestra tarjeta.
- **RF-17:** Bloom entiende días como «hoy», «ayer» o «el lunes» respecto a la semana actual.
- **RF-18:** Si el nombre no coincide con ningún hábito, o encaja con varios, Bloom pregunta cuál en vez de proponer.
- **RF-19:** Si el hábito ya está en el estado pedido ese día, Bloom lo dice y no muestra tarjeta.
- **RF-20:** Al confirmar, el día queda en el estado que indicaba la tarjeta, aunque entretanto se haya cambiado desde Inicio. Rachas, protecciones, estadísticas, Inicio y widgets se actualizan igual que al marcar desde Inicio.
- **RF-21:** Si el hábito se eliminó después de la propuesta, al confirmar se muestra un error y no cambia nada.

**Chat sin hábitos y documentación**

- **RF-22:** El chat se puede usar sin hábitos. En ese caso, el saludo invita a crear el primero desde el chat y Bloom responde sin recuentos semanales.
- **RF-23:** `spec.md` se actualiza con este comportamiento.

## Fuera de alcance

<!-- Exclusiones acordadas, no deducidas por el agente. Si no hay exclusiones
adicionales, indícalo tras revisarlo con la persona. -->

- Editar o eliminar hábitos desde el chat (decidido: solo crear, marcar y desmarcar).
- Marcar días de semanas anteriores o días futuros (decidido: solo la semana actual, hasta hoy).
- Ejecutar sin confirmación (decidido: siempre con tarjeta).
- Recordatorios desde el chat (decidido: no en esta versión).
- Abrir desde el chat el formulario Crear hábito relleno.
- Guardar la conversación: sigue sin guardarse, como ahora.
- Cambios en el formulario Crear hábito o en cómo se marca desde Inicio.

## Flujo de usuario

<!-- Cómo se inicia, qué hace el usuario y qué resultado obtiene.
Incluye pantallas afectadas, navegación y alternativas relevantes. -->

**Crear**

1. La persona abre el chat de Bloom, tenga o no hábitos, y escribe «crea un hábito de leer 20 minutos cada noche».
2. Bloom responde con un texto breve y una tarjeta: «Leer 20 minutos», icono de estudio, un color, diario.
3. Pulsa Crear. La tarjeta indica que se está guardando y, después, que el hábito está creado. El hábito ya aparece en Inicio.

**Marcar o desmarcar**

1. La persona escribe «ayer leí» o «marca leer como hecho ayer».
2. Bloom responde con una tarjeta: «Leer 20 minutos · ayer, jueves 12 · Marcar como hecho».
3. Pulsa Marcar como hecho. La tarjeta indica que se está guardando y después que está hecho. En Inicio, ayer aparece marcado y la racha se actualiza.
4. Para quitarlo: «al final no leí el lunes». La tarjeta dice «Quitar hecho» y funciona igual.

**Alternativas**

- **Cancelar:** pulsa Cancelar; la tarjeta queda como cancelada y no cambia nada.
- **Corregir:** pide un cambio («mejor de lunes a viernes», «no, era el martes»). Bloom propone una tarjeta nueva y la anterior conserva sus botones.
- **No se puede:** pide un día futuro o de la semana pasada; Bloom explica que solo puede hasta hoy dentro de esta semana.
- **Error:** sin red, al confirmar aparece el error y la tarjeta sigue con sus botones.

## Datos y reglas de negocio

<!-- Información que necesita el usuario, campos obligatorios, validaciones,
límites y reglas como duplicados u orden de presentación. Describe significado
y comportamiento, sin diseñar tablas, DTO, DAO ni almacenamiento. -->

- **Crear, campos que Bloom puede proponer:** nombre (obligatorio, máximo 100 caracteres, el límite que ya aplica la API), descripción opcional, icono y color (solo de los que ofrece el formulario) y frecuencia: diaria, días concretos (al menos uno) u objetivo semanal (de 1 a 7 veces). El recordatorio queda siempre apagado y el hábito, privado; se pueden cambiar después desde Editar hábito.
- **Crear, validación:** si la propuesta trae un icono o un color que el formulario no ofrece, la tarjeta usa los de por defecto. Si falta el nombre o la frecuencia no es válida (sin días, u objetivo fuera de 1–7), no se muestra tarjeta, solo el texto de Bloom.
- **Crear, nombres repetidos:** se permiten, igual que en el formulario.
- **Marcar, validación:** la app comprueba la propuesta antes de mostrar la tarjeta y otra vez al confirmar. Si el hábito no existe, el día es futuro, de otra semana o no toca ese día, no se muestra tarjeta (o, al confirmar, se muestra un error). La semana empieza el lunes y se usa la fecha del móvil.
- **Marcar, día en la tarjeta:** se muestra con el nombre del día y la fecha, en el idioma de la app.
- **Datos enviados a Bloom:** los mismos que ahora (nombres y recuentos semanales), qué días de la semana actual está hecho cada hábito y en qué días toca, la fecha de hoy y el resultado de las propuestas (hecha o cancelada). No se envían otros datos personales.

## Comportamiento mobile y casos alternativos

<!-- Adapta la tabla usando MOBILE_GUIDELINES.md. Añade escenarios relevantes.
Marca No aplica con su motivo cuando corresponda. No presupongas soporte offline
ni conservación de todo el estado. Expresa resultados, no mecanismos técnicos. -->

| Situación | Comportamiento esperado |
| --- | --- |
| Carga o acción en curso | Mientras Bloom responde, como ahora. Mientras se guarda, la tarjeta indica que se está guardando y sus botones no responden. |
| Sin datos | Sin hábitos, el chat se puede usar y el saludo invita a crear el primero. Si se pide marcar, Bloom explica que aún no hay hábitos. |
| Entrada inválida | Orden ambigua o hábito no encontrado: Bloom pregunta. Día no permitido: Bloom lo explica. Propuesta inválida: solo texto, o icono y color por defecto (ver Datos). |
| Error o espera excesiva | Errores al hablar con Bloom: los mensajes actuales. Error al guardar: mensaje de error y la tarjeta conserva sus botones. |
| Sin conexión o conexión interrumpida | No cambia nada y se muestra el error. La tarjeta permite reintentar. |
| Cancelar o volver atrás | Salir del chat con una propuesta sin confirmar no hace nada. Si el guardado ya había empezado, termina. |
| Pasar a segundo plano y regresar | La conversación y el estado de las tarjetas se mantienen. |
| Cambio de día o de semana | Si una tarjeta de marcar queda fuera de la semana actual (por ejemplo, se confirma el lunes una tarjeta del domingo), al confirmar se muestra un error y no cambia nada. |
| Recrear la pantalla | Igual que al volver: se mantienen (por ejemplo, al girar el móvil). |
| Reabrir después de terminarse el proceso | La conversación se pierde, como ahora. Lo que ya se guardó se mantiene. |
| Accesibilidad | El lector de pantalla lee la tarjeta entera (acción, hábito, día o frecuencia y estado), y los botones tienen etiqueta. El estado (hecho o cancelado) se indica con texto, no solo con color. |
| Idiomas | Los textos nuevos de la app van en inglés y en español. Bloom propone el nombre del hábito en el idioma de la conversación. |
| Privacidad | Los logs no recogen el contenido de los mensajes ni los datos de los hábitos. |

**Puntos de la guía no aplicables y motivo:** permisos y trabajo en segundo plano: no hay recordatorios desde el chat, no se piden permisos y no hay tareas en segundo plano.

## Restricciones del pedido

<!-- Condiciones ya impuestas: compatibilidad, límites de alcance, requisitos
de accesibilidad o rendimiento medibles, o una tecnología expresamente exigida.
Ejemplo: Usar Room puede ser una restricción; el diseño de entidades va en PLAN.md.
No conviertas una preferencia del agente en una restricción. -->

- Confirmación obligatoria con una tarjeta dentro del chat para crear, marcar y desmarcar.
- Marcar y desmarcar solo hoy y los días pasados de la semana actual.
- El chat funciona aunque no haya hábitos, en la app y en la API.
- Crear y marcar usan el mismo guardado online-first que la app.

## Criterios de aceptación

<!-- Resultados observables que permitan decidir si se cumple cada requisito.
Incluye los casos alternativos acordados. No uses Funciona correctamente.
Repite el formato según sea necesario. -->

- **CA-01 · RF-01:** Dado el chat, cuando pregunto «¿cómo voy esta semana?», entonces Bloom responde solo con texto, sin tarjeta.
- **CA-02 · RF-10, RF-11:** Dado el chat, cuando escribo «crea un hábito de leer 20 minutos», entonces aparece una tarjeta con un nombre de lectura, un icono y un color del formulario, frecuencia diaria y los botones Crear y Cancelar.
- **CA-03 · RF-03, RF-14:** Dada una tarjeta de crear, cuando pulso Crear, entonces el hábito aparece en Inicio con esos datos, privado y con el recordatorio apagado, y la tarjeta muestra «creado» sin botones.
- **CA-04 · RF-04:** Dada una tarjeta, cuando pulso Cancelar, entonces no cambia nada y la tarjeta muestra «cancelado» sin botones.
- **CA-05 · RF-05:** Dada una tarjeta, cuando pulso confirmar dos veces seguidas, entonces la acción se hace una sola vez (un solo hábito nuevo; un solo cambio de día).
- **CA-06 · RF-06:** Dado que no hay red, cuando confirmo una tarjeta, entonces aparece el error, no cambia nada y la tarjeta conserva sus botones. Con la red de vuelta, confirmar funciona.
- **CA-07 · RF-12:** Dado el chat, cuando escribo solo «crea un hábito», entonces Bloom pregunta cuál y no muestra tarjeta.
- **CA-08 · RF-10:** Dado el chat, cuando pido «crea un hábito de ir al gimnasio lunes, miércoles y viernes», entonces la tarjeta muestra esos tres días.
- **CA-09 · RF-13:** Dado el chat, cuando pido un hábito «con recordatorio a las 8», entonces la tarjeta no tiene recordatorio y Bloom dice que se activa desde Editar hábito.
- **CA-10 · RF-15, RF-17:** Dado un hábito «Leer» sin hacer ayer (y ayer es de esta semana), cuando escribo «marca leer como hecho ayer», entonces aparece una tarjeta con Leer, el día de ayer y «Marcar como hecho».
- **CA-11 · RF-03, RF-20:** Dada esa tarjeta, cuando la confirmo, entonces en Inicio ayer aparece hecho para Leer, la racha se actualiza y la tarjeta muestra «hecho» sin botones.
- **CA-12 · RF-15:** Dado un hábito hecho el lunes de esta semana, cuando escribo «no hice leer el lunes» y confirmo, entonces el lunes deja de estar hecho.
- **CA-13 · RF-16:** Dado el chat, cuando pido marcar mañana o un día de la semana pasada, entonces Bloom explica que no puede y no muestra tarjeta.
- **CA-14 · RF-16:** Dado un hábito de días concretos que no toca hoy, cuando pido marcarlo hoy, entonces Bloom explica que hoy no toca y no muestra tarjeta.
- **CA-15 · RF-18:** Dados dos hábitos «Leer libro» y «Leer noticias», cuando escribo «marca leer», entonces Bloom pregunta cuál. Cuando pido marcar un hábito que no existe, entonces Bloom dice que no lo encuentra.
- **CA-16 · RF-19:** Dado un hábito ya hecho hoy, cuando pido marcarlo hoy, entonces Bloom dice que ya está hecho y no muestra tarjeta.
- **CA-17 · RF-20:** Dada una tarjeta «Marcar como hecho» de hoy, cuando lo marco antes desde Inicio y luego confirmo la tarjeta, entonces el día sigue hecho (no se desmarca) y la tarjeta muestra «hecho».
- **CA-18 · RF-21:** Dada una tarjeta de marcar, cuando elimino ese hábito y confirmo, entonces aparece un error y no cambia nada.
- **CA-19 · RF-07:** Dada una tarjeta, cuando pido una corrección, entonces aparece una tarjeta nueva y la anterior sigue con sus botones.
- **CA-20 · RF-08:** Dado que acabo de confirmar o cancelar una tarjeta, cuando pregunto a Bloom qué ha pasado, entonces responde según el resultado real.
- **CA-21 · RF-09:** Dado el chat, cuando pido crear dos hábitos a la vez, entonces aparece una sola tarjeta y Bloom ofrece seguir con el otro.
- **CA-22 · RF-22:** Dada una cuenta sin hábitos, cuando abro el chat, entonces puedo escribir, veo el saludo para crear el primero y puedo crearlo desde una tarjeta.
- **CA-23 · Datos:** Dada una propuesta de crear con un icono desconocido, entonces la tarjeta muestra el icono por defecto. Dada una propuesta sin nombre, o una de marcar con un día futuro o un hábito inexistente, entonces no aparece tarjeta.
- **CA-24 · Mobile:** Dada una tarjeta sin confirmar, cuando giro el móvil o salgo y vuelvo a la app, entonces la tarjeta sigue igual y con sus botones.
- **CA-25 · RF-23:** `spec.md` describe las órdenes del chat y el chat sin hábitos.

## Cómo se comprueba el comportamiento

<!-- Una fila por criterio: escenario y resultado que debemos comprobar.
La selección de tests, herramientas, comandos y evidencias se desarrolla en PLAN.md.
No marques los criterios como superados durante la especificación. -->

| Criterio | Condiciones y pasos | Resultado esperado |
| --- | --- | --- |
| CA-01 | Preguntar por el progreso de la semana | Solo texto |
| CA-02 | Escribir «crea un hábito de leer 20 minutos» | Tarjeta válida con botones |
| CA-03 | Pulsar Crear e ir a Inicio | Hábito visible; tarjeta «creado» |
| CA-04 | Otra propuesta; pulsar Cancelar | Nada cambia; tarjeta «cancelado» |
| CA-05 | Doble toque rápido al confirmar (crear y marcar) | La acción se hace una vez |
| CA-06 | Modo avión y confirmar; quitarlo y confirmar de nuevo | Error y botones; después se guarda |
| CA-07 | Escribir «crea un hábito» | Pregunta, sin tarjeta |
| CA-08 | Pedir gimnasio lunes, miércoles y viernes | Tarjeta con esos tres días |
| CA-09 | Pedir un hábito con recordatorio a las 8 | Sin recordatorio; Bloom lo explica |
| CA-10 | «marca leer como hecho ayer» | Tarjeta de ayer, «Marcar como hecho» |
| CA-11 | Confirmar e ir a Inicio, día de ayer | Hecho; racha actualizada |
| CA-12 | «no hice leer el lunes» y confirmar | El lunes deja de estar hecho |
| CA-13 | Pedir mañana; pedir un día de la semana pasada | Explicación, sin tarjeta |
| CA-14 | Hábito que no toca hoy; pedir marcarlo hoy | Explicación, sin tarjeta |
| CA-15 | Dos hábitos «Leer…»; «marca leer». Hábito inexistente | Pregunta cuál; «no lo encuentro» |
| CA-16 | Hábito hecho hoy; pedir marcarlo hoy | «Ya está hecho», sin tarjeta |
| CA-17 | Tarjeta de hoy; marcar desde Inicio; confirmar la tarjeta | Sigue hecho |
| CA-18 | Tarjeta de marcar; eliminar el hábito; confirmar | Error; nada cambia |
| CA-19 | Tarjeta; pedir corrección | Tarjeta nueva; la anterior con botones |
| CA-20 | Confirmar o cancelar y preguntar a Bloom | Respuesta coherente con el resultado |
| CA-21 | Pedir dos hábitos a la vez | Una tarjeta y oferta de seguir |
| CA-22 | Cuenta sin hábitos; abrir el chat y pedir uno | Se puede escribir y crear |
| CA-23 | Respuestas de Bloom simuladas: icono desconocido; sin nombre; día futuro; hábito inexistente | Icono por defecto; sin tarjeta en los demás |
| CA-24 | Tarjeta pendiente; girar el móvil y pasar a segundo plano | Tarjeta intacta |
| CA-25 | Revisar `spec.md` | Comportamiento descrito |

## Decisiones pendientes

<!-- Al resolverlas, actualiza las secciones afectadas. Escribe Ninguna cuando
no queden pendientes funcionales ni restricciones por decidir. -->

Ninguna.

<!-- ANTES DE SOLICITAR APROBACIÓN
Comprueba que el alcance está acordado, los flujos son coherentes, los puntos
mobile relevantes están cubiertos y cada requisito tiene criterios comprobables.
Resuelve las dudas y los marcadores pendientes. Mantén el diseño técnico en PLAN.md.
-->
