package com.example.nettool

import android.os.Bundle
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 允许内容延伸到系统栏下方
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 移除手动设置状态栏颜色，改由主题控制

        setContent {
            val themeMode by ThemeManager.getThemeFlow(this).collectAsState(initial = "auto")
            val isDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            val colorScheme = if (isDark) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)

            MaterialTheme(colorScheme = colorScheme) {
                val navController = rememberNavController()
                val viewModel: MainViewModel = viewModel()

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                            NavigationBarItem(
                                selected = currentRoute == Screen.Home.route,
                                onClick = { navController.navigate(Screen.Home.route) },
                                icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                                label = { Text("首页") }
                            )
                            NavigationBarItem(
                                selected = currentRoute == Screen.SavedList.route,
                                onClick = { navController.navigate(Screen.SavedList.route) },
                                icon = { Icon(Icons.Outlined.List, contentDescription = null) },
                                label = { Text("存储") }
                            )
                            NavigationBarItem(
                                selected = currentRoute == Screen.Settings.route,
                                onClick = { navController.navigate(Screen.Settings.route) },
                                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                                label = { Text("设置") }
                            )
                        }
                    },
                    modifier = Modifier.systemBarsPadding()
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToSmartParse = { navController.navigate(Screen.SmartParse.route) },
                                onNavigateToSavedList = { navController.navigate(Screen.SavedList.route) }
                            )
                        }
                        composable(Screen.SavedList.route) {
                            SavedListScreen(
                                viewModel = viewModel,
                                onNavigateToHome = { navController.navigate(Screen.Home.route) }
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(navController = navController)
                        }
                        composable(Screen.SmartParse.route) {
                            SmartParseScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.TemplateManagement.route) {
                            TemplateManagementScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.CategoryManagement.route) {
                            CategoryManagementScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.RecycleBin.route) {
                            RecycleBinScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Backup.route) {
                            BackupScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
