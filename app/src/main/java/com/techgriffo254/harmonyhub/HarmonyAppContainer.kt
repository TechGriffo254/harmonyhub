package com.techgriffo254.harmonyhub

import android.content.Context
import androidx.media3.common.util.UnstableApi
import com.techgriffo254.harmonyhub.data.JamendoApiService
import com.techgriffo254.harmonyhub.data.local.TrackDao
import com.techgriffo254.harmonyhub.data.remote.JamendoAuthManager
import com.techgriffo254.harmonyhub.domain.repository.TrackRepository
import com.techgriffo254.harmonyhub.ui.presentation.auth.AuthViewModel
import com.techgriffo254.harmonyhub.ui.presentation.auth.AuthViewModelFactory
import com.techgriffo254.harmonyhub.ui.presentation.home.HomeViewModel
import com.techgriffo254.harmonyhub.ui.presentation.home.HomeViewModelFactory
import com.techgriffo254.harmonyhub.ui.presentation.player.PlayerViewModel
import com.techgriffo254.harmonyhub.ui.presentation.player.PlayerViewModelFactory
import com.techgriffo254.harmonyhub.utils.UserPreferences

@UnstableApi
class HarmonyAppContainer(
    private val apiService: JamendoApiService,
    private val trackDao: TrackDao,
    userPreferences: UserPreferences,
    authManager: JamendoAuthManager,
    trackRepository: TrackRepository,
    applicationContext: Context
) {
    val authViewModel = AuthViewModelFactory(authManager,userPreferences, trackRepository).create(
        AuthViewModel::class.java)

    val homeViewModel = HomeViewModelFactory(TrackRepository(trackDao,apiService)).create(
        HomeViewModel::class.java)

    val playerViewModel = PlayerViewModelFactory(applicationContext, trackRepository).create(
        PlayerViewModel::class.java)
}
