package com.marta.habittracker.data.remote

/**
 * Storage object path stays `{userId}/avatar.jpg`. The query is only for Coil/HTTP caches.
 */
internal fun cacheBustAvatarUrl(publicUrl: String, versionMillis: Long): String {
    val base = publicUrl.substringBefore('?')
    return "$base?v=$versionMillis"
}
