package com.aristidevs.habittracker.data.response

import com.aristidevs.habittracker.domain.entity.UserEntity
import com.aristidevs.habittracker.domain.entity.UserMode.COMPANY_USER
import com.aristidevs.habittracker.domain.entity.UserMode.CONTENT_CREATOR_USER
import com.aristidevs.habittracker.domain.entity.UserMode.REGULAR_USER
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val userId: String,
    @SerialName("na-me") val name: String,
    val nickname: String,
    val followers: Int = 0,
    val following: List<String> = emptyList(),
    val userType: Int,
    val verified: Boolean
)

fun UserResponse.toDomain(): UserEntity {

    val userMode = when (userType) {
        REGULAR_USER.userType -> REGULAR_USER
        CONTENT_CREATOR_USER.userType -> CONTENT_CREATOR_USER
        COMPANY_USER.userType -> COMPANY_USER
        else -> REGULAR_USER
    }

    return UserEntity(
        userId = userId,
        name = name,
        nickname = nickname,
        followers = followers,
        following = following,
        userMode = userMode,
        verified = verified
    )
}