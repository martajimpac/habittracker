# 🎯 CI/CD Profesional con Release Please - Documentación Integral

## 📑 Índice Rápido

1. **[Resumen Ejecutivo](#resumen-ejecutivo)** - Vista de alto nivel
2. **[Arquitectura del Sistema](#arquitectura-del-sistema)** - Cómo funciona todo
3. **[Guía Rápida de Inicio](#guía-rápida-de-inicio)** - Primeros pasos
4. **[Flujo de Release](#flujo-de-release)** - Cómo se generan releases
5. **[Troubleshooting](#troubleshooting)** - Solucionar problemas
6. **[Referencias](#referencias)** - Enlaces útiles

---

## 📊 Resumen Ejecutivo

Se ha implementado un **sistema CI/CD profesional** que:

✅ **Automatiza validación de código** en cada PR (lint, tests, build)  
✅ **Genera releases automáticos** usando Release Please + Conventional Commits  
✅ **Crea Release Notes con IA** resumiendo cambios técnicos  
✅ **Mantiene versionado SemVer** automático basado en tipos de commit  
✅ **Construye artefactos** (APK, AAB) automáticamente en cada release  
✅ **Documenta cambios** con changelog automático  

**Beneficios:**
- 🚀 Releases más rápidas (sin manual paperwork)
- 📝 Changelog consistente y profesional
- 🤖 Menos trabajo manual repetitivo
- 📊 Historial claro de cambios
- 🔒 Garantía de calidad antes de publicar

---

## 🏗️ Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────────┐
│                     GitHub Repository                            │
│  (main branch protegida con reglas de checks)                   │
└──────────────────────┬──────────────────────────────────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
        ▼                             ▼
   ┌─────────────┐           ┌──────────────┐
   │ Pull Request│           │  Direct Push │
   │   Creation  │           │   to main    │
   └──────┬──────┘           └──────┬───────┘
          │                         │
          ▼                         ▼
   ┌──────────────────────────────────────┐
   │  JOB 1: PR Checks                    │
   │  ├─ Lint validation (Android)        │
   │  ├─ Unit tests execution             │
   │  ├─ Build APK validation             │
   │  └─ Code quality analysis            │
   └──────────────────────────────────────┘
          │
          │ (Must PASS for merge)
          ▼
   ┌──────────────────────────────────────┐
   │  PR Approved & Merged                │
   │  (triggers on push to main)          │
   └──────────────────────────────────────┘
          │
          ▼
   ┌──────────────────────────────────────┐
   │  JOB 2: Release Please               │
   │  ├─ Analiza Conventional Commits     │
   │  ├─ Calcula nueva versión (SemVer)   │
   │  ├─ Genera changelog                 │
   │  └─ Crea Release PR                  │
   └──────────────────────────────────────┘
          │
          │ (Si es release, continúa)
          ▼
   ┌──────────────────────────────────────┐
   │  JOB 3: AI Release Notes             │
   │  ├─ Extrae commits                   │
   │  ├─ Procesa con OpenAI (opcional)    │
   │  └─ Actualiza GitHub Release         │
   └──────────────────────────────────────┘
          │
          ▼
   ┌──────────────────────────────────────┐
   │  JOB 4: Build Release Artifacts      │
   │  ├─ Compila APK release              │
   │  ├─ Genera Bundle (AAB)              │
   │  └─ Sube artefactos a Release        │
   └──────────────────────────────────────┘
          │
          ▼
   ┌──────────────────────────────────────┐
   │  JOB 5: Notificación Final           │
   │  ├─ Genera resumen                   │
   │  ├─ Imprime en workflow summary      │
   │  └─ ✅ Release Publicado             │
   └──────────────────────────────────────┘
```

---

## 🚀 Guía Rápida de Inicio

### Paso 1: Clonar y Configurar (Primera Vez)
```bash
# Clonar el repositorio
git clone https://github.com/tu-usuario/HabitTracker.git
cd HabitTracker

# Instalar git hooks para validación local
chmod +x scripts/commit-msg
cp scripts/commit-msg .git/hooks/commit-msg
chmod +x .git/hooks/commit-msg

# (Opcional) Configurar OpenAI para AI Release Notes
export OPENAI_API_KEY="sk-..."
```

### Paso 2: Crear Feature/Fix
```bash
# Crear rama
git checkout -b feat/mi-feature

# Hacer cambios
# Editar archivos...

# Validar localmente (mismo que CI)
./gradlew clean build

# Commit con Conventional Commits
git commit -m "feat(scope): descripción corta

Descripción más detallada de qué cambió y por qué.
Incluye contexto técnico relevante.

Closes #123"
```

El hook validará automáticamente el formato:
```
✅ Commit válido: feat(scope): descripción corta
```

### Paso 3: Push y PR
```bash
# Push a rama
git push origin feat/mi-feature

# GitHub Actions ejecutará PR Checks automáticamente
# Espera a que pasen todos los checks

# Merge a main (en GitHub)
# El pipeline de Release Please se dispara automáticamente
```

### Paso 4: Release Automático
1. Release Please detecta los commits
2. Crea un PR con:
   - Nueva versión calculada
   - Changelog generado
   - Release Notes de IA
3. Al hacer merge del PR:
   - Se crea GitHub Release
   - Se construyen APK/AAB
   - Se publican artefactos

---

## 📝 Flujo de Release

### Ejemplo Real: Múltiples Features y Fixes

**Commits realizados:**
```
main
  ↓
[Commit 1] feat(home): add dark mode
[Commit 2] fix(auth): resolve email validation
[Commit 3] feat(database): add habit backup
[Commit 4] perf(home): optimize queries
[Commit 5] test(dao): add unit tests
```

**Release Please calcula:**
```
feat (x2) = MINOR bump
fix (x1) = PATCH bump
→ Resultado: MINOR prioritario
→ Nueva versión: 1.1.0 (asumiendo anterior era 1.0.0)
```

**Changelog generado automáticamente:**
```markdown
## 1.1.0 (2026-06-10)

### ✨ Features
- **home**: add dark mode ([abc1234](https://github.com/repo/commit/abc1234))
- **database**: add habit backup ([def5678](https://github.com/repo/commit/def5678))

### 🐛 Bug Fixes
- **auth**: resolve email validation ([ghi9012](https://github.com/repo/commit/ghi9012))

### ⚡ Performance Improvements
- **home**: optimize queries ([jkl3456](https://github.com/repo/commit/jkl3456))

### 🧪 Testing
- **dao**: add unit tests ([mno7890](https://github.com/repo/commit/mno7890))
```

**Release Notes generadas por IA:**
```markdown
## 📋 Release 1.1.0

### 🎯 Resumen
Esta versión introduce tema oscuro en home screen, mejora significativa
de rendimiento en consultas de base de datos, y correcciones críticas
en validación de autenticación.

### ✨ Mejoras Principales
- **Dark Mode**: Integración completa de tema oscuro usando Material3
- **Database Backup**: Nuevo sistema de respaldo de hábitos a Firebase
- **Performance**: Optimización de 40% en consultas frecuentes

### 📋 Cambios Técnicos
- Refactor de StateFlow en HomeViewModel
- Implementación de Room Backup Executor
- Índices de base de datos agregados

### 🚀 Instalación
./gradlew clean build
```

---

## 🔄 Conventional Commits Reference

| Tipo | Bump | Ejemplo |
|------|------|---------|
| `feat` | MINOR | `feat(auth): add oauth provider` |
| `fix` | PATCH | `fix(home): resolve null pointer` |
| `perf` | MINOR | `perf(db): add query indexes` |
| `refactor` | MINOR | `refactor(viewmodel): simplify logic` |
| `docs` | No | `docs(readme): add setup guide` |
| `test` | No | `test(dao): add unit tests` |
| `chore` | No | `chore: update deps` |
| `style` | No | `style: format code` |
| `BREAKING CHANGE` | MAJOR | `BREAKING CHANGE: remove old API` |

---

## 🤖 Integración de IA: Paso a Paso

### Configuración Requerida

#### Opción 1: OpenAI (Recomendado)
```bash
# 1. Obtén API Key en https://platform.openai.com/api-keys
# 2. Guarda en GitHub Secrets:
#    Repo → Settings → Secrets → OPENAI_API_KEY = sk-...

# 3. El workflow usa OpenAI automáticamente en cada release
```

#### Opción 2: Ejecutar Localmente
```bash
# 1. Instala librería
pip install openai

# 2. Configura variable
export OPENAI_API_KEY="sk-..."

# 3. Genera notas manualmente
python scripts/generate-release-notes.py /path/to/commits.txt 1.1.0
```

### Cómo Funciona el Script de IA

```python
# El script recibe:
# 1. Commits entre versiones (hash|subject|body)
# 2. Número de versión

# Procesa con IA:
# - Clasifica cambios (features, fixes, improvements)
# - Genera resumen ejecutivo
# - Identifica breaking changes
# - Crea guía de actualización

# Retorna:
# Release Notes en Markdown listos para publicar
```

---

## 📋 Archivos Principales

```
.github/
├── workflows/
│   ├── pr-checks.yml                # Validación en PRs
│   ├── instrumented-tests.yml       # Tests en emulador
│   └── release-please.yml           # 🔑 FLUJO PRINCIPAL
├── CI_CD.md                         # Documentación básica
└── RELEASE_PLEASE_GUIDE.md          # Guía completa

scripts/
├── commit-msg                       # Hook git para validación
└── generate-release-notes.py        # Script de IA

CONVENTIONAL_COMMITS.md             # Guía de commits
DEVELOPMENT_SETUP.md                # Setup local
CI_CD_MASTER_GUIDE.md               # Este archivo
```

---

## 🔧 Troubleshooting

### Release Please no crea PR

**Problema:** Hizo push a main pero no hay Release PR

**Soluciones:**
1. Verifica que commits sigan **Conventional Commits**:
   ```bash
   git log --oneline origin/main..HEAD
   # Deben ser: feat(...), fix(...), etc.
   ```

2. Verifica que el workflow se ejecutó:
   - GitHub → Actions → Release Please CI/CD Pipeline
   - Busca la ejecución más reciente

3. Ejecuta el análisis manualmente:
   ```bash
   # Ver qué versión calcularía Release Please
   git log --pretty=format:"%B" origin/main..HEAD | grep -E "^(feat|fix|BREAKING)"
   ```

### AI Release Notes no se generan

**Problema:** Las notas de release no tienen contenido de IA

**Soluciones:**
1. Verifica que `OPENAI_API_KEY` esté en Secrets:
   ```
   Repo → Settings → Secrets and variables → Actions
   ```

2. Verifica que la key sea válida:
   ```bash
   python3 -c "from openai import OpenAI; OpenAI()" 
   # Si no da error, es válida
   ```

3. Revisa los logs del workflow:
   - GitHub → Actions → último release
   - Job: "AI-Powered Release Notes Generation"
   - Busca línea de error

4. Si todo falla, se usa template por defecto (sin IA)

### Build falla en GitHub pero pasa localmente

**Problema:** El workflow de release falla en build step

**Soluciones:**
```bash
# 1. Ejecuta lo mismo que CI localmente
./gradlew clean build

# 2. Si falla localmente, corrige antes de hacer push
# Los mismos comandos del workflow:
./gradlew lint test assembleDebug

# 3. Verifica configuración local
./gradlew --version
gradle.properties  # Verifica variables

# 4. Limpia caché Gradle
rm -rf .gradle
./gradlew build
```

### Commits son rechazados por hook

**Problema:** Git hook rechaza mensaje de commit

**Soluciones:**
```bash
# 1. Verifica que el hook esté instalado
ls -la .git/hooks/commit-msg
# Debe existir y ser ejecutable (x)

# 2. Valida el formato del mensaje
bash scripts/commit-msg "tu mensaje"
# Si dice ✅ Válido, es correcto

# 3. Asegúrate que cumple con Conventional Commits
# ❌ Malo: "added feature"
# ✅ Bueno: "feat(scope): add feature"
```

---

## 📊 Métricas y Monitoreo

### Acceder a Métricas del Workflow
```bash
# Ver historial de releases
gh release list --repo tu-usuario/HabitTracker

# Ver últimos workflows
gh run list --repo tu-usuario/HabitTracker

# Ver logs de un workflow específico
gh run view <RUN_ID> --log
```

### Dashboard en GitHub
1. Accede a tu repositorio
2. **Actions** tab → Ver todos los workflows
3. **Release Please CI/CD Pipeline** → histórico de ejecuciones
4. Click en una ejecución para ver detalles y logs

---

## 🎓 Mejores Prácticas

### ✅ Commits Óptimos
```bash
# Frecuencia: 2-4 commits por feature
# Atomicidad: Cada commit debe compilar y pasar tests
# Claridad: Mensaje descripción, no comentario

✅ Ejemplo bueno:
git commit -m "feat(auth): add two-factor authentication

- Implement TOTP provider using Google Authenticator API
- Add verification screen in AuthViewModel
- Store secret in secure EncryptedSharedPreferences

Closes #234"
```

### ✅ Flujo de PR
```bash
1. Crea rama desde main actualizado
2. Hace commits pequeños y atómicos
3. Pushea y crea PR
4. Espera a que pasen todos los checks (lint, test, build)
5. Request review de un compañero
6. Discute y realiza cambios si necesario
7. Merge cuando esté aprobado
8. Release Please genera PR automáticamente
```

### ✅ Versionado
```bash
# Respeta SemVer:
# MAJOR: Breaking changes (cambios incompatibles)
# MINOR: Features (funcionalidad nueva)
# PATCH: Fixes (correcciones)

v1.0.0  (inicial)
v1.1.0  (nuevas features)
v1.1.1  (bugs fixed)
v2.0.0  (breaking changes)
```

---

## 🔗 Referencias Útiles

- **Release Please**: https://github.com/googleapis/release-please
- **Conventional Commits**: https://www.conventionalcommits.org/
- **Semantic Versioning**: https://semver.org/
- **GitHub Actions**: https://docs.github.com/en/actions
- **OpenAI API**: https://platform.openai.com/docs
- **Git Hooks**: https://git-scm.com/book/en/v2/Customizing-Git-Git-Hooks

---

## 📞 Soporte y Contacto

Para problemas o preguntas:
1. Revisa el **Troubleshooting** arriba
2. Consulta los documentos específicos:
   - `RELEASE_PLEASE_GUIDE.md` - Guía detallada
   - `DEVELOPMENT_SETUP.md` - Setup local
   - `CONVENTIONAL_COMMITS.md` - Formato de commits
3. Abre una issue en GitHub con detalles

---

## 📝 Control de Versiones de esta Documentación

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0.0 | 2026-06-09 | Implementación inicial |

---

**Última actualización:** 9 de Junio, 2026  
**Maintainer:** DevOps Team  
**Status:** ✅ Production Ready

