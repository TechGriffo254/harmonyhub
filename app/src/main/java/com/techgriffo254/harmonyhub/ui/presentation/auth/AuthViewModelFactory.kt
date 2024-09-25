package com.techgriffo254.harmonyhub.ui.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.techgriffo254.harmonyhub.data.remote.JamendoAuthManager
import com.techgriffo254.harmonyhub.domain.repository.TrackRepository
import com.techgriffo254.harmonyhub.utils.UserPreferences

class AuthViewModelFactory(
    private val authManager: JamendoAuthManager,
    private val userPreferences: UserPreferences,
    private val trackRepository: TrackRepository
    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authManager,userPreferences, trackRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}