package com.example.nettool

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object SavedList : Screen("saved_list")
    data object Settings : Screen("settings")
}