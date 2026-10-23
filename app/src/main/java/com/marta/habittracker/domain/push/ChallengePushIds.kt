package com.marta.habittracker.domain.push

object ChallengePushIds {
    fun notificationIdFor(challengeId: String): Int =
        stableId("challenge:$challengeId")

    fun declinedNotificationIdFor(challengeId: String): Int =
        stableId("challenge-declined:$challengeId")

    private fun stableId(key: String): Int =
        (key.hashCode() and 0x7fffffff).let { if (it == 0) 1 else it }
}
