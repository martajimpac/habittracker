package com.marta.habittracker.data.repository

import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.errors.AppError

/**
 * Creates the challenger's habit and then the challenge row, so that a retry with the same ids
 * never duplicates anything. If inserting the challenge fails, it first checks whether the row
 * exists anyway (the response may have been lost, or a retry hit the primary key). Only when it
 * does not exist is the habit rolled back.
 */
internal suspend fun createChallengeSteps(
    insertHabit: suspend () -> DataResult<Unit, AppError>,
    insertChallenge: suspend () -> DataResult<Unit, AppError>,
    challengeExists: suspend () -> DataResult<Boolean, AppError>,
    rollbackHabit: suspend () -> Unit,
): DataResult<Unit, AppError> {
    when (val inserted = insertHabit()) {
        is DataResult.Error -> return inserted
        is DataResult.Success -> Unit
    }
    val created = insertChallenge()
    if (created is DataResult.Success) return created
    val exists = challengeExists()
    if (exists is DataResult.Success && exists.data) return DataResult.Success(Unit)
    rollbackHabit()
    return created
}
