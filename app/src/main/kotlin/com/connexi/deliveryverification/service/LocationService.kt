package com.connexi.deliveryverification.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Looper
import com.connexi.deliveryverification.domain.model.Location
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        5000L // 5 seconds
    ).apply {
        setMinUpdateIntervalMillis(2000L) // 2 seconds
        setMaxUpdateDelayMillis(10000L) // 10 seconds
    }.build()

    /**
     * Check if location services are enabled
     */
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Get current location once
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        if (!isLocationEnabled()) {
            Timber.w("Location services are disabled")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    Timber.d("Got location: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}")
                    continuation.resume(
                        Location(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            timestamp = location.time
                        )
                    )
                } else {
                    // Last location not available, request fresh location
                    requestFreshLocation { freshLocation ->
                        continuation.resume(freshLocation)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Timber.e(exception, "Failed to get location")
                continuation.resume(null)
            }
    }

    /**
     * Get continuous location updates
     */
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(): Flow<Location?> = callbackFlow {
        if (!isLocationEnabled()) {
            Timber.w("Location services are disabled")
            trySend(null)
            close()
            return@callbackFlow
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Timber.d("Location update: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}")
                    trySend(
                        Location(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            timestamp = location.time
                        )
                    )
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    Timber.w("Location not available")
                    trySend(null)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )

        awaitClose {
            Timber.d("Stopping location updates")
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    /**
     * Request a fresh location update
     */
    @SuppressLint("MissingPermission")
    private fun requestFreshLocation(callback: (Location?) -> Unit) {
        val freshLocationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            0L
        ).apply {
            setMaxUpdates(1)
        }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    callback(
                        Location(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            timestamp = location.time
                        )
                    )
                } ?: callback(null)
            }
        }

        fusedLocationClient.requestLocationUpdates(
            freshLocationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }
}
