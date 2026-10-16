package com.marta.habittracker.domain.push

object FriendPushIds {
    fun notificationIdFor(friendshipId: String): Int =
        (("friend:$friendshipId").hashCode() and 0x7fffffff).let { if (it == 0) 1 else it }
}
