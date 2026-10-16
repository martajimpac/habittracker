package com.marta.habittracker.domain.push

object ChallengePushIds {
    fun notificationIdFor(challengeId: String): Int =
        (("challenge:$challengeId").hashCode() and 0x7fffffff).let { if (it == 0) 1 else it }
}
