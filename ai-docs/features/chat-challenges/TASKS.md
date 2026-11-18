# TASKS: Órdenes en el chat de Bloom: crear retos

**SPEC:** `SPEC.md` (Aprobada) · **PLAN:** `PLAN.md` (Aprobado) · **Implementación autorizada:** 2026-11-16 (el despliegue de la Lambda requiere confirmación aparte)

| ID | Objetivo | Alcance | Depende de | RF / CA | Validación | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| T-01 | `createChallenge` idempotente: ids opcionales y comprobación de existencia antes de borrar el hábito | `FriendsRepository.kt`, `FriendsRepositoryImpl.kt`, `CreateChallengeSteps.kt` (nuevo), `ChallengeRemoteDataSource.kt` + `CreateChallengeStepsTest` | — | RF-02, RF-03 / CA-04, CA-05 | `testDebugUnitTest` | Hecha |
| T-02 | API: herramienta `propose_create_challenge`, validación, descartes en el historial y prompt | `bloom-api/main.py`, `tests/test_actions.py` | — | RF-01, RF-04, RF-05, RF-07, RF-12 / CA-01, CA-02, CA-07, CA-09, CA-10, CA-12 | `pytest` | Hecha |
| T-03 | Dominio: modelos, `matchChallengeFriend`, `nearestChallengeDuration`, `ResolveChallengeProposal`, rama del ejecutor | `domain/model/*`, `domain/usecase/*` + `MatchChallengeFriendTest`, `ResolveChallengeProposalTest`, `ExecuteChallengeActionTest` | T-01 | RF-04 a RF-06, RF-10 / CA-01, CA-03, CA-07, CA-08, CA-10, CA-11 | `testDebugUnitTest` | Hecha |
| T-04 | Datos del chat: DTO, mapper, DataSource; test de privacidad del cuerpo | `data/remote/ai/*`, `data/mapper/AssistantActionMapper.kt` + `BloomRecommendationDataSourceTest` | T-03 | RF-12 / privacidad | `testDebugUnitTest` | Hecha |
| T-05 | ViewModel y UI: resolución, avisos, tarjeta de reto, «Ver reto», textos en/es | `RecommendationViewModel.kt`, `RecommendationScreen.kt`, `ActionCard.kt`, `NavigationBottomWrapper.kt`, `AppErrorUiMessages.kt`, `strings.xml` en/es + `RecommendationViewModelTest` | T-04 | RF-01, RF-02, RF-06, RF-08 / CA-04, CA-06, CA-07 | `testDebugUnitTest`, `assembleDebug` | Hecha (preview sin revisar visualmente) |
| T-06 | Desplegar la API | `bloom-api` (`sam build` + `sam deploy`) | T-02 | — | `/health` 200, `/api/chat` sin token 401 | Pendiente (requiere confirmación) |
| T-07 | Validación manual | App de depuración + 2 cuentas amigas | T-05, T-06 | CA-01 a CA-12 (Manual) | Capturas y notas | Pendiente (bloqueada por T-06) |
| T-08 | Documentar en `spec.md` y Brain | `spec.md`, `.mobiai/brain` | T-05 | RF-11 | Revisión del diff | Hecha |

## Resultados de validación

Ejecutado el 2026-11-16:

| Comprobación | Resultado | Criterios |
| --- | --- | --- |
| `pytest` en `bloom-api` (venv local) | Pasa: 47 tests (12 nuevos de retos; 2 tests existentes actualizados porque la lista de herramientas ahora incluye la de retos) | CA-01, CA-02, CA-07, CA-09, CA-10, CA-12 (contrato, validación y prompt) |
| `./gradlew :app:testDebugUnitTest` | Pasa: 400 tests, 0 fallos. Nuevos: `CreateChallengeStepsTest` 5, `MatchChallengeFriendTest` 8, `ResolveChallengeProposalTest` 8, `ExecuteChallengeActionTest` 3, `RecommendationViewModelTest` +6, `BloomRecommendationDataSourceTest` +3 (incluye el de privacidad) | CA-01 a CA-08, CA-10, CA-11, RF-12, privacidad |
| `./gradlew :app:assembleDebug` | Compila | RF-01, RF-08 |
| `./gradlew :app:lintDebug` | 13 errores, todos anteriores (`AvatarJpegEncoder` NewApi, `themes.xml`, `MonthCalendar`, `HomeScreen`). Ninguno en archivos de esta funcionalidad | Regresión |

**Pendiente:**
- T-06: despliegue de la Lambda, a la espera de confirmación.
- T-07 (manual, tras el despliegue): lo que decide el modelo real (CA-01, CA-02, CA-09, CA-10, CA-12); flujo completo con push y detalle (CA-03); modo avión (CA-05); dos Lauras (CA-07); cuenta sin amigos (CA-08); amistad borrada antes de confirmar (CA-11); regresión de la hoja de Amigos; revisión visual de la tarjeta con texto grande y TalkBack.

**Cambios respecto al plan:**
- La lógica idempotente de `createChallenge` se extrajo a `data/repository/CreateChallengeSteps.kt` (función interna con los pasos como parámetros) para poder probarla: `FriendsRepositoryImpl` usa DataSources concretos sobre `SupabaseClient` y no tenía tests.
- La validación de la propuesta de reto vive en `checkChallengeProposal` (función interna en `ResolveChallengeProposal.kt`).
- El aviso de la app no se guarda como campo aparte (`notice`): se calcula en la UI a partir de `AssistantTurn.discard` (motivo, nombre escrito y candidatos).
- Los tests del ejecutor de retos están en un archivo nuevo, `ExecuteChallengeActionTest.kt`.
