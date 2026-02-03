package com.connexi.deliveryverification.domain.usecase

import com.connexi.deliveryverification.data.repository.RouteRepository
import com.connexi.deliveryverification.domain.model.Route
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchRoutesUseCase @Inject constructor(
    private val routeRepository: RouteRepository
) {
    suspend fun fetchFromRemote(): Result<List<Route>> {
        return routeRepository.fetchRoutesFromRemote()
    }

    fun getLocalRoutes(): Flow<List<Route>> {
        return routeRepository.getRoutes()
    }

    fun getRouteById(routeId: String): Flow<Route?> {
        return routeRepository.getRouteById(routeId)
    }
}
