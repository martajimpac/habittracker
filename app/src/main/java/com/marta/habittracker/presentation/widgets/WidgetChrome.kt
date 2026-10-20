package com.marta.habittracker.presentation.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.LazyListScope
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.marta.habittracker.R
import com.marta.habittracker.presentation.components.habitIconRes
import com.marta.habittracker.presentation.theme.HabitCream
import com.marta.habittracker.presentation.theme.HabitField
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitOnSurfaceVariant
import com.marta.habittracker.presentation.theme.HabitOutline
import com.marta.habittracker.presentation.theme.HabitPrimary
import com.marta.habittracker.presentation.theme.HabitSurface
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

internal fun widgetDayName(today: LocalDate): String {
    val locale = Locale.getDefault()
    return today.dayOfWeek.getDisplayName(JavaTextStyle.FULL, locale)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

internal fun widgetDateLabel(today: LocalDate): String {
    val locale = Locale.getDefault()
    val pattern = if (locale.language == "es") "d 'de' MMMM" else "MMMM d"
    return today.format(DateTimeFormatter.ofPattern(pattern, locale))
}

@Composable
internal fun WidgetScreen(
    headerTitle: String,
    headerSubtitle: String?,
    openAppAction: Action,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(HabitSurface))
            .clickable(openAppAction),
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(HabitPrimary))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = headerTitle,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(HabitCream),
                ),
            )
            if (!headerSubtitle.isNullOrBlank()) {
                Text(
                    text = headerSubtitle,
                    modifier = GlanceModifier.padding(top = 2.dp),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(HabitCream),
                    ),
                )
            }
        }
        Column(modifier = GlanceModifier.fillMaxWidth().padding(10.dp)) {
            content()
        }
    }
}

@Composable
internal fun WidgetScrollingScreen(
    headerTitle: String,
    headerSubtitle: String?,
    openAppAction: Action,
    listContent: LazyListScope.() -> Unit,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(HabitSurface)),
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(HabitPrimary))
                .clickable(openAppAction)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = headerTitle,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(HabitCream),
                ),
            )
            if (!headerSubtitle.isNullOrBlank()) {
                Text(
                    text = headerSubtitle,
                    modifier = GlanceModifier.padding(top = 2.dp),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(HabitCream),
                    ),
                )
            }
        }
        LazyColumn(
            modifier = GlanceModifier.defaultWeight().padding(horizontal = 10.dp, vertical = 10.dp),
            content = listContent,
        )
    }
}

@Composable
internal fun WidgetHabitRow(
    name: String,
    iconKey: String,
    accent: Color,
    completed: Boolean,
    detail: String?,
    onClick: Action? = null,
    trailing: @Composable () -> Unit,
) {
    val rowModifier = GlanceModifier
        .fillMaxWidth()
        .padding(bottom = 6.dp)
        .cornerRadius(16.dp)
        .background(ColorProvider(HabitField))
        .padding(horizontal = 10.dp, vertical = 8.dp)
    Row(
        modifier = if (onClick == null) rowModifier else rowModifier.clickable(onClick),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(32.dp)
                .cornerRadius(10.dp)
                .background(ColorProvider(accent.copy(alpha = 0.16f))),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(habitIconRes(iconKey)),
                contentDescription = null,
                modifier = GlanceModifier.size(18.dp),
                colorFilter = ColorFilter.tint(ColorProvider(accent)),
            )
        }
        Column(
            modifier = GlanceModifier.defaultWeight().padding(start = 8.dp),
        ) {
            Text(
                text = name,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(if (completed) HabitOnSurfaceVariant else HabitOnSurface),
                    textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
                ),
                maxLines = 1,
            )
            if (!detail.isNullOrBlank()) {
                Text(
                    text = detail,
                    style = TextStyle(color = ColorProvider(accent)),
                    maxLines = 1,
                )
            }
        }
        trailing()
    }
}

@Composable
internal fun WidgetCheck(completed: Boolean, accent: Color, onToggle: Action? = null) {
    val mark = GlanceModifier
        .size(22.dp)
        .cornerRadius(11.dp)
        .background(ColorProvider(if (completed) accent else HabitOutline.copy(alpha = 0.35f)))
        .let { base -> if (onToggle == null) base else base.clickable(onToggle) }
    Box(
        modifier = mark,
        contentAlignment = Alignment.Center,
    ) {
        if (completed) {
            Text(
                text = "✓",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(HabitCream),
                ),
            )
        }
    }
}

@Composable
internal fun WidgetLoading(text: String) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(HabitSurface))
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        Image(
            provider = ImageProvider(R.drawable.logo),
            contentDescription = null,
            modifier = GlanceModifier.size(width = 148.dp, height = 64.dp),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = text,
            style = TextStyle(
                color = ColorProvider(HabitOnSurfaceVariant),
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
internal fun WidgetProgressBar(progress: Int, accent: Color) {
    val filled = (progress.coerceIn(0, 100) / 10)
    Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)) {
        repeat(10) { index ->
            Spacer(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(6.dp)
                    .padding(horizontal = 1.dp)
                    .cornerRadius(3.dp)
                    .background(
                        ColorProvider(if (index < filled) accent else HabitOutline.copy(alpha = 0.28f)),
                    ),
            )
        }
    }
}
