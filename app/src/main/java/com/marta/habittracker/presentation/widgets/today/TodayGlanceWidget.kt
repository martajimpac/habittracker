package com.marta.habittracker.presentation.widgets.today

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.action.clickable
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.marta.habittracker.R
import com.marta.habittracker.domain.model.Habit
import com.marta.habittracker.presentation.theme.HabitOnSurfaceVariant
import com.marta.habittracker.presentation.utils.parseHabitColor
import com.marta.habittracker.presentation.widgets.WidgetCheck
import com.marta.habittracker.presentation.widgets.WidgetEntryPoint
import com.marta.habittracker.presentation.widgets.WidgetHabitRow
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras
import com.marta.habittracker.presentation.widgets.WidgetLoad
import com.marta.habittracker.presentation.widgets.WidgetLoading
import com.marta.habittracker.presentation.widgets.WidgetScrollingScreen
import com.marta.habittracker.presentation.widgets.widgetDateLabel
import com.marta.habittracker.presentation.widgets.widgetDayName
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

class TodayGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d("AAAAAA", "STARTED")


        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            WidgetEntryPoint::class.java
        )

        Log.d(TAG, "2. EntryPoint obtained")

        provideContent {
            Log.d("AAAAAA", "EXECUTED")
            Log.d(TAG, "provideContent composition started")

            val habits by entryPoint.habitRepository()
                .getAllHabitsWithRecords()
                .collectAsState(initial = emptyList())

            Log.d(TAG, "Habits received: ${habits.size}")

            val today = LocalDate.now()

            val snapshot = todayWidgetSnapshot(
                habits = habits,
                today = today
            )

            Log.d(TAG, "Snapshot created")
            Log.d(TAG, "Items count: ${snapshot.items.size}")
            Log.d(TAG, "First pending ID: ${snapshot.firstPendingId}")

            snapshot.items.forEachIndexed { index, item ->
                Log.d(
                    TAG,
                    """
            Item [$index]
            ID: ${item.id}
            Name: ${item.name}
            Completed: ${item.completed}
            Color: ${item.colorHex}
            Icon: ${item.iconKey}
            """.trimIndent()
                )
            }

            val countText = if (snapshot.items.isEmpty()) {
                null
            } else {
                context.getString(
                    R.string.today_widget_count,
                    snapshot.items.count { it.completed },
                    snapshot.items.size
                )
            }

            Log.d(TAG, "Count text: $countText")

            TodayWidgetContent(
                dayName = widgetDayName(today),
                dateLabel = context.getString(
                    R.string.home_today_label,
                    widgetDateLabel(today)
                ),
                countText = countText,
                rows = snapshot.items,
                emptyText = context.getString(R.string.today_widget_empty),
                openAppText = context.getString(R.string.widget_open_app),
                openAppAction = todayWidgetOpenAction(
                    context,
                    snapshot.firstPendingId
                )
            )

            Log.d(TAG, "TodayWidgetContent invoked")
        }
    }


    private companion object {
        const val TAG = "WidgetRefresher TodayWidget"


    }
}

internal fun todayWidgetOpenAction(context: Context, firstPendingId: String?): Action =
    actionStartActivity(
        WidgetLaunchExtras.openHomeIntent(context, firstPendingId),
    )

@SuppressLint("RestrictedApi")
@Composable
internal fun TodayWidgetContent(
    dayName: String,
    dateLabel: String,
    countText: String?,
    rows: List<TodayWidgetItem>,
    emptyText: String,
    openAppText: String,
    openAppAction: Action,
) {
    Log.d(
        "WidgetRefresher TodayWidgetContent",
        """
        ========== TODAY WIDGET DATA ==========
        dayName: $dayName
        dateLabel: $dateLabel
        countText: $countText
        emptyText: $emptyText
        openAppText: $openAppText
        rows.size: ${rows.size}
        
        ROWS:
        ${rows.mapIndexed { index, row ->
            """
            [$index]
              id: ${row.id}
              name: ${row.name}
              iconKey: ${row.iconKey}
              colorHex: ${row.colorHex}
              completed: ${row.completed}
            """.trimIndent()
        }.joinToString("\n")}
        =======================================
        """.trimIndent()
    )


    val subtitle = listOfNotNull(dateLabel, countText).joinToString("  ")
    WidgetScrollingScreen(
        headerTitle = dayName,
        headerSubtitle = subtitle,
        openAppAction = openAppAction,
        openAppButtonText = openAppText,
    ) {
        if (rows.isEmpty()) {
            item {
                Text(
                    text = emptyText,
                    modifier = GlanceModifier.clickable(openAppAction).padding(vertical = 4.dp),
                    style = TextStyle(
                        color = ColorProvider(HabitOnSurfaceVariant)
                    ),
                )
            }
        } else {
            items(rows) { row ->
                val accent = parseHabitColor(row.colorHex)
                val toggleAction = actionRunCallback<ToggleTodayHabitAction>(
                    actionParametersOf(ToggleTodayHabitAction.habitIdKey to row.id),
                )
                WidgetHabitRow(
                    name = row.name,
                    iconKey = row.iconKey,
                    accent = accent,
                    completed = row.completed,
                    detail = null,
                    onClick = openAppAction,
                    trailing = {
                        WidgetCheck(
                            completed = row.completed,
                            accent = accent,
                            onToggle = toggleAction,
                        )
                    },
                )
            }
        }
    }
}
