package com.kbtc.runningtracker.app.repository

import com.kbtc.runningtracker.app.api.ApiResponse
import com.kbtc.runningtracker.app.api.WorkoutApiService
import com.kbtc.runningtracker.app.api.WorkoutData
import com.kbtc.runningtracker.app.api.WorkoutRecord
import com.kbtc.runningtracker.app.data.LocationPoint
import com.kbtc.runningtracker.app.data.Workout
import com.kbtc.runningtracker.app.data.WorkoutType
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class WorkoutRepository(private val apiService: WorkoutApiService) {
    
    suspend fun getWorkouts(token: String): List<Workout> {
        return try {
            val response = apiService.getWorkouts("Bearer $token")
            if (response.message == "No workouts found.") {
                emptyList()
            } else {
                response.records?.map { record ->
                    Workout(
                        id = record.id.toLong(),
                        type = WorkoutType.RUNNING, // Default to RUNNING since the API doesn't provide type
                        startTime = LocalDateTime.parse(record.created_at, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        distance = record.distance.toDouble() * 1000, // Convert km to meters
                        calories = record.calories_burned.toInt(),
                        notes = "Duration: ${record.duration}s, Avg Speed: ${record.average_speed} km/h"
                    )
                } ?: emptyList()
            }
        } catch (e: Exception) {
            // Return empty list for any error, including 404
            emptyList()
        }
    }
    
    suspend fun saveWorkoutToServer(workout: Workout, userId: Int, token: String): ApiResponse {
        return try {
            // Calculate duration in seconds
            val duration = if (workout.endTime != null) {
                Duration.between(workout.startTime, workout.endTime).seconds.toInt()
            } else {
                0
            }
            
            // Calculate average speed in km/h
            val distanceKm = (workout.distance ?: 0.0) / 1000.0
            val durationHours = duration / 3600.0
            val averageSpeed = if (durationHours > 0) distanceKm / durationHours else 0.0
            
            // Get start and end locations
            val startLocation = if (workout.route.isNotEmpty()) {
                "${workout.route.first().latitude},${workout.route.first().longitude}"
            } else {
                "0,0"
            }
            
            val endLocation = if (workout.route.isNotEmpty()) {
                "${workout.route.last().latitude},${workout.route.last().longitude}"
            } else {
                "0,0"
            }
            
            val workoutData = WorkoutData(
                distance = distanceKm,
                duration = duration,
                average_speed = averageSpeed,
                calories_burned = workout.calories ?: 0,
                start_location = startLocation,
                end_location = endLocation
            )
            
            apiService.saveWorkoutToServer("Bearer $token", workoutData)
        } catch (e: Exception) {
            ApiResponse(message = "Failed to save workout: ${e.message}")
        }
    }
    
    // Helper method to calculate distance between two points
    fun calculateDistance(point1: LocationPoint, point2: LocationPoint): Double {
        return calculateDistanceBetweenPoints(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude
        )
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
} 