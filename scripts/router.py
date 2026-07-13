import re

# Definición de la Skill de Android Nativo
ANDROID_SKILL = {
    "name": "Android Native Expert",
    "path": "skills/android-expert.md",
    "patterns": [
        r".*\.kt$",                 # Archivos Kotlin
        r".*\.xml$",                # Layouts y recursos
        r"build\.gradle.*",         # Configuración de Gradle
        r"AndroidManifest\.xml",    # Manifiesto
        r"(?i)compose|jetpack",     # UI moderna
        r"(?i)viewmodel|livedata"   # Arquitectura
    ]
}

def route_skills(input_context):
    """
    input_context: Puede ser el nombre del archivo o el prompt del usuario.
    """
    for pattern in ANDROID_SKILL["patterns"]:
        if re.search(pattern, input_context):
            print(f"Cargando skill: {ANDROID_SKILL['name']}")
            return ANDROID_SKILL["path"]

    return "agent.md" # Fallback a reglas generales si no hay coincidencia

# Ejemplo de uso:
# skill_to_load = route_skills("MainActivity.kt")
# -> Retorna "skills/android-expert.md"