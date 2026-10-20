package com.marta.habittracker.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.marta.habittracker.presentation.theme.HabitDanger
import kotlin.math.cos
import kotlin.math.sin

enum class HabitDialogTone {
    Positive,
    Negative,
}

@Composable
fun HabitDialog(
    onDismissRequest: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    tone: HabitDialogTone? = null,
    dismissText: String? = null,
    confirmVariant: HabitButtonVariant = if (tone == HabitDialogTone.Negative) {
        HabitButtonVariant.Destructive
    } else {
        HabitButtonVariant.Primary
    },
    content: @Composable (() -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (tone != null) {
                    DialogFlower(bloomed = tone == HabitDialogTone.Positive)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
                if (message != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                if (content != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    content()
                }
                Spacer(modifier = Modifier.height(20.dp))
                if (dismissText == null) {
                    HabitButton(
                        text = confirmText,
                        onClick = onConfirm,
                        variant = confirmVariant,
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        HabitButton(
                            text = dismissText,
                            onClick = onDismissRequest,
                            modifier = Modifier.weight(1f),
                            variant = HabitButtonVariant.Secondary,
                            fillMaxWidth = false,
                            compact = true,
                        )
                        HabitButton(
                            text = confirmText,
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            variant = confirmVariant,
                            fillMaxWidth = false,
                            compact = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogFlower(
    bloomed: Boolean,
    modifier: Modifier = Modifier,
) {
    val stemColor = if (bloomed) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val petalColor = if (bloomed) HabitDanger else HabitDanger.copy(alpha = 0.45f)
    val centerColor = if (bloomed) BloomYellow else MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = modifier.size(width = 72.dp, height = 96.dp)) {
        if (bloomed) {
            drawUprightFlower(stemColor, petalColor, centerColor)
        } else {
            drawFallenFlower(stemColor, petalColor, centerColor)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawUprightFlower(
    stemColor: Color,
    petalColor: Color,
    centerColor: Color,
) {
    val ground = Offset(size.width / 2f, size.height * 0.92f)
    val head = Offset(size.width / 2f, size.height * 0.28f)
    val stem = Path().apply {
        moveTo(ground.x, ground.y)
        cubicTo(
            ground.x - size.width * 0.08f,
            size.height * 0.7f,
            head.x + size.width * 0.06f,
            size.height * 0.48f,
            head.x,
            head.y,
        )
    }
    drawPath(
        path = stem,
        color = stemColor,
        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
    )
    drawOval(
        color = stemColor,
        topLeft = Offset(size.width * 0.18f, size.height * 0.52f),
        size = Size(size.width * 0.22f, size.height * 0.12f),
    )
    drawOval(
        color = stemColor,
        topLeft = Offset(size.width * 0.58f, size.height * 0.46f),
        size = Size(size.width * 0.22f, size.height * 0.12f),
    )
    drawBloom(head, petalColor, centerColor, petalScale = 1f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFallenFlower(
    stemColor: Color,
    petalColor: Color,
    centerColor: Color,
) {
    val ground = Offset(size.width * 0.28f, size.height * 0.9f)
    val head = Offset(size.width * 0.72f, size.height * 0.72f)
    val stem = Path().apply {
        moveTo(ground.x, ground.y)
        cubicTo(
            size.width * 0.4f,
            size.height * 0.55f,
            size.width * 0.55f,
            size.height * 0.5f,
            head.x,
            head.y,
        )
    }
    drawPath(
        path = stem,
        color = stemColor,
        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
    )
    drawOval(
        color = stemColor.copy(alpha = 0.7f),
        topLeft = Offset(size.width * 0.34f, size.height * 0.58f),
        size = Size(size.width * 0.16f, size.height * 0.08f),
    )
    drawBloom(head, petalColor, centerColor, petalScale = 0.72f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBloom(
    head: Offset,
    petalColor: Color,
    centerColor: Color,
    petalScale: Float,
) {
    val petalRadius = size.minDimension * 0.16f * petalScale
    val orbit = size.minDimension * 0.18f * petalScale
    repeat(5) { index ->
        val angle = Math.toRadians(-90.0 + index * 72.0)
        drawCircle(
            color = petalColor,
            radius = petalRadius,
            center = Offset(
                x = head.x + cos(angle).toFloat() * orbit,
                y = head.y + sin(angle).toFloat() * orbit,
            ),
        )
    }
    drawCircle(
        color = centerColor,
        radius = size.minDimension * 0.09f * petalScale,
        center = head,
    )
}

private val BloomYellow = Color(0xFFFFB938)
