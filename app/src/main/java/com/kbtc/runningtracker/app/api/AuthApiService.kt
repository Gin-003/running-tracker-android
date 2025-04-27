package com.kbtc.runningtracker.app.api

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("running_tracker_web/api/login.php")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("running_tracker_web/api/register.php")
    suspend fun register(@Body request: RegisterRequest): AuthResponse
}

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class AuthResponse(
    val message: String,
    val user_id: String? = null,
    val username: String? = null,
    val token: String? = null
) 