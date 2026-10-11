package com.marta.habittracker.core.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.marta.habittracker.data.remote.SocialRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class DeviceTokenRegistrar @Inject constructor(
    private val socialRemoteDataSource: SocialRemoteDataSource,
) {

    suspend fun registerCurrentToken() {
        try {
            val token = fetchFirebaseToken()
            socialRemoteDataSource.registerDeviceToken(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "registerCurrentToken failed", e)
        }
    }

    suspend fun clearCurrentToken() {
        try {
            val token = fetchFirebaseToken()
            socialRemoteDataSource.deleteDeviceToken(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "clearCurrentToken failed", e)
        }
    }

    private suspend fun fetchFirebaseToken(): String = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener
                if (task.isSuccessful) {
                    val token = task.result
                    if (token.isNullOrBlank()) {
                        continuation.resumeWithException(IllegalStateException("FCM token was empty"))
                    } else {
                        continuation.resume(token)
                    }
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("FCM token request failed"),
                    )
                }
            }
    }

    companion object {
        private const val TAG = "DeviceTokenRegistrar"
    }
}
