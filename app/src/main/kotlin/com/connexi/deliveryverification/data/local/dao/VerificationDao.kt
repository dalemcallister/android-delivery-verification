package com.connexi.deliveryverification.data.local.dao

import androidx.room.*
import com.connexi.deliveryverification.data.local.entities.SyncStatus
import com.connexi.deliveryverification.data.local.entities.VerificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VerificationDao {

    @Query("SELECT * FROM verifications WHERE deliveryId = :deliveryId")
    fun getVerificationByDelivery(deliveryId: String): Flow<VerificationEntity?>

    @Query("SELECT * FROM verifications WHERE deliveryId = :deliveryId")
    suspend fun getVerificationByDeliverySync(deliveryId: String): VerificationEntity?

    @Query("SELECT * FROM verifications WHERE syncStatus = :syncStatus ORDER BY verifiedAt ASC")
    fun getVerificationsBySyncStatus(syncStatus: SyncStatus): Flow<List<VerificationEntity>>

    @Query("SELECT * FROM verifications WHERE syncStatus = :syncStatus ORDER BY verifiedAt ASC")
    suspend fun getVerificationsBySyncStatusSync(syncStatus: SyncStatus): List<VerificationEntity>

    @Query("SELECT * FROM verifications ORDER BY verifiedAt DESC")
    fun getAllVerifications(): Flow<List<VerificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerification(verification: VerificationEntity)

    @Update
    suspend fun updateVerification(verification: VerificationEntity)

    @Delete
    suspend fun deleteVerification(verification: VerificationEntity)

    @Query("DELETE FROM verifications WHERE deliveryId = :deliveryId")
    suspend fun deleteVerificationByDelivery(deliveryId: String)

    @Query("DELETE FROM verifications")
    suspend fun deleteAllVerifications()
}
