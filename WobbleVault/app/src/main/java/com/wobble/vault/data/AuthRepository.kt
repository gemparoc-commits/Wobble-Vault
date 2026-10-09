package com.wobble.vault.data

import com.google.gson.Gson
import com.wobble.vault.data.api.WobbleApi
import com.wobble.vault.data.model.ApiError
import com.wobble.vault.data.model.AuthUser
import com.wobble.vault.data.model.LoginRequest
import retrofit2.Response

class AuthRepository(
    private val api: WobbleApi,
    private val store: TokenStore,
    private val gson: Gson = Gson()
) {

    sealed class AuthResult {
        data class Success(val user: AuthUser) : AuthResult()
        data class Failure(val message: String) : AuthResult()
    }

    suspend fun login(email: String, password: String): AuthResult {
        val resp = try {
            api.login(LoginRequest(email, password))
        } catch (e: Exception) {
            return AuthResult.Failure("Could not reach the server. Check your connection.")
        }
        if (!resp.isSuccessful) return AuthResult.Failure(errorMessage(resp))
        val body = resp.body() ?: return AuthResult.Failure("Empty response from server")
        store.save(body.accessToken, gson.toJson(body.user))
        refreshCsrf()
        return AuthResult.Success(body.user)
    }

    suspend fun restore(): AuthResult {
        store.load()
        if (store.bearer == null) return AuthResult.Failure("No session")
        val resp = try {
            api.me()
        } catch (e: Exception) {
            return AuthResult.Failure("Could not reach the server. Check your connection.")
        }
        val user = resp.body()
        return if (resp.isSuccessful && user != null) {
            store.updateUserJson(gson.toJson(user))
            refreshCsrf()
            AuthResult.Success(user)
        } else {
            store.clear()
            AuthResult.Failure("Session expired")
        }
    }

    suspend fun logout() {
        try {
            if (store.csrfToken == null) api.csrf()
            api.logout()
        } catch (e: Exception) {
        } finally {
            store.clear()
        }
    }

    private suspend fun refreshCsrf() {
        try {
            val response = api.csrf()
            response.body()?.token?.takeIf { it.isNotBlank() }?.let { store.csrfToken = it }
        } catch (_: Exception) {
        }
    }

    fun cachedUser(): AuthUser? {
        val json = store.userJson ?: return null
        return try {
            gson.fromJson(json, AuthUser::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun <T> errorMessage(resp: Response<T>): String {
        val body = resp.errorBody()?.string()
        val apiError = try {
            if (body.isNullOrBlank()) null else gson.fromJson(body, ApiError::class.java)
        } catch (e: Exception) {
            null
        }
        return when {
            !apiError?.message.isNullOrBlank() -> apiError!!.message!!
            resp.code() == 429 -> "Too many failed login attempts. Please try again later."
            resp.code() == 401 -> "Invalid email or password"
            else -> "Request failed (${resp.code()})"
        }
    }
}
