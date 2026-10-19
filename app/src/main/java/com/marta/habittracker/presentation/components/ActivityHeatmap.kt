package com.marta.habittracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marta.habittracker.R
import com.marta.habittracker.presentation.theme.HabitOutline
import com.marta.habittracker.presentation.theme.HabitPrimary
import java.time.LocalDate

private val CellShape = RoundedCornerShape(6.dp)
private val CardShape = RoundedCornerShape(20.dp)

@Composable
fun ActivityHeatmap(
    weeks: List<ActivityHeatmapWeek>,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    emptyColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    filledColor: Color = HabitPrimary,
    showChrome: Boolean = true,
    cellSize: Dp = Dp.Unspecified,
) {
    val compact = cellSize != Dp.Unspecified
    val gap = if (compact) 3.dp else 4.dp
    val weekLabelWidth = if (compact) 18.dp else 24.dp
    val cellShape = if (compact) RoundedCornerShape(3.dp) else CellShape
    val labelStyle = if (compact) {
        MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp)
    } else {
        MaterialTheme.typography.labelSmall
    }
    val dayLabels = listOf(
        stringResource(R.string.activity_day_mon),
        stringResource(R.string.activity_day_tue),
        stringResource(R.string.activity_day_wed),
        stringResource(R.string.activity_day_thu),
        stringResource(R.string.activity_day_fri),
        stringResource(R.string.activity_day_sat),
        stringResource(R.string.activity_day_sun),
    )

    Column(
        modifier = modifier
            .then(if (compact) Modifier else Modifier.fillMaxWidth())
            .then(
                if (showChrome) {
                    Modifier
                        .clip(CardShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .border(1.dp, HabitOutline, CardShape)
                        .padding(16.dp)
                } else {
                    Modifier
                },
            ),
    ) {
        if (showChrome) {
            Text(
                text = stringResource(R.string.activity_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            modifier = if (compact) Modifier else Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            Spacer(modifier = Modifier.width(weekLabelWidth))
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = if (compact) Modifier.width(cellSize) else Modifier.weight(1f),
                    style = labelStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(modifier = Modifier.height(if (compact) 4.dp else 6.dp))

        weeks.forEach { week ->
            Row(
                modifier = if (compact) Modifier else Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                Text(
                    text = stringResource(R.string.activity_week_label, week.weekIndex),
                    modifier = Modifier.width(weekLabelWidth),
                    style = labelStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                week.cells.forEach { cell ->
                    val cellColor = lerp(emptyColor, filledColor, cell.intensity)
                    val isToday = cell.date == today
                    Box(
                        modifier = Modifier
                            .then(
                                if (compact) {
                                    Modifier.size(cellSize)
                                } else {
                                    Modifier.weight(1f).aspectRatio(1f)
                                },
                            )
                            .clip(cellShape)
                            .background(cellColor)
                            .then(
                                if (isToday) {
                                    Modifier.border(if (compact) 1.dp else 1.5.dp, HabitPrimary, cellShape)
                                } else {
                                    Modifier.border(1.dp, HabitOutline.copy(alpha = 0.6f), cellShape)
                                },
                            ),
                    )
                }
            }
            Spacer(modifier = Modifier.height(gap))
        }

        if (showChrome) {
            Spacer(modifier = Modifier.height(4.dp))
            HeatmapLegend(emptyColor = emptyColor, filledColor = filledColor)
        }
    }
}

@Composable
private fun HeatmapLegend(emptyColor: Color, filledColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
            Text(
                text = stringResource(R.string.activity_less),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(6.dp))
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { step ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(lerp(emptyColor, filledColor, step))
                        .border(1.dp, HabitOutline.copy(alpha = 0.5f), RoundedCornerShape(3.dp)),
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.activity_more),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

@Composable
fun rememberActivityWeeks(
    today: LocalDate = LocalDate.now(),
    intensityForDate: (LocalDate) -> Float,
): List<ActivityHeatmapWeek> {
    return remember(today) {
        toHeatmapWeeks(buildRollingFourWeeks(today), intensityForDate)
    }
}
