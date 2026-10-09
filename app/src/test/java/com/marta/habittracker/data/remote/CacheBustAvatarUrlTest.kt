package com.marta.habittracker.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class CacheBustAvatarUrlTest {

    @Test
    fun `appends v query to stable public url`() {
        val publicUrl =
            "https://example.supabase.co/storage/v1/object/public/avatars/user-1/avatar.jpg"

        val busted = cacheBustAvatarUrl(publicUrl, versionMillis = 1_700_000_000_123)

        assertEquals("$publicUrl?v=1700000000123", busted)
    }

    @Test
    fun `replaces existing query so path stays avatar jpg`() {
        val publicUrl =
            "https://example.supabase.co/storage/v1/object/public/avatars/user-1/avatar.jpg?v=1"

        val busted = cacheBustAvatarUrl(publicUrl, versionMillis = 99)

        assertEquals(
            "https://example.supabase.co/storage/v1/object/public/avatars/user-1/avatar.jpg?v=99",
            busted,
        )
    }
}
