package com.marta.habittracker.domain.util

import java.time.Instant

interface Clock {
    fun now(): Instant
}
