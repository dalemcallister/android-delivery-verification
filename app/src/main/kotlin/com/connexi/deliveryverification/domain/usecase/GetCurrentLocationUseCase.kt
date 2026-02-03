package com.connexi.deliveryverification.domain.usecase

import com.connexi.deliveryverification.domain.model.Location
import com.connexi.deliveryverification.service.LocationService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentLocationUseCase @Inject constructor(
    private val locationService: LocationService
) {
    operator fun invoke(): Flow<Location?> {
        return locationService.getLocationUpdates()
    }

    suspend fun getCurrentLocation(): Location? {
        return locationService.getCurrentLocation()
    }
}
