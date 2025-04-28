package com.kbtc.runningtracker.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kbtc.runningtracker.app.di.NetworkModule
import com.kbtc.runningtracker.app.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Initial : AuthUiState()
    data object Loading : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    data class Success(val userId: String, val username: String, val token: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(NetworkModule.authApiService, application)
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            authRepository.userId.collect { userId ->
                if (userId != null) {
                    authRepository.username.collect { username ->
                        authRepository.token.collect { token ->
                            if (token != null) {
                                _uiState.value = AuthUiState.Success(userId, username ?: "", token)
                            }
                        }
                    }
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                authRepository.login(email, password)
                    .onSuccess {
                        checkAuthState()
                    }
                    .onFailure { exception ->
                        _uiState.value = AuthUiState.Error(exception.message ?: "Login failed")
                    }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = authRepository.register(username, email, password)
                if (response.user_id > 0) {
                    _uiState.value = AuthUiState.Success(
                        userId = response.user_id.toString(),
                        username = username,
                        token = ""
                    )
                } else {
                    _uiState.value = AuthUiState.Error(response.message)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Registration failed: ${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState.Initial
        }
    }
} 