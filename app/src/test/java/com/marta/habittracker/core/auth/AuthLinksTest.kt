package com.marta.habittracker.core.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthLinksTest {

    @Test
    fun `confirm link is not the password reset link`() {
        assertEquals(AuthDeeplinkTarget.Confirm, authDeeplinkTarget("habittracker", "auth", "/confirm"))
        assertEquals(AuthDeeplinkTarget.Reset, authDeeplinkTarget("habittracker", "auth", "/reset"))
        assertEquals(AuthDeeplinkTarget.Ignore, authDeeplinkTarget("https", "localhost", "/confirm"))
    }
}
