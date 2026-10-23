package com.marta.habittracker.core.push

import com.marta.habittracker.domain.push.ChallengeDeclineNotifier
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class ChallengeDeclineNotifierImpl @Inject constructor() : ChallengeDeclineNotifier {
    private val declined = MutableSharedFlow<String>(extraBufferCapacity = 8)
    override val declinedIds: Flow<String> = declined.asSharedFlow()

    override fun publish(challengeId: String) {
        declined.tryEmit(challengeId)
    }
}
