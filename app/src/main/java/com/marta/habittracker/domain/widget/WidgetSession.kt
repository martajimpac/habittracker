package com.marta.habittracker.domain.widget

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError

sealed interface ChallengeWidgetLookup {
    data object Unavailable : ChallengeWidgetLookup

    data object MissingFromAccount : ChallengeWidgetLookup

    data class Found(val snapshotJson: String) : ChallengeWidgetLookup
}

fun resolveChallengeWidgetSnapshot(
    hasSession: Boolean,
    cachedJson: String,
    lookup: ChallengeWidgetLookup,
): String? {
    if (!hasSession) return null
    return when (lookup) {
        ChallengeWidgetLookup.Unavailable -> cachedJson
        ChallengeWidgetLookup.MissingFromAccount -> null
        is ChallengeWidgetLookup.Found -> lookup.snapshotJson
    }
}

suspend fun onHabitSyncFinished(
    result: DataResult<Unit, AppError>,
    refreshAll: suspend () -> Unit,
) {
    if (result is DataResult.Success) refreshAll()
}

suspend fun onSignedOut(
    result: DataResult<Unit, AppError>,
    clearLocalAccount: suspend () -> Unit,
) {
    if (result is DataResult.Success) clearLocalAccount()
}
