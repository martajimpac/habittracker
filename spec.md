# HabitTracker Spec

## Objetivo

HabitTracker es una aplicacion Android para crear, consultar y completar habitos personales. La app debe proteger la privacidad del usuario, mantener la integridad de los datos locales y comunicarse con servicios remotos usando canales seguros.

### View

- Las pantallas Compose solo deben renderizar estado y enviar eventos al ViewModel.
- Los ViewModels exponen estado observable mediante `StateFlow`.
- Separar siempre `*Screen` (solo ViewModel + eventos / navegacion) de `*Content` (UI pura con UiState + callbacks, sin ViewModel) para Preview, tests y reutilizacion.
- El estado de UI no debe contener secretos persistentes por defecto.
- Las credenciales introducidas por el usuario no deben aparecer en logs ni en representaciones persistentes del estado.
- El login debe empezar deshabilitado hasta que email y password sean validos.
- Pantalla Login: fondo con gradiente (azul → cyan), marca `logo` + nombre de app arriba, y el formulario (email, password, acciones existentes) dentro de una card blanca redondeada.
- Fuera de alcance en Login: Remember me, Google/Facebook u otros social logins.
- Password reset (aprobado 2026-08-08): ver § Password reset (deep link).
- El drawable `res/drawable/logo.xml` es el logo de la app (login + icono launcher).
- Bottom navigation (Figma Make): Home, Stats, FAB central "New" (abre crear hábito), Friends, Profile — con labels e iconos; selección con fondo `#EEE8F4` y tint `#6750A4`.
- Tab Friends (Figma Make — social):
  - UI Android implementada y conectada al flujo real, alineada con Figma: header "Friends" + botón "Add", sección retos activos, lista de amigos, solicitudes pendientes y bottom sheets (añadir / ver hábitos / crear reto).
  - Textos en ingles (`strings.xml`).
  - Android usa el contrato de dominio `FriendsRepository` y `SocialRemoteDataSource` (Supabase); el `FriendsViewModel` expone el estado de la UI y recarga los datos al volver a primer plano.
  - Todas las lecturas y mutaciones sociales requieren red (remote-only, online-first); no existe cache Room de friends/challenges en el MVP.
  - La privacidad se persiste extremo a extremo mediante `Habit.isPublic`, `HabitEntity.isPublic` y `HabitDto.is_public`; por defecto cada hábito es privado.
  - Plan de implementación: `docs/plans/2026-07-31-friends-figma-android.md`.
  - Fuera de alcance inmediato: notificaciones push, Realtime, cache offline de amigos.

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
- Configuración manual en Supabase Dashboard → Auth → Redirect URLs: incluir `habittracker://auth/reset`.
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
- El usuario puede eliminar habitos y sus registros asociados.
- El usuario puede crear un habito con nombre, descripcion opcional, icono, color, hora de recordatorio opcional y dias de la semana.
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
- `habits`: `id` (uuid), `user_id`, `name`, `description`, `days_of_week` (smallint[] 1=Mon..7=Sun), `icon`, `color_hex`, `reminder_time`, `is_public` (boolean not null default false), `created_at`, `updated_at`, `deleted_at` (soft delete).
- `habit_records`: `id` (uuid), `habit_id`, `user_id`, `date`, `is_completed`, `updated_at`, `deleted_at`; UNIQUE(`habit_id`, `date`); FK a `habits` con CASCADE.
- Estrategia: **online-first**. Mutaciones requieren red. Orden: check connectivity → Supabase → Room cache.
- Pull al login / arranque autenticado: descarga filas del usuario y reemplaza/actualiza la cache Room (remoto gana; no hay cola de cambios offline pendientes).
- Room almacena los mismos campos de sync (`updatedAt` / `deletedAt`) para alinear con el remoto.
- Fuera de alcance: outbox, dual-write offline, WorkManager periodico, UI rica de sync, resolucion manual de conflictos.

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
   - `avatar_color` text not null default `#6750A4`
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
   - `challenger_habit_id`, `challenged_habit_id` uuid FK `habits`
   - `criteria` text check: `streak` | `all_days` | `completion_pct`
   - `starts_at`, `ends_at` timestamptz not null (`ends_at` > `starts_at`)
   - `status` text check: `pending` | `active` | `completed` | `declined` | `cancelled`
   - `created_at` / `updated_at`
   - RLS: solo participantes leen/actualizan su reto; challenger inserta

4. Lectura de `habit_records` de otro usuario: permitida a amigos solo para hábitos `is_public` del amigo, y/o para el hábito del otro en un challenge `active`/`pending` donde el lector es participante (ventana del reto). Escrituras de records siguen siendo solo del dueño.

**Fuera de alcance (schema social):** push notifications, Realtime obligatorio, chat, Room cache de friends (se define en el plan de app).

### Profile (tab)

- La tab Profile muestra UI alineada con el diseno Figma Make (header con gradiente, avatar con iniciales, stats, menu rows, Sign Out).
- Header usa datos reales del usuario autenticado: display name y email (via `AuthRepository`); avatar = iniciales derivadas del nombre.
- Stats del header (reales, desde `HabitRepository.getAllHabitsWithRecords()`):
  - **Day Streak:** max streak entre habitos (`calculateStreak` sobre records, igual que Home).
  - **Completed:** total de records con `isCompleted`.
  - **Habits:** numero de habitos.
- Filas de menu: Notifications, Goals & Targets, Reminders, Preferences, Achievements — solo UI; sin navegacion ni acciones.
- Sign Out: llama a `AuthRepository.signOut()` (Supabase) y navega a Login limpiando el back stack.
- Todos los textos de Profile van en ingles (`strings.xml`).
- `ProfileViewModel` expone nombre, email, stats y el evento de sign-out; no maneja clicks de menu.

### Activity heatmap

- Componente reutilizable `ActivityHeatmap` (grid 4 semanas × 7 dias, lun–dom).
- Ventana: ultimas 4 semanas rolling alineadas a lunes (W4 = semana actual).
- Colores invertidos vs mock oscuro: fondo claro; celda blanca = Less / no completado; morado oscuro = More / completado (escala intermedia en Stats).
- **Detail → tab Calendar:** intensidad binaria del habito (0 o 1).
- **Stats (tab global):** un heatmap agregado; intensidad = % de habitos programados ese dia que estan completados.
- Leyenda Less → More; etiqueta de hoy con borde sutil opcional.
- Tab Stats de Detail mantiene el anillo de %% overall (sin cambiar en este alcance).

### Home screen widgets (Jetpack Glance)

Aprobado 2026-08-07. Tres widgets independientes (Approach A).

**Común**
- Implementación con **Jetpack Glance** + App Widget.
- Textos en ingles (`strings.xml`).
- Colores alineados al theme Habit (`HabitPrimary` `#6750A4` / `HabitPrimaryLight`).
- Actualización: tras completar un hábito, al volver a primer plano la app, y refresh periódico razonable (30–60 min).
- Configuración: **activity de configuración al añadir** el widget (picker clásico), no ajustes dentro de Profile.
- Mutaciones desde widget siguen **online-first** (igual que la app): sin red no se escribe Room ni Supabase.

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

**Fuera de alcance (widgets)**
- Completar desde el widget de reto o del semanal.
- Configurar widgets desde pantallas Profile/Detail.
- Offline mutations / outbox desde el widget.
- Un único widget multipágina.

## Requisitos de Seguridad

### Credenciales

- No se aceptan passwords vacias, triviales o demasiado debiles.
- Una password valida debe tener longitud suficiente y combinar al menos varios tipos de caracteres.
- Las credenciales no deben estar precargadas en `LoginUiState`.
- `LoginUiState.toString()` no debe exponer email ni password.
- La app no debe registrar passwords, tokens ni secretos mediante `Log`.

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
