# Debug Database Issue

## Check if mock data loaded properly

Add this to RouteDetailViewModel to debug:

```kotlin
private fun loadRoute() {
    viewModelScope.launch {
        fetchRoutesUseCase.getRouteById(routeId)
            .catch { e ->
                Timber.e(e, "Error loading route")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
            .collect { route ->
                Timber.d("Loaded route: ${route?.routeId}, deliveries: ${route?.deliveries?.size}")
                route?.deliveries?.forEach { delivery ->
                    Timber.d("  - Delivery: ${delivery.facilityName}, routeId=${delivery.routeId}")
                }
                _uiState.value = _uiState.value.copy(
                    route = route,
                    isLoading = false
                )
            }
    }
}
```

Check logcat for these messages.
