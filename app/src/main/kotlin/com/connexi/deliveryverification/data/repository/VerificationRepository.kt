package com.connexi.deliveryverification.data.repository

import com.connexi.deliveryverification.data.local.AppDatabase
import com.connexi.deliveryverification.data.local.entities.DeliveryStatus
import com.connexi.deliveryverification.data.local.entities.SyncStatus
import com.connexi.deliveryverification.data.local.entities.VerificationEntity
import com.connexi.deliveryverification.data.remote.dto.DataValueDto
import com.connexi.deliveryverification.data.remote.dto.EventDto
import com.connexi.deliveryverification.domain.model.Verification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerificationRepository @Inject constructor(
    private val database: AppDatabase,
    private val authRepository: AuthRepository
) {
    private val verificationDao = database.verificationDao()
    private val deliveryDao = database.deliveryDao()

    /**
     * Create a new verification
     */
    suspend fun createVerification(
        deliveryId: String,
        gpsLatitude: Double,
        gpsLongitude: Double,
        gpsAccuracy: Float,
        distanceFromTarget: Float,
        actualVolume: Float,
        actualWeight: Float,
        comments: String?,
        signatureBase64: String?,
        photoBase64: String?,
        photoLocalUri: String?
    ): Result<Verification> {
        return try {
            val verificationEntity = VerificationEntity(
                id = UUID.randomUUID().toString(),
                deliveryId = deliveryId,
                gpsLatitude = gpsLatitude,
                gpsLongitude = gpsLongitude,
                gpsAccuracy = gpsAccuracy,
                distanceFromTarget = distanceFromTarget,
                actualVolume = actualVolume,
                actualWeight = actualWeight,
                comments = comments,
                signatureBase64 = signatureBase64,
                photoBase64 = photoBase64,
                photoLocalUri = photoLocalUri,
                verifiedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )

            verificationDao.insertVerification(verificationEntity)

            // Update delivery status
            val delivery = deliveryDao.getDeliveryByIdSync(deliveryId)
            delivery?.let {
                deliveryDao.updateDelivery(
                    it.copy(
                        status = DeliveryStatus.COMPLETED,
                        verifiedAt = verificationEntity.verifiedAt
                    )
                )
            }

            Timber.d("Created verification for delivery $deliveryId")
            Result.success(verificationEntity.toDomain())
        } catch (e: Exception) {
            Timber.e(e, "Failed to create verification")
            Result.failure(e)
        }
    }

    /**
     * Get pending verifications that need to be synced
     */
    fun getPendingVerifications(): Flow<List<Verification>> {
        return verificationDao.getVerificationsBySyncStatus(SyncStatus.PENDING)
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Sync pending verifications to DHIS2
     */
    suspend fun syncPendingVerifications(): Result<Int> {
        return try {
            val service = authRepository.getDHIS2Service()
                ?: return Result.failure(Exception("Not logged in"))

            val pendingVerifications = verificationDao.getVerificationsBySyncStatusSync(SyncStatus.PENDING)

            if (pendingVerifications.isEmpty()) {
                Timber.d("No pending verifications to sync")
                return Result.success(0)
            }

            var syncedCount = 0

            for (verification in pendingVerifications) {
                try {
                    val delivery = deliveryDao.getDeliveryByIdSync(verification.deliveryId)
                        ?: continue

                    // TODO: Replace with actual program and data element IDs
                    val programId = "VERIFICATION_PROGRAM_ID"
                    val programStageId = "VERIFICATION_STAGE_ID"

                    val event = EventDto(
                        program = programId,
                        programStage = programStageId,
                        orgUnit = delivery.facilityId,
                        eventDate = formatDate(verification.verifiedAt),
                        status = "COMPLETED",
                        coordinate = com.connexi.deliveryverification.data.remote.dto.CoordinateDto(
                            latitude = verification.gpsLatitude,
                            longitude = verification.gpsLongitude
                        ),
                        dataValues = buildDataValues(verification, delivery)
                    )

                    val response = service.createEvent(event)

                    if (response.isSuccessful) {
                        val eventId = response.body()?.response?.importSummaries?.firstOrNull()?.reference
                        verificationDao.updateVerification(
                            verification.copy(
                                syncStatus = SyncStatus.SYNCED,
                                dhis2EventId = eventId
                            )
                        )
                        syncedCount++
                        Timber.d("Synced verification ${verification.id} -> event $eventId")
                    } else {
                        Timber.e("Failed to sync verification ${verification.id}: ${response.message()}")
                        verificationDao.updateVerification(
                            verification.copy(syncStatus = SyncStatus.FAILED)
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error syncing verification ${verification.id}")
                    verificationDao.updateVerification(
                        verification.copy(syncStatus = SyncStatus.FAILED)
                    )
                }
            }

            Timber.d("Synced $syncedCount of ${pendingVerifications.size} verifications")
            Result.success(syncedCount)
        } catch (e: Exception) {
            Timber.e(e, "Error syncing verifications")
            Result.failure(e)
        }
    }

    private fun buildDataValues(
        verification: VerificationEntity,
        delivery: com.connexi.deliveryverification.data.local.entities.DeliveryEntity
    ): List<DataValueDto> {
        // TODO: Replace with actual data element IDs from DHIS2
        return listOf(
            DataValueDto("ORDER_VOLUME_DE", delivery.orderVolume.toString()),
            DataValueDto("ORDER_WEIGHT_DE", delivery.orderWeight.toString()),
            DataValueDto("ACTUAL_VOLUME_DE", verification.actualVolume.toString()),
            DataValueDto("ACTUAL_WEIGHT_DE", verification.actualWeight.toString()),
            DataValueDto("GPS_LATITUDE_DE", verification.gpsLatitude.toString()),
            DataValueDto("GPS_LONGITUDE_DE", verification.gpsLongitude.toString()),
            DataValueDto("GPS_ACCURACY_DE", verification.gpsAccuracy.toString()),
            DataValueDto("DISTANCE_FROM_TARGET_DE", verification.distanceFromTarget.toString()),
            DataValueDto("VERIFICATION_TIMESTAMP_DE", verification.verifiedAt.toString())
        ).plus(
            verification.comments?.let {
                listOf(DataValueDto("COMMENTS_DE", it))
            } ?: emptyList()
        ).plus(
            verification.signatureBase64?.let {
                listOf(DataValueDto("SIGNATURE_DE", it))
            } ?: emptyList()
        ).plus(
            verification.photoBase64?.let {
                listOf(DataValueDto("PHOTO_DE", it))
            } ?: emptyList()
        )
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(timestamp))
    }
}

private fun VerificationEntity.toDomain() = Verification(
    id = id,
    deliveryId = deliveryId,
    gpsLatitude = gpsLatitude,
    gpsLongitude = gpsLongitude,
    gpsAccuracy = gpsAccuracy,
    distanceFromTarget = distanceFromTarget,
    actualVolume = actualVolume,
    actualWeight = actualWeight,
    comments = comments,
    signatureBase64 = signatureBase64,
    photoBase64 = photoBase64,
    photoLocalUri = photoLocalUri,
    verifiedAt = verifiedAt,
    dhis2EventId = dhis2EventId,
    syncStatus = syncStatus
)
