package com.kbtc.runningtracker.app.api

import com.kbtc.runningtracker.app.data.Workout
import retrofit2.http.*

interface WorkoutApiService {
    @GET("workouts")
    suspend fun getAllWorkouts(): List<Workout>

    @GET("workouts/{id}")
    suspend fun getWorkoutById(@Path("id") id: Long): Workout

    @POST("workouts")
    suspend fun createWorkout(@Body workout: Workout): Workout

    @PUT("workouts/{id}")
    suspend fun updateWorkout(@Path("id") id: Long, @Body workout: Workout): Workout

    @DELETE("workouts/{id}")
    suspend fun deleteWorkout(@Path("id") id: Long)
    
    @POST("running_tracker_web/api/save_workout.php")
    suspend fun saveWorkoutToServer(@Body workoutData: WorkoutData): ApiResponse
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