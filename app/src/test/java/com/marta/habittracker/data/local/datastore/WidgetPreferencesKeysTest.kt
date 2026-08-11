package com.marta.habittracker.data.local.datastore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WidgetPreferencesKeysTest {

    @Test
    fun `keys include appWidgetId and stay unique across widget types`() {
        val habit = WidgetPreferencesKeys.habitWidgetHabitId(5)
        val challengeId = WidgetPreferencesKeys.challengeWidgetChallengeId(5)
        val snapshot = WidgetPreferencesKeys.challengeWidgetSnapshotJson(5)

        assertEquals("habit_widget_habit_id_5", habit.name)
        assertEquals("challenge_widget_challenge_id_5", challengeId.name)
        assertEquals("challenge_widget_snapshot_json_5", snapshot.name)
        assertNotEquals(habit.name, challengeId.name)
        assertNotEquals(challengeId.name, snapshot.name)
    }

    @Test
    fun `different appWidgetIds produce different keys`() {
        assertNotEquals(
            WidgetPreferencesKeys.habitWidgetHabitId(1).name,
            WidgetPreferencesKeys.habitWidgetHabitId(2).name,
        )
    }
}
