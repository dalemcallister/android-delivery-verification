package com.connexi.deliveryverification.ui.sync

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connexi.deliveryverification.data.repository.VerificationRepository
import com.connexi.deliveryverification.domain.model.Verification
import com.connexi.deliveryverification.worker.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SyncUiState(
    val pendingVerifications: List<Verification> = emptyList(),
    val pendingCount: Int = 0,
    val isSyncing: Boolean = false,
    val lastSyncResult: String? = null
)

@HiltViewModel
class SyncViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val verificationRepository: VerificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        loadPendingVerifications()
    }

    private fun loadPendingVerifications() {
        viewModelScope.launch {
            verificationRepository.getPendingVerifications()
                .collect { verifications ->
                    _uiState.value = _uiState.value.copy(
                        pendingVerifications = verifications,
                        pendingCount = verifications.size
                    )
                }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, lastSyncResult = null)

            val result = verificationRepository.syncPendingVerifications()

            if (result.isSuccess) {
                val syncedCount = result.getOrNull() ?: 0
                val message = "Successfully synced $syncedCount verifications"
                Timber.d(message)
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    lastSyncResult = message
                )
            } else {
                val message = "Sync failed: ${result.exceptionOrNull()?.message}"
                Timber.e(message)
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    lastSyncResult = message
                )
            }
        }
    }
}
