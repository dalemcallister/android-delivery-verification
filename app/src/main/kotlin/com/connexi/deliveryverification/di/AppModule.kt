package com.connexi.deliveryverification.di

import android.content.Context
import androidx.room.Room
import com.connexi.deliveryverification.data.local.AppDatabase
import com.connexi.deliveryverification.data.local.dao.DeliveryDao
import com.connexi.deliveryverification.data.local.dao.RouteDao
import com.connexi.deliveryverification.data.local.dao.VerificationDao
import com.connexi.deliveryverification.data.repository.AuthRepository
import com.connexi.deliveryverification.data.repository.DeliveryRepository
import com.connexi.deliveryverification.data.repository.RouteRepository
import com.connexi.deliveryverification.data.repository.VerificationRepository
import com.connexi.deliveryverification.service.LocationService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideRouteDao(database: AppDatabase): RouteDao {
        return database.routeDao()
    }

    @Provides
    @Singleton
    fun provideDeliveryDao(database: AppDatabase): DeliveryDao {
        return database.deliveryDao()
    }

    @Provides
    @Singleton
    fun provideVerificationDao(database: AppDatabase): VerificationDao {
        return database.verificationDao()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext context: Context
    ): AuthRepository {
        return AuthRepository(context)
    }

    @Provides
    @Singleton
    fun provideRouteRepository(
        database: AppDatabase,
        authRepository: AuthRepository,
        gson: Gson
    ): RouteRepository {
        return RouteRepository(database, authRepository, gson)
    }

    @Provides
    @Singleton
    fun provideDeliveryRepository(
        database: AppDatabase
    ): DeliveryRepository {
        return DeliveryRepository(database)
    }

    @Provides
    @Singleton
    fun provideVerificationRepository(
        database: AppDatabase,
        authRepository: AuthRepository
    ): VerificationRepository {
        return VerificationRepository(database, authRepository)
    }

    @Provides
    @Singleton
    fun provideLocationService(
        @ApplicationContext context: Context
    ): LocationService {
        return LocationService(context)
    }
}
