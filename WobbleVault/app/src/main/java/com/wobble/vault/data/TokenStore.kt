package com.wobble.vault.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "session")

class TokenStore(private val context: Context) {

    @Volatile
    var bearer: String? = null
        private set

    @Volatile
    var userJson: String? = null
        private set

    @Volatile
    var csrfToken: String? = null

    private val dataStore get() = context.sessionDataStore

    suspend fun load() {
        val prefs = dataStore.data.first()
        bearer = prefs[KEY_TOKEN]
        userJson = prefs[KEY_USER]
    }

    suspend fun save(token: String, json: String) {
        dataStore.edit {
            it[KEY_TOKEN] = token
            it[KEY_USER] = json
        }
        bearer = token
        userJson = json
    }

    suspend fun updateUserJson(json: String) {
        dataStore.edit { it[KEY_USER] = json }
        userJson = json
    }

    suspend fun clear() {
        dataStore.edit {
            it.remove(KEY_TOKEN)
            it.remove(KEY_USER)
        }
        bearer = null
        userJson = null
        csrfToken = null
    }

    private companion object {
        val KEY_TOKEN = stringPreferencesKey("access_token")
        val KEY_USER = stringPreferencesKey("current_user")
    }
}
