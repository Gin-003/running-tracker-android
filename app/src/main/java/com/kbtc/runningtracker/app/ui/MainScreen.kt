package com.kbtc.runningtracker.app.ui

import android.Manifest
import android.location.Location
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kbtc.runningtracker.app.data.Workout
import com.kbtc.runningtracker.app.data.WorkoutType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: WorkoutViewModel,
    onStartWorkout: (WorkoutType) -> Unit,
    onEndWorkout: (Workout) -> Unit,
    onNavigateToDetails: (Workout) -> Unit,
    onLogout: () -> Unit
) {
    var showWorkoutDialog by remember { mutableStateOf(false) }
    var selectedWorkoutType by remember { mutableStateOf<WorkoutType?>(null) }
    var showConfirmationAlert by remember { mutableStateOf(false) }
    var confirmationMessage by remember { mutableStateOf("") }
    var isLoadingWorkouts by remember { mutableStateOf(false) }

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

    // Fetch workouts when MainScreen is first displayed
    LaunchedEffect(Unit) {
        isLoadingWorkouts = true
        viewModel.loadWorkouts()
        isLoadingWorkouts = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Running Tracker") },
                actions = {
                    IconButton(onClick = { onLogout() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showWorkoutDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Start Workout")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = (uiState as WorkoutUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                is WorkoutUiState.Success -> {
                    val workouts = (uiState as WorkoutUiState.Success).workouts
                    
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Show active workout if it exists
                        activeWorkout?.let { workout ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Active Workout",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = MaterialTheme.typography.titleMedium.fontWeight
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Type: ${workout.type.name}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Started: ${workout.startTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
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

                            // Show map with current location and route
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                MapScreen(
                                    workout = workout,
                                    currentLocation = currentLocation,
                                    onLocationUpdate = { location ->
                                        // Handle location updates if needed
                                    }
                                )
                            }
                        }

                        // Show workouts list
                        if (workouts.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "No workouts yet",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Start your first workout to track your progress",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showWorkoutDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Start Workout")
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(workouts) { workout ->
                                    WorkoutItem(
                                        workout = workout,
                                        onItemClick = { onNavigateToDetails(workout) }
                                    )
                                }
                            }
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