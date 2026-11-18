package com.marta.habittracker

import android.content.Intent
import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.marta.habittracker.core.auth.AuthDeeplinkTarget
import com.marta.habittracker.core.auth.AuthLinks
import com.marta.habittracker.core.auth.authDeeplinkTarget
import com.marta.habittracker.presentation.theme.HabitAppTheme
import com.marta.habittracker.presentation.navigation.LaunchTarget
import com.marta.habittracker.presentation.navigation.LaunchTargetCoordinator
import com.marta.habittracker.presentation.navigation.NavigationWrapper
import com.marta.habittracker.presentation.navigation.TabScreens
import com.marta.habittracker.presentation.auth.AuthDeeplinkCoordinator
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras
import com.marta.habittracker.presentation.widgets.WidgetRefresher
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var supabase: SupabaseClient
    @Inject lateinit var authDeeplinkCoordinator: AuthDeeplinkCoordinator
    @Inject lateinit var launchTargetCoordinator: LaunchTargetCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            applyLaunchExtras(intent)
        }
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            HabitAppTheme {
                NavigationWrapper()
            }
        }
        handleAuthDeeplink(intent)
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch { WidgetRefresher.refreshTodayWidget(applicationContext) }
    }

    override fun onStop() {
        super.onStop()
        // The launcher (and the widget) becomes visible again: push today's habits to it.
        lifecycleScope.launch { WidgetRefresher.refreshTodayWidget(applicationContext) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyLaunchExtras(intent)
        handleAuthDeeplink(intent)
    }

    private fun applyLaunchExtras(intent: Intent?) {
        launchTargetFromIntent(intent)?.let(launchTargetCoordinator::open)
    }

    /** One target per intent. Priority: habit > challenge > friend request > tab. */
    private fun launchTargetFromIntent(intent: Intent?): LaunchTarget? {
        WidgetLaunchExtras.habitIdFromIntent(intent)?.let { return LaunchTarget.HabitDetail(it) }
        WidgetLaunchExtras.challengeIdFromIntent(intent)?.let { return LaunchTarget.ChallengeDetail(it) }
        if (WidgetLaunchExtras.friendRequestIdFromIntent(intent) != null) {
            return LaunchTarget.Tab(TabScreens.TabFriends)
        }
        val tab = when (WidgetLaunchExtras.tabFromIntent(intent)) {
            WidgetLaunchExtras.TAB_HOME -> TabScreens.TabHome
            WidgetLaunchExtras.TAB_STATS -> TabScreens.TabStats
            WidgetLaunchExtras.TAB_FRIENDS -> TabScreens.TabFriends
            else -> null
        }
        return tab?.let(LaunchTarget::Tab)
    }

    private fun handleAuthDeeplink(intent: Intent) {
        val uri = intent.data ?: return
        when (authDeeplinkTarget(uri.scheme, uri.host, uri.path)) {
            AuthDeeplinkTarget.Reset -> importAuthDeeplink(intent, AuthLinks.RESET_PATH)
            AuthDeeplinkTarget.Confirm -> importAuthDeeplink(intent, AuthLinks.CONFIRM_PATH)
            AuthDeeplinkTarget.Ignore -> Unit
        }
    }

    private fun importAuthDeeplink(intent: Intent, path: String) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Handling auth deep link path=$path")
                supabase.handleDeeplinks(intent)
                if (path == AuthLinks.CONFIRM_PATH) {
                    val signedIn = supabase.auth.currentSessionOrNull() != null
                    authDeeplinkCoordinator.onEmailConfirmed(signedIn)
                } else {
                    authDeeplinkCoordinator.onResetLinkHandled()
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Auth deep link handling failed path=$path", exception)
                if (path == AuthLinks.CONFIRM_PATH) {
                    authDeeplinkCoordinator.onEmailConfirmFailed()
                } else {
                    authDeeplinkCoordinator.onResetLinkFailed()
                }
            }
        }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
