package com.connexi.deliveryverification.ui.route_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connexi.deliveryverification.domain.model.Route
import com.connexi.deliveryverification.domain.usecase.FetchRoutesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RouteDetailUiState(
    val route: Route? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class RouteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fetchRoutesUseCase: FetchRoutesUseCase
) : ViewModel() {

    private val routeId: String = savedStateHandle.get<String>("routeId") ?: ""

    private val _uiState = MutableStateFlow(RouteDetailUiState())
    val uiState: StateFlow<RouteDetailUiState> = _uiState.asStateFlow()

    init {
        loadRoute()
    }

    private fun loadRoute() {
        viewModelScope.launch {
            timber.log.Timber.d("Loading route with ID: $routeId")
            fetchRoutesUseCase.getRouteById(routeId)
                .catch { e ->
                    timber.log.Timber.e(e, "Error loading route")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                .collect { route ->
                    timber.log.Timber.d("Loaded route: ${route?.routeId}, deliveries count: ${route?.deliveries?.size}")
                    route?.deliveries?.forEachIndexed { index, delivery ->
                        timber.log.Timber.d("  Delivery $index: ${delivery.facilityName}, routeId=${delivery.routeId}, id=${delivery.id}")
                    }
                    _uiState.value = _uiState.value.copy(
                        route = route,
                        isLoading = false
                    )
                }
        }
    }
}
