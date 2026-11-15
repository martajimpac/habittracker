# TASKS: Marcar días desde Inicio, no desde el detalle

**SPEC:** `SPEC.md` (Aprobada) · **PLAN:** `PLAN.md` (Aprobado) · **Implementación autorizada:** 2026-11-15

| ID | Objetivo | Alcance | Depende de | RF / CA | Validación | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| T-01 | Inicio guarda hoy y días pasados, con bloqueo mientras guarda | `HomeViewModel.kt` + `HomeViewModelTest.kt` | — | RF-01, RF-02, RF-03, RF-05, RF-06 / CA-01 a CA-04, CA-06, CA-07 | `testDebugUnitTest` | Hecha |
| T-02 | Tarjeta de Inicio tocable en pasados y sin atenuar | `HomeScreen.kt` | T-01 | RF-01 a RF-05 / CA-05 | `assembleDebug` | Hecha |
| T-03 | Calendario del detalle solo de consulta | `HabitDetailViewModel.kt`, `HabitDetailScreen.kt`, `MonthCalendar.kt`, `strings.xml` EN/ES, `HabitDetailViewModelTest.kt` | — | RF-07 / CA-08 | `testDebugUnitTest`, `assembleDebug`, `lintDebug` | Hecha |
| T-04 | Documentar en `spec.md` | `spec.md` | T-02, T-03 | RF-08 | Revisión | Hecha |
| T-05 | Validación en dispositivo | Móvil por ADB | T-04 | CA-01, CA-05, CA-07, CA-08, CA-09 | Revisión en el móvil | Hecha |

## Resultados de validación

Ejecutado el 2026-11-15:

| Comprobación | Resultado | Criterios |
| --- | --- | --- |
| `./gradlew :app:testDebugUnitTest` | Pasa: 338 tests, 0 fallos. `HomeViewModelTest` 12 (6 nuevos: día pasado, desmarcar, hoy, futuro, doble toque, error) | CA-01 a CA-04, CA-06, CA-07 |
| `./gradlew :app:assembleDebug` | Compila; el calendario ya no recibe clics (CA-08 por compilación) | RF-01 a RF-07 |
| `./gradlew :app:lintDebug` | 13 errores, todos anteriores (`AvatarJpegEncoder`, `themes.xml:8-9`, `MonthCalendar` `NonObservableLocale` en la descripción del día, `HomeScreen` `LocalContextGetResourceValueCall` del snackbar). Desaparece el de `HabitDetailScreen` al quitar su snackbar | Regresión |

**Cambios respecto al plan:**
- Se quitan también `isMonthCalendarDayEditable`, sus tests y `detail_calendar_toggle_action`, que solo servían para editar desde el calendario.
- El calendario conserva la descripción «día, hecho / no hecho» para el lector de pantalla, ahora sin acción.

**T-05 (2026-11-15):** la propietaria lo probó en su móvil y funciona. Quedan cubiertos CA-01, CA-05, CA-07, CA-08 y CA-09.
