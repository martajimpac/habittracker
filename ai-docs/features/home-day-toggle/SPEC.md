# SPEC: Marcar días desde Inicio, no desde el detalle

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

Quien registra sus hábitos quiere marcar y desmarcar los días desde un único sitio: Inicio. Allí podrá marcar como hecho, o quitar, un hábito de hoy y también de los días ya pasados de la semana actual. El calendario del detalle del hábito pasa a ser solo de consulta.

## Situación actual

<!-- Comportamiento actual relevante, limitación que queremos resolver y
comportamientos existentes que deben conservarse. No describas la arquitectura. -->

- **Inicio:** la fila de la semana (de lunes a domingo de la semana actual) permite elegir un día. Solo en hoy se puede tocar un hábito para marcarlo o quitarlo. En un día pasado o futuro, las filas no responden al toque y los hábitos sin hacer se ven atenuados.
- **Detalle del hábito:** en el calendario mensual se puede tocar cualquier día hasta hoy para marcarlo o quitarlo. Mientras se guarda, ese día se atenúa y no admite otro toque. Si falla, un snackbar muestra el error. Debajo hay una línea que explica que se puede tocar un día pasado.
- **Se conserva:**
  - la regla online-first: el cambio solo se guarda si el servidor lo acepta; si falla (p. ej. sin red), el día no cambia y se muestra el error;
  - el recálculo automático de rachas, protecciones, porcentajes, estadísticas, widgets y el progreso del día al cambiar un registro;
  - qué hábitos aparecen cada día (según su programación);
  - el calendario del detalle mostrando los días hechos, con flechas para cambiar de mes;
  - la fila de la semana: solo la semana actual.

## Dentro del alcance

<!-- Requisitos concretos, con identificadores estables para vincularlos a
criterios, decisiones del plan y tareas. -->

- **RF-01:** En Inicio, con un día pasado de la semana actual seleccionado, tocar un hábito lo marca como hecho ese día, o lo quita si ya estaba hecho, igual que hoy.
- **RF-02:** En Inicio, hoy se sigue marcando y quitando como hasta ahora.
- **RF-03:** En Inicio, los días futuros no se pueden marcar: tocar un hábito no hace nada.
- **RF-04:** En Inicio, los hábitos de un día pasado se ven igual que los de hoy, sin atenuar. Los días futuros siguen como están.
- **RF-05:** Mientras se guarda el cambio de un hábito en un día, esa fila no admite otro toque hasta que termina.
- **RF-06:** Si el cambio falla, el hábito no cambia y se muestra el mensaje de error, como hoy.
- **RF-07:** En el detalle del hábito, el calendario es solo de consulta: tocar un día no cambia nada. Desaparece la línea que dice que se puede tocar un día pasado.
- **RF-08:** `spec.md` se actualiza con este comportamiento.

## Fuera de alcance

<!-- Exclusiones acordadas, no deducidas por el agente. Si no hay exclusiones
adicionales, indícalo tras revisarlo con la persona. -->

- Navegar a semanas anteriores desde Inicio (decidido: solo la semana actual).
- Que tocar un día del calendario del detalle lleve a Inicio (decidido: solo consulta).
- Cambios en los widgets: el widget de hoy sigue sin permitir marcar.
- Cambios en Estadísticas.

## Flujo de usuario

<!-- Cómo se inicia, qué hace el usuario y qué resultado obtiene.
Incluye pantallas afectadas, navegación y alternativas relevantes. -->

1. En Inicio, la persona toca un día pasado de la fila de la semana.
2. Ve la lista de hábitos de ese día con el mismo aspecto que hoy.
3. Toca un hábito: queda marcado como hecho (o se quita si ya lo estaba). El conteo del día, la flor, el porcentaje del día en la fila de la semana y las rachas se actualizan.
4. Si falla, aparece un snackbar con el error y el hábito sigue como estaba.
5. En el detalle de un hábito, el calendario muestra los días hechos, pero tocar un día no hace nada.

## Datos y reglas de negocio

<!-- Información que necesita el usuario, campos obligatorios, validaciones,
límites y reglas como duplicados u orden de presentación. Describe significado
y comportamiento, sin diseñar tablas, DTO, DAO ni almacenamiento. -->

- Se pueden editar hoy y los días anteriores de la semana actual en los que el hábito aparece en Inicio. Los días futuros, no.
- Marcar o quitar un día pasado tiene el mismo efecto sobre rachas, protecciones y estadísticas que hacerlo hoy; se recalculan solos.
- No hay confirmación: un toque cambia el estado, igual que hoy.

## Comportamiento mobile y casos alternativos

<!-- Adapta la tabla usando MOBILE_GUIDELINES.md. Añade escenarios relevantes.
Marca No aplica con su motivo cuando corresponda. No presupongas soporte offline
ni conservación de todo el estado. Expresa resultados, no mecanismos técnicos. -->

| Situación | Comportamiento esperado |
| --- | --- |
| Carga o acción en curso | La fila del hábito que se está guardando no admite otro toque hasta que termina. |
| Sin datos | Sin cambios: un día sin hábitos muestra el estado vacío actual. |
| Entrada inválida | Tocar un hábito en un día futuro no hace nada. |
| Error o espera excesiva | Snackbar con el error; el hábito no cambia. |
| Sin conexión o conexión interrumpida | No se guarda; snackbar con el error, como hoy. |
| Cancelar o volver atrás | No aplica: el cambio es un toque, sin formulario. |
| Pasar a segundo plano y regresar | Se mantiene el día seleccionado, como hoy. Si pasa la medianoche, se aplica la regla actual de cambio de día. |
| Recrear la pantalla | Se mantiene el comportamiento actual del día seleccionado. |
| Reabrir después de terminarse el proceso | Inicio abre en hoy, como ahora. |
| Accesibilidad | Las filas de días pasados se pueden activar con el lector de pantalla igual que las de hoy. |

**Puntos de la guía no aplicables y motivo:** permisos, trabajo en segundo plano y textos nuevos: no hay permisos ni tareas en segundo plano, y no se añaden textos (se quita uno).

## Restricciones del pedido

<!-- Condiciones ya impuestas: compatibilidad, límites de alcance, requisitos
de accesibilidad o rendimiento medibles, o una tecnología expresamente exigida.
Ejemplo: Usar Room puede ser una restricción; el diseño de entidades va en PLAN.md.
No conviertas una preferencia del agente en una restricción. -->

- Usar el mismo guardado online-first que ya existe para marcar un hábito.
- En Inicio, solo la semana actual.

## Criterios de aceptación

<!-- Resultados observables que permitan decidir si se cumple cada requisito.
Incluye los casos alternativos acordados. No uses Funciona correctamente.
Repite el formato según sea necesario. -->

- **CA-01 · RF-01:** Dado un día pasado de la semana actual seleccionado en Inicio y un hábito sin hacer ese día, cuando lo toco, entonces queda marcado como hecho ese día y el conteo del día sube.
- **CA-02 · RF-01:** Dado un día pasado con un hábito hecho, cuando lo toco, entonces deja de estar hecho ese día.
- **CA-03 · RF-02:** Dado hoy seleccionado, cuando toco un hábito, entonces se marca o se quita como hasta ahora.
- **CA-04 · RF-03:** Dado un día futuro seleccionado, cuando toco un hábito, entonces no cambia nada.
- **CA-05 · RF-04:** Dado un día pasado seleccionado, entonces los hábitos sin hacer no se ven atenuados.
- **CA-06 · RF-05:** Dado un cambio guardándose, cuando vuelvo a tocar ese hábito, entonces el segundo toque se ignora.
- **CA-07 · RF-06:** Dado que no hay red, cuando toco un hábito en un día pasado, entonces aparece el error y el hábito no cambia.
- **CA-08 · RF-07:** Dado el detalle de un hábito, cuando toco un día del calendario, entonces no cambia nada y no aparece la línea de ayuda.
- **CA-09 · RF-01:** Dado que marco un día pasado desde Inicio, cuando abro el detalle de ese hábito, entonces el calendario muestra ese día como hecho.

## Cómo se comprueba el comportamiento

<!-- Una fila por criterio: escenario y resultado que debemos comprobar.
La selección de tests, herramientas, comandos y evidencias se desarrolla en PLAN.md.
No marques los criterios como superados durante la especificación. -->

| Criterio | Condiciones y pasos | Resultado esperado |
| --- | --- | --- |
| CA-01 | Inicio, elegir ayer, tocar un hábito sin hacer | Marcado; el conteo y el porcentaje del día suben |
| CA-02 | Mismo día, tocar el hábito hecho | Deja de estar hecho |
| CA-03 | Hoy, tocar un hábito | Igual que antes |
| CA-04 | Elegir un día futuro, tocar un hábito | Sin cambios |
| CA-05 | Elegir un día pasado | Filas sin atenuar |
| CA-06 | Dos toques seguidos en el mismo hábito mientras se guarda | Solo se aplica el primero |
| CA-07 | Modo avión, día pasado, tocar un hábito | Snackbar de error; sin cambios |
| CA-08 | Detalle, tocar un día del calendario | Sin cambios; sin línea de ayuda |
| CA-09 | Tras CA-01, abrir el detalle de ese hábito | Ese día aparece hecho |

## Decisiones pendientes

<!-- Al resolverlas, actualiza las secciones afectadas. Escribe Ninguna cuando
no queden pendientes funcionales ni restricciones por decidir. -->

Ninguna.

<!-- ANTES DE SOLICITAR APROBACIÓN
Comprueba que el alcance está acordado, los flujos son coherentes, los puntos
mobile relevantes están cubiertos y cada requisito tiene criterios comprobables.
Resuelve las dudas y los marcadores pendientes. Mantén el diseño técnico en PLAN.md.
-->
