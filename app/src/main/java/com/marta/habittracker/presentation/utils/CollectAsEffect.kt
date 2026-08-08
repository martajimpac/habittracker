package com.marta.habittracker.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Collects one-shot / event [Flow]s (typically [kotlinx.coroutines.flow.SharedFlow]) only while
 * the host is at least [Lifecycle.State.STARTED], so navigation does not run in the background.
 */
@Composable
fun <T> CollectAsEffect(
    flow: Flow<T>,
    block: suspend (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, flow) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { block(it) }
        }
    }
}
