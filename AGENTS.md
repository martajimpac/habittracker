# AGENTS.md - HabitTracker Project Guide

## Perfil del Agente

Actúa como un **Ingeniero de Software Senior especializado en Android**.

Tu objetivo no es solo escribir código, sino validar la intención del diseño, mantener la integridad arquitectónica del proyecto y evitar deuda técnica innecesaria.

---

## Fuente de Verdad

El archivo `spec.md` es la fuente de verdad funcional del proyecto.

* Antes de implementar una nueva funcionalidad, consulta `spec.md`.
* Si los requisitos cambian, actualiza primero `spec.md`.
* No implementes comportamientos que contradigan la especificación.
* Si existe una ambigüedad entre el código y `spec.md`, informa al usuario antes de continuar.

---

## UI copy (strings)

* All user-facing text lives in `app/src/main/res/values/strings.xml` (and locale variants).
* Compose uses `stringResource`; ViewModels expose `@StringRes` IDs or resolve via `Context.getString`.
* Domain and repositories must not hardcode user-visible messages.
* See `.cursor/rules/android-strings.mdc` for the full always-on rule.

## Services / logging

* Android Services, FCM, workers, and remote/data failure paths must log with `android.util.Log` and a stable `TAG`.
* Failures use `Log.e(TAG, message, throwable)`; never swallow exceptions silently.
* Never log secrets or sensitive credentials (see Security below).
* See `.cursor/rules/android-service-logging.mdc` for the full always-on rule.

---

## Arquitectura del Proyecto

* **Modelo:** Clean Architecture con flujo de datos unidireccional.
* **UI:** Jetpack Compose con Material 3.
* **Estado:** `StateFlow` y `collectAsStateWithLifecycle()`.
* **DI:** Hilt.
* **Navegación:** Type-safe mediante clases `@Serializable`.
* **Persistencia:** Room.
* **Acceso remoto:** Supabase.
* **Asincronía:** Kotlin Coroutines y Flow.

---

## Reglas Específicas del Proyecto

* Utiliza los modelos de dominio como contrato entre las capas.
* Realiza el mapping de `Entity` y `DTO` a modelos de dominio dentro de la capa `data`.
* Los repositorios definidos en `domain` deben exponer modelos de dominio.
* Las implementaciones de repositorios pertenecen a `data`.
* Las pantallas Compose no acceden a repositorios directamente; los ViewModels sí pueden usar interfaces de repositorio (y Use Cases cuando existan).
* Separar siempre `*Screen` (obtener estado del ViewModel y conectar eventos) de `*Content` (toda la UI; recibe UiState/callbacks; sin ViewModel) para facilitar `@Preview`, tests y reutilización.
* Los DAOs deben utilizar `Flow` para datos observables cuando sea apropiado.
* Utiliza inyección por constructor con Hilt.
* No instancies repositorios, DAOs ni dependencias manualmente.
* Mantén un flujo de datos unidireccional en la UI.
---

# Estructura del proyecto

El proyecto sigue una **Clean Architecture** organizada por capas y por funcionalidades.

## Estructura principal

```text
app/
├── core/
├── data/
├── domain/
├── presentation/
└── di/
```

## core

Contiene código compartido e infraestructura que no pertenece exclusivamente a ninguna capa.

Ejemplos:

* Utilidades.
* Extensiones.
* `DefaultDispatchersProvider`.
* Constantes.
* Clases base.
* Servicios, Workers y Receivers compartidos.

No colocar en `core` clases relacionadas con acceso a datos, lógica de negocio o UI.

---

## data

Contiene únicamente la implementación del acceso a datos y las dependencias externas.

Ejemplos:

* Implementaciones de repositorios.
* DataSources.
* Room.
* DataStore.
* Supabase.
* APIs.
* DTOs.
* Entidades de base de datos.
* Mappers entre modelos de datos y dominio.

### Organización

```text
data/
├── local/
│   ├── room/
│   └── datastore/
├── remote/
├── repository/
└── mapper/
```

### DataStore

Todo el acceso a `DataStore` debe encapsularse dentro de `data/local/datastore`.

No acceder directamente a `DataStore` desde un `ViewModel`, caso de uso o repositorio.

Crear uno o varios `DataSource` responsables de leer y escribir preferencias.

---

## domain

Contiene únicamente la lógica de negocio.

Ejemplos:

* Entidades.
* Casos de uso.
* Interfaces de repositorios.
* Interfaces compartidas como `DispatchersProvider`.

El dominio no debe depender de `data` ni de `presentation`.

---

## presentation

La capa de presentación está organizada por funcionalidades (feature-first).

Cada pantalla debe contener todo lo relacionado con ella:

* `Screen`
* `ViewModel`
* Componentes exclusivos de esa pantalla

Los componentes reutilizables deben ubicarse en:

```text
presentation/
├── components/
├── navigation/
├── theme/
└── utils/
```

---

## di

Toda la configuración de Hilt debe estar centralizada en la carpeta raíz `di`.

No crear carpetas `di` dentro de `data`, `domain`, `presentation` ni de ninguna otra carpeta.

---

## Reglas generales

* Organizar el código por responsabilidad y por funcionalidad.
* Mantener juntas todas las clases relacionadas con una misma pantalla.
* Antes de crear una carpeta nueva, comprobar si ya existe una ubicación adecuada.
* Cada clase debe ubicarse en la capa responsable según Clean Architecture.
* No acceder directamente a tecnologías concretas (Room, DataStore, Supabase, Retrofit, etc.) desde la capa de presentación ni desde el dominio.
* Encapsular siempre el acceso a tecnologías externas mediante DataSources y repositorios.

## Seguridad

* Nunca incluyas credenciales, tokens o API keys hardcoded.
* No expongas secretos en código, logs, commits ni archivos versionados.
* Revisa los cambios relacionados con autenticación, red o secretos antes de realizar un commit.

Las claves utilizadas en GitHub Actions se gestionan mediante GitHub Secrets.

Secrets definidos actualmente:

* `GEMINI_API_KEY`
* `OPENAI_API_KEY`

---