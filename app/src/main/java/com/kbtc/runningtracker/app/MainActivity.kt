package com.kbtc.runningtracker.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import com.kbtc.runningtracker.app.data.Workout
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
                    var selectedWorkout by remember { mutableStateOf<Workout?>(null) }

                    when (authUiState) {
                        is AuthUiState.Success -> {
                            (authUiState as AuthUiState.Success).userId?.let { userId ->
                                workoutViewModel.setUserId(userId.toInt())
                            }
                            (authUiState as AuthUiState.Success).token?.let { token ->
                                workoutViewModel.setAuthToken(token)
                            }
                            
                            if (selectedWorkout != null) {
                                WorkoutDetailsScreen(
                                    workout = selectedWorkout!!,
                                    onNavigateBack = { selectedWorkout = null }
                                )
                            } else {
                                MainScreen(
                                    viewModel = workoutViewModel,
                                    onStartWorkout = { type -> workoutViewModel.startWorkout(type) },
                                    onEndWorkout = { workout -> 
                                        workoutViewModel.endWorkout(
                                            workout = workout,
                                            distance = null,
                                            calories = null,
                                            heartRate = null,
                                            notes = null
                                        )
                                    },
                                    onNavigateToDetails = { workout -> selectedWorkout = workout },
                                    onLogout = { authViewModel.logout() }
                                )
                            }
                        }
                        is AuthUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
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