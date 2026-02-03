package com.connexi.deliveryverification.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "deliveries",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routeId")]
)
data class DeliveryEntity(
    @PrimaryKey
    val id: String,
    val routeId: String,
    val facilityId: String,
    val facilityName: String,
    val latitude: Double,
    val longitude: Double,
    val orderVolume: Float,
    val orderWeight: Float,
    val stopNumber: Int,
    val distanceFromPrevious: Float,
    val status: DeliveryStatus,
    val verifiedAt: Long? = null,
    val syncStatus: SyncStatus
)

enum class DeliveryStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
