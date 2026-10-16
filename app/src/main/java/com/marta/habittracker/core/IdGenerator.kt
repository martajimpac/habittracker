@file:OptIn(ExperimentalUuidApi::class)

package com.marta.habittracker.core

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object IdGenerator {
    fun newId(): String = Uuid.generateV7().toString()
}
