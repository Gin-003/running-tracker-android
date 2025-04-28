package com.kbtc.runningtracker.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationCallback
import com.kbtc.runningtracker.app.data.LocationPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.LocalDateTime

class LocationService(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        5000 // Update interval in milliseconds
    ).apply {
        setMinUpdateDistanceMeters(5f) // Minimum distance between updates in meters
        setMinUpdateIntervalMillis(3000) // Minimum time between updates in milliseconds
        setMaxUpdateDelayMillis(10000) // Maximum delay between updates in milliseconds
    }.build()
    
    private val locationChannel = Channel<Location>(Channel.BUFFERED)
    val locationFlow = locationChannel.receiveAsFlow()
    
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                locationChannel.trySend(location)
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(): Flow<Location> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    trySend(location)
                }
            }
        }
        
        try {
            val hasLocationPermission = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            if (hasLocationPermission) {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )
            } else {
                throw SecurityException("Location permission not granted")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    fun locationToLocationPoint(location: Location): LocationPoint {
        return LocationPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = LocalDateTime.now(),
            speed = location.speed.toFloat(),
            altitude = location.altitude.toFloat()
        )
    }
} 