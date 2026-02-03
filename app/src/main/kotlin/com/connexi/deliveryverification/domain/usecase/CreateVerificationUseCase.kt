package com.connexi.deliveryverification.domain.usecase

import com.connexi.deliveryverification.data.repository.VerificationRepository
import com.connexi.deliveryverification.domain.model.Location
import com.connexi.deliveryverification.domain.model.Verification
import javax.inject.Inject

class CreateVerificationUseCase @Inject constructor(
    private val verificationRepository: VerificationRepository
) {
    suspend operator fun invoke(
        deliveryId: String,
        currentLocation: Location,
        distanceFromTarget: Float,
        actualVolume: Float,
        actualWeight: Float,
        comments: String?,
        signatureBase64: String?,
        photoBase64: String?,
        photoLocalUri: String?
    ): Result<Verification> {
        return verificationRepository.createVerification(
            deliveryId = deliveryId,
            gpsLatitude = currentLocation.latitude,
            gpsLongitude = currentLocation.longitude,
            gpsAccuracy = currentLocation.accuracy,
            distanceFromTarget = distanceFromTarget,
            actualVolume = actualVolume,
            actualWeight = actualWeight,
            comments = comments,
            signatureBase64 = signatureBase64,
            photoBase64 = photoBase64,
            photoLocalUri = photoLocalUri
        )
    }
}
