package com.marta.habittracker.presentation.widgets.today

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.marta.habittracker.R
import com.marta.habittracker.presentation.theme.HabitOnSurfaceVariant
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitPrimary
import com.marta.habittracker.presentation.theme.HabitSurface
import com.marta.habittracker.presentation.widgets.WidgetEntryPoint
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.flow.first

class TodayGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val snapshot = todayWidgetSnapshot(
            habits = entryPoint.habitRepository().getAllHabitsWithRecords().first(),
            today = LocalDate.now(),
        )
        val display = todayWidgetDisplay(snapshot.items)
        val overflowText = if (display.overflowCount == 0) {
            null
        } else {
            context.getString(R.string.today_widget_more, display.overflowCount)
        }

        provideContent {
            TodayWidgetContent(
                title = context.getString(R.string.today_widget_title),
                rows = display.rows,
                emptyText = context.getString(R.string.today_widget_empty),
                pendingFormat = context.getString(R.string.today_widget_pending),
                doneFormat = context.getString(R.string.today_widget_done),
                overflowText = overflowText,
                openAppAction = actionStartActivity(
                    WidgetLaunchExtras.openHomeIntent(context, snapshot.firstPendingId),
                ),
            )
        }
    }
}

@Composable
internal fun TodayWidgetContent(
    title: String,
    rows: List<TodayWidgetItem>,
    emptyText: String,
    pendingFormat: String,
    doneFormat: String,
    overflowText: String?,
    openAppAction: Action,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(HabitSurface)
            .clickable(openAppAction)
            .padding(16.dp),
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                color = ColorProvider(HabitPrimary),
            ),
        )
        if (rows.isEmpty()) {
            Text(
                text = emptyText,
                modifier = GlanceModifier.padding(top = 8.dp),
                style = TextStyle(color = ColorProvider(HabitOnSurfaceVariant)),
            )
        } else {
            rows.forEach { row ->
                val format = if (row.completed) doneFormat else pendingFormat
                Text(
                    text = String.format(Locale.getDefault(), format, row.name),
                    modifier = GlanceModifier.padding(top = 6.dp),
                    style = TextStyle(color = ColorProvider(HabitOnSurface)),
                )
            }
            if (overflowText != null) {
                Text(
                    text = overflowText,
                    modifier = GlanceModifier.padding(top = 6.dp),
                    style = TextStyle(color = ColorProvider(HabitOnSurfaceVariant)),
                )
            }
        }
    }
}
