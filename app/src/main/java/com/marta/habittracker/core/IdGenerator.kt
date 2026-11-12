@file:OptIn(ExperimentalUuidApi::class)

package com.marta.habittracker.core

import android.util.Log
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object IdGenerator {
    fun newId(): String  {
        Log.d(
            "MARTUKI",
            "GENERANDO NUEVO ID"
        )
        return Uuid.generateV7().toString()
    }
}
