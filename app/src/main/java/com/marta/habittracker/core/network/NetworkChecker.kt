package com.marta.habittracker.core.network

fun interface NetworkChecker {
    fun isOnline(): Boolean
}
