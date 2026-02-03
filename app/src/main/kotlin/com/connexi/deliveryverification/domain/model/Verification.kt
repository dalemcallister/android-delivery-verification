package com.connexi.deliveryverification.domain.model

import com.connexi.deliveryverification.data.local.entities.SyncStatus

data class Verification(
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
