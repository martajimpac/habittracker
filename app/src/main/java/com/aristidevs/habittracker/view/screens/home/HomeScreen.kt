package com.aristidevs.habittracker.view.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aristidevs.habittracker.data.local.database.HabitWithStatus
import com.aristidevs.habittracker.view.utils.getCalendarDays
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onHabitClick: (Long) -> Unit
) {
    // 1. Recoger datos del ViewModel de forma segura para el ciclo de vida
    val habits: List<HabitWithStatus> by viewModel.habits.collectAsStateWithLifecycle()
    val selectedDate: LocalDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    // 2. Cálculos derivados para la UI
    val completedCount by remember(habits) { derivedStateOf { habits.count { it.isCompleted } } }
    val allCompleted by remember(habits) {
        derivedStateOf { habits.isNotEmpty() && habits.all { it.isCompleted } }
    }

    val calendarDays = remember { getCalendarDays() }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Cabecera: Contador y mensaje de éxito
        Text(
            text = "Hábitos: $completedCount / ${habits.size}",
            style = MaterialTheme.typography.titleMedium
        )

        if (allCompleted) {
            Text("🎉 ¡Todo listo por hoy!", color = Color(0xFF2E7D32))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de fecha Horizontal
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(calendarDays) { date ->
                val isSelected = date == selectedDate
                DateItem(
                    date = date,
                    isSelected = isSelected,
                    onDateClick = { viewModel.onDateSelected(date) }
                )
            }
        }

        // Lista de Hábitos
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(habits) { habitWithStatus ->
                HabitItem(
                    habitWithStatus = habitWithStatus,
                    onToggle = { viewModel.toggleComplete(habitWithStatus) },
                    onItemClick = { onHabitClick(habitWithStatus.habit.id) }
                )
            }
        }
    }
}

@Composable
fun DateItem(date: LocalDate, isSelected: Boolean, onDateClick: () -> Unit) {
    // Formateador para mostrar "Lun", "Mar", etc.
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.medium
            )
            .clickable { onDateClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = dayName, color = if (isSelected) Color.White else Color.Black)
        Text(text = date.dayOfMonth.toString(), color = if (isSelected) Color.White else Color.Black)
    }
}

@Composable
fun HabitItem(
    habitWithStatus: HabitWithStatus,
    onToggle: () -> Unit,
    onItemClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habitWithStatus.habit.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                habitWithStatus.habit.description?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Usamos Switch para marcar completado
            Switch(
                checked = habitWithStatus.isCompleted,
                onCheckedChange = { _ -> onToggle() }
            )
        }
    }
}
