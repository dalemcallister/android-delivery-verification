package com.connexi.deliveryverification.domain.model

import com.connexi.deliveryverification.data.local.entities.RouteStatus
import com.connexi.deliveryverification.data.local.entities.SyncStatus

data class Route(
    val id: String,
    val routeId: String,
    val vehicleType: String,
    val totalStops: Int,
    val totalDistance: Float,
    val totalVolume: Float,
    val totalWeight: Float,
    val status: RouteStatus,
    val syncStatus: SyncStatus,
    val createdAt: Long,
    val deliveries: List<Delivery> = emptyList()
) {
    val completedStops: Int
        get() = deliveries.count { it.status == com.connexi.deliveryverification.data.local.entities.DeliveryStatus.COMPLETED }

    val progressPercent: Int
        get() = if (totalStops > 0) (completedStops * 100) / totalStops else 0
}
