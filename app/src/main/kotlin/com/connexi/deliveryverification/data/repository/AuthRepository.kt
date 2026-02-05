package com.connexi.deliveryverification.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.connexi.deliveryverification.data.remote.DHIS2Client
import com.connexi.deliveryverification.data.remote.DHIS2Service
import com.connexi.deliveryverification.domain.model.DriverProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
        val REMEMBER_CREDENTIALS = booleanPreferencesKey("remember_credentials")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val DRIVER_USER_ID = stringPreferencesKey("driver_user_id")
        val DRIVER_NAME = stringPreferencesKey("driver_name")
        val DRIVER_TRUCK_ID = stringPreferencesKey("driver_truck_id")
        val DRIVER_TRUCK_NAME = stringPreferencesKey("driver_truck_name")
        val DRIVER_TRUCK_CODE = stringPreferencesKey("driver_truck_code")
    }

    private var dhis2Service: DHIS2Service? = null

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_LOGGED_IN] ?: false
    }

    val serverUrl: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SERVER_URL]
    }

    val username: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USERNAME]
    }

    val rememberCredentials: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.REMEMBER_CREDENTIALS] ?: false
    }

    suspend fun login(
        serverUrl: String,
        username: String,
        password: String,
        rememberCredentials: Boolean
    ): Result<Unit> {
        return try {
            // Create DHIS2 client
            val service = DHIS2Client.create(
                baseUrl = serverUrl,
                username = username,
                password = password,
                debug = true
            )

            // Test connection
            val response = service.getSystemInfo()
            if (response.isSuccessful) {
                dhis2Service = service

                // Fetch driver profile
                val userResponse = service.getCurrentUser()
                if (userResponse.isSuccessful) {
                    val userDto = userResponse.body()
                    val truck = userDto?.getAssignedTruckOrgUnit()

                    // Save credentials and driver profile
                    dataStore.edit { preferences ->
                        preferences[PreferencesKeys.SERVER_URL] = serverUrl
                        preferences[PreferencesKeys.USERNAME] = username
                        if (rememberCredentials) {
                            preferences[PreferencesKeys.PASSWORD] = password
                        } else {
                            preferences.remove(PreferencesKeys.PASSWORD)
                        }
                        preferences[PreferencesKeys.REMEMBER_CREDENTIALS] = rememberCredentials
                        preferences[PreferencesKeys.IS_LOGGED_IN] = true

                        // Save driver profile
                        if (userDto != null) {
                            preferences[PreferencesKeys.DRIVER_USER_ID] = userDto.id
                            preferences[PreferencesKeys.DRIVER_NAME] = userDto.getFullName()
                            if (truck != null) {
                                preferences[PreferencesKeys.DRIVER_TRUCK_ID] = truck.id
                                preferences[PreferencesKeys.DRIVER_TRUCK_NAME] = truck.name
                                truck.code?.let { preferences[PreferencesKeys.DRIVER_TRUCK_CODE] = it }
                                Timber.d("Driver ${userDto.getFullName()} assigned to truck: ${truck.name} (${truck.id})")
                            }
                        }
                    }

                    Timber.d("Login successful for user: $username")
                    Result.success(Unit)
                } else {
                    Timber.e("Failed to fetch user profile: ${userResponse.code()}")
                    Result.failure(Exception("Failed to fetch user profile"))
                }
            } else {
                Timber.e("Login failed: ${response.code()} ${response.message()}")
                Result.failure(Exception("Login failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Login error")
            Result.failure(e)
        }
    }

    suspend fun logout() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_LOGGED_IN] = false
            preferences.remove(PreferencesKeys.PASSWORD)
            preferences.remove(PreferencesKeys.DRIVER_USER_ID)
            preferences.remove(PreferencesKeys.DRIVER_NAME)
            preferences.remove(PreferencesKeys.DRIVER_TRUCK_ID)
            preferences.remove(PreferencesKeys.DRIVER_TRUCK_NAME)
            preferences.remove(PreferencesKeys.DRIVER_TRUCK_CODE)
        }
        dhis2Service = null
        Timber.d("Logged out")
    }

    suspend fun getDriverProfile(): DriverProfile? {
        val prefs = dataStore.data.first()
        val userId = prefs[PreferencesKeys.DRIVER_USER_ID]
        val driverName = prefs[PreferencesKeys.DRIVER_NAME]
        val truckId = prefs[PreferencesKeys.DRIVER_TRUCK_ID]
        val truckName = prefs[PreferencesKeys.DRIVER_TRUCK_NAME]
        val truckCode = prefs[PreferencesKeys.DRIVER_TRUCK_CODE]
        val username = prefs[PreferencesKeys.USERNAME]

        return if (userId != null && driverName != null && truckId != null && truckName != null && username != null) {
            DriverProfile(
                userId = userId,
                username = username,
                driverName = driverName,
                assignedTruckId = truckId,
                assignedTruckName = truckName,
                assignedTruckCode = truckCode
            )
        } else {
            null
        }
    }

    suspend fun getAssignedTruckId(): String? {
        val prefs = dataStore.data.first()
        return prefs[PreferencesKeys.DRIVER_TRUCK_ID]
    }

    suspend fun getDHIS2Service(): DHIS2Service? {
        if (dhis2Service != null) {
            return dhis2Service
        }

        // Try to restore session
        val prefs = dataStore.data.first()
        val serverUrl = prefs[PreferencesKeys.SERVER_URL]
        val username = prefs[PreferencesKeys.USERNAME]
        val password = prefs[PreferencesKeys.PASSWORD]
        val isLoggedIn = prefs[PreferencesKeys.IS_LOGGED_IN] ?: false

        if (isLoggedIn && serverUrl != null && username != null && password != null) {
            dhis2Service = DHIS2Client.create(
                baseUrl = serverUrl,
                username = username,
                password = password,
                debug = true
            )
        }

        return dhis2Service
    }

    suspend fun getSavedCredentials(): Triple<String, String, String>? {
        val prefs = dataStore.data.first()
        val serverUrl = prefs[PreferencesKeys.SERVER_URL]
        val username = prefs[PreferencesKeys.USERNAME]
        val password = prefs[PreferencesKeys.PASSWORD]

        return if (serverUrl != null && username != null && password != null) {
            Triple(serverUrl, username, password)
        } else {
            null
        }
    }
}
