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

## Arquitectura del Proyecto

* **Modelo:** Clean Architecture con flujo de datos unidireccional.
* **UI:** Jetpack Compose con Material 3.
* **Estado:** `StateFlow` y `collectAsStateWithLifecycle()`.
* **DI:** Hilt.
* **Navegación:** Type-safe mediante clases `@Serializable`.
* **Persistencia:** Room.
* **Acceso remoto:** Supabase.
* **Asincronía:** Kotlin Coroutines y Flow.

Respeta las responsabilidades de cada capa:

```text
presentation → domain ← data
```

La capa `domain` no debe depender de `data` ni de `presentation`.

Los modelos específicos de infraestructura, como Room Entities o DTOs, no deben escapar de la capa `data`.

---

## Reglas Específicas del Proyecto

* Utiliza los modelos de dominio como contrato entre las capas.
* Realiza el mapping de `Entity` y `DTO` a modelos de dominio dentro de la capa `data`.
* Los repositorios definidos en `domain` deben exponer modelos de dominio.
* Las implementaciones de repositorios pertenecen a `data`.
* Los DAOs deben utilizar `Flow` para datos observables cuando sea apropiado.
* Utiliza inyección por constructor con Hilt.
* No instancies repositorios, DAOs ni dependencias manualmente.
* Mantén un flujo de datos unidireccional en la UI.

---

## Protocolo de Trabajo

### Plan Mode

Para cambios estructurales, migraciones o refactorizaciones que afecten a múltiples capas:

1. Analiza el impacto del cambio.
2. Presenta un plan detallado.
3. Espera la aprobación del usuario.
4. Implementa únicamente después de recibir aprobación.

No es necesario solicitar aprobación para correcciones pequeñas, cambios locales o tareas explícitamente definidas por el usuario.

### Testing

Todo cambio de lógica de negocio debe incluir tests adecuados.

Prioriza:

* Tests unitarios para `UseCase`, ViewModels, mappers y lógica de dominio.
* Fakes para dependencias simples.
* Mocks únicamente cuando aporten valor real.
* Tests instrumentados para comportamiento dependiente del framework Android.
* Espresso para flujos de UI cuando sea necesario.

El código sin cobertura de la lógica crítica se considera deuda técnica.

---

## Git y CI/CD

* Sigue Conventional Commits.
* Está prohibido hacer push directo a `main`.
* Todo cambio debe realizarse en una rama independiente.
* Todo archivo nuevo creado durante una tarea debe añadirse al staging de Git mediante `git add <archivo>`.
* Nunca dejes archivos creados por el agente en estado `untracked`.
* Antes de realizar un commit, ejecuta los tests relevantes para los cambios realizados.
* No realices commits si los tests relevantes fallan.

Cualquier merge en `main` debe pasar:

* Linter.
* Validaciones de seguridad.
* Suite de tests.

Las releases utilizan Release Please.

No realices despliegues manuales. El despliegue a producción ocurre mediante el flujo automatizado después del merge de un Release PR aprobado.

---

## Seguridad

* Nunca incluyas credenciales, tokens o API keys hardcoded.
* No expongas secretos en código, logs, commits ni archivos versionados.
* Revisa los cambios relacionados con autenticación, red o secretos antes de realizar un commit.

Las claves utilizadas en GitHub Actions se gestionan mediante GitHub Secrets.

Secrets definidos actualmente:

* `GEMINI_API_KEY`
* `OPENAI_API_KEY`

---