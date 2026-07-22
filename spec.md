A# HabitTracker Spec

## Objetivo

HabitTracker es una aplicacion Android para crear, consultar y completar habitos personales. La app debe proteger la privacidad del usuario, mantener la integridad de los datos locales y comunicarse con servicios remotos usando canales seguros.

### View

- Las pantallas Compose solo deben renderizar estado y enviar eventos al ViewModel.
- Los ViewModels exponen estado observable mediante `StateFlow`.
- El estado de UI no debe contener secretos persistentes por defecto.
- Las credenciales introducidas por el usuario no deben aparecer en logs ni en representaciones persistentes del estado.
- El login debe empezar deshabilitado hasta que email y password sean validos.

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

## Requisitos Funcionales Basicos

- El usuario puede iniciar sesion con email y password validos.
- El usuario puede visualizar habitos por fecha.
- El usuario puede marcar y desmarcar un habito como completado.
- El usuario puede consultar el detalle e historial de un habito.
- El usuario puede eliminar habitos y sus registros asociados.
- El usuario puede crear un habito con nombre, descripcion opcional, icono, color, hora de recordatorio opcional y dias de la semana.
- Home, Stats y Detail muestran el icono y color persistidos del habito.
- Los habitos se leen desde Room para uso offline.
- El id del habito es un UUID generado en el cliente (String), compartido entre Room y el futuro sync con Supabase (sin `remoteId` separado; sin sync en este alcance).
- Detalle de diseno: `docs/designs/2026-07-21-add-habit-figma-design.md`.

### Profile (tab)

- La tab Profile muestra UI alineada con el diseno Figma Make (header con gradiente, avatar con iniciales, stats, menu rows, Sign Out).
- Header usa datos reales del usuario autenticado: display name y email (via `AuthRepository`); avatar = iniciales derivadas del nombre.
- Stats del header (Day Streak, Completed, Habits) son valores mock fijos por ahora; no se calculan desde habitos.
- Filas de menu: Notifications, Goals & Targets, Reminders, Preferences, Achievements — solo UI; sin navegacion ni acciones.
- Sign Out es solo UI; sin logout real en este alcance.
- Todos los textos de Profile van en ingles (`strings.xml`).
- `ProfileViewModel` solo expone nombre y email; no maneja clicks de menu ni sign-out.

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

### Secretos

- El proyecto debe escanearse en tests para detectar posibles claves de Firebase, API keys, tokens, passwords o private keys hardcodeadas.
- Los secretos reales deben vivir fuera del repositorio, por ejemplo en variables de entorno, configuracion segura o secretos del CI.

## Testing Minimo

- Unit tests de dominio para validacion de credenciales.
- Unit tests de estado UI para evitar exposicion de credenciales.
- Unit tests de red para verificar HTTPS y manejo seguro de tokens.
- Unit tests o scripts de seguridad para deteccion de secretos hardcodeados.
- Instrumented tests de Room para validar integridad referencial y unicidad de registros.

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

