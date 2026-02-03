package com.connexi.deliveryverification.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.connexi.deliveryverification.data.local.dao.DeliveryDao
import com.connexi.deliveryverification.data.local.dao.RouteDao
import com.connexi.deliveryverification.data.local.dao.VerificationDao
import com.connexi.deliveryverification.data.local.entities.DeliveryEntity
import com.connexi.deliveryverification.data.local.entities.RouteEntity
import com.connexi.deliveryverification.data.local.entities.VerificationEntity

@Database(
    entities = [
        RouteEntity::class,
        DeliveryEntity::class,
        VerificationEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun deliveryDao(): DeliveryDao
    abstract fun verificationDao(): VerificationDao

    companion object {
        const val DATABASE_NAME = "delivery_verification.db"
    }
}
