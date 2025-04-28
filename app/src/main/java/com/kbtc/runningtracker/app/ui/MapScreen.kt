package com.kbtc.runningtracker.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.kbtc.runningtracker.app.data.LocationPoint
import com.kbtc.runningtracker.app.data.Workout
import com.kbtc.runningtracker.app.location.LocationService
import kotlinx.coroutines.launch

@Composable
fun MapScreen(
    workout: Workout? = null,
    currentLocation: Location? = null,
    onLocationUpdate: (Location) -> Unit = {}
) {
    val context = LocalContext.current
    val locationService = remember { LocationService(context) }
    val scope = rememberCoroutineScope()
    var locationPermissionGranted by remember { mutableStateOf(false) }
    
    // Check if we have location permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions.entries.all { it.value }
    }
    
    // Check permissions on composition
    LaunchedEffect(Unit) {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        locationPermissionGranted = hasFineLocation || hasCoarseLocation
        
        if (!locationPermissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    // Handle location updates
    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            scope.launch {
                try {
                    locationService.getLocationUpdates().collect { location ->
                        onLocationUpdate(location)
                    }
                } catch (e: SecurityException) {
                    // Handle permission error
                    e.printStackTrace()
                }
            }
        }
    }
    
    // Default to Yangon, Myanmar if no location is available
    val defaultLocation = LatLng(16.8409, 96.1735)
    val cameraPositionState = rememberCameraPositionState()
    
    // Update camera position when location changes
    LaunchedEffect(workout?.route?.firstOrNull(), currentLocation) {
        val targetLocation = workout?.route?.firstOrNull()?.let { 
            LatLng(it.latitude, it.longitude)
        } ?: currentLocation?.let {
            LatLng(it.latitude, it.longitude)
        } ?: defaultLocation
        
        cameraPositionState.position = CameraPosition.fromLatLngZoom(targetLocation, 15f)
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = locationPermissionGranted,
                mapType = MapType.NORMAL
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = true
            )
        ) {
            // Show current location marker if available
            currentLocation?.let { location ->
                Marker(
                    state = MarkerState(
                        position = LatLng(location.latitude, location.longitude)
                    ),
                    title = "Current Location"
                )
            }
            
            // Show workout route if available
            workout?.route?.let { points ->
                if (points.isNotEmpty()) {
                    // Draw the route
                    Polyline(
                        points = points.map { LatLng(it.latitude, it.longitude) },
                        color = androidx.compose.ui.graphics.Color.Blue,
                        width = 5f
                    )
                    
                    // Add start and end markers
                    Marker(
                        state = MarkerState(
                            position = LatLng(points.first().latitude, points.first().longitude)
                        ),
                        title = "Start"
                    )
                    
                    if (points.size > 1) {
                        Marker(
                            state = MarkerState(
                                position = LatLng(points.last().latitude, points.last().longitude)
                            ),
                            title = "End"
                        )
                    }
                }
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