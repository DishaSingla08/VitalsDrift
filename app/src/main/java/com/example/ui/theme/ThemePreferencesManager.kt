package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemePreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("vitals_theme_prefs", Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow(
        prefs.getBoolean(KEY_IS_DARK, false) // Default to Light Mode per user specs
    )
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleTheme(): Boolean {
        val newMode = !_isDarkMode.value
        _isDarkMode.value = newMode
        prefs.edit().putBoolean(KEY_IS_DARK, newMode).apply()
        return newMode
    }

    fun setDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
        prefs.edit().putBoolean(KEY_IS_DARK, isDark).apply()
    }

    companion object {
        private const val KEY_IS_DARK = "key_is_dark_mode"

        @Volatile
        private var INSTANCE: ThemePreferencesManager? = null

        fun getInstance(context: Context): ThemePreferencesManager {
            return INSTANCE ?: synchronized(this) {
                val instance = ThemePreferencesManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
