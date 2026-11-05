# Widget picker + Glance previews Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Give the three existing Glance widgets a home-screen picker thumbnail (`previewLayout`) and an Android Studio `@GlancePreview`, without adding a fourth widget.

**Architecture:** Static XML layouts in `res/layout` are wired from each `appwidget-provider`. Glance UI composables become `internal` so `src/debug` preview functions can call them with sample data and a no-op `Action`. Preview-only Gradle artifacts stay on `debugImplementation`.

**Tech Stack:** Jetpack Glance 1.1.1, `glance-preview`, `glance-appwidget-preview`, App Widget `previewLayout`

**Platform:** Android

---

## File map

| Area | Paths |
|------|--------|
| Catalog / Gradle | `gradle/libs.versions.toml`, `app/build.gradle.kts` |
| Picker XML | `app/src/main/res/layout/habit_widget_preview.xml`, `challenge_widget_preview.xml`, `weekly_widget_preview.xml` |
| Providers | `app/src/main/res/xml/habit_widget_info.xml`, `challenge_widget_info.xml`, `weekly_widget_info.xml` |
| Strings | `app/src/main/res/values/strings.xml`, `values-es/strings.xml` |
| Glance content | `HabitGlanceWidget.kt`, `ChallengeGlanceWidget.kt`, `WeeklyGlanceWidget.kt` (`private` → `internal` content) |
| Studio previews | `app/src/debug/java/com/marta/habittracker/presentation/widgets/**/*Preview.kt` |
| Tests | `app/src/test/java/com/marta/habittracker/presentation/widgets/WidgetPreviewLayoutTest.kt` |

**Out of scope:** fourth widget, config activity changes, widget sizes, mutation behavior.

---

### Task 1: Failing test that providers declare previewLayout

**Files:**
- Create: `app/src/test/java/com/marta/habittracker/presentation/widgets/WidgetPreviewLayoutTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.marta.habittracker.presentation.widgets

import android.content.Context
import android.view.LayoutInflater
import androidx.test.core.app.ApplicationProvider
import com.marta.habittracker.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
class WidgetPreviewLayoutTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `habit challenge and weekly providers declare previewLayout`() {
        assertEquals(
            "@layout/habit_widget_preview",
            previewLayoutAttr(R.xml.habit_widget_info),
        )
        assertEquals(
            "@layout/challenge_widget_preview",
            previewLayoutAttr(R.xml.challenge_widget_info),
        )
        assertEquals(
            "@layout/weekly_widget_preview",
            previewLayoutAttr(R.xml.weekly_widget_info),
        )
    }

    @Test
    fun `preview layouts inflate`() {
        val inflater = LayoutInflater.from(context)
        assertNotNull(inflater.inflate(R.layout.habit_widget_preview, null))
        assertNotNull(inflater.inflate(R.layout.challenge_widget_preview, null))
        assertNotNull(inflater.inflate(R.layout.weekly_widget_preview, null))
    }

    private fun previewLayoutAttr(xmlRes: Int): String? {
        val parser = context.resources.getXml(xmlRes)
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG &&
                parser.name == "appwidget-provider"
            ) {
                return parser.getAttributeValue(
                    "http://schemas.android.com/apk/res/android",
                    "previewLayout",
                )
            }
        }
        return null
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:testDebugUnitTest --tests "com.marta.habittracker.presentation.widgets.WidgetPreviewLayoutTest" --no-daemon`

Expected: FAIL (missing `previewLayout` and/or `R.layout.*_widget_preview`)

- [ ] **Step 3: Add string resources for sample preview copy**

Modify: `app/src/main/res/values/strings.xml` and `values-es/strings.xml` (after existing widget strings)

English:

```xml
<string name="habit_widget_preview_name">Read</string>
<string name="challenge_widget_preview_habit">Run</string>
<string name="challenge_widget_preview_versus">vs Alex</string>
<string name="challenge_widget_preview_progress">You 60% · Alex 40%</string>
<string name="challenge_widget_preview_days_left">5 days left</string>
```

Spanish:

```xml
<string name="habit_widget_preview_name">Leer</string>
<string name="challenge_widget_preview_habit">Correr</string>
<string name="challenge_widget_preview_versus">vs Alex</string>
<string name="challenge_widget_preview_progress">Tú 60% · Alex 40%</string>
<string name="challenge_widget_preview_days_left">5 días restantes</string>
```

Reuse existing `habit_widget_completed`, `habit_widget_mark_complete`, `weekly_widget_title`, `weekly_widget_day_*`, `weekly_widget_percent` in XML via `@string/`.

- [ ] **Step 4: Add the three preview layouts**

Create `app/src/main/res/layout/habit_widget_preview.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#FFF1F1F1"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/habit_widget_preview_name"
        android:textColor="#FFF04F45"
        android:textSize="16sp"
        android:textStyle="bold" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="4dp"
        android:text="@string/habit_widget_completed"
        android:textColor="#FF1A1D1F"
        android:textSize="12sp" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:background="#FF021C4B"
        android:paddingHorizontal="12dp"
        android:paddingVertical="6dp"
        android:text="@string/habit_widget_mark_complete"
        android:textColor="#FFFFFFFF"
        android:textSize="12sp" />
</LinearLayout>
```

Create `app/src/main/res/layout/challenge_widget_preview.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#1FF04F45"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/challenge_widget_preview_habit"
        android:textColor="#FFF04F45"
        android:textSize="16sp"
        android:textStyle="bold" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="4dp"
        android:text="@string/challenge_widget_preview_versus"
        android:textColor="#FF1A1D1F"
        android:textSize="12sp" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="4dp"
        android:text="@string/challenge_widget_preview_progress"
        android:textColor="#FF1A1D1F"
        android:textSize="12sp" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="4dp"
        android:text="@string/challenge_widget_preview_days_left"
        android:textColor="#FF1A1D1F"
        android:textSize="12sp" />
</LinearLayout>
```

Create `app/src/main/res/layout/weekly_widget_preview.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#FFF1F1F1"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="12dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/weekly_widget_title"
        android:textColor="#FF1A1D1F"
        android:textSize="14sp"
        android:textStyle="bold" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:gravity="center"
        android:orientation="horizontal">

        <!-- seven children: Mon..Sun using weekly_widget_day_* and a sample percent TextView each -->
        <!-- Implementation: seven vertical LinearLayouts with day label + "80%" style sample; Sat/Sun can use "0%" -->
    </LinearLayout>
</LinearLayout>
```

When implementing the weekly row, expand the comment into seven `LinearLayout` columns (Mon–Fri with a non-zero sample like `80`, Sat/Sun `0`) using `@string/weekly_widget_day_mon` … `sun`. Do not leave the XML comment in the shipped file.

- [ ] **Step 5: Wire previewLayout on providers**

Modify each info XML: add `android:previewLayout="@layout/habit_widget_preview"` (and challenge/weekly equivalents) on the `appwidget-provider` root. Keep existing `configure`, `minWidth`, `minHeight`, `resizeMode`, `updatePeriodMillis`, `widgetCategory`.

- [ ] **Step 6: Re-run tests — expect PASS**

Same Gradle command as Step 2. Expected: PASS

---

### Task 2: Glance Studio previews (debug)

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts`
- Modify: `HabitGlanceWidget.kt`, `ChallengeGlanceWidget.kt`, `WeeklyGlanceWidget.kt` (content `private` → `internal`)
- Create debug previews under `app/src/debug/java/com/marta/habittracker/presentation/widgets/`

- [ ] **Step 1: Catalog + debug dependencies**

`libs.versions.toml` (same `glance` version `1.1.1`):

```toml
androidx-glance-preview = { group = "androidx.glance", name = "glance-preview", version.ref = "glance" }
androidx-glance-appwidget-preview = { group = "androidx.glance", name = "glance-appwidget-preview", version.ref = "glance" }
```

`app/build.gradle.kts` next to existing Glance deps:

```kotlin
debugImplementation(libs.androidx.glance.preview)
debugImplementation(libs.androidx.glance.appwidget.preview)
```

- [ ] **Step 2: Make Glance content `internal`**

Change `private fun HabitWidgetContent` / `ChallengeWidgetContent` / `WeeklyWidgetContent` to `internal fun`. No other behavior changes.

Make `openAppAction` still required; debug previews pass `androidx.glance.action.action { }`.

- [ ] **Step 3: Debug preview composables**

Create `app/src/debug/java/com/marta/habittracker/presentation/widgets/habit/HabitGlanceWidgetPreview.kt`:

```kotlin
package com.marta.habittracker.presentation.widgets.habit

import androidx.compose.runtime.Composable
import androidx.glance.action.action
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.GlancePreview
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.presentation.theme.HabitColor1
import com.marta.habittracker.presentation.utils.toHex
import java.time.Instant
import kotlinx.datetime.DayOfWeek

@OptIn(ExperimentalGlancePreviewApi::class)
@GlancePreview(widthDp = 180, heightDp = 110)
@Composable
fun HabitGlanceWidgetPreview() {
    HabitWidgetContent(
        habit = Habit(
            id = "preview",
            name = "Read",
            description = null,
            daysOfWeek = setOf(DayOfWeek.MONDAY),
            icon = "book",
            colorHex = HabitColor1.toHex(),
            reminderTime = null,
            createdAt = Instant.EPOCH,
            records = emptyList(),
            isCompleted = true,
        ),
        appWidgetId = 0,
        configureText = "Configure",
        completedText = "Completed today",
        notCompletedText = "Not completed today",
        markCompleteText = "Complete",
        markIncompleteText = "Mark incomplete",
        streakText = "3 day streak",
        openAppAction = action { },
    )
}
```

Create analogous `ChallengeGlanceWidgetPreview.kt` (`ChallengeWidgetSnapshot` sample vs Alex, 60/40, 5 days, `status = "active"`) at `@GlancePreview(widthDp = 180, heightDp = 110)` calling `ChallengeWidgetContent`.

Create `WeeklyGlanceWidgetPreview.kt` at `@GlancePreview(widthDp = 250, heightDp = 110)` with seven `WeeklyWidgetDayCell` values (sample percents Mon–Sun) and existing day-label strings in English.

If `action { }` is unresolved, use `actionStartActivity` only in runtime widgets; for preview add:

```kotlin
internal fun glanceNoOpAction(): androidx.glance.action.Action = action { }
```

in `presentation/widgets/GlanceNoOpAction.kt` (main source set).

- [ ] **Step 4: Compile debug Kotlin**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:compileDebugKotlin --no-daemon`

Expected: BUILD SUCCESSFUL

---

### Task 3: Verification

- [ ] **Step 1:** `.\gradlew.bat :app:compileDebugKotlin --no-daemon` — SUCCESS
- [ ] **Step 2:** `.\gradlew.bat :app:testDebugUnitTest --tests "com.marta.habittracker.presentation.widgets.WidgetPreviewLayoutTest" --no-daemon` — PASS
- [ ] **Step 3:** Manual: Android Studio widget file / debug preview pane; device widget picker shows the three thumbnails.

---

## Spec coverage

| Requirement | Task |
|-------------|------|
| Picker `previewLayout` for habit, challenge, weekly | Task 1 |
| `@GlancePreview` in debug for the same three | Task 2 |
| No fourth widget | Out of scope |

## Placeholder scan

No TBD / “similar to Task N” leftovers except weekly XML which must be fully expanded in Task 1 Step 4 (seven day columns).
