package com.marta.habittracker.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UsernameAvailabilityDto(
    val normalized: String = "",
    val available: Boolean = false,
    val suggestions: List<String> = emptyList(),
)

@Serializable
data class UsernameAvailabilityParams(
    val requested: String,
)
