package com.marta.habittracker

import android.content.Intent
import android.util.Log
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
import com.marta.habittracker.presentation.auth.AuthDeeplinkCoordinator
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras
import com.marta.habittracker.presentation.widgets.WidgetRefresher
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var supabase: SupabaseClient
    @Inject lateinit var authDeeplinkCoordinator: AuthDeeplinkCoordinator

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
        handleAuthDeeplink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        initialTab = WidgetLaunchExtras.tabFromIntent(intent)
        handleAuthDeeplink(intent)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            WidgetRefresher.refreshAll(this@MainActivity)
        }
    }

    private fun handleAuthDeeplink(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme != AUTH_DEEPLINK_SCHEME || uri.host != AUTH_DEEPLINK_HOST ||
            !uri.path.orEmpty().startsWith(AUTH_DEEPLINK_PATH)
        ) {
            return
        }

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Handling password reset deep link")
                supabase.handleDeeplinks(intent)
                authDeeplinkCoordinator.onResetLinkHandled()
            } catch (exception: Exception) {
                Log.e(TAG, "Password reset deep link handling failed", exception)
                authDeeplinkCoordinator.onResetLinkFailed()
            }
        }
    }

    private companion object {
        const val TAG = "MainActivity"
        const val AUTH_DEEPLINK_SCHEME = "habittracker"
        const val AUTH_DEEPLINK_HOST = "auth"
        const val AUTH_DEEPLINK_PATH = "/reset"
    }
}