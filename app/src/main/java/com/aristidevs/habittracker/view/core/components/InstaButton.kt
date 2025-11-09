package com.aristidevs.habittracker.view.core.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun InstaButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    onClick: () -> Unit,
    text: String,
    colors: ButtonColors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
) {
    Button(
        modifier = modifier,
        colors = colors,
        onClick = { onClick() },
        enabled = enabled,
        shape = shape
    ) {
        InstaText(
            modifier = Modifier.padding(vertical = 4.dp),
            text = text,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}