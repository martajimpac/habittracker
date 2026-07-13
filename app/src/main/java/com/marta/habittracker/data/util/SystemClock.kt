package com.marta.habittracker.data.util

import com.marta.habittracker.domain.util.Clock
import java.time.Instant
import javax.inject.Inject

class SystemClock
    @Inject
    constructor() : Clock {
        override fun now(): Instant = Instant.now()
    }
