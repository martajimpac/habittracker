package com.marta.habittracker.data.network.response

import com.marta.habittracker.domain.model.User
import com.marta.habittracker.domain.model.UserMode.CompanyUser
import com.marta.habittracker.domain.model.UserMode.ContentCreatorUser
import com.marta.habittracker.domain.model.UserMode.RegularUser
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

fun UserResponse.toDomain(): User {

    val userMode = when (userType) {
        RegularUser.userType -> RegularUser
        ContentCreatorUser.userType -> ContentCreatorUser
        CompanyUser.userType -> CompanyUser
        else -> RegularUser
    }

    return User(
        userId = userId,
        name = name,
        nickname = nickname,
        followers = followers,
        following = following,
        userMode = userMode,
        verified = verified
    )
}