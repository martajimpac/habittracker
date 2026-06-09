# 🚀 Guía de Implementación: Release Please CI/CD Pipeline

## 📋 Descripción General

Este documento proporciona la guía de implementación para el flujo de **Integración Continua y Entrega Continua (CI/CD)** con **Release Please**, siguiendo el estándar de **Conventional Commits** y con integración de **IA para generar Release Notes automáticas**.

---

## 🏗️ Arquitectura del Pipeline

El pipeline se divide en **5 fases** ejecutadas secuencialmente:

```
┌─────────────────────────────────────────────────────────────┐
│ FASE 1: Validación de Código (CI Mínimo Viable)            │
│ └─ Lint Check → Unit Tests → Build Validation              │
├─────────────────────────────────────────────────────────────┤
│ FASE 2: Release Please (Versionado Automático)             │
│ └─ Detecta commits, crea Release PR, aplica SemVer        │
├─────────────────────────────────────────────────────────────┤
│ FASE 3: Generación de Release Notes con IA                 │
│ └─ Procesa commits, genera resumen inteligente             │
├─────────────────────────────────────────────────────────────┤
│ FASE 4: Construcción de Release                            │
│ └─ Compila APK y Bundle de producción                      │
├─────────────────────────────────────────────────────────────┤
│ FASE 5: Notificación y Resumen                             │
│ └─ Genera reporte final del release                        │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 Requisitos Previos

### 1. Instalación Local
```bash
# Asegúrate de tener instalado:
- JDK 11+
- Android SDK (API 30+)
- Gradle 7.0+
```

### 2. Configuración de GitHub Secrets

Accede a **GitHub Repo → Settings → Secrets and variables → Actions** y configura:

#### Secreto Obligatorio:
```
GITHUB_TOKEN (se crea automáticamente en GitHub Actions)
```

#### Secreto Opcional (para AI Release Notes):
```
OPENAI_API_KEY: "sk-..." (obtén desde https://platform.openai.com/api-keys)
```

---

## 🎯 Flujo de Conventional Commits

El pipeline está configurado para detectar y procesar los siguientes tipos de commits:

| Tipo    | Descripción                          | Ejemplo                          |
|---------|--------------------------------------|----------------------------------|
| `feat`  | Nueva característica                 | `feat: add dark mode support`    |
| `fix`   | Corrección de bug                    | `fix: resolve memory leak`       |
| `perf`  | Mejora de rendimiento                | `perf: optimize image loading`   |
| `refactor` | Refactorización del código          | `refactor: simplify auth logic`  |
| `docs`  | Cambios en documentación             | `docs: update README`            |
| `test`  | Añadir o actualizar tests            | `test: add unit tests for DAO`   |
| `chore` | Cambios de build, CI, deps           | `chore: update dependencies`     |
| `style` | Formato, espacios en blanco          | `style: format code`             |

### Breaking Changes
Para indicar cambios incompatibles, añade `BREAKING CHANGE:` en el body del commit:

```
feat: redesign authentication system

BREAKING CHANGE: The old auth API is deprecated. Use the new AuthService instead.
```

---

## 🔄 Flujo de Release Automático

### ¿Cómo funciona Release Please?

1. **Monitorea commits** en la rama `main` con Conventional Commits
2. **Crea un Release PR** automáticamente con:
   - Versión actualizada (SemVer)
   - Changelog generado
   - Historial de commits organizados
3. **Al hacer merge** del Release PR:
   - Se crea una GitHub Release
   - Se genera tag con la versión
   - Se ejecutan jobs de construcción
4. **Notifica** con resumen y artefactos

### Versioning Semántico (SemVer)

```
MAJOR.MINOR.PATCH
└──┬───┘ └─┬──┘ └┬─┘
   │      │      └─ PATCH: fix, chore, style
   │      └──────── MINOR: feat, refactor, perf
   └─────────────── MAJOR: BREAKING CHANGE
```

**Ejemplos:**
- `fix: bug` → v1.0.1 (PATCH)
- `feat: new feature` → v1.1.0 (MINOR)
- `BREAKING CHANGE:` → v2.0.0 (MAJOR)

---

## 🤖 Integración de IA para Release Notes

### Configuración Actual (Placeholder)

El pipeline incluye un job `generate-ai-release-notes` que:
- ✅ Extrae commits entre versiones
- ✅ Prepara datos para procesamiento de IA
- ⚠️ Actualmente usa una plantilla por defecto

### Habilitar OpenAI Integration

Para activar la generación automática de Release Notes con IA:

#### Paso 1: Obtén tu API Key
1. Accede a https://platform.openai.com/api-keys
2. Crea una nueva API Key
3. Copia el valor (comienza con `sk-`)

#### Paso 2: Configura el Secret en GitHub
```
Repo → Settings → Secrets and variables → Actions
Click "New repository secret"
Name: OPENAI_API_KEY
Value: sk-... (tu API Key)
```

#### Paso 3: Implementa el Script de Integración

Crea el archivo `scripts/generate-release-notes.py`:

```python
#!/usr/bin/env python3
"""
Script para generar Release Notes automáticas con IA (OpenAI)
Uso: python generate-release-notes.py <commits_file> <version>
"""

import json
import sys
import os
from openai import OpenAI

def generate_ai_release_notes(commits_text, version):
    """
    Genera release notes usando OpenAI API
    """
    client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))
    
    prompt = f"""
    Eres un ingeniero senior de software. Analiza los siguientes commits 
    y genera Release Notes profesionales y concisas (máximo 300 palabras).
    
    Versión: {version}
    
    Commits:
    {commits_text}
    
    Genera:
    1. Resumen técnico (2-3 líneas)
    2. Features principales
    3. Bug fixes importantes
    4. Breaking changes (si hay)
    5. Notas de migración (si aplica)
    
    Formato: Markdown
    """
    
    response = client.chat.completions.create(
        model="gpt-3.5-turbo",
        messages=[
            {"role": "system", "content": "Eres un senior DevOps engineer experto en release management."},
            {"role": "user", "content": prompt}
        ],
        temperature=0.7,
        max_tokens=500
    )
    
    return response.choices[0].message.content

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python generate-release-notes.py <commits_file> [version]")
        sys.exit(1)
    
    commits_file = sys.argv[1]
    version = sys.argv[2] if len(sys.argv) > 2 else "1.0.0"
    
    with open(commits_file, 'r') as f:
        commits = f.read()
    
    notes = generate_ai_release_notes(commits, version)
    print(notes)
```

---

## 📚 Comandos Locales Equivalentes

Antes de hacer push, ejecuta localmente:

```bash
# Validar código (igual que CI/CD)
./gradlew lint test assembleDebug

# Ver commits no pusheados
git log --oneline origin/main..HEAD

# Validar formato de commits (Conventional Commits)
git log --format=%B origin/main..HEAD | grep -E "^(feat|fix|perf|refactor|docs|test|chore|style):"

# Simular Release Please localmente
gh release view --repo yourusername/HabitTracker
```

---

## 🔧 Configuración Avanzada

### Modificar tipos de changelog

En `release-please.yml`, ajusta la sección `changelog-types`:

```yaml
changelog-types: |
  [
    {"type": "feat", "section": "✨ Features", "hidden": false},
    {"type": "custom-type", "section": "📌 Custom Section", "hidden": false}
  ]
```

### Cambiar rama de release

Para usar otra rama en lugar de `main`:

```yaml
on:
  push:
    branches:
      - main        # Cambiar a: develop, production, etc.
```

### Habilitar release automático a Google Play

Añade un nuevo job después de `build-release`:

```yaml
deploy-play-store:
  name: 🚀 Deploy to Google Play
  runs-on: ubuntu-latest
  needs: build-release
  if: needs.release-please.outputs.release_created == 'true'
  steps:
    - uses: actions/checkout@v4
    - uses: r0adkll/upload-google-play@v1
      with:
        serviceAccountJsonPlainText: ${{ secrets.PLAY_STORE_KEY }}
        packageName: com.aristidevs.habittracker
        releaseFiles: app/build/outputs/bundle/release/app-release.aab
        releaseName: ${{ needs.release-please.outputs.version }}
        track: production
```

---

## ✅ Verificación Post-Configuración

Después de commitear el archivo `release-please.yml`:

1. **Accede a GitHub → Actions** y verifica que aparezca "Release Please CI/CD Pipeline"
2. **Haz un commit en `main`** con mensaje: `feat: test release pipeline`
3. **Observa el pipeline**:
   - ✅ CI checks deben completarse
   - ✅ Release Please debe crear un PR
   - ⏳ Espera a que se complete el análisis

4. **Revisa el Release PR**:
   - Versión actualizada en `build.gradle.kts`
   - Changelog actualizado
   - Commits organizados por tipo

5. **Haz merge** del Release PR para disparar la construcción final

---

## 🐛 Troubleshooting

| Problema | Solución |
|----------|----------|
| Release Please no crea PR | Verifica commits sigan Conventional Commits |
| AI Release Notes no se generan | Comprueba que `OPENAI_API_KEY` esté en Secrets |
| Build fallosusa en release | Ejecuta `./gradlew clean build` localmente |
| Permisos insuficientes | Verifica `permissions` en el YAML |

---

## 📖 Recursos Adicionales

- [Release Please Documentation](https://github.com/googleapis/release-please)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Semantic Versioning](https://semver.org/)
- [GitHub Actions Security](https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions)
- [OpenAI API Documentation](https://platform.openai.com/docs)

---

## 🎓 Mejores Prácticas

✅ **Commits claros**: Usa Conventional Commits en cada commit  
✅ **Review PRs**: Revisa Release PR antes de hacer merge  
✅ **Tag protection**: Protege la rama `main` con branch rules  
✅ **Changelog review**: Verifica changelog generado antes de liberar  
✅ **Testing local**: Valida con `./gradlew test` antes de push  
✅ **Versioning**: Respeta SemVer para cambios API  

---

**Última actualización:** Junio 2026  
**Versión del Pipeline:** 1.0.0

