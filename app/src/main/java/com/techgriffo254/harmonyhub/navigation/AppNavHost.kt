package com.techgriffo254.harmonyhub.navigation

import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.techgriffo254.harmonyhub.HarmonyAppContainer
import com.techgriffo254.harmonyhub.data.remote.JamendoAuthManager
import com.techgriffo254.harmonyhub.ui.presentation.auth.AuthScreen
import com.techgriffo254.harmonyhub.ui.presentation.auth.NavigationEvent
import com.techgriffo254.harmonyhub.ui.presentation.home.HomeScreen
import com.techgriffo254.harmonyhub.ui.presentation.player.PlayerScreen

@OptIn(UnstableApi::class)
@Composable
fun AppNavHost(
    authManager: JamendoAuthManager,
    appContainer: HarmonyAppContainer
) {
    val navController = rememberNavController()

    val backStack = navController.currentBackStackEntryAsState()

    val authViewModel = appContainer.authViewModel
    val authState by authViewModel.state.collectAsState()

    // Observe changes in authState.isAuthenticated
    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated) {
            navController.navigate(HarmonyScreen.Home.route) {
                popUpTo(HarmonyScreen.SignIn.route) { inclusive = true }
            }
        }
    }

    // Observe navigation events
    LaunchedEffect(authViewModel) {
        authViewModel.navigationEvents.collect { event ->
            when (event) {
                is NavigationEvent.NavigateToHome -> {
                    navController.navigate(HarmonyScreen.Home.route) {
                        popUpTo(HarmonyScreen.SignIn.route) { inclusive = true }
                    }
                }
                is NavigationEvent.NavigateToLogin -> {
                    navController.navigate(HarmonyScreen.SignIn.route) {
                        popUpTo(HarmonyScreen.Home.route) { inclusive = true }
                    }
                }
            }
        }
    }

    NavHost (
        navController = navController,
        startDestination = if (authState.isAuthenticated) HarmonyScreen.Home.route else HarmonyScreen.SignIn.route
    ){
        composable(HarmonyScreen.SignIn.route){
            AuthScreen(
                onSignInClick = { authManager.startAuthProcess() },
                onSignUpClick = { authManager.startSignUpProcess()}
            )
        }
        composable(HarmonyScreen.Home.route) {
            val viewModel = appContainer.homeViewModel
            val uiState by viewModel.uiState.collectAsState()

            HomeScreen(
                uiState = uiState,
                onSearchQueryChange = { viewModel.searchTracks(it) },
                onClick = { trackId ->
                    navController.navigate("${HarmonyScreen.Player.route}/$trackId")
                },
//                onSignOut = {
//                    authViewModel.signOut()
//                    navController.navigate(HarmonyScreen.SignIn.route) {
//                        popUpTo(HarmonyScreen.Home.route) { inclusive = true }
//                    }
//                }
            )
        }
        composable(
            route = "${HarmonyScreen.Player.route}/{trackId}",
            arguments = listOf(navArgument("trackId"){type = NavType.StringType})
        ) {
            val postId = backStack.value?.arguments?.getString("trackId") ?: -1

            PlayerScreen(
                trackId = postId.toString(),
                viewModel = appContainer.playerViewModel,
                onBackClick = {navController.popBackStack()}
            )
        }
    }
}

@Composable
private fun NavigationEffect(
    isAuthenticated: Boolean,
    navController: NavHostController
) {
    Log.d("NavigationEffect", "isAuthenticated: $isAuthenticated")
    if (isAuthenticated) {
        navController.navigate(HarmonyScreen.Home.route) {
            popUpTo(HarmonyScreen.SignIn.route) { inclusive = true }
        }
    } else {
        navController.navigate(HarmonyScreen.SignIn.route) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }
}