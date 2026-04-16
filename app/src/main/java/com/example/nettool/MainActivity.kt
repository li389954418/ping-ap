package com.example.nettool

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        setContent {
            val themeMode by ThemeManager.getThemeFlow(this).collectAsState(initial = "auto")
            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            DisposableEffect(isDark) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !isDark
                onDispose { }
            }

            val colorScheme = if (isDark) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
            MaterialTheme(colorScheme = colorScheme) { AppContent() }
        }
    }
}

@Composable
fun AppContent() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                NavigationBarItem(selected = currentRoute == Screen.Home.route, onClick = { navController.navigate(Screen.Home.route) }, icon = { Icon(Icons.Outlined.Home, null) }, label = { Text("首页") })
                NavigationBarItem(selected = currentRoute == Screen.SavedList.route, onClick = { navController.navigate(Screen.SavedList.route) }, icon = { Icon(Icons.Outlined.List, null) }, label = { Text("存储") })
                NavigationBarItem(selected = currentRoute == Screen.Settings.route, onClick = { navController.navigate(Screen.Settings.route) }, icon = { Icon(Icons.Outlined.Settings, null) }, label = { Text("设置") })
            }
        },
        modifier = Modifier.systemBarsPadding()
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Home.route, modifier = Modifier.padding(innerPadding)) {
            composable(Screen.Home.route) { HomeScreen(viewModel, { navController.navigate(Screen.SmartParse.route) }, { navController.navigate(Screen.SavedList.route) }) }
            composable(Screen.SavedList.route) { SavedListScreen(viewModel, { navController.navigate(Screen.Home.route) }) }
            composable(Screen.Settings.route) { SettingsScreen(navController) }
            composable(Screen.SmartParse.route) { SmartParseScreen(viewModel, { navController.popBackStack() }) }
            composable(Screen.TemplateManagement.route) { TemplateManagementScreen(viewModel, { navController.popBackStack() }) }
            composable(Screen.CategoryManagement.route) { CategoryManagementScreen(viewModel, { navController.popBackStack() }) }
            composable(Screen.RecycleBin.route) { RecycleBinScreen(viewModel, { navController.popBackStack() }) }
            composable(Screen.Backup.route) { BackupScreen(viewModel, { navController.popBackStack() }) }
        }
    }
}
