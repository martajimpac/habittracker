# Tareas: Chat de hábitos con bloom-api

**SPEC:** `docs/features/habit-ai-recommendations/SPEC.md` (Aprobada, 2026-10-30)
**PLAN:** `docs/features/habit-ai-recommendations/PLAN.md` (Aprobado, 2026-10-30)

Implementación autorizada el 2026-10-30 al pedir cerrar el flujo. Las tareas T1 y T2 de la recomendación de un solo uso siguen vigentes: el cálculo semanal y `BLOOM_API_BASE_URL` no se rehacen.

## T3 — Cliente del chat

- **Objetivo:** Enviar el hilo a `POST /api/chat` y traducir los fallos sin inventar una respuesta.
- **Alcance:** `AssistantTurn`, `RecommendationRepository.send`, `BloomRecommendationDataSource.send` y la delegación del repositorio. No llamar a Ollama desde la app.
- **Depende de:** T1, T2.
- **Requisitos:** RF-02, RF-06, CA-06, CA-09.
- **Validación:** `BloomRecommendationDataSourceTest` en rojo y luego en verde.
- **Estado:** Hecha. El test no compilaba (`send` y `AssistantTurn` no existían). Tras el cliente: 5 tests, 0 fallos, incluido el de `/api/chat` y el de `POST /api/recommendations` (`:app:testPhoneDebugUnitTest`, 2026-10-30).

## T4 — Estado del hilo

- **Objetivo:** Un envío a la vez, reintento sin duplicar el mensaje y ninguna llamada sin hábitos o con texto vacío.
- **Alcance:** `RecommendationViewModel` y su test. El hilo y el borrador quedan en memoria.
- **Depende de:** T3.
- **Requisitos:** RF-01, RF-02, RF-04, RF-05, RF-06, RF-07, RF-08, CA-01, CA-02, CA-04, CA-05, CA-06, CA-07, CA-08.
- **Validación:** `RecommendationViewModelTest`.
- **Estado:** Hecha. El test no compilaba (`onDraftChange`, `send`, `retry`, `ChatPhase`). Después: 5 tests, 0 fallos.

## T5 — Pantalla de chat

- **Objetivo:** Sustituir cifras y botón por la lista, el campo, el error y el reintento.
- **Alcance:** `RecommendationScreen.kt`, cadenas en inglés y español, `adjustResize` en `MainActivity`.
- **Depende de:** T4.
- **Requisitos:** RF-01, RF-03, RF-04, RF-05, RF-07, RF-08, CA-03, CA-08.
- **Validación:** La pantalla no referencia `recommendation_stat`. `assemblePhoneDebug` compila.
- **Estado:** Hecha en código. `WindowInsetsRulers` no está en el BOM de este proyecto; el teclado usa los insets `safeDrawing` del `Scaffold` y `adjustResize`. La compilación se registra en T7.

## T6 — Endpoint en bloom-api

- **Objetivo:** Aceptar el cuerpo del chat y devolver un solo texto.
- **Alcance:** `C:\Users\marta\apis\bloom-api\main.py`. No se elimina `POST /api/recommendations`.
- **Depende de:** ninguna respecto a la app. El contrato es el del plan.
- **Requisitos:** RF-02, RF-03, RF-04, RF-06.
- **Validación:** `python -m py_compile main.py`.
- **Estado:** Hecha. `python -m py_compile main.py` terminó con código 0. No se llamó a Ollama.

## T7 — Verificación

- **Objetivo:** Ejecutar los tests unitarios del chat y compilar la app.
- **Alcance:** `testPhoneDebugUnitTest` y `assemblePhoneDebug`. `testDebugUnitTest` no existe: el sabor `environment` parte la tarea en `testPhoneDebugUnitTest` y `testEmulatorDebugUnitTest`.
- **Depende de:** T3, T4, T5.
- **Requisitos:** CA-01 a CA-09, en la parte que los tests cubren.
- **Validación:** Registrar el comando, el resultado y lo que no se pudo abrir en un dispositivo.
- **Estado:** Parcial. Los 10 tests del chat y del cliente pasaron. La suite `:app:testPhoneDebugUnitTest` completa hizo 326 tests y 1 fallo: `HardcodedSecretsTest` señala `password = "Secret123"` y `password = "Secret12"` en `.worktrees\cmp-habit-slice`, fuera de este cambio. `:app:assemblePhoneDebug` terminó con BUILD SUCCESSFUL en 5 min 50 s. No se abrió el chat en un emulador ni en un móvil, así que CA-01, CA-04, CA-08 y CA-09 en dispositivo siguen sin esa evidencia.
