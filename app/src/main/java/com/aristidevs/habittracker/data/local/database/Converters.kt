package com.aristidevs.habittracker.data.local.database

import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.LocalDate

object Converters {
    @TypeConverter
    fun fromDayOfWeekList(days: List<DayOfWeek>?): String? {
        return days?.joinToString(",") { it.name }
    }

    // Convierte la cadena "1,3" de vuelta a [MONDAY, WEDNESDAY]
    @TypeConverter
    fun toDaysOfWeek(value: String): List<DayOfWeek> {
        if (value.isBlank()) return emptyList()
        return value.split(",").map { DayOfWeek.valueOf(it) }
    }

    // De LocalDate a String (para guardar en la DB)
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.toString() // Guarda "yyyy-MM-dd"
    }

    // De String a LocalDate (para leer de la DB)
    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let {
            LocalDate.parse(it) // Lee el String y lo convierte en objeto LocalDate
        }
    }
}
