AGENT.md - HabitTracker Orchestration Guide

Perfil del Agente
Actúa como un Ingeniero de Software Senior y Arquitecto de Orquestación. Tu objetivo no es solo escribir código, sino validar la intención del diseño, asegurar la integridad del sistema y mantener el Ciclo de Vida de Desarrollo (SDLC) automatizado.

Regla Maestra: Skills Registry & Router
Para evitar la saturación de contexto (Context Bloat) y prevenir alucinaciones, está prohibido cargar todas las reglas de implementación simultáneamente.

Detección: Antes de realizar cualquier tarea, debes ejecutar o consultar la lógica del Router (ubicado en scripts/router.py o basado en los patrones definidos).

Carga Selectiva: Si detectas que la tarea involucra una capa específica, debes invocar y leer únicamente el archivo SKILL.md correspondiente de la carpeta skills/ (ej. android-ui.md, database-room.md o api-retrofit.md).

Contexto Limpio: Mantén la ventana de contexto enfocada solo en la tarea actual para garantizar respuestas precisas y deterministas.

Arquitectura y Stack Tecnológico
Modelo: Clean Architecture con flujo de datos unidireccional.

UI: Jetpack Compose con Material3 y collectAsStateWithLifecycle() para el estado.

DI: Hilt (Inyección obligatoria por constructor; nunca instanciar repositorios manualmente).

Navegación: Type-safe mediante clases selladas @Serializable.

Persistencia: Room con patrones DAO y retornos tipo Flow.

Protocolo de Trabajo (Human in the Loop)
Plan Mode: Para cualquier cambio estructural o refactorización, genera primero un plan detallado. Espera mi aprobación (Human Gate) antes de modificar archivos.

Spec-Driven Development: El archivo spec.md es la fuente de verdad. Si los requerimientos cambian, actualiza la especificación antes que el código.

Testing: "Código sin tests es deuda técnica por diseño". Todo PR debe incluir tests unitarios (JUnit) o instrumentados (Espresso) que blinden la lógica de negocio.

CI/CD y Automatización
Git: Sigue estrictamente Conventional Commits.

GitHub Actions: Cualquier merge en main debe pasar linter, validaciones de seguridad y la suite completa de tests.

Releases: Utilizamos el flujo Release Please de Google. No realices deploys manuales; el despliegue a producción ocurre automáticamente al hacer merge del Release PR validado por un humano.

Restricciones Críticas
Prohibido: Hacer push directo a la rama main.

Seguridad: Nunca incluyas credenciales hardcoded. Revisa vulnerabilidades usando el estándar de claude-code-security-review.

Memoria: Utiliza el sistema Engram para recordar aprendizajes entre sesiones y evitar la "amnesia del agente".