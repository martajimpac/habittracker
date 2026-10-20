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
import com.marta.habittracker.core.auth.AuthDeeplinkTarget
import com.marta.habittracker.core.auth.AuthLinks
import com.marta.habittracker.core.auth.authDeeplinkTarget
import com.marta.habittracker.presentation.theme.HabitAppTheme
import com.marta.habittracker.presentation.navigation.NavigationWrapper
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

    private var initialTab by mutableStateOf<String?>(null)
    private var initialHabitId by mutableStateOf<String?>(null)
    private var initialChallengeId by mutableStateOf<String?>(null)
    private var initialFriendRequestId by mutableStateOf<String?>(null)

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
                NavigationWrapper(
                    initialTabRoute = initialTab,
                    initialHabitId = initialHabitId,
                    onHabitIdConsumed = { consumeHabitId() },
                    initialChallengeId = initialChallengeId,
                    onChallengeIdConsumed = { consumeChallengeId() },
                    initialFriendRequestId = initialFriendRequestId,
                    onFriendRequestConsumed = { consumeFriendRequest() },
                )
            }
        }
        handleAuthDeeplink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyLaunchExtras(intent)
        handleAuthDeeplink(intent)
    }

    private fun applyLaunchExtras(intent: Intent?) {
        initialHabitId = WidgetLaunchExtras.habitIdFromIntent(intent)
        initialChallengeId = if (initialHabitId == null) {
            WidgetLaunchExtras.challengeIdFromIntent(intent)
        } else {
            null
        }
        val friendRequestId = WidgetLaunchExtras.friendRequestIdFromIntent(intent)
        initialFriendRequestId = if (initialHabitId == null && initialChallengeId == null) {
            friendRequestId
        } else {
            null
        }
        initialTab = when {
            initialHabitId != null || initialChallengeId != null -> null
            initialFriendRequestId != null -> WidgetLaunchExtras.TAB_FRIENDS
            else -> WidgetLaunchExtras.tabFromIntent(intent)
        }
        if (initialHabitId != null || initialChallengeId != null) {
            intent?.removeExtra(WidgetLaunchExtras.EXTRA_FRIEND_REQUEST_ID)
        }
    }

    private fun consumeHabitId() {
        initialHabitId = null
        intent?.removeExtra(WidgetLaunchExtras.EXTRA_HABIT_ID)
        intent?.removeExtra(WidgetLaunchExtras.EXTRA_BOTTOM_TAB)
    }

    private fun consumeChallengeId() {
        initialChallengeId = null
        intent?.removeExtra(WidgetLaunchExtras.EXTRA_CHALLENGE_ID)
    }

    private fun consumeFriendRequest() {
        initialFriendRequestId = null
        initialTab = null
        intent?.removeExtra(WidgetLaunchExtras.EXTRA_FRIEND_REQUEST_ID)
        intent?.removeExtra(WidgetLaunchExtras.EXTRA_BOTTOM_TAB)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            WidgetRefresher.refreshAll(this@MainActivity)
        }
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
