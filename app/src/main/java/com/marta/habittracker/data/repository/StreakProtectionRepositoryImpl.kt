package com.marta.habittracker.data.repository

import android.util.Log
import com.marta.habittracker.core.network.NetworkChecker
import com.marta.habittracker.core.toKotlin
import com.marta.habittracker.data.local.room.StreakProtectionDao
import com.marta.habittracker.data.local.room.entities.StreakBaselineEntity
import com.marta.habittracker.data.local.room.entities.StreakProtectionEventEntity
import com.marta.habittracker.data.remote.StreakProtectionRemoteDataSource
import com.marta.habittracker.data.remote.dto.StreakProtectionEventDto
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.model.protectionMilestone
import com.marta.habittracker.domain.model.resolvedSchedule
import com.marta.habittracker.domain.model.storedValue
import com.marta.habittracker.domain.repository.StreakProtectionRepository
import com.marta.habittracker.domain.usecase.HabitProtectionInput
import com.marta.habittracker.domain.usecase.ProtectionEvent
import com.marta.habittracker.domain.usecase.ProtectionEventType
import com.marta.habittracker.domain.usecase.ProtectionReconcileResult
import com.marta.habittracker.domain.usecase.reconcileStreakProtections
import com.marta.habittracker.domain.usecase.streakForHabit
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.LocalDate as KotlinLocalDate

@Singleton
class StreakProtectionRepositoryImpl @Inject constructor(
    private val dao: StreakProtectionDao,
    private val remote: StreakProtectionRemoteDataSource,
    private val networkChecker: NetworkChecker,
) : StreakProtectionRepository {

    override suspend fun reconcile(habits: List<Habit>, today: LocalDate): ProtectionReconcileResult {
        if (networkChecker.isOnline()) {
            try {
                val remoteEvents = remote.fetchEvents().map { it.toEvent() }
                dao.insertEvents(remoteEvents.map { it.toEntity() })
                Log.d(TAG, "pull ok count=${remoteEvents.size}")
            } catch (e: Exception) {
                Log.e(TAG, "pull failed", e)
            }
        }
        val existing = dao.events().map { it.toEvent() }
        val baselines = dao.baselines().associateBy { it.habitId }
        val todayKotlin = today.toKotlin()
        val inputs = habits.map { habit -> inputFor(habit, baselines[habit.id], today, todayKotlin) }
        val result = reconcileStreakProtections(
            habits = inputs,
            existingEvents = existing,
            today = todayKotlin,
            protectionSince = inputs.minOfOrNull { it.evaluateFrom } ?: todayKotlin,
        )
        if (result.newEvents.isNotEmpty()) {
            dao.insertEvents(result.newEvents.map { it.toEntity() })
            Log.d(TAG, "stored events count=${result.newEvents.size}")
            if (networkChecker.isOnline()) {
                result.newEvents.forEach { event ->
                    try {
                        remote.insertEvent(event.toDto())
                    } catch (e: Exception) {
                        Log.e(TAG, "push failed habitId=${event.habitId}", e)
                    }
                }
            }
        }
        result.habits.forEach { status ->
            val habit = habits.first { it.id == status.habitId }
            val input = inputs.first { it.habitId == status.habitId }
            val previous = baselines[habit.id]
            val scheduleChanged = previous != null && previous.scheduleKey != scheduleKey(habit)
            val first = previous == null
            dao.upsertBaseline(
                StreakBaselineEntity(
                    habitId = habit.id,
                    baselineStreak = if (first || scheduleChanged) input.baselineStreak else previous.baselineStreak,
                    baselineProgress = if (first || scheduleChanged) input.baselineProgress else previous.baselineProgress,
                    since = if (first || scheduleChanged) input.evaluateFrom.toString() else previous.since,
                    scheduleKey = scheduleKey(habit),
                    currentStreak = status.streak,
                    currentProgress = status.progress,
                ),
            )
        }
        return result
    }

    private fun inputFor(
        habit: Habit,
        row: StreakBaselineEntity?,
        today: LocalDate,
        todayKotlin: KotlinLocalDate,
    ): HabitProtectionInput {
        val createdOn = KotlinLocalDate.parse(
            habit.createdAt.atZone(ZoneId.systemDefault()).toLocalDate().toString(),
        )
        val schedule = habit.resolvedSchedule()
        val key = scheduleKey(habit)
        val (baselineStreak, baselineProgress, evaluateFrom) = when {
            row == null -> {
                val baseline = streakForHabit(
                    habit = habit,
                    today = KotlinLocalDate.parse(today.minusDays(1).toString()),
                    createdOn = createdOn,
                )
                Triple(baseline, baseline % schedule.protectionMilestone(), todayKotlin)
            }
            row.scheduleKey != key -> Triple(row.currentStreak, row.currentProgress, todayKotlin)
            else -> Triple(row.baselineStreak, row.baselineProgress, KotlinLocalDate.parse(row.since))
        }
        return HabitProtectionInput(
            habitId = habit.id,
            schedule = schedule,
            daysOfWeek = habit.daysOfWeek,
            daysPerWeek = habit.daysPerWeek,
            completedDates = habit.records.filter { it.isCompleted }.map { it.date }.toSet(),
            createdOn = createdOn,
            baselineStreak = baselineStreak,
            baselineProgress = baselineProgress,
            evaluateFrom = evaluateFrom,
        )
    }

    private fun scheduleKey(habit: Habit): String {
        val days = habit.daysOfWeek.map { it.name }.sorted().joinToString(",")
        return "${habit.resolvedSchedule().storedValue()}|$days|${habit.daysPerWeek}"
    }

    companion object {
        private const val TAG = "StreakProtectionRepo"
    }
}

private fun StreakProtectionEventEntity.toEvent() = ProtectionEvent(
    habitId = habitId,
    occasionKey = occasionKey,
    type = if (type == ProtectionEventType.GRANT.name) {
        ProtectionEventType.GRANT
    } else {
        ProtectionEventType.CONSUME
    },
)

private fun ProtectionEvent.toEntity() = StreakProtectionEventEntity(
    habitId = habitId,
    occasionKey = occasionKey,
    type = type.name,
)

private fun ProtectionEvent.toDto() = StreakProtectionEventDto(
    habitId = habitId,
    occasionKey = occasionKey,
    eventType = type.name,
)

private fun StreakProtectionEventDto.toEvent() = ProtectionEvent(
    habitId = habitId,
    occasionKey = occasionKey,
    type = if (eventType == ProtectionEventType.GRANT.name) {
        ProtectionEventType.GRANT
    } else {
        ProtectionEventType.CONSUME
    },
)
