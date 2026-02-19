package com.example.multilink.data.local

import android.content.Context
import android.content.SharedPreferences

object LocalData {
    private const val PREF_NAME = "MultiLinkPrefs"
    private const val KEY_THEME = "app_theme"

    // Configuration Constants
    const val MAX_PEOPLE_LIMIT = "50"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }


    fun saveTheme(context: Context, isDark: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_THEME, isDark).apply()
    }

    fun isDarkTheme(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_THEME, false) // Default Light
    }
}