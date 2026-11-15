# TASKS: Órdenes en el chat de Bloom: crear hábitos y marcar días

**SPEC:** `SPEC.md` (Aprobada) · **PLAN:** `PLAN.md` (Aprobado) · **Implementación autorizada:** 2026-11-15 (incluye el despliegue de la Lambda)

| ID | Objetivo | Alcance | Depende de | RF / CA | Validación | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| T-01 | La API acepta el contexto ampliado y `habits` vacía, ofrece las herramientas y devuelve `{message, action?}` validada, con el historial de propuestas | `bloom-api/main.py`, `tests/test_actions.py` | — | RF-01, RF-02, RF-08 a RF-13, RF-15 a RF-19, RF-22 / CA-01, CA-02, CA-07 a CA-10, CA-13, CA-15, CA-20 a CA-22 | `pytest` (nuevos + `test_auth.py`) | Hecha |
| T-02 | Modelos de dominio de acciones y contexto del chat | `domain/model/AssistantTurn.kt`, `AssistantAction.kt`, `errors/AssistantActionError.kt`, `HabitWeekRecommendationStats.kt` (+ `ChatContext`) + test | — | RF-10, RF-15 | `testDebugUnitTest` | Hecha |
| T-03 | `ResolveAssistantAction`: valida la propuesta con las reglas de Inicio y los catálogos | `domain/usecase/ResolveAssistantAction.kt` + test | T-02 | RF-11, RF-16, RF-18, RF-19 / CA-13, CA-14, CA-16, CA-23 | `testDebugUnitTest` | Hecha |
| T-04 | `ExecuteAssistantAction`: revalida y guarda con `SaveHabit` o `toggleHabitCompletion` | `domain/usecase/ExecuteAssistantAction.kt` + test | T-03 | RF-03, RF-06, RF-20, RF-21 / CA-03, CA-11, CA-12, CA-17, CA-18 | `testDebugUnitTest` | Hecha |
| T-05 | Contrato de red: DTO, mapper, DataSource y repositorio devuelven `ChatReply` | `data/remote/ai/dto/*`, `data/mapper/AssistantActionMapper.kt`, `AIDataSource.kt`, `RecommendationRepository*.kt` + test | T-02 | RF-08, RF-15 / CA-20, CA-23 | `testDebugUnitTest` | Hecha |
| T-06 | ViewModel: chat sin hábitos, confirmar, cancelar, evitar duplicados y evento de error | `RecommendationViewModel.kt`, `AppErrorUiMessages.kt` + test | T-04, T-05 | RF-02 a RF-07, RF-22 / CA-03 a CA-06, CA-19, CA-20, CA-22 | `testDebugUnitTest` | Hecha |
| T-07 | UI: `ActionCard`, snackbar, saludo sin hábitos y textos en/es | `RecommendationScreen.kt`, `ActionCard.kt`, `strings.xml` en/es | T-06 | RF-10, RF-15, RF-22 / CA-24, accesibilidad | `assembleDebug`, `lintDebug`, previews | Hecha (previews sin revisar visualmente) |
| T-08 | Desplegar la API en Lambda | `bloom-api` (`sam build` + `sam deploy`) | T-01 | — | `GET /health` y `POST /api/chat` sin token → 401 | Hecha |
| T-09 | Validación manual en emulador o móvil contra la API desplegada | App de depuración | T-07, T-08 | CA-01 a CA-24 (los marcados Manual) | Capturas y notas | Pendiente (la propietaria) |
| T-10 | Documentar en `spec.md` | `spec.md` | T-07 | RF-23 / CA-25 | Revisión del diff | Hecha |

## Resultados de validación

Ejecutado el 2026-11-15:

| Comprobación | Resultado | Criterios |
| --- | --- | --- |
| `pytest` en `bloom-api` (venv local, Python 3.14) | Pasa: 35 tests (23 nuevos en `test_actions.py`; los 12 de `test_auth.py` siguen en verde) | CA-01, CA-02, CA-07 a CA-10, CA-13, CA-15, CA-20 a CA-22 (contrato y validación) |
| `pytest` en contenedor `python:3.12-slim` (versión de Lambda) | Pasa: 35 tests | Ídem |
| `./gradlew :app:testDebugUnitTest` | Pasa: 366 tests, 0 fallos. Nuevos o ampliados: `ResolveAssistantActionTest` 10, `ExecuteAssistantActionTest` 8, `RecommendationViewModelTest` 12, `BloomRecommendationDataSourceTest` 15, `HabitWeekRecommendationStatsTest` 3 | CA-03 a CA-06, CA-10 a CA-14, CA-16 a CA-20, CA-22, CA-23 |
| `./gradlew :app:assembleDebug` | Compila | RF-10, RF-15, RF-22 |
| `./gradlew :app:lintDebug` | 13 errores, todos anteriores (`AvatarJpegEncoder` NewApi, `MonthCalendar` NonObservableLocale, `HomeScreen` LocalContext, `themes.xml`). Ninguno en archivos de esta funcionalidad | Regresión |
| `sam build` + `sam deploy` (stack `bloom-api`, eu-north-1) | `UPDATE_COMPLETE`. La Lambda conserva la clave de Groq, el modelo y `SUPABASE_URL` (comprobado sin leer valores) | T-08 |
| `GET /health` · `POST /api/chat` sin token | 200 `{"status":"ok"}` · 401 | Regresión de auth |

**Pendiente (T-09, prueba manual de la propietaria con la app de depuración, que ya apunta a la Lambda desplegada):**
- Lo que decide el modelo real: CA-01, CA-02, CA-07 a CA-10, CA-13 a CA-16 y CA-21 (propone, pregunta o explica).
- Flujo completo en la app: CA-03, CA-05, CA-06 (modo avión), CA-11 (Inicio y racha), CA-22 (cuenta sin hábitos) y CA-24 (girar el móvil y pasar a segundo plano).
- Revisión visual de la tarjeta (previews de `ActionCard`), con texto grande y con TalkBack.

**Cambios respecto al plan:**
- No se creó `ChatContext.kt`: `ChatContext` vive junto a `HabitWeekStat` en `HabitWeekRecommendationStats.kt`, y `ChatReply`, `ProposedAction` y `HabitCatalog` en `AssistantAction.kt`.
- Las tarjetas se identifican por su posición en la conversación (solo se añaden mensajes), no por un id de turno.
- Los errores nuevos se traducen en `AppErrorUiMessages.kt`, porque el `when` es exhaustivo.
- Los botones de la tarjeta van en una fila con dos mitades (`Row` + `weight`), no en `FlowRow`.
- `bloom-api/` está en `.gitignore`, así que los cambios de la API no quedan versionados en este repositorio.
- La skill `mobiai-mobile-verification` no existe; la verificación sigue el plan.
