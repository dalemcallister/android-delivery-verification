package com.connexi.deliveryverification.ui.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connexi.deliveryverification.data.repository.AuthRepository
import com.connexi.deliveryverification.data.repository.RouteRepository
import com.connexi.deliveryverification.domain.model.DriverProfile
import com.connexi.deliveryverification.domain.model.Route
import com.connexi.deliveryverification.domain.usecase.FetchRoutesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class RoutesUiState(
    val routes: List<Route> = emptyList(),
    val driverProfile: DriverProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RoutesViewModel @Inject constructor(
    private val fetchRoutesUseCase: FetchRoutesUseCase,
    private val routeRepository: RouteRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutesUiState())
    val uiState: StateFlow<RoutesUiState> = _uiState.asStateFlow()

    init {
        loadDriverProfile()
        loadLocalRoutes()
        // Auto-refresh from DHIS2 on startup
        refreshRoutes()
    }

    private fun loadDriverProfile() {
        viewModelScope.launch {
            val profile = authRepository.getDriverProfile()
            _uiState.value = _uiState.value.copy(driverProfile = profile)
            Timber.d("Driver profile loaded: ${profile?.driverName} - Truck: ${profile?.assignedTruckName}")
        }
    }

    private fun loadLocalRoutes() {
        viewModelScope.launch {
            fetchRoutesUseCase.getLocalRoutes()
                .catch { e ->
                    Timber.e(e, "Error loading routes")
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
                .collect { routes ->
                    _uiState.value = _uiState.value.copy(routes = routes)
                }
        }
    }

    fun refreshRoutes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            Timber.d("Fetching routes from DHIS2 for driver: ${_uiState.value.driverProfile?.driverName}")

            val result = fetchRoutesUseCase.fetchFromRemote()

            if (result.isSuccess) {
                val fetchedRoutes = result.getOrNull() ?: emptyList()
                Timber.d("Routes refreshed successfully: ${fetchedRoutes.size} routes")
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                Timber.e("Failed to refresh routes: ${result.exceptionOrNull()?.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to fetch routes"
                )
            }
        }
    }
}
