package com.connexi.deliveryverification.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RouteDto(
    @SerializedName("route_id")
    val routeId: String,
    @SerializedName("vehicle_type")
    val vehicleType: String,
    @SerializedName("total_stops")
    val totalStops: Int,
    @SerializedName("total_distance")
    val totalDistance: Float,
    @SerializedName("total_volume")
    val totalVolume: Float,
    @SerializedName("total_weight")
    val totalWeight: Float,
    @SerializedName("stops")
    val stops: List<StopDto>
)

data class StopDto(
    @SerializedName("facility_id")
    val facilityId: String,
    @SerializedName("facility_name")
    val facilityName: String,
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("order_volume")
    val orderVolume: Float,
    @SerializedName("order_weight")
    val orderWeight: Float,
    @SerializedName("stop_number")
    val stopNumber: Int,
    @SerializedName("distance_from_previous")
    val distanceFromPrevious: Float
)

data class RoutesResponse(
    @SerializedName("routes")
    val routes: List<RouteDto>
)
