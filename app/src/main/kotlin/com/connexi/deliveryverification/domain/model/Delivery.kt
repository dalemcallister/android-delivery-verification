package com.connexi.deliveryverification.domain.model

import com.connexi.deliveryverification.data.local.entities.DeliveryStatus
import com.connexi.deliveryverification.data.local.entities.SyncStatus

data class Delivery(
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
    val syncStatus: SyncStatus,
    val verification: Verification? = null
)
