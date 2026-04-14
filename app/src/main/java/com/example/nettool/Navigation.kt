package com.example.nettool

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object SavedList : Screen("saved_list")
    data object Settings : Screen("settings")
    data object SmartParse : Screen("smart_parse")
    data object TemplateManagement : Screen("template_management")
    data object CategoryManagement : Screen("category_management")
    data object RecycleBin : Screen("recycle_bin")
    data object Backup : Screen("backup")
}
