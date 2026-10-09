package com.wobble.vault.data.model

data class UserDto(
    val id: String? = null,
    val username: String? = null,
    val email: String? = null,
    val role: String? = null,
    val salary: Double? = null,
    val createdAt: String? = null,
    val permissions: List<PermissionDto> = emptyList()
)

data class CreateUserRequest(
    val username: String,
    val email: String? = null,
    val password: String? = null,
    val role: String,
    val salary: Double? = null
)
