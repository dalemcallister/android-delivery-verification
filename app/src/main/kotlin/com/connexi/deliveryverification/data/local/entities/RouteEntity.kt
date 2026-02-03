package com.connexi.deliveryverification.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey
    val id: String,
    val routeId: String,
    val vehicleType: String,
    val totalStops: Int,
    val totalDistance: Float,
    val totalVolume: Float,
    val totalWeight: Float,
    val status: RouteStatus,
    val syncStatus: SyncStatus,
    val createdAt: Long
)

enum class RouteStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}

enum class SyncStatus {
    SYNCED,
    PENDING,
    FAILED
}
