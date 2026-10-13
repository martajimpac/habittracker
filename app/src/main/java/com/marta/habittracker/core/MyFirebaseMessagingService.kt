package com.marta.habittracker.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.marta.habittracker.MainActivity
import com.marta.habittracker.R
import com.marta.habittracker.core.push.DeviceTokenRegistration
import com.marta.habittracker.domain.push.ChallengePushIds
import com.marta.habittracker.presentation.widgets.WidgetLaunchExtras
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        if (data["type"] == CHALLENGE_CREATED_TYPE) {
            val challengeId = data["challengeId"]?.takeIf { it.isNotBlank() }
            if (challengeId != null) {
                Log.d(TAG, "Challenge push received type=$CHALLENGE_CREATED_TYPE challengeId=$challengeId")
                val challengerName = data["challengerName"].orEmpty().ifBlank {
                    getString(R.string.challenge_detail_title)
                }
                sendChallengeNotification(
                    title = getString(R.string.challenge_push_title),
                    body = getString(R.string.challenge_push_body, challengerName),
                    challengeId = challengeId,
                )
                return
            }
        }

        remoteMessage.notification?.let {
            sendNotification(
                it.title ?: getString(R.string.notification_default_title),
                it.body ?: "",
            )
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        tokenRefreshScope.launch {
            try {
                val registration = EntryPointAccessors.fromApplication(
                    applicationContext,
                    DeviceTokenRegistrarEntryPoint::class.java,
                ).deviceTokenRegistration()
                registration.registerCurrentToken()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "registerCurrentToken failed", e)
            }
        }
    }

    private fun sendChallengeNotification(title: String, body: String, challengeId: String) {
        val intent = WidgetLaunchExtras.openChallengeIntent(this, challengeId)
        val pendingIntent = PendingIntent.getActivity(
            this,
            ChallengePushIds.notificationIdFor(challengeId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        postNotification(
            notificationId = ChallengePushIds.notificationIdFor(challengeId),
            title = title,
            body = body,
            pendingIntent = pendingIntent,
        )
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )
        postNotification(
            notificationId = 0,
            title = title,
            body = messageBody,
            pendingIntent = pendingIntent,
        )
    }

    private fun postNotification(
        notificationId: Int,
        title: String,
        body: String,
        pendingIntent: PendingIntent,
    ) {
        val channelId = NOTIFICATION_CHANNEL_ID
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DeviceTokenRegistrarEntryPoint {
        fun deviceTokenRegistration(): DeviceTokenRegistration
    }

    companion object {
        private val tokenRefreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private const val TAG = "MyFirebaseMsgService"
        private const val NOTIFICATION_CHANNEL_ID = "habit_tracker_notifications"
        private const val CHALLENGE_CREATED_TYPE = "challenge_created"
    }
}
