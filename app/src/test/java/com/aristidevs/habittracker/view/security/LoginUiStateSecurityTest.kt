package com.aristidevs.habittracker.view.security

import com.aristidevs.habittracker.view.screens.auth.login.LoginUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LoginUiStateSecurityTest {

    @Test
    fun defaultLoginStateDoesNotPersistCredentials() {
        val state = LoginUiState()

        assertEquals("Email must not be prefilled in persistent UI state", "", state.email)
        assertEquals("Password must never be prefilled in persistent UI state", "", state.password)
        assertFalse("Login must start disabled until valid credentials are typed", state.isLoginEnabled)
    }

    @Test
    fun loginUiStateStringRepresentationDoesNotExposeCredentials() {
        val state = LoginUiState(
            email = "user@example.com",
            password = "Sup3rSecret!"
        )

        val serializedForLogs = state.toString()

        assertFalse(serializedForLogs.contains("Sup3rSecret!"))
        assertFalse(serializedForLogs.contains("password=", ignoreCase = true))
        assertFalse(serializedForLogs.contains("user@example.com"))
    }
}
