package com.marta.habittracker.presentation.widgets

internal sealed interface WidgetLoad<out T> {
    data object Loading : WidgetLoad<Nothing>

    data class Ready<T>(val value: T) : WidgetLoad<T>
}
