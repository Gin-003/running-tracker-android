package com.kbtc.runningtracker.app.ui

import android.app.Application
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kbtc.runningtracker.app.api.ApiResponse
import com.kbtc.runningtracker.app.api.WorkoutApiService
import com.kbtc.runningtracker.app.data.LocationPoint
import com.kbtc.runningtracker.app.data.Workout
import com.kbtc.runningtracker.app.data.WorkoutType
import com.kbtc.runningtracker.app.di.NetworkModule
import com.kbtc.runningtracker.app.location.LocationService
import com.kbtc.runningtracker.app.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.math.roundToInt

sealed class WorkoutUiState {
    data object Loading : WorkoutUiState()
    data class Success(val workouts: List<Workout>) : WorkoutUiState()
    data class Error(val message: String) : WorkoutUiState()
}

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<WorkoutUiState>(WorkoutUiState.Loading)
    val uiState: StateFlow<WorkoutUiState> = _uiState

    // Location tracking
    private val locationService = LocationService(application)
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation
    
    private val _routePoints = MutableStateFlow<List<LocationPoint>>(emptyList())
    val routePoints: StateFlow<List<LocationPoint>> = _routePoints
    
    private var activeWorkout: Workout? = null
    private val _activeWorkoutState = MutableStateFlow<Workout?>(null)
    val activeWorkoutState: StateFlow<Workout?> = _activeWorkoutState
    
    private var isTracking = false

    // Repository for API communication
    private val workoutRepository = WorkoutRepository(NetworkModule.workoutApiService)
    
    // User ID from authentication
    private var userId: Int = 0
    private var authToken: String = ""

    fun setUserId(id: Int) {
        userId = id
        loadWorkouts()
    }

    fun setAuthToken(token: String) {
        authToken = token
    }

    init {
        loadWorkouts()
    }

    fun loadWorkouts() {
        viewModelScope.launch {
            try {
                if (authToken.isNotEmpty()) {
                    val workouts = workoutRepository.getWorkouts(authToken)
                    _uiState.value = WorkoutUiState.Success(workouts)
                } else {
                    _uiState.value = WorkoutUiState.Success(emptyList())
                }
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error("Failed to load workouts: ${e.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startWorkout(type: WorkoutType) {
        viewModelScope.launch {
            try {
                val workout = Workout(
                    id = System.currentTimeMillis(), // Temporary ID generation
                    type = type,
                    startTime = LocalDateTime.now()
                )
                activeWorkout = workout
                _activeWorkoutState.value = workout
                
                // Start location tracking
                startLocationTracking()
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error("Failed to start workout: ${e.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startLocationTracking() {
        viewModelScope.launch {
            isTracking = true
            _routePoints.value = emptyList()
            
            locationService.getLocationUpdates().collect { location ->
                _currentLocation.value = location
                
                if (isTracking) {
                    val locationPoint = locationService.locationToLocationPoint(location)
                    _routePoints.value = _routePoints.value + locationPoint
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun endWorkout(workout: Workout, distance: Double?, calories: Int?, heartRate: Int?, notes: String?) {
        viewModelScope.launch {
            try {
                // Stop location tracking
                isTracking = false
                locationService.stopLocationUpdates()
                
                // Auto-compute metrics if not provided
                val computedDistance = distance ?: calculateDistance(_routePoints.value)
                val computedCalories = calories ?: calculateCalories(computedDistance, workout.type)
                val computedHeartRate = heartRate ?: calculateAverageHeartRate(_routePoints.value, workout.type)
                
                val updatedWorkout = workout.copy(
                    endTime = LocalDateTime.now(),
                    distance = computedDistance,
                    calories = computedCalories,
                    heartRate = computedHeartRate,
                    notes = notes,
                    route = _routePoints.value
                )
                
                // Save workout to server
                val response = workoutRepository.saveWorkoutToServer(updatedWorkout, userId, authToken)
                if (response.message.contains("Failed", ignoreCase = true)) {
                    throw Exception(response.message)
                }
                
                // Clear route points and active workout state
                _routePoints.value = emptyList()
                activeWorkout = null
                _activeWorkoutState.value = null
                
                // Update UI state with the completed workout
                when (val currentState = _uiState.value) {
                    is WorkoutUiState.Success -> {
                        _uiState.value = WorkoutUiState.Success(currentState.workouts + updatedWorkout)
                    }
                    else -> {
                        _uiState.value = WorkoutUiState.Success(listOf(updatedWorkout))
                    }
                }
                
            } catch (e: Exception) {
                _uiState.value = WorkoutUiState.Error("Failed to end workout: ${e.message}")
            }
        }
    }
    
    private suspend fun saveWorkoutToServer(workout: Workout, userId: Int, token: String): ApiResponse {
        return workoutRepository.saveWorkoutToServer(workout, userId, token)
    }
    
    // Calculate total distance in meters based on route points
    private fun calculateDistance(routePoints: List<LocationPoint>): Double {
        if (routePoints.size < 2) return 0.0
        
        var totalDistance = 0.0
        for (i in 0 until routePoints.size - 1) {
            val point1 = routePoints[i]
            val point2 = routePoints[i + 1]
            totalDistance += calculateDistanceBetweenPoints(
                point1.latitude, point1.longitude,
                point2.latitude, point2.longitude
            )
        }
        return totalDistance
    }
    
    // Calculate distance between two points using the Haversine formula
    private fun calculateDistanceBetweenPoints(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371000.0 // Earth radius in meters
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return r * c
    }
    
    // Calculate calories burned based on distance and workout type
    private fun calculateCalories(distance: Double, workoutType: WorkoutType): Int {
        // Approximate calories burned per kilometer based on workout type
        val caloriesPerKm = when (workoutType) {
            WorkoutType.RUNNING -> 60.0
            WorkoutType.WALKING -> 30.0
            WorkoutType.CYCLING -> 25.0
            WorkoutType.SWIMMING -> 70.0
            WorkoutType.OTHER -> 40.0
        }
        
        // Convert distance from meters to kilometers and calculate calories
        val distanceKm = distance / 1000.0
        return (distanceKm * caloriesPerKm).roundToInt()
    }
    
    // Calculate average heart rate based on route points
    private fun calculateAverageHeartRate(routePoints: List<LocationPoint>, workoutType: WorkoutType): Int {
        // If we have speed data, estimate heart rate based on intensity
        if (routePoints.isNotEmpty()) {
            val speeds = routePoints.mapNotNull { it.speed }
            if (speeds.isNotEmpty()) {
                val avgSpeed = speeds.average()
                // Rough estimation: higher speed = higher heart rate
                return when {
                    avgSpeed > 5.0 -> 160 // High intensity
                    avgSpeed > 3.0 -> 140 // Medium intensity
                    else -> 120 // Low intensity
                }
            }
        }
        
        // Default heart rate based on workout type
        return when (workoutType) {
            WorkoutType.RUNNING -> 150
            WorkoutType.WALKING -> 120
            WorkoutType.CYCLING -> 130
            WorkoutType.SWIMMING -> 140
            else -> 125
        }
    }


} 