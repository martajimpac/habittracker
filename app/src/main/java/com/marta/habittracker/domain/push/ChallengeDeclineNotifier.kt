package com.marta.habittracker.domain.push

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface ChallengeDeclineNotifier {
    val declinedIds: Flow<String>
    fun publish(challengeId: String)
}

internal object SilentChallengeDeclineNotifier : ChallengeDeclineNotifier {
    override val declinedIds: Flow<String> = emptyFlow()
    override fun publish(challengeId: String) = Unit
}
