package com.condosuper.app.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager as AndroidLocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationManager(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()
    
    private val _authorizationStatus = MutableStateFlow<Boolean>(false)
    val authorizationStatus: StateFlow<Boolean> = _authorizationStatus.asStateFlow()
    
    private var locationCallback: LocationCallback? = null

    init {
        checkPermissionStatus()
    }

    fun checkPermissionStatus() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        _authorizationStatus.value = hasPermission
        android.util.Log.d("LocationManager", "Permission status: $hasPermission")
    }

    fun requestPermission() {
        android.util.Log.d("LocationManager", "Requesting location permission...")
        checkPermissionStatus()
    }

    fun startUpdating() {
        if (!_authorizationStatus.value) {
            android.util.Log.w("LocationManager", "Location permission not granted")
            return
        }

        android.util.Log.d("LocationManager", "Starting location updates...")
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdateDelayMillis(10000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    _location.value = location
                    android.util.Log.d(
                        "LocationManager",
                        "Location updated: ${location.latitude}, ${location.longitude}"
                    )
                }
            }
        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        }
    }

    fun stopUpdating() {
        android.util.Log.d("LocationManager", "Stopping location updates...")
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    suspend fun getCurrentLocation(): Location? {
        if (!_authorizationStatus.value) return null
        
        return try {
            val location = fusedLocationClient.lastLocation.await()
            location?.let { _location.value = it }
            location
        } catch (e: Exception) {
            android.util.Log.e("LocationManager", "Error getting current location", e)
            null
        }
    }
}

// Extension function for Task
suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return kotlinx.coroutines.tasks.await(this)
}


