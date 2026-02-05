package com.connexi.deliveryverification.domain.model

data class DriverProfile(
    val userId: String,
    val username: String,
    val driverName: String,
    val assignedTruckId: String,
    val assignedTruckName: String,
    val assignedTruckCode: String?
)
