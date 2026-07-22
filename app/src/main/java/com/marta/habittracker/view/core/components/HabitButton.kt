package com.marta.habittracker.view.core.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marta.habittracker.ui.theme.HabitField
import com.marta.habittracker.ui.theme.HabitOnSurface
import com.marta.habittracker.ui.theme.HabitOutline
import com.marta.habittracker.ui.theme.HabitPrimary
import com.marta.habittracker.ui.theme.HabitPrimaryLight

enum class HabitButtonVariant {
    Primary,
    PrimaryLight,
    GhostPill,
    Secondary,
    TextLink,
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
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.98f else 1f,
        label = "habitButtonScale",
    )

    val buttonModifier = modifier
        .scale(scale)
        .then(if (fillMaxWidth && variant != HabitButtonVariant.TextLink) Modifier.fillMaxWidth() else Modifier)
        .then(
            when (variant) {
                HabitButtonVariant.Primary,
                HabitButtonVariant.PrimaryLight,
                HabitButtonVariant.Secondary,
                -> Modifier.defaultMinSize(minHeight = HabitButtonHeight)

                HabitButtonVariant.GhostPill -> Modifier
                HabitButtonVariant.TextLink -> Modifier
            }
        )

    when (variant) {
        HabitButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier.height(HabitButtonHeight),
                shape = HabitButtonShape,
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = HabitPrimaryLight,
                    contentColor = Color.White,
                    disabledContentColor = Color.White,
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(HabitButtonShape)
                        .background(
                            if (enabled && !loading) {
                                Brush.linearGradient(listOf(HabitPrimary, HabitPrimaryLight))
                            } else {
                                Brush.linearGradient(listOf(HabitPrimaryLight, HabitPrimaryLight))
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
            }
        }

        HabitButtonVariant.PrimaryLight -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier.height(HabitButtonHeight),
                shape = HabitButtonShape,
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = HabitPrimary,
                    disabledContainerColor = Color.White.copy(alpha = 0.7f),
                    disabledContentColor = HabitPrimary.copy(alpha = 0.5f),
                ),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = HabitPrimary,
                )
            }
        }

        HabitButtonVariant.GhostPill -> {
            TextButton(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                interactionSource = interactionSource,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
            }
        }

        HabitButtonVariant.Secondary -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier.height(HabitButtonHeight),
                shape = HabitButtonShape,
                interactionSource = interactionSource,
                border = BorderStroke(1.dp, HabitOutline),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HabitField,
                    contentColor = HabitOnSurface,
                    disabledContainerColor = HabitField.copy(alpha = 0.6f),
                    disabledContentColor = HabitOnSurface.copy(alpha = 0.5f),
                ),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = HabitOnSurface,
                )
            }
        }

        HabitButtonVariant.TextLink -> {
            TextButton(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier,
                interactionSource = interactionSource,
                colors = ButtonDefaults.textButtonColors(contentColor = HabitPrimary),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = HabitPrimary,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 40.dp,
    backgroundColor: Color = HabitField,
    iconTint: Color = HabitOnSurface,
) {
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
            .size(size),
        shape = CircleShape,
        color = backgroundColor,
        interactionSource = interactionSource,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
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
                .background(Brush.linearGradient(listOf(HabitPrimary, HabitPrimaryLight))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
            )
        }
    }
}
