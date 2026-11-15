# TASKS: Widget de reto con el diseño de la tarjeta de la app

**SPEC:** `SPEC.md` (Aprobada) · **PLAN:** `PLAN.md` (Aprobado) · **Implementación autorizada:** 2026-11-14

| ID | Objetivo | Alcance | Depende de | RF / CA | Validación | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| T-01 | Campos nuevos del snapshot y `avatarInitials` pura | `ChallengeGlanceWidget.kt` (snapshot), `ProfileAvatar.kt` + tests | — | RF-02, RF-03 / CA-09 | `testDebugUnitTest` | Hecha |
| T-02 | `ChallengeWidgetCardUi` / `toCardUi` | `ChallengeWidgetCard.kt` + `ChallengeWidgetCardTest.kt` | T-01 | RF-02 a RF-06 / CA-03 a CA-05 | `testDebugUnitTest` | Hecha |
| T-03 | Interfaz Glance completa y compacta + icono de reloj | `ChallengeGlanceWidget.kt`, `ic_widget_schedule.xml` | T-02 | RF-01 a RF-07 | `assembleDebug` | Hecha |
| T-04 | Preview, tamaño por defecto y textos | `challenge_widget_preview.xml`, `widget_preview_progress.xml`, `challenge_widget_info.xml`, `strings.xml` EN/ES | T-03 | RF-07 a RF-09 | `assembleDebug`, `lintDebug` | Hecha |
| T-05 | Documentar en `spec.md` | `spec.md` | T-04 | — | Revisión | Hecha |
| T-06 | Validación en dispositivo | Móvil por ADB | T-04 | CA-01 a CA-08 | Revisión en el móvil | Hecha |

## Resultados de validación

Ejecutado el 2026-11-14:

| Comprobación | Resultado | Criterios |
| --- | --- | --- |
| `./gradlew :app:testDebugUnitTest` | Pasa: 338 tests, 0 fallos (`ChallengeWidgetCardTest` 10, `ChallengeWidgetSnapshotTest` 6, `ChallengeLeaderTest` 3) | CA-03, CA-04, CA-05, CA-09 y las iniciales de RF-03 |
| `./gradlew :app:assembleDebug` | Compila | RF-01 a RF-08 (compilación) |
| `./gradlew :app:lintDebug` | 14 errores, todos anteriores y en archivos no tocados (`AvatarJpegEncoder`, `themes.xml:8-9`, `MonthCalendar`, `HabitDetailScreen`, `HomeScreen`). En los archivos del cambio solo hay avisos esperados: `targetCellWidth`/`targetCellHeight` solo en API 31+ | Regresión |

**Cambios respecto al plan:**
- Se quitan `challengeLeaderLabel` y `friends_challenge_tie`, que solo usaba el widget anterior (código muerto tras el cambio), además de los recursos de las barras segmentadas.
- La descripción para el lector de pantalla reutiliza `friends_challenge_score_description`.

**T-06 (2026-11-15):** build debug instalada en el móvil (Android 13). La propietaria revisó en el dispositivo la preview del selector, el widget colocado y los tamaños: se ve bien. Quedan cubiertos CA-01, CA-02, CA-06, CA-07 y CA-08.
