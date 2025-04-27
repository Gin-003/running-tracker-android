package com.kbtc.runningtracker.app.ui

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.kbtc.runningtracker.app.data.LocationPoint
import com.kbtc.runningtracker.app.data.Workout

@Composable
fun MapScreen(
    workout: Workout?,
    currentLocation: Location?,
    routePoints: List<LocationPoint>,
    modifier: Modifier = Modifier
) {
    var cameraPositionState = rememberCameraPositionState()
    
    // Default to a default location if no workout or current location
    val defaultLocation = LatLng(37.5665, 126.9780) // Seoul, South Korea
    
    // Determine the center of the map
    val centerLocation = when {
        workout != null && workout.route.isNotEmpty() -> {
            // Use the first point of the workout route
            LatLng(workout.route.first().latitude, workout.route.first().longitude)
        }
        currentLocation != null -> {
            // Use current location
            LatLng(currentLocation.latitude, currentLocation.longitude)
        }
        else -> defaultLocation
    }
    
    // Update camera position when center location changes
    LaunchedEffect(centerLocation) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(centerLocation, 15f)
    }
    
    // Create a list of LatLng points for the route
    val routeLatLngs = when {
        workout != null && workout.route.isNotEmpty() -> {
            workout.route.map { LatLng(it.latitude, it.longitude) }
        }
        routePoints.isNotEmpty() -> {
            routePoints.map { LatLng(it.latitude, it.longitude) }
        }
        else -> emptyList()
    }
    
    // Create a list of LatLng points for the markers
    val markerLatLngs = when {
        workout != null && workout.route.isNotEmpty() -> {
            listOf(
                workout.route.first().let { LatLng(it.latitude, it.longitude) }, // Start
                workout.route.last().let { LatLng(it.latitude, it.longitude) }   // End
            )
        }
        routePoints.isNotEmpty() -> {
            listOf(
                routePoints.first().let { LatLng(it.latitude, it.longitude) }, // Start
                routePoints.last().let { LatLng(it.latitude, it.longitude) }   // End
            )
        }
        else -> emptyList()
    }
    
    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            // Draw the route polyline
            if (routeLatLngs.isNotEmpty()) {
                Polyline(
                    points = routeLatLngs,
                    color = MaterialTheme.colorScheme.primary,
                    width = 5f
                )
            }
            
            // Add markers for start and end points
            markerLatLngs.forEachIndexed { index, latLng ->
                Marker(
                    state = MarkerState(position = latLng),
                    title = if (index == 0) "Start" else "End",
                    snippet = if (index == 0) "Workout started here" else "Workout ended here"
                )
            }
            
            // Add a marker for current location if available and not part of a workout
            if (currentLocation != null && workout == null) {
                Marker(
                    state = MarkerState(position = LatLng(currentLocation.latitude, currentLocation.longitude)),
                    title = "Current Location",
                    snippet = "You are here"
                )
            }
        }
        
        // Show workout stats if available
        workout?.let { workout ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(androidx.compose.ui.Alignment.TopCenter)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Workout Stats",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    workout.distance?.let {
                        Text("Distance: ${String.format("%.2f", it / 1000)} km")
                    }
                    
                    workout.calories?.let {
                        Text("Calories: $it")
                    }
                    
                    workout.heartRate?.let {
                        Text("Heart Rate: $it bpm")
                    }
                }
            }
        }
    }
} 