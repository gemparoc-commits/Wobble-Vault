package com.wobble.vault.data

import com.google.gson.Gson
import retrofit2.Response

data class ApiErrorBody(
    val status: Int? = null,
    val error: String? = null,
    val message: String? = null,
    val path: String? = null,
    val timestamp: String? = null
)

fun <T> apiErrorMessage(gson: Gson, response: Response<T>, fallback: String): String {
    val body = response.errorBody()?.string()
    if (body.isNullOrBlank()) return fallback
    return try {
        gson.fromJson(body, ApiErrorBody::class.java)?.message?.takeIf { it.isNotBlank() } ?: fallback
    } catch (_: Exception) {
        fallback
    }
}

fun <T> Response<T>.isUnauthorized(): Boolean = code() == 401
