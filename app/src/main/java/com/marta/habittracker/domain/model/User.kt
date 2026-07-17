package com.marta.habittracker.domain.model

data class User(
    val userId: String,
    val name: String,
    val nickname: String,
    val followers: Int,
    val following: List<String>,
    val userMode: UserMode,
    val verified: Boolean,
)

sealed class UserMode(val userType: Int) {
    data object RegularUser : UserMode(0)
    data object ContentCreatorUser : UserMode(1)
    data object CompanyUser : UserMode(2)
}
