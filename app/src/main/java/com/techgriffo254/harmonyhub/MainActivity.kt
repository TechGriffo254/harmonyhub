package com.techgriffo254.harmonyhub

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.techgriffo254.harmonyhub.data.local.AppDatabase
import com.techgriffo254.harmonyhub.data.remote.AuthResult
import com.techgriffo254.harmonyhub.data.remote.JamendoAuthManager
import com.techgriffo254.harmonyhub.data.remote.RetrofitInstance
import com.techgriffo254.harmonyhub.domain.repository.TrackRepository
import com.techgriffo254.harmonyhub.navigation.AppNavHost
import com.techgriffo254.harmonyhub.ui.presentation.PermissionsRequester
import com.techgriffo254.harmonyhub.ui.presentation.auth.AuthViewModel
import com.techgriffo254.harmonyhub.ui.presentation.home.HomeViewModel
import com.techgriffo254.harmonyhub.utils.UserPreferences
import kotlinx.coroutines.launch
import org.json.JSONObject

@UnstableApi
class MainActivity : ComponentActivity() {
    private lateinit var appContainer: HarmonyAppContainer
    private lateinit var authManager: JamendoAuthManager
    private lateinit var authViewModel: AuthViewModel
    private lateinit var trackRepository: TrackRepository
    private lateinit var userPreferences: UserPreferences
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userPreferences = UserPreferences(applicationContext)
        authManager = JamendoAuthManager(this, userPreferences)
        trackRepository = TrackRepository(
            AppDatabase.getDatabase(this).trackDao(),
            RetrofitInstance.api
        )
        appContainer = HarmonyAppContainer(
            RetrofitInstance.api,
            AppDatabase.getDatabase(this).trackDao(),
            userPreferences,
            authManager,
            trackRepository,
            applicationContext
        )

        authViewModel = appContainer.authViewModel
        homeViewModel = appContainer.homeViewModel

        handleIntent(intent)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(authManager = authManager, appContainer = appContainer)
                }
            }
        }


        // Create and use PermissionsRequester
        val permissionsRequester = PermissionsRequester(
            context = this,
            onPermissionsGranted = {
                // Fetch local tracks if permissions are granted
                homeViewModel.fetchLocalTracks()
            },
            onPermissionsDenied = {
                // Handle the case when permissions are denied
                Log.e("MainActivity", "Permissions denied")
            }
        )

        // Check and request permissions
        permissionsRequester.checkAndRequestPermissions(this)
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent?.data?.let { uri ->
            if (uri.toString().startsWith(authManager.redirectUri)) {
                lifecycleScope.launch {
                    val result = authManager.handleAuthorizationResponse(uri)
                    appContainer.authViewModel.handleAuthResult(result)
                }
            }
        }
    }

    private fun handleAuthorizationResponse(intent: Intent?) {
        val uri = intent?.data
        if (uri != null && uri.toString().startsWith(authManager.redirectUri)) {
            Log.d("MainActivity", "Received authorization response: $uri")
            lifecycleScope.launch {
                when (val result = authManager.handleAuthorizationResponse(uri)) {
                    is AuthResult.Success -> {
                        trackRepository.refreshTracks(result.accessToken, authManager.getClientId())
                    }

                    is AuthResult.Error -> {
                        try {
                            val errorJson = JSONObject(result.message)
                            errorJson.optJSONObject("headers")?.optString("error_message")
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data
        if (data != null && data.toString().startsWith(authManager.redirectUri)) {
            handleAuthorizationResponse(intent)
        }
    }
}