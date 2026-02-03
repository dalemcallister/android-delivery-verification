package com.connexi.deliveryverification.data.local.dao

import androidx.room.*
import com.connexi.deliveryverification.data.local.entities.DeliveryEntity
import com.connexi.deliveryverification.data.local.entities.DeliveryStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {

    @Query("SELECT * FROM deliveries WHERE routeId = :routeId ORDER BY stopNumber ASC")
    fun getDeliveriesByRoute(routeId: String): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE id = :deliveryId")
    fun getDeliveryById(deliveryId: String): Flow<DeliveryEntity?>

    @Query("SELECT * FROM deliveries WHERE id = :deliveryId")
    suspend fun getDeliveryByIdSync(deliveryId: String): DeliveryEntity?

    @Query("SELECT * FROM deliveries WHERE status = :status ORDER BY stopNumber ASC")
    fun getDeliveriesByStatus(status: DeliveryStatus): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE routeId = :routeId AND status = :status ORDER BY stopNumber ASC")
    fun getDeliveriesByRouteAndStatus(routeId: String, status: DeliveryStatus): Flow<List<DeliveryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(delivery: DeliveryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveries(deliveries: List<DeliveryEntity>)

    @Update
    suspend fun updateDelivery(delivery: DeliveryEntity)

    @Delete
    suspend fun deleteDelivery(delivery: DeliveryEntity)

    @Query("DELETE FROM deliveries WHERE routeId = :routeId")
    suspend fun deleteDeliveriesByRoute(routeId: String)

    @Query("DELETE FROM deliveries")
    suspend fun deleteAllDeliveries()
}
