package com.wobble.vault.data.model

data class LoginRequest(val email: String, val password: String)

data class PermissionDto(val id: String? = null, val pageName: String)

data class AuthUser(
    val id: String? = null,
    val username: String = "",
    val email: String? = null,
    val role: String = "",
    val createdAt: String? = null,
    val permissions: List<PermissionDto> = emptyList()
) {
    fun hasPermission(pageName: String): Boolean =
        role == "ADMIN" || permissions.any { it.pageName == pageName }
}

data class LoginResponse(val accessToken: String, val user: AuthUser)

data class CsrfTokenResponse(val token: String? = null)

data class ApiError(
    val timestamp: String? = null,
    val status: Int? = null,
    val error: String? = null,
    val message: String? = null,
    val path: String? = null
)
