package com.marta.habittracker.data.remote.ai.dto

import kotlinx.serialization.Serializable

/** Icon keys and `#RRGGBB` colors Bloom may choose when proposing a habit. */
@Serializable
internal data class HabitCatalogBody(
    val icons: List<String>,
    val colors: List<String>,
)
