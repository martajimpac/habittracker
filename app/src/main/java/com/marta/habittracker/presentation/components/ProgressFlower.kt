package com.marta.habittracker.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.marta.habittracker.presentation.theme.HabitDanger
import kotlin.math.cos
import kotlin.math.sin

private val BloomYellow = Color(0xFFFFB938)

data class FlowerGrowth(
    val stem: Float,
    val leaves: Float,
    val bloom: Float,
)

fun flowerGrowth(progress: Float): FlowerGrowth {
    val p = progress.coerceIn(0f, 1f)
    return FlowerGrowth(
        stem = (p / 0.6f).coerceIn(0f, 1f),
        leaves = ((p - 0.25f) / 0.5f).coerceIn(0f, 1f),
        bloom = ((p - 0.5f) / 0.5f).coerceIn(0f, 1f),
    )
}

@Composable
fun ProgressFlower(
    progress: Float,
    modifier: Modifier = Modifier,
    stemColor: Color = MaterialTheme.colorScheme.primary,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 550),
        label = "progress_flower",
    )
    Canvas(modifier = modifier.size(width = 56.dp, height = 92.dp)) {
        val growth = flowerGrowth(animated)
        val width = size.width
        val height = size.height
        val ground = height * 0.96f
        val flowerY = height * 0.24f
        val tipY = ground + (flowerY - ground) * growth.stem

        if (growth.stem > 0.02f) {
            val stem = Path().apply {
                moveTo(width * 0.5f, ground)
                cubicTo(
                    width * 0.46f,
                    ground + (tipY - ground) * 0.35f,
                    width * 0.58f,
                    ground + (tipY - ground) * 0.72f,
                    width * 0.5f,
                    tipY,
                )
            }
            drawPath(
                path = stem,
                color = stemColor,
                style = Stroke(width = width * 0.07f, cap = StrokeCap.Round),
            )
        }

        if (growth.leaves > 0f) {
            val leafWidth = width * 0.36f * growth.leaves
            val leafHeight = height * 0.08f * growth.leaves
            val lower = ground + (tipY - ground) * 0.62f
            val upper = ground + (tipY - ground) * 0.4f
            drawOval(
                color = stemColor,
                topLeft = Offset(width * 0.5f - leafWidth, lower - leafHeight / 2f),
                size = Size(leafWidth, leafHeight),
            )
            drawOval(
                color = stemColor,
                topLeft = Offset(width * 0.5f, upper - leafHeight / 2f),
                size = Size(leafWidth, leafHeight),
            )
        }

        if (growth.bloom > 0f) {
            val bloomCenter = Offset(width * 0.5f, tipY)
            val petalRadius = width * 0.2f * growth.bloom
            repeat(5) { index ->
                val angle = Math.toRadians((index * 72.0) - 90.0)
                drawCircle(
                    color = HabitDanger,
                    radius = petalRadius,
                    center = Offset(
                        bloomCenter.x + cos(angle).toFloat() * petalRadius * 0.82f,
                        bloomCenter.y + sin(angle).toFloat() * petalRadius * 0.82f,
                    ),
                )
            }
            drawCircle(
                color = BloomYellow,
                radius = petalRadius * 0.5f,
                center = bloomCenter,
            )
        }
    }
}

@Composable
fun WiltedFlower(modifier: Modifier = Modifier) {
    val stemColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val petalColor = HabitDanger.copy(alpha = 0.5f)
    val centerColor = BloomYellow.copy(alpha = 0.65f)
    Canvas(modifier = modifier.size(width = 72.dp, height = 88.dp)) {
        val ground = Offset(size.width * 0.38f, size.height * 0.94f)
        val head = Offset(size.width * 0.7f, size.height * 0.58f)
        val stem = Path().apply {
            moveTo(ground.x, ground.y)
            cubicTo(
                size.width * 0.4f,
                size.height * 0.42f,
                size.width * 0.78f,
                size.height * 0.22f,
                head.x,
                head.y,
            )
        }
        drawPath(
            path = stem,
            color = stemColor,
            style = Stroke(width = size.width * 0.06f, cap = StrokeCap.Round),
        )
        drawOval(
            color = stemColor,
            topLeft = Offset(size.width * 0.22f, size.height * 0.5f),
            size = Size(size.width * 0.2f, size.height * 0.07f),
        )
        val petalRadius = size.minDimension * 0.11f
        val orbit = size.minDimension * 0.13f
        repeat(5) { index ->
            val angle = Math.toRadians(30.0 + index * 72.0)
            val hang = if (index == 2) 1.35f else 1f
            drawCircle(
                color = petalColor,
                radius = petalRadius,
                center = Offset(
                    x = head.x + cos(angle).toFloat() * orbit,
                    y = head.y + sin(angle).toFloat() * orbit * hang,
                ),
            )
        }
        drawCircle(
            color = centerColor,
            radius = petalRadius * 0.55f,
            center = head,
        )
    }
}
