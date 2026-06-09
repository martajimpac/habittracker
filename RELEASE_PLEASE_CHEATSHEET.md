# 🚀 Cheat Sheet - Release Please CI/CD en HabitTracker

## 📌 Resumen de 30 segundos

```
┌─────────────────────────────────────────────────────────────┐
│ Haces cambios → Commit con Conventional Commits            │
│             ↓                                               │
│ Push a rama → GitHub Actions valida (lint, test, build)    │
│             ↓                                               │
│ Merge a main → Release Please crea Release PR automático   │
│             ↓                                               │
│ Merge Release PR → Construye APK/AAB, publica release      │
│                   con Release Notes de IA                  │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎯 Comandos Esenciales

### Setup Inicial (Primera Vez)
```bash
# Instalar git hooks
chmod +x scripts/commit-msg
cp scripts/commit-msg .git/hooks/commit-msg
chmod +x .git/hooks/commit-msg
```

### Desarrollo Diario
```bash
# Crear rama
git checkout -b feat/tu-feature

# Validar código antes de push (= CI/CD)
./gradlew clean build

# Commit con formato requerido
git commit -m "feat(scope): mensaje corto

Descripción detallada.
Incluye por qué.

Closes #123"

# Push a rama
git push origin feat/tu-feature

# En GitHub: Crea PR → Espera checks → Merge a main
# Release Please hace el resto automáticamente
```

---

## 📋 Formato de Commits (MUY IMPORTANTE)

```
TIPO(SCOPE): MENSAJE

feat(home)     → Nueva feature → MINOR version (+1 en x.Y.z)
fix(auth)      → Bug fix      → PATCH version (+1 en x.y.Z)
perf(db)       → Performance  → MINOR version
refactor(api)  → Refactor     → MINOR version
docs(readme)   → Documentación → No afecta version
test(dao)      → Tests        → No afecta version
chore(deps)    → Build/deps   → No afecta version

BREAKING CHANGE: ... → MAJOR version (+1 en X.y.z)
```

**Ejemplos:**
```bash
git commit -m "feat(home): add dark mode"
git commit -m "fix(auth): resolve email validation bug"
git commit -m "perf(database): optimize queries"
git commit -m "docs(readme): update setup instructions"
```

---

## ✅ Pre-Push Checklist

```bash
# 1. Compila correctamente
./gradlew build

# 2. Todos los tests pasan
./gradlew test

# 3. Lint sin errores
./gradlew lint

# 4. Commits siguen Conventional Commits
git log --oneline main..HEAD
# Debe mostrar: feat(...), fix(...), etc.

# 5. Commit messages son descriptivos
git log --format="%B" main..HEAD

# ✅ Si todo OK → git push
```

---

## 🔄 Versioning Automático

| Commits | Versión Anterior | Nueva Versión |
|---------|------------------|---------------|
| feat | 1.0.0 | **1.1.0** |
| fix | 1.0.0 | **1.0.1** |
| feat + fix | 1.0.0 | **1.1.0** |
| BREAKING CHANGE | 1.0.0 | **2.0.0** |

**Release Please calcula automáticamente el bump semántico**

---

## 🤖 AI Release Notes

### Setup (Opcional)
```bash
# 1. Obtén API key en https://platform.openai.com/api-keys
# 2. En GitHub: Settings → Secrets → Add OPENAI_API_KEY
# 3. ¡Listo! Se generan automáticamente en cada release
```

### Sin Setup
```bash
# Funciona con template por defecto (sin IA)
# Las notas de release igualmente se generan correctamente
```

---

## 📊 Ver Estado del Pipeline

### En GitHub
1. Tu PR → Pestaña "Checks"
2. Ver que pasen: Lint ✅, Tests ✅, Build ✅
3. Una vez merged → Actions → Ver Release Please

### En Terminal
```bash
# Ver últimas releases
gh release list

# Ver historial de workflows
gh run list

# Ver logs detallados
gh run view <RUN_ID> --log
```

---

## 🐛 Troubleshooting Rápido

| Problema | Solución |
|----------|----------|
| **Commit rechazado** | Revisa formato: `type(scope): message` |
| **CI/CD falla en lint** | `./gradlew lint` localmente y corrige |
| **Tests fallan en GitHub** | `./gradlew test` localmente y arregla |
| **Release PR no se crea** | Verifica commits sigan Conventional Commits |
| **AI no genera notas** | Configura OPENAI_API_KEY en Secrets |
| **Build de release falla** | `./gradlew clean build` y revisa logs |

---

## 📁 Archivos Creados

```
.github/
├── workflows/
│   ├── pr-checks.yml              ← Validación en PRs
│   ├── release-please.yml         ← 🔑 Release automático
│   └── instrumented-tests.yml     ← Tests en emulador
├── CI_CD.md                       ← Docs básicas
└── RELEASE_PLEASE_GUIDE.md        ← Guía detallada

scripts/
├── commit-msg                     ← Hook git para validar
└── generate-release-notes.py      ← Script IA (opcional)

Documentación:
├── CI_CD_MASTER_GUIDE.md          ← Guía completa
├── CONVENTIONAL_COMMITS.md        ← Cómo escribir commits
├── DEVELOPMENT_SETUP.md           ← Setup local
└── AGENTS.md                      ← Guía para IA agents
```

---

## 🔑 Conceptos Clave

### Conventional Commits
Formato estándar para mensajes de commit:
```
<type>(<scope>): <subject>

<body>

<footer>
```
Permite que Release Please calcule versiones automáticamente.

### Semantic Versioning (SemVer)
- **MAJOR** (v2.0.0): Breaking changes
- **MINOR** (v1.1.0): Nuevas features
- **PATCH** (v1.0.1): Bug fixes

### Release Please
Herramienta de Google que:
- Lee commits Conventional
- Calcula nueva versión
- Crea Release PR
- Genera changelog
- Publica GitHub Release

### GitHub Actions
CI/CD native de GitHub que ejecuta:
- Validación en cada PR
- Release automático en main
- Construcción de artefactos

---

## 💡 Tips Profesionales

✅ **Commits frecuentes**: Hacer commits después de completar feature lógica  
✅ **Mensajes claros**: Describe QUÉ y POR QUÉ, no solo cambios  
✅ **Validar localmente**: Corre `./gradlew build` antes de push  
✅ **Revisar PRs**: No mergees sin que pasen todos los checks  
✅ **Mantén main limpio**: main siempre debe estar en estado releaseable  
✅ **Documenta en commits**: Good commits = good changelogs  

---

## 🚀 Primera Release

```bash
# 1. Haz algunos commits con formato Conventional
git commit -m "feat(home): add calendar view"
git commit -m "fix(auth): resolve session bug"

# 2. Push a main
git push origin feat-branch
# Luego merge en GitHub

# 3. Espera a que Release Please cree Release PR
# Verifica la versión y changelog generados

# 4. Merge del Release PR
# Automáticamente se construye release completo

# 5. GitHub Release disponible en:
# github.com/tu-usuario/HabitTracker/releases
```

---

## 📚 Documentación Completa

Para detalles sobre cada componente:

- **RELEASE_PLEASE_GUIDE.md** - Configuración detallada
- **DEVELOPMENT_SETUP.md** - Setup local paso a paso
- **CONVENTIONAL_COMMITS.md** - Todos los formatos permitidos
- **CI_CD_MASTER_GUIDE.md** - Arquitectura completa

---

## ❓ FAQ Rápida

**P: ¿Qué pasa si cometo un error en el commit?**
R: Puedes amend o squash antes de hacer push:
```bash
git commit --amend -m "feat(scope): corrected message"
```

**P: ¿Cómo reversal un commit?**
R: Crea un nuevo commit que revierte:
```bash
git revert <commit-hash>
```

**P: ¿Puedo forzar un release?**
R: Release Please lo hace automático, pero puedes crear un tag manualmente:
```bash
git tag -a v1.2.0 -m "Manual release"
git push origin v1.2.0
```

**P: ¿Qué si quiero saltarme Release Please?**
R: En workflow settings puedes deshabilitarlo, pero no es recomendado en producción.

---

**⚡ TL;DR:** Commit con `feat/fix/perf(scope): msg` → Push → GitHub hace el resto 🚀


