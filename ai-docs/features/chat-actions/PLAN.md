# PLAN: Órdenes en el chat de Bloom: crear hábitos y marcar días

**SPEC de referencia:** `ai-docs/features/chat-actions/SPEC.md`
**Versión de la spec revisada:** aprobada el 2026-11-15 (rama `feature/home-day-toggle`, sobre `4abc543`)
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

Rutas de la app relativas a `app/src/main/java/com/marta/habittracker/`.

| Componente o archivo existente | Ruta verificada | Responsabilidad y uso previsto |
| --- | --- | --- |
| API de Bloom | `bloom-api/main.py` | `POST /api/chat`: valida la sesión, monta el prompt con los recuentos y llama a Groq (`openai/gpt-oss-120b`). Devuelve `{"message"}`. Exige al menos un hábito. Se amplía con acciones. |
| Tests de la API | `bloom-api/tests/test_auth.py`, `conftest.py` | pytest con Groq simulado (`groq_calls`). Se añade `test_actions.py`. |
| DataSource del chat | `data/remote/ai/datasources/AIDataSource.kt` (`BloomRecommendationDataSource`) | Serializa la petición, reintenta tras 401 y analiza la respuesta. Se amplía el cuerpo y el análisis. |
| DTO del chat | `data/remote/ai/dto/ChatRequestBody.kt`, `ChatMessageBody.kt`, `ChatResponseBody.kt`, `RecommendationHabitBody.kt` | Contrato de red. Se amplían con campos opcionales. |
| Repositorio del chat | `domain/repository/RecommendationRepository.kt`, `data/repository/RecommendationRepositoryImpl.kt` | `send(stats, language, messages): DataResult<String, …>`. Pasa a devolver una respuesta con acción opcional. |
| Turno del chat | `domain/model/AssistantTurn.kt` | `role` + `text`. Se añaden id, acción y resultado. |
| Estadísticas semanales | `domain/usecase/HabitWeekRecommendationStats.kt` | Construye `HabitWeekStat`. Se amplía con id, programación y días hechos. |
| Reglas de programación | `domain/usecase/WeeklyQuota.kt` (`shownOn`, `completedInWeek`, `weeklyTarget`), `domain/model/HabitSchedule.kt` | Mismas reglas que Inicio para saber si un hábito aparece un día (RF-16). Se reutilizan. |
| Crear hábito | `domain/usecase/SaveHabit.kt` | Guardado online-first que usa el formulario. Se reutiliza tal cual (RF-03, RF-11). |
| Marcar un día | `HabitRepository.toggleHabitCompletion(habitId, isCompleted, date)` en `data/repository/HabitRepositoryImpl.kt` | Online-first. Guarda `!isCompleted`, así que pasando el contrario del estado deseado fija un estado absoluto (RF-20). Refresca el widget. |
| ViewModel del chat | `presentation/screens/recommendations/RecommendationViewModel.kt` | Fases `LoadingHabits / Empty / Ready / Sending`, envío y reintento. Se amplía. |
| Pantalla del chat | `presentation/screens/recommendations/RecommendationScreen.kt` | `RecommendationScreen` / `RecommendationContent`, burbujas, `Greeting`, `EmptyCopy`, `RetryBar`, `Composer`. Se añade la tarjeta. |
| Catálogos | `presentation/components/HabitIcons.kt` (`HabitIconOptions`, `HabitIconKeys.DEFAULT`), `presentation/utils/ColorUtils.kt` (`HabitColorOptions`, `toHex`) | Iconos y colores del formulario. Fuente única para la tarjeta y para Bloom. |
| Eventos one-shot | `presentation/utils/ObserveAsEvents.kt`; ejemplo en `HomeViewModel` (`Channel(BUFFERED)` + `receiveAsFlow`) | Patrón para el snackbar de error. |
| Errores | `domain/model/errors/AppError.kt`, `presentation/utils/AppErrorUiMessages.kt` | Mensajes de red y desconocido. Se añade un error propio de acciones. |
| Tests de la app | `app/src/test/.../BloomRecommendationDataSourceTest.kt`, `RecommendationViewModelTest.kt`, `HabitWeekRecommendationStatsTest.kt`, `SaveHabitTest.kt`, `WeeklyQuotaTest.kt` | Se amplían. |

**Convenciones y patrón de referencia:** Clean Architecture (`AGENTS.md`). El ViewModel usa interfaces de dominio y casos de uso. `*Screen` y `*Content` van separados. El estado se expone con `StateFlow` y los eventos one-shot van con `Channel` y `ObserveAsEvents`. Los textos están en `strings.xml` (en y es). Los fallos se registran con `Log.e(TAG, …)` sin contenido sensible. La decisión vigente de Brain es online-first (`online-first-habit-sync-read-cache`), y la API se autentica con el JWT de Supabase.

## Solución propuesta

<!-- Explica el enfoque y sus motivos. Describe las responsabilidades y el
recorrido de datos y eventos hasta la interfaz. Usa un diagrama si aporta claridad. -->

**Idea central:** Bloom **propone** y la app **ejecuta**. La API no guarda nada en Supabase y no necesita ningún secreto nuevo. Devuelve el texto y, opcionalmente, una acción estructurada. La app valida esa acción con las reglas de dominio, muestra la tarjeta y, al confirmar, la guarda con el mismo código que el formulario y que Inicio.

**Cómo propone el modelo una acción:** con *tool calling* de Groq. La API declara dos herramientas, `propose_create_habit` y `propose_set_completion`, con `tool_choice: "auto"`. Cada herramienta incluye un campo `message`, el texto breve que acompaña a la tarjeta, así que basta una sola llamada a Groq. Según la documentación de Groq, `gpt-oss-120b` admite tool use pero no llamadas en paralelo, lo que encaja con «una propuesta por respuesta» (RF-09). En cualquier caso, la API se queda solo con la primera llamada. Descartamos *structured outputs* porque Groq no lo combina con tool use y hay informes de que el modo estricto falla con este modelo.

```
Usuario ──texto──▶ ViewModel ──send(contexto, historial)──▶ Repo ▶ DataSource ──POST /api/chat──▶ API ──tools──▶ Groq
                                                                                        ◀── {message, action?} ──
ViewModel ◀── ChatReply(text, action?) ── valida acción (dominio) ──▶ tarjeta Pendiente
Tarjeta ──Confirmar──▶ ExecuteAssistantAction ──▶ SaveHabit | toggleHabitCompletion ──▶ Done / error (snackbar)
Siguiente envío: el historial lleva cada propuesta con su resultado (done / cancelled / pending)
```

**Recorrido:**

1. **Contexto que envía la app (RF-15 a RF-19, RF-22).** Va en cada petición:
   - Por hábito: `id`, `name`, programación (`schedule`: `daily`, `weekdays` o `weekly_count`, más `days` o `weekly_target`), las fechas hechas en la semana actual y los recuentos actuales (`completed_days` y `total_days`, que se conservan).
   - `today` (fecha ISO del móvil).
   - El catálogo de iconos y de colores (hex) del formulario. Así la app sigue siendo la única fuente de los catálogos y la API no los duplica.
   - La lista de hábitos puede estar vacía.
2. **La API** valida el cuerpo, construye el prompt y llama a Groq con las herramientas:
   - El prompt añade al actual: las órdenes que puede proponer; preguntar si la orden es ambigua; no proponer días futuros, de otra semana ni días en que el hábito no toca; avisar si el día ya está en ese estado; no hay recordatorios; y el aviso de que aún no hay hábitos.
   - Valida la llamada a la herramienta con pydantic. Si no es válida (falta el nombre, `habit_id` desconocido, fecha fuera de lunes–`today`, frecuencia imposible), descarta la acción y devuelve solo el texto.
   - Si Groq rechaza la llamada (400 `tool_use_failed`), la API reintenta una vez sin herramientas para responder al menos con texto.
3. **La app** convierte la respuesta en un `ChatReply(text, action?)`. El caso de uso `ResolveAssistantAction` la valida con las reglas de dominio y los hábitos actuales (CA-23):
   - **Crear:** icono o color fuera del catálogo → los de por defecto. Sin nombre, o frecuencia inválida → sin acción.
   - **Marcar:** el hábito debe existir. La fecha debe estar entre el lunes de esta semana y hoy, y cumplir `shownOn(date)`. Si el día ya está en el estado pedido → sin acción.
4. **Tarjeta:** el ViewModel añade el turno de Bloom con su acción en estado `Pending`. El `*Content` pinta la tarjeta.
5. **Confirmar (RF-03, RF-05, RF-20, RF-21):** el caso de uso `ExecuteAssistantAction` vuelve a validar con los hábitos y el `today` actuales, porque el hábito pudo borrarse o la semana pudo cambiar. Después:
   - **Crear:** llama a `SaveHabit` con el recordatorio apagado y el hábito privado.
   - **Marcar:** llama a `toggleHabitCompletion(habitId, isCompleted = !target, date)`, que deja el día en `target` aunque se haya cambiado desde Inicio.
   - Si va bien, el estado pasa a `Done`. Si falla, vuelve a `Pending` y se envía un evento de snackbar con el error.
6. **Historial (RF-08):** cada turno de Bloom con acción se envía con la propuesta y su estado (`pending`, `done` o `cancelled`). La API lo convierte en una línea de texto para el modelo, por ejemplo `[Propuesta: marcar «Leer» el 2026-11-12 — estado: hecha]`. Así no hace falta reproducir el protocolo de herramientas en el historial.

**Compatibilidad:** todos los campos nuevos de la petición son opcionales en la API, y la app ignora las claves desconocidas (`ignoreUnknownKeys`). Una app antigua sigue funcionando con la API nueva. Con la API antigua, la app nueva sigue recibiendo `message`, aunque una cuenta sin hábitos recibirá un 400 hasta desplegar la API.

## Módulos y componentes afectados

<!-- Si el proyecto está modularizado, identifica los módulos afectados, sus
responsabilidades y la dirección de sus dependencias. Respeta los límites
existentes y justifica cualquier módulo o dependencia nueva. Si no está
modularizado, describe las carpetas o componentes afectados sin introducir
modularización fuera del alcance; marca la tabla de módulos como No aplica. -->

| Módulo | Existe / nuevo | Responsabilidad y cambios | Dependencias afectadas |
| --- | --- | --- | --- |
| No aplica | — | El proyecto tiene un solo módulo `:app` (más `bloom-api`, que es un servicio aparte). Los cambios se describen por carpetas abajo. | — |

<!-- Distingue lo que se reutiliza, modifica o crea. Las rutas nuevas son propuestas.
Señala impacto sobre modelos, contratos o componentes compartidos. -->

| Componente o ruta | Acción | Cambio y responsabilidad | Requisito relacionado |
| --- | --- | --- | --- |
| `bloom-api/main.py` | Modificar | Modelos pydantic ampliados (hábito con `id`, `schedule`, `done_dates`; `today`; `catalog`; mensaje con `action` y `action_status` opcionales; `habits` puede estar vacía). Herramientas, prompt, validación de la llamada, reintento sin herramientas y respuesta `{message, action?}`. | RF-01, RF-02, RF-08 a RF-13, RF-15 a RF-19, RF-22 |
| `bloom-api/tests/test_actions.py` | Crear | pytest con Groq simulado para cada caso de herramienta y validación. | CA-01, CA-02, CA-07, CA-09, CA-13 a CA-16, CA-20 a CA-23 |
| `bloom-api/tests/conftest.py` | Modificar | El simulador de Groq puede devolver `tool_calls` y registra `tools` y `messages`. | Tests |
| `domain/model/AssistantTurn.kt` | Modificar | Añade `id`, `action: AssistantAction?` y `actionStatus: AssistantActionStatus?` (`Pending`, `Done`, `Cancelled`). | RF-02 a RF-04, RF-08 |
| `domain/model/AssistantAction.kt` | Crear | `sealed interface`: `CreateHabit(name, description, icon, colorHex, schedule, daysOfWeek, daysPerWeek)` y `SetCompletion(habitId, habitName, habitIcon, date, completed)`. | RF-10, RF-15 |
| `domain/model/ChatContext.kt` | Crear | `ChatContext(habits: List<ChatHabit>, today, catalog: HabitCatalog)`, `ChatReply(text, action?)` y la propuesta en bruto (`ProposedAction`) tal como llega de Bloom. | RF-15 a RF-19 |
| `domain/usecase/HabitWeekRecommendationStats.kt` | Modificar | Construye `ChatHabit` (id, nombre, programación, días hechos esta semana, recuentos). Mantiene `HabitWeekStat` si sigue en uso; si no, se sustituye. | RF-16, RF-19 |
| `domain/usecase/ResolveAssistantAction.kt` | Crear | Función pura: propuesta + hábitos + hoy + catálogo → `AssistantAction?`. | CA-23, RF-11, RF-16, RF-18, RF-19 |
| `domain/usecase/ExecuteAssistantAction.kt` | Crear | Revalida y ejecuta con `SaveHabit` o `toggleHabitCompletion`. Devuelve `DataResult<Unit, AppError>`. | RF-03, RF-06, RF-20, RF-21 |
| `domain/model/errors/AssistantActionError.kt` | Crear | `AppError`: `HabitNotFound` y `DayNotAllowed`. | RF-21, cambio de semana |
| `domain/repository/RecommendationRepository.kt` + impl | Modificar | `send(context, language, messages): DataResult<ChatReply, RecommendationError>`. | RF-01 |
| `data/remote/ai/dto/*` | Modificar / crear | Amplía `ChatRequestBody`, `RecommendationHabitBody` y `ChatMessageBody`; `ChatResponseBody.action`. Crea `ChatActionBody`, `HabitScheduleBody` y `HabitCatalogBody`, una clase por archivo. | RF-08, RF-15 |
| `data/mapper/AssistantActionMapper.kt` | Crear | DTO ↔ dominio de acciones y del contexto. | — |
| `data/remote/ai/datasources/AIDataSource.kt` | Modificar | Serializa el contexto y analiza `action`. Una acción malformada se descarta con `Log.w` y se mantiene el texto. | CA-23 |
| `presentation/.../RecommendationViewModel.kt` | Modificar | Sin la fase `Empty` bloqueante (`hasHabits` en el estado); `ChatUiState.savingActionIds`; `confirmAction(id)` y `cancelAction(id)`; `Channel` de eventos (`ShowError(@StringRes)`); construye el catálogo con `HabitIconOptions` y `HabitColorOptions`. | RF-02 a RF-06, RF-22 |
| `presentation/.../RecommendationScreen.kt` | Modificar | `ActionCard` dentro de la burbuja de Bloom; `SnackbarHost` + `ObserveAsEvents`; saludo nuevo sin hábitos; se retira `EmptyCopy`. | RF-02 a RF-06, RF-22, accesibilidad |
| `presentation/.../ActionCard.kt` | Crear | Tarjeta sin estado (`*Content`): crear (icono, color, nombre, descripción, frecuencia) y marcar (icono, nombre, día con su fecha, acción). Botones, «guardando», «hecho» o «cancelado» y semántica fusionada. Con `@Preview`. | RF-10, RF-15, accesibilidad |
| `res/values/strings.xml`, `values-es/strings.xml` | Modificar | Textos de la tarjeta, de estados, de errores y del nuevo saludo. Se retiran `recommendation_empty_*` si dejan de usarse. | Idiomas |
| `spec.md` | Modificar | Apartado del chat de Bloom: órdenes y chat sin hábitos. | RF-23 |

## Datos y contratos

<!-- Completa solo lo aplicable. Si un punto no aplica, indica el motivo. -->

- **Modelos y contratos de entrada y salida:**
  - Petición: `habits[]{id, name, completed_days, total_days, schedule{mode, days[], weekly_target}, done_dates[]}`, `today`, `catalog{icons[], colors[]}`, `language` y `messages[]{role, content, action?, action_status?}`.
  - Respuesta: `{message, action?}`.
  - `action` puede ser `{type:"create_habit", name, description?, icon, color, schedule{…}}` o `{type:"set_completion", habit_id, date, completed}`.
  - Los días se envían como `MONDAY…SUNDAY` y las fechas en ISO `YYYY-MM-DD`.
- **Identificadores, relaciones y restricciones:**
  - `habit_id` debe ser uno de los hábitos enviados en esa petición (lo valida la API) y seguir existiendo al confirmar (lo valida la app).
  - El id de cada turno lo genera el ViewModel con `IdGenerator` y sirve de clave en la `LazyColumn` y para las acciones de la tarjeta.
  - El nombre propuesto tiene un máximo de 100 caracteres, que es el límite que ya aplica la API a los nombres.
- **Origen de los datos mostrados y transformaciones:**
  - El contexto sale de `getAllHabitsWithRecords()` (Room).
  - La tarjeta de crear usa `HabitLineIcon` con la clave del icono y `parseHabitColor`.
  - El día de la tarjeta de marcar se formatea con `java.time` y el `Locale` de la app (por ejemplo, «jueves, 12 nov»).
- **Persistencia, consultas y actualizaciones:** no hay tablas nuevas. Crear usa `insertHabit` (vía `SaveHabit`) y marcar usa `upsertRecord` (vía `toggleHabitCompletion`). La conversación sigue en memoria.
- **Convivencia entre datos locales y remotos:** sin cambios: online-first, primero Supabase y después Room. Inicio, Estadísticas y los widgets se actualizan por el `Flow` de Room y `refreshTodayWidget()`.
- **Compatibilidad y migraciones de datos existentes:** no hay migraciones de Room ni de Supabase. La compatibilidad del contrato de la API se explica en la Solución propuesta.

## Estado, operaciones y errores

<!-- Cómo se implementan los comportamientos aprobados en la spec.
Referencia RF/CA y aplica las consideraciones relevantes de MOBILE_GUIDELINES.md. -->

- **Gestión del estado de interfaz y navegación:**
  - `ChatUiState` conserva `phase` (`LoadingHabits`, `Ready` o `Sending`), `messages`, `draft` y `errorRes`, y añade `hasHabits` y `savingActionIds: Set<String>`.
  - El estado de cada tarjeta se calcula a partir de `turn.actionStatus` y de `savingActionIds`.
  - Los errores al confirmar se envían por `Channel(BUFFERED)` y se muestran en un snackbar con `ObserveAsEvents`. No se guardan como flags en el estado.
  - No hay navegación nueva.
- **Conservación y restauración del estado:** el estado vive en el ViewModel. Sobrevive a la rotación y al paso a segundo plano (CA-24) y se pierde si el proceso termina, como ahora (lo acepta la spec). No usamos `SavedStateHandle`.
- **Ejecución, concurrencia y cancelación de operaciones:**
  - `confirmAction` ignora la llamada si el id ya está en `savingActionIds` o si el estado no es `Pending` (RF-05, CA-05).
  - Se ejecuta en `viewModelScope`, así que salir de la pantalla no lo cancela mientras el ViewModel viva. Si se sale del todo, el guardado ya enviado a Supabase termina en el repositorio, igual que en Inicio.
  - Se puede seguir escribiendo mientras se guarda una tarjeta.
- **Errores, reintentos y prevención de duplicados:**
  - Red o servidor: `AppError.Common.Network` o `Unknown`, con el mensaje que ya usa la app (`AppErrorUiMessages`).
  - `HabitNotFound` y `DayNotAllowed`: textos nuevos.
  - Si hay error, la tarjeta vuelve a `Pending` y se puede reintentar (RF-06).
  - Crear usa un id nuevo en cada intento y solo vuelve a `Pending` cuando hay error, así que un error de red después de que Supabase haya guardado podría duplicar el hábito al reintentar. Para evitarlo, el id del hábito se genera al crear la propuesta y se reutiliza en cada reintento: `SaveHabit` acepta `id` y `upsertHabit` es idempotente.
- **Otras consideraciones mobile aplicables y su solución:**
  - Accesibilidad: `Modifier.semantics(mergeDescendants = true)` en el cuerpo de la tarjeta, con descripción del tipo «Propuesta: marcar Leer como hecho el jueves 12. Estado: pendiente». Los botones tienen texto propio y un mínimo de 48 dp, y el estado se indica con texto además del color.
  - Texto grande: la tarjeta ocupa todo el ancho de la burbuja, los textos pueden ocupar varias líneas y los botones pasan a otra fila si no caben (`FlowRow`).
  - Teclado e insets: no cambian; el `Composer` ya existe. Ver la skill `edge-to-edge` solo si se toca el layout.
  - Privacidad: los `Log` no recogen los mensajes, los nombres ni las fechas, solo el tipo de acción y el resultado.

## Dependencias y configuración

<!-- Librerías, servicios, permisos o configuración afectados. Verifica compatibilidad
con el proyecto y justifica las incorporaciones. No agregues dependencias por defecto. -->

- No hay dependencias nuevas en la app ni en la API. Se usan el SDK `groq` que ya está instalado (parámetros `tools` y `tool_choice` de `chat.completions.create`) y `kotlinx.serialization`.
- No se piden permisos ni hacen falta secretos nuevos.
- **Despliegue:** el cambio de la API requiere desplegar la Lambda (`bloom-api`, `sam deploy` según `samconfig.toml`). Es una acción sobre AWS: la haré solo con tu autorización explícita, o la harás tú.

## Estrategia de validación

<!-- Una fila por criterio de la spec. Selecciona el método capaz de demostrarlo:
test unitario, integración, UI o prueba manual. No todos requieren todos los métodos.
Identifica tests existentes y separa los nuevos propuestos. Incluye regresiones relevantes.
Una captura aislada no demuestra persistencia ni ausencia de peticiones de red. -->

Leyenda: **API** = pytest con Groq simulado (comprueba el contrato y la validación, no lo que decide el modelo); **Dom** = test unitario de un caso de uso; **VM** = test de `RecommendationViewModelTest` con fakes; **Manual** = app de depuración en emulador contra la API desplegada (comprueba lo que decide el modelo real).

| Criterio | Método y test existente o propuesto | Entorno y datos necesarios | Evidencia prevista |
| --- | --- | --- | --- |
| CA-01 | API: el texto sin `tool_calls` da `action` nula. Manual | Emulador + API desplegada | pytest + captura |
| CA-02 | API: `propose_create_habit` válida → `action`. Dom: resolver. Manual | Ídem | pytest + captura |
| CA-03 | VM: confirmar llama a `SaveHabit` (privado, sin recordatorio) y pasa a `Done`. Manual: Inicio | Fakes; emulador | Test + captura |
| CA-04 | VM: cancelar pasa a `Cancelled` sin llamar al repositorio | Fakes | Test |
| CA-05 | VM: dos `confirmAction` seguidos dan una sola ejecución | Fakes con suspensión controlada | Test |
| CA-06 | VM: un error del repositorio da evento de error y `Pending`. Manual: modo avión | Fakes; emulador | Test + captura |
| CA-07 | API: el prompt contiene la regla de preguntar. Manual | Emulador | pytest + captura |
| CA-08 | API: horario `weekdays` con 3 días. Manual | Emulador | pytest + captura |
| CA-09 | API: el prompt dice que no hay recordatorios; la herramienta no tiene ese campo. Manual | Emulador | pytest + captura |
| CA-10 | API: `propose_set_completion` válida. Dom: resolver con ayer. Manual | Emulador | pytest + captura |
| CA-11 | Dom: ejecutar llama a `toggleHabitCompletion(id, false, ayer)`. Manual: Inicio y racha | Fakes; emulador | Test + captura |
| CA-12 | Dom: desmarcar llama con `isCompleted = true` | Fakes | Test |
| CA-13 | API: fecha futura o de la semana pasada → sin `action`. Dom: lo mismo. Manual | — | pytest + test |
| CA-14 | Dom: `weekdays` sin el día de hoy → sin acción. Manual | — | Test + captura |
| CA-15 | API: `habit_id` desconocido → sin `action`. Manual: pregunta de desambiguación | Emulador | pytest + captura |
| CA-16 | Dom: ya hecho → sin acción. Manual | — | Test + captura |
| CA-17 | Dom: el estado cambió entre la propuesta y la confirmación y el resultado final es `target` | Fakes | Test |
| CA-18 | Dom: el hábito ya no existe → `HabitNotFound`, sin escribir | Fakes | Test |
| CA-19 | VM: una propuesta nueva no cambia el estado de la anterior | Fakes | Test |
| CA-20 | API: un mensaje con `action_status` llega al modelo como línea de texto. VM: el historial lleva el estado. Manual | Emulador | pytest + test + captura |
| CA-21 | API: con dos `tool_calls`, solo se devuelve la primera. Manual | Emulador | pytest + captura |
| CA-22 | API: `habits: []` → 200. VM: sin hábitos se puede enviar. Manual: cuenta nueva | Cuenta de prueba sin hábitos | pytest + test + captura |
| CA-23 | Dom: icono desconocido → por defecto; sin nombre, día futuro o id inexistente → nulo. DataSource: JSON de acción malformado → solo texto | — | Tests |
| CA-24 | Manual: girar el móvil y pasar a segundo plano con una tarjeta pendiente | Emulador | Capturas antes y después |
| CA-25 | Revisión de `spec.md` | — | Diff |

**Comprobaciones de regresión:**
- Los tests actuales de `test_auth.py` (401, 503 y ausencia del token en los logs).
- `BloomRecommendationDataSourceTest` (reintento tras 401 y desenvoltura del JSON).
- `RecommendationViewModelTest` (envío y reintento).
- `HabitWeekRecommendationStatsTest`, `SaveHabitTest` y `WeeklyQuotaTest`.
- Manual: una pregunta normal sigue dando consejo.

**Comandos verificados para compilar y ejecutar tests:**
- App: `./gradlew :app:testDebugUnitTest :app:assembleDebug` (el que usa CI, según `bloom-api-auth/PLAN.md`).
- API: `pytest` desde `bloom-api`, en un entorno virtual con `requirements.txt` y `requirements-dev.txt`.

**Pruebas en dispositivo, emulador o simulador:** emulador con la app de depuración apuntando a la API desplegada (`BLOOM_API_BASE_URL_*`) y dos cuentas de prueba, una con hábitos de los tres tipos y otra sin hábitos. Los escenarios son los marcados como Manual. No hace falta hardware real.

**Limitaciones del entorno:**
- Lo que decide el modelo (preguntar, explicar, no proponer) no es determinista. Se comprueba a mano con la API desplegada, y los tests solo cubren el contrato y la validación.
- Sin el despliegue de la Lambda, las pruebas manuales quedan bloqueadas y así se registrará en TASKS.md.
- No existe la skill `mobiai-mobile-verification`, así que la verificación final seguirá este plan y quedará anotado.

<!-- Esta sección planifica la validación. Durante la implementación, registra
en TASKS.md o en el informe de validación acordado los resultados y evidencias
reales. Distingue pruebas ejecutadas, fallidas, no ejecutadas y bloqueadas.
Compilar o tener tests en verde no sustituye revisar los criterios de la spec. -->

## Orden de implementación

<!-- Etapas y dependencias principales. El desglose ejecutable se escribe en TASKS.md.
Incluye puntos de comprobación para avanzar con cambios pequeños. -->

1. **API:** contrato ampliado, herramientas, validación, historial y `habits` vacía. Comprobación: pytest en verde, con los tests nuevos y los de auth.
2. **Dominio:** modelos, `ResolveAssistantAction`, `ExecuteAssistantAction` y la ampliación de las estadísticas. Comprobación: tests de dominio.
3. **Datos:** DTO, mapper, DataSource y repositorio. Comprobación: `BloomRecommendationDataSourceTest` ampliado.
4. **ViewModel:** confirmar, cancelar, eventos y chat sin hábitos. Comprobación: `RecommendationViewModelTest`.
5. **UI:** `ActionCard`, snackbar, saludo y textos en/es, con `@Preview`. Comprobación: `assembleDebug` y revisión de las previews.
6. **Despliegue de la API** (con autorización) y **pruebas manuales** en emulador. Se registran las evidencias en TASKS.md.
7. **`spec.md`** actualizado. Comprobación: diff revisado.

## Riesgos y decisiones pendientes

<!-- Riesgos concretos de esta solución y cómo se resolverán, sin listas genéricas.
Escribe Ninguna en las decisiones pendientes cuando estén resueltas. -->

- **Riesgos y medidas acordadas:**
  - *El modelo genera llamadas a herramientas mal formadas o Groq las rechaza (`tool_use_failed`).* La API reintenta una vez sin herramientas, y tanto la API como la app validan la acción, así que nunca se muestra una tarjeta rota.
  - *El modelo propone cuando no debe (RF-01) o no pregunta (RF-07, RF-18).* Lo cubren las reglas del prompt, la validación de dominio y la confirmación obligatoria, porque nada se guarda sin pulsar. Se ajusta el prompt durante la prueba manual.
  - *Petición más grande* (días hechos de la semana y catálogo de unos 31 iconos y 12 colores): unos cientos de bytes por petición, aceptable.
  - *Fecha de hoy:* la manda el móvil, porque la API no conoce su zona horaria. La app revalida al confirmar, así que una fecha manipulada no permite escribir fuera de la semana.
  - *Duplicados al reintentar una creación:* se evitan reutilizando el id del hábito (ver Errores).
  - *Desfase de versiones entre app y API:* los campos nuevos son opcionales, así que conviene desplegar la API antes de publicar la app.
- **Decisiones pendientes:** Ninguna.
  - Resuelto: nombre propuesto desde el chat de 100 caracteres como máximo; SPEC corregida.
  - Resuelto: el agente despliega la Lambda (autorizado el 2026-11-15).
