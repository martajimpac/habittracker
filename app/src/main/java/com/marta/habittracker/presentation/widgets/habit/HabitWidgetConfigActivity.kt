package com.marta.habittracker.presentation.widgets.habit

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.marta.habittracker.R
import com.marta.habittracker.data.local.datastore.WidgetPreferencesDataSource
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.domain.repository.HabitRepository
import com.marta.habittracker.presentation.components.HabitLineIcon
import com.marta.habittracker.presentation.screens.home.parseHabitColor
import com.marta.habittracker.presentation.theme.InstaDevTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HabitWidgetConfigActivity : ComponentActivity() {

    @Inject
    lateinit var habitRepository: HabitRepository

    @Inject
    lateinit var widgetPreferencesDataSource: WidgetPreferencesDataSource

    private var habits by mutableStateOf<List<Habit>>(emptyList())
    private var isLoading by mutableStateOf(true)
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            InstaDevTheme {
                HabitWidgetConfigContent(
                    habits = habits,
                    isLoading = isLoading,
                    onHabitSelected = ::configureWidget,
                )
            }
        }

        lifecycleScope.launch {
            habits = habitRepository.getAllHabitsWithRecords().first()
            isLoading = false
        }
    }

    private fun configureWidget(habit: Habit) {
        lifecycleScope.launch {
            widgetPreferencesDataSource.setHabitWidgetHabitId(appWidgetId, habit.id)
            val glanceId = GlanceAppWidgetManager(this@HabitWidgetConfigActivity)
                .getGlanceIdBy(appWidgetId)
            HabitGlanceWidget().update(this@HabitWidgetConfigActivity, glanceId)
            setResult(
                RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
            )
            finish()
        }
    }
}

@androidx.compose.runtime.Composable
private fun HabitWidgetConfigContent(
    habits: List<Habit>,
    isLoading: Boolean,
    onHabitSelected: (Habit) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.habit_widget_config_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        if (!isLoading && habits.isEmpty()) {
            Text(text = stringResource(R.string.habit_widget_empty_config))
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(habits, key = Habit::id) { habit ->
                    ListItem(
                        headlineContent = { Text(habit.name) },
                        leadingContent = {
                            HabitLineIcon(
                                habit = habit,
                                tint = parseHabitColor(habit.colorHex),
                            )
                        },
                        modifier = Modifier.clickable { onHabitSelected(habit) },
                    )
                }
            }
        }
    }
}
