package com.aristidevs.habittracker.view.screens.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitDetailScreen(
    onBack: () -> Unit,
    viewModel: HabitDetailViewModel = hiltViewModel()
) {
    val habit by viewModel.habit.collectAsStateWithLifecycle()
    val completionRate by viewModel.completionPercentage.collectAsStateWithLifecycle()
    val habitName = habit?.name ?: "Cargando..."

    // 1. Definimos las pestañas
    val tabs = listOf("Estadísticas", "Calendario", "Ajustes")

    // 2. Estado del Pager (controla en qué página estamos)
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    // 3. Scope para lanzar animaciones (al hacer clic en un Tab)
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // --- CABECERA DE LA PANTALLA ---
        HabitDetailHeader(
            name = habitName,
            onBack = onBack
        )

        // --- TAB ROW ---
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                // Línea que se mueve bajo la pestaña seleccionada
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage])
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        // Al pulsar, hacemos scroll animado a la página
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(text = title) }
                )
            }
        }

        // --- HORIZONTAL PAGER  ---
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f), // Ocupa el resto de la pantalla
            verticalAlignment = Alignment.Top
        ) { pageIndex ->
            // Aquí dibujamos el contenido de cada página según el índice
            when (pageIndex) {
                0 -> StatisticsTab()
                1 -> CalendarTab()
                2 -> SettingsTab()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailHeader(name: String, onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(text = name, style = MaterialTheme.typography.titleLarge)
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver atrás"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun StatisticsTab() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Progreso Mensual", style = MaterialTheme.typography.titleLarge)
        // Aquí irían tus gráficas
    }
}

@Composable
fun CalendarTab() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Historial de cumplimiento")
    }
}

@Composable
fun SettingsTab() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Editar Hábito")
        // Aquí irían botones para borrar o cambiar días
    }
}