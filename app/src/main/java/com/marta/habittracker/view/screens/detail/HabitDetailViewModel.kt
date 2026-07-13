package com.marta.habittracker.view.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marta.habittracker.data.local.database.entities.HabitEntity
import com.marta.habittracker.data.local.database.entities.HabitRecordEntity
import com.marta.habittracker.domain.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.emptyList

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    private val repository: HabitRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /*****
     * ¿Por qué usamos SavedStateHandle?
     *
     * Persistencia:
     * Si el sistema operativo mata tu app para liberar
     * memoria mientras estás en el detalle, al volver, SavedStateHandle
     * seguirá teniendo el habitId.
     *
     *
     * Seguridad de tipos:
     * Al usar la navegación Type-Safe de Compose, Hilt sabe
     * que si la ruta es TabDetail(val habitId: Long), debe buscar
     * una clave llamada "habitId".
     */

    // Extraemos el habitId.
    // "habitId" debe llamarse igual que la propiedad en tu data class TabDetail
    private val habitId: Long = checkNotNull(savedStateHandle["habitId"])

    // 1. Estado del Hábito (Nombre, descripción, días...)
    // Lo exponemos como un StateFlow para que la UI reaccione si se edita
    val habit: StateFlow<HabitEntity?> = repository.getHabitById(habitId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // 2. Estado de los Registros (Para la pestaña de Estadísticas y Calendario)
    val history: StateFlow<List<HabitRecordEntity>> = repository.getRecordsForHabit(habitId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 3. Acciones posibles en el detalle
    fun deleteHabit() {
        viewModelScope.launch {
            habit.value?.let { repository.deleteHabit(it) }
        }
    }

    // Ejemplo: lógica para las estadísticas que podrías usar en la pestaña 1
    val completionPercentage: StateFlow<Float> = history.map { records ->
        if (records.isEmpty()) 0f else {
            val completed = records.count { it.isCompleted }
            completed.toFloat() / records.size
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)
}