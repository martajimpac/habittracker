# SPEC: Widget de reto con el diseño de la tarjeta de la app

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

Quien sigue un reto desde la pantalla de inicio debe reconocer de un vistazo la misma tarjeta que ve en la app. El widget de reto y su preview en el selector de widgets pasan a tener el diseño de la tarjeta de reto de la app.

## Situación actual

<!-- Comportamiento actual relevante, limitación que queremos resolver y
comportamientos existentes que deben conservarse. No describas la arquitectura. -->

- **App (tarjeta de reto):**
  - Tarjeta clara con esquinas redondeadas.
  - Arriba, el icono del hábito sobre su color suave y el nombre del hábito.
  - Los dos participantes cara a cara, cada uno con su avatar y su nombre, y en medio un «VS» grande en el color del hábito. Quien va ganando lleva el avatar con borde de color y una insignia con copa.
  - Una barra por participante, con su nombre y «puntos/total» sobre la misma escala. La barra de quien va por detrás aparece más tenue.
  - Los días restantes con un icono de reloj, o «Último día».
- **Widget actual:**
  - Cabecera verde con el hábito y «vs rival».
  - Una línea con copa y el nombre de quien va ganando.
  - Una línea «Tú 60 % · Alex 40 %».
  - Dos barras de 10 segmentos y «N días restantes».
- **La preview del selector** imita el widget actual.
- **Se conserva:**
  - la configuración (elegir reto y reconfigurar);
  - el estado «Configurar desafío» sin reto;
  - la carga con el logo;
  - el snapshot para pintar sin red;
  - el tap que abre Amigos, y el que abre el selector cuando no hay reto;
  - la marca «Pendiente» en retos pendientes;
  - el widget de hoy tal como está.

## Dentro del alcance

<!-- Requisitos concretos, con identificadores estables para vincularlos a
criterios, decisiones del plan y tareas. -->

- **RF-01:** El widget de reto pierde la cabecera verde y se pinta como la tarjeta de la app: fondo claro, esquinas redondeadas y, arriba, el icono del hábito sobre su color suave y el nombre del hábito.
- **RF-02:** Con espacio suficiente, muestra a los dos participantes cara a cara, con avatar y nombre, y el «VS» grande en el color del hábito. El participante local se llama por su nombre de perfil, o «Yo» si no tiene.
- **RF-03:** Los avatares son círculos con las iniciales del participante sobre su color de avatar, sin fotos. Las iniciales siguen la misma regla que en la app. Si no hay color, se usa el color por defecto de las iniciales de la app.
- **RF-04:** Quien va ganando lleva el avatar con borde en el color del hábito y una insignia con copa. Si van empatados, ninguno la lleva.
- **RF-05:** Una barra por participante, con su nombre y «puntos/total» sobre la misma escala que en la app. La barra y la puntuación de quien va por detrás aparecen más tenues. En empate, las dos aparecen destacadas.
- **RF-06:** Abajo, los días restantes con icono de reloj, con el mismo texto que en la app («N días restantes» o «Último día»). Si el reto está pendiente, se sigue mostrando «Pendiente».
- **RF-07:** Si el widget es pequeño y la tarjeta completa no cabe, se muestra una versión compacta sin avatares ni «VS»: hábito, las dos barras y los días restantes.
- **RF-08:** La preview del selector de widgets muestra la tarjeta completa con datos de ejemplo y el mismo aspecto que el widget real.
- **RF-09:** Los textos nuevos están en inglés y español.

## Fuera de alcance

<!-- Exclusiones acordadas, no deducidas por el agente. Si no hay exclusiones
adicionales, indícalo tras revisarlo con la persona. -->

- Fotos de perfil en el widget (decidido: solo iniciales).
- Cambios en el widget de hoy.
- Cambios en la tarjeta de la app, la configuración, el refresco o los datos que se cargan.
- Completar el hábito desde el widget de reto.
- Tema oscuro propio del widget (sigue con los colores claros actuales).

## Flujo de usuario

<!-- Cómo se inicia, qué hace el usuario y qué resultado obtiene.
Incluye pantallas afectadas, navegación y alternativas relevantes. -->

1. En el selector de widgets, la persona ve la preview de reto con el aspecto de la tarjeta.
2. Coloca el widget y elige un reto (o lo configura después, como ahora).
3. El widget muestra la tarjeta del reto. Si lo hace pequeño, pasa a la versión compacta, y al agrandarlo vuelve la tarjeta completa.
4. Al pulsarlo se abre Amigos, como ahora.

## Datos y reglas de negocio

<!-- Información que necesita el usuario, campos obligatorios, validaciones,
límites y reglas como duplicados u orden de presentación. Describe significado
y comportamiento, sin diseñar tablas, DTO, DAO ni almacenamiento. -->

- Datos que pasan a usarse:
  - icono del hábito;
  - nombre, iniciales y color de avatar de los dos participantes;
  - puntos de cada uno y total de la escala.
  Todos los da ya el reto que carga la app. Se guardan en el snapshot del widget para poder pintarlos sin red.
- Quién va ganando sigue la misma regla que la app y que el widget actual.
- Un snapshot guardado por una versión anterior, sin los datos nuevos, se pinta sin errores con valores por defecto y se completa en el siguiente refresco con red.

## Comportamiento mobile y casos alternativos

<!-- Adapta la tabla usando MOBILE_GUIDELINES.md. Añade escenarios relevantes.
Marca No aplica con su motivo cuando corresponda. No presupongas soporte offline
ni conservación de todo el estado. Expresa resultados, no mecanismos técnicos. -->

| Situación | Comportamiento esperado |
| --- | --- |
| Carga o acción en curso | Igual que hoy: logo y texto de carga. |
| Sin datos | Igual que hoy: «Configurar desafío» si no hay reto. |
| Entrada inválida | No aplica: el widget no tiene entradas. |
| Error o espera excesiva | Igual que hoy: se pinta el último snapshot. |
| Sin conexión o conexión interrumpida | Igual que hoy: último snapshot, con el nuevo diseño. |
| Snapshot antiguo (de la versión anterior) | Se pinta con el nuevo diseño. Los datos que falten usan valores por defecto (por ejemplo, el icono genérico) hasta el siguiente refresco. |
| Cambiar el tamaño del widget | Pasa entre tarjeta completa y versión compacta sin recortar texto ni controles. |
| Nombres largos | Se cortan con puntos suspensivos sin descolocar la tarjeta. |
| Cancelar, volver atrás, segundo plano, recrear, reabrir | No aplica: no cambia el ciclo de vida del widget. |
| Accesibilidad | Las barras y la copa conservan su descripción para lectores de pantalla. |
| Idiomas | Textos en inglés y español. |

**Puntos de la guía no aplicables y motivo:** permisos, trabajo en segundo plano, formularios y conectividad nueva. Solo cambia el aspecto, no la carga ni las acciones.

## Restricciones del pedido

<!-- Condiciones ya impuestas: compatibilidad, límites de alcance, requisitos
de accesibilidad o rendimiento medibles, o una tecnología expresamente exigida.
Ejemplo: Usar Room puede ser una restricción; el diseño de entidades va en PLAN.md.
No conviertas una preferencia del agente en una restricción. -->

- Diseño similar a la tarjeta de reto de la app, tanto en el widget como en su preview.
- Avatares con iniciales, sin cabecera verde y con versión compacta en tamaños pequeños (decisiones de la propietaria).
- Sigue siendo un widget de Jetpack Glance y la preview sigue siendo un `previewLayout`.

## Criterios de aceptación

<!-- Resultados observables que permitan decidir si se cumple cada requisito.
Incluye los casos alternativos acordados. No uses Funciona correctamente.
Repite el formato según sea necesario. -->

- **CA-01 · RF-01:** Dado un reto configurado, el widget no tiene cabecera verde y muestra la tarjeta clara redondeada con el icono y el nombre del hábito arriba.
- **CA-02 · RF-02, RF-03:** Dado un widget grande, se ven los dos avatares con iniciales sobre su color, los nombres y el «VS» en el color del hábito.
- **CA-03 · RF-04:** Si gana el participante local, su avatar lleva el borde y la copa, y el del rival no. Al revés si gana el rival. En empate, ninguno.
- **CA-04 · RF-05:** Las barras muestran «puntos/total» de cada participante sobre la misma escala. La del que va por detrás está atenuada, y en empate las dos están destacadas.
- **CA-05 · RF-06:** Se muestra «N días restantes» o «Último día» con el icono de reloj, y «Pendiente» si el reto está pendiente.
- **CA-06 · RF-07:** Dado un widget de 2×2, se ve la versión compacta sin avatares ni VS y sin texto recortado. Al agrandarlo aparece la tarjeta completa.
- **CA-07 · RF-08:** En el selector de widgets, la preview de reto muestra la tarjeta completa con el mismo aspecto que el widget real.
- **CA-08 · RF-09:** Con el móvil en español, todos los textos del widget y de la preview están en español.
- **CA-09:** Un snapshot guardado por la versión anterior se pinta sin errores con el nuevo diseño.

## Cómo se comprueba el comportamiento

<!-- Una fila por criterio: escenario y resultado que debemos comprobar.
La selección de tests, herramientas, comandos y evidencias se desarrolla en PLAN.md.
No marques los criterios como superados durante la especificación. -->

| Criterio | Condiciones y pasos | Resultado esperado |
| --- | --- | --- |
| CA-01 | Widget con un reto configurado | Tarjeta sin cabecera verde |
| CA-02 | Widget grande | Avatares con iniciales, nombres y VS |
| CA-03 | Retos con ventaja local, ventaja rival y empate | Copa y borde solo en quien gana |
| CA-04 | Mismos casos | Barras con «puntos/total» y atenuación correcta |
| CA-05 | Reto activo con días, último día y pendiente | Texto e icono correctos |
| CA-06 | Redimensionar entre 2×2 y grande | Compacta ↔ completa, sin recortes |
| CA-07 | Abrir el selector de widgets | Preview con la tarjeta |
| CA-08 | Idioma español | Textos en español |
| CA-09 | Snapshot antiguo guardado | Se pinta sin error |

## Decisiones pendientes

<!-- Al resolverlas, actualiza las secciones afectadas. Escribe Ninguna cuando
no queden pendientes funcionales ni restricciones por decidir. -->

Ninguna. Resueltas con la propietaria: avatares con iniciales, sin cabecera verde y versión compacta en tamaños pequeños.
