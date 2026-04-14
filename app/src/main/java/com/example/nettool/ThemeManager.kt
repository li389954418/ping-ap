package com.example.nettool

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

object ThemeManager {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val USER_NAME = stringPreferencesKey("user_name")

    fun getThemeFlow(context: Context): Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: "auto"
    }

    suspend fun setTheme(context: Context, mode: String) {
        context.dataStore.edit { prefs -> prefs[THEME_MODE] = mode }
    }

    fun getUserNameFlow(context: Context): Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_NAME] ?: ""
    }

    suspend fun setUserName(context: Context, name: String) {
        context.dataStore.edit { prefs -> prefs[USER_NAME] = name }
    }
}
