package com.techgriffo254.harmonyhub.navigation

enum class Screen {
    SignIn,
    SignUp,
    Home,
    Player
}

sealed class HarmonyScreen(val route: String) {
    data object SignIn : HarmonyScreen(Screen.SignIn.name)
    data object SignUp : HarmonyScreen(Screen.SignUp.name)
    data object Home : HarmonyScreen(Screen.Home.name)
    data object Player : HarmonyScreen(Screen.Player.name)
}