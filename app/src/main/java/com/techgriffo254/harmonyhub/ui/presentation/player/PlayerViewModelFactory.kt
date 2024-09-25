package com.techgriffo254.harmonyhub.ui.presentation.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.techgriffo254.harmonyhub.domain.repository.TrackRepository

class PlayerViewModelFactory(private val applicationContext: Context, private val trackRepository: TrackRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlayerViewModel(applicationContext,trackRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}