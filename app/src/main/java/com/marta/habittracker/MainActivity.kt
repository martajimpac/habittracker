package com.marta.habittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.marta.habittracker.presentation.theme.InstaDevTheme
import com.marta.habittracker.presentation.navigation.NavigationWrapper
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras
import com.marta.habittracker.presentation.widgets.WidgetRefresher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var initialTab by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialTab = WidgetLaunchExtras.tabFromIntent(intent)
        enableEdgeToEdge()
        setContent {
            InstaDevTheme {
                NavigationWrapper(initialTabRoute = initialTab)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        initialTab = WidgetLaunchExtras.tabFromIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            WidgetRefresher.refreshAll(this@MainActivity)
        }
    }
}