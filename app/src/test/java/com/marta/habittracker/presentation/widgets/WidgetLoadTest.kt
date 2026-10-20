package com.marta.habittracker.presentation.widgets

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLoadTest {

    @Test
    fun loadingIsNotContentAndEmptyOrMissingDataIsReady() {
        val loading: WidgetLoad<List<String>> = WidgetLoad.Loading
        val empty = WidgetLoad.Ready(emptyList<String>())
        val missing = WidgetLoad.Ready<String?>(null)

        assertTrue(loading !is WidgetLoad.Ready<*>)
        assertTrue(empty.value.isEmpty())
        assertNull(missing.value)
    }
}
