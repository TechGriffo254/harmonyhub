package com.techgriffo254.harmonyhub.ui.presentation.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techgriffo254.harmonyhub.data.remote.AuthResult
import com.techgriffo254.harmonyhub.data.remote.JamendoAuthManager
import com.techgriffo254.harmonyhub.domain.repository.TrackRepository
import com.techgriffo254.harmonyhub.domain.model.Track
import com.techgriffo254.harmonyhub.utils.UserPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authManager: JamendoAuthManager,
    private val userPreferences: UserPreferences,
    private val trackRepository: TrackRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    // Define a MutableSharedFlow for navigation events
    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    // Expose the MutableSharedFlow as a SharedFlow
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents

    init {
        viewModelScope.launch {
            checkAuthState()
        }
    }

    fun checkAndRefreshToken() {
        viewModelScope.launch {
            val accessToken = userPreferences.accessToken.first()
            val refreshToken = userPreferences.refreshToken.first()

            if (accessToken == null || refreshToken == null) {
                authManager.startAuthProcess()
            } else {
                val tokenExpiry = userPreferences.tokenExpiry.first()
                if (tokenExpiry != null && System.currentTimeMillis() > tokenExpiry) {
                    when (val result = authManager.refreshAccessToken()) {
                        is AuthResult.Success -> {
                            // Token refreshed successfully, navigate to home screen
                            _navigationEvents.emit(NavigationEvent.NavigateToHome)
                        }

                        is AuthResult.Error -> {
                            // Token refresh failed, navigate to login screen
                            _navigationEvents.emit(NavigationEvent.NavigateToLogin)
                        }
                    }
                } else {
                    trackRepository.refreshTracks(accessToken, authManager.getClientId())
                }
            }
        }
    }

    private suspend fun checkAuthState() {
        val accessToken = userPreferences.accessToken.first()
        val tokenExpiry = userPreferences.tokenExpiry.first()

        if (accessToken != null && tokenExpiry != null && System.currentTimeMillis() < tokenExpiry) {
            _state.value = AuthState(isAuthenticated = true)
        } else if (accessToken != null) {
            // Token expired, try to refresh
            when (val result = authManager.refreshAccessToken()) {
                is AuthResult.Success -> _state.value = AuthState(isAuthenticated = true)
                is AuthResult.Error -> _state.value = AuthState(isAuthenticated = false, error = result.message)
            }
        } else {
            _state.value = AuthState(isAuthenticated = false)
        }
    }

    fun handleAuthResult(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> {
                viewModelScope.launch {
                    _state.value = AuthState(isAuthenticated = true)
                }
            }
            is AuthResult.Error -> {
                _state.value = AuthState(isAuthenticated = false, error = result.message)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            userPreferences.clearAuthTokens()
            _state.value = AuthState(isAuthenticated = false)
        }
    }
}

data class AuthState(
    val isAuthenticated: Boolean = false,
    val error: String? = null
)

// Define a sealed class for navigation events
sealed class NavigationEvent {
    data object NavigateToHome : NavigationEvent()
    data object NavigateToLogin : NavigationEvent()
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null,
    val tracks: List<Track> = emptyList(),
    val accessToken: String? = null,
)
