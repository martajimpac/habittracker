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
    fun `clear removes challenge prefs for widget`() = runTest {
        dataSource.setChallengeWidget(
            appWidgetId = 3,
            challengeId = "challenge-9",
            snapshotJson = """{"done":true}""",
        )

        dataSource.clearWidget(appWidgetId = 3)

        assertNull(dataSource.getChallengeWidget(appWidgetId = 3))
    }
}
