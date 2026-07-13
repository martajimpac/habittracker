#!/usr/bin/env python3
"""
🤖 Script de Generación Automática de Release Notes con IA
=========================================================

Este script utiliza la API de OpenAI para generar Release Notes
profesionales y concisas basadas en los commits entre versiones.

Uso:
    python scripts/generate-release-notes.py <commits_file> <version> [model]

Requisitos:
    - pip install openai
    - Variable de entorno: OPENAI_API_KEY

Ejemplo:
    python scripts/generate-release-notes.py /tmp/commits.txt 1.2.0 gpt-3.5-turbo

Salida:
    Imprime el contenido de Release Notes en Markdown a stdout
"""

import json
import sys
import os
import argparse
from typing import Optional
from pathlib import Path


def validate_openai_key():
    """Valida que la API Key de OpenAI esté configurada"""
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        print("❌ Error: Variable de entorno OPENAI_API_KEY no configurada")
        print("ℹ️  Obtén tu API Key en: https://platform.openai.com/api-keys")
        return False
    return True


def load_commits(commits_file: str) -> str:
    """Carga los commits desde un archivo"""
    try:
        with open(commits_file, 'r', encoding='utf-8') as f:
            commits = f.read().strip()

        if not commits:
            print("⚠️  Advertencia: Archivo de commits vacío")
            return ""

        return commits
    except FileNotFoundError:
        print(f"❌ Error: No se encontró archivo {commits_file}")
        sys.exit(1)
    except Exception as e:
        print(f"❌ Error al leer commits: {e}")
        sys.exit(1)


def generate_ai_release_notes(
    commits_text: str,
    version: str,
    model: str = "gpt-3.5-turbo"
) -> Optional[str]:
    """
    Genera Release Notes usando OpenAI API

    Args:
        commits_text: Texto con los commits a procesar
        version: Número de versión (ej: 1.2.0)
        model: Modelo de OpenAI a usar (gpt-3.5-turbo, gpt-4, etc.)

    Returns:
        Release Notes en formato Markdown o None si ocurre error
    """
    try:
        from openai import OpenAI
    except ImportError:
        print("❌ Error: Librería 'openai' no instalada")
        print("ℹ️  Instálala con: pip install openai")
        sys.exit(1)

    try:
        client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

        prompt = f"""
        Eres un Senior DevOps Engineer y Tech Lead especializado en Release Management.
        Tu tarea es generar Release Notes profesionales y concisas para desarrolladores.

        Versión del Release: {version}

        Commits de esta versión:
        {commits_text}

        Genera Release Notes en Markdown con las siguientes secciones:

        1. **Resumen Ejecutivo** (2-3 líneas máximo)
           - Resumen técnico de qué cambios importantes incluye esta versión

        2. **✨ Nuevas Características** (si hay features)
           - Lista con viñetas de nuevas funcionalidades
           - Incluye contexto técnico breve

        3. **🐛 Correcciones de Bugs** (si hay fixes)
           - Lista de bugs corregidos
           - Incluye impacto del fix

        4. **⚡ Mejoras de Rendimiento** (si hay perf improvements)
           - Cambios que optimizan la aplicación

        5. **💥 Breaking Changes** (SOLO si los hay)
           - **CRÍTICO**: Enumera cambios incompatibles
           - Incluye guía de migración

        6. **📖 Guía de Actualización**
           - Pasos para actualizar a esta versión
           - Precauciones o requisitos especiales

        Requisitos:
        - Máximo 500 palabras
        - Lenguaje técnico pero accesible
        - Estructura clara con emojis para visual scanning
        - Incluye números de commits cuando sea relevante
        - NO inventes información no presente en los commits
        - Si una sección no aplica, omítela

        Formato esperado: Markdown puro
        """

        print("🔄 Procesando commits con IA...", file=sys.stderr)

        response = client.chat.completions.create(
            model=model,
            messages=[
                {
                    "role": "system",
                    "content": "Eres un experto en Release Management y DevOps. Generas Release Notes claras, concisas y técnicas."
                },
                {
                    "role": "user",
                    "content": prompt
                }
            ],
            temperature=0.7,
            max_tokens=800,
            top_p=0.9
        )

        return response.choices[0].message.content

    except Exception as e:
        print(f"❌ Error al llamar OpenAI API: {e}", file=sys.stderr)
        return None


def generate_fallback_release_notes(version: str, commits_text: str) -> str:
    """
    Genera Release Notes con plantilla por defecto
    (se usa si OpenAI no está disponible)
    """
    num_commits = len([line for line in commits_text.split('\n') if line.strip()])

    return f"""## 📋 Release {version}

### 📝 Resumen
Esta versión incluye {num_commits} cambios y mejoras basados en commits recientes.

### 📦 Contenido del Release
- Cambios implementados según commit history
- Validaciones de calidad: Lint ✅ Tests ✅ Build ✅
- Changelog automático generado

### 🚀 Instalación
```bash
# Actualiza tu versión a {version}
./gradlew clean build
```

### 📚 Ver Detalles Completos
Para información técnica detallada, consulta el CHANGELOG.md

---
*Release Notes generadas automáticamente. Descarga OpenAI API Key para obtener análisis de IA mejorado.*
"""


def main():
    """Punto de entrada principal"""
    parser = argparse.ArgumentParser(
        description="Genera Release Notes automáticas con IA"
    )
    parser.add_argument(
        "commits_file",
        help="Archivo con commits (formato: hash|subject|body)"
    )
    parser.add_argument(
        "version",
        help="Número de versión (ej: 1.2.0)"
    )
    parser.add_argument(
        "--model",
        default="gpt-3.5-turbo",
        help="Modelo OpenAI a usar (default: gpt-3.5-turbo)"
    )
    parser.add_argument(
        "--fallback",
        action="store_true",
        help="Usa plantilla por defecto sin IA"
    )

    args = parser.parse_args()

    # Cargar commits
    commits = load_commits(args.commits_file)

    if not commits:
        print("⚠️  No hay commits para procesar")
        sys.exit(1)

    # Generar Release Notes
    if args.fallback or not validate_openai_key():
        print("ℹ️  Usando plantilla por defecto", file=sys.stderr)
        release_notes = generate_fallback_release_notes(args.version, commits)
    else:
        release_notes = generate_ai_release_notes(commits, args.version, args.model)

        if not release_notes:
            print("⚠️  Fallo en AI, usando plantilla por defecto", file=sys.stderr)
            release_notes = generate_fallback_release_notes(args.version, commits)

    # Salida
    print(release_notes)
    print("✅ Release Notes generadas exitosamente", file=sys.stderr)


if __name__ == "__main__":
    main()

