package com.connexi.deliveryverification.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "verifications",
    foreignKeys = [
        ForeignKey(
            entity = DeliveryEntity::class,
            parentColumns = ["id"],
            childColumns = ["deliveryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deliveryId"), Index("syncStatus")]
)
data class VerificationEntity(
    @PrimaryKey
    val id: String,
    val deliveryId: String,
    val gpsLatitude: Double,
    val gpsLongitude: Double,
    val gpsAccuracy: Float,
    val distanceFromTarget: Float,
    val actualVolume: Float,
    val actualWeight: Float,
    val comments: String? = null,
    val signatureBase64: String? = null,
    val photoBase64: String? = null,
    val photoLocalUri: String? = null,
    val verifiedAt: Long,
    val dhis2EventId: String? = null,
    val syncStatus: SyncStatus
)
