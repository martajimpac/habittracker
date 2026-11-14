# PLAN: Proteger la API de Bloom con la sesión del usuario

**SPEC de referencia:** `ai-docs/features/bloom-api-auth/SPEC.md`
**Versión de la spec revisada:** Aprobada el 2026-11-14 (sin commit todavía)
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

| Componente o archivo existente | Ruta verificada | Responsabilidad y uso previsto |
| --- | --- | --- |
| API FastAPI | `bloom-api/main.py` (repo git propio) | `/health` y `/api/chat`; se añade la verificación del token |
| Plantilla SAM | `bloom-api/template.yaml` | Lambda en imagen + Function URL; se añaden concurrencia, presupuesto y parámetro de Supabase |
| Imagen Lambda | `bloom-api/Dockerfile.lambda` | Solo copia `main.py`; sin cambios |
| Transporte HTTP | `app/.../data/remote/ai/RecommendationTransport.kt`, `KtorAITransport.kt` | POST JSON con Ktor; recibirá el token |
| DataSource del chat | `app/.../data/remote/ai/datasources/AIDataSource.kt` (`BloomRecommendationDataSource`) | Construye la petición e interpreta códigos; añade token, 401 y reintento |
| Sesión Supabase | `app/.../data/remote/supabase/SupabaseSession.kt` | Ya resuelve el usuario; dará el token y lo refrescará |
| Errores del chat | `app/.../domain/model/errors/RecommendationError.kt` | Nuevo caso de sesión |
| Mapeo a textos | `RecommendationViewModel.kt` (`toMessageRes`), `presentation/utils/AppErrorUiMessages.kt` | Nuevo texto |
| DI | `app/.../di/BloomApiModule.kt` | Pasa la sesión al DataSource |
| Tests | `BloomRecommendationDataSourceTest.kt`, `RecommendationViewModelTest.kt` | Se amplían |

**Hecho verificado:** el proyecto Supabase publica claves de firma asimétricas (ES256) en `https://<proyecto>.supabase.co/auth/v1/.well-known/jwks.json`. La API puede validar tokens con la clave pública, sin ningún secreto.

**Convenciones y patrón de referencia:**
- El DataSource devuelve `DataResult<_, RecommendationError>`.
- Los ViewModels mapean a `@StringRes`.
- Logs con `TAG` y `Log.e(TAG, msg, throwable)`.
- Los DataSources usan `SupabaseSession` para la sesión.
- Tests con fakes.

## Solución propuesta

<!-- Explica el enfoque y sus motivos. Describe las responsabilidades y el
recorrido de datos y eventos hasta la interfaz. Usa un diagrama si aporta claridad. -->

**API (RF-01, RF-05, RF-07):**
- Una dependencia FastAPI `require_user`, aplicada solo a `/api/chat`, lee `Authorization: Bearer <token>`.
- Valida el token con `PyJWT` y `PyJWKClient`. El cliente descarga y cachea el JWKS de `SUPABASE_URL`; la descarga se hace con `asyncio.to_thread` para no bloquear.
- Comprueba: algoritmo ES256 (lista cerrada), firma, `exp`, `iss = <SUPABASE_URL>/auth/v1`, `aud = authenticated`, `role = authenticated`, `sub` presente y `is_anonymous` distinto de true.
- Cualquier fallo devuelve 401 con `WWW-Authenticate: Bearer` antes de tocar Groq.
- Si `SUPABASE_URL` no está configurada o el JWKS no se puede descargar, devuelve 503, porque no es culpa del usuario.
- Los logs nunca imprimen el token, solo el tipo de error.

**Infraestructura (RF-06), en `template.yaml`:**
- `ReservedConcurrentExecutions` en un parámetro, con 5 por defecto.
- ~~`AWS::Budgets::Budget` en la plantilla~~ → la alerta de 5 USD/mes (gasto real al 80 % y previsto al 100 %) se crea en AWS Budgets fuera de la plantilla, porque CloudFormation no admite ese recurso en `eu-north-1` (ver TASKS.md).
- Nuevo parámetro `SupabaseUrl`.

**App:**
```
ViewModel ─ send ─▶ Repository ─▶ BloomRecommendationDataSource
                                   │ 1. token = accessTokens.current()  (null → SessionExpired, sin petición)
                                   │ 2. transport.post(url, body, token)
                                   │ 3. 401 → accessTokens.refresh()
                                   │        ├ Token → post una vez más (401 otra vez → SessionExpired)
                                   │        ├ Rejected/NoSession → SessionExpired
                                   │        └ NetworkFailure → Unavailable
                                   ▼
                      KtorAITransport (cabecera Authorization)
```
- Nueva interfaz `BloomAccessTokens` en `data/remote/ai`. La implementa `SupabaseSession`:
  - `current()` espera a que la sesión se inicialice y devuelve `currentSessionOrNull()?.accessToken`.
  - `refresh()` llama a `refreshCurrentSession()` y distingue error de autenticación de error de red.
  - Así el DataSource no depende de `SupabaseClient` y se prueba con un fake.
- `RecommendationError.SessionExpired` se mapea al nuevo texto `recommendation_error_session` (EN/ES) en el ViewModel y en `AppErrorUiMessages`.

## Módulos y componentes afectados

<!-- Si el proyecto está modularizado, identifica los módulos afectados, sus
responsabilidades y la dirección de sus dependencias. Respeta los límites
existentes y justifica cualquier módulo o dependencia nueva. Si no está
modularizado, describe las carpetas o componentes afectados sin introducir
modularización fuera del alcance; marca la tabla de módulos como No aplica. -->

| Módulo | Existe / nuevo | Responsabilidad y cambios | Dependencias afectadas |
| --- | --- | --- | --- |
| No aplica | — | La app es un único módulo `:app`. `bloom-api` es un repo aparte. | — |

<!-- Distingue lo que se reutiliza, modifica o crea. Las rutas nuevas son propuestas.
Señala impacto sobre modelos, contratos o componentes compartidos. -->

| Componente o ruta | Acción | Cambio y responsabilidad | Requisito relacionado |
| --- | --- | --- | --- |
| `bloom-api/main.py` | Modificar | `require_user` en `/api/chat`; `/health` sin cambios | RF-01, RF-05, RF-07 |
| `bloom-api/requirements.txt` | Modificar | `PyJWT[crypto]` | RF-01 |
| `bloom-api/requirements-dev.txt` | Crear | `pytest` (+ `httpx` ya presente) | CA-01, CA-02, CA-07 |
| `bloom-api/tests/test_auth.py` | Crear | Tests con clave EC generada y JWKS falso | CA-01, CA-02, CA-07, CA-09 |
| `bloom-api/template.yaml` | Modificar | `SupabaseUrl`, `AlertEmail`, concurrencia 5, presupuesto 5 USD | RF-06 |
| `bloom-api/.env` (local, ignorado) | Documentar | Añadir `SUPABASE_URL` para el uso local | RF-01 |
| `data/remote/ai/BloomAccessTokens.kt` | Crear | Interfaz + resultado del refresco | RF-02, RF-03 |
| `data/remote/supabase/SupabaseSession.kt` | Modificar | Implementa `BloomAccessTokens` | RF-02, RF-03, RF-04 |
| `data/remote/ai/RecommendationTransport.kt`, `KtorAITransport.kt` | Modificar | Parámetro `bearerToken` y cabecera | RF-02 |
| `data/remote/ai/datasources/AIDataSource.kt` | Modificar | Token, 401, un reintento, sin sesión | RF-02, RF-03, RF-04 |
| `domain/model/errors/RecommendationError.kt` | Modificar | `SessionExpired` | RF-03, RF-04 |
| `RecommendationViewModel.kt`, `AppErrorUiMessages.kt` | Modificar | Mapeo al nuevo texto | RF-03, RF-04 |
| `res/values/strings.xml`, `res/values-es/strings.xml` | Modificar | `recommendation_error_session` | RF-03, RF-04 |
| `di/BloomApiModule.kt` | Modificar | Inyecta `SupabaseSession` en el DataSource | RF-02 |
| Tests de DataSource y ViewModel | Modificar | Casos nuevos | CA-03 a CA-06, CA-09 |
| `spec.md` | Modificar | Breve apartado de Bloom: autenticación y nuevo error | Fuente de verdad (`AGENTS.md`) |

## Datos y contratos

<!-- Completa solo lo aplicable. Si un punto no aplica, indica el motivo. -->

- **Modelos y contratos de entrada y salida:** el cuerpo de petición y respuesta de `/api/chat` no cambia. Nueva cabecera obligatoria `Authorization: Bearer <access_token>`. Nuevo código 401 con `{"detail": "..."}`.
- **Identificadores, relaciones y restricciones:** se usa el `sub` del token solo para validar, no para guardar nada.
- **Origen de los datos mostrados y transformaciones:** no aplica; no cambian.
- **Persistencia, consultas y actualizaciones:** no aplica; la API no guarda nada y la app reutiliza la sesión que ya persiste el SDK de Supabase.
- **Convivencia entre datos locales y remotos:** no aplica.
- **Compatibilidad y migraciones de datos existentes:** las versiones de la app ya publicadas no envían token y dejarán de poder usar Bloom cuando se active la API protegida. Es lo esperado: es el abuso que queremos cortar.

## Estado, operaciones y errores

<!-- Cómo se implementan los comportamientos aprobados en la spec.
Referencia RF/CA y aplica las consideraciones relevantes de MOBILE_GUIDELINES.md. -->

- **Gestión del estado de interfaz y navegación:** sin cambios. El nuevo error usa el mismo `errorRes` y el botón de reintentar que ya existen.
- **Conservación y restauración del estado:** sin cambios.
- **Ejecución, concurrencia y cancelación de operaciones:** el refresco y el reintento ocurren dentro del mismo `dispatch` en `viewModelScope`. `CancellationException` se relanza siempre.
- **Errores, reintentos y prevención de duplicados:**
  - Como máximo un refresco y un reintento por mensaje (CA-05).
  - Un fallo de red al refrescar da `Unavailable`, no `SessionExpired`.
  - El ViewModel ya impide envíos simultáneos (`ChatPhase.Sending`).
- **Otras consideraciones mobile aplicables y su solución:** tras matar el proceso, `current()` espera a que se cargue la sesión guardada (`awaitInitialization`). Los logs registran el resultado, nunca el token (RF-07).

## Dependencias y configuración

<!-- Librerías, servicios, permisos o configuración afectados. Verifica compatibilidad
con el proyecto y justifica las incorporaciones. No agregues dependencias por defecto. -->

- **API:** `PyJWT[crypto]` para validar ES256 y leer el JWKS (`PyJWKClient`). Es la librería estándar y trae `cryptography`. `pytest` solo en desarrollo.
- **App:** ninguna dependencia nueva; se usa `auth-kt` 3.7.0 y Ktor 3.5.1, que ya están.
- **AWS:**
  - Parámetros nuevos de despliegue: `SupabaseUrl` (no secreta) y `AlertEmail`.
  - AWS Budgets envía un correo de confirmación que hay que aceptar.
  - Reservar concurrencia falla si la cuenta tiene un límite total menor de 15. Ver riesgos.

## Estrategia de validación

<!-- Una fila por criterio de la spec. Selecciona el método capaz de demostrarlo:
test unitario, integración, UI o prueba manual. No todos requieren todos los métodos.
Identifica tests existentes y separa los nuevos propuestos. Incluye regresiones relevantes.
Una captura aislada no demuestra persistencia ni ausencia de peticiones de red. -->

| Criterio | Método y test existente o propuesto | Entorno y datos necesarios | Evidencia prevista |
| --- | --- | --- | --- |
| CA-01 | pytest nuevo: sin cabecera, mal formado, otra clave, caducado, `aud`/`iss` erróneos, anónimo; Groq simulado no llamado | Clave EC generada en el test | Salida de pytest |
| CA-02 | pytest nuevo: token válido → 200 con Groq simulado. Manual tras desplegar con un token real | Despliegue AWS (propietaria) | pytest + `curl` |
| CA-03 | Test nuevo de DataSource: el fake de transporte recibe el token | JVM | `testDebugUnitTest` |
| CA-04 | Test nuevo de DataSource: 401 → refresh OK → 200 | JVM | `testDebugUnitTest` |
| CA-05 | Test nuevo de DataSource: 401, 401 → `SessionExpired`, 2 peticiones exactas | JVM | `testDebugUnitTest` |
| CA-06 | Test nuevo de DataSource: sin token → 0 peticiones. Test de ViewModel: muestra el texto | JVM | `testDebugUnitTest` |
| CA-07 | pytest nuevo: `/health` sin token → 200. Invocación local de la imagen Lambda | Docker | pytest + salida |
| CA-08 | Revisión de `template.yaml` (`sam validate --lint` si SAM está instalado) y consola AWS tras desplegar | Cuenta AWS (propietaria) | Plantilla + confirmación de la propietaria |
| CA-09 | pytest: el token no aparece en los logs capturados. Revisión de los `Log.*` del DataSource | — | pytest + revisión |

**Comprobaciones de regresión:** los tests actuales de `BloomRecommendationDataSourceTest` y `RecommendationViewModelTest` siguen pasando (adaptados a la nueva firma). `assembleDebug` compila.

**Comandos verificados para compilar y ejecutar tests:**
- App: `./gradlew :app:testDebugUnitTest :app:assembleDebug` (es el que usa CI).
- API: `pytest` desde `bloom-api` en un entorno virtual con `requirements.txt` y `requirements-dev.txt`.

**Pruebas en dispositivo, emulador o simulador:** enviar un mensaje a Bloom con sesión, contra la API desplegada en AWS. Requiere el despliegue de la propietaria.

**Limitaciones del entorno:**
- Aquí no hay AWS CLI/SAM ni credenciales, así que CA-08, el `curl` de CA-02 y la prueba en dispositivo quedan pendientes de la propietaria.
- En este equipo hay Python 3.14 y Lambda usa 3.12. Por eso pytest se ejecuta también dentro de la imagen Docker de 3.12.

<!-- Esta sección planifica la validación. Durante la implementación, registra
en TASKS.md o en el informe de validación acordado los resultados y evidencias
reales. Distingue pruebas ejecutadas, fallidas, no ejecutadas y bloqueadas.
Compilar o tener tests en verde no sustituye revisar los criterios de la spec. -->

## Orden de implementación

<!-- Etapas y dependencias principales. El desglose ejecutable se escribe en TASKS.md.
Incluye puntos de comprobación para avanzar con cambios pequeños. -->

1. **API:** `require_user` + tests. Comprobación: pytest en verde.
2. **Plantilla SAM:** parámetros, concurrencia y presupuesto. Comprobación: revisión de la plantilla y build local de la imagen con `/health` 200 y `/api/chat` 401 sin token.
3. **App, datos:** interfaz de tokens, `SupabaseSession`, transporte, DataSource y error + tests. Comprobación: `testDebugUnitTest`.
4. **App, presentación:** textos EN/ES y mapeos + test del ViewModel. Comprobación: `testDebugUnitTest` y `assembleDebug`.
5. **Documentación:** `spec.md` y pasos de despliegue. Staging en ambos repos.
6. **Pendiente de la propietaria:** `sam deploy` con los nuevos parámetros y prueba en dispositivo.

## Riesgos y decisiones pendientes

<!-- Riesgos concretos de esta solución y cómo se resolverán, sin listas genéricas.
Escribe Ninguna en las decisiones pendientes cuando estén resueltas. -->

- **Riesgos y medidas acordadas:**
  - *Cuentas AWS nuevas con límite de concurrencia de 10:* el despliegue fallaría al reservar 5. Medida acordada en la spec: el parámetro admite `0` para desactivarlo (con una condición en la plantilla), y queda documentado.
  - *Rotación de claves de Supabase:* `PyJWKClient` vuelve a descargar el JWKS cuando aparece un `kid` desconocido. No hace falta redesplegar.
  - *Primer arranque de Lambda:* la descarga del JWKS añade una petición, que luego queda en caché mientras la instancia sigue viva. Impacto bajo.
  - *Versiones antiguas de la app:* dejarán de usar Bloom (ver compatibilidad). Es aceptable y es el objetivo.
- **Decisiones pendientes:** Ninguna.
