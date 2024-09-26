package com.techgriffo254.harmonyhub.ui.presentation.home

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techgriffo254.harmonyhub.domain.repository.TrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.jar.Manifest


class HomeViewModel(
    private val repository: TrackRepository,
    private val application: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        fetchTracks()
    }

    private fun fetchTracks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.getAllTracks().collect { tracks ->
                    _uiState.value = _uiState.value.copy(remoteTracks = tracks, isLoading = false)
                    Log.e("HomeViewModel", "fetchTracks: $tracks")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun searchTracks(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, searchQuery = query)
            try {
                repository.searchTracks(query).collect { tracks ->
                    _uiState.value = _uiState.value.copy(remoteTracks = tracks, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun fetchLocalTracks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.getLocalAudioFiles(application).collect { tracks ->
                    _uiState.value = _uiState.value.copy(localTracks = tracks, isLoading = false)
                    Log.e("HomeViewModel", "fetchLocaltracks: $tracks")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }
}
