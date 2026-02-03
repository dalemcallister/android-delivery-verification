package com.connexi.deliveryverification.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connexi.deliveryverification.BuildConfig
import com.connexi.deliveryverification.data.repository.AuthRepository
import com.connexi.deliveryverification.worker.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class LoginUiState(
    val serverUrl: String = BuildConfig.DEFAULT_DHIS2_URL,
    val username: String = "",
    val password: String = "",
    val rememberCredentials: Boolean = false,
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null
) {
    val canLogin: Boolean
        get() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        checkLoginStatus()
        loadSavedCredentials()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            val isLoggedIn = authRepository.isLoggedIn.first()
            _uiState.value = _uiState.value.copy(isLoggedIn = isLoggedIn)
        }
    }

    private fun loadSavedCredentials() {
        viewModelScope.launch {
            val credentials = authRepository.getSavedCredentials()
            if (credentials != null) {
                _uiState.value = _uiState.value.copy(
                    serverUrl = credentials.first,
                    username = credentials.second,
                    password = credentials.third,
                    rememberCredentials = true
                )
            }
        }
    }

    fun onServerUrlChange(serverUrl: String) {
        _uiState.value = _uiState.value.copy(serverUrl = serverUrl, error = null)
    }

    fun onUsernameChange(username: String) {
        _uiState.value = _uiState.value.copy(username = username, error = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun onRememberCredentialsChange(remember: Boolean) {
        _uiState.value = _uiState.value.copy(rememberCredentials = remember)
    }

    fun login() {
        val state = _uiState.value
        if (!state.canLogin) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = authRepository.login(
                serverUrl = state.serverUrl,
                username = state.username,
                password = state.password,
                rememberCredentials = state.rememberCredentials
            )

            if (result.isSuccess) {
                Timber.d("Login successful")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true
                )
            } else {
                Timber.e("Login failed: ${result.exceptionOrNull()?.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Login failed"
                )
            }
        }
    }
}
