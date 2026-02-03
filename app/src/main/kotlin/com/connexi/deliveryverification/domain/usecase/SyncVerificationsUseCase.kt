package com.connexi.deliveryverification.domain.usecase

import com.connexi.deliveryverification.data.repository.VerificationRepository
import javax.inject.Inject

class SyncVerificationsUseCase @Inject constructor(
    private val verificationRepository: VerificationRepository
) {
    suspend operator fun invoke(): Result<Int> {
        return verificationRepository.syncPendingVerifications()
    }
}
