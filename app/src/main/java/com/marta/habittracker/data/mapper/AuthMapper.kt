package com.marta.habittracker.data.mapper

import com.marta.habittracker.data.remote.supabase.dto.AuthUserDto
import com.marta.habittracker.domain.model.User
import com.marta.habittracker.domain.model.UserMode

fun AuthUserDto.toDomain(): User =
    User(
        userId = id,
        name = name,
        nickname = email.orEmpty(),
        followers = 0,
        following = emptyList(),
        userMode = UserMode.RegularUser,
        verified = emailConfirmed,
    )
