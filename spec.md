# HabitTracker Spec

## Objetivo

La app se llama Bloom. Sirve para crear, consultar y completar habitos personales. Debe proteger la privacidad del usuario, mantener la integridad de los datos locales y comunicarse con servicios remotos usando canales seguros.

### View

- Las pantallas Compose solo deben renderizar estado y enviar eventos al ViewModel.
- Los ViewModels exponen estado observable mediante `StateFlow`.
- Eventos one-shot (navegación, diálogos, snackbars, abrir pantallas): siempre `Channel(Channel.BUFFERED)` + `receiveAsFlow()` y `ObserveAsEvents` en Compose. Prohibido flags sticky en `UiState`, `SharedFlow`/`CollectAsEffect` para eventos nuevos, y `collectAsState*` sobre flujos de eventos. Ver `.cursor/rules/android-one-shot-events.mdc` y `.cursor/skills/android-one-shot-events/`.
- Separar siempre `*Screen` (solo ViewModel + eventos / navegacion) de `*Content` (UI pura con UiState + callbacks, sin ViewModel) para Preview, tests y reutilizacion.
- El estado de UI no debe contener secretos persistentes por defecto.
- Las credenciales introducidas por el usuario no deben aparecer en logs ni en representaciones persistentes del estado.
- El login debe empezar deshabilitado hasta que email y password sean validos.
- Pantalla Login: fondo verde bosque (`colorScheme.primary`). El wordmark `logo` es transparente y va al inicio de la card. En modo claro las letras son verde oscuro, para que se lean sobre la card clara. En modo oscuro se usa `logo_dark`: el mismo wordmark en blanco, también transparente. El mismo wordmark, según el tema, va arriba del formulario de crear cuenta. Debajo sigue el formulario (email, password, acciones existentes).
- Fuera de alcance en Login: Remember me, Google/Facebook u otros social logins.
- Password reset (aprobado 2026-08-08): ver § Password reset (deep link).
- El icono de la app (launcher) es una flor de loto: cada pétalo usa un color de la paleta de hábitos y el trazo es gris claro. La marca del login es el wordmark Bloom en `res/drawable-nodpi/logo.png`, y en modo oscuro `res/drawable-nodpi/logo_dark.png`.
- Bottom navigation: ver § Material 3 chrome (NavigationBar + theme). No hay FAB de alta.
- Tab Friends (Figma Make — social):
  - UI Android implementada y conectada al flujo real, alineada con Figma: header "Friends" + botón "Add", sección retos activos (la tarjeta usa el mismo fondo que un hábito de Inicio; el color del hábito queda en el icono, la copa y las barras. Muestra quién va ganando junto a una copa; si van iguales, empate), lista de amigos (el nombre y los datos abren sus hábitos; desafiar es un botón de solo icono), solicitudes pendientes (foto y nombre de quien la envía; aceptar y rechazar son dos botones de solo icono, uno encima del otro) y bottom sheets (añadir / ver hábitos / crear reto). La hoja de añadir amigo no lleva asa ni divisor bajo el título, y cierra con un botón de solo icono.
  - Textos en ingles (`strings.xml`).
  - Android usa el contrato de dominio `FriendsRepository` y `SocialRemoteDataSource` (Supabase); el `FriendsViewModel` expone el estado de la UI y recarga los datos al volver a primer plano.
  - Todas las lecturas y mutaciones sociales requieren red (remote-only, online-first); no existe cache Room de friends/challenges en el MVP.
  - La privacidad se persiste extremo a extremo mediante `Habit.isPublic`, `HabitEntity.isPublic` y `HabitDto.is_public`; por defecto cada hábito es privado.
  - Plan de implementación: `docs/plans/2026-07-31-friends-figma-android.md`.
  - Fuera de alcance inmediato: Realtime y cache offline de amigos. El push al crear un reto está en § Challenge push + detail. El push al enviar una solicitud de amistad está en § Friend request push.

### Material 3 chrome (NavigationBar + theme)

Aprobado 2026-10-09.

**Objetivo:** que el chrome (barras y fondos de pantalla) se lea como Material 3 actual, sin rediseñar cards de contenido ni añadir FAB. Los iconos de interfaz, hábitos, onboarding y calendario usan Material Icons Rounded. El logo no cambia.

**NavigationBar**
- `NavigationBar` con cinco huecos: Home, Stats, **Nuevo**, Friends, Profile (Nuevo en el centro). Solo iconos; el texto de cada tab queda como `contentDescription`. La barra mide 64 dp de alto.
- Nuevo es una card verde (`colorScheme.primary`) con el icono Add, más grande que el resto y con elevación. No es un destino seleccionado: navega a crear hábito (`TabAddContent`). Al volver, permanece seleccionada la tab anterior (Home/Stats/Friends/Profile), no Nuevo.
- Los otros cuatro usan `NavigationBarItem` sin label, con la píldora de selección. Su icono va en contorno y, al seleccionar la pestaña, cambia a la versión rellena.
- La barra se oculta en Crear, Editar hábito, Detalle de hábito, Detalle de reto, Preferencias y Borrar cuenta.
- Sin `FloatingActionButton` / `HabitFab` para el alta. `contentDescription` de Nuevo: `tab_new` (`strings.xml`).

**Botones**
- Los botones con texto usan `HabitButton`. Los grandes (56 dp, ancho completo salvo que compartan fila) comparten forma, tipografía y, si son de acción, el verde sólido `colorScheme.primary`. Sin degradado.
- Cancelar y rechazar usan la variante Secondary: fondo `HabitField` y borde de 1 dp.
- En Crear hábito, cerrar va a la derecha del título.
- Onboarding: fondo `colorScheme.surface` y botón `HabitButton` Primary. Tallo, hojas, títulos e indicadores usan el verde del tema. Los pétalos quedan en coral `#FF7775` y el centro en amarillo `#FFB938`. Tres pantallas; el último botón marca el onboarding como completado y navega a Login. En debug, `DebugFlags.ALWAYS_SHOW_ONBOARDING` en `true` lo vuelve a mostrar en cada arranque. En release esa variable se ignora.

**Theme**
- Completar `lightColorScheme` y `darkColorScheme` con roles M3: `primaryContainer` / `onPrimaryContainer`, `secondaryContainer` / `onSecondaryContainer`, `tertiary` / `onTertiary` / `tertiaryContainer`, `errorContainer` / `onErrorContainer`, y `surfaceContainerLowest` … `surfaceContainerHighest` (más `surfaceContainer` / `surfaceContainerHigh` / `Low` según el scheme). En modo oscuro los neutros son gris, sin tinte marrón. El fondo (`surface`) es el tono más oscuro; las cards (`surfaceContainerLowest` y el resto de containers) quedan siempre más claras.
- Marca: verde bosque (`HabitPrimary` `#294536`, `HabitPrimaryLight` `#3D6E50`). Los contenedores y el terciario son verdes del mismo tono. No volver al navy `#021C4B` ni al púrpura `#6750A4`. En claro, el blanco es crema `HabitCream` `#FDF9F1`: cards, `HabitField` y el texto sobre el primario. El fondo y el resto de containers son esa crema más oscura (`#EFEBE3`, `#EAE6DE`, `#E4E0D8`, `#DEDAD2`). El modo oscuro sigue en grises, con texto blanco. El verde de acento en oscuro es `#8FCBAA`, con texto `#143028` encima, para que se distinga del negro.
- Dynamic color **apagado** (`InstaDevTheme(dynamicColor = false)`). Nunito y `shapes` sin cambio.

**Chrome (sí) vs contenido (no)**
- Sí: `Scaffold.containerColor`, `TopAppBar` colors, fondos de pantalla (Home, Stats, Friends, Profile, Add, Auth, Preferences, Delete, Detalle) → `MaterialTheme.colorScheme.surface` / `surfaceContainer` (y `onSurface` en títulos de barra). `NavigationBar` usa tokens del scheme, no `Color.White` ni `HabitSurface` sueltos. La zona del notch usa el mismo color que la parte de arriba de la pantalla: el fondo llega hasta el borde superior.
- No: cards de hábitos, heatmap, `HabitField` / botones custom, widgets Glance, chips, SearchBar, motion scheme, progreso determinado de retos (`LinearProgressIndicator`).
- Cargando (indeterminado): `HabitLoadingIndicator` → `CircularWavyProgressIndicator` con `colorScheme.primary` (Material3 `1.5.0-beta01`; el BOM 2026.06.01 aún pincha 1.4). No sustituye el anillo de % de Detalle ni las barras de retos.

**Capas / tests**
- Solo `presentation/theme` y pantallas Compose (Screen/Content). Sin dominio, Room ni tests de ViewModel nuevos.
- Previews de la barra (ítem seleccionado + Nuevo) y theme light/dark.

**Fuera de alcance**
- FAB; quinto destino “siempre seleccionado” en Nuevo; dynamic color; Compose Styles alpha; `WideNavigationRail`; rediseño Figma de contenido.

### Domain

- La logica de negocio debe vivir en casos de uso o validadores testeables.
- La validacion de credenciales debe ocurrir antes de llamar a repositorios remotos.
- Las passwords debiles deben rechazarse localmente.
- Los casos de uso no deben depender de clases Android.

### Data

- Los repositorios implementan contratos del dominio.
- Room debe garantizar integridad referencial entre habitos y registros.
- No deben quedar registros huerfanos al eliminar un habito.
- No debe existir mas de un registro de cumplimiento para el mismo habito y fecha.
- Retrofit debe usar exclusivamente HTTPS para endpoints remotos.
- Los tokens de autenticacion deben enviarse en headers seguros, no en query params ni logs.
- Habitos y registros se sincronizan con Supabase (online-first): Supabase es la fuente de verdad en escrituras; Room es cache de lectura (tambien offline).

### Password reset (deep link)

Aprobado 2026-08-08.

**Flujo**
1. Login → "Forgot password?" → pantalla **Forgot password** (email; precargar el email ya escrito en Login si existe).
2. La app solicita el reset vía Supabase Auth (`resetPasswordForEmail`) con  
   `redirectTo = habittracker://auth/reset`.
3. Tras enviar, mostrar mensaje genérico de éxito (no revelar si el email está registrado) y volver a Login (evento one-shot `SharedFlow`, no flags sticky).
4. El usuario abre el enlace del correo → custom scheme abre la app → se importa la sesión de recovery desde la URI.
5. Pantalla **Reset password**: nueva password + confirmación; validación local con `PasswordValidator`.
6. Al guardar con éxito: actualizar password (`updateUser`) → **signOut** de la sesión recovery → navegar a **Login** con mensaje de éxito.

**Técnico**
- Custom scheme: `habittracker://auth/reset` (intent-filter en `MainActivity` / deep link de Navigation).
- Configuración manual en Supabase Dashboard → Auth → URL Configuration: Site URL y Redirect URLs deben incluir `habittracker://auth/reset` y `habittracker://auth/confirm`. Si Site URL se queda en `http://localhost:3000`, el enlace de confirmar el correo abre una página vacía.
- Capas: métodos en `AuthRepository` (+ use cases si aportan); Screens/ViewModels Screen/Content; strings EN en `strings.xml`.
- Logs: no registrar tokens del deep link ni passwords; email solo truncado si hace falta.

**Fuera de alcance**
- Página web intermedia / hosting propio de reset.
- Android App Links (`https://`).
- Cambio de password desde Profile estando ya logueado.
- iOS.

## Requisitos Funcionales Basicos

- El usuario puede iniciar sesion con email y password validos.
- El usuario puede solicitar un reset de password por email y definir una nueva password en la app tras abrir el deep link del correo.
- El usuario puede visualizar habitos por fecha.
- El usuario puede marcar y desmarcar un habito como completado.
- El usuario puede consultar el detalle e historial de un habito.
- En Stats, pulsar un hábito abre su detalle (`TabDetail`).
- En el detalle no hay pestañas. La ficha del hábito (icono, nombre, descripción, días programados, recordatorio y racha viva) va en una tarjeta. Debajo, otra tarjeta agrupa los gráficos: el anillo de «Desempeño general», las barras de las últimas 4 semanas y las barras por día de la semana. En las últimas 4 semanas, cada barra es el porcentaje de días programados completados esa semana; no entran los días futuros ni los anteriores a la creación del hábito. Las barras por día de la semana cuentan cuántas veces se completó el hábito cada día, de lunes a domingo; no cuentan los días futuros. El calendario mensual sigue en su propia tarjeta, con el mismo borde. Editar, eliminar y atrás son `HabitIconButton`: cuadrados de 40 dp. Por defecto usa el mismo color primario, el texto `onPrimary` y las esquinas de 16 dp que `HabitButton`. Eliminar usa la paleta `HabitDanger`: fondo del contenedor, icono y borde de 1 dp, los mismos que cerrar sesión y borrar cuenta. El nombre y el icono del hábito van dentro de la ficha. Editar abre el formulario de crear hábito ya relleno (`TabEditHabit`) y guarda con `updateHabit`. Eliminar pide confirmación, llama a `deleteHabit` y vuelve atrás.
- El usuario puede eliminar habitos y sus registros asociados.
- El usuario puede crear un habito con nombre, descripcion opcional, icono, color (12 opciones en 2 filas de 6), recordatorio local opcional (apagado por defecto) y dias de la semana. Ver § Habit reminders. El selector de icono incluye dos flores sólidas, de un solo color y tintables: bloom y loto (`flower_bloom`, `flower_lotus`). Tulipán y la flor de cinco pétalos siguen pintándose si un hábito ya las tenía. También hay pesa, no fumar, no móvil, teléfono, mensaje, portátil, estudio, pasos, dólar, árbol, maletín, sol, manzana, limpieza y carrito. Se pintan con `HabitLineIcon` y la clave guardada, igual que el resto de iconos.
- Si faltan el nombre, los días o el formato del recordatorio, el botón de crear sigue pulsable con aspecto deshabilitado. Al pulsarlo, el foco va al primer campo incompleto y ese campo muestra el error.
- Home, Stats y Detail muestran el icono y color persistidos del habito.
- Lectura: los habitos se muestran desde Room (cache); sin red el usuario puede ver datos ya sincronizados.
- Escritura (crear, completar, editar, borrar): requiere conexion a internet. Sin red, la operacion no modifica Room ni Supabase y la UI muestra un error de "sin internet" (`strings.xml`).
- Con red: la escritura va primero a Supabase; solo si el remoto OK se actualiza Room. Si falla el remoto: log (`Log.e`), error en UI, Room no cambia.
- Los IDs generados en el cliente (hábitos, habit_records, friendships, challenges y cualquier otro PK/FK creado en app) son **UUIDv7** (`String`), compartidos entre Room y Supabase (sin `remoteId` separado).
- Generación centralizada vía helper (`newId()` / equivalente) usando `kotlin.uuid.Uuid.generateV7()` (opt-in `ExperimentalUuidApi`); no usar `UUID.randomUUID()` (v4) para IDs nuevos.
- IDs ya persistidos (p. ej. v4 históricos) **no se migran**; solo los IDs creados a partir de este cambio usan v7.
- Schema Postgres `uuid` sin cambios; v7 es un UUID de 128 bits válido.
- Detalle de diseno: `docs/designs/2026-07-21-add-habit-figma-design.md`.

### Sync de habitos (Supabase)

- Schema remoto: tablas `habits` y `habit_records` en `public`, con RLS (dueño + excepciones sociales abajo).
- `habits`: `id` (uuid), `user_id`, `name`, `description`, `days_of_week` (smallint[] 1=Mon..7=Sun), `icon`, `color_hex`, `reminder_time`, `reminder_enabled` (boolean not null default false), `is_public` (boolean not null default false), `created_at`, `updated_at`, `deleted_at` (soft delete).
- `habit_records`: `id` (uuid), `habit_id`, `user_id`, `date`, `is_completed`, `updated_at`, `deleted_at`; UNIQUE(`habit_id`, `date`); FK a `habits` con CASCADE.
- Estrategia: **online-first**. Mutaciones requieren red. Orden: check connectivity → Supabase → Room cache.
- Pull al login / arranque autenticado: descarga filas del usuario y reemplaza/actualiza la cache Room (remoto gana; no hay cola de cambios offline pendientes).
- Room almacena los mismos campos de sync (`updatedAt` / `deletedAt`) para alinear con el remoto.
- Fuera de alcance: outbox, dual-write offline, WorkManager periodico, UI rica de sync, resolucion manual de conflictos.

### Habit reminders (local notifications)

Aprobado 2026-10-09.

**Producto**
- Recordatorio **local** (no FCM) a la `reminder_time` del hábito, solo en los `days_of_week` del hábito, y solo si `reminder_enabled` es true.
- Fuente de verdad: `habits.reminder_time` (`HH:mm` o null) y `habits.reminder_enabled`. La hora se guarda aunque el interruptor esté apagado. La alarma solo existe si el interruptor está encendido y hay una hora válida. `reminder_time` null significa que el hábito no tiene hora.
- Los hábitos ya guardados con `reminder_time` no vacío quedan con `reminder_enabled = true`.
- **Crear hábito:** interruptor Recordatorio **apagado por defecto** y sin hora. La fila de hora se ve siempre. Elegir una hora la guarda aunque el recordatorio siga apagado. Al encender el recordatorio sin hora, la hora inicial es `08:00`.
- **Editar hábito:** el mismo interruptor + hora. Apagar el recordatorio no borra la hora. Al **borrar** el hábito se cancelan sus alarmas.
- Home y Estadísticas ordenan primero los hábitos con recordatorio, por hora. Los que no tienen recordatorio van al final; entre ellos también por hora, y sin hora los últimos. El widget de hoy usa ese mismo orden. Si un hábito es el del usuario en un reto pendiente o activo, Inicio, Estadísticas y el detalle lo marcan con una insignia de espadas blancas sobre el color del hábito, en la esquina superior izquierda del icono.
- Al pulsar la notificación: abre el **detalle** de ese hábito. Sin acción “Completar” en la notificación.
- Si ese día el hábito **ya está completado** (record en Room): no se muestra la notificación; sí se programa la siguiente ocurrencia.
- La fila Notifications del perfil está oculta de momento. Preferencias no gestiona recordatorios de hábitos.

**Técnico**
- Dominio libre de APIs Android. Puerto `HabitReminderScheduler`: `schedule(habit)`, `cancel(habitId)`, `rescheduleAll()`. Implementación con `AlarmManager.setExactAndAllowWhileIdle`.
- Una alarma por hábito, hacia la **próxima** fecha/hora válida. Tras disparar (haga o no notify) se programa la siguiente.
- Programar/cancelar solo **después** de un save/delete OK (online-first: si falla la red, no queda alarma de un hábito no persistido).
- `BOOT_COMPLETED` y cambio de zona horaria / hora del sistema → `rescheduleAll()` desde Room (hábitos no soft-deleted con recordatorio encendido y `reminder_time` no vacío).
- Permisos: `POST_NOTIFICATIONS` (API 33+) y `SCHEDULE_EXACT_ALARM` (API 31+). Si el usuario deniega, error en UI (`strings.xml`), interruptor off, no se persiste hora. Sin crash si más tarde quita el permiso en Ajustes.
- Canal de notificación **distinto** del de FCM. `notificationId` estable por `habitId` (varios hábitos a la misma hora no se pisan; no usar `notify(0)` compartido).
- Título = nombre del hábito; cuerpo desde `strings.xml` (EN + ES).
- Logs: `Log.d`/`Log.e` en scheduler/receiver; no PII innecesaria.

**Tests**
- Cálculo de próxima ocurrencia (hora ya pasada hoy → siguiente día de la frecuencia).
- Skip notify si ya completado hoy.
- ViewModel: recordatorio apagado conserva la hora; encendido persiste `HH:mm` y `reminder_enabled`; permiso denegado → error e interruptor off.
- Scheduler con fake del puerto: schedule / cancel / rescheduleAll.
- No se exigen tests instrumentados de que el OS dispare la alarma.

**Fuera de alcance**
- FCM / push de servidor para recordatorios de hábitos. `MyFirebaseMessagingService` queda para el push de retos (§ Challenge push + detail), no para estos recordatorios locales.
- WorkManager periódico como reloj de hábitos.
- Completar el hábito desde la notificación.
- Pantalla global de notificaciones en Profile.
- iOS.

### Social (Supabase) — Friends + challenges

Aprobado 2026-07-31. Schema en `public` con RLS; GRANT a `authenticated`; tablas nuevas expuestas al Data API.

**Amistad:** solicitud → aceptar/rechazar (no follow unidireccional ni add instantáneo).

**Hábitos visibles a amigos:** privacidad **por hábito** (`habits.is_public`). Amigos `accepted` pueden `SELECT` hábitos públicos (y no soft-deleted) del otro.

**Retos:** mismo hábito semántico (mismo nombre/tema); cada participante tiene su propia fila en `habits`. Progreso y `days_left` se **calculan** desde `habit_records` entre `starts_at` y `ends_at` (no columnas de progreso denormalizadas).

**Tablas:**

1. `profiles`
   - `id` uuid PK FK `auth.users` ON DELETE CASCADE
   - `username` text unique not null (búsqueda Add friend)
   - `display_name` text not null
   - `avatar_color` text not null default `#294536` (`HabitPrimary`)
   - `avatar_url` text nullable (path o URL pública del avatar en Storage; null = iniciales)
   - `created_at` / `updated_at` timestamptz
   - Trigger: al crear usuario en `auth.users`, insertar profile (username provisional derivado del email / id)

2. `friendships`
   - `id` uuid PK
   - `requester_id`, `addressee_id` uuid FK `profiles`
   - `status` text check: `pending` | `accepted` | `rejected`
   - `created_at` / `updated_at`
   - CHECK `requester_id <> addressee_id`
   - UNIQUE canónico del par (p.ej. `least(requester_id, addressee_id)`, `greatest(...)`)
   - RLS: requester/addressee ven sus filas; requester crea `pending`; addressee actualiza status; no deletes arbitrarios de terceros

3. `challenges`
   - `id` uuid PK
   - `challenger_id`, `challenged_id` uuid FK `profiles` (deben ser amigos `accepted`)
   - `challenger_habit_id` uuid FK `habits` not null; `challenged_habit_id` uuid FK `habits` nullable hasta que la persona retada acepta
   - `criteria` text check: `streak` | `all_days` | `completion_pct`
   - `starts_at`, `ends_at` timestamptz not null (`ends_at` > `starts_at`)
   - `status` text check: `pending` | `active` | `completed` | `declined` | `cancelled`
   - `created_at` / `updated_at`
   - RLS: solo participantes leen/actualizan su reto; challenger inserta

4. Lectura de `habit_records` de otro usuario: permitida a amigos solo para hábitos `is_public` del amigo, y/o para el hábito del otro en un challenge `active`/`pending` donde el lector es participante (ventana del reto). Escrituras de records siguen siendo solo del dueño.

**Fuera de alcance:** Realtime obligatorio, chat, Room cache de friends. El aviso push al crear un reto y la pantalla para aceptarlo o rechazarlo están en § Challenge push + detail. El aviso push al enviar una solicitud de amistad está en § Friend request push.

### Challenge push + detail

Aprobado 2026-10-10.

**Producto**
- Al crear un reto (`INSERT` en `challenges`, `status = pending`), la persona retada recibe una notificación del sistema aunque la app esté cerrada.
- El texto se resuelve en el dispositivo con `strings.xml` (EN + ES): título fijo y cuerpo con el `display_name` de quien reta.
- Pulsar la notificación abre el detalle de ese reto (`challengeId`).
- La tarjeta de retos en Amigos abre la misma pantalla.
- El detalle es online-first y usa las mismas tarjetas con borde que el detalle de un hábito. Arriba van el hábito y el criterio con la forma «Criterio: nombre», el estado y los días restantes. La única gráfica es una matriz de semanas, desde el lunes de la semana de inicio hasta el domingo de la semana de fin. Cada persona tiene una fila; un día cumplido va relleno. Los días fuera del reto quedan vacíos. El primer día lleva un borde y el último una marca abajo.
- Crear un reto no reutiliza hábitos ya existentes. Quien reta define uno nuevo (nombre, icono, color y días). Si pulsa enviar con el botón deshabilitado, el nombre vacío y los días sin elegir se marcan. Junto a Criterio hay un icono de interrogación que explica los tres criterios. Ese hábito, público y con el mismo horario, se crea en su cuenta al enviar. La otra persona recibe el suyo solo al aceptar; si rechaza, no se le crea y el de quien retó se queda. Un reto ya creado con dos hábitos sigue igual.
- Quien fue retado, con el reto en `pending`: **Aceptar** crea su copia del hábito, pasa a `active` y se queda en la pantalla, ya sin botones; **Rechazar** pasa a `declined` y vuelve a Amigos. La lista recarga en `ON_RESUME` y deja de mostrar `declined`.
- Quien retó, o un reto que no está `pending`: solo lectura. No se puede cancelar el reto.
- Sin red, reto inexistente o si quien abre no es participante: error en UI y el estado no cambia. Un rechazo que falla no navega atrás.
- Sin sesión al abrir la notificación: se muestra Login y el `challengeId` no se reabre después de entrar.
- Quien retó no recibe un push cuando el otro acepta o rechaza.
- La barra inferior se oculta en esta pantalla, igual que en el detalle de hábito.
- No hay un permiso de notificaciones nuevo. Si `POST_NOTIFICATIONS` está denegado, el reto se crea igual y no hay aviso en la bandeja.

**Técnico**
- Tabla `device_tokens`: `token` text PK, `user_id` uuid FK `profiles` ON DELETE CASCADE, `updated_at` timestamptz. RLS: `authenticated` solo puede `DELETE` de sus filas (`user_id = auth.uid()`). No hay `SELECT`/`INSERT`/`UPDATE` para el cliente.
- El guardado lo hace la Edge Function `register-device-token`, con el JWT del usuario y escritura con service role. Así un token que quedó de otra sesión en el mismo dispositivo se reasigna: un `UPDATE` con RLS no podría tocar la fila del usuario anterior.
- Al cerrar sesión se borra antes el token de esta instalación, con la sesión todavía válida. Si el borrado falla, se registra el error y el sign-out sigue; el próximo `register-device-token` reasigna el token.
- Se registra el token al haber sesión (arranque autenticado, login y registro) y cuando Firebase lo renueva. El token no se escribe en logs.
- Un webhook de `INSERT` en `public.challenges` llama a la Edge Function `notify-challenge`. El gateway no exige un JWT de usuario en esa función (`verify_jwt = false` en `supabase/config.toml`); `register-device-token` sí lo exige. La función exige el header `x-webhook-secret` igual al secreto `CHALLENGE_WEBHOOK_SECRET`. Con service role lee los tokens del `challenged_id` y el `display_name` del retador, y envía un data message FCM HTTP v1 de prioridad alta: `type=challenge_created`, `challengeId`, `challengerName`. La cuenta de servicio de Firebase es el secreto `FIREBASE_SERVICE_ACCOUNT`. Si no hay tokens, el reto queda creado y no hay push. Un fallo de Firebase no deshace el insert.
- `MyFirebaseMessagingService` pinta la notificación en el canal existente `habit_tracker_notifications`. El `notificationId` es el hash de `"challenge:" + challengeId` y nunca es 0. El `PendingIntent` abre `MainActivity` con el extra de `challengeId`, en el mismo estilo que `WidgetLaunchExtras` para hábitos.
- Ruta `TabChallengeDetail(challengeId)`. `FriendsRepository.getChallenge(id)` y `respondToChallenge(id, accept)`.
- El dominio no depende de Android ni de textos de UI. Los errores visibles usan `@StringRes`.
- RLS de `challenges` sigue permitiendo que cualquier participante actualice la fila. Qué botones se muestran lo decide la app.

**Tests**
- ViewModel del detalle: retado + `pending` puede aceptar y rechazar; aceptar deja `active` y no navega; rechazar con éxito emite volver atrás; aceptar o rechazar con error (incluida la red) no navega y muestra el error; el retador no tiene acciones.
- Repositorio: sin red, `getChallenge` y `respondToChallenge` devuelven `AppError.Common.Network` y no escriben en remoto.
- El id de notificación de un reto no es 0 y se calcula con el prefijo `challenge:`, distinto del id de recordatorio de un hábito.
- No se exigen tests instrumentados del webhook ni de que FCM entregue el mensaje.

**Fuera de alcance**
- Push al retador cuando aceptan o rechazan.
- Cancelar un reto.
- Endurecer la RLS para impedir transiciones de estado que la app no ofrece.
- Realtime, chat, cache Room de amigos.
- Pantalla global de notificaciones en Profile.
- FCM para recordatorios de hábitos.
- iOS.

### Friend request push

Aprobado 2026-10-16.

**Producto**
- Al enviar una solicitud (`INSERT` en `friendships` con `status = pending`), quien la recibe ve una notificación del sistema aunque la app esté cerrada.
- El texto se resuelve en el dispositivo con `strings.xml` (EN + ES): título fijo y cuerpo con el `display_name` de quien invita. Si el nombre llega vacío, se usa el fallback localizado.
- Pulsar la notificación abre la pestaña Amigos, donde ya está la solicitud pendiente.
- Sin sesión al abrir la notificación: se muestra Login y Amigos no se abre después de entrar.
- Un hábito o un reto pendientes de abrir ganan a esta notificación.
- Aceptar o rechazar la solicitud no envía un push de vuelta.
- No hay un permiso de notificaciones nuevo. Si `POST_NOTIFICATIONS` está denegado, la solicitud se crea igual y no hay aviso en la bandeja.

**Técnico**
- Reutiliza `device_tokens` y el mismo secreto de webhook que los retos (`CHALLENGE_WEBHOOK_SECRET` y el secreto de Vault `challenge_webhook_secret`). No hay un secreto nuevo.
- Un trigger `AFTER INSERT` en `public.friendships` llama a la Edge Function `notify-friend-request` (`verify_jwt = false`). La función exige `x-webhook-secret`. Con service role lee los tokens del `addressee_id` y el `display_name` del `requester_id`, y envía un data message FCM HTTP v1 de prioridad alta: `type=friend_request`, `friendshipId`, `requesterName`. La cuenta de servicio sigue siendo `FIREBASE_SERVICE_ACCOUNT`. Si no hay tokens, la solicitud queda creada y no hay push. Un fallo de Firebase no deshace el insert.
- `MyFirebaseMessagingService` pinta la notificación en el canal existente `habit_tracker_notifications`. El `notificationId` es el hash de `"friend:" + friendshipId` y nunca es 0. El `PendingIntent` abre `MainActivity` en la pestaña Amigos.
- El dominio no depende de Android ni de textos de UI.

**Tests**
- El id de notificación de una solicitud no es 0 y es distinto del id de un reto con el mismo identificador.
- El intent de la notificación lleva la pestaña Amigos y el id de la solicitud.
- No se exigen tests instrumentados del webhook ni de que FCM entregue el mensaje.

**Fuera de alcance**
- Push a quien envió la solicitud cuando la aceptan o la rechazan.
- Pantalla de detalle de una solicitud.
- Realtime.

### Profile (tab)

- La tab Profile muestra UI alineada con el diseno Figma Make (header con gradiente, avatar, stats, menu rows, Sign Out).
- Hasta que el perfil y los hábitos han llegado, la tab no muestra nada: ni nombre de reserva, ni ceros, ni menú. El fondo es el de la superficie. El primer estado real marca `isReady`.
- Header usa datos reales: `display_name` y `avatar_url` desde `profiles` (no Auth `user_metadata`); email via `AuthRepository`. Si `avatar_url` es null, avatar = iniciales derivadas del display name. Ese círculo usa siempre `HabitPrimaryLight` (`#3D6E50`) en perfil, inicio, preferencias y amigos, e ignora el `avatar_color` guardado. El color guardado solo queda detrás de una foto. En inicio y en el perfil ese círculo lleva un reborde crema `#FDF9F1`. El fondo del header sigue siendo el verde primario, con el estampado `flowers.png` en un verde un poco más claro para que las flores se distingan. Bajo el nombre visible, el perfil muestra `profiles.username` (el texto con el que un amigo te busca) y una frase que lo explica.
- Stats del header (reales, desde `HabitRepository.getAllHabitsWithRecords()`):
  - **Day Streak:** máximo de `calculateScheduledStreak` entre hábitos, igual que Home: días programados cumplidos seguidos. Un día fuera del horario no suma ni rompe la racha.
  - **Completed:** total de records con `isCompleted`.
  - **Habits:** numero de habitos.
- Filas de menu: **Notifications** queda oculto de momento (no hay pantalla detrás). **Goals** queda oculto de momento (la pantalla sigue en el proyecto, pero no hay fila en el perfil). **Preferences** navega a la pantalla Preferencias. **Modo oscuro** es un interruptor en el perfil: apagado fuerza tema claro, encendido fuerza tema oscuro, y se guarda en DataStore. Si aún no hay elección, sigue el tema del sistema. El tema elegido aplica a toda la app, incluidos login y el selector de widgets. Los textos sobre fondos del tema usan `colorScheme.onSurface` y `onSurfaceVariant`, no el negro fijo de `HabitOnSurface`.
- **Goals:** meta fija de 5 días en la semana lunes–domingo que contiene hoy. Un día cuenta si hay al menos un hábito programado y todos los programados ese día tienen un record `isCompleted`. Un día sin hábitos no cuenta. Los datos salen de `HabitRepository.getAllHabitsWithRecords()` (Room). No hay tabla nueva ni meta editable. La barra inferior se oculta, igual que en Preferencias. La pantalla muestra el progreso y los días en tarjetas redondeadas, como las filas del perfil.
- Preferencias agrupa foto y nombre en tarjetas. Cerrar sesión y borrar cuenta usan la paleta `HabitDanger` (coral `#FF7775`, no el rol `error`). El botón es ese coral con texto `#5C2422`. En claro, el texto de esas filas usa `onSurface`, el mismo que el resto de textos. El icono y el borde usan el coral más oscuro `#D94E4C` sobre el fondo `#FFE8E6`. En modo oscuro el icono de esas acciones es blanco. El botón final de borrar usa ese coral, no el primario verde.
- Sign Out: pide confirmación. Si se confirma, llama a `AuthRepository.signOut()` (Supabase) y navega a Login limpiando el back stack.
- Todos los textos de Profile van en ingles (`strings.xml`).
- `ProfileViewModel` expone nombre, email, avatar, stats y el evento de sign-out; Preferences maneja sus propios clicks.

### Preferences (desde Profile)

Aprobado 2026-10-08.

- Pantalla `PreferencesScreen` / `PreferencesContent` (type-safe nav desde la fila Preferences).
- **Display name:** editar `profiles.display_name`. Trim, no vacio, max 40 caracteres. Guardar online-first; sin red: error UI, no se escribe. Al exito: volver o refrescar; Profile y Home leen el mismo `profiles.display_name`. Encima, Preferencias muestra `profiles.username` en solo lectura, dentro de un recuadro con un icono para copiarlo. Copiar pone ese nombre en el portapapeles y muestra un aviso. Es el mismo nombre con el que se busca un amigo.
- **Avatar:** Photo Picker de Android (solo galeria, sin camara). Cambiar foto es una insignia redonda con icono de editar, encima de la foto, en la esquina superior derecha. Subir a Supabase Storage bucket `avatars` path `{userId}/avatar`; persistir URL/path en `profiles.avatar_url`. Quitar foto sigue siendo un enlace de texto: pone `avatar_url` null y borra el objeto. Preview circular sin editor de recorte. Online-first.
- **Delete account:** boton separado. Navega a pantalla de confirmacion: el usuario debe escribir exactamente `DELETE` (token fijo, no se traduce); el boton de borrar se habilita solo entonces. Edge Function (service role) elimina `auth.users` (CASCADE a `profiles` y FKs) y el objeto de Storage. Exito: Login con back stack vacio. Online-first.
- Eventos one-shot: `Channel` + `ObserveAsEvents`.
- Fuera de alcance: editar username, camara, notificaciones globales de Profile, cambio de password, widgets, enviar el Name del registro a Auth metadata. Recordatorios por hábito: § Habit reminders.

### Activity heatmap

- Componente reutilizable `ActivityHeatmap` (grid 4 semanas × 7 dias, lun–dom).
- Ventana: ultimas 4 semanas rolling alineadas a lunes (W4 = semana actual).
- Colores invertidos vs mock oscuro: fondo claro; celda blanca = Less / no completado; morado oscuro = More / completado (escala intermedia en Stats).
- **Detalle:** calendario mensual (lunes a domingo) en la misma pantalla que el anillo de porcentaje. Un día se marca si ese hábito tiene un record `isCompleted`. Flechas para el mes anterior y el siguiente. Los días de los meses vecinos se ven atenuados.
- **Stats (tab global):** un heatmap agregado; intensidad = % de habitos programados ese dia que estan completados. Encima, una sola tarjeta de racha. Si un hábito lleva una racha viva más larga que la de cumplir todos los hábitos programados, y llega a `StatsStreakMinimums.SINGLE_HABIT_DAYS`, la tarjeta nombra ese hábito (el nombre en negrita) con una frase rotativa. Si la racha de cumplir todos los programados llega a `StatsStreakMinimums.ALL_HABITS_DAYS` y ningún hábito va por delante, la tarjeta lo dice con un icono de fuego y una de varias frases; si además es la racha perfecta más larga del historial reciente, alguna frase lo señala. Un día sin hábitos programados no rompe esa racha. Por debajo de ambos mínimos no hay tarjeta. Los dos mínimos viven en `StatsStreakMinimums` y hoy valen 3. La racha de un hábito cuenta los días programados cumplidos seguidos. Un día que no está en el horario no suma ni la rompe. Un día programado sin completar la rompe, también si es hoy. En cada hábito de la lista, el icono, el nombre, la racha (desde 1 día) y el horario van agrupados a la izquierda, y a la derecha va una matriz de 4 semanas más pequeña, con celdas fijas, e intensidad binaria de ese hábito (color del hábito). Si no hay hábitos, el vacío sigue el de Amigos: icono, título y una frase.
- Home no muestra la tarjeta «Día perfecto» cuando el día está completado al 100 %. La tarjeta de hábitos completados muestra una flor con tallo que crece con el porcentaje del día: primero el tallo, luego las hojas y al final la flor (tallo verde, pétalos `#FF7775`, centro `#FFB938`). El porcentaje va al lado del conteo. La lista del día ordena primero los hábitos con recordatorio, por hora; los que no tienen recordatorio van al final. El día marcado es la fecha local: al pasar la medianoche, si seguías en hoy, pasa al día nuevo. En la fila de la semana, el número de hoy y la letra de ese día van en negrita, y la fecha de hoy del encabezado también. Tampoco muestra la tarjeta de resumen del día en hoy ni en los días próximos. En un día pasado incompleto sigue el registro con el conteo. El saludo es «buenos días» hasta las 14:00, «buenas tardes» desde las 14:00 hasta las 21:00 y «buenas noches» a partir de las 21:00. Si el día no tiene hábitos programados, encima del texto hay una flor marchita.
- Leyenda Less → More. El día de hoy lleva solo el contorno en negro; el relleno sigue la intensidad de ese día. Su fila de semana y su columna de día (por ejemplo S4 y L) también van en negro. En modo oscuro esas marcas son blancas.
- El detalle muestra la ficha del hábito en una tarjeta, los gráficos (anillo de %, barras de las últimas 4 semanas y barras por día de la semana) en otra, y el calendario mensual en una tercera, sin pestañas ni heatmap de 4 semanas. La racha viva del hábito, si es de al menos 1 día, va en la ficha y usa la misma cuenta de días programados cumplidos seguidos.

### Home screen widgets (Jetpack Glance)

Aprobado 2026-08-07. Cuatro widgets independientes (Approach A).

**Común**
- Implementación con **Jetpack Glance** + App Widget.
- Textos en ingles (`strings.xml`).
- Colores alineados al theme Habit: `HabitPrimary`, `HabitSurface`, `HabitOnSurface`, `HabitField` y el color del hábito. Las previews del selector usan los mismos tokens en `colors.xml`.
- El aspecto sigue la app: cabecera verde, página crema y piezas redondeadas. El de hoy es un inicio reducido (día en negrita, fecha, conteo y filas con icono y tic). El de un hábito es una ficha de inicio. El semanal usa pastillas de día, con la de hoy en verde. El de reto muestra el nombre en la cabecera, quién va ganando junto a una copa y barras de progreso. Si el progreso está empatado, dice empate.
- Actualización: tras completar un hábito, al volver a primer plano la app, al cerrar sesión, tras sincronizar los hábitos de la cuenta que entra, y refresh periódico razonable (30–60 min). Al cerrar sesión se borra la caché local de hábitos. Sin sesión, o si el reto guardado no pertenece a la cuenta actual, el widget de reto no pinta el snapshot anterior.
- Al abrir o actualizar, el widget pinta enseguida el logo de Bloom y el texto de carga. Los datos se leen después. Así no aparece el error del sistema mientras Room o la red aún no han respondido.
- Configuración: **activity de configuración al añadir** el widget (picker clásico), no ajustes dentro de Profile.
- Mutaciones desde widget siguen **online-first** (igual que la app): sin red no se escribe Room ni Supabase.
- **Previews (2026-10-08):** cada widget declara `android:previewLayout` (thumbnail del selector de pantalla de inicio) y un `@Preview` de Glance (`androidx.glance.preview`, source set `debug`) en Android Studio. Datos de muestra; no cambian la lógica. El widget de hoy usa el mismo tratamiento.

**1. Habit widget (configurable)**
- Al añadirlo: picker de un hábito propio.
- Muestra: nombre, icono/color, estado de hoy (y streak si cabe).
- Acción: completar / descompletar el hábito de hoy vía `HabitRepository` (online-first).
- Tap en el cuerpo (fuera del toggle): abre la app (Home o detalle del hábito).
- Lectura de estado: Room (cache).

**2. Challenge widget (configurable, solo lectura)**
- Al añadirlo: picker de un reto (`pending` / `active`) del usuario.
- Persistencia local del `challengeId` + **snapshot** (nombres, progresos, días restantes, colores) para pintar sin red (los retos no tienen cache Room en el MVP social).
- Refresh con red actualiza el snapshot; sin red muestra el último snapshot.
- Sin toggle de completar en el widget.
- Tap: abre la app (tab Friends).

**3. Weekly summary widget (sin picker de hábito)**
- Resumen Lun–Dom de todos los hábitos: por día, % o conteo de hábitos programados completados (misma idea que Stats agregado).
- Solo lectura; tap abre Stats o Home.
- Datos desde Room.

**4. Today widget (sin picker)**
- Lista los hábitos programados hoy, en el mismo orden que Home. Los hechos se marcan. La lista hace scroll y muestra todos, no solo los primeros.
- Si no hay hábitos hoy, el texto es el estado vacío.
- El círculo de cada fila completa o descompleta ese hábito de hoy, online-first, igual que el widget de un hábito. Sin red no cambia.
- Tap en la fila, fuera del círculo: abre el detalle del primer hábito de esa lista que aún no está completado hoy. Si todos están hechos, o no hay ninguno, abre Home.
- Datos desde Room.

**Fuera de alcance (widgets)**
- Completar desde el widget de reto o del semanal.
- Configurar widgets desde pantallas Profile/Detail.
- Offline mutations / outbox desde el widget.
- Un único widget multipágina.

## Requisitos de Seguridad

### Credenciales

- No se aceptan passwords vacias, triviales o demasiado debiles.
- Una password valida debe tener longitud suficiente y combinar al menos varios tipos de caracteres.
- En el registro, la contraseña es válida con al menos 8 caracteres, una letra y un número. El campo no se marca en rojo mientras está vacío. En cuanto se escribe y no cumple la regla, muestra el mensaje de contraseña débil. El botón de crear cuenta usa la misma regla. Si se pulsa mientras sigue deshabilitado, marca cada dato que falta (nombre, nombre de usuario, correo, contraseña y condiciones) y lleva el foco al primero. El nombre de usuario se comprueba al escribirlo: si ya existe, el campo lo dice y propone alternativas libres. El elegido se guarda en `profiles.username`. El recuadro de las condiciones es una flor: contorno si no está aceptada y rellena si sí. Si crear la cuenta exige confirmar el correo, la app vuelve al login y muestra un diálogo positivo: cuenta creada, revisa el correo. El enlace del correo abre la app en `habittracker://auth/confirm`, no una web. Si la sesión queda importada, entra en inicio; si no, el login avisa de que el correo ya está confirmado. Si se inicia sesión sin haber confirmado, un diálogo negativo avisa de que la cuenta no está confirmada.
- Los diálogos de la app usan `HabitDialog`. Si el aviso es positivo, arriba hay una flor florecida; si es negativo, una flor caída. Cerrar sesión, eliminar un hábito y la cuenta sin confirmar son negativos. La cuenta creada es positivo. El selector de hora usa la misma tarjeta, sin flor.
- Las credenciales no deben estar precargadas en `LoginUiState`.
- `LoginUiState.toString()` no debe exponer email ni password.
- La app no debe registrar passwords, tokens ni secretos mediante `Log`.
- Los cierres inesperados se informan con Firebase Crashlytics. El informe no debe incluir passwords, tokens ni credenciales.

### Datos Locales

- La base de datos local debe mantener consistencia entre `habits` y `habit_records`.
- La eliminacion de un habito debe eliminar sus registros asociados.
- La insercion repetida de un registro para el mismo habito y fecha debe actualizar el registro existente, no duplicarlo.
- Los datos sensibles no deben almacenarse en recursos, propiedades o constantes hardcodeadas.

### Red y API

- Toda comunicacion remota debe usar HTTPS.
- Los tokens deben viajar en el header `Authorization`.
- Los tokens no deben viajar en URL, query params o bodies innecesarios.
- Los interceptores no deben escribir tokens ni credenciales en logs.
- Las politicas RLS de Supabase deben impedir lecturas/escrituras indebidas: por defecto solo el dueño (`user_id = auth.uid()`). Excepciones documentadas en Social: amigos pueden leer hábitos `is_public` y records asociados; participantes de un challenge pueden leer el hábito/records del otro necesarios para el progreso.

### Secretos

- El proyecto debe escanearse en tests para detectar posibles claves de Firebase, API keys, tokens, passwords o private keys hardcodeadas.
- Los secretos reales deben vivir fuera del repositorio, por ejemplo en variables de entorno, configuracion segura o secretos del CI.

## Testing Minimo

- Unit tests de dominio para validacion de credenciales.
- Unit tests de estado UI para evitar exposicion de credenciales.
- Unit tests de red para verificar HTTPS y manejo seguro de tokens.
- Unit tests o scripts de seguridad para deteccion de secretos hardcodeados.
- Instrumented tests de Room para validar integridad referencial y unicidad de registros.
- Unit tests del comportamiento online-first: sin red las mutaciones fallan sin tocar Room; con remoto OK se actualiza cache.
- Chrome M3 (2026-10-09): Previews de `NavigationBar` y schemes light/dark; no exige tests de ViewModel nuevos.

## CI/CD

- Todo cambio debe pasar tests unitarios.
- Los tests instrumentados deben ejecutarse antes de mergear cambios que afecten Room, DAO o UI critica.
- No se debe hacer push directo a `main`.
- Los commits deben seguir Conventional Commits.
- Los despliegues deben realizarse mediante el flujo automatizado definido por el proyecto.

## Deuda Tecnica Conocida

- `LoginUiState` no debe tener email ni password por defecto.
- La politica de passwords debe moverse a un validador de dominio testeable.
- `LoginUseCase` debe rechazar credenciales debiles antes de llamar al repositorio.
- Hilt debe tener un binding explicito entre `domain.repository.HabitRepository` y `HabitRepositoryImpl`.
- Los ViewModels deben depender del contrato de dominio, no de una implementacion de data.
- La version de KSP debe alinearse con la version del plugin de Kotlin.
