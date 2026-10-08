package com.marta.habittracker.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class IdGeneratorTest {

    @Test
    fun `newId returns a parseable UUID with version 7`() {
        val id = IdGenerator.newId()
        val uuid = UUID.fromString(id)
        assertEquals(7, uuid.version())
    }

    @Test
    fun `newId returns distinct values`() {
        assertNotEquals(IdGenerator.newId(), IdGenerator.newId())
    }

    @Test
    fun `newId matches UUID string shape`() {
        val id = IdGenerator.newId()
        assertTrue(
            id.matches(
                Regex("^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"),
            ),
        )
    }
}
