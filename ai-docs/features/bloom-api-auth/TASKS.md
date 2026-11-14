# TASKS: Proteger la API de Bloom con la sesión del usuario

**SPEC:** `SPEC.md` (Aprobada) · **PLAN:** `PLAN.md` (Aprobado) · **Implementación autorizada:** 2026-11-14

| ID | Objetivo | Alcance | Depende de | RF / CA | Validación | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| T-01 | Validar el token de Supabase en `/api/chat` | `bloom-api/main.py`, `requirements.txt` | — | RF-01, RF-05, RF-07 | T-02 | Hecha |
| T-02 | Tests de la API | `bloom-api/tests/test_auth.py`, `requirements-dev.txt` | T-01 | CA-01, CA-02, CA-07, CA-09 | pytest (local y en imagen 3.12) | Hecha |
| T-03 | Concurrencia, presupuesto y parámetros en SAM | `bloom-api/template.yaml` | T-01 | RF-06 | Revisión + build de la imagen e invocación local (`/health` 200, `/api/chat` 401) | Hecha |
| T-04 | Proveedor de tokens y transporte con cabecera | `BloomAccessTokens.kt`, `SupabaseSession.kt`, `RecommendationTransport.kt`, `KtorAITransport.kt`, `BloomApiModule.kt` | — | RF-02 | `testDebugUnitTest` | Hecha |
| T-05 | 401, reintento único y sin sesión en el DataSource | `AIDataSource.kt`, `RecommendationError.kt` + tests | T-04 | RF-02–RF-04, RF-07 / CA-03–CA-06 | `testDebugUnitTest` | Hecha |
| T-06 | Texto de sesión EN/ES y mapeos | `strings.xml` (EN/ES), `RecommendationViewModel.kt`, `AppErrorUiMessages.kt` + test | T-05 | RF-03, RF-04 / CA-06 | `testDebugUnitTest`, `assembleDebug` | Hecha |
| T-07 | Documentar en `spec.md` | `spec.md` | T-06 | — | Revisión | Hecha |
| T-08 | Desplegar y probar en dispositivo | AWS + móvil | T-01–T-07 | CA-02, CA-08 | `sam deploy`, `curl`, prueba en dispositivo | Pendiente (propietaria) |

## Resultados de validación

Ejecutado el 2026-11-14:

| Comprobación | Resultado | Criterios |
| --- | --- | --- |
| `pytest tests` en `python:3.12-slim` (16 tests) | Pasa: 16/16 | CA-01, CA-02, CA-07, CA-09 (API) |
| `cfn-lint template.yaml` | Pasa sin avisos | RF-06 (plantilla) |
| Imagen `Dockerfile.lambda` invocada con el emulador de Lambda y `SUPABASE_URL` real | `/health` 200; `/api/chat` sin token 401; token falso 401 (`InvalidSignatureError` contra el JWKS real); el log no muestra el token | CA-01, CA-07, CA-09 |
| `./gradlew :app:testDebugUnitTest :app:assembleDebug` | Pasa: 326 tests, 0 fallos (DataSource 12, ViewModel 6); APK debug compilado | CA-03 a CA-06, CA-09 (app) |
| Revisión de los `Log.*` nuevos | Ninguno incluye el token | CA-09 |

**Pendiente (no ejecutable en este entorno):**
- CA-02 contra la API desplegada con un token real (`curl`).
- CA-08: `sam deploy` y comprobar en la consola la concurrencia reservada y el presupuesto.
- Prueba en dispositivo enviando un mensaje a Bloom.
- No hay AWS CLI/SAM ni credenciales aquí.
- La skill `mobiai-mobile-verification`, que exige la SDD, no existe en el repositorio. La validación se ha hecho con los checks anteriores.

## Cambio durante el despliegue (2026-11-14)

- La organización de AWS de la cuenta solo deja desplegar en `eu-north-1`. En esa región, CloudFormation rechaza `AWS::Budgets::Budget` ("Unrecognized resource types").
- Se quita el presupuesto (y los parámetros `AlertEmail` y `MonthlyBudgetUsd`) de `template.yaml`. La alerta de 5 USD/mes de RF-06 se crea aparte en AWS Budgets (consola o CLI). El comportamiento de la spec no cambia; solo cambia dónde se define.
- `cfn-lint` sobre la plantilla nueva: pasa.
- La cuenta tiene un límite total de concurrencia de 10 y Lambda exige dejar 10 sin reservar, así que reservar 5 falla. Se despliega con `MaxConcurrentExecutions=0` (sin tope), que es la alternativa prevista en la SPEC. RF-06 queda cubierto solo por la alerta de presupuesto, si la cuenta permite crearla. El límite de 10 de la cuenta actúa en la práctica como tope.
- Desplegado en `eu-north-1` (stack `bloom-api`). Comprobación contra la URL real: `GET /health` → 200; `POST /api/chat` sin token → 401 «Falta la sesión.»; con token falso → 401 «Sesión no válida.» (CA-01 y CA-07 en producción). Pendiente: CA-02 con un token real desde la app.
