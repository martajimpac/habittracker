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
import androidx.compose.ui.unit.dp
import com.marta.habittracker.R
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitOnSurfaceVariant
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
    emptyColor: Color = Color.White,
    filledColor: Color = HabitPrimary,
) {
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
            .fillMaxWidth()
            .clip(CardShape)
            .background(Color.White)
            .border(1.dp, HabitOutline, CardShape)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.activity_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = HabitOnSurface,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(28.dp))
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = HabitOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.activity_week_label, week.weekIndex),
                    modifier = Modifier.width(24.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = HabitOnSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                week.cells.forEach { cell ->
                    val cellColor = lerp(emptyColor, filledColor, cell.intensity)
                    val isToday = cell.date == today
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CellShape)
                            .background(cellColor)
                            .then(
                                if (isToday) {
                                    Modifier.border(1.5.dp, HabitPrimary, CellShape)
                                } else {
                                    Modifier.border(1.dp, HabitOutline.copy(alpha = 0.6f), CellShape)
                                },
                            ),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.activity_less),
                style = MaterialTheme.typography.labelSmall,
                color = HabitOnSurfaceVariant,
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
                color = HabitOnSurfaceVariant,
            )
        }
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
