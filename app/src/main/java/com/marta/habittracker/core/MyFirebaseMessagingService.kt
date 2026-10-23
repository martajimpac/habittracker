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
import com.marta.habittracker.domain.push.ChallengeDeclineNotifier
import com.marta.habittracker.domain.push.ChallengePushIds
import com.marta.habittracker.domain.push.FriendPushIds
import com.marta.habittracker.domain.widget.AppWidgetRefresher
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

        if (data["type"] == CHALLENGE_DECLINED_TYPE) {
            val challengeId = data["challengeId"]?.takeIf { it.isNotBlank() }
            if (challengeId != null) {
                Log.d(TAG, "Challenge declined push received challengeId=$challengeId")
                val declinerName = data["declinerName"].orEmpty().ifBlank {
                    getString(R.string.challenge_detail_title)
                }
                sendChallengeDeclinedNotification(
                    title = getString(R.string.challenge_declined_push_title),
                    body = getString(R.string.challenge_declined_push_body, declinerName),
                    challengeId = challengeId,
                )
                tokenRefreshScope.launch {
                    try {
                        val entryPoint = EntryPointAccessors.fromApplication(
                            applicationContext,
                            DeviceTokenRegistrarEntryPoint::class.java,
                        )
                        entryPoint.challengeDeclineNotifier().publish(challengeId)
                        entryPoint.appWidgetRefresher().refreshAll()
                        Log.d(TAG, "Challenge declined follow-up finished challengeId=$challengeId")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Challenge declined follow-up failed challengeId=$challengeId", e)
                    }
                }
                return
            }
        }

        if (data["type"] == FRIEND_REQUEST_TYPE) {
            val friendshipId = data["friendshipId"]?.takeIf { it.isNotBlank() }
            if (friendshipId != null) {
                Log.d(TAG, "Friend request push received type=$FRIEND_REQUEST_TYPE friendshipId=$friendshipId")
                val requesterName = data["requesterName"].orEmpty().ifBlank {
                    getString(R.string.friend_request_push_sender_fallback)
                }
                sendFriendRequestNotification(
                    title = getString(R.string.friend_request_push_title),
                    body = getString(R.string.friend_request_push_body, requesterName),
                    friendshipId = friendshipId,
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

    private fun sendChallengeDeclinedNotification(title: String, body: String, challengeId: String) {
        val notificationId = ChallengePushIds.declinedNotificationIdFor(challengeId)
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            WidgetLaunchExtras.openFriendsIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        postNotification(
            notificationId = notificationId,
            title = title,
            body = body,
            pendingIntent = pendingIntent,
        )
    }

    private fun sendFriendRequestNotification(title: String, body: String, friendshipId: String) {
        val intent = WidgetLaunchExtras.openFriendRequestIntent(this, friendshipId)
        val notificationId = FriendPushIds.notificationIdFor(friendshipId)
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        postNotification(
            notificationId = notificationId,
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
        fun challengeDeclineNotifier(): ChallengeDeclineNotifier
        fun appWidgetRefresher(): AppWidgetRefresher
    }

    companion object {
        private val tokenRefreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private const val TAG = "MyFirebaseMsgService"
        private const val NOTIFICATION_CHANNEL_ID = "habit_tracker_notifications"
        private const val CHALLENGE_CREATED_TYPE = "challenge_created"
        private const val CHALLENGE_DECLINED_TYPE = "challenge_declined"
        private const val FRIEND_REQUEST_TYPE = "friend_request"
    }
}
