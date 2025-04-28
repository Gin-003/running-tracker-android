package com.kbtc.runningtracker.app.api

import com.kbtc.runningtracker.app.data.Workout
import retrofit2.http.*

interface WorkoutApiService {
    @GET("running_tracker_web/api/get_workouts.php")
    suspend fun getWorkouts(@Header("Authorization") token: String): WorkoutsResponse
    
    @POST("running_tracker_web/api/save_workout.php")
    suspend fun saveWorkoutToServer(
        @Header("Authorization") token: String,
        @Body workoutData: WorkoutData
    ): ApiResponse
}

data class WorkoutData(
    val distance: Double,
    val duration: Int,
    val average_speed: Double,
    val calories_burned: Int,
    val start_location: String,
    val end_location: String
)

data class ApiResponse(
    val message: String
)

data class WorkoutsResponse(
    val message: String,
    val records: List<WorkoutRecord>? = null
)

data class WorkoutRecord(
    val id: String,
    val user_id: String,
    val distance: String,
    val duration: String,
    val average_speed: String,
    val calories_burned: String,
    val start_location: String,
    val end_location: String,
    val created_at: String
) 