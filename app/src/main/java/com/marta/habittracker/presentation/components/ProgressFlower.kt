package com.marta.habittracker.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marta.habittracker.presentation.theme.HabitDanger
import kotlin.math.cos
import kotlin.math.sin

private const val PETAL_COUNT = 5
private val BloomYellow = Color(0xFFFFB938)

fun petalFillAmounts(progress: Float, petalCount: Int = PETAL_COUNT): List<Float> {
    val scaled = progress.coerceIn(0f, 1f) * petalCount
    return List(petalCount) { index ->
        (scaled - index).coerceIn(0f, 1f)
    }
}

@Composable
fun ProgressFlower(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450),
        label = "progress_flower",
    )
    Canvas(modifier = modifier.size(size)) {
        val amounts = petalFillAmounts(animated)
        val petalRadius = this.size.minDimension * 0.22f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        amounts.forEachIndexed { index, amount ->
            val angle = Math.toRadians((index * 72.0) - 90.0)
            val petalCenter = Offset(
                center.x + cos(angle).toFloat() * petalRadius * 0.95f,
                center.y + sin(angle).toFloat() * petalRadius * 0.95f,
            )
            drawCircle(
                color = HabitDanger.copy(alpha = 0.18f),
                radius = petalRadius,
                center = petalCenter,
            )
            if (amount > 0f) {
                drawCircle(
                    color = HabitDanger,
                    radius = petalRadius * amount,
                    center = petalCenter,
                )
            }
        }
        drawCircle(
            color = BloomYellow,
            radius = petalRadius * 0.7f,
            center = center,
        )
    }
}
