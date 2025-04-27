package com.kbtc.runningtracker.app.data

import java.time.LocalDateTime

data class Workout(
    val id: Long = 0,
    val type: WorkoutType,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val distance: Double? = null, // in meters
    val calories: Int? = null,
    val heartRate: Int? = null, // average heart rate
    val notes: String? = null,
    val route: List<LocationPoint> = emptyList() // List of location points for the workout route
)

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: LocalDateTime,
    val speed: Float? = null, // in meters per second
    val altitude: Float? = null // in meters
)

enum class WorkoutType {
    RUNNING,
    WALKING,
    CYCLING,
    SWIMMING,
    OTHER
} 