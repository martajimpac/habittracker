package com.aristidevs.habittracker.domain.entity

data class UserEntity(
    val userId: String,
    val name: String,
    val nickname: String,
    val followers: Int,
    val following: List<String>,
    val userMode: UserMode,
    val verified:Boolean
)

sealed class UserMode(val userType: Int) {
    data object REGULAR_USER : UserMode(0)
    data object CONTENT_CREATOR_USER : UserMode(1)
    data object COMPANY_USER : UserMode(2)
}