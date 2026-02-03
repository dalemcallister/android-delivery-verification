package com.connexi.deliveryverification.data.repository

import com.connexi.deliveryverification.data.local.AppDatabase
import com.connexi.deliveryverification.data.local.entities.*
import com.connexi.deliveryverification.data.remote.dto.EventsListResponse
import com.connexi.deliveryverification.domain.model.Delivery
import com.connexi.deliveryverification.domain.model.Route
import com.connexi.deliveryverification.util.LocationUtils
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepository @Inject constructor(
    private val database: AppDatabase,
    private val authRepository: AuthRepository,
    private val gson: Gson
) {
    private val routeDao = database.routeDao()
    private val deliveryDao = database.deliveryDao()

    /**
     * Get all routes from local database
     */
    fun getRoutes(): Flow<List<Route>> {
        return routeDao.getAllRoutes().map { routeEntities ->
            routeEntities.map { entity ->
                val deliveries = deliveryDao.getDeliveriesByRoute(entity.id)
                    .map { deliveryEntities ->
                        deliveryEntities.map { it.toDomain() }
                    }
                entity.toDomain(deliveries.map { emptyList() }.first())
            }
        }
    }

    /**
     * Get route by ID with deliveries
     */
    fun getRouteById(routeId: String): Flow<Route?> {
        return routeDao.getRouteById(routeId).combine(
            deliveryDao.getDeliveriesByRoute(routeId)
        ) { routeEntity, deliveryEntities ->
            routeEntity?.toDomain(deliveryEntities.map { it.toDomain() })
        }
    }

    /**
     * Fetch routes from DHIS2 server
     */
    suspend fun fetchRoutesFromRemote(): Result<List<Route>> {
        return try {
            val service = authRepository.getDHIS2Service()
                ?: return Result.failure(Exception("Not logged in"))

            // TODO: Replace with actual program ID from DHIS2
            // This should fetch events from "Route Assignment Program"
            val programId = "ROUTE_PROGRAM_ID" // Placeholder
            val response = service.getEvents(
                programId = programId,
                ouMode = "ACCESSIBLE"
            )

            if (response.isSuccessful) {
                val events = response.body()?.events ?: emptyList()
                val routes = parseRoutesFromEvents(events)

                // Save to local database
                routes.forEach { route ->
                    val routeEntity = route.toEntity()
                    routeDao.insertRoute(routeEntity)

                    val deliveryEntities = route.deliveries.map { it.toEntity() }
                    deliveryDao.insertDeliveries(deliveryEntities)
                }

                Timber.d("Fetched ${routes.size} routes from DHIS2")
                Result.success(routes)
            } else {
                val error = "Failed to fetch routes: ${response.code()} ${response.message()}"
                Timber.e(error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching routes")
            Result.failure(e)
        }
    }

    /**
     * Update route status
     */
    suspend fun updateRouteStatus(routeId: String, status: RouteStatus) {
        val route = routeDao.getRouteById(routeId).map { it }
            .collect { routeEntity ->
                routeEntity?.let {
                    routeDao.updateRoute(it.copy(status = status))
                }
            }
    }

    /**
     * Parse routes from DHIS2 events
     */
    private fun parseRoutesFromEvents(events: List<com.connexi.deliveryverification.data.remote.dto.EventDto>): List<Route> {
        // This is a placeholder implementation
        // In real implementation, parse event data values to extract route information
        return events.mapNotNull { event ->
            try {
                // Extract route data from event dataValues
                val dataValuesMap = event.dataValues.associate { it.dataElement to it.value }

                // TODO: Map actual data element IDs
                val routeId = dataValuesMap["ROUTE_ID_DE"] ?: UUID.randomUUID().toString()
                val vehicleType = dataValuesMap["VEHICLE_TYPE_DE"] ?: "TRUCK"
                val stopsJson = dataValuesMap["STOPS_JSON_DE"] ?: "[]"

                // Parse stops from JSON
                val stops = gson.fromJson(stopsJson, Array<StopData>::class.java).toList()

                Route(
                    id = event.event ?: UUID.randomUUID().toString(),
                    routeId = routeId,
                    vehicleType = vehicleType,
                    totalStops = stops.size,
                    totalDistance = stops.sumOf { it.distanceFromPrevious.toDouble() }.toFloat(),
                    totalVolume = stops.sumOf { it.orderVolume.toDouble() }.toFloat(),
                    totalWeight = stops.sumOf { it.orderWeight.toDouble() }.toFloat(),
                    status = RouteStatus.PENDING,
                    syncStatus = SyncStatus.SYNCED,
                    createdAt = System.currentTimeMillis(),
                    deliveries = stops.map { stop ->
                        Delivery(
                            id = UUID.randomUUID().toString(),
                            routeId = routeId,
                            facilityId = stop.facilityId,
                            facilityName = stop.facilityName,
                            latitude = stop.latitude,
                            longitude = stop.longitude,
                            orderVolume = stop.orderVolume,
                            orderWeight = stop.orderWeight,
                            stopNumber = stop.stopNumber,
                            distanceFromPrevious = stop.distanceFromPrevious,
                            status = DeliveryStatus.PENDING,
                            syncStatus = SyncStatus.SYNCED
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse route from event")
                null
            }
        }
    }

    private data class StopData(
        val facilityId: String,
        val facilityName: String,
        val latitude: Double,
        val longitude: Double,
        val orderVolume: Float,
        val orderWeight: Float,
        val stopNumber: Int,
        val distanceFromPrevious: Float
    )
}

// Extension functions for mapping
private fun RouteEntity.toDomain(deliveries: List<Delivery>) = Route(
    id = id,
    routeId = routeId,
    vehicleType = vehicleType,
    totalStops = totalStops,
    totalDistance = totalDistance,
    totalVolume = totalVolume,
    totalWeight = totalWeight,
    status = status,
    syncStatus = syncStatus,
    createdAt = createdAt,
    deliveries = deliveries
)

private fun Route.toEntity() = RouteEntity(
    id = id,
    routeId = routeId,
    vehicleType = vehicleType,
    totalStops = totalStops,
    totalDistance = totalDistance,
    totalVolume = totalVolume,
    totalWeight = totalWeight,
    status = status,
    syncStatus = syncStatus,
    createdAt = createdAt
)

private fun DeliveryEntity.toDomain() = Delivery(
    id = id,
    routeId = routeId,
    facilityId = facilityId,
    facilityName = facilityName,
    latitude = latitude,
    longitude = longitude,
    orderVolume = orderVolume,
    orderWeight = orderWeight,
    stopNumber = stopNumber,
    distanceFromPrevious = distanceFromPrevious,
    status = status,
    verifiedAt = verifiedAt,
    syncStatus = syncStatus
)

private fun Delivery.toEntity() = DeliveryEntity(
    id = id,
    routeId = routeId,
    facilityId = facilityId,
    facilityName = facilityName,
    latitude = latitude,
    longitude = longitude,
    orderVolume = orderVolume,
    orderWeight = orderWeight,
    stopNumber = stopNumber,
    distanceFromPrevious = distanceFromPrevious,
    status = status,
    verifiedAt = verifiedAt,
    syncStatus = syncStatus
)
