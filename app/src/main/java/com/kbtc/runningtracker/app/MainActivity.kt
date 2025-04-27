package com.kbtc.runningtracker.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

import com.kbtc.runningtracker.app.data.WorkoutType
import com.kbtc.runningtracker.app.ui.*
import com.kbtc.runningtracker.app.ui.AuthViewModel
import com.kbtc.runningtracker.app.ui.theme.RunningTrackerTheme

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val workoutViewModel: WorkoutViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RunningTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authUiState by authViewModel.uiState.collectAsState()
                    var showLogin by remember { mutableStateOf(true) }

                    when (authUiState) {
                        is AuthUiState.Success -> {
                            (authUiState as AuthUiState.Success).userId?.let { userId ->
                                workoutViewModel.setUserId(userId.toInt())
                            }
                            
                            MainScreen(
                                viewModel = workoutViewModel,
                                onStartWorkout = { type -> workoutViewModel.startWorkout(type) },
                                onEndWorkout = { workout -> workoutViewModel.endWorkout(workout, workout.distance, workout.calories, workout.heartRate, workout.notes) }
                            )
                        }
                        is AuthUiState.Loading -> {
                            // Show loading indicator
                        }
                        is AuthUiState.Error -> {
                            if (showLogin) {
                                LoginScreen(
                                    onLogin = { email, password -> 
                                        authViewModel.login(email, password)
                                    },
                                    onNavigateToRegister = { showLogin = false },
                                    isLoading = false,
                                    error = (authUiState as AuthUiState.Error).message
                                )
                            } else {
                                RegisterScreen(
                                    onRegister = { username, email, password ->
                                        authViewModel.register(username, email, password)
                                    },
                                    onNavigateToLogin = { showLogin = true },
                                    isLoading = false,
                                    error = (authUiState as AuthUiState.Error).message
                                )
                            }
                        }
                        AuthUiState.Initial -> {
                            if (showLogin) {
                                LoginScreen(
                                    onLogin = { email, password -> 
                                        authViewModel.login(email, password)
                                    },
                                    onNavigateToRegister = { showLogin = false }
                                )
                            } else {
                                RegisterScreen(
                                    onRegister = { username, email, password ->
                                        authViewModel.register(username, email, password)
                                    },
                                    onNavigateToLogin = { showLogin = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}