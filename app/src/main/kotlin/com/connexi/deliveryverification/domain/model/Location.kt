package com.connexi.deliveryverification.domain.model

data class Location(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long
)

enum class LocationValidationStatus {
    VALID,
    POOR_ACCURACY,
    TOO_FAR_FROM_TARGET,
    NO_LOCATION
}

data class LocationValidationResult(
    val status: LocationValidationStatus,
    val distanceFromTarget: Float,
    val message: String
)
