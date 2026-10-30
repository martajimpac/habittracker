# PLAN: Chat de hábitos con bloom-api

**SPEC de referencia:** `docs/features/habit-ai-recommendations/SPEC.md`
**Versión de la spec revisada:** Aprobada el 2026-10-30 en el árbol de trabajo, al pedir cerrar el flujo con las acciones recomendadas.
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

La misma instrucción del 2026-10-30 que aprobó la spec aprueba este plan y autoriza la implementación. No se añaden dependencias.

## Contexto técnico verificado

| Componente o archivo existente | Ruta verificada | Responsabilidad y uso previsto |
| --- | --- | --- |
| Pantalla actual | `app/src/main/java/com/marta/habittracker/presentation/screens/recommendations/RecommendationScreen.kt` | Un solo uso: cifras y botón. Pasa a ser el hilo y el campo. |
| Estado actual | `.../recommendations/RecommendationViewModel.kt` | Sustituir el estado de una sugerencia por mensajes, borrador y un envío en curso. |
| Contadores | `domain/usecase/HabitWeekRecommendationStats.kt` | Reutilizar `habitWeekRecommendationStats`. No cambia el cálculo. |
| Contrato remoto | `domain/repository/RecommendationRepository.kt` | Añadir el envío del hilo. Conservar `request` para no borrar `POST /api/recommendations`. La pantalla no lo usa. |
| Cliente | `data/remote/BloomRecommendationDataSource.kt` | Añadir `POST /api/chat` sobre el mismo transporte y la misma URL base. |
| Transporte | `data/remote/KtorRecommendationTransport.kt` | Reutilizar. Timeout de llamada ya es 100 s, por encima de los 90 s del servicio. |
| Inyección | `di/BloomApiModule.kt` | Sin cambios de grafo: el repositorio sigue recibiendo el data source. |
| Navegación | `presentation/navigation/NavigationBottomWrapper.kt` | Estadísticas sigue abriendo `TabRecommendations`. |
| Actividad | `MainActivity.kt` ya llama a `enableEdgeToEdge`. El manifiesto no declara `windowSoftInputMode`. | Añadir `adjustResize` en `MainActivity`. |
| API | `C:\Users\marta\apis\bloom-api\main.py` | Solo `GET /health` y `POST /api/recommendations`. El endpoint de chat se añade ahí. |

**Convenciones y patrón de referencia:** Clean Architecture. La pantalla no llama al repositorio. El ViewModel sí. `StateFlow` para el estado. Errores como `RecommendationError` y textos en `values` y `values-es`. Fakes en los tests unitarios, sin mocks.

## Solución propuesta

`bloom-api` gana `POST /api/chat`. El cuerpo lleva `habits` (mismo significado que hoy), `language` (`en` o `es`) y `messages` (`role` + `content`) en orden. La respuesta útil es `message`, un texto. El servicio valida la lista de hábitos como ya hace recomendaciones, exige al menos un mensaje, exige que el último sea de la persona y responde en el idioma pedido usando solo esos contadores. Sigue llamando a Ollama con `stream: false`, el modelo `qwen2.5-coder:7b` y 90 segundos.

En la app, el ViewModel guarda los mensajes y el borrador solo en memoria. Cada envío recalcula los contadores, añade el mensaje de la persona, limpia el borrador y llama a `send`. Un fallo deja ese mensaje, guarda el error y `retry` repite la misma lista sin añadir otra copia. El idioma sale de la configuración de la app: `es` si el idioma empieza por `es`; si no, `en`.

La pantalla usa el `Scaffold` existente, insets de dibujo seguro y el teclado con `fitInside` del IME para que el campo quede visible. Sin hábitos solo muestra la explicación.

## Módulos y componentes afectados

El proyecto no está modularizado. No se crea un módulo.

| Módulo | Existe / nuevo | Responsabilidad y cambios | Dependencias afectadas |
| --- | --- | --- | --- |
| No aplica | — | La app sigue en `:app`. | — |

| Componente o ruta | Acción | Cambio y responsabilidad | Requisito relacionado |
| --- | --- | --- | --- |
| `domain/model/AssistantTurn.kt` | Crear | Mensaje del hilo: persona o asistente, y texto. | RF-02 |
| `domain/repository/RecommendationRepository.kt` | Modificar | `send(stats, language, messages)`. | RF-02 |
| `data/remote/BloomRecommendationDataSource.kt` | Modificar | `POST /api/chat` y lectura de `message`. | RF-02, RF-06 |
| `data/repository/RecommendationRepositoryImpl.kt` | Modificar | Delegar `send`. | RF-02 |
| `presentation/screens/recommendations/RecommendationViewModel.kt` | Modificar | Hilo, borrador, un envío, reintento. | RF-01 a RF-08 |
| `presentation/screens/recommendations/RecommendationScreen.kt` | Modificar | Lista, campo, error y reintento. Sin bloque de cifras. | RF-01, RF-03, RF-05, RF-07 |
| `res/values/strings.xml` y `values-es/strings.xml` | Modificar | Rótulos del chat. | RF-04 |
| `app/src/main/AndroidManifest.xml` | Modificar | `adjustResize` en `MainActivity`. | RF-08 |
| `C:\Users\marta\apis\bloom-api\main.py` | Modificar | Endpoint nuevo. No se elimina `/api/recommendations`. | RF-02, RF-03, RF-04 |
| Tests de data source y ViewModel | Modificar | Cubren el contrato y el hilo. | CA-02, CA-05, CA-06, CA-07, CA-09 |

## Datos y contratos

- **Modelos y contratos de entrada y salida:** `POST /api/chat` con `habits[{name, completed_days, total_days}]`, `language` y `messages[{role, content}]`. Respuesta `{ "message": "<texto>" }`. `role` es `user` o `assistant`.
- **Identificadores, relaciones y restricciones:** Sin ids nuevos. Nombre de hábito de 1 a 100 caracteres tras recortar. Contadores enteros de cero o más. Si `completed_days` supera `total_days`, el servicio responde 400. Mensaje vacío no sale de la app. El servicio rechaza una lista de mensajes vacía y un último mensaje que no sea de la persona.
- **Origen de los datos mostrados y transformaciones:** Los contadores salen de `habitWeekRecommendationStats` en el momento del envío y no se muestran. Los mensajes visibles son los de la visita más la respuesta.
- **Persistencia, consultas y actualizaciones:** El hilo y el borrador no se escriben en Room ni en `SavedState`. Viven en el ViewModel de esa visita.
- **Convivencia entre datos locales y remotos:** La lectura de hábitos sigue en Room. El envío es remoto y no modifica hábitos.
- **Compatibilidad y migraciones de datos existentes:** No hay migración. `POST /api/recommendations` permanece.

## Estado, operaciones y errores

- **Gestión del estado de interfaz y navegación:** Fases `LoadingHabits`, `Empty`, `Ready` y `Sending`. Estadísticas sigue navegando a la misma ruta. Volver atrás destruye el ViewModel de la visita.
- **Conservación y restauración del estado:** El ViewModel conserva mensajes y borrador al rotar. Matar el proceso crea un estado nuevo y vacío.
- **Ejecución, concurrencia y cancelación de operaciones:** Una corrutina por envío. `Sending` ignora otro `send` o `retry`. Salir de la pantalla cancela el ámbito del ViewModel. No se lanza una segunda llamada al volver de segundo plano.
- **Errores, reintentos y prevención de duplicados:** 400 y 422 son petición inválida; 504 y el timeout del transporte son espera agotada; cuerpo sin texto es respuesta vacía; el resto es no disponible. El mensaje de la persona ya está en la lista. `retry` reutiliza esa lista.
- **Otras consideraciones mobile aplicables y su solución:** Teclado con `adjustResize` y `fitInside` del IME. Rótulos en recursos. El estado de carga usa texto y un indicador, no solo color. El campo y el botón tienen descripción para el lector. No hay permiso nuevo ni notificación.

## Dependencias y configuración

- No hay librerías nuevas. Se reutilizan Ktor, kotlinx.serialization, Hilt, Compose y el transporte actual.
- `BLOOM_API_BASE_URL` no cambia.
- El endpoint vive en `bloom-api`, fuera de este repositorio. El código queda escrito en `main.py` y también se entrega en el informe para copiarlo.

## Estrategia de validación

| Criterio | Método y test existente o propuesto | Entorno y datos necesarios | Evidencia prevista |
| --- | --- | --- | --- |
| CA-01 | Prueba manual. El unitario comprueba que con hábitos la fase queda `Ready` y la lista vacía, sin llamada. | Cuenta con hábitos, o el fake del ViewModel. | Test de ViewModel. La pantalla en emulador queda pendiente si no hay dispositivo. |
| CA-02 | `RecommendationViewModelTest` envía dos turnos y revisa la lista enviada. | Fake del repositorio. | La segunda llamada lleva los mensajes previos y los contadores. |
| CA-03 | Revisión de `RecommendationScreen`: no queda `recommendation_stat`. El prompt de `main.py` prohíbe otras cifras. | Código. | Diff de la pantalla y el prompt. |
| CA-04 | El ViewModel pasa el idioma inyectado. Los recursos existen en `values` y `values-es`. | Tests y recursos. | Llamada con `en` o `es`. No se ejecuta el modelo en este entorno salvo que la API esté levantada. |
| CA-05 | Test de ViewModel sin hábitos. | Fake. | Fase `Empty` y cero llamadas. |
| CA-06 | Test de ViewModel con error y `retry`. Test del data source para cuerpo vacío, 400, 504 y fallo de transporte. | Fakes. | Un solo mensaje de la persona y segunda llamada con la misma lista. |
| CA-07 | Test de ViewModel con la primera llamada bloqueada. | `CompletableDeferred`. | `calls == 1`. |
| CA-08 | El estado está en el ViewModel, sin `SavedState`. | Revisión de código. | No hay clave de guardado. La rotación real queda para dispositivo. |
| CA-09 | `BloomRecommendationDataSourceTest` comprueba `http://10.0.2.2:8000/api/chat` y que la URL no contiene el puerto 11434. | Fake del transporte. | URL y cuerpo. El móvil con IP privada no se ejecuta aquí. |

**Comprobaciones de regresión:** `HabitWeekRecommendationStatsTest` y el test existente de `POST /api/recommendations`.

**Comandos verificados para compilar y ejecutar tests:** `gradlew.bat :app:testDebugUnitTest` y `gradlew.bat :app:assembleDebug`.

**Pruebas en dispositivo, emulador o simulador:** Abrir Estadísticas, entrar al chat, enviar un mensaje con la API en marcha y comprobar el teclado. Hace falta emulador o móvil, Ollama y `bloom-api`.

**Limitaciones del entorno:** Sin un dispositivo conectado no se demuestra la rotación, el teclado ni el idioma visual. Sin la API en marcha no se demuestra una respuesta real del modelo. Esas comprobaciones quedan pendientes en `TASKS.md`.

## Orden de implementación

1. Test del data source en rojo, luego `POST /api/chat` en el cliente. Comprobar el test.
2. Tests del ViewModel en rojo, luego el estado del hilo. Comprobar los tests.
3. Pantalla, cadenas y `adjustResize`. Compilar.
4. Endpoint en `main.py`.
5. `testDebugUnitTest` del paquete de recomendaciones y `assembleDebug`.

## Riesgos y decisiones pendientes

- **Riesgos y medidas acordadas:** Un hilo largo puede exceder el contexto del modelo. No se recorta: el fallo vuelve como error recuperable, que es el comportamiento aprobado. El modelo puede ignorar la instrucción de no inventar cifras; el prompt lo prohíbe y la pantalla no crea un bloque de estadísticas. `fitInside` del IME depende de Compose 2026.06.01, que es el BOM ya declarado; si no compila, se usa `imePadding` debajo de `consumeWindowInsets`, una sola vez.
- **Decisiones pendientes:** Ninguna.

<!-- ANTES DE SOLICITAR APROBACIÓN
Comprueba que el plan cubre los requisitos, respeta las exclusiones, reutiliza
componentes verificados y permite demostrar todos los criterios de aceptación.
Resuelve dudas y marcadores pendientes. Si la spec cambió, revisa su impacto.
Tras aprobar el plan, deriva TASKS.md con IDs, dependencias, referencias a RF/CA
y comprobaciones. No marques una tarea terminada sin realizar su validación;
si está bloqueada, registra el motivo.
-->
