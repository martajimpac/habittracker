# PLAN: Recomendación de hábitos con bloom-api

**SPEC de referencia:** `docs/features/habit-ai-recommendations/SPEC.md`
**Versión de la spec revisada:** Aprobada el 2026-10-28 en el árbol de trabajo. No hay commit de esa aprobación.
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

| Componente o archivo existente | Ruta verificada | Responsabilidad y uso previsto |
| --- | --- | --- |
| API intermedia | `C:\Users\marta\bloom-api\main.py` | `GET /health` y `POST /api/recommendations`. Ella llama a Ollama. La app no la modifica. |
| Hábitos con registros | `HabitRepository.getAllHabitsWithRecords()` vía `StatsViewModel` | Origen local de los hábitos. La pantalla nueva lee el mismo flujo. |
| Objetivo y días de la semana | `WeeklyQuota.kt`: `weeklyTarget()`, `completedInWeek()` | Cálculo ya existente para `total_days` y `completed_days`. |
| Estadísticas | `StatsScreen.kt`, ruta `TabStats` en `NavigationBottomWrapper.kt` | Ahí se añade el botón. El contenido actual de la pestaña no se rediseña. |
| Pantalla secundaria | `GoalsScreen.kt`, `TabGoals`, `hideBottomBar` en `BottomNavScreen.kt` | Patrón de pantalla con atrás y barra inferior oculta. |
| Configuración local | `app/build.gradle.kts`, `local.properties`, `local.properties.example` | `SUPABASE_URL` ya pasa de `local.properties` a `BuildConfig`. La URL de la API sigue ese camino. |
| HTTP | `ktor` 3.5.1 en `gradle/libs.versions.toml`, dependencia `ktor-client-android` | Cliente para el `POST`. No se reutiliza el cliente de Supabase. |
| Textos | `values/strings.xml` y `values-es/strings.xml` | Rótulos y errores nuevos en los dos idiomas. |
| Eventos de UI | `presentation/utils/ObserveAsEvents.kt` | No hace falta un evento one-shot: el resultado y el error son estado de la pantalla. |
| Manifiesto | `AndroidManifest.xml` | Ya tiene `INTERNET`. No hay `usesCleartextTraffic` ni `networkSecurityConfig`. |

**Convenciones y patrón de referencia:** Clean Architecture en un solo módulo `:app`. Pantalla y contenido separados, como `GoalsScreen`. ViewModel con `StateFlow` e inyección por Hilt. Repositorios de dominio implementados en `data`. Textos de UI en recursos. Logs con `android.util.Log` en el acceso remoto, sin cuerpos ni nombres de hábitos.

## Solución propuesta

La app calcula en dominio la pareja semanal de cada hábito y la muestra ella. Un cliente HTTP propio envía solo ese resumen a `bloom-api` y pinta el texto `recommendations` como sugerencia. La dirección sale de `BuildConfig`, no del código que calcula los contadores.

```text
Estadísticas --botón--> pantalla nueva
    |                       |
    |                       lee Room (hábitos y registros)
    |                       weeklyTarget + completedInWeek
    |                       muestra nombre y dos contadores
    |                       POST {BASE_URL}/api/recommendations
    |                       bloom-api --POST /api/chat--> Ollama en 127.0.0.1:11434
    |                       pinta el texto aparte
```

Al salir de la pantalla se destruye su ViewModel y se cancela la petición. No hay tabla nueva ni notificación.

No se añade un módulo Gradle. No se propone otra API ni sustituir `bloom-api`.

## Módulos y componentes afectados

El proyecto de esta rama tiene un solo módulo de aplicación. No se crea otro.

| Módulo | Existe / nuevo | Responsabilidad y cambios | Dependencias afectadas |
| --- | --- | --- | --- |
| `:app` | Existente | Pantalla, cálculo semanal, cliente de `bloom-api` y URL de entorno | Ningún módulo nuevo. Sigue dependiendo de Room, Hilt, Compose y Ktor, que ya están declarados |

| Componente o ruta | Acción | Cambio y responsabilidad | Requisito relacionado |
| --- | --- | --- | --- |
| `domain/usecase/WeeklyQuota.kt` | Reutilizar | `weeklyTarget()` y `completedInWeek()` | RF-02, CA-02 |
| `domain/usecase/HabitWeekRecommendationStats.kt` | Crear | Función pura: hábitos y fecha de hoy a la lista `name`, `completed_days`, `total_days`. Omite un nombre en blanco. No recorta contadores ni nombres | RF-02, RF-04, CA-02 |
| `domain/repository/RecommendationRepository.kt` | Crear | Contrato: enviar la lista y recibir el texto, o un error de dominio sin textos de UI | RF-03, RF-05 |
| `data/remote/BloomRecommendationDataSource.kt` | Crear | `POST` con Ktor al `baseUrl` inyectado. Timeout de llamada de 100 s para poder recibir el 504 de la API. No registra el cuerpo | RF-03, RF-05, RF-08 |
| `data/repository/RecommendationRepositoryImpl.kt` | Crear | Traduce HTTP 400, 502, 504, cuerpo vacío y fallo de red a error de dominio | RF-05, CA-05 |
| `di/BloomApiModule.kt` | Crear | Entrega la URL de `BuildConfig` y el origen de datos. Vive en `di`, como el resto de Hilt | RF-08 |
| `app/build.gradle.kts`, `local.properties.example` | Modificar | `BLOOM_API_BASE_URL` junto a `SUPABASE_URL`. El ejemplo documenta las dos direcciones de desarrollo | RF-08, CA-09 |
| `src/debug/AndroidManifest.xml` | Crear | `usesCleartextTraffic` solo en debug, porque las direcciones aprobadas son `http` y el manifiesto actual bloquea el claro | RF-08, CA-09 |
| `presentation/navigation/TabScreens.kt` | Modificar | Destino `TabRecommendations` | RF-01 |
| `NavigationBottomWrapper.kt`, `BottomNavScreen.kt` | Modificar | Navegación desde Estadísticas y barra inferior oculta, igual que Metas | RF-01 |
| `presentation/screens/stats/StatsScreen.kt` | Modificar | Un botón que llama a `onOpenRecommendations`. No cambia los gráficos | RF-01 |
| `presentation/screens/recommendations/RecommendationScreen.kt` y `RecommendationContent.kt` | Crear | `Scaffold`, atrás, lista de hechos, acción, carga, vacío, error y texto de sugerencia. El contenido no ve el ViewModel | RF-01, RF-06, RF-07 |
| `RecommendationViewModel.kt` | Crear | Lee el repositorio de hábitos, calcula, llama una sola vez y guarda el estado de la visita | RF-04, RF-06, CA-06 |
| `values/strings.xml`, `values-es/strings.xml` | Modificar | Botón, títulos, vacío, carga, error y etiqueta de sugerencia | RF-07, CA-08 |

## Datos y contratos

- **Modelos y contratos de entrada y salida:** cuerpo `{"habits":[{"name","completed_days","total_days"}]}`. Respuesta útil `{"recommendations":"<texto>"}`. El modelo de dominio de la petición es esa pareja más el nombre. El texto no se parsea para extraer cifras.
- **Identificadores, relaciones y restricciones:** no hay id nuevo. `completed_days` puede ser mayor que `total_days`; se envía igual y un 400 se muestra como error. Un nombre de más de 100 caracteres también falla la validación de la API y se muestra como error, sin recortar el nombre visible.
- **Origen de los datos mostrados y transformaciones:** Room, semana lunes–domingo de hoy en la zona local, funciones ya citadas. La sugerencia solo llega de `recommendations`.
- **Persistencia, consultas y actualizaciones:** no se escribe la sugerencia. La lectura de hábitos es la que ya existe.
- **Convivencia entre datos locales y remotos:** los contadores salen de Room aunque no haya internet hacia Supabase. La sugerencia exige que `bloom-api` responda. No se mezclan con el aviso de «sin internet» de guardar un hábito.
- **Compatibilidad y migraciones de datos existentes:** no hay migración.

## Estado, operaciones y errores

- **Gestión del estado de interfaz y navegación:** `StateFlow` con reposo, carga, vacío, resultado y error. El botón de Estadísticas navega con el callback que ya usan Metas y el detalle. Atrás hace `popBackStack`. No hay `Channel`: no hay diálogo, snackbar ni navegación disparados por un resultado.
- **Conservación y restauración del estado:** el ViewModel conserva la visita si la pantalla se recrea o pasa a segundo plano y el proceso sigue vivo. Al salir o al morir el proceso, el estado desaparece. Cumple D7.
- **Ejecución, concurrencia y cancelación de operaciones:** la petición corre en `viewModelScope`. Un segundo toque no llama si el estado es carga. Salir cancela el ámbito. La UI no espera en el hilo principal.
- **Errores, reintentos y prevención de duplicados:** 400, 502, 504, cuerpo sin texto, URL vacía y fallo de conexión usan `@StringRes` en el ViewModel. Reintentar lanza otra petición solo cuando no hay una en curso. El código de estado se registra con `Log.e`; el cuerpo y los nombres, no.
- **Otras consideraciones mobile aplicables y su solución:** `Scaffold` con el `innerPadding` del contenido, como el resto de pantallas con barra superior. No hay campo de texto. El estado de carga tiene texto, no solo un indicador. Los rótulos largos van en una columna con scroll. No se piden permisos nuevos.

## Dependencias y configuración

- No se añade ninguna librería. El `POST` usa el cliente Ktor 3.5.1 ya declarado. El cliente de Supabase no sirve: apunta a otro host y lleva la sesión.
- `BLOOM_API_BASE_URL` en `local.properties`, copiada a `BuildConfig` en `defaultConfig`. Vacía si falta la propiedad. `local.properties` sigue sin versionarse. El ejemplo, sí, con `http://10.0.2.2:8000` y un comentario para la IP privada y el puerto `8000`.
- El claro se permite solo en el manifiesto `debug`. Un build release contra esas URLs `http` no llegará a la API. Encaja con el uso de desarrollo aprobado. Release no abre el claro para todo el tráfico.
- La API se arranca fuera de la app: `python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload`, con Ollama en el mismo ordenador.

## Estrategia de validación

| Criterio | Método y test existente o propuesto | Entorno y datos necesarios | Evidencia prevista |
| --- | --- | --- | --- |
| CA-01 | Manual. No hay test de UI de esta pantalla | Emulador, API y Ollama en marcha, cuenta con hábitos | Una respuesta, sin lista de mensajes |
| CA-02 | Test unitario nuevo de `HabitWeekRecommendationStats`. No hay test previo de `weeklyTarget` | Hábitos y registros fijados, sin red | Contadores iguales a `weeklyTarget` y `completedInWeek` |
| CA-03 | Test unitario del data source con un motor HTTP falso | URL base inyectada | La ruta es `/api/recommendations` y no `/api/chat` |
| CA-04 | Test unitario del ViewModel con repositorio falso | Lista de hábitos vacía | El remoto no se llama y el estado es vacío |
| CA-05 | Test unitario del repositorio para 400, 502, 504, cuerpo vacío y conexión fallida | Respuestas controladas | Error de dominio y ningún texto de sugerencia |
| CA-06 | Test unitario del ViewModel | Petición que no termina | Una sola llamada al remoto |
| CA-07 | Manual, revisando que los contadores de la lista no salen del texto | Respuesta que mencione un número ausente del resumen | La lista de hechos no incorpora ese número |
| CA-08 | Revisión de `values` y `values-es`, y cambio de idioma en el emulador | Cadenas nuevas en los dos ficheros | Los rótulos cambian. El párrafo sigue en español |
| CA-09 | Test de que la URL usada es la inyectada, más prueba manual | `local.properties` con `10.0.2.2` y, en otro arranque, la IP privada | Cada entorno llama a su base. El cálculo no referencia la URL |

**Comprobaciones de regresión:** `StatsScreen` sigue mostrando racha y heatmap. Metas y el resto de destinos siguen navegando. No se llama a Supabase para esta sugerencia.

**Comandos verificados para compilar y ejecutar tests:** el proyecto usa el wrapper de Gradle. Los comandos previstos, aún no ejecutados para este plan, son `gradlew.bat :app:testDebugUnitTest` y `gradlew.bat :app:assembleDebug`.

**Pruebas en dispositivo, emulador o simulador:**

1. Arrancar Ollama en este ordenador y la API con el comando aprobado.
2. Poner `BLOOM_API_BASE_URL=http://10.0.2.2:8000` y pedir la recomendación en el emulador estándar.
3. Cambiar la propiedad a `http://<IP privada>:8000`, reinstalar y repetir en un móvil de la misma Wi-Fi.
4. Repetir con la API apagada y con una cuenta sin hábitos.

Hace falta un móvil real solo para el paso 3. El emulador no demuestra la IP privada.

**Limitaciones del entorno:** este plan no ha ejecutado esos comandos ni la prueba manual. Un build release contra HTTP queda fuera de lo comprobable con el manifiesto debug. No se puede demostrar desde aquí que el firewall de Windows deje pasar el puerto `8000` al móvil.

## Orden de implementación

1. Función de dominio y su test. Comprobar CA-02 antes de tocar la red.
2. Data source, repositorio, módulo Hilt y URL. Tests de CA-03 y CA-05.
3. ViewModel y tests de CA-04 y CA-06.
4. Pantalla, navegación, cadenas y manifiesto debug. Compilar `assembleDebug`.
5. Prueba manual de CA-01, CA-07, CA-08 y CA-09. Anotar en `TASKS.md` lo que no se haya podido ejecutar.

## Riesgos y decisiones pendientes

- **Riesgos y medidas acordadas:**
  - El modelo `qwen2.5-coder:7b` puede redactar mal un consejo de hábitos. La pantalla mantiene los contadores calculados por la app separados del texto.
  - Un hábito con más días completados que su objetivo, o un nombre de más de 100 caracteres, recibe 400 o 422. Se muestra error y no se alteran los datos.
  - El teléfono no llega a `0.0.0.0`. Por eso la app usa `10.0.2.2` o la IP privada. Si el firewall bloquea el `8000`, la prueba de móvil falla y se registra, sin cambiar la API.
  - `usesCleartextTraffic` en debug abre el HTTP claro de ese build, no solo hacia `bloom-api`. Se acepta porque la IP de desarrollo no es fija y el release no lleva esa bandera.
  - No se usan subagentes en este plan. `android-architect` y `android-code-reviewer` existen; la implementación aún no está autorizada.
- **Decisiones pendientes:** Ninguna.

<!-- ANTES DE SOLICITAR APROBACIÓN
Comprueba que el plan cubre los requisitos, respeta las exclusiones, reutiliza
componentes verificados y permite demostrar todos los criterios de aceptación.
Resuelve dudas y marcadores pendientes. Si la spec cambió, revisa su impacto.
Tras aprobar el plan, deriva TASKS.md con IDs, dependencias, referencias a RF/CA
y comprobaciones. No marques una tarea terminada sin realizar su validación;
si está bloqueada, registra el motivo.
-->
