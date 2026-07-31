# Friends Screen (Figma + Supabase) Android Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans`. Checkboxes track progress. Supabase schema already applied (`friends_social_schema`).

**Goal:** Replace Friends empty-state with Figma Make UI wired to Supabase: friends list, active challenges (progress computed), Add-friend request sheet, view public habits, create challenge.

**Architecture:** Clean Architecture. New `FriendsRepository` + `SocialRemoteDataSource` (PostgREST). Social reads/writes are **online-required** (same as habit mutations); **no Room cache for friends/challenges in this plan** (per spec). Challenge progress computed in domain from `habit_records` between `starts_at`/`ends_at`. Extend existing habit pipeline with `isPublic` so friends can see habits.

**Tech Stack:** Compose, Hilt, Supabase Kotlin PostgREST, Coroutines/Flow, JUnit

**Platform:** Android

**Depends on:** Remote tables `profiles`, `friendships`, `challenges`, `habits.is_public` (already live).

---

## File map (create / modify)

| Area | Paths |
|------|--------|
| Domain | `domain/model/Profile.kt`, `Friendship.kt`, `Challenge.kt`, `FriendListItem.kt`; `domain/repository/FriendsRepository.kt`; `domain/usecase/ComputeChallengeProgress.kt` (or package-private helpers) |
| Data | `data/remote/SocialRemoteDataSource.kt`, `data/remote/dto/ProfileDto.kt` (+ Friendship/Challenge DTOs), `data/mapper/SocialMapper.kt`, `data/repository/FriendsRepositoryImpl.kt` |
| Habits privacy | Modify `Habit.kt`, `HabitEntity`, `HabitDto`, mappers, Room DB version, `SaveHabit`, AddContent UI |
| DI | `di/DataModule.kt` bind `FriendsRepository` |
| UI | `presentation/screens/friends/FriendsViewModel.kt`, rewrite `FriendsScreen.kt`; strings |
| Tests | `FakeFriendsRepository`, `FriendsViewModelTest`, `ComputeChallengeProgressTest`, `FriendsRepositoryImplTest` (optional fake remote) |

---

### Task 1: Domain models + challenge progress (TDD)

**Files:**
- Create: `app/src/main/java/com/marta/habittracker/domain/model/Profile.kt`
- Create: `app/src/main/java/com/marta/habittracker/domain/model/ChallengeCriteria.kt`
- Create: `app/src/main/java/com/marta/habittracker/domain/model/Challenge.kt`
- Create: `app/src/main/java/com/marta/habittracker/domain/usecase/ComputeChallengeProgress.kt`
- Test: `app/src/test/java/com/marta/habittracker/domain/usecase/ComputeChallengeProgressTest.kt`

- [ ] **Step 1: Failing tests for progress %**

```kotlin
@Test
fun `completion_pct is completed days over total challenge days`() {
    val starts = Instant.parse("2026-07-01T00:00:00Z")
    val ends = Instant.parse("2026-07-08T00:00:00Z") // 7 days
    val records = listOf(
        HabitRecord("1", "h", LocalDate(2026, 7, 1), true),
        HabitRecord("2", "h", LocalDate(2026, 7, 2), true),
        HabitRecord("3", "h", LocalDate(2026, 7, 3), false),
    )
    val pct = computeChallengeProgress(
        criteria = ChallengeCriteria.CompletionPct,
        startsAt = starts,
        endsAt = ends,
        records = records,
        today = LocalDate(2026, 7, 4),
    )
    // define exact formula in impl; assert documented expected
}
```

- [ ] **Step 2: Implement `ChallengeCriteria` + `computeChallengeProgress` for `Streak`, `AllDays`, `CompletionPct`**
  - Document formula in KDoc (e.g. CompletionPct = completed days in [start, min(today, end)) / totalDays * 100).
  - `daysLeft` = max(0, days between today and endDate).

- [ ] **Step 3: Tests green**

Run: `./gradlew :app:testDebugUnitTest --tests "com.marta.habittracker.domain.usecase.ComputeChallengeProgressTest"`

- [ ] **Step 4: Commit** (if user asked) / stage

---

### Task 2: `FriendsRepository` contract + Fake

**Files:**
- Create: `domain/repository/FriendsRepository.kt`
- Create: `domain/model/FriendListItem.kt` (profile + bestStreak + publicHabitsCount + activeChallengeCount)
- Create: `test/.../FakeFriendsRepository.kt`

```kotlin
interface FriendsRepository {
    suspend fun getAcceptedFriends(): DataResult<List<FriendListItem>, AppError>
    suspend fun getActiveChallenges(): DataResult<List<ChallengeCard>, AppError>
    suspend fun searchProfiles(query: String): DataResult<List<Profile>, AppError>
    suspend fun sendFriendRequest(addresseeId: String): DataResult<Unit, AppError>
    suspend fun respondToFriendRequest(friendshipId: String, accept: Boolean): DataResult<Unit, AppError>
    suspend fun getPublicHabitsForFriend(friendUserId: String): DataResult<List<Habit>, AppError>
    suspend fun createChallenge(
        challengedId: String,
        challengerHabitId: String,
        challengedHabitId: String,
        criteria: ChallengeCriteria,
        durationDays: Int,
    ): DataResult<Unit, AppError>
}
```

`ChallengeCard` = challenge + friend profile snapshot + myProgress/theirProgress/daysLeft (computed when loading).

- [ ] Write Fake + one ViewModel test that fails until Task 6
- [ ] Stage

---

### Task 3: Social remote DTOs + `SocialRemoteDataSource`

**Files:**
- Create: `data/remote/dto/ProfileDto.kt`, `FriendshipDto.kt`, `ChallengeDto.kt`
- Create: `data/remote/SocialRemoteDataSource.kt`
- Create: `data/mapper/SocialMapper.kt`

Mirror `HabitRemoteDataSource`:

```kotlin
suspend fun fetchAcceptedFriendships(userId: String): List<FriendshipDto> {
    return supabase.from("friendships").select(Columns.ALL) {
        filter {
            eq("status", "accepted")
            or {
                eq("requester_id", userId)
                eq("addressee_id", userId)
            }
        }
    }.decodeList()
}
```

Also: fetch profiles by ids, search `username` ilike, insert friendship pending, update status, fetch challenges where participant + status in (`pending`,`active`), fetch habits for friend where `is_public=true`, fetch records for habit ids in date range.

Log failures with `Log.e(TAG, …, e)` (service logging rule).

- [ ] Compile after wiring
- [ ] Stage

---

### Task 4: `FriendsRepositoryImpl` + DI

**Files:**
- Create: `data/repository/FriendsRepositoryImpl.kt`
- Modify: `di/DataModule.kt`

```kotlin
@Singleton
class FriendsRepositoryImpl @Inject constructor(
    private val networkChecker: NetworkChecker,
    private val socialRemote: SocialRemoteDataSource,
    private val habitRemote: HabitRemoteDataSource, // or methods on SocialRemote for friend habits/records
) : FriendsRepository {
    override suspend fun getAcceptedFriends(): DataResult<List<FriendListItem>, AppError> {
        if (!networkChecker.isOnline()) return DataResult.Error(AppError.Common.Network)
        return try {
            // load friendships → peer profile ids → profiles → optional public habit counts / streaks from remote records
            DataResult.Success(...)
        } catch (e: Exception) {
            Log.e(TAG, "getAcceptedFriends failed", e)
            DataResult.Error(AppError.Common.Unknown)
        }
    }
}
```

Best streak for a friend: max streak across their **public** habits’ records (reuse `calculateStreak` from Home with `LocalDate.now()`).

- [ ] `@Binds fun bindFriendsRepository…`
- [ ] Unit test with fake remote or FakeFriendsRepository for ViewModel later
- [ ] Stage

---

### Task 5: Habit `isPublic` (DTO + Room + Save + Add UI)

Without this, friends always see empty public lists.

**Files:**
- Modify: `domain/model/Habit.kt` add `isPublic: Boolean = false`
- Modify: `HabitEntity`, `HabitDto` (`@SerialName("is_public")`), mappers, `HabitDatabase` version bump (e.g. `20260731`) + destructive or simple migration if project uses fallback
- Modify: `SaveHabit` / `AddContentUiState` + toggle “Public habit” in AddContent
- Modify: strings (`habit_is_public`)
- Tests: update any Habit construction in tests with default `isPublic = false`

- [ ] Failing test: SaveHabit / mapper round-trip includes `isPublic`
- [ ] Implement + compile + unit tests
- [ ] Stage

---

### Task 6: `FriendsViewModel` (TDD)

**Files:**
- Create: `presentation/screens/friends/FriendsViewModel.kt`
- Test: `presentation/screens/friends/FriendsViewModelTest.kt`

```kotlin
data class FriendsUiState(
    val isLoading: Boolean = true,
    val friends: List<FriendListItemUi> = emptyList(),
    val challenges: List<ChallengeCardUi> = emptyList(),
    val sheet: FriendsSheet = FriendsSheet.None,
    @StringRes val errorRes: Int? = null,
)

sealed class FriendsSheet {
    data object None : FriendsSheet()
    data object AddFriend : FriendsSheet()
    data class ViewFriend(val friendId: String) : FriendsSheet()
    data class CreateChallenge(val friendId: String) : FriendsSheet()
}
```

- [ ] Test: successful load exposes friends + challenges
- [ ] Test: offline → error string res
- [ ] Test: `onAddFriendClicked` opens Add sheet; `sendRequest` calls repo
- [ ] Implement ViewModel (inject `FriendsRepository`, `HabitRepository` for own habits when creating challenge)
- [ ] Stage

---

### Task 7: Friends UI (Figma layout)

**Files:**
- Rewrite: `presentation/screens/friends/FriendsScreen.kt`
- Modify: `strings.xml` (English): active challenges, days left, add friend, search hint, send request, public habits, private habits, create challenge, criteria labels, duration, empty friends, etc.

**Layout (match Figma Make):**
1. Header: title + gradient Add → `onAddFriend`
2. Body bg light lilac (`#F6F0FF` or `HabitTermsBg` / surface):
   - Horizontal carousel **Active challenges** (cards ~220dp, gradient from habit color, me vs friend progress bars, days left)
   - **Friends** list rows: avatar circle + color, name, `@username`, best streak, public habit count or “Private”, actions View / Challenge
3. Modal bottom sheets:
   - **Add:** search field, send request (no-op success toast/snackbar via state flag)
   - **View:** friend header + public habits list or private empty
   - **Challenge:** habit picker (friend public + my habits with **same name** preferred / user picks pair), duration 3/7/14/30, criteria, send

Reuse: `HabitLineIcon` for habit icons (no emoji glyphs if project standard is drawables — map friend remote icon keys the same way).

- [ ] `FriendsScreen` collects VM; `FriendsContent(uiState, callbacks)` pure
- [ ] CompileDebug
- [ ] Stage

---

### Task 8: Wire navigation + refresh

**Files:**
- `NavigationBottomWrapper.kt` — already hosts `FriendsScreen()`; ensure Hilt VM works (default)
- Optional: pull-to-refresh / reload on resume (`Lifecycle.resume`)

- [ ] Manual smoke checklist (device):
  - [ ] Empty friends → empty challenges UI
  - [ ] Two accounts: send request, accept (accept UI: show pending on Friends or Profile — include pending row section under friends list)
  - [ ] Mark habit public → friend sees it
  - [ ] Create challenge → appears in carousel with progress

---

### Task 9: Spec + verification

**Files:**
- Modify: `spec.md` — Friends Android now in scope; document `FriendsRepository`, online-required social ops, `isPublic` on habits
- Run: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`
- Stage all task files
- Propose brain decision: “Friends social is remote-only (no Room) online-first”

---

## Out of scope (this plan)

- Push notifications for requests/challenges
- Room cache / offline friends list
- Realtime subscriptions
- Email search (username only for MVP; email optional later)
- Editing challenge mid-flight
- Friends tab deep links

---

## Dependency graph

```text
Task1 progress ─┐
Task2 contract ─┼─► Task4 impl ─► Task6 VM ─► Task7 UI ─► Task8 ─► Task9
Task3 remote  ─┘         ▲
Task5 isPublic ──────────┘ (needed before meaningful View sheet / challenges)
```

**Parallel-safe:** Task1 ‖ Task2 ‖ Task3 ‖ Task5 (watch Habit files for Task5 vs Task4 if sharing HabitDto). Prefer **Task5 before Task4** if `getPublicHabits` filters `is_public`.

---

## Spec coverage check

| Spec item | Task |
|-----------|------|
| Figma header + Add + list + sheets | 7 |
| Friend request accept flow | 2, 4, 6, 7 |
| Public habits per habit | 5, 4 |
| Challenges + computed progress | 1, 4, 6, 7 |
| Online-first social | 4 |
| English strings | 7 |
| No Room friends cache | 4 (explicit) |

---

## Execution handoff

Plan saved to `docs/plans/2026-07-31-friends-figma-android.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** — fresh subagent per task, review between tasks  
2. **Inline Execution** — same session with checkpoints  

Which approach?
