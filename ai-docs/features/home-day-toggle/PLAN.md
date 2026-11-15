# PLAN: Marcar días desde Inicio, no desde el detalle

**SPEC de referencia:** `ai-docs/features/home-day-toggle/SPEC.md`
**Versión de la spec revisada:** Aprobada el 2026-11-15, sobre `main` `dc15b20`
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
| `HomeViewModel.toggleComplete` | `app/src/main/java/com/marta/habittracker/presentation/screens/home/HomeViewModel.kt` | Hoy sale si `selectedDate != today`. Se abre a días ≤ hoy y se añade el bloqueo mientras guarda. |
| `HomeHabitCard` | `.../presentation/screens/home/HomeScreen.kt` | `clickable(enabled = isToday)` y `alpha(0.55f)` si no es hoy y no está hecho. Pasa a depender de «es futuro» y «está guardando». |
| `HabitRepository.toggleHabitCompletion(habitId, wasCompleted, date)` | `domain/repository`, `data/repository/HabitRepositoryImpl.kt` | Guardado online-first ya usado por Inicio y el detalle; refresca el widget de hoy. Se reutiliza sin cambios. |
| `HomeEvent.HabitToggle` + snackbar | `HomeViewModel.kt`, `HomeScreen.kt` | Error del guardado. Se reutiliza (RF-06). |
| `HabitDetailViewModel.toggleDay`, `savingDates`, `HabitDetailEvent.ToggleFailed` | `.../presentation/screens/detail/HabitDetailViewModel.kt` | Edición desde el calendario. Se elimina (RF-07). |
| `DetailCalendar` + snackbar | `.../presentation/screens/detail/HabitDetailScreen.kt` | Pasa `onDayClick`/`pendingDates` y pinta `detail_calendar_hint`. Se quita. |
| `MonthCalendar(onDayClick, pendingDates)` | `.../presentation/components/MonthCalendar.kt` | Solo lo usa el detalle. Los dos parámetros quedan sin uso y se eliminan. |
| `detail_calendar_hint` | `res/values/strings.xml`, `res/values-es/strings.xml` | Se elimina. |
| Tests | `HomeViewModelTest.kt`, `HabitDetailViewModelTest.kt`, `FakeHabitRepository` | Se añaden tests de Inicio y se quitan los de `toggleDay`. |

**Convenciones y patrón de referencia:** el bloqueo mientras guarda copia el patrón de `savingDates` del detalle (`MutableStateFlow<Set<…>>` + `try/finally`). Eventos one-shot con `Channel` + `ObserveAsEvents` (ya existentes). Separación `HomeScreen`/`HomeContent`.

## Solución propuesta

<!-- Explica el enfoque y sus motivos. Describe las responsabilidades y el
recorrido de datos y eventos hasta la interfaz. Usa un diagrama si aporta claridad. -->

Mover la edición del detalle a Inicio sin tocar datos ni repositorio.

1. **HomeViewModel:**
   - `toggleComplete(habit)` captura `date = selectedDate.value`; sale si `date.isAfter(today.value)` (RF-03) o si `habit.id to date` ya se está guardando (RF-05).
   - Añade `savingToggles: StateFlow<Set<Pair<String, LocalDate>>>`; marca la clave antes de llamar a `toggleHabitCompletion(habit.id, habit.isCompleted, date)` y la quita en `finally`.
   - El resultado sigue saliendo como `HomeEvent.HabitToggle` (RF-06). En error se añade `Log.e(TAG, …)` (regla de logging del proyecto), sin datos sensibles.
2. **HomeScreen / HomeContent:** recoge `savingToggles` y pasa a cada `HomeHabitCard` `isFuture = selectedDate.isAfter(today)` e `isSaving`. La tarjeta usa `clickable(enabled = !isFuture && !isSaving)` (RF-01, RF-02, RF-03, RF-05) y atenúa solo si `isFuture && !isDone` (RF-04). La semántica de clic de Compose expone las filas pasadas al lector de pantalla igual que la de hoy.
3. **Detalle:** se quitan `toggleDay`, `savingDates`, `HabitDetailEvent`/`events` (su único evento era `ToggleFailed`), el snackbar del detalle si queda sin uso, la línea de ayuda y los parámetros `onDayClick`/`pendingDates` de `MonthCalendar` (RF-07). El calendario sigue mostrando los días hechos y las flechas de mes.
4. **spec.md:** actualizar Detalle (calendario solo de consulta) e Inicio (marcar hoy y días pasados de la semana) (RF-08).

Rachas, protecciones, conteos, porcentaje de la fila de la semana y el detalle se recalculan solos porque todos derivan del flujo de Room que actualiza el repositorio (CA-09).

## Módulos y componentes afectados

<!-- Si el proyecto está modularizado, identifica los módulos afectados, sus
responsabilidades y la dirección de sus dependencias. Respeta los límites
existentes y justifica cualquier módulo o dependencia nueva. Si no está
modularizado, describe las carpetas o componentes afectados sin introducir
modularización fuera del alcance; marca la tabla de módulos como No aplica. -->

| Módulo | Existe / nuevo | Responsabilidad y cambios | Dependencias afectadas |
| --- | --- | --- | --- |
| No aplica (módulo único `app`) | — | — | — |

<!-- Distingue lo que se reutiliza, modifica o crea. Las rutas nuevas son propuestas.
Señala impacto sobre modelos, contratos o componentes compartidos. -->

| Componente o ruta | Acción | Cambio y responsabilidad | Requisito relacionado |
| --- | --- | --- | --- |
| `screens/home/HomeViewModel.kt` | Modificar | Días ≤ hoy, bloqueo por hábito y día, log de error | RF-01, RF-02, RF-03, RF-05, RF-06 |
| `screens/home/HomeScreen.kt` | Modificar | `isFuture`/`isSaving` en la tarjeta, sin atenuar en pasados | RF-01 a RF-05 |
| `screens/detail/HabitDetailViewModel.kt` | Modificar | Quitar edición del calendario | RF-07 |
| `screens/detail/HabitDetailScreen.kt` | Modificar | Calendario sin clic, sin ayuda, sin snackbar de toggle | RF-07 |
| `components/MonthCalendar.kt` | Modificar | Quitar `onDayClick`/`pendingDates` (sin uso) | RF-07 |
| `res/values*/strings.xml` | Modificar | Quitar `detail_calendar_hint` | RF-07 |
| `HomeViewModelTest.kt` | Modificar | Tests nuevos | CA-01 a CA-07 |
| `HabitDetailViewModelTest.kt` | Modificar | Quitar tests de `toggleDay` | RF-07 |
| `spec.md` | Modificar | Documentar el cambio | RF-08 |

## Datos y contratos

<!-- Completa solo lo aplicable. Si un punto no aplica, indica el motivo. -->

- **Modelos y contratos de entrada y salida:** sin cambios en dominio ni repositorio.
- **Identificadores, relaciones y restricciones:** la clave de guardado es `(habitId, fecha)`.
- **Origen de los datos mostrados y transformaciones:** el mismo flujo `getHabitsWithStatus(fecha)`; `habit.isCompleted` es el estado de ese día.
- **Persistencia, consultas y actualizaciones:** sin cambios; `toggleHabitCompletion` ya admite cualquier fecha.
- **Convivencia entre datos locales y remotos:** sin cambios (online-first).
- **Compatibilidad y migraciones de datos existentes:** no aplica.

## Estado, operaciones y errores

<!-- Cómo se implementan los comportamientos aprobados en la spec.
Referencia RF/CA y aplica las consideraciones relevantes de MOBILE_GUIDELINES.md. -->

- **Gestión del estado de interfaz y navegación:** `savingToggles` como `StateFlow` en el ViewModel; sin navegación nueva.
- **Conservación y restauración del estado:** el día seleccionado sigue igual que ahora. El estado de guardado es transitorio y no se restaura.
- **Ejecución, concurrencia y cancelación de operaciones:** `viewModelScope`; la fecha se captura al tocar, así que cambiar de día durante el guardado no altera qué día se guarda.
- **Errores, reintentos y prevención de duplicados:** sin reintentos automáticos; el bloqueo por `(habitId, fecha)` evita el doble toque (CA-06); el error sale por el snackbar actual (CA-07).
- **Otras consideraciones mobile aplicables y su solución:** accesibilidad por la semántica de `clickable`; sin textos nuevos.

## Dependencias y configuración

<!-- Librerías, servicios, permisos o configuración afectados. Verifica compatibilidad
con el proyecto y justifica las incorporaciones. No agregues dependencias por defecto. -->

- Ninguna.

## Estrategia de validación

<!-- Una fila por criterio de la spec. Selecciona el método capaz de demostrarlo:
test unitario, integración, UI o prueba manual. No todos requieren todos los métodos.
Identifica tests existentes y separa los nuevos propuestos. Incluye regresiones relevantes.
Una captura aislada no demuestra persistencia ni ausencia de peticiones de red. -->

| Criterio | Método y test existente o propuesto | Entorno y datos necesarios | Evidencia prevista |
| --- | --- | --- | --- |
| CA-01 | Test nuevo `HomeViewModelTest`: día pasado sin hacer → `toggleHabitCompletion(id, false, ayer)`; manual en el móvil | Fake repo; móvil | Test en verde; revisión en el móvil |
| CA-02 | Test nuevo: día pasado hecho → `wasCompleted = true` | Fake repo | Test en verde |
| CA-03 | Test nuevo: hoy sigue guardando | Fake repo | Test en verde |
| CA-04 | Test nuevo: día futuro no llama al repo | Fake repo | Test en verde |
| CA-05 | Manual en el móvil | Móvil | Revisión en el móvil |
| CA-06 | Test nuevo: dos toques seguidos → una llamada | Fake repo | Test en verde |
| CA-07 | Test nuevo: error → `HomeEvent.HabitToggle` con error; manual en modo avión | Fake repo; móvil | Test en verde; revisión |
| CA-08 | Compilación (sin `onDayClick`) + manual | Móvil | Build; revisión |
| CA-09 | Manual: marcar ayer en Inicio y abrir el detalle | Móvil | Revisión |

**Comprobaciones de regresión:** tests existentes de Home y Detalle (cambio de día a medianoche, carga, protecciones).

**Comandos verificados para compilar y ejecutar tests:** `./gradlew :app:testDebugUnitTest`, `./gradlew :app:assembleDebug`, `./gradlew :app:lintDebug`, `./gradlew :app:installDebug`.

**Pruebas en dispositivo, emulador o simulador:** CA-01, CA-05, CA-07, CA-08, CA-09 en el móvil conectado por ADB.

**Limitaciones del entorno:** la revisión visual en el móvil la hace la propietaria.

<!-- Esta sección planifica la validación. Durante la implementación, registra
en TASKS.md o en el informe de validación acordado los resultados y evidencias
reales. Distingue pruebas ejecutadas, fallidas, no ejecutadas y bloqueadas.
Compilar o tener tests en verde no sustituye revisar los criterios de la spec. -->

## Orden de implementación

<!-- Etapas y dependencias principales. El desglose ejecutable se escribe en TASKS.md.
Incluye puntos de comprobación para avanzar con cambios pequeños. -->

1. `HomeViewModel` + tests (comprobación: `testDebugUnitTest`).
2. `HomeScreen` (comprobación: `assembleDebug`).
3. Detalle, `MonthCalendar`, strings y tests del detalle (comprobación: `testDebugUnitTest`, `assembleDebug`, `lintDebug`).
4. `spec.md`.
5. Instalar y revisar en el móvil.

## Riesgos y decisiones pendientes

<!-- Riesgos concretos de esta solución y cómo se resolverán, sin listas genéricas.
Escribe Ninguna en las decisiones pendientes cuando estén resueltas. -->

- **Riesgos y medidas acordadas:** un toque mientras cambia el día seleccionado guardaría un día distinto del que se ve; se evita capturando la fecha al tocar. `habit.isCompleted` debe corresponder a la fecha capturada: la lista que se toca es la del día seleccionado, así que coinciden.
- **Decisiones pendientes:** Ninguna.

<!-- ANTES DE SOLICITAR APROBACIÓN
Comprueba que el plan cubre los requisitos, respeta las exclusiones, reutiliza
componentes verificados y permite demostrar todos los criterios de aceptación.
Resuelve dudas y marcadores pendientes. Si la spec cambió, revisa su impacto.
Tras aprobar el plan, deriva TASKS.md con IDs, dependencias, referencias a RF/CA
y comprobaciones. No marques una tarea terminada sin realizar su validación;
si está bloqueada, registra el motivo.
-->
