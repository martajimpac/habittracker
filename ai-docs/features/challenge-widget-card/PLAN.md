# PLAN: Widget de reto con el diseño de la tarjeta de la app

**SPEC de referencia:** `ai-docs/features/challenge-widget-card/SPEC.md`
**Versión de la spec revisada:** Aprobada el 2026-11-14
**Estado:** Aprobado <!-- Borrador | En revisión | Aprobado -->

<!-- PARA LA PERSONA
Copia esta plantilla como PLAN.md junto a la SPEC.md aprobada.
Este documento define la solución técnica. Una vez revisado, el agente puede
derivar TASKS.md con tareas, dependencias y comprobaciones.
-->

<!-- PARA EL AGENTE
- Lee la SPEC.md aprobada, las instrucciones del proyecto y MOBILE_GUIDELINES.md.
  Si falta un documento necesario o la spec no está aprobada, indícalo antes de avanzar.
- Inspecciona el repositorio. Referencia rutas verificadas y distingue las nuevas propuestas.
- Propón una solución proporcional al alcance y coherente con el proyecto.
  Reutiliza lo existente y justifica nuevas dependencias o cambios de arquitectura.
- Distingue hechos, decisiones confirmadas y propuestas. Consulta las decisiones
  no resueltas; haz pocas preguntas por vez y actualiza el plan con las respuestas.
- Referencia los requisitos y criterios por su ID, sin copiar toda la spec.
- Si una decisión cambia el comportamiento o alcance, vuelve a la spec y solicita
  confirmación. No resuelvas una duda de producto mediante una suposición técnica.
- Conserva estos comentarios. No implementes durante la planificación.
- Solicita aprobación antes de marcar el plan como Aprobado. La autorización
  para implementar debe ser explícita; no se deduce del estado de los documentos.
-->

## Contexto técnico verificado

<!-- Qué existe hoy y cómo participa en la funcionalidad. -->

| Componente o archivo existente | Ruta verificada | Responsabilidad y uso previsto |
| --- | --- | --- |
| Widget Glance de reto | `presentation/widgets/challenge/ChallengeGlanceWidget.kt` | Carga y snapshot (no cambian). Se rehace `ChallengeWidgetContent`. |
| Snapshot | `ChallengeWidgetSnapshot` (mismo archivo) | JSON guardado en DataStore. Se amplía con campos opcionales. |
| Tarjeta de la app | `presentation/components/ChallengeVersusCard.kt` | Referencia visual: medidas, colores y textos. |
| Avatar e iniciales | `presentation/components/ProfileAvatar.kt` (`userAvatarInitials`, `@Composable`) | Se saca la regla de iniciales a una función pura reutilizable. |
| Ganador | `domain/model/ChallengeLeader.kt` (`challengeLeader`) | Se reutiliza tal cual. |
| Piezas de widget | `presentation/widgets/WidgetChrome.kt` | El widget de reto deja de usar `WidgetScreen`, `WidgetProgressBar` y el texto del ganador. El widget de hoy no se toca. |
| Iconos | `habitIconRes`, `R.drawable.ic_trophy`, `R.drawable.widget_check_circle` (óvalo tintable) | Icono del hábito, copa y círculos de avatar. |
| Preview | `res/layout/challenge_widget_preview.xml`, `res/xml/challenge_widget_info.xml` | Se rehace la preview. Se añade un tamaño por defecto. |
| Picker de configuración | `ChallengeWidgetConfigActivity.kt` | Usa `challenge_widget_versus`. Se conserva. |
| Tests | `ChallengeWidgetSnapshotTest.kt`, `ChallengeLeaderTest.kt` | Se amplían. |
| Dependencias | `glance-appwidget` / `glance-material3` 1.1.1 | Tienen `SizeMode.Responsive`, `LocalSize` y `LinearProgressIndicator`. Sin dependencias nuevas. |

**Convenciones y patrón de referencia:**
- Glance con `ColorProvider` de los tokens `Habit*`.
- Textos con `context.getString`.
- La preview solo usa vistas de framework permitidas en `RemoteViews`.
- Hay notas en el código sobre el reciclado de vistas y los paddings de Glance.

**Skill `glance-canonical-layouts`:** consultada. Sirve para copiar los layouts canónicos de ejemplo de Google, y este widget tiene un diseño propio que debe imitar la tarjeta de la app. Solo se aplica su recomendación de declarar `sizeMode` explícito.

## Solución propuesta

<!-- Explica el enfoque y sus motivos. Describe las responsabilidades y el
recorrido de datos y eventos hasta la interfaz. Usa un diagrama si aporta claridad. -->

**Datos (RF-02 a RF-05, CA-09):**
- `ChallengeWidgetSnapshot` gana estos campos, todos con valor por defecto para que los JSON antiguos se lean sin error: `habitIcon`, `myDisplayName`, `myAvatarColor`, `opponentAvatarColor`, `myScore`, `theirScore` y `scoreTotal`.
- Se rellenan desde `ChallengeCard`, que ya los trae (`habitIcon`, `me`, `opponent.avatarColor` y los `*Score`).
- Si `scoreTotal` es 0 (snapshot antiguo), las barras usan el porcentaje de progreso (`60/100`) hasta el siguiente refresco.

**Lógica pura y testeable** (`presentation/widgets/challenge/ChallengeWidgetCard.kt`, nuevo):
- `ChallengeWidgetCardUi`: todo lo que pinta la tarjeta, ya resuelto.
  - Nombres, iniciales y colores de avatar.
  - Ganador.
  - Puntuación, fracción de barra y si va destacada.
  - Texto de días restantes y si está pendiente.
- `snapshot.toCardUi(strings)` lo calcula con el mismo criterio que la app:
  - ganador con `challengeLeader(myProgress, theirProgress)`;
  - barra destacada si su participante no va por detrás;
  - días con el plural `friends_days_remaining`, o `friends_challenge_last_day` si quedan 0.
- **Iniciales:** `avatarInitials(displayName, fallback)` pasa a ser una función pura en `ProfileAvatar.kt`. `userAvatarInitials` la usa por dentro, así que la app no cambia de comportamiento.

**Interfaz Glance (RF-01 a RF-07):**
- `sizeMode = SizeMode.Responsive(setOf(COMPACT, FULL))`, con `COMPACT = 110×110 dp` y `FULL = 220×220 dp`. `LocalSize` decide qué pintar (RF-07).
- **Raíz:** fondo `HabitCream` (el `surfaceContainerLowest` claro de la app), `appWidgetBackground()` y esquinas del sistema. Sin cabecera verde (RF-01).
- **Cabecera:** caja de 32 dp con el icono del hábito sobre su color al 12 % y el nombre en negrita, máximo 2 líneas en la versión completa y 1 en la compacta.
- **Participantes** (solo en FULL):
  - Avatar de 44 dp: anillo del color del hábito si gana, o `HabitOutline` si no, y dentro un círculo de 40 dp del color del avatar con las iniciales en crema. Se usa el óvalo tintable `widget_check_circle`.
  - Insignia con copa (óvalo del color del hábito de 18 dp y copa blanca) arriba a la derecha, en una capa superpuesta.
  - Nombre debajo, con puntos suspensivos si no cabe.
  - «VS» en negrita y grande, en el color del hábito, entre los dos.
- **Barras:**
  - Fila con el nombre y «puntos/total» (`friends_challenge_score`), en el color del hábito o en `HabitOnSurfaceVariant` si va por detrás.
  - Debajo, `LinearProgressIndicator` de 8 dp con esquinas redondeadas: color del hábito, o el mismo al 55 % si va por detrás; fondo `HabitOutline` al 35 %.
  - Descripción para el lector de pantalla con `friends_challenge_score_description`.
- **Pie:** icono de reloj nuevo (`ic_widget_schedule.xml`, vector Material «schedule» rounded) y el texto de días. «Pendiente» en el color del hábito si el reto está pendiente (RF-06).
- **COMPACT:** cabecera, las dos barras y el pie, con paddings más pequeños.
- Los estados de carga y «Configurar desafío» y las acciones de tap no cambian.

**Preview (RF-08):**
- `challenge_widget_preview.xml` se rehace con la tarjeta completa y datos de ejemplo:
  - Run, tú contra Alex, 4/7 frente a 3/7, quedan 5 días;
  - vas ganando tú, así que la copa va en tu avatar.
- Avatares y anillos con óvalos y `backgroundTint`. Barras con `ProgressBar` horizontal y un `progressDrawable` redondeado nuevo.
- `challenge_widget_info.xml` añade `targetCellWidth="4"` y `targetCellHeight="3"` (Android 12+), para que al colocarlo salga por defecto la tarjeta completa. `minWidth` y `minHeight` no cambian.

**Textos (RF-09):** se reutilizan `friends_challenge_vs`, `friends_challenge_score`, `friends_days_remaining`, `friends_challenge_last_day`, `friends_me`, `friends_challenge_leader` y `challenge_widget_status_pending`, que ya existen en EN y ES. Nuevos, solo los de la preview: nombres de ejemplo, iniciales y puntuaciones, en EN y ES. Se borran los de la preview anterior y `challenge_widget_progress`, `challenge_widget_days_left` y `challenge_widget_preview_*` si quedan sin uso. `challenge_widget_versus` se queda porque lo usa el picker.

## Módulos y componentes afectados

<!-- Si el proyecto está modularizado, identifica los módulos afectados, sus
responsabilidades y la dirección de sus dependencias. Respeta los límites
existentes y justifica cualquier módulo o dependencia nueva. Si no está
modularizado, describe las carpetas o componentes afectados sin introducir
modularización fuera del alcance; marca la tabla de módulos como No aplica. -->

| Módulo | Existe / nuevo | Responsabilidad y cambios | Dependencias afectadas |
| --- | --- | --- | --- |
| No aplica | — | Módulo único `:app` | — |

<!-- Distingue lo que se reutiliza, modifica o crea. Las rutas nuevas son propuestas.
Señala impacto sobre modelos, contratos o componentes compartidos. -->

| Componente o ruta | Acción | Cambio y responsabilidad | Requisito relacionado |
| --- | --- | --- | --- |
| `widgets/challenge/ChallengeGlanceWidget.kt` | Modificar | Campos del snapshot, `sizeMode` y nuevo `ChallengeWidgetContent` (completo y compacto) | RF-01 a RF-07 |
| `widgets/challenge/ChallengeWidgetCard.kt` | Crear | `ChallengeWidgetCardUi` y `toCardUi` (lógica pura) | RF-02 a RF-06, CA-09 |
| `components/ProfileAvatar.kt` | Modificar | `avatarInitials` pura; `userAvatarInitials` la usa | RF-03 |
| `res/drawable/ic_widget_schedule.xml`, `widget_preview_progress.xml` | Crear | Icono de reloj y barra de la preview | RF-06, RF-08 |
| `res/layout/challenge_widget_preview.xml` | Reescribir | Preview con la tarjeta | RF-08 |
| `res/xml/challenge_widget_info.xml` | Modificar | Tamaño por defecto 4×3 | RF-07 |
| `res/values{,-es}/strings.xml` | Modificar | Textos de la preview; borrar los que queden sin uso | RF-09 |
| `ChallengeWidgetSnapshotTest.kt` | Modificar | Campos nuevos y JSON antiguo | CA-09 |
| `ChallengeWidgetCardTest.kt` | Crear | Ganador, atenuación, empate, días, pendiente e iniciales | CA-03 a CA-05 |
| `spec.md` (§ Home screen widgets) | Modificar | Nuevo aspecto del widget de reto y de su preview | Fuente de verdad |

## Datos y contratos

<!-- Completa solo lo aplicable. Si un punto no aplica, indica el motivo. -->

- **Modelos y contratos de entrada y salida:** `ChallengeWidgetSnapshot` añade 7 campos opcionales con valor por defecto.
- **Identificadores, relaciones y restricciones:** no cambian.
- **Origen de los datos mostrados y transformaciones:** `ChallengeCard` → snapshot → `ChallengeWidgetCardUi` → Glance.
- **Persistencia, consultas y actualizaciones:** el mismo JSON en DataStore (`WidgetPreferencesDataSource`), sin cambios de esquema.
- **Convivencia entre datos locales y remotos:** igual que ahora: con red se refresca el snapshot y sin red se usa el guardado.
- **Compatibilidad y migraciones de datos existentes:** los JSON antiguos se leen con los valores por defecto (el decoder ya usa `ignoreUnknownKeys`, y los campos nuevos tienen valor por defecto). Se cubre con un test (CA-09).

## Estado, operaciones y errores

<!-- Cómo se implementan los comportamientos aprobados en la spec.
Referencia RF/CA y aplica las consideraciones relevantes de MOBILE_GUIDELINES.md. -->

- **Gestión del estado de interfaz y navegación:** sin cambios: `WidgetLoad` y las mismas acciones de tap.
- **Conservación y restauración del estado:** no aplica.
- **Ejecución, concurrencia y cancelación de operaciones:** sin cambios en la carga.
- **Errores, reintentos y prevención de duplicados:** un snapshot ilegible sigue mostrando «Configurar desafío», como ahora. Si el icono es desconocido, se usa el icono por defecto de `habitIconRes`.
- **Otras consideraciones mobile aplicables y su solución:**
  - `maxLines` y puntos suspensivos en los nombres.
  - En Android 8–11, las esquinas de las barras pueden salir rectas, porque `cornerRadius` de Glance solo funciona en 12+. Es aceptable.
  - Se mantienen las descripciones para el lector de pantalla.

## Dependencias y configuración

<!-- Librerías, servicios, permisos o configuración afectados. Verifica compatibilidad
con el proyecto y justifica las incorporaciones. No agregues dependencias por defecto. -->

- Ninguna dependencia nueva.
- `targetCellWidth` y `targetCellHeight` en el `appwidget-provider` (atributos de Android 12+; en versiones anteriores se ignoran).

## Estrategia de validación

<!-- Una fila por criterio de la spec. Selecciona el método capaz de demostrarlo:
test unitario, integración, UI o prueba manual. No todos requieren todos los métodos.
Identifica tests existentes y separa los nuevos propuestos. Incluye regresiones relevantes.
Una captura aislada no demuestra persistencia ni ausencia de peticiones de red. -->

| Criterio | Método y test existente o propuesto | Entorno y datos necesarios | Evidencia prevista |
| --- | --- | --- | --- |
| CA-01, CA-02 | Manual en dispositivo: widget grande con un reto | Móvil conectado por ADB (hay uno) + cuenta con reto | Captura `adb screencap` |
| CA-03 | Test nuevo `toCardUi`: Me / Opponent / Tie → insignia y anillo. Manual con un reto real | JVM + dispositivo | `testDebugUnitTest` + captura |
| CA-04 | Test nuevo: puntos/total, fracción, atenuación y empate | JVM | `testDebugUnitTest` |
| CA-05 | Test nuevo: «Quedan N días», «Último día» y pendiente | JVM | `testDebugUnitTest` |
| CA-06 | Manual: redimensionar a 2×2 y a grande | Dispositivo | Capturas de los dos tamaños |
| CA-07 | Manual: selector de widgets | Dispositivo Android 12+ | Captura |
| CA-08 | Manual con el móvil en español, y revisión de `values-es` | Dispositivo | Captura |
| CA-09 | Test nuevo: decodificar un JSON antiguo sin los campos nuevos | JVM | `testDebugUnitTest` |

**Comprobaciones de regresión:**
- Los tests actuales de snapshot y ganador.
- El texto de iniciales de la app no cambia (test de `avatarInitials`).
- El widget de hoy se ve igual.
- `assembleDebug` y `lintDebug` sin errores de `RemoteViews`.

**Comandos verificados para compilar y ejecutar tests:** `./gradlew :app:testDebugUnitTest :app:assembleDebug`. Instalación con `./gradlew :app:installDebug`.

**Pruebas en dispositivo, emulador o simulador:** hay un móvil conectado por ADB inalámbrico y un AVD `Medium_Phone`. Colocar y redimensionar el widget requiere interacción en el launcher: la propietaria lo coloca y yo tomo las capturas con `adb`.

**Limitaciones del entorno:** no se puede automatizar colocar el widget en el launcher. No se comprobará en Android 8–11 salvo que haya un dispositivo con esas versiones.

<!-- Esta sección planifica la validación. Durante la implementación, registra
en TASKS.md o en el informe de validación acordado los resultados y evidencias
reales. Distingue pruebas ejecutadas, fallidas, no ejecutadas y bloqueadas.
Compilar o tener tests en verde no sustituye revisar los criterios de la spec. -->

## Orden de implementación

<!-- Etapas y dependencias principales. El desglose ejecutable se escribe en TASKS.md.
Incluye puntos de comprobación para avanzar con cambios pequeños. -->

1. **Snapshot y `avatarInitials`** + tests. Comprobación: `testDebugUnitTest`.
2. **`ChallengeWidgetCardUi` / `toCardUi`** + tests. Comprobación: `testDebugUnitTest`.
3. **Interfaz Glance** (completa y compacta) e icono de reloj. Comprobación: `assembleDebug`.
4. **Preview, `widget_info` y textos.** Comprobación: `assembleDebug` y `lintDebug`.
5. **`spec.md`.**
6. **Validación en el dispositivo** con capturas (requiere a la propietaria para colocar el widget).

## Riesgos y decisiones pendientes

<!-- Riesgos concretos de esta solución y cómo se resolverán, sin listas genéricas.
Escribe Ninguna en las decisiones pendientes cuando estén resueltas. -->

- **Riesgos y medidas acordadas:**
  - *Espacio:* la tarjeta completa necesita unos 220 dp de alto. Por debajo se pinta la compacta (umbral `FULL`). Si en el dispositivo no cabe, se ajusta el umbral en la validación.
  - *Superposición de la copa:* Glance no tiene `offset`. La insignia se coloca con una capa superpuesta alineada arriba a la derecha dentro de la caja del avatar, así que puede quedar unos dp más adentro que en la app.
  - *Grosor del «VS»:* Glance solo admite Normal, Medium y Bold, no Black. Se usa Bold con un tamaño mayor.
  - *Borde de la tarjeta:* el borde de 1 dp al 8 % de la app no se reproduce en Glance; la diferencia es casi invisible.
- **Decisiones pendientes:** Ninguna.
