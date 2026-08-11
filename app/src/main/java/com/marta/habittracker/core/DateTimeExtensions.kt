package com.marta.habittracker.core

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import kotlinx.datetime.LocalDate as KotlinLocalDate
import kotlinx.datetime.DayOfWeek as KotlinDayOfWeek

fun List<DayOfWeek>.toKotlinSet(): Set<KotlinDayOfWeek> {
    return this.map { javaDayOfWeek ->
        when (javaDayOfWeek) {
            DayOfWeek.MONDAY -> KotlinDayOfWeek.MONDAY
            DayOfWeek.TUESDAY -> KotlinDayOfWeek.TUESDAY
            DayOfWeek.WEDNESDAY -> KotlinDayOfWeek.WEDNESDAY
            DayOfWeek.THURSDAY -> KotlinDayOfWeek.THURSDAY
            DayOfWeek.FRIDAY -> KotlinDayOfWeek.FRIDAY
            DayOfWeek.SATURDAY -> KotlinDayOfWeek.SATURDAY
            DayOfWeek.SUNDAY -> KotlinDayOfWeek.SUNDAY
        }
    }.toSet()
}

fun Set<KotlinDayOfWeek>.toJavaList(): List<DayOfWeek> {
    return this.map { kotlinDayOfWeek ->
        when (kotlinDayOfWeek) {
            KotlinDayOfWeek.MONDAY -> DayOfWeek.MONDAY
            KotlinDayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
            KotlinDayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
            KotlinDayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
            KotlinDayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
            KotlinDayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
            KotlinDayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
        }
    }
}

fun Long.toInstant(): Instant = Instant.ofEpochMilli(this)

fun Instant.toEpochMillis(): Long = this.toEpochMilli()

fun LocalDate.toKotlin(): KotlinLocalDate = KotlinLocalDate.parse(this.toString())

fun KotlinLocalDate.toJava(): LocalDate = LocalDate.parse(this.toString())
