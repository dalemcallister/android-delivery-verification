package com.connexi.deliveryverification.ui.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connexi.deliveryverification.data.repository.RouteRepository
import com.connexi.deliveryverification.domain.model.Route
import com.connexi.deliveryverification.domain.usecase.FetchRoutesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class RoutesUiState(
    val routes: List<Route> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RoutesViewModel @Inject constructor(
    private val fetchRoutesUseCase: FetchRoutesUseCase,
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutesUiState())
    val uiState: StateFlow<RoutesUiState> = _uiState.asStateFlow()

    init {
        loadMockData()
        loadLocalRoutes()
    }

    private fun loadMockData() {
        viewModelScope.launch {
            routeRepository.loadMockData()
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

            // For now, just reload mock data
            // TODO: Implement real DHIS2 route fetching after creating data elements
            Timber.d("Refreshing routes (using mock data)")
            routeRepository.loadMockData()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = null
            )

            // Uncomment below when DHIS2 data elements are set up:
            /*
            val result = fetchRoutesUseCase.fetchFromRemote()

            if (result.isSuccess) {
                Timber.d("Routes refreshed successfully")
                _uiState.value = _uiState.value.copy(isLoading = false)
            } else {
                Timber.e("Failed to refresh routes: ${result.exceptionOrNull()?.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to fetch routes"
                )
            }
            */
        }
    }
}
