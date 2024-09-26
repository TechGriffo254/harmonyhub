package com.techgriffo254.harmonyhub.ui.presentation.home

import com.techgriffo254.harmonyhub.domain.model.Track

data class HomeUiState(
    val searchQuery: String = "",
    val isActive: Boolean = false,
    val remoteTracks: List<Track> = emptyList(),
    val localTracks: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)