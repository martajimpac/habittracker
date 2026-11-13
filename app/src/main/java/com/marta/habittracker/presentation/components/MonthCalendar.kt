package com.marta.habittracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.marta.habittracker.R
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitOutline
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CardShape = RoundedCornerShape(20.dp)

/** Monday-aligned dates covering [month], including leading and trailing days. */
fun monthCalendarDates(month: YearMonth): List<LocalDate> {
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val start = first.minusDays(leading.toLong())
    val cellCount = (((leading + month.lengthOfMonth()) + 6) / 7) * 7
    return List(cellCount) { offset -> start.plusDays(offset.toLong()) }
}

fun monthCalendarTitle(month: YearMonth, locale: Locale = Locale.getDefault()): String {
    val raw = month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
    return raw.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(locale) else char.toString()
    }
}

/**
 * A day can be marked or unmarked when it belongs to the shown [month] and is not after [today].
 * Past days are never locked.
 */
fun isMonthCalendarDayEditable(date: LocalDate, month: YearMonth, today: LocalDate): Boolean =
    YearMonth.from(date) == month && !date.isAfter(today)

@Composable
fun MonthCalendar(
    month: YearMonth,
    completedDates: Set<LocalDate>,
    accentColor: Color,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    onDayClick: ((LocalDate) -> Unit)? = null,
    pendingDates: Set<LocalDate> = emptySet(),
) {
    val dates = remember(month) { monthCalendarDates(month) }
    val title = remember(month) { monthCalendarTitle(month) }
    val dayLabels = listOf(
        stringResource(R.string.activity_day_mon),
        stringResource(R.string.activity_day_tue),
        stringResource(R.string.activity_day_wed),
        stringResource(R.string.activity_day_thu),
        stringResource(R.string.activity_day_fri),
        stringResource(R.string.activity_day_sat),
        stringResource(R.string.activity_day_sun),
    )
    val onAccent = if (accentColor.luminance() > 0.6f) HabitOnSurface else Color.White

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, HabitOutline, CardShape)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.detail_calendar_previous_month),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = title,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.detail_calendar_next_month),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        dates.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    MonthDayCell(
                        date = date,
                        inMonth = YearMonth.from(date) == month,
                        completed = date in completedDates,
                        isToday = date == today,
                        accentColor = accentColor,
                        onAccent = onAccent,
                        onClick = onDayClick
                            ?.takeIf { isMonthCalendarDayEditable(date, month, today) }
                            ?.let { click -> { click(date) } },
                        isPending = date in pendingDates,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    date: LocalDate,
    inMonth: Boolean,
    completed: Boolean,
    isToday: Boolean,
    accentColor: Color,
    onAccent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isPending: Boolean = false,
) {
    val stateDescription = if (inMonth && onClick != null) {
        val label = date.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
        stringResource(
            if (completed) R.string.detail_calendar_day_done else R.string.detail_calendar_day_not_done,
            label,
        )
    } else {
        null
    }
    val actionLabel = stringResource(R.string.detail_calendar_toggle_action)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(CircleShape)
                        .clickable(
                            enabled = !isPending,
                            onClickLabel = actionLabel,
                            role = Role.Button,
                            onClick = onClick,
                        )
                        .clearAndSetSemantics { stateDescription?.let { contentDescription = it } }
                } else {
                    Modifier
                },
            )
            .alpha(if (isPending) 0.5f else 1f),
        contentAlignment = Alignment.Center,
    ) {
        if (inMonth && completed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(accentColor),
            )
        } else if (inMonth && isToday) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, accentColor, CircleShape),
            )
        }
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (inMonth && (completed || isToday)) FontWeight.Bold else FontWeight.Normal,
            color = when {
                !inMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                completed -> onAccent
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

// @Preview(showBackground = true, showSystemUi = true)
// @Composable
// private fun MonthCalendarPreview(){
    // MonthCalendar(
        // month = YearMonth.now(),
        // completedDates = emptySet(),
        // accentColor = Color.Red,
        // onPreviousMonth = {},
        // onNextMonth = {},
        // modifier = Modifier.fillMaxSize(),
    // )
// }