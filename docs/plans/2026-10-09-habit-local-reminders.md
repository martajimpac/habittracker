# Habit local reminders Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Fire a local exact-time notification for a habit when the user enables the Reminder toggle on Create and Detail → Settings, using stored `reminder_time` and `days_of_week`.

**Architecture:** `reminder_time == null` means off. Domain stays Android-free (`NextHabitReminder` + `HabitReminderScheduler` port). `HabitRepositoryImpl` schedules/cancels only after a successful online-first write (and `rescheduleAll` after sync). Android `AlarmManager.setExactAndAllowWhileIdle` lives in `core`. Receivers use Hilt `EntryPoint` (same pattern as widgets). Notification tap reuses `WidgetLaunchExtras.EXTRA_HABIT_ID` to open `TabDetail`.

**Tech Stack:** AlarmManager, NotificationCompat, POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, Hilt, Compose, Room, online-first Supabase writes already in `HabitRepositoryImpl`.

**Platform:** Android (minSdk 26)

**Do not commit unless the user asks.** Stage task files (`git add`). JAVA_HOME: `C:\Program Files\Android\Android Studio\jbr`. Gradle: `.\gradlew.bat --no-daemon`.

---

## File map

| Area | Paths |
|------|--------|
| Spec | `spec.md` (already updated) |
| Domain | `domain/reminder/NextHabitReminder.kt`, `domain/reminder/HabitReminderScheduler.kt` |
| Data | `HabitRepositoryImpl.kt` (hook after insert/update/delete/sync) |
| Android | `core/reminder/AndroidHabitReminderScheduler.kt`, `HabitReminderReceiver.kt`, `HabitReminderBootReceiver.kt`, `HabitReminderEntryPoint.kt` |
| Manifest | `app/src/main/AndroidManifest.xml` |
| Nav | `MainActivity.kt`, `NavigationWrapper.kt`, `BottomNavScreen.kt`, `WidgetLaunchExtras.kt` |
| UI | `AddContentScreen.kt` / `AddContentViewModel.kt`, `HabitDetailScreen.kt` / `HabitDetailViewModel.kt`, shared `ReminderToggleRow` in `presentation/components` |
| DI | `DataModule.kt` `@Binds HabitReminderScheduler` |
| Strings | `values/strings.xml`, `values-es/strings.xml` |
| Tests | `NextHabitReminderTest`, `HabitRepositoryImpl` reminder hooks (fake scheduler), `AddContentViewModelTest`, `HabitDetailViewModelTest` |

**Out of scope:** FCM habit reminders, complete-from-notification, Profile Notifications screen, WorkManager clock, iOS, mass-null migration of existing `reminder_time`.

**Order:** Task 1 (domain next-fire) → 2 (port + repo hooks) → 3 (AlarmManager + receivers + manifest) → 4 (tap → TabDetail) → 5 (Create UI + permission) → 6 (Detail Settings UI). Do not parallelize 5–6 (shared toggle). Do not parallelize 2–3 (same repo/DI).

---

### Task 1: Next occurrence calculator (TDD)

**Files:**
- Create: `app/src/main/java/com/marta/habittracker/domain/reminder/NextHabitReminder.kt`
- Test: `app/src/test/java/com/marta/habittracker/domain/reminder/NextHabitReminderTest.kt`

- [ ] **Step 1: Failing tests**

```kotlin
class NextHabitReminderTest {
    private val zone = ZoneId.of("Europe/Madrid")

    @Test
    fun `today later hour on scheduled day is used`() {
        val now = ZonedDateTime.of(2026, 10, 9, 7, 0, 0, 0, zone) // Friday
        val next = NextHabitReminder.nextTrigger(
            now = now,
            reminderTime = "08:00",
            daysOfWeek = setOf(KotlinDayOfWeek.FRIDAY),
        )
        assertEquals(ZonedDateTime.of(2026, 10, 9, 8, 0, 0, 0, zone).toInstant(), next)
    }

    @Test
    fun `hour already passed today jumps to next scheduled weekday`() {
        val now = ZonedDateTime.of(2026, 10, 9, 9, 0, 0, 0, zone) // Friday
        val next = NextHabitReminder.nextTrigger(
            now = now,
            reminderTime = "08:00",
            daysOfWeek = setOf(KotlinDayOfWeek.MONDAY, KotlinDayOfWeek.FRIDAY),
        )
        assertEquals(ZonedDateTime.of(2026, 10, 12, 8, 0, 0, 0, zone).toInstant(), next)
    }

    @Test
    fun `blank time or empty days returns null`() {
        val now = ZonedDateTime.of(2026, 10, 9, 7, 0, 0, 0, zone)
        assertNull(NextHabitReminder.nextTrigger(now, null, setOf(KotlinDayOfWeek.FRIDAY)))
        assertNull(NextHabitReminder.nextTrigger(now, "08:00", emptySet()))
        assertNull(NextHabitReminder.nextTrigger(now, "bad", setOf(KotlinDayOfWeek.FRIDAY)))
    }

    @Test
    fun `isCompletedOn skips notify`() {
        val today = KotlinLocalDate(2026, 10, 9)
        val habit = sampleHabit(
            days = setOf(KotlinDayOfWeek.FRIDAY),
            records = listOf(HabitRecord("r", "h", today, true)),
        )
        assertFalse(NextHabitReminder.shouldNotify(habit, today))
        assertTrue(NextHabitReminder.shouldNotify(habit.copy(records = emptyList()), today))
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL (unresolved `NextHabitReminder`)**

```
.\gradlew.bat :app:testDebugUnitTest --tests "com.marta.habittracker.domain.reminder.NextHabitReminderTest" --no-daemon
```

- [ ] **Step 3: Implement**

```kotlin
package com.marta.habittracker.domain.reminder

import com.marta.habittracker.domain.model.Habit
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlinx.datetime.DayOfWeek as KotlinDayOfWeek
import kotlinx.datetime.LocalDate as KotlinLocalDate

object NextHabitReminder {
    private val TimePattern = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""")

    fun nextTrigger(
        now: ZonedDateTime,
        reminderTime: String?,
        daysOfWeek: Set<KotlinDayOfWeek>,
    ): Instant? {
        val time = parse(reminderTime) ?: return null
        if (daysOfWeek.isEmpty()) return null
        for (plusDays in 0..7) {
            val candidate = now.toLocalDate().plusDays(plusDays.toLong()).atTime(time).atZone(now.zone)
            if (candidate.isBefore(now) || candidate.isEqual(now)) continue
            val kotlinDow = listOf(candidate.dayOfWeek).toKotlinSet().single() // core.DateTimeExtensions
            if (kotlinDow in daysOfWeek) return candidate.toInstant()
        }
        return null
    }

    fun shouldNotify(habit: Habit, today: KotlinLocalDate): Boolean {
        if (habit.reminderTime.isNullOrBlank()) return false
        if (today.dayOfWeek !in habit.daysOfWeek) return false
        return habit.records.none { it.date == today && it.isCompleted }
    }

    private fun parse(value: String?): LocalTime? {
        val match = TimePattern.matchEntire(value?.trim().orEmpty()) ?: return null
        return LocalTime.of(match.groupValues[1].toInt(), match.groupValues[2].toInt())
    }
}
```

Verify `KotlinDayOfWeek(iso)` matches existing `DateTimeExtensions` / habit mapping. If `DayOfWeek(value)` is wrong for kotlinx, map via `java.time.DayOfWeek` the same way `toKotlinSet` does.

- [ ] **Step 4: Tests PASS**

- [ ] **Step 5: Stage** `git add -- domain/reminder tests`. Do not commit unless asked.

---

### Task 2: Scheduler port + repository hooks (TDD)

**Files:**
- Create: `domain/reminder/HabitReminderScheduler.kt`
- Create: `app/src/test/.../FakeHabitReminderScheduler.kt`
- Modify: `HabitRepositoryImpl.kt` (inject scheduler; after successful insert/update `schedule(habit)`; delete `cancel(id)`; after successful `syncFromRemote` `rescheduleAll(habits)` — load non-deleted with non-null reminder from DAO/mapped list)
- Modify: `di/DataModule.kt` — bind later in Task 3; for unit tests constructor-inject fake. If Hilt requires a binding now, bind a no-op `NoOpHabitReminderScheduler` in data that Task 3 replaces — **prefer bind Android impl only in Task 3** and keep `HabitRepositoryImpl` constructor injectable in tests without Hilt.
- Test: extend or add `HabitRepositoryImplTest` **only if** a fake remote already exists. If `HabitRepositoryImpl` is hard to construct, test a thin `HabitReminderHooks` helper **or** verify via a small package-visible function. Prefer: extract

```kotlin
internal fun afterHabitPersisted(habit: Habit, scheduler: HabitReminderScheduler) {
    if (habit.reminderTime.isNullOrBlank()) scheduler.cancel(habit.id)
    else scheduler.schedule(habit)
}
```

and unit-test that. Call it from insert/update success. delete → `cancel`. sync → `rescheduleAll`.

**Interface:**

```kotlin
interface HabitReminderScheduler {
    fun schedule(habit: Habit)
    fun cancel(habitId: String)
    fun rescheduleAll(habits: List<Habit>)
}
```

`rescheduleAll`: cancel-all then schedule each with non-null time (implementation detail of Android class; fake records calls).

- [ ] **Step 1: Failing tests** on hooks: persist with time → schedule; persist null → cancel; delete → cancel; list with mixed → rescheduleAll called.

- [ ] **Step 2: FAIL then implement hooks in `HabitRepositoryImpl`.** Catch scheduler exceptions: `Log.e` then still return `Success` for the write (habit is saved; reminder is best-effort). Do **not** fail the user’s save if AlarmManager throws.

- [ ] **Step 3: Tests PASS.** `compileDebugKotlin`.

- [ ] **Step 4: Stage.** No commit unless asked.

---

### Task 3: AlarmManager, notification, receivers, manifest

**Files:**
- Create: `core/reminder/AndroidHabitReminderScheduler.kt`
- Create: `core/reminder/HabitReminderReceiver.kt`
- Create: `core/reminder/HabitReminderBootReceiver.kt`
- Create: `core/reminder/HabitReminderEntryPoint.kt`
- Modify: `DataModule.kt` `@Binds AndroidHabitReminderScheduler`
- Modify: `AndroidManifest.xml`
- Modify: `strings.xml` + `values-es/strings.xml`
- Test: `AndroidHabitReminderScheduler` next-fire uses `NextHabitReminder` — keep Android class thin; unit-test `notificationIdFor(habitId: String): Int` = `habitId.hashCode() and 0x7fffffff` (never 0).

**Manifest (inside `<manifest>`):**

```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

`POST_NOTIFICATIONS` already exists. Inside `<application>`:

```xml
<receiver
    android:name=".core.reminder.HabitReminderReceiver"
    android:exported="false" />
<receiver
    android:name=".core.reminder.HabitReminderBootReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.TIMEZONE_CHANGED" />
        <action android:name="android.intent.action.TIME_SET" />
    </intent-filter>
</receiver>
```

**Strings:**
- `habit_reminder_channel_id` is **not** user copy — keep as Kotlin const `habit_reminder_channel` (distinct from FCM `habit_tracker_notifications`).
- `habit_reminder_channel_name` / `habit_reminder_channel_name` ES
- `habit_reminder_body` e.g. `Time to check in` / `Es hora de registrar el hábito`
- `habit_reminder_cd` if needed

**Scheduler:**

```kotlin
@Singleton
class AndroidHabitReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : HabitReminderScheduler {
    override fun schedule(habit: Habit) {
        val trigger = NextHabitReminder.nextTrigger(
            now = ZonedDateTime.now(),
            reminderTime = habit.reminderTime,
            daysOfWeek = habit.daysOfWeek,
        )
        if (trigger == null) {
            cancel(habit.id)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(AlarmManager::class.java)
            if (!am.canScheduleExactAlarms()) {
                Log.w(TAG, "schedule skipped: exact alarms disabled habitId=${habit.id}")
                return
            }
        }
        val pending = pendingIntent(habit.id)
        context.getSystemService(AlarmManager::class.java).setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            trigger.toEpochMilli(),
            pending,
        )
        Log.d(TAG, "schedule ok habitId=${habit.id}")
    }
    // cancel: am.cancel(pendingIntent(id))
    // rescheduleAll: habits.forEach { if (reminderTime != null) schedule(it) else cancel(it.id) }
}
```

PendingIntent: `getBroadcast`, `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`, extra `EXTRA_HABIT_ID`, requestCode = `notificationIdFor(habitId)`.

**Receiver `HabitReminderReceiver`:** `goAsync()`, `EntryPointAccessors.fromApplication` → `habitRepository()` + scheduler. `runBlocking`/`CoroutineScope` on IO: `getHabitById(id).first()` (include records). If habit null/deleted → return. `shouldNotify` → `NotificationCompat` + `notify(notificationId, ...)`. Always `schedule(habit)` after (next occurrence). `CancellationException` rethrow. `Log.e` on failure. ContentIntent: `WidgetLaunchExtras.openHomeIntent(context, habitId)` with `FLAG_IMMUTABLE` activity PI.

Channel `IMPORTANCE_DEFAULT` or HIGH; name from strings. Small icon: reuse launcher/`ic_launcher_foreground` like FCM for now.

**Boot receiver:** `goAsync`, load `getAllHabitsWithRecords().first()`, `rescheduleAll`. Log. Do not read email.

- [ ] **Step 1: Implement + `compileDebugKotlin`.**

- [ ] **Step 2: Unit test `notificationIdFor` uniqueness-ish (two ids → possibly same hash is OK to document).**

- [ ] **Step 3: Stage.** No commit unless asked.

---

### Task 4: Notification tap opens `TabDetail`

**Files:**
- Modify: `WidgetLaunchExtras.kt` — `habitIdFromIntent(intent): String?`
- Modify: `MainActivity.kt` — `initialHabitId` state like `initialTab`, set in `onCreate`/`onNewIntent`
- Modify: `NavigationWrapper` — pass `initialHabitId` to `BottomNavScreen`
- Modify: `BottomNavScreen.kt` — `LaunchedEffect(initialHabitId)` if non-null `navigate(TabScreens.TabDetail(id))` then clear (callback) so it does not re-navigate forever

Wire `HomeScreen` `onHabitClick` **only if** needed for the same extra; tap from notification does not require Home list click. Optional: Home row click → Detail is **YAGNI unless already designed**; spec only requires notification tap → detail. **Do not** add Home click unless a one-line callback already exists.

- [x] **Step 1: Implement navigation path. `compileDebugKotlin`.**

- [x] **Step 2: Stage.**

---

### Task 5: Create habit UI — toggle + permissions (TDD)

**Files:**
- Create: `presentation/components/ReminderToggleRow.kt` (switch + optional time row; no ViewModel)
- Modify: `AddContentUiState` — `reminderEnabled: Boolean = false`; default `reminderTime` stay `"08:00"` **but save sends null when `!reminderEnabled`**
- Modify: `AddContentViewModel` / `AddContentViewModelTest` / `AddContentScreen.kt`
- Modify: `AddContentScreen` — `rememberLauncherForActivityResult(RequestPermission())` for `POST_NOTIFICATIONS` when API ≥ 33. Exact-alarm: if API ≥ 31 && `!AlarmManager.canScheduleExactAlarms()`, do not enable; send `ShowError` / `errorMessageRes` (`R.string.habit_reminder_exact_alarm_denied`) and keep toggle off. Settings intent `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` is optional; spec only requires error + toggle off.

**Save:**

```kotlin
reminderTime = if (currentState.reminderEnabled) {
    currentState.reminderTime.trim().ifBlank { null }
} else null
```

**Tests:**
- `reminderEnabled false` → `SaveHabit` gets `reminderTime = null` (use FakeHabitRepository)
- `reminderEnabled true` + `08:00` → `"08:00"`
- Existing reminder format validation only when enabled

**UI:** Switch off by default; time picker visible only when on. Reuse current TimePicker dialog. All copy in EN+ES.

Screen requests permission **when user turns the switch on**. If denied → ViewModel `onReminderEnabled(false)` + error string.

- [ ] **Step 1: Failing ViewModel tests**

- [ ] **Step 2: Implement VM + UI**

- [ ] **Step 3: Tests + `compileDebugKotlin` PASS**

- [ ] **Step 4: Stage**

---

### Task 6: Detail → Settings same toggle + `updateHabit`

**Files:**
- Modify: `HabitDetailViewModel.kt` — state for reminderEnabled/time copied from loaded habit (`reminderTime != null` → enabled). `saveReminder()` / include in a save that calls `repository.updateHabit(habit.copy(reminderTime = ...))`. Permission same as create: Screen-level launcher.
- Modify: `HabitDetailScreen.kt` — `SettingsTab` currently placeholder `detail_edit_habit`. Replace with `ReminderToggleRow` + Save (`HabitButton`) **for reminder only** (do not build a full habit editor). Delete habit stays as today.
- Test: `HabitDetailViewModelTest` — load with `08:00` → enabled; save with enabled false → `updateHabit` with `reminderTime = null`.

Events: `Channel` + `ObserveAsEvents` for snackbar errors; no sticky nav flags.

- [ ] **Step 1: Failing tests**

- [ ] **Step 2: Implement**

- [ ] **Step 3: `compileDebugKotlin` + unit tests PASS**

- [ ] **Step 4: Stage**

---

### Task 7: Verification

- [ ] `.\gradlew.bat :app:compileDebugKotlin --no-daemon`
- [ ] `.\gradlew.bat :app:testDebugUnitTest --tests "com.marta.habittracker.domain.reminder.*" --tests "*AddContentViewModelTest*" --tests "*HabitDetailViewModelTest*" --no-daemon`
- [ ] Manual (device): enable reminder, grant notifications, fire or adb date; complete habit then no notify; reboot reschedules; tap opens detail; deny permission keeps toggle off.

---

## Spec coverage

| Requirement | Task |
|-------------|------|
| Local exact AlarmManager | 3 |
| `null` reminder_time = off | 2, 5, 6 |
| Toggle default off on create | 5 |
| Detail Settings same control | 6 |
| Cancel on delete | 2 |
| Skip if completed | 1 + 3 receiver |
| Tap → detail | 4 |
| Boot / timezone reschedule | 3 |
| Distinct channel + per-habit notify id | 3 |
| Permissions + error strings | 5, 6 |
| Existing non-null times treated as on | 2 sync `rescheduleAll` |
| No FCM / Profile Notifications / complete action | honored |

## Placeholder scan

None. Scheduler exceptions on save are logged, write still succeeds (explicit).
