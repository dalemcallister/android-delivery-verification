package com.connexi.deliveryverification.data.local

import androidx.room.TypeConverter
import com.connexi.deliveryverification.data.local.entities.*

class Converters {

    @TypeConverter
    fun fromRouteStatus(value: RouteStatus): String {
        return value.name
    }

    @TypeConverter
    fun toRouteStatus(value: String): RouteStatus {
        return RouteStatus.valueOf(value)
    }

    @TypeConverter
    fun fromDeliveryStatus(value: DeliveryStatus): String {
        return value.name
    }

    @TypeConverter
    fun toDeliveryStatus(value: String): DeliveryStatus {
        return DeliveryStatus.valueOf(value)
    }

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String {
        return value.name
    }

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return SyncStatus.valueOf(value)
    }
}
