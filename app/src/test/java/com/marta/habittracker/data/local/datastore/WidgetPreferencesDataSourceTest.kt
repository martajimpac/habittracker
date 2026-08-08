package com.marta.habittracker.data.local.datastore

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], application = Application::class)
class WidgetPreferencesDataSourceTest {

    private lateinit var context: Context
    private lateinit var dataSource: WidgetPreferencesDataSource

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataSource = WidgetPreferencesDataSource(context)
    }

    @Test
    fun `set and get habit widget habit id`() = runTest {
        dataSource.setHabitWidgetHabitId(appWidgetId = 1, habitId = "habit-42")

        assertEquals("habit-42", dataSource.getHabitWidgetHabitId(appWidgetId = 1))
    }

    @Test
    fun `set and get challenge widget prefs`() = runTest {
        dataSource.setChallengeWidget(
            appWidgetId = 2,
            challengeId = "challenge-7",
            snapshotJson = """{"progress":0.5}""",
        )

        val prefs = dataSource.getChallengeWidget(appWidgetId = 2)

        assertEquals(
            ChallengeWidgetPrefs(
                challengeId = "challenge-7",
                snapshotJson = """{"progress":0.5}""",
            ),
            prefs,
        )
    }

    @Test
    fun `clear removes habit and challenge prefs for widget`() = runTest {
        dataSource.setHabitWidgetHabitId(appWidgetId = 3, habitId = "habit-99")
        dataSource.setChallengeWidget(
            appWidgetId = 3,
            challengeId = "challenge-9",
            snapshotJson = """{"done":true}""",
        )

        dataSource.clearWidget(appWidgetId = 3)

        assertNull(dataSource.getHabitWidgetHabitId(appWidgetId = 3))
        assertNull(dataSource.getChallengeWidget(appWidgetId = 3))
    }

    @Test
    fun `habit widget ids are isolated per appWidgetId`() = runTest {
        dataSource.setHabitWidgetHabitId(appWidgetId = 10, habitId = "habit-a")
        dataSource.setHabitWidgetHabitId(appWidgetId = 11, habitId = "habit-b")

        assertEquals("habit-a", dataSource.getHabitWidgetHabitId(appWidgetId = 10))
        assertEquals("habit-b", dataSource.getHabitWidgetHabitId(appWidgetId = 11))

        dataSource.clearWidget(appWidgetId = 10)

        assertNull(dataSource.getHabitWidgetHabitId(appWidgetId = 10))
        assertEquals("habit-b", dataSource.getHabitWidgetHabitId(appWidgetId = 11))
    }
}
