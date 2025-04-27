package com.kbtc.runningtracker.app.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kbtc.runningtracker.app.api.AuthApiService
import com.kbtc.runningtracker.app.api.LoginRequest
import com.kbtc.runningtracker.app.api.RegisterRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

class AuthRepository(
    private val authApiService: AuthApiService,
    private val context: Context
) {
    private object PreferencesKeys {
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val TOKEN = stringPreferencesKey("token")
    }

    val userId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_ID]
    }

    val username: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USERNAME]
    }

    val token: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TOKEN]
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = authApiService.login(LoginRequest(email, password))
            if (response.token != null) {
                saveAuthData(
                    response.user_id ?: "",
                    response.username ?: "",
                    response.token
                )
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(username: String, email: String, password: String): Result<Unit> {
        return try {
            val response = authApiService.register(RegisterRequest(username, email, password))
            if (response.token != null) {
                saveAuthData(
                    response.user_id ?: "",
                    response.username ?: "",
                    response.token
                )
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveAuthData(userId: String, username: String, token: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = userId
            preferences[PreferencesKeys.USERNAME] = username
            preferences[PreferencesKeys.TOKEN] = token
        }
    }

    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
} 