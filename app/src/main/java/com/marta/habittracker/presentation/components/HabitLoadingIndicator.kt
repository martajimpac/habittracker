package com.marta.habittracker.presentation.components

import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.marta.habittracker.presentation.theme.InstaDevTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HabitLoadingIndicator(
    modifier: Modifier = Modifier,
) {
    CircularWavyProgressIndicator(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Preview(showBackground = true)
@Composable
private fun HabitLoadingIndicatorPreview() {
    InstaDevTheme {
        HabitLoadingIndicator()
    }
}
