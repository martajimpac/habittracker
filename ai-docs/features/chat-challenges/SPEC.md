# SPEC: Órdenes en el chat de Bloom: crear retos

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

Quien usa Bloom y tiene amigos quiere retarles sin salir del chat. Podrá escribir cosas como «reta a Laura a meditar cada día durante 14 días». Bloom propondrá el reto en una tarjeta dentro de la conversación, igual que ya hace al crear hábitos o marcar días, y el reto se enviará cuando la persona lo confirme.

## Situación actual

<!-- Comportamiento actual relevante, limitación que queremos resolver y
comportamientos existentes que deben conservarse. No describas la arquitectura. -->

- **Chat de Bloom (feature `chat-actions`):** Bloom ya propone dos órdenes con tarjeta y confirmación: crear un hábito y marcar o desmarcar un día. Bloom no sabe nada de los amigos ni de los retos.
- **Crear un reto:** hoy solo se puede desde Amigos, con el botón de retar de un amigo. La hoja pide un hábito nuevo (nombre, icono, color y frecuencia), un criterio (% completado, todos los días o racha; por defecto % completado) y una duración (3, 7, 14 o 30 días; por defecto 7). Al enviar, el reto empieza en ese momento, queda `pending` y en la cuenta de quien reta se crea un hábito público con esos datos. La persona retada recibe un push y su copia del hábito se crea solo si acepta.
- Solo se puede retar a amigos aceptados. Todo lo social es online-first: sin red no se guarda nada y se muestra el error.
- **Se conserva:**
  - las órdenes actuales del chat (crear hábito, marcar y desmarcar) y todas sus reglas (RF-01 a RF-09 de `chat-actions`: solo con petición explícita, tarjeta con confirmar y cancelar, una propuesta por respuesta, reintento si falla, Bloom sabe el resultado de las propuestas anteriores);
  - la hoja de crear reto de Amigos, sin cambios;
  - el push al retado, la pantalla de detalle del reto, aceptar y rechazar, sin cambios.

## Dentro del alcance

<!-- Requisitos concretos, con identificadores estables para vincularlos a
criterios, decisiones del plan y tareas. -->

- **RF-01:** Cuando la persona pide retar a un amigo, Bloom propone el reto con una tarjeta que muestra el amigo (nombre y foto), el hábito del reto (nombre, icono, color y frecuencia), el criterio y la duración, con los botones Retar y Cancelar.
- **RF-02:** Confirmar, cancelar, guardar en curso, error y reintento funcionan igual que en las tarjetas actuales (RF-02 a RF-08 de `chat-actions`). Un doble toque envía un solo reto.
- **RF-03:** Al confirmar, el reto se crea exactamente igual que desde la hoja de Amigos: queda pendiente, quien reta recibe su hábito público, y la persona retada recibe el push.
- **RF-04:** Lo que la persona no indica lo completa Bloom como la hoja: icono y color del catálogo del formulario, frecuencia diaria, criterio % completado y duración de 7 días.
- **RF-05:** La duración solo puede ser 3, 7, 14 o 30 días, como en la hoja. Si la persona pide otra, Bloom propone la más cercana (en empate, la más larga; menos de 3 pasa a 3 y más de 30 pasa a 30) y lo dice.
- **RF-06:** Solo se puede retar a amigos aceptados. Bloom no conoce a los amigos: solo recoge el nombre que escribió la persona, y es la app quien lo busca entre sus amigos aceptados. Si no coincide con ninguno, la app muestra, en lugar de la tarjeta y del texto de Bloom, que no está entre sus amigos. Si encaja con varios, la app muestra sus nombres y pide que indique a cuál. Sin amigos, la app explica que primero hay que añadir uno desde Amigos.
- **RF-12:** En los mensajes siguientes, Bloom sabe que esa propuesta no se mostró y por qué (amigo no encontrado, varios posibles o sin amigos), sin recibir nombres de amigos.
- **RF-07:** Si la orden no deja claro de qué es el reto (por ejemplo, solo «reta a Laura»), Bloom pregunta de qué hábito en vez de proponer.
- **RF-08:** Tras confirmar, la tarjeta muestra que el reto se ha enviado y permite abrir su detalle.
- **RF-09:** El reto creado aparece en Amigos (retos activos y pendientes) y su hábito en Inicio y Estadísticas con la insignia de reto, igual que si se hubiera creado desde Amigos.
- **RF-10:** Si el amigo deja de serlo entre la propuesta y la confirmación, al confirmar se muestra un error y no se crea nada.
- **RF-11:** `spec.md` se actualiza con este comportamiento.

## Fuera de alcance

<!-- Exclusiones acordadas, no deducidas por el agente. Si no hay exclusiones
adicionales, indícalo tras revisarlo con la persona. -->

- Decidido con la persona:
  - Aceptar, rechazar, renombrar o cancelar retos desde el chat.
  - Retar usando un hábito ya existente (la regla actual de la app es crear siempre uno nuevo).
  - Retar a varias personas a la vez o a alguien que no es amigo.
  - Enviar solicitudes de amistad desde el chat.
  - Cambios en la hoja de crear reto de Amigos.

## Flujo de usuario

<!-- Cómo se inicia, qué hace el usuario y qué resultado obtiene.
Incluye pantallas afectadas, navegación y alternativas relevantes. -->

1. La persona abre el chat de Bloom y escribe «reta a Laura a meditar 10 minutos cada día durante 14 días, gana quien tenga mejor racha».
2. Bloom responde con un texto breve y una tarjeta: Laura (foto y nombre), «Meditar 10 minutos», icono y color, diario, criterio racha, 14 días.
3. Pulsa Retar. La tarjeta indica que se está enviando y después que el reto se ha enviado, con un enlace a su detalle.
4. Laura recibe el push como cualquier reto. En Amigos el reto aparece como pendiente.

**Alternativas**

- **Cancelar:** la tarjeta queda cancelada y no se crea nada.
- **Corregir:** «mejor 7 días»: Bloom propone una tarjeta nueva; la anterior conserva sus botones.
- **Amigo no encontrado o ambiguo:** se pide aclarar a quién; no hay tarjeta.
- **Error:** sin red, al confirmar aparece el error y la tarjeta conserva sus botones.

## Datos y reglas de negocio

<!-- Información que necesita el usuario, campos obligatorios, validaciones,
límites y reglas como duplicados u orden de presentación. Describe significado
y comportamiento, sin diseñar tablas, DTO, DAO ni almacenamiento. -->

- **Campos del reto:** amigo (obligatorio, entre los aceptados), nombre del hábito (obligatorio, máximo 100 caracteres), icono y color (del catálogo del formulario; si no, los de por defecto), frecuencia (diaria, días concretos con al menos uno, u objetivo semanal 1–7), criterio (% completado, todos los días o racha) y duración (3, 7, 14 o 30 días).
- **Validación:** la app comprueba la propuesta antes de mostrar la tarjeta y otra vez al confirmar. Sin amigo válido, sin nombre o con frecuencia inválida, no se muestra tarjeta, solo el texto.
- **Inicio del reto:** en el momento de confirmar, no en el de la propuesta, igual que en la hoja.
- **Retos repetidos:** se permiten varios retos con el mismo amigo, como en la hoja.
- **Datos enviados a Bloom:** los mismos que en `chat-actions`, más el resultado de las propuestas de reto (enviada, cancelada o descartada y su motivo). No se envían nombres, usuarios ni fotos de amigos; Bloom solo ve el nombre que la persona escribe en su mensaje.
- **Búsqueda del amigo:** la app compara el nombre escrito con el nombre visible y el usuario de cada amigo aceptado, sin distinguir mayúsculas ni acentos. Una coincidencia exacta tiene prioridad sobre las parciales.

## Comportamiento mobile y casos alternativos

<!-- Adapta la tabla usando MOBILE_GUIDELINES.md. Añade escenarios relevantes.
Marca No aplica con su motivo cuando corresponda. No presupongas soporte offline
ni conservación de todo el estado. Expresa resultados, no mecanismos técnicos. -->

| Situación | Comportamiento esperado |
| --- | --- |
| Carga o acción en curso | Mientras se envía, la tarjeta indica que se está enviando y sus botones no responden. |
| Sin datos | Sin amigos: se explica que hay que añadir uno desde Amigos; no hay tarjeta. |
| Entrada inválida | Amigo no encontrado o ambiguo, o reto sin hábito claro: se pide aclarar. Duración fuera de las opciones: ver RF-05. |
| Error o espera excesiva | Error al enviar: mensaje de error y la tarjeta conserva sus botones. Errores de Bloom: los actuales. |
| Sin conexión o conexión interrumpida | No se crea nada y se muestra el error. La tarjeta permite reintentar. No se crea un reto duplicado al reintentar. |
| Cancelar o volver atrás | Salir con una propuesta sin confirmar no hace nada. Si el envío ya había empezado, termina. |
| Pasar a segundo plano y regresar | La conversación y el estado de las tarjetas se mantienen. |
| Recrear la pantalla | Igual que al volver (por ejemplo, al girar el móvil). |
| Reabrir después de terminarse el proceso | La conversación se pierde, como ahora. El reto ya enviado se mantiene. |
| Notificaciones | Si `POST_NOTIFICATIONS` está denegado para la persona retada, el reto se crea igual (regla actual). No se pide ningún permiso nuevo. |
| Accesibilidad | El lector de pantalla lee la tarjeta entera (amigo, hábito, frecuencia, criterio, duración y estado), y los botones tienen etiqueta. El estado se indica con texto, no solo con color. |
| Idiomas | Textos nuevos en inglés y español. Bloom propone el nombre del hábito en el idioma de la conversación. |
| Privacidad | Los logs no recogen mensajes, nombres de amigos ni datos de hábitos. |

**Puntos de la guía no aplicables y motivo:** trabajo en segundo plano: el push lo envía el backend existente, no hay tareas nuevas en el móvil.

## Restricciones del pedido

<!-- Condiciones ya impuestas: compatibilidad, límites de alcance, requisitos
de accesibilidad o rendimiento medibles, o una tecnología expresamente exigida.
Ejemplo: Usar Room puede ser una restricción; el diseño de entidades va en PLAN.md.
No conviertas una preferencia del agente en una restricción. -->

- Confirmación obligatoria con tarjeta, como las órdenes actuales del chat.
- El reto se crea con el mismo guardado online-first que la hoja de Amigos.

## Criterios de aceptación

<!-- Resultados observables que permitan decidir si se cumple cada requisito.
Incluye los casos alternativos acordados. No uses Funciona correctamente.
Repite el formato según sea necesario. -->

- **CA-01 · RF-01, RF-04:** Dado un amigo aceptado «Laura», cuando escribo «reta a Laura a meditar», entonces aparece una tarjeta con Laura, un hábito de meditar con icono y color del catálogo, diario, % completado, 7 días y los botones Retar y Cancelar.
- **CA-02 · RF-01:** Cuando escribo «reta a Laura a correr lunes, miércoles y viernes durante 14 días, gana la mejor racha», entonces la tarjeta muestra esos tres días, racha y 14 días.
- **CA-03 · RF-03, RF-08, RF-09:** Dada una tarjeta de reto, cuando pulso Retar, entonces el reto aparece pendiente en Amigos, el hábito aparece en Inicio con la insignia de reto, Laura recibe el push, y la tarjeta muestra «enviado» sin botones y con acceso al detalle.
- **CA-04 · RF-02:** Cuando pulso Retar dos veces seguidas, entonces se crea un solo reto y un solo hábito.
- **CA-05 · RF-02:** Dado que no hay red, cuando confirmo, entonces aparece el error, no se crea nada y la tarjeta conserva sus botones; con red, confirmar crea un solo reto.
- **CA-06 · RF-02:** Cuando pulso Cancelar, entonces no se crea nada y la tarjeta muestra «cancelado» sin botones.
- **CA-07 · RF-06, RF-12:** Cuando pido retar a alguien que no es mi amigo, entonces no hay tarjeta y la app indica que no está entre mis amigos. Con dos amigos «Laura G.» y «Laura M.», cuando escribo «reta a Laura», entonces la app muestra los dos nombres y pide a cuál; cuando respondo «Laura G.», entonces aparece la tarjeta con Laura G.
- **CA-08 · RF-06:** Dado que no tengo amigos, cuando pido retar a alguien, entonces se me explica que primero añada amigos desde Amigos.
- **CA-09 · RF-07:** Cuando escribo solo «reta a Laura», entonces Bloom pregunta de qué y no muestra tarjeta.
- **CA-10 · RF-05:** Cuando pido un reto de 10 días, entonces la tarjeta muestra 7 días y Bloom dice que solo puede ser de 3, 7, 14 o 30 días.
- **CA-11 · RF-10:** Dada una tarjeta de reto con Laura, cuando Laura deja de ser mi amiga y luego confirmo, entonces aparece un error y no se crea nada.
- **CA-12 · RF-01:** Dado el chat, cuando pregunto algo sin pedir un reto, entonces no aparece tarjeta de reto.

## Cómo se comprueba el comportamiento

<!-- Una fila por criterio: escenario y resultado que debemos comprobar.
La selección de tests, herramientas, comandos y evidencias se desarrolla en PLAN.md.
No marques los criterios como superados durante la especificación. -->

| Criterio | Condiciones y pasos | Resultado esperado |
| --- | --- | --- |
| CA-01 | Cuenta con amiga aceptada Laura; escribir «reta a Laura a meditar». | Tarjeta con valores por defecto y botones Retar/Cancelar. |
| CA-02 | Igual, con días, duración y criterio explícitos. | Tarjeta con lunes/miércoles/viernes, racha, 14 días. |
| CA-03 | Confirmar la tarjeta; abrir Amigos e Inicio; revisar el móvil de Laura. | Reto pendiente, hábito con insignia, push recibido, tarjeta «enviado» con acceso al detalle. |
| CA-04 | Doble toque rápido en Retar. | Un reto y un hábito en Supabase. |
| CA-05 | Modo avión, confirmar; quitar modo avión, confirmar. | Error sin cambios; después, un único reto. |
| CA-06 | Pulsar Cancelar. | Sin reto; tarjeta «cancelado». |
| CA-07 | Pedir retar a un no amigo; con dos Lauras, «reta a Laura» y luego «Laura G.». | Sin tarjeta y mensaje de la app; luego lista de las dos; luego tarjeta con Laura G. |
| CA-08 | Cuenta sin amigos; pedir un reto. | Mensaje para añadir amigos; sin tarjeta. |
| CA-09 | «reta a Laura». | Pregunta de qué; sin tarjeta. |
| CA-10 | «reta a Laura a leer durante 10 días». | Tarjeta con 7 días y aviso de las duraciones permitidas. |
| CA-11 | Proponer; eliminar la amistad desde la otra cuenta; confirmar. | Error; sin reto. |
| CA-12 | «¿cómo voy esta semana?». | Solo texto. |

## Decisiones pendientes

<!-- Al resolverlas, actualiza las secciones afectadas. Escribe Ninguna cuando
no queden pendientes funcionales ni restricciones por decidir. -->

Ninguna. Resueltas con la persona:

1. Los nombres de amigos no se envían a Bloom; la app resuelve el nombre (RF-06, RF-12).
2. Por defecto, % completado y 7 días (RF-04).
3. Solo 3, 7, 14 o 30 días (RF-05).
4. Fuera de alcance confirmado.
