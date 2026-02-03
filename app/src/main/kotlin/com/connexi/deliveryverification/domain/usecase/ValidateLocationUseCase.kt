package com.connexi.deliveryverification.domain.usecase

import com.connexi.deliveryverification.domain.model.Location
import com.connexi.deliveryverification.domain.model.LocationValidationResult
import com.connexi.deliveryverification.domain.model.LocationValidationStatus
import com.connexi.deliveryverification.util.LocationUtils
import javax.inject.Inject

class ValidateLocationUseCase @Inject constructor() {

    companion object {
        const val MAX_ACCURACY_METERS = 50f
        const val MAX_DISTANCE_FROM_TARGET_METERS = 100f
    }

    operator fun invoke(
        currentLocation: Location?,
        targetLatitude: Double,
        targetLongitude: Double
    ): LocationValidationResult {
        if (currentLocation == null) {
            return LocationValidationResult(
                status = LocationValidationStatus.NO_LOCATION,
                distanceFromTarget = 0f,
                message = "No GPS location available"
            )
        }

        val distanceFromTarget = LocationUtils.calculateDistance(
            currentLocation.latitude,
            currentLocation.longitude,
            targetLatitude,
            targetLongitude
        )

        return when {
            currentLocation.accuracy > MAX_ACCURACY_METERS -> {
                LocationValidationResult(
                    status = LocationValidationStatus.POOR_ACCURACY,
                    distanceFromTarget = distanceFromTarget,
                    message = "GPS accuracy too low: ${currentLocation.accuracy.toInt()}m (required: <${MAX_ACCURACY_METERS.toInt()}m)"
                )
            }
            distanceFromTarget > MAX_DISTANCE_FROM_TARGET_METERS -> {
                LocationValidationResult(
                    status = LocationValidationStatus.TOO_FAR_FROM_TARGET,
                    distanceFromTarget = distanceFromTarget,
                    message = "Too far from delivery location: ${distanceFromTarget.toInt()}m (max: ${MAX_DISTANCE_FROM_TARGET_METERS.toInt()}m)"
                )
            }
            else -> {
                LocationValidationResult(
                    status = LocationValidationStatus.VALID,
                    distanceFromTarget = distanceFromTarget,
                    message = "Location valid: ${distanceFromTarget.toInt()}m from target"
                )
            }
        }
    }
}
