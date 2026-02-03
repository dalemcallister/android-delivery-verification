package com.connexi.deliveryverification.data.repository

import com.connexi.deliveryverification.data.local.AppDatabase
import com.connexi.deliveryverification.data.local.entities.DeliveryStatus
import com.connexi.deliveryverification.domain.model.Delivery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeliveryRepository @Inject constructor(
    private val database: AppDatabase
) {
    private val deliveryDao = database.deliveryDao()
    private val verificationDao = database.verificationDao()

    /**
     * Get deliveries for a route
     */
    fun getDeliveriesByRoute(routeId: String): Flow<List<Delivery>> {
        return deliveryDao.getDeliveriesByRoute(routeId).map { entities ->
            entities.map { entity ->
                val verification = verificationDao.getVerificationByDeliverySync(entity.id)
                Delivery(
                    id = entity.id,
                    routeId = entity.routeId,
                    facilityId = entity.facilityId,
                    facilityName = entity.facilityName,
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    orderVolume = entity.orderVolume,
                    orderWeight = entity.orderWeight,
                    stopNumber = entity.stopNumber,
                    distanceFromPrevious = entity.distanceFromPrevious,
                    status = entity.status,
                    verifiedAt = entity.verifiedAt,
                    syncStatus = entity.syncStatus,
                    verification = verification?.let {
                        com.connexi.deliveryverification.domain.model.Verification(
                            id = it.id,
                            deliveryId = it.deliveryId,
                            gpsLatitude = it.gpsLatitude,
                            gpsLongitude = it.gpsLongitude,
                            gpsAccuracy = it.gpsAccuracy,
                            distanceFromTarget = it.distanceFromTarget,
                            actualVolume = it.actualVolume,
                            actualWeight = it.actualWeight,
                            comments = it.comments,
                            signatureBase64 = it.signatureBase64,
                            photoBase64 = it.photoBase64,
                            photoLocalUri = it.photoLocalUri,
                            verifiedAt = it.verifiedAt,
                            dhis2EventId = it.dhis2EventId,
                            syncStatus = it.syncStatus
                        )
                    }
                )
            }
        }
    }

    /**
     * Get delivery by ID
     */
    fun getDeliveryById(deliveryId: String): Flow<Delivery?> {
        return deliveryDao.getDeliveryById(deliveryId).map { entity ->
            entity?.let {
                val verification = verificationDao.getVerificationByDeliverySync(it.id)
                Delivery(
                    id = it.id,
                    routeId = it.routeId,
                    facilityId = it.facilityId,
                    facilityName = it.facilityName,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    orderVolume = it.orderVolume,
                    orderWeight = it.orderWeight,
                    stopNumber = it.stopNumber,
                    distanceFromPrevious = it.distanceFromPrevious,
                    status = it.status,
                    verifiedAt = it.verifiedAt,
                    syncStatus = it.syncStatus,
                    verification = verification?.let { v ->
                        com.connexi.deliveryverification.domain.model.Verification(
                            id = v.id,
                            deliveryId = v.deliveryId,
                            gpsLatitude = v.gpsLatitude,
                            gpsLongitude = v.gpsLongitude,
                            gpsAccuracy = v.gpsAccuracy,
                            distanceFromTarget = v.distanceFromTarget,
                            actualVolume = v.actualVolume,
                            actualWeight = v.actualWeight,
                            comments = v.comments,
                            signatureBase64 = v.signatureBase64,
                            photoBase64 = v.photoBase64,
                            photoLocalUri = v.photoLocalUri,
                            verifiedAt = v.verifiedAt,
                            dhis2EventId = v.dhis2EventId,
                            syncStatus = v.syncStatus
                        )
                    }
                )
            }
        }
    }

    /**
     * Update delivery status
     */
    suspend fun updateDeliveryStatus(
        deliveryId: String,
        status: DeliveryStatus,
        verifiedAt: Long? = null
    ) {
        val delivery = deliveryDao.getDeliveryByIdSync(deliveryId)
        delivery?.let {
            deliveryDao.updateDelivery(
                it.copy(
                    status = status,
                    verifiedAt = verifiedAt ?: it.verifiedAt
                )
            )
            Timber.d("Updated delivery $deliveryId status to $status")
        }
    }
}
