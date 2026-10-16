# Client IDs UUIDv7 Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Generate all new client IDs as UUIDv7 via a central helper; leave existing persisted v4 IDs untouched.

**Architecture:** Add `IdGenerator.newId()` in `core` wrapping `kotlin.uuid.Uuid.generateV7().toString()` with `@file:OptIn(ExperimentalUuidApi::class)`. Replace every `UUID.randomUUID().toString()` used as an entity ID. No schema migration, no new Gradle dependency.

**Tech Stack:** Kotlin 2.4.10 stdlib `kotlin.uuid`, JUnit unit tests, existing Clean Architecture layers.

**Platform:** Android

**Approved design:** 2026-08-11 (spec + brain)

---

## File map

| Action | Path |
|--------|------|
| Create | `app/src/main/java/com/marta/habittracker/core/IdGenerator.kt` |
| Create | `app/src/test/java/com/marta/habittracker/core/IdGeneratorTest.kt` |
| Modify | `app/src/main/java/com/marta/habittracker/domain/usecase/SaveHabit.kt` |
| Modify | `app/src/main/java/com/marta/habittracker/data/repository/HabitRepositoryImpl.kt` |
| Modify | `app/src/main/java/com/marta/habittracker/data/mapper/HabitMapper.kt` |
| Modify | `app/src/main/java/com/marta/habittracker/data/remote/SocialRemoteDataSource.kt` |
| Already done | `spec.md`, `.mobiai/brain/memories/decisions.md` |

**Out of scope:** Migrating existing rows; Postgres defaults; changing column types; Profile/auth user ids from Supabase.

---

### Task 1: IdGenerator + unit tests (TDD)

**Files:**
- Create: `app/src/test/java/com/marta/habittracker/core/IdGeneratorTest.kt`
- Create: `app/src/main/java/com/marta/habittracker/core/IdGenerator.kt`

- [x] **Step 1: Write the failing test**

```kotlin
package com.marta.habittracker.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class IdGeneratorTest {

    @Test
    fun `newId returns a parseable UUID with version 7`() {
        val id = IdGenerator.newId()
        val uuid = UUID.fromString(id)
        assertEquals(7, uuid.version())
    }

    @Test
    fun `newId returns distinct values`() {
        assertNotEquals(IdGenerator.newId(), IdGenerator.newId())
    }

    @Test
    fun `newId matches UUID string shape`() {
        val id = IdGenerator.newId()
        assertTrue(
            id.matches(
                Regex("^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"),
            ),
        )
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.marta.habittracker.core.IdGeneratorTest"`

Expected: FAIL (unresolved `IdGenerator`)

- [x] **Step 3: Write minimal implementation**

```kotlin
@file:OptIn(ExperimentalUuidApi::class)

package com.marta.habittracker.core

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object IdGenerator {
    fun newId(): String = Uuid.generateV7().toString()
}
```

- [x] **Step 4: Run tests and ensure they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.marta.habittracker.core.IdGeneratorTest"`

Expected: PASS

- [x] **Step 5: Stage (do not commit unless user asks)**

```bash
git add -- app/src/main/java/com/marta/habittracker/core/IdGenerator.kt app/src/test/java/com/marta/habittracker/core/IdGeneratorTest.kt
```

---

### Task 2: Replace habit / record ID generation

**Files:**
- Modify: `app/src/main/java/com/marta/habittracker/domain/usecase/SaveHabit.kt`
- Modify: `app/src/main/java/com/marta/habittracker/data/repository/HabitRepositoryImpl.kt`
- Modify: `app/src/main/java/com/marta/habittracker/data/mapper/HabitMapper.kt`

- [x] **Step 1: SaveHabit** — replace `import java.util.UUID` and `UUID.randomUUID().toString()` with:

```kotlin
import com.marta.habittracker.core.IdGenerator
// ...
val habitId = id ?: IdGenerator.newId()
```

- [x] **Step 2: HabitRepositoryImpl** — same for new `HabitRecordEntity` id:

```kotlin
import com.marta.habittracker.core.IdGenerator
// ...
id = IdGenerator.newId(),
```

Remove unused `java.util.UUID` import if no longer referenced.

- [x] **Step 3: HabitMapper** — blank-id fallback:

```kotlin
import com.marta.habittracker.core.IdGenerator
// ...
id: String = this.id.ifBlank { IdGenerator.newId() },
```

- [x] **Step 4: Compile**

Run: `./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL

- [x] **Step 5: Stage touched files**

---

### Task 3: Replace social ID generation

**Files:**
- Modify: `app/src/main/java/com/marta/habittracker/data/remote/SocialRemoteDataSource.kt`

- [x] **Step 1:** Replace both `UUID.randomUUID().toString()` call sites (friend request insert ~line 106 and challenge insert ~line 185) with `IdGenerator.newId()`.

- [x] **Step 2:** Remove `import java.util.UUID` if unused; add `import com.marta.habittracker.core.IdGenerator`.

- [x] **Step 3: Compile**

Run: `./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL

- [x] **Step 4: Stage**

---

### Task 4: Repo-wide guard + verification

**Files:** none new (search + tests)

- [x] **Step 1: Guard** — ensure no remaining client ID generation via v4:

```bash
rg "UUID\.randomUUID" app/src/main
```

Expected: **zero** matches (or only non-ID uses if any; if found for IDs, fix before continuing).

- [x] **Step 2: Unit tests**

Run: `./gradlew :app:testDebugUnitTest --rerun-tasks`

Expected: BUILD SUCCESSFUL, 0 failures

- [x] **Step 3: Stage all task files** including plan + already-updated spec/brain:

```bash
git add -- AGENTS.md spec.md .mobiai app/src/main app/src/test docs/plans/2026-08-11-client-ids-uuidv7.md
git status --short
```

- [x] **Step 4:** Mark plan checkboxes complete. Do **not** commit unless the user asks.

---

## Dependency graph

```
Task1 → Task2 → Task3 → Task4
```

Tasks 2 and 3 could parallelize after Task 1, but both are tiny; prefer sequential to avoid import churn conflicts on shared patterns.

## Spec coverage

| Spec requirement | Task |
|------------------|------|
| New client IDs are UUIDv7 String | 1–3 |
| Central helper + no `UUID.randomUUID` for IDs | 1, 4 |
| No migration of existing IDs | (no migration task — intentional) |
| No Postgres schema change | (out of scope) |
