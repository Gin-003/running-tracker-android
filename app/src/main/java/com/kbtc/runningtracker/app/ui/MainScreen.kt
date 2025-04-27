package com.kbtc.runningtracker.app.ui

import android.Manifest
import android.location.Location
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kbtc.runningtracker.app.data.LocationPoint
import com.kbtc.runningtracker.app.data.Workout
import com.kbtc.runningtracker.app.data.WorkoutType
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: WorkoutViewModel,
    onStartWorkout: (WorkoutType) -> Unit,
    onEndWorkout: (Workout) -> Unit
) {
    var showWorkoutDialog by remember { mutableStateOf(false) }
    var selectedWorkoutType by remember { mutableStateOf<WorkoutType?>(null) }
    var selectedWorkout by remember { mutableStateOf<Workout?>(null) }
    var showConfirmationAlert by remember { mutableStateOf(false) }
    var confirmationMessage by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val routePoints by viewModel.routePoints.collectAsState()
    val activeWorkout by viewModel.activeWorkoutState.collectAsState()
    
    // Request location permissions
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Fitness Tracker",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = { showWorkoutDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start New Workout")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Always show map
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(bottom = 16.dp)
        ) {
            MapScreen(
                workout = selectedWorkout,
                currentLocation = currentLocation,
                routePoints = routePoints
            )
        }

        when (uiState) {
            is WorkoutUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is WorkoutUiState.Error -> {
                Text(
                    text = (uiState as WorkoutUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            is WorkoutUiState.Success -> {
                val workouts = (uiState as WorkoutUiState.Success).workouts
                
                // Show active workout if it exists
                if (activeWorkout != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Active Workout: ${activeWorkout?.type?.name}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                Text(
                                    text = "Started: ${activeWorkout?.startTime?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Button(
                                onClick = { 
                                    activeWorkout?.let { workout ->
                                        onEndWorkout(workout)
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("End Workout")
                            }
                        }
                    }
                }
                
                if (workouts.isEmpty()) {
                    Text(
                        text = "No workouts yet. Start your first workout!",
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn {
                        items(workouts) { workout ->
                            WorkoutItem(
                                workout = workout,
                                onEndWorkout = { 
                                    // Auto-save the workout
                                    onEndWorkout(workout)
                                    // Show confirmation alert
                                    confirmationMessage = "Workout saved successfully!"
                                    showConfirmationAlert = true
                                },
                                onSelectWorkout = { selectedWorkout = workout }
                            )
                        }
                    }
                }
            }
        }
    }

    // Request location permissions if not granted
    LaunchedEffect(Unit) {
        if (!locationPermissionsState.allPermissionsGranted) {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }

    if (showWorkoutDialog) {
        AlertDialog(
            onDismissRequest = { showWorkoutDialog = false },
            title = { Text("Select Workout Type") },
            text = {
                Column {
                    WorkoutType.values().forEach { type ->
                        TextButton(
                            onClick = {
                                selectedWorkoutType = type
                                onStartWorkout(type)
                                showWorkoutDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(type.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showWorkoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Alert
    if (showConfirmationAlert) {
        AlertDialog(
            onDismissRequest = { showConfirmationAlert = false },
            title = { Text("Success") },
            text = { Text(confirmationMessage) },
            confirmButton = {
                TextButton(onClick = { showConfirmationAlert = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun WorkoutItem(
    workout: Workout,
    onEndWorkout: () -> Unit,
    onSelectWorkout: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onSelectWorkout() }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = workout.type.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Started: ${workout.startTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (workout.endTime == null) {
                Button(
                    onClick = onEndWorkout,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("End Workout")
                }
            } else {
                Text(
                    text = "Ended: ${workout.endTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))}",
                    style = MaterialTheme.typography.bodyMedium
                )
                workout.distance?.let {
                    Text("Distance: ${it / 1000} km")
                }
                workout.calories?.let {
                    Text("Calories: $it")
                }
                workout.heartRate?.let {
                    Text("Heart Rate: $it bpm")
                }
                workout.notes?.let {
                    Text("Notes: $it")
                }
                
                if (workout.route.isNotEmpty()) {
                    Text("Route recorded: ${workout.route.size} points")
                }
            }
        }
    }
}

// Helper functions for auto-computing workout metrics
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

private fun calculateAverageHeartRate(routePoints: List<LocationPoint>): Int {
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
    return getDefaultHeartRate(WorkoutType.OTHER)
}

private fun getDefaultHeartRate(workoutType: WorkoutType): Int {
    return when (workoutType) {
        WorkoutType.RUNNING -> 150
        WorkoutType.WALKING -> 120
        WorkoutType.CYCLING -> 130
        WorkoutType.SWIMMING -> 140
        WorkoutType.OTHER -> 125
    }
} 