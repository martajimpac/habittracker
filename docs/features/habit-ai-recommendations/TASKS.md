# Tareas: Recomendación de hábitos con bloom-api

**SPEC:** `docs/features/habit-ai-recommendations/SPEC.md` (Aprobada, 2026-10-28)
**PLAN:** `docs/features/habit-ai-recommendations/PLAN.md` (Aprobado, 2026-10-28)

Implementación autorizada el 2026-10-28. T1–T5 están hechas. T6 no se ha ejecutado.

## T1 — Cálculo semanal

- **Objetivo:** Obtener, por hábito, el nombre, los días completados de la semana lunes–domingo actual y el objetivo semanal, con las funciones que ya existen.
- **Alcance:** Crear `domain/usecase/HabitWeekRecommendationStats.kt` y su test unitario. Reutilizar `weeklyTarget()` y `completedInWeek()`. No llamar a la red.
- **Depende de:** ninguna.
- **Requisitos:** RF-02, RF-04, CA-02.
- **Validación:** el test unitario pasa con hábitos y registros fijados, sin red. Un nombre en blanco no entra en la lista. Los contadores no se recortan.
- **Estado:** Hecha. `HabitWeekRecommendationStatsTest`: 2 tests, 0 fallos (`gradlew.bat :app:testDebugUnitTest`, 2026-10-28).

## T2 — Dirección de entorno

- **Objetivo:** La URL de `bloom-api` llega desde la configuración local y no desde el cálculo de la recomendación.
- **Alcance:** `BLOOM_API_BASE_URL` en `app/build.gradle.kts`, leída de `local.properties` como `SUPABASE_URL`, y documentada en `local.properties.example` con `http://10.0.2.2:8000` y la IP privada en el puerto `8000`. No versionar `local.properties`.
- **Depende de:** ninguna.
- **Requisitos:** RF-08, CA-09.
- **Validación:** el ejemplo contiene las dos direcciones y `defaultConfig` declara el campo. `local.properties` no se añade al commit.
- **Estado:** Hecha. `BLOOM_API_BASE_URL` está en `app/build.gradle.kts` y en `local.properties.example`. `local.properties` queda fuera del stage.

## T3 — Cliente de bloom-api

- **Objetivo:** Enviar el resumen con `POST /api/recommendations` y traducir los fallos sin mostrar un texto inventado.
- **Alcance:** Contrato en dominio, data source Ktor, implementación del repositorio y `di/BloomApiModule.kt`. Timeout de llamada de 100 s. Log de estado, sin cuerpo ni nombres. Sin librerías nuevas.
- **Depende de:** T1, T2.
- **Requisitos:** RF-03, RF-05, RF-08, CA-03, CA-05.
- **Validación:** un motor HTTP falso demuestra que la ruta es `/api/recommendations` y no `/api/chat`. Los casos 400, 502, 504, cuerpo vacío y conexión fallida producen error y ningún texto de sugerencia.
- **Estado:** Hecha. `BloomRecommendationDataSourceTest`: 3 tests, 0 fallos. Los tests JVM usan `unitTests.isReturnDefaultValues = true` para que `android.util.Log` no lance en la JVM.

## T4 — Estado de la pantalla

- **Objetivo:** Pedir la recomendación una sola vez, no llamarla sin hábitos y conservar solo la visita en curso.
- **Alcance:** `RecommendationViewModel` con `StateFlow`. Errores como `@StringRes`. Sin `Channel`.
- **Depende de:** T1, T3.
- **Requisitos:** RF-04, RF-05, RF-06, CA-04, CA-06.
- **Validación:** con hábitos vacíos el remoto no se llama. Con una petición sin terminar, el segundo intento no dispara otra llamada.
- **Estado:** Hecha. `RecommendationViewModelTest`: 2 tests, 0 fallos.

## T5 — Pantalla y navegación

- **Objetivo:** Abrir desde Estadísticas una pantalla nueva con hechos, sugerencia, vacío, carga y error.
- **Alcance:** `RecommendationScreen` y `RecommendationContent`, destino `TabRecommendations`, botón en `StatsScreen`, barra inferior oculta, cadenas en `values` y `values-es`, y `usesCleartextTraffic` solo en el manifiesto debug. `Scaffold` con el padding del sistema.
- **Depende de:** T4.
- **Requisitos:** RF-01, RF-06, RF-07, CA-08.
- **Validación:** `gradlew.bat :app:assembleDebug` compila. Las cadenas nuevas existen en inglés y español. Estadísticas conserva racha y heatmap.
- **Estado:** Hecha. `assembleDebug` terminó con BUILD SUCCESSFUL en 2 min 15 s. Las cadenas `recommendation_*` están en `values` y `values-es`.

## T6 — Prueba manual

- **Objetivo:** Comprobar en emulador y en un móvil lo que los tests no pueden demostrar.
- **Alcance:** No añade comportamiento. Ejecuta la prueba y anota aquí el resultado real.
- **Depende de:** T5. Ollama en este ordenador y `python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload`.
- **Requisitos:** CA-01, CA-07, CA-08, CA-09.
- **Validación:**
  1. Emulador con `http://10.0.2.2:8000`: una respuesta, sin chat, y los contadores separados del texto.
  2. Móvil en la misma Wi-Fi con `http://<IP privada>:8000`: la misma comprobación.
  3. API apagada: error y reintento, sin sugerencia.
  4. Cuenta sin hábitos: vacío y ninguna petición.
  5. Cambiar el idioma: cambian los rótulos y el párrafo sigue en español.
- **Estado:** Pendiente. No se ha abierto la pantalla en un emulador ni en un móvil.
  - Ollama respondió 200 en `http://127.0.0.1:11434/api/tags`.
  - `http://127.0.0.1:8000/health` agotó el tiempo: bloom-api no estaba en marcha.
  - `adb devices` no listó ningún dispositivo.
  - Los pasos 1–5 de esta tarea no se han ejecutado. El móvil real solo hace falta en el paso 2. Un build release contra HTTP no forma parte de esta prueba.
