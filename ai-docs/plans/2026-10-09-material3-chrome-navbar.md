# Material 3 chrome (NavigationBar + theme) Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Replace the custom bottom bar and hardcoded screen chrome with Material 3 `NavigationBar` (five equal items, Nuevo in the center, no FAB) and a complete navy `ColorScheme`.

**Architecture:** Presentation-only. `BottomNavigation` lists five bar items; four are destinations and `TabNew` only navigates to `TabAddContent`. Theme tokens live in `Color.kt` / `Theme.kt`. Screen `Scaffold` / `TopAppBar` / root backgrounds use `MaterialTheme.colorScheme.surface` (or `surfaceContainer` on the bar), not `HabitSurface` / `Color.White`. Habit cards, heatmap, `HabitField`, Glance widgets, and Login’s white form card stay as they are.

**Tech Stack:** Jetpack Compose Material 3 (BOM already `2026.06.01`), existing `InstaDevTheme`, Nunito, current `shapes`.

**Platform:** Android

**Do not commit unless the user asks.** Stage task files (`git add`). JAVA_HOME: `C:\Program Files\Android\Android Studio\jbr`. Gradle: `.\gradlew.bat --no-daemon`. Stay on branch `redesign`.

---

## File map

| Area | Paths |
|------|--------|
| Spec | `spec.md` § Material 3 chrome (already updated) |
| Nav model | `presentation/navigation/BottomNavigation.kt` |
| Bar UI | `presentation/screens/bottom_nav_screen/BottomNavScreen.kt` |
| Theme | `presentation/theme/Color.kt`, `Theme.kt` |
| Chrome screens | `HomeScreen.kt`, `StatsScreen.kt`, `ProfileScreen.kt`, `AddContentScreen.kt`, `PreferencesScreen.kt`, `DeleteAccountScreen.kt`, `RegisterScreen.kt`, `ForgotPasswordScreen.kt`, `ResetPasswordScreen.kt` |
| Already surface | `FriendsScreen.kt` root, `HabitDetailScreen.kt` tab/header — leave unless still on `HabitSurface` |
| Tests | `app/src/test/.../navigation/BottomNavigationTest.kt` |
| Previews | `BottomNavScreen` + theme previews in `Theme.kt` |

**Out of scope:** FAB / `HabitFab`, dynamic color, habit cards / heatmap / `HabitField`, Glance widgets, Login gradient + white card (`spec.md` View Login), chips, SearchBar, Expressive motion, `WideNavigationRail`, Compose Styles alpha, purple `#6750A4`.

**Order:** Task 1 (nav model TDD) → 2 (theme tokens) → 3 (`NavigationBar`) → 4 (screen chrome) → 5 (compile). Do not parallelize 3 with 1 (same `BottomNavigation`). Task 4 can start after 2.

---

### Task 1: BottomNavigation five items (TDD)

**Files:**
- Modify: `app/src/main/java/com/marta/habittracker/presentation/navigation/BottomNavigation.kt`
- Test: `app/src/test/java/com/marta/habittracker/presentation/navigation/BottomNavigationTest.kt`

- [ ] **Step 1: Failing tests**

```kotlin
package com.marta.habittracker.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomNavigationTest {

    @Test
    fun tabBarItems_haveNewInCenter() {
        val items = BottomNavigation.tabBarItems
        assertEquals(5, items.size)
        assertEquals(BottomNavigation.TabHome, items[0])
        assertEquals(BottomNavigation.TabStats, items[1])
        assertEquals(BottomNavigation.TabNew, items[2])
        assertEquals(BottomNavigation.TabFriends, items[3])
        assertEquals(BottomNavigation.TabProfile, items[4])
    }

    @Test
    fun destinationTabs_excludeNew() {
        val destinations = BottomNavigation.destinationTabs
        assertEquals(4, destinations.size)
        assertFalse(destinations.contains(BottomNavigation.TabNew))
        assertTrue(destinations.contains(BottomNavigation.TabHome))
        assertEquals(TabScreens.TabAddContent, BottomNavigation.TabNew.tabScreen)
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL** (`TabNew` / `tabBarItems` unresolved)

```
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest --tests "com.marta.habittracker.presentation.navigation.BottomNavigationTest" --no-daemon
```

- [ ] **Step 3: Minimal model**

Keep existing `TabHome` / `TabStats` / `TabFriends` / `TabProfile`. Add:

```kotlin
import androidx.compose.material.icons.outlined.Add

companion object {
    val destinationTabs = listOf(TabHome, TabStats, TabFriends, TabProfile)
    val tabBarItems = listOf(TabHome, TabStats, TabNew, TabFriends, TabProfile)
}

data object TabNew : BottomNavigation(
    label = R.string.tab_new,
    icon = Icons.Outlined.Add,
    tabScreen = TabScreens.TabAddContent,
)
```

Remove `tabBottomItemsList` **or** keep it as an alias of `destinationTabs` and update all call sites in Task 3. Prefer renaming to `destinationTabs` and fixing `BottomNavScreen` in Task 3 (Task 1 may leave a deprecated alias `tabBottomItemsList = destinationTabs` so the app still compiles).

- [ ] **Step 4: Re-run tests — expect PASS**

- [ ] **Step 5:** Stage. Do not commit unless asked.

---

### Task 2: Complete ColorScheme

**Files:**
- Modify: `app/src/main/java/com/marta/habittracker/presentation/theme/Color.kt`
- Modify: `app/src/main/java/com/marta/habittracker/presentation/theme/Theme.kt`

- [ ] **Step 1: Named container tokens in `Color.kt`** (keep existing `HabitPrimary` / `HabitSurface` / habit rainbow / `HabitSurfaceVariant` for **content** cards)

```kotlin
val HabitPrimaryContainer = Color(0xFFD6E3F8)
val HabitOnPrimaryContainer = Color(0xFF021C4B)
val HabitSecondaryContainer = Color(0xFFDCE2F0)
val HabitOnSecondaryContainer = Color(0xFF021C4B)
val HabitTertiary = Color(0xFF1D6B8A)
val HabitOnTertiary = Color.White
val HabitTertiaryContainer = Color(0xFFC5E7F5)
val HabitOnTertiaryContainer = Color(0xFF00344A)
val HabitErrorContainer = Color(0xFFFFDAD6)
val HabitOnErrorContainer = Color(0xFF410002)
val HabitSurfaceContainerLowest = Color(0xFFFFFFFF)
val HabitSurfaceContainerLow = Color(0xFFF1F1F1)
val HabitSurfaceContainer = Color(0xFFECECEC)
val HabitSurfaceContainerHigh = Color(0xFFE6E6E6)
val HabitSurfaceContainerHighest = Color(0xFFE0E0E0)

val HabitPrimaryContainerDark = Color(0xFF163A7A)
val HabitOnPrimaryContainerDark = Color(0xFFD6E3F8)
val HabitSurfaceContainerDark = Color(0xFF302A28)
val HabitSurfaceContainerLowestDark = Color(0xFF161210)
val HabitSurfaceContainerHighDark = Color(0xFF3A3330)
val HabitErrorContainerDark = Color(0xFF93000A)
val HabitOnErrorContainerDark = Color(0xFFFFDAD6)
```

Do **not** introduce `#6750A4`.

- [ ] **Step 2: Fill both schemes in `Theme.kt`**

`lightColorScheme` must set, in addition to existing primary/secondary/background/surface/error:

- `primaryContainer` / `onPrimaryContainer`
- `secondaryContainer` / `onSecondaryContainer`
- `tertiary` / `onTertiary` / `tertiaryContainer` / `onTertiaryContainer`
- `errorContainer` / `onErrorContainer`
- `surfaceContainerLowest` … `surfaceContainerHighest` (and `inverseSurface` / `inverseOnSurface` / `inversePrimary` if the compiler requires them; otherwise omit)

Keep `background` / `surface` = `HabitSurface`, `onSurface` = `HabitOnSurface`, `dynamicColor` default **false**. Empty `CompositionLocalProvider()` can stay or be removed (no behavior change).

Dark scheme: `primary = HabitPrimaryLight`, `surface` / `background` = `Gray20`, containers from the Dark tokens above, `onSurface` = `Color.White`.

- [ ] **Step 3: Theme previews** at the bottom of `Theme.kt`

```kotlin
@Preview(name = "Light chrome", showBackground = true)
@Composable
private fun LightThemePreview() {
    InstaDevTheme(darkTheme = false) {
        androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.surface) {
            androidx.compose.material3.Text("surface", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Preview(name = "Dark chrome", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DarkThemePreview() {
    InstaDevTheme(darkTheme = true) {
        androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.surface) {
            androidx.compose.material3.Text("surface", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
```

- [ ] **Step 4:** Stage. Do not commit unless asked.

---

### Task 3: Material `NavigationBar`

**Files:**
- Modify: `app/src/main/java/com/marta/habittracker/presentation/screens/bottom_nav_screen/BottomNavScreen.kt`

- [ ] **Step 1: Selection uses `destinationTabs` only**

Replace `tabBottomItemsList` with `BottomNavigation.destinationTabs`. `TabNew` is never `selected` while the bar is visible. Hide-bar routes unchanged (`TabAddContent`, `TabDetail`, `TabPreferences`, `TabDeleteAccount`).

- [ ] **Step 2: Replace `HabitBottomBar` / `BottomTabItem`**

Delete the custom `Row` / center `Box`. Use:

```kotlin
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme

@Composable
fun HabitBottomBar(
    selectedTab: BottomNavigation,
    onTabSelected: (BottomNavigation) -> Unit,
    onAdd: () -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        BottomNavigation.tabBarItems.forEach { tab ->
            val selected = tab != BottomNavigation.TabNew && tab == selectedTab
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (tab == BottomNavigation.TabNew) onAdd() else onTabSelected(tab)
                },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = stringResource(tab.label),
                    )
                },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}
```

`Scaffold.containerColor = MaterialTheme.colorScheme.surface`. Remove unused `HabitPrimary` / `Color.White` / `border` / `clickable without indication` from this file if they are only used by the old bar.

`onAdd` still `tabNavController.navigate(TabScreens.TabAddContent)`. Other tabs keep `popUpTo` + `saveState` + `launchSingleTop` + `restoreState`.

- [ ] **Step 3: Update `@Preview`** so five items and selected Home are visible (`showBackground = true`).

- [ ] **Step 4:** Stage. Do not commit unless asked.

---

### Task 4: Screen chrome tokens

**Files (chrome only — do not change habit cards, heatmap cells, `HabitField`, Profile menu row fills, Friends `Color.White` cards, Login white card, Glance):**

| File | Change |
|------|--------|
| `HomeScreen.kt` | `Scaffold.containerColor = MaterialTheme.colorScheme.surface` (keep `HomeHabitCard` / progress `Color.White`) |
| `StatsScreen.kt` | root `.background(MaterialTheme.colorScheme.surface)`; keep `HabitOnSurface` on titles if already used |
| `ProfileScreen.kt` | root Column `.background(MaterialTheme.colorScheme.surface)` only |
| `AddContentScreen.kt` | root Column `.background(MaterialTheme.colorScheme.surface)`; keep navy header `HabitPrimary` |
| `PreferencesScreen.kt` | `Scaffold` + `TopAppBar` `containerColor = MaterialTheme.colorScheme.surface` |
| `DeleteAccountScreen.kt` | same as Preferences |
| `RegisterScreen.kt` | `Scaffold` + `TopAppBar` `containerColor = MaterialTheme.colorScheme.surface`; leave `HabitSurfaceVariant` on inner fields |
| `ForgotPasswordScreen.kt` | `Scaffold(containerColor = MaterialTheme.colorScheme.surface)` + `TopAppBar` surface colors |
| `ResetPasswordScreen.kt` | `Scaffold { }` today has no `containerColor`; set `containerColor = MaterialTheme.colorScheme.surface` |

Skip `HabitDetailScreen` if already `colorScheme.surface`. Skip `FriendsScreen` root (already `colorScheme.surface`). Skip `LoginScreen` card and primary backdrop. Skip widgets.

- [ ] **Step 1:** Apply the table. Drop unused `HabitSurface` imports on those screens.

- [ ] **Step 2:** Stage. Do not commit unless asked.

---

### Task 5: Verification

- [ ] `.\gradlew.bat :app:compileDebugKotlin --no-daemon` — `BUILD SUCCESSFUL`
- [ ] `.\gradlew.bat :app:testDebugUnitTest --tests "com.marta.habittracker.presentation.navigation.BottomNavigationTest" --no-daemon` — PASS, 0 failures
- [ ] Manual (device/preview): five equal items, Nuevo center, tap Nuevo → Create and bar hidden; back → previous tab still selected; light/dark chrome uses scheme surfaces; habit cards still white/`HabitField`

---

## Spec coverage

| Requirement | Task |
|-------------|------|
| `NavigationBar` + 5 equal items, Nuevo center | 1, 3 |
| Nuevo not a persistent selected destination | 1, 3 |
| Hide bar on Create/Detail/Preferences/Delete | 3 (unchanged routes) |
| No FAB | 3 |
| Complete light/dark ColorScheme, navy brand, no `#6750A4` | 2 |
| Dynamic color off | 2 |
| Chrome scaffolds/top bars/screen backgrounds | 3, 4 |
| Content cards / heatmap / widgets / Login card untouched | 4 out-of-scope |
| Previews bar + theme | 2, 3 |
| No new ViewModel tests | 1 unit test on nav model only |

## Placeholder scan

None.
