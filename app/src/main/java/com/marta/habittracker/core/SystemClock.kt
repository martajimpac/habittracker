package com.marta.habittracker.core

import com.marta.habittracker.domain.util.Clock
import java.time.Instant
import javax.inject.Inject

class SystemClock
    @Inject
    constructor() : Clock {
        override fun now(): Instant = Instant.now()
    }
