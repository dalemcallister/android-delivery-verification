package com.connexi.deliveryverification.ui.delivery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connexi.deliveryverification.data.repository.DeliveryRepository
import com.connexi.deliveryverification.domain.model.Delivery
import com.connexi.deliveryverification.domain.model.Location
import com.connexi.deliveryverification.domain.model.LocationValidationResult
import com.connexi.deliveryverification.domain.usecase.CreateVerificationUseCase
import com.connexi.deliveryverification.domain.usecase.GetCurrentLocationUseCase
import com.connexi.deliveryverification.domain.usecase.ValidateLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class DeliveryVerificationUiState(
    val delivery: Delivery? = null,
    val currentLocation: Location? = null,
    val locationValidation: LocationValidationResult? = null,
    val actualVolume: String = "",
    val actualWeight: String = "",
    val comments: String = "",
    val signatureBase64: String? = null,
    val photoUri: String? = null,
    val isSubmitting: Boolean = false,
    val verificationComplete: Boolean = false,
    val error: String? = null
) {
    val canComplete: Boolean
        get() = delivery != null &&
                currentLocation != null &&
                actualVolume.isNotBlank() &&
                actualWeight.isNotBlank()
}

@HiltViewModel
class DeliveryVerificationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deliveryRepository: DeliveryRepository,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
    private val validateLocationUseCase: ValidateLocationUseCase,
    private val createVerificationUseCase: CreateVerificationUseCase
) : ViewModel() {

    private val deliveryId: String = savedStateHandle.get<String>("deliveryId") ?: ""

    private val _uiState = MutableStateFlow(DeliveryVerificationUiState())
    val uiState: StateFlow<DeliveryVerificationUiState> = _uiState.asStateFlow()

    init {
        loadDelivery()
        startLocationUpdates()
    }

    private fun loadDelivery() {
        viewModelScope.launch {
            deliveryRepository.getDeliveryById(deliveryId)
                .collect { delivery ->
                    _uiState.value = _uiState.value.copy(
                        delivery = delivery,
                        actualVolume = delivery?.orderVolume?.toString() ?: "",
                        actualWeight = delivery?.orderWeight?.toString() ?: ""
                    )
                }
        }
    }

    private fun startLocationUpdates() {
        viewModelScope.launch {
            getCurrentLocationUseCase()
                .collect { location ->
                    _uiState.value = _uiState.value.copy(currentLocation = location)
                    validateLocation(location)
                }
        }
    }

    private fun validateLocation(location: Location?) {
        val delivery = _uiState.value.delivery ?: return
        val validation = validateLocationUseCase(
            currentLocation = location,
            targetLatitude = delivery.latitude,
            targetLongitude = delivery.longitude
        )
        _uiState.value = _uiState.value.copy(locationValidation = validation)
    }

    fun onActualVolumeChange(value: String) {
        _uiState.value = _uiState.value.copy(actualVolume = value)
    }

    fun onActualWeightChange(value: String) {
        _uiState.value = _uiState.value.copy(actualWeight = value)
    }

    fun onCommentsChange(value: String) {
        _uiState.value = _uiState.value.copy(comments = value)
    }

    fun completeDelivery() {
        val state = _uiState.value
        if (!state.canComplete) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)

            val actualVolume = state.actualVolume.toFloatOrNull()
            val actualWeight = state.actualWeight.toFloatOrNull()

            if (actualVolume == null || actualWeight == null) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = "Invalid volume or weight"
                )
                return@launch
            }

            val result = createVerificationUseCase(
                deliveryId = deliveryId,
                currentLocation = state.currentLocation!!,
                distanceFromTarget = state.locationValidation?.distanceFromTarget ?: 0f,
                actualVolume = actualVolume,
                actualWeight = actualWeight,
                comments = state.comments.ifBlank { null },
                signatureBase64 = state.signatureBase64,
                photoBase64 = null,
                photoLocalUri = state.photoUri
            )

            if (result.isSuccess) {
                Timber.d("Verification completed successfully")
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    verificationComplete = true
                )
            } else {
                Timber.e("Verification failed: ${result.exceptionOrNull()?.message}")
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = result.exceptionOrNull()?.message ?: "Verification failed"
                )
            }
        }
    }
}
