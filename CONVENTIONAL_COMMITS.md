# Guía de Conventional Commits para HabitTracker

## 📋 Formato Estándar

```
<type>(<scope>): <subject>

<body>

<footer>
```

---

## 🎯 Ejemplos Reales para este Proyecto

### ✨ Feature Example
```
feat(home): add habit completion calendar view

- Implement calendar component showing completed habits by date
- Add LocalDate calculations for day-of-week filtering
- Integrate with HomeViewModel reactive flow

Closes #42
```

### 🐛 Fix Example
```
fix(auth): resolve email validation regex pattern

The previous EMAIL_ADDRESS pattern was too strict and rejected valid emails
with special characters. Updated to use Android Patterns.EMAIL_ADDRESS.

Fixes #51
```

### ⚡ Performance Example
```
perf(database): optimize habit query with indexed search

- Add database index on habit_id column
- Reduce database queries in HomeViewModel from 3 to 1
- Benchmark shows 40% faster load times

Related to #38
```

### 💥 Breaking Change Example
```
feat(api): redesign habit creation API

BREAKING CHANGE: The old createHabit(name, frequency) method is removed.
Use createHabit(habitRequest: HabitRequest) instead. See migration guide.

Old API:
  habitRepository.createHabit("Read", 7)

New API:
  habitRepository.createHabit(HabitRequest(
    name = "Read",
    frequencyDays = listOf(1, 2, 3, 4, 5, 6, 7)
  ))

Closes #67
```

### 🧪 Test Example
```
test(repository): add unit tests for habit DAO operations

- Test getHabitsWithStatus with various dates
- Test toggleHabitCompletion edge cases
- Verify database transaction rollback

Coverage increased from 62% to 78%
```

### 📚 Documentation Example
```
docs(readme): add setup instructions for development

- Add environment setup guide
- Include keystore configuration
- Add troubleshooting section

See: SETUP.md
```

### ♻️ Refactor Example
```
refactor(viewmodel): simplify HomeViewModel state management

- Replace manual Flow subscriptions with StateFlow
- Use flatMapLatest instead of switchMap
- Remove redundant date formatting logic

No functional changes, improves maintainability.
```

---

## 📝 Guía de Escritura

### Tipo (type)
Debe ser uno de:
- **feat**: Nueva funcionalidad
- **fix**: Corrección de bug
- **perf**: Mejora de rendimiento
- **refactor**: Cambio de código sin cambiar funcionalidad
- **test**: Añadir o actualizar tests
- **docs**: Cambios en documentación
- **style**: Cambios de formato (sin lógica)
- **chore**: Cambios en build, CI, deps

### Scope (scope)
Área del código afectada:
- `auth` - Sistema de autenticación
- `home` - Home screen y funcionalidades
- `detail` - Detail screen
- `database` - Room DB
- `api` - Retrofit y network
- `theme` - Compose theme y UI
- `navigation` - Navigation logic
- `di` - Dependency Injection

### Subject (sujeto)
- Máximo 50 caracteres
- Imperativo: "add" no "added" o "adds"
- Minúsculas excepto nombres propios
- Sin punto al final

### Body (cuerpo)
- Explica QUÉ cambió y POR QUÉ
- Máximo 100 caracteres por línea
- Separado del subject por línea en blanco
- Usa viñetas para múltiples cambios

### Footer (pie de página)
- Referencia a issues: `Closes #123`, `Fixes #456`
- Breaking changes: `BREAKING CHANGE: description`

---

## ❌ Ejemplos INCORRECTOS

```
❌ feat: fixed bug in home screen
   (Debería ser "fix", no "feat")

❌ refactor(auth): many improvements
   (Demasiado genérico, proporciona detalles)

❌ chore: big refactor and feature addition
   (Combina múltiples cambios, separa en commits)

❌ docs(readme): README.
   (Sujeto demasiado corto)

❌ feat: Add new UI components and fix bugs and update database
   (Demasiados cambios en un commit)
```

---

## ✅ Consejos para Mejores Commits

1. **Un commit = un cambio lógico**: No combines features, fixes y refactors
2. **Commits atómicos**: Cada commit debe compilar y pasar tests
3. **Frecuentes pero lógicos**: No hagas commits cada línea, pero tampoco esperes a terminar todo
4. **Escribe en presente imperativo**: "add feature" no "added feature"
5. **Proporciona contexto**: Explica por qué cambio, no solo qué cambió
6. **Referencia issues**: Usa `Closes #123` para cerrar issues automáticamente

---

## 🔄 Workflow Típico

```bash
# 1. Crea rama
git checkout -b feat/add-habit-reminders

# 2. Realiza cambios
# Edita archivos...

# 3. Commit con Conventional Commits
git add .
git commit -m "feat(home): add notification reminders for habits

- Integrate Firebase Cloud Messaging for push notifications
- Add reminder scheduling based on habit frequency
- Add notification preferences in Profile screen

Closes #89"

# 4. Haz más commits si hay más cambios lógicos
git commit -m "test(home): add tests for reminder notification logic"

# 5. Push a rama
git push origin feat/add-habit-reminders

# 6. Crea PR en GitHub
# El mensaje de PR puede ser el mismo del commit

# 7. Merge a main (Release Please detectará y creará Release PR)
```

---

## 🚀 Integración con Release Please

El pipeline `release-please.yml` automáticamente:
1. Lee tus commits Conventional
2. Determina próxima versión (MAJOR.MINOR.PATCH)
3. Genera CHANGELOG.md
4. Crea Release PR

**Ejemplo automático:**
```
Tu commit: feat(auth): add oauth support
Release Please: MINOR bump → v1.1.0

Tu commit: fix: resolve null pointer
Release Please: PATCH bump → v1.0.1

Tu commit: BREAKING CHANGE: ...
Release Please: MAJOR bump → v2.0.0
```

---

## 📚 Recursos

- [Conventional Commits](https://www.conventionalcommits.org/)
- [Angular Commit Guidelines](https://github.com/angular/angular/blob/master/CONTRIBUTING.md#-commit-message-guidelines)
- [Semantic Versioning](https://semver.org/)


