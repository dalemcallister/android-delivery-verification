package com.connexi.deliveryverification.data.repository

import com.connexi.deliveryverification.data.local.AppDatabase
import com.connexi.deliveryverification.data.local.entities.*
import com.connexi.deliveryverification.data.remote.EventsListResponse
import com.connexi.deliveryverification.domain.model.Delivery
import com.connexi.deliveryverification.domain.model.Route
import com.connexi.deliveryverification.util.LocationUtils
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
                // Return routes without deliveries for list view
                // Use getRouteById() to get full details with deliveries
                entity.toDomain(emptyList())
            }
        }
    }

    /**
     * Get route by ID with deliveries
     */
    fun getRouteById(routeId: String): Flow<Route?> {
        Timber.d("getRouteById called with routeId: $routeId")
        return routeDao.getRouteById(routeId).combine(
            deliveryDao.getDeliveriesByRoute(routeId)
        ) { routeEntity, deliveryEntities ->
            Timber.d("RouteEntity: ${routeEntity?.id}, ${routeEntity?.routeId}")
            Timber.d("Delivery entities count: ${deliveryEntities.size}")
            deliveryEntities.forEach { delivery ->
                Timber.d("  - Delivery entity: ${delivery.facilityName}, routeId=${delivery.routeId}")
            }
            routeEntity?.toDomain(deliveryEntities.map { it.toDomain() })
        }
    }

    /**
     * Fetch routes from DHIS2 server (from data values)
     */
    suspend fun fetchRoutesFromRemote(): Result<List<Route>> {
        return try {
            val service = authRepository.getDHIS2Service()
                ?: return Result.failure(Exception("Not logged in"))

            // Data element UIDs for route information
            val ROUTE_ID_DE = "kLPeW2Yx9Zy"
            val ROUTE_DETAILS_DE = "nBv8JxPq1Rs"
            val ROUTE_STATUS_DE = "pYzQ3Wm8Ktx"
            val VEHICLE_TYPE_DE = "mXc7V2Np5Wq"

            // Get current period (YYYYMM format)
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
            val period = "$year$month"

            // Fetch data values for route information
            val response = service.getDataValueSets(
                dataElements = "$ROUTE_ID_DE,$ROUTE_DETAILS_DE,$ROUTE_STATUS_DE,$VEHICLE_TYPE_DE",
                period = period,
                orgUnitMode = "ACCESSIBLE"
            )

            if (response.isSuccessful) {
                val dataValues = response.body()?.dataValues ?: emptyList()
                Timber.d("Fetched ${dataValues.size} data values")

                // Group data values by orgUnit to reconstruct routes
                val routesByOrgUnit = dataValues.groupBy { it.orgUnit }

                val routes = mutableListOf<Route>()

                routesByOrgUnit.forEach { (orgUnit, values) ->
                    // Group values by some identifier (since we may have multiple routes per orgUnit)
                    // For simplicity, we'll assume each route is stored with unique timestamps
                    val routeDetailsValues = values.filter { it.dataElement == ROUTE_DETAILS_DE }

                    routeDetailsValues.forEach { detailValue ->
                        try {
                            // Parse route details JSON
                            val routeData = gson.fromJson(detailValue.value, RouteData::class.java)

                            val route = Route(
                                id = UUID.randomUUID().toString(),
                                routeId = routeData.routeId,
                                vehicleType = routeData.vehicleType,
                                totalStops = routeData.totalStops,
                                totalDistance = routeData.totalDistance,
                                totalVolume = routeData.totalVolume,
                                totalWeight = routeData.totalWeight,
                                status = RouteStatus.PENDING,
                                syncStatus = SyncStatus.SYNCED,
                                createdAt = System.currentTimeMillis(),
                                deliveries = routeData.stops.map { stop ->
                                    Delivery(
                                        id = UUID.randomUUID().toString(),
                                        routeId = routeData.routeId,
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

                            routes.add(route)

                            // Save to local database
                            routeDao.insertRoute(route.toEntity())
                            val deliveryEntities = route.deliveries.map { it.toEntity() }
                            deliveryDao.insertDeliveries(deliveryEntities)

                        } catch (e: Exception) {
                            Timber.e(e, "Failed to parse route from data value")
                        }
                    }
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

    private data class RouteData(
        val routeId: String,
        val vehicleType: String,
        val totalStops: Int,
        val totalDistance: Float,
        val totalVolume: Float,
        val totalWeight: Float,
        val totalCost: Float,
        val stops: List<StopData>
    )

    private data class StopData(
        val facilityId: String,
        val facilityName: String,
        val latitude: Double,
        val longitude: Double,
        val orderVolume: Float,
        val orderWeight: Float,
        val stopNumber: Int,
        val distanceFromPrevious: Float,
        val arrivalTime: String? = null
    )

    /**
     * Load mock data for testing
     */
    suspend fun loadMockData() {
        try {
            // Check if we already have data
            val existingRoutes = routeDao.getAllRoutes().map { it.size }.first()
            if (existingRoutes > 0) {
                Timber.d("Mock data already loaded")
                return
            }

            Timber.d("Loading mock data...")

            // Mock Route 1: Nairobi City Route
            val route1Id = "ROUTE-001"
            val route1 = Route(
                id = route1Id,
                routeId = route1Id,
                vehicleType = "TRUCK",
                totalStops = 4,
                totalDistance = 15000f, // 15km
                totalVolume = 800f,
                totalWeight = 3200f,
                status = RouteStatus.PENDING,
                syncStatus = SyncStatus.SYNCED,
                createdAt = System.currentTimeMillis(),
                deliveries = listOf(
                    Delivery(
                        id = UUID.randomUUID().toString(),
                        routeId = route1Id,
                        facilityId = "facility-001",
                        facilityName = "Kenyatta National Hospital",
                        latitude = -1.3018,
                        longitude = 36.8073,
                        orderVolume = 200f,
                        orderWeight = 800f,
                        stopNumber = 1,
                        distanceFromPrevious = 0f,
                        status = DeliveryStatus.PENDING,
                        syncStatus = SyncStatus.SYNCED
                    ),
                    Delivery(
                        id = UUID.randomUUID().toString(),
                        routeId = route1Id,
                        facilityId = "facility-002",
                        facilityName = "Nairobi South Hospital",
                        latitude = -1.3142,
                        longitude = 36.8472,
                        orderVolume = 150f,
                        orderWeight = 600f,
                        stopNumber = 2,
                        distanceFromPrevious = 4200f,
                        status = DeliveryStatus.PENDING,
                        syncStatus = SyncStatus.SYNCED
                    ),
                    Delivery(
                        id = UUID.randomUUID().toString(),
                        routeId = route1Id,
                        facilityId = "facility-003",
                        facilityName = "Mbagathi District Hospital",
                        latitude = -1.3281,
                        longitude = 36.7981,
                        orderVolume = 250f,
                        orderWeight = 1000f,
                        stopNumber = 3,
                        distanceFromPrevious = 5500f,
                        status = DeliveryStatus.PENDING,
                        syncStatus = SyncStatus.SYNCED
                    ),
                    Delivery(
                        id = UUID.randomUUID().toString(),
                        routeId = route1Id,
                        facilityId = "facility-004",
                        facilityName = "Karen Hospital",
                        latitude = -1.3231,
                        longitude = 36.7020,
                        orderVolume = 200f,
                        orderWeight = 800f,
                        stopNumber = 4,
                        distanceFromPrevious = 5300f,
                        status = DeliveryStatus.PENDING,
                        syncStatus = SyncStatus.SYNCED
                    )
                )
            )

            // Mock Route 2: Westlands Route
            val route2Id = "ROUTE-002"
            val route2 = Route(
                id = route2Id,
                routeId = route2Id,
                vehicleType = "VAN",
                totalStops = 3,
                totalDistance = 8500f, // 8.5km
                totalVolume = 450f,
                totalWeight = 1800f,
                status = RouteStatus.PENDING,
                syncStatus = SyncStatus.SYNCED,
                createdAt = System.currentTimeMillis(),
                deliveries = listOf(
                    Delivery(
                        id = UUID.randomUUID().toString(),
                        routeId = route2Id,
                        facilityId = "facility-005",
                        facilityName = "Aga Khan Hospital",
                        latitude = -1.2673,
                        longitude = 36.8078,
                        orderVolume = 150f,
                        orderWeight = 600f,
                        stopNumber = 1,
                        distanceFromPrevious = 0f,
                        status = DeliveryStatus.PENDING,
                        syncStatus = SyncStatus.SYNCED
                    ),
                    Delivery(
                        id = UUID.randomUUID().toString(),
                        routeId = route2Id,
                        facilityId = "facility-006",
                        facilityName = "Westlands Health Centre",
                        latitude = -1.2676,
                        longitude = 36.8062,
                        orderVolume = 100f,
                        orderWeight = 400f,
                        stopNumber = 2,
                        distanceFromPrevious = 3000f,
                        status = DeliveryStatus.PENDING,
                        syncStatus = SyncStatus.SYNCED
                    ),
                    Delivery(
                        id = UUID.randomUUID().toString(),
                        routeId = route2Id,
                        facilityId = "facility-007",
                        facilityName = "Parklands Health Clinic",
                        latitude = -1.2631,
                        longitude = 36.8241,
                        orderVolume = 200f,
                        orderWeight = 800f,
                        stopNumber = 3,
                        distanceFromPrevious = 5500f,
                        status = DeliveryStatus.PENDING,
                        syncStatus = SyncStatus.SYNCED
                    )
                )
            )

            // Save routes to database
            routeDao.insertRoute(route1.toEntity())
            deliveryDao.insertDeliveries(route1.deliveries.map { it.toEntity() })

            routeDao.insertRoute(route2.toEntity())
            deliveryDao.insertDeliveries(route2.deliveries.map { it.toEntity() })

            Timber.d("Mock data loaded: 2 routes with ${route1.deliveries.size + route2.deliveries.size} deliveries")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load mock data")
        }
    }
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
