package com.example.tutor

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.example.tutor.security.AppLockManager
import com.example.tutor.ui.fragments.SettingsFragment

class TutorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i("TutorApplication", "onCreate вызван")

        // Применяем начальную тему
        applySavedTheme()

        // --- Инициализация AppLockManager ---
        AppLockManager.init(this)
        // --- Конец инициализации ---
    }

    private fun applySavedTheme() {
        try {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val themeValue = sharedPreferences.getString(
                getString(R.string.settings_theme_key),
                getString(R.string.settings_theme_default_value)
            ) ?: getString(R.string.settings_theme_default_value)

            Log.i("TutorApplication", "Применение начальной темы из настроек: $themeValue")
            SettingsFragment.applyTheme(themeValue)
        } catch (e: Exception) {
            Log.e("TutorApplication", "Ошибка при применении сохраненной темы", e)
        }
    }
}