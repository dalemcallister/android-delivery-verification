package com.connexi.deliveryverification.util

import kotlin.math.*

object LocationUtils {

    /**
     * Calculate distance between two coordinates using Haversine formula
     * @return distance in meters
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val earthRadius = 6371000.0 // meters

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (earthRadius * c).toFloat()
    }

    /**
     * Format distance for display
     */
    fun formatDistance(meters: Float): String {
        return when {
            meters < 1000 -> "${meters.toInt()}m"
            else -> "${"%.2f".format(meters / 1000)}km"
        }
    }

    /**
     * Format coordinates for display
     */
    fun formatCoordinates(latitude: Double, longitude: Double): String {
        return "${"%.6f".format(latitude)}, ${"%.6f".format(longitude)}"
    }

    /**
     * Validate if coordinates are valid
     */
    fun isValidCoordinates(latitude: Double, longitude: Double): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    /**
     * Parse coordinates string in format "[longitude,latitude]" (DHIS2 format)
     */
    fun parseCoordinates(coordinates: String?): Pair<Double, Double>? {
        if (coordinates.isNullOrBlank()) return null

        return try {
            val cleaned = coordinates.trim('[', ']', ' ')
            val parts = cleaned.split(',')
            if (parts.size == 2) {
                val longitude = parts[0].trim().toDouble()
                val latitude = parts[1].trim().toDouble()
                if (isValidCoordinates(latitude, longitude)) {
                    Pair(latitude, longitude)
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
