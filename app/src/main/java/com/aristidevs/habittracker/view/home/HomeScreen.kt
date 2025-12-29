package com.aristidevs.habittracker.view.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}

@Composable
fun HomeScreen() {
    // Lista de días de la semana
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    // Estado del día seleccionado
    var selectedDay by remember { mutableStateOf(daysOfWeek[0]) }

    // Aquí podrías traer tus hábitos de un viewmodel
    val habits = remember(selectedDay) {
        getHabitsForDay(selectedDay)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Días de la semana - horizontal
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(daysOfWeek) { day ->
                Text(
                    text = day,
                    color = if (day == selectedDay) Color.White else Color.Black,
                    modifier = Modifier
                        .background(
                            color = if (day == selectedDay) Color.Blue else Color.LightGray,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            selectedDay = day
                        }
                )
            }
        }

        // Hábitos del día - vertical
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(habits) { habit ->
                HabitItem(habit)
            }
        }
    }
}

@Composable
fun HabitItem(name: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(16.dp),
            color = Color.Black
        )
    }
}

// Función de ejemplo para simular hábitos por día
fun getHabitsForDay(day: String): List<String> {
    return when(day) {
        "Mon" -> listOf("Exercise", "Read", "Drink water")
        "Tue" -> listOf("Meditate", "Journal")
        "Wed" -> listOf("Exercise", "Call a friend")
        else -> listOf("Relax", "Sleep early")
    }
}
