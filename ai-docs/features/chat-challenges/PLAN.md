# PLAN: Órdenes en el chat de Bloom: crear retos

**SPEC de referencia:** `ai-docs/features/chat-challenges/SPEC.md`
**Versión de la spec revisada:** aprobada el 2026-11-15 (rama `feature/chat-actions`, sobre `08ec353`, sin commit)
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
| API de Bloom | `bloom-api/main.py` | `POST /api/chat` con tool calling (`propose_create_habit`, `propose_set_completion`). Valida con pydantic (`CreateHabitAction`, `SetCompletionAction`), describe propuestas anteriores con `_describe_action` y devuelve `{message, action?}`. Se añade una tercera herramienta. |
| Tests de la API | `bloom-api/tests/test_actions.py`, `conftest.py`, `test_auth.py` | pytest con Groq simulado. Se amplía `test_actions.py`. |
| Modelos de acción | `domain/model/AssistantAction.kt` (`AssistantAction`, `AssistantActionStatus`, `ProposedAction`, `ChatReply`, `HabitCatalog`), `domain/model/AssistantTurn.kt` | Se añade la variante de reto, el estado `Discarded` y el motivo del descarte. |
| Resolver | `domain/usecase/ResolveAssistantAction.kt` (`resolveAssistantAction`, `resolveCreate`) | Validación pura de crear hábito y marcar. La parte de nombre, frecuencia, icono y color se reutiliza para el hábito del reto. |
| Ejecutor | `domain/usecase/ExecuteAssistantAction.kt` | Revalida y guarda. Se añade la rama de reto. |
| Retos | `domain/repository/FriendsRepository.kt` (`getAcceptedFriends`, `createChallenge`), `data/repository/FriendsRepositoryImpl.kt` (`createChallenge`: valida, crea el hábito público con `newChallengeHabit`, inserta el reto, borra el hábito si falla), `data/remote/supabase/datasources/ChallengeRemoteDataSource.kt` (`insertChallenge` genera el id, `fetchChallengeById`) | Mismo guardado que la hoja de Amigos (RF-03). Hoy no es idempotente: genera ids nuevos en cada llamada. |
| Borrador del hábito del reto | `domain/model/ChallengeHabitDraft.kt`, `domain/model/ChallengeCriteria.kt` | Se reutilizan tal cual. |
| RLS de `challenges` | `supabase/migrations/20261020092754_challenge_shared_habit.sql` (`challenges_insert_as_challenger`: `private.is_friend_with(challenged_id)`, hábito público del retador) | El servidor ya rechaza retar a quien no es amigo (refuerza RF-10). Sin cambios. |
| Push al retado | Trigger `challenges_insert_push` + Edge Function `notify-challenge` | Se dispara con cualquier `INSERT`, así que el chat no necesita nada nuevo (RF-03). |
| DTO y mapper del chat | `data/remote/ai/dto/ChatActionBody.kt`, `ChatMessageBody.kt`, `ChatRequestBody.kt`, `ChatResponseBody.kt`; `data/mapper/AssistantActionMapper.kt`; `data/remote/ai/datasources/AIDataSource.kt` | Se amplían con el tipo `create_challenge` y el estado `discarded`. |
| ViewModel del chat | `presentation/screens/recommendations/RecommendationViewModel.kt` (`dispatch`, `confirmAction(index)`, `cancelAction`, `savingActions`, `ChatEvent.ShowError` por `Channel`) | Se amplía para resolver la propuesta de reto y los descartes. |
| UI del chat | `presentation/screens/recommendations/RecommendationScreen.kt`, `ActionCard.kt` | Se añade la tarjeta de reto y el aviso de la app. |
| Componentes reutilizables | `presentation/components/ProfileAvatar.kt`, `HabitIcons.kt`; textos `friends_criteria_*`, `friends_duration_days` en `strings.xml` | Foto del amigo, icono del hábito, etiquetas de criterio y duración. |
| Navegación | `presentation/navigation/NavigationBottomWrapper.kt` (`TabRecommendations`, `TabChallengeDetail(id)`) | El enlace «Ver reto» navega a la ruta existente. |
| Tests de la app | `app/src/test/.../ResolveAssistantActionTest.kt`, `ExecuteAssistantActionTest.kt`, `FakeFriendsRepository.kt`, `RecommendationViewModelTest.kt`, `BloomRecommendationDataSourceTest.kt` | Se amplían. |

**Convenciones y patrón de referencia:** el mismo que `chat-actions` (decisión de Brain `bloom-chat-actions-model-proposes-via-tool-calling-app-valid-20261115-180600`): el modelo propone, la app valida y ejecuta, y la API nunca escribe en Supabase. Clean Architecture y separación `*Screen`/`*Content` (`AGENTS.md`). Eventos one-shot con `Channel` + `ObserveAsEvents`. Textos en `strings.xml` (en y es). Social online-first sin caché Room. Ids UUIDv7 con `IdGenerator` (decisión de Brain `client-ids-uuidv7-kotlin-uuid-20260811`).

## Solución propuesta

<!-- Explica el enfoque y sus motivos. Describe las responsabilidades y el
recorrido de datos y eventos hasta la interfaz. Usa un diagrama si aporta claridad. -->

**Idea central:** igual que en `chat-actions`, pero el amigo lo resuelve la app. Bloom nunca recibe la lista de amigos (decisión 1 de la SPEC). La herramienta solo lleva `friend_name`, el nombre tal como lo escribió la persona. La app lo busca entre sus amigos aceptados y, según el resultado, muestra la tarjeta o un aviso propio en lugar del texto de Bloom.

```
Usuario ──«reta a Laura a…»──▶ API ──propose_create_challenge{friend_name, hábito, criterio, duración}──▶ {message, action}
ViewModel ──ResolveChallengeProposal──▶ getAcceptedFriends() ──matchChallengeFriend(friend_name)──┐
   ├─ 1 amigo ───────▶ tarjeta de reto (Pending; habitId y challengeId generados aquí)
   ├─ 0 / varios / sin amigos ──▶ aviso de la app en lugar del texto; turno Discarded(motivo)
   └─ fallo al leer amigos ──▶ texto de Bloom sin tarjeta + snackbar de error; Discarded(lookup_failed)
Confirmar ──ExecuteAssistantAction──▶ amigo sigue aceptado? ──▶ FriendsRepository.createChallenge(…, habitId, challengeId)
Historial: la propuesta viaja con friend_name (lo que escribió la persona) y su estado/motivo, nunca con datos del amigo
```

**Recorrido:**

1. **API (RF-01, RF-04, RF-05, RF-07, RF-12).** Nueva herramienta `propose_create_challenge`, siempre disponible, con estos argumentos:
   - `message` y `friend_name` (texto tal como lo escribió la persona, máximo 100 caracteres);
   - los campos del hábito de `propose_create_habit` salvo la descripción, porque la hoja no la tiene;
   - `criteria` (`completion_pct`, `all_days` o `streak`; por defecto `completion_pct`);
   - `duration_days` (entero; por defecto 7).

   El prompt añade estas reglas:
   - proponer solo si se pide un reto;
   - preguntar de qué hábito si no está claro;
   - copiar el nombre del amigo tal como se escribió, sin inventar ni completar;
   - Bloom no conoce a los amigos de la persona;
   - solo se permiten 3, 7, 14 o 30 días; si se pide otra duración, decir cuál se usará.

   `CreateChallengeAction` (pydantic) valida tipos y longitudes: `duration_days` de 1 a 365, y el redondeo lo hace la app. Una llamada inválida se descarta y queda solo el texto, como ahora.
2. **Historial (RF-12).** `ChatMessage.action_status` acepta `discarded`, con `discard_reason` (`friend_not_found`, `friend_ambiguous`, `no_friends` o `lookup_failed`). `_describe_action` lo convierte en una línea para el modelo, por ejemplo `[Proposal card for challenge with "Laura": not shown, several friends match that name; the person must say which one.]`.
   - Para un turno descartado, la app envía como contenido el texto original de Bloom, nunca el aviso, porque el aviso de «varios» lleva nombres de amigos.
3. **Resolver (RF-04, RF-05, RF-06).** `ResolveChallengeProposal` es un caso de uso nuevo que inyecta `FriendsRepository`:
   - Valida el hábito con la misma lógica que `resolveCreate`, extraída a una función compartida. Un hábito inválido no da tarjeta.
   - Fija el criterio (`null` pasa a `CompletionPct`) y la duración (`nearestChallengeDuration`: opciones 3, 7, 14 y 30; en empate la mayor; menos de 3 pasa a 3 y más de 30 pasa a 30).
   - Llama a `getAcceptedFriends()` y a `matchChallengeFriend`.
   - Devuelve `Card(action)`, `Discarded(reason, candidates)` o `Invalid`. Un fallo al leer los amigos da `Discarded(lookup_failed)` más el error.
4. **Búsqueda del amigo (regla de la SPEC).** `matchChallengeFriend(query, friends)` es una función pura:
   - Normaliza el texto: minúsculas, sin acentos (`java.text.Normalizer`), sin `@` inicial y con los espacios colapsados.
   - Coincidencia exacta con `displayName` o `username`: si hay una, gana. Si no, coincidencia parcial: el nombre empieza por la consulta o contiene una palabra que empieza por ella.
   - Resultados: una coincidencia da `Match`, varias dan `Ambiguous` (lista ordenada por nombre), ninguna da `NotFound`, y una lista vacía da `NoFriends`.
5. **Tarjeta (RF-01, RF-08).** `AssistantAction.CreateChallenge(challengeId, habitId, friend: Profile, draft: ChallengeHabitDraft, criteria, durationDays)`. Los dos ids se generan al resolver y se reutilizan en cada reintento.
   - Al confirmar, la tarjeta muestra «Reto enviado» y un botón «Ver reto» que navega a `TabChallengeDetail(challengeId)`.
   - Esa navegación es una acción directa del usuario, así que es un callback de UI, no un evento del ViewModel.
6. **Confirmar (RF-02, RF-03, RF-10).** `ExecuteAssistantAction` revalida con `getAcceptedFriends()`. Si el amigo ya no está, da `AssistantActionError.FriendNotFound` (error nuevo) sin escribir nada. Si está, llama a `createChallenge(challengedId, draft, criteria, durationDays, habitId, challengeId)`.
7. **Idempotencia (CA-04, CA-05).** `createChallenge` acepta `habitId` y `challengeId` opcionales; la hoja de Amigos no los pasa y sigue igual. `insertChallenge` recibe el id.
   - Si el `INSERT` falla, antes de borrar el hábito la implementación comprueba con `fetchChallengeById(challengeId)` si el reto ya existe, por ejemplo cuando la respuesta se perdió o el reintento choca con la clave primaria. Si existe, devuelve éxito y no borra el hábito.
   - El hábito usa `upsertHabit`, que ya es idempotente.
   - Esto también arregla un riesgo actual de la hoja: si se perdía la respuesta, se borraba el hábito de un reto que sí se había creado.

**Compatibilidad:** los campos nuevos son opcionales y la app ignora claves desconocidas. Una app antigua no recibe la herramienta nueva como problema: verá un `action.type` desconocido y el DataSource lo descartará, quedando solo el texto. Esto se comprueba en la fase 3 de la implementación. Conviene desplegar la API antes de publicar la app.

## Módulos y componentes afectados

<!-- Si el proyecto está modularizado, identifica los módulos afectados, sus
responsabilidades y la dirección de sus dependencias. Respeta los límites
existentes y justifica cualquier módulo o dependencia nueva. Si no está
modularizado, describe las carpetas o componentes afectados sin introducir
modularización fuera del alcance; marca la tabla de módulos como No aplica. -->

| Módulo | Existe / nuevo | Responsabilidad y cambios | Dependencias afectadas |
| --- | --- | --- | --- |
| No aplica | — | Un solo módulo `:app`, más `bloom-api` como servicio aparte. | — |

<!-- Distingue lo que se reutiliza, modifica o crea. Las rutas nuevas son propuestas.
Señala impacto sobre modelos, contratos o componentes compartidos. -->

| Componente o ruta | Acción | Cambio y responsabilidad | Requisito relacionado |
| --- | --- | --- | --- |
| `bloom-api/main.py` | Modificar | `CreateChallengeAction`, herramienta `propose_create_challenge`, `_parse_tool_call`, `discarded` + `discard_reason` en `ChatMessage`, `_describe_action` y reglas del prompt. | RF-01, RF-04, RF-05, RF-07, RF-12 |
| `bloom-api/tests/test_actions.py` | Modificar | Casos de la herramienta nueva, descartes en el historial y que ningún dato de amigos aparece en el prompt. | CA-01, CA-02, CA-07 a CA-10, CA-12 |
| `domain/model/AssistantAction.kt` | Modificar | `AssistantAction.CreateChallenge`, `ProposedAction.CreateChallenge`, `AssistantActionStatus.Discarded`, enum `ChallengeDiscardReason`. | RF-01, RF-06, RF-12 |
| `domain/model/AssistantTurn.kt` | Modificar | `discardReason: ChallengeDiscardReason?` y `notice: ChatNotice?` (aviso de la app, con los candidatos si los hay; solo para mostrar). | RF-06, RF-12 |
| `domain/usecase/ResolveAssistantAction.kt` | Modificar | Extrae la validación del hábito a una función interna compartida, sin cambiar el comportamiento actual. | RF-04 |
| `domain/usecase/ResolveChallengeProposal.kt` | Crear | Caso de uso: valida, fija valores por defecto y duración, y resuelve el amigo. | RF-04 a RF-07 |
| `domain/usecase/MatchChallengeFriend.kt` | Crear | `matchChallengeFriend` y `nearestChallengeDuration` (funciones puras). | RF-05, RF-06 |
| `domain/usecase/ExecuteAssistantAction.kt` | Modificar | Inyecta `FriendsRepository`; rama `CreateChallenge` con revalidación del amigo. | RF-02, RF-03, RF-10 |
| `domain/model/errors/AssistantActionError.kt` | Modificar | `FriendNotFound`. | RF-10 |
| `domain/repository/FriendsRepository.kt` | Modificar | `createChallenge(…, habitId: String? = null, challengeId: String? = null)`. | RF-02, RF-03 |
| `data/repository/FriendsRepositoryImpl.kt` | Modificar | Usa los ids recibidos; comprueba si el reto existe antes de borrar el hábito tras un fallo. | CA-04, CA-05 |
| `data/remote/supabase/datasources/ChallengeRemoteDataSource.kt` | Modificar | `insertChallenge(id, …)`. | CA-04 |
| `data/remote/ai/dto/ChatActionBody.kt`, `ChatMessageBody.kt` | Modificar | Campos de reto (`friend_name`, `criteria`, `duration_days`) y `discard_reason`. | RF-12 |
| `data/mapper/AssistantActionMapper.kt` | Modificar | DTO ↔ dominio de la variante de reto. En el historial, `friend_name` sale de la consulta original, nunca del `Profile`. | RF-12, privacidad |
| `data/remote/ai/datasources/AIDataSource.kt` | Modificar | Analiza `create_challenge`; un tipo desconocido o malformado se descarta con `Log.w`. | Compatibilidad |
| `presentation/.../RecommendationViewModel.kt` | Modificar | En `dispatch`, una propuesta de reto pasa por `ResolveChallengeProposal`; los descartes crean el turno con `Discarded` y `notice`; `lookup_failed` envía `ChatEvent.ShowError`. `confirmAction` y `cancelAction` no cambian. | RF-02, RF-06, RF-12 |
| `presentation/.../ActionCard.kt` | Modificar | `ChallengeActionCard`: `ProfileAvatar` y nombre, icono, nombre y color del hábito, frecuencia, criterio, duración, Retar y Cancelar, y en estado hecho «Reto enviado» con «Ver reto». Semántica fusionada y `@Preview`. | RF-01, RF-08, accesibilidad |
| `presentation/.../RecommendationScreen.kt` | Modificar | Muestra el `notice` en lugar del texto de Bloom; callback `onOpenChallenge(id)`. | RF-06, RF-08 |
| `presentation/navigation/NavigationBottomWrapper.kt` | Modificar | `RecommendationScreen(onOpenChallenge = { navigate(TabChallengeDetail(it)) })`. | RF-08 |
| `res/values/strings.xml`, `values-es/strings.xml` | Modificar | Tarjeta, estados, avisos (no encontrado, varios con la lista, sin amigos), `FriendNotFound`. Se reutilizan `friends_criteria_*` y `friends_duration_days`. | Idiomas |
| `spec.md` | Modificar | Apartado del chat: retos desde el chat. | RF-11 |

## Datos y contratos

<!-- Completa solo lo aplicable. Si un punto no aplica, indica el motivo. -->

- **Modelos y contratos de entrada y salida:**
  - La respuesta puede traer `action = {type:"create_challenge", friend_name, name, icon, color, schedule{mode, days[], weekly_target}, criteria, duration_days}`.
  - En los mensajes de la petición, `action` admite ese tipo y `action_status` admite `discarded` junto a `discard_reason`.
  - `criteria` se envía en `snake_case` y se mapea a `ChallengeCriteria` en el mapper.
- **Identificadores, relaciones y restricciones:**
  - `challengeId` y `habitId` son UUIDv7 generados al resolver la propuesta.
  - `friend.id` es el `challenged_id`. La RLS exige amistad aceptada y un hábito público del retador, y `newChallengeHabit` ya lo crea público.
  - El nombre del hábito tiene un máximo de 100 caracteres y `friend_name` también.
- **Origen de los datos mostrados y transformaciones:**
  - Amigo: `getAcceptedFriends()` (Supabase), con `Profile.displayName` y `avatarUrl` o `avatarColor`.
  - Hábito: iconos y colores del catálogo, como en la tarjeta de crear.
  - Criterio y duración: textos existentes de Amigos.
- **Persistencia, consultas y actualizaciones:** no hay tablas ni columnas nuevas. Se usan `upsertHabit` y el `INSERT` en `challenges` de siempre. La conversación sigue en memoria.
- **Convivencia entre datos locales y remotos:** sin cambios. El hábito del reto entra en Room por `insertHabit`. Amigos e Inicio recargan los retos e insignias en `ON_RESUME` al volver del chat (RF-09).
- **Compatibilidad y migraciones de datos existentes:** no hay migraciones. La compatibilidad del contrato se explica en la Solución propuesta.

## Estado, operaciones y errores

<!-- Cómo se implementan los comportamientos aprobados en la spec.
Referencia RF/CA y aplica las consideraciones relevantes de MOBILE_GUIDELINES.md. -->

- **Gestión del estado de interfaz y navegación:**
  - El estado de la tarjeta sigue saliendo de `turn.actionStatus` y de `savingActions`.
  - `Discarded` no tiene botones y pinta el `notice`.
  - Los errores van por `ChatEvent.ShowError`, el `Channel` que ya existe, y se muestran con `ObserveAsEvents`.
  - «Ver reto» es un callback de UI hacia la ruta `TabChallengeDetail`, sin cambios en la navegación.
- **Conservación y restauración del estado:** igual que `chat-actions`: el estado vive en el ViewModel, sobrevive a la rotación y al segundo plano, y se pierde si el proceso termina (lo acepta la SPEC).
- **Ejecución, concurrencia y cancelación de operaciones:**
  - Mientras se resuelve el amigo (una petición más), el chat sigue en `Sending`, así que no se puede enviar otro mensaje a la vez.
  - `confirmAction` ya ignora un segundo toque mientras se guarda (RF-02).
  - La ejecución corre en `viewModelScope`.
- **Errores, reintentos y prevención de duplicados:**
  - Si falla el guardado, la tarjeta vuelve a `Pending` con un snackbar.
  - Al reintentar se usan los mismos ids, y la comprobación «el reto ya existe» evita un reto o un hábito duplicados (CA-04, CA-05).
  - Si el amigo ya no está, `FriendNotFound` y no se escribe nada (CA-11). Si la app no lo detecta y la RLS rechaza el `INSERT`, se borra el hábito y se muestra el error, como hace hoy la hoja.
  - Un fallo al leer los amigos tras la respuesta de Bloom da el texto de Bloom sin tarjeta, un snackbar de red y `Discarded(lookup_failed)`. La persona puede repetir la petición.
- **Otras consideraciones mobile aplicables y su solución:**
  - Accesibilidad: la tarjeta fusiona su semántica («Propuesta de reto con Laura: Meditar, diario, racha, 14 días. Estado: pendiente»). Los botones tienen texto y un mínimo de 48 dp, y el estado se indica con texto.
  - Texto grande: `FlowRow` para los botones, como en la tarjeta actual.
  - Privacidad: los `Log` no recogen `friend_name`, nombres ni ids de amigos, solo el tipo de acción y el resultado. El prompt nunca recibe datos de amigos (lo comprueba un test).

## Dependencias y configuración

<!-- Librerías, servicios, permisos o configuración afectados. Verifica compatibilidad
con el proyecto y justifica las incorporaciones. No agregues dependencias por defecto. -->

- No hay dependencias, permisos, secretos ni migraciones nuevos.
- **Despliegue:** hay que volver a desplegar la Lambda `bloom-api` (`sam deploy`). La autorización del 2026-11-15 era para `chat-actions`. Para este despliegue te pediré confirmación de nuevo.

## Estrategia de validación

<!-- Una fila por criterio de la spec. Selecciona el método capaz de demostrarlo:
test unitario, integración, UI o prueba manual. No todos requieren todos los métodos.
Identifica tests existentes y separa los nuevos propuestos. Incluye regresiones relevantes.
Una captura aislada no demuestra persistencia ni ausencia de peticiones de red. -->

Leyenda: **API** = pytest con Groq simulado; **Dom** = test unitario de dominio con `FakeFriendsRepository`; **Repo** = test de `FriendsRepositoryImpl` con fakes; **VM** = `RecommendationViewModelTest`; **Manual** = emulador con la API desplegada y dos cuentas amigas (más una sin amigos).

| Criterio | Método y test existente o propuesto | Entorno y datos necesarios | Evidencia prevista |
| --- | --- | --- | --- |
| CA-01 | API: herramienta con mínimos da `action` con criterio y duración por defecto. Dom: resolver con amigo único da `Card` con `CompletionPct` y 7. Manual | Cuentas amigas | pytest + test + captura |
| CA-02 | API: `weekdays` con 3 días, `streak` y 14. Manual | Ídem | pytest + captura |
| CA-03 | Dom: ejecutar llama a `createChallenge` con amigo, borrador, ids. Manual: Amigos, Inicio con insignia, push en la otra cuenta, «Ver reto» abre el detalle | Dos dispositivos o emuladores con sesión | Test + capturas |
| CA-04 | VM: doble `confirmAction` ejecuta una vez (existente, se amplía a reto). Repo: segundo `createChallenge` con mismos ids no duplica | Fakes | Tests |
| CA-05 | Repo: `INSERT` falla y el reto existe → éxito sin borrar el hábito; no existe → borra y error. VM: error → `Pending`. Manual: modo avión | Fakes; emulador | Tests + captura |
| CA-06 | VM: cancelar → `Cancelled` sin llamar al repositorio (existente, se amplía) | Fakes | Test |
| CA-07 | Dom: `matchChallengeFriend` (no encontrado, varios, exacto gana a parcial, acentos, `@usuario`). VM: aviso en lugar del texto y `Discarded` con motivo. API: el historial descartado llega como línea. Manual: secuencia con dos Lauras | Cuenta con dos amigos «Laura …» | Tests + capturas |
| CA-08 | Dom: sin amigos → `NoFriends`. Manual | Cuenta sin amigos | Test + captura |
| CA-09 | API: el prompt contiene la regla de preguntar el hábito. Manual | Cuentas amigas | pytest + captura |
| CA-10 | Dom: `nearestChallengeDuration` (10→7, 5→7, 1→3, 60→30, 14→14). API: el prompt contiene las duraciones. Manual: Bloom avisa | — | Tests + captura |
| CA-11 | Dom: el amigo desaparece entre resolver y ejecutar → `FriendNotFound` sin llamar a `createChallenge`. Manual: borrar la amistad y confirmar | Dos cuentas | Test + captura |
| CA-12 | API: sin herramienta llamada, `action` nula (existente). Manual | — | pytest + captura |

**Comprobaciones de regresión:**
- `test_actions.py` y `test_auth.py` actuales.
- `ResolveAssistantActionTest` (la extracción no cambia el comportamiento), `ExecuteAssistantActionTest`, `RecommendationViewModelTest` y `BloomRecommendationDataSourceTest`.
- Los tests de `FriendsViewModel` (la hoja de Amigos sin ids sigue igual).
- Test nuevo de privacidad: el cuerpo serializado de la petición no contiene el nombre visible, el usuario ni el id del amigo resuelto.
- Manual: crear un reto desde la hoja de Amigos sigue funcionando.

**Comandos verificados para compilar y ejecutar tests:**
- App: `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
- API: `pytest` desde `bloom-api`, con `requirements.txt` y `requirements-dev.txt`.

**Pruebas en dispositivo, emulador o simulador:** dos emuladores, o un emulador y un móvil, con cuentas amigas para el push y el detalle. Una tercera cuenta sin amigos y una con dos amigos de nombre parecido. No hace falta hardware real salvo que el push de FCM no llegue al emulador; en ese caso se usará un móvil.

**Limitaciones del entorno:**
- Lo que decide el modelo (proponer, preguntar, avisar de la duración) solo se comprueba a mano con la API desplegada.
- Sin el despliegue, las pruebas manuales quedan bloqueadas y se anotará en TASKS.md.
- La skill `mobiai-mobile-verification` no existe en el repo, así que la verificación final seguirá este plan.

<!-- Esta sección planifica la validación. Durante la implementación, registra
en TASKS.md o en el informe de validación acordado los resultados y evidencias
reales. Distingue pruebas ejecutadas, fallidas, no ejecutadas y bloqueadas.
Compilar o tener tests en verde no sustituye revisar los criterios de la spec. -->

## Orden de implementación

<!-- Etapas y dependencias principales. El desglose ejecutable se escribe en TASKS.md.
Incluye puntos de comprobación para avanzar con cambios pequeños. -->

1. **Repositorio idempotente:** ids opcionales en `createChallenge` e `insertChallenge`, y la comprobación antes de borrar. Comprobación: tests de repositorio y de Amigos en verde.
2. **API:** herramienta, validación, descartes en el historial y prompt. Comprobación: pytest en verde.
3. **Dominio:** modelos, `matchChallengeFriend`, `nearestChallengeDuration`, `ResolveChallengeProposal`, la extracción en el resolver y la rama del ejecutor. Comprobación: tests de dominio.
4. **Datos del chat:** DTO, mapper y DataSource, más el test de privacidad del cuerpo. Comprobación: `BloomRecommendationDataSourceTest`.
5. **ViewModel y UI:** dispatch, `notice`, tarjeta de reto, «Ver reto», textos en/es y `@Preview`. Comprobación: `RecommendationViewModelTest` y `assembleDebug`.
6. **Despliegue de la API** (con tu confirmación) y **pruebas manuales**. Las evidencias se registran en TASKS.md.
7. **`spec.md`** actualizado y decisión guardada en Brain. Comprobación: diff revisado.

## Riesgos y decisiones pendientes

<!-- Riesgos concretos de esta solución y cómo se resolverán, sin listas genéricas.
Escribe Ninguna en las decisiones pendientes cuando estén resueltas. -->

- **Riesgos y medidas acordadas:**
  - *El modelo inventa o «corrige» el nombre del amigo.* El prompt exige copiarlo tal cual. Además, la app solo acepta coincidencias reales y nada se envía sin confirmar.
  - *El texto de Bloom contradice el aviso de la app («aquí tienes tu reto con Laura»).* Al descartar, se muestra el aviso en lugar del texto (RF-06), y la línea del historial corrige al modelo.
  - *Fuga de datos de amigos al historial.* El contenido de los turnos descartados es el texto de Bloom, no el aviso, y el mapper usa `friend_name` y nunca el `Profile`. Lo cubre un test.
  - *Duplicados al reintentar.* Se evitan con ids fijos y la comprobación de existencia antes de borrar. Esto también corrige el caso equivalente en la hoja de Amigos.
  - *Una petición extra a Supabase en cada propuesta de reto.* Solo ocurre cuando hay una propuesta de reto, y es la misma consulta que usa Amigos.
  - *Coincidencias parciales demasiado amplias* (por ejemplo, «a»). Si hay varias, se pregunta; si la consulta tiene menos de 2 caracteres, se trata como no encontrada.
- **Decisiones pendientes:** Ninguna.
