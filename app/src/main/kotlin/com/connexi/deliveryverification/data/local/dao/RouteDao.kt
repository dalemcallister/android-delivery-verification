package com.connexi.deliveryverification.data.local.dao

import androidx.room.*
import com.connexi.deliveryverification.data.local.entities.RouteEntity
import com.connexi.deliveryverification.data.local.entities.RouteStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Query("SELECT * FROM routes ORDER BY createdAt DESC")
    fun getAllRoutes(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE id = :routeId")
    fun getRouteById(routeId: String): Flow<RouteEntity?>

    @Query("SELECT * FROM routes WHERE status = :status ORDER BY createdAt DESC")
    fun getRoutesByStatus(status: RouteStatus): Flow<List<RouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(routes: List<RouteEntity>)

    @Update
    suspend fun updateRoute(route: RouteEntity)

    @Delete
    suspend fun deleteRoute(route: RouteEntity)

    @Query("DELETE FROM routes WHERE id = :routeId")
    suspend fun deleteRouteById(routeId: String)

    @Query("DELETE FROM routes")
    suspend fun deleteAllRoutes()
}
