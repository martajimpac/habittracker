package com.marta.habittracker.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.marta.habittracker.presentation.theme.HabitField
import com.marta.habittracker.presentation.theme.HabitOnSurface
import com.marta.habittracker.presentation.theme.HabitOnSurfaceVariant
import com.marta.habittracker.presentation.theme.HabitOutline
import com.marta.habittracker.presentation.theme.habitDangerColors

enum class HabitButtonVariant {
    Primary,
    PrimaryLight,
    GhostPill,
    Secondary,
    TextLink,
    Destructive,
}

private val HabitButtonShape = RoundedCornerShape(16.dp)
private val HabitButtonHeight = 56.dp

@Composable
fun HabitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: HabitButtonVariant = HabitButtonVariant.Primary,
    loading: Boolean = false,
    fillMaxWidth: Boolean = true,
    compact: Boolean = false,
    icon: ImageVector? = null,
    clickableWhenDisabled: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val disabledContainer = colors.surfaceContainerHighest
    val disabledContent = colors.onSurfaceVariant
    val onPrimary = colors.onPrimary
    val field = colors.surfaceContainerLowest
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.98f else 1f,
        label = "habitButtonScale",
    )

    val isLarge = !compact && variant != HabitButtonVariant.GhostPill && variant != HabitButtonVariant.TextLink
    val buttonModifier = modifier
        .scale(scale)
        .then(
            if (fillMaxWidth && variant != HabitButtonVariant.TextLink && variant != HabitButtonVariant.GhostPill) {
                Modifier.fillMaxWidth()
            } else {
                Modifier
            },
        )
        .then(if (isLarge) Modifier.defaultMinSize(minHeight = HabitButtonHeight) else Modifier)

    when (variant) {
        HabitButtonVariant.Primary -> {
            val active = enabled && !loading
            val interactive = (enabled || clickableWhenDisabled) && !loading
            Button(
                onClick = onClick,
                enabled = interactive,
                modifier = if (isLarge) buttonModifier.height(HabitButtonHeight) else buttonModifier,
                shape = HabitButtonShape,
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (active) colors.primary else disabledContainer,
                    disabledContainerColor = disabledContainer,
                    contentColor = if (active) onPrimary else disabledContent,
                    disabledContentColor = disabledContent,
                ),
            ) {
                HabitButtonLabel(
                    text = text,
                    icon = icon,
                    color = if (active) onPrimary else disabledContent,
                )
            }
        }

        HabitButtonVariant.PrimaryLight -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = if (isLarge) buttonModifier.height(HabitButtonHeight) else buttonModifier,
                shape = HabitButtonShape,
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = field,
                    contentColor = colors.primary,
                    disabledContainerColor = disabledContainer,
                    disabledContentColor = disabledContent,
                ),
            ) {
                HabitButtonLabel(
                    text = text,
                    icon = icon,
                    color = if (enabled) colors.primary else disabledContent,
                )
            }
        }

        HabitButtonVariant.GhostPill -> {
            TextButton(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier
                    .clip(CircleShape)
                    .background(
                        if (enabled) onPrimary.copy(alpha = 0.2f) else onPrimary.copy(alpha = 0.12f),
                    ),
                interactionSource = interactionSource,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = onPrimary,
                    disabledContentColor = disabledContent,
                ),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (enabled) onPrimary else disabledContent,
                )
            }
        }

        HabitButtonVariant.Secondary -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = if (isLarge) buttonModifier.height(HabitButtonHeight) else buttonModifier,
                shape = HabitButtonShape,
                interactionSource = interactionSource,
                border = BorderStroke(
                    1.dp,
                    if (enabled) HabitOutline else disabledContainer,
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HabitField,
                    contentColor = HabitOnSurface,
                    disabledContainerColor = disabledContainer,
                    disabledContentColor = disabledContent,
                ),
            ) {
                HabitButtonLabel(
                    text = text,
                    icon = icon,
                    color = if (enabled) HabitOnSurface else disabledContent,
                )
            }
        }

        HabitButtonVariant.Destructive -> {
            val danger = habitDangerColors()
            val active = enabled && !loading
            val interactive = (enabled || clickableWhenDisabled) && !loading
            Button(
                onClick = onClick,
                enabled = interactive,
                modifier = if (isLarge) buttonModifier.height(HabitButtonHeight) else buttonModifier,
                shape = HabitButtonShape,
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (active) danger.button else disabledContainer,
                    disabledContainerColor = disabledContainer,
                    contentColor = if (active) danger.onButton else disabledContent,
                    disabledContentColor = disabledContent,
                ),
            ) {
                HabitButtonLabel(
                    text = text,
                    icon = icon,
                    color = if (active) danger.onButton else disabledContent,
                )
            }
        }

        HabitButtonVariant.TextLink -> {
            TextButton(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier,
                interactionSource = interactionSource,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.primary,
                    disabledContentColor = HabitOnSurfaceVariant,
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (enabled) colors.primary else HabitOnSurfaceVariant,
                    textAlign = TextAlign.End,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
    }
}

@Composable
private fun HabitButtonLabel(
    text: String,
    icon: ImageVector?,
    color: Color,
) {
    if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = color,
        )
        Spacer(Modifier.width(6.dp))
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = Color.Unspecified,
    iconTint: Color = Color.Unspecified,
) {
    val colors = MaterialTheme.colorScheme
    val resolvedBackground = when {
        backgroundColor != Color.Unspecified -> backgroundColor
        enabled -> colors.primary
        else -> colors.surfaceContainerHighest
    }
    val resolvedTint = when {
        iconTint != Color.Unspecified -> iconTint
        enabled -> colors.onPrimary
        else -> colors.onSurfaceVariant
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.95f else 1f,
        label = "habitIconButtonScale",
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .scale(scale)
            .size(40.dp),
        shape = HabitButtonShape,
        color = resolvedBackground,
        interactionSource = interactionSource,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = resolvedTint,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitFab(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.95f else 1f,
        label = "habitFabScale",
    )
    val fabShape: Shape = HabitButtonShape

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .scale(scale)
            .size(56.dp),
        shape = fabShape,
        shadowElevation = 8.dp,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
