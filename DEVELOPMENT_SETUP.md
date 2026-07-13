# 🛠️ Setup de Desarrollo Local - HabitTracker CI/CD

Este documento describe cómo configurar tu entorno local para trabajar con el pipeline CI/CD de HabitTracker.

## 📋 Requisitos Previos

### Sistema
- **Git** 2.30+
- **JDK** 11+ (para Android development)
- **Android SDK** API 30+ (compilación)
- **Gradle** 7.0+ (incluido en proyecto)

### Python (para scripts IA opcionales)
```bash
python3 --version  # 3.8+
pip install openai  # Solo si usarás AI Release Notes
```

---

## ⚙️ Instalación Inicial

### 1. Clonar el repositorio
```bash
git clone https://github.com/tu-usuario/HabitTracker.git
cd HabitTracker
```

### 2. Configurar Git Hooks (Validación de Commits)
```bash
# Hacer el script ejecutable
chmod +x scripts/commit-msg

# Instalar el hook
cp scripts/commit-msg .git/hooks/commit-msg
chmod +x .git/hooks/commit-msg
```

**Verificación:**
```bash
# El siguiente comando debe fallar (mensaje inválido)
git commit -m "added new feature"
# ❌ Commit inválido: No sigue Conventional Commits

# El siguiente debe pasar (mensaje válido)
git commit -m "feat(home): add new feature"
# ✅ Commit válido
```

### 3. Configurar OpenAI (Opcional)
Si deseas generar Release Notes con IA:

```bash
# En Windows PowerShell
$env:OPENAI_API_KEY = "sk-..."

# En Linux/macOS
export OPENAI_API_KEY="sk-..."

# Persistir en bash (Linux/macOS)
echo 'export OPENAI_API_KEY="sk-..."' >> ~/.bashrc
source ~/.bashrc
```

**Obtener API Key:**
1. Accede a https://platform.openai.com/api-keys
2. Click "Create new secret key"
3. Copia el valor (comienza con `sk-`)
4. Nunca lo compartas en repositorios

---

## 🔄 Workflow Diario

### 1. Crear rama de feature
```bash
git checkout main
git pull origin main
git checkout -b feat/tu-feature

# O para fixes
git checkout -b fix/bug-name
```

### 2. Realizar cambios y commits
```bash
# Editar archivos...

# Validar cambios localmente
./gradlew lint test

# Commit con Conventional Commits (el hook validará)
git commit -m "feat(home): add dark mode toggle

- Integrate system theme detection
- Add theme preferences in ProfileViewModel
- Update Material3 theme colors

Closes #42"
```

**Nota:** El hook validará que el mensaje siga el formato:
```
<type>(<scope>): <message>
```

### 3. Push a rama
```bash
git push origin feat/tu-feature
```

### 4. Crear Pull Request
- GitHub Actions ejecutará automáticamente:
  - ✅ Lint checks
  - ✅ Unit tests
  - ✅ Build validation

### 5. Merge a main
Cuando el PR esté approved y todos los checks pasen:
- Release Please detectará el commit
- Creará Release PR automáticamente
- Al hacer merge del Release PR, se ejecutará la construcción final

---

## 🧪 Validación Local Equivalente al CI/CD

Antes de hacer push, ejecuta lo mismo que el CI:

```bash
# 1️⃣ Validación de lint
./gradlew lint

# 2️⃣ Ejecutar tests unitarios
./gradlew test

# 3️⃣ Construir APK
./gradlew assembleDebug

# 4️⃣ Ejecutar todos los checks juntos
./gradlew build

# 5️⃣ Limpiar antes de push (recomendado)
./gradlew clean build
```

### Script rápido (alias)
Para no escribir siempre los comandos:

**Linux/macOS (.bashrc o .zshrc):**
```bash
alias ci-validate="./gradlew clean build && echo '✅ CI validado'"
```

**Windows PowerShell ($PROFILE):**
```powershell
function ci-validate { & ".\gradlew.bat" clean build; Write-Host "✅ CI validado" }
```

Uso:
```bash
ci-validate
```

---

## 🚀 Probar Localmente: Simular Release Please

Para simular cómo funciona Release Please localmente:

### 1. Ver commits pendientes
```bash
git log --oneline main..HEAD
```

### 2. Categorizar tus commits
El pipeline automáticamente:
```
feat(*)  →  MINOR version bump
fix(*)   →  PATCH version bump
BREAKING CHANGE  →  MAJOR version bump
```

### 3. Verificar formato Conventional Commits
```bash
# Todos tus commits deben cumplir este patrón
git log --format="%h %s" main..HEAD | \
  grep -E "^[a-f0-9]+ (feat|fix|perf|refactor|docs|test|chore|style)"
```

---

## 🤖 Generar Release Notes con IA Localmente

### 1. Preparar archivo de commits
```bash
# Extraer commits entre versiones
git log --pretty=format:"%H|%s|%b" main..HEAD > /tmp/commits.txt
```

### 2. Generar Release Notes
```bash
# Con IA (requiere OPENAI_API_KEY)
python scripts/generate-release-notes.py /tmp/commits.txt 1.2.0

# Con plantilla por defecto (sin IA)
python scripts/generate-release-notes.py /tmp/commits.txt 1.2.0 --fallback
```

### 3. Salida esperada
```markdown
## 📋 Release 1.2.0

### Resumen Ejecutivo
- Incluye 5 nuevas features
- 3 bug fixes críticos
- Mejoras de rendimiento del 25%

### ✨ Nuevas Características
- Dark mode support
- Habit reminders with Firebase
- ...
```

---

## 🔧 Troubleshooting

### El hook de commit no funciona
```bash
# Verificar que existe
ls -la .git/hooks/commit-msg

# Hacer ejecutable
chmod +x .git/hooks/commit-msg

# Probar manualmente
bash scripts/commit-msg "feat(test): this is a test"
```

### Build falla localmente pero pasa en CI
```bash
# Ejecutar clean completo
./gradlew clean

# Invalidar caché Gradle
rm -rf .gradle

# Reintentar
./gradlew build
```

### OpenAI API Key no funciona
```bash
# Verificar que está configurada
echo $OPENAI_API_KEY  # Linux/Mac
echo $env:OPENAI_API_KEY  # PowerShell

# Verificar validez de la key
python3 << 'EOF'
from openai import OpenAI
try:
    client = OpenAI()
    print("✅ OpenAI API Key válida")
except Exception as e:
    print(f"❌ Error: {e}")
EOF
```

### Commits están siendo rechazados por el hook
El script espera el formato Conventional Commits:

```bash
# ❌ INCORRECTO
git commit -m "fixed bug"

# ✅ CORRECTO
git commit -m "fix: resolve login issue"

# ✅ CON SCOPE (recomendado)
git commit -m "fix(auth): resolve email validation in login"
```

---

## 📊 Ver estado del Pipeline

### En GitHub
1. Accede a tu repositorio
2. Pestaña "Actions"
3. Selecciona "Pull Request Checks" o "Release Please CI/CD Pipeline"
4. Ve el progreso en tiempo real

### Con Git CLI
```bash
# Listar workflows
gh workflow list

# Ver últimas ejecuciones
gh run list --workflow release-please.yml

# Ver detalles de una ejecución
gh run view <run-id> --log
```

---

## 📚 Archivos de Configuración

| Archivo | Propósito |
|---------|-----------|
| `.github/workflows/pr-checks.yml` | Validación en PR |
| `.github/workflows/release-please.yml` | Release automático |
| `.github/RELEASE_PLEASE_GUIDE.md` | Documentación completa |
| `CONVENTIONAL_COMMITS.md` | Guía de commits |
| `scripts/commit-msg` | Hook de validación |
| `scripts/generate-release-notes.py` | Script de IA |

---

## 🎓 Mejores Prácticas

✅ **Valida antes de push:**
```bash
./gradlew clean build && git push
```

✅ **Commits frecuentes y atómicos:**
```bash
# ✅ Bueno: múltiples commits lógicos
git commit -m "feat(auth): add login UI"
git commit -m "test(auth): add login tests"

# ❌ Malo: un mega-commit
git commit -m "feat: add login and auth"
```

✅ **Escribe mensajes descriptivos:**
```bash
# ✅ Bueno
git commit -m "fix(home): resolve habit list not updating after toggle

When toggling a habit completion status, the UI would not reflect
the change immediately. Fixed by ensuring StateFlow emissions
trigger recomposition in Compose."

# ❌ Pobre
git commit -m "fix: bug fixes"
```

✅ **Sync frecuentemente:**
```bash
git fetch origin
git rebase origin/main
```

---

## 🔗 Enlaces Útiles

- [Release Please Documentation](https://github.com/googleapis/release-please)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [GitHub Actions Docs](https://docs.github.com/en/actions)
- [OpenAI API Documentation](https://platform.openai.com/docs)

---

## ❓ Preguntas Frecuentes

**P: ¿Por qué falla mi commit si el mensaje parece correcto?**
R: Revisa que el formato sea exactamente: `type(scope): message`. El script valida el patrón estrictamente.

**P: ¿Puedo hacer commits sin seguir Conventional Commits?**
R: El hook de git lo impedirá. Si necesitas forzar un commit (no recomendado):
```bash
git commit --no-verify -m "tu mensaje"
```

**P: ¿Qué pasa si mi rama local está desactualizada?**
R: Haz rebase antes de push:
```bash
git fetch origin
git rebase origin/main
```

**P: ¿Cómo veo qué cambios generarán una nueva versión?**
R: Usa `git log --oneline main..HEAD` para ver tus commits y determina si son feat, fix, etc.

---

**Última actualización:** Junio 2026  
**Versión:** 1.0.0

