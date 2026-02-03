package com.connexi.deliveryverification.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.connexi.deliveryverification.data.repository.VerificationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val verificationRepository: VerificationRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Starting verification sync")

            val result = verificationRepository.syncPendingVerifications()

            if (result.isSuccess) {
                val syncedCount = result.getOrNull() ?: 0
                Timber.d("Sync completed successfully: $syncedCount verifications synced")
                Result.success(
                    workDataOf(
                        KEY_SYNCED_COUNT to syncedCount
                    )
                )
            } else {
                Timber.e("Sync failed: ${result.exceptionOrNull()?.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "Sync worker error")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "sync_verifications"
        const val KEY_SYNCED_COUNT = "synced_count"

        /**
         * Schedule periodic sync work
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )

            Timber.d("Scheduled periodic sync work")
        }

        /**
         * Trigger immediate one-time sync
         */
        fun syncNow(context: Context): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(syncRequest)

            Timber.d("Triggered immediate sync")
            return syncRequest
        }

        /**
         * Cancel periodic sync
         */
        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Timber.d("Cancelled periodic sync")
        }
    }
}
