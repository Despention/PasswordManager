package com.example.tutor.ui.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import com.example.tutor.R
import com.example.tutor.db.AppDatabase
import com.example.tutor.db.SecurityLogDao
import com.example.tutor.db.SecurityLogEvent
import com.example.tutor.ui.activities.LoginActivity
import com.example.tutor.util.SecurityEventTypes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.lang.Exception
import java.lang.IllegalArgumentException

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var appLockPreference: SwitchPreferenceCompat

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        auth = Firebase.auth
        executor = ContextCompat.getMainExecutor(requireContext())

        // --- Настройка Биометрии ---
        appLockPreference = findPreference(getString(R.string.settings_app_lock_key))!!
        setupBiometricPreference()
        setupBiometricPrompt()

        // --- Настройка Темы ---
        val themePreference: ListPreference? = findPreference(getString(R.string.settings_theme_key))
        themePreference?.onPreferenceChangeListener = this
        updateListPreferenceSummary(themePreference)

        // --- Настройка Очистки Буфера Обмена ---
        val clipboardPreference: ListPreference? = findPreference(getString(R.string.settings_clipboard_clear_key))
        clipboardPreference?.onPreferenceChangeListener = this
        updateListPreferenceSummary(clipboardPreference)

        // --- Настройка перехода в Корзину ---
        val trashPreference: Preference? = findPreference(getString(R.string.settings_trash_key))
        trashPreference?.setOnPreferenceClickListener {
            Log.d("SettingsFragment", "Trash preference clicked")
            try {
                findNavController().navigate(R.id.action_settings_to_trash)
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Navigation to trash failed", e)
                Toast.makeText(context, "Error navigating to trash", Toast.LENGTH_SHORT).show()
            }
            true
        }

        // --- Настройка перехода в Журнал безопасности ---
        val securityLogPreference: Preference? = findPreference(getString(R.string.settings_security_log_key))
        securityLogPreference?.setOnPreferenceClickListener {
            Log.d("SettingsFragment", "Security Log preference clicked")
            try {
                findNavController().navigate(R.id.action_settings_to_security_log)
            } catch (e: IllegalArgumentException) {
                Log.e("SettingsFragment", "Navigation action/destination for security log not found!", e)
                Toast.makeText(context, "Error opening security log screen", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Navigation to security log failed", e)
                Toast.makeText(context, "Error opening security log screen", Toast.LENGTH_SHORT).show()
            }
            true
        }

        // --- Настройка Выхода ---
        val logoutPreference: Preference? = findPreference(getString(R.string.settings_logout_key))
        logoutPreference?.setOnPreferenceClickListener {
            Log.d("SettingsFragment", "Logout preference clicked")
            showConfirmLogoutDialog()
            true
        }
    }

    // Обработчик изменений для ListPreference
    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        Log.d("SettingsFragment", "Preference changed: ${preference.key}, New value: $newValue")
        var updateSummary = false

        if (preference is ListPreference) {
            val stringValue = newValue as String
            if (preference.value != stringValue) {
                updateSummary = true
            }

            // --- ИСПРАВЛЕНИЕ ЗДЕСЬ ---
            // Вызываем метод из companion object через имя класса
            if (preference.key == getString(R.string.settings_theme_key)) {
                applyTheme(stringValue)
            }
            // --- КОНЕЦ ИСПРАВЛЕНИЯ ---
            else if (preference.key == getString(R.string.settings_clipboard_clear_key)) {
                Log.d("SettingsFragment", "Clipboard timeout set to: $stringValue seconds")
                // TODO: Передать значение механизму очистки
            }
        }

        // Обновляем summary только для ListPreference и если значение изменилось
        if (updateSummary && preference is ListPreference) {
            updateListPreferenceSummaryWithValue(preference, newValue as String)
        }

        return true // Сохраняем новое значение
    }


    // --- НАЧАЛО: Логика для Биометрии (без изменений) ---
    private fun setupBiometricPreference() {
        val biometricManager = BiometricManager.from(requireContext())
        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG // Требуем надежную биометрию (Класс 3)

        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                appLockPreference.isEnabled = true
                appLockPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    handleAppLockToggle(newValue as Boolean)
                    false // Сохранение будет после аутентификации
                }
                appLockPreference.isChecked = preferenceManager.sharedPreferences?.getBoolean(appLockPreference.key, false) ?: false
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                appLockPreference.isEnabled = false
                appLockPreference.summary = "Biometric hardware not found"
                appLockPreference.isChecked = false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                appLockPreference.isEnabled = false
                appLockPreference.summary = "Biometric hardware unavailable"
                appLockPreference.isChecked = false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                appLockPreference.isEnabled = true
                appLockPreference.summary = "No biometrics enrolled. Please set up in system settings."
                appLockPreference.isChecked = false
                appLockPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    if (newValue as Boolean) {
                        promptToEnrollBiometrics()
                    }
                    false // Не меняем состояние
                }
            }
            else -> {
                appLockPreference.isEnabled = false
                appLockPreference.summary = "Biometric check failed"
                appLockPreference.isChecked = false
            }
        }
    }

    private fun setupBiometricPrompt() {
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(
                        "SettingsFragment",
                        "Biometric Authentication error: $errorCode - $errString"
                    )
                    appLockPreference.isChecked = false // Сбросить переключатель
                    Toast.makeText(context, "Authentication error: $errString", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d("SettingsFragment", "Biometric Authentication succeeded!")
                    saveAppLockPreference(true) // Сохраняем true
                    appLockPreference.isChecked = true // Обновляем UI
                    Toast.makeText(context, "App Lock Enabled", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w("SettingsFragment", "Biometric Authentication failed.")
                    appLockPreference.isChecked = false // Сбросить переключатель
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for ${getString(R.string.app_name)}")
            .setSubtitle("Confirm biometric to enable app lock")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
    }

    private fun handleAppLockToggle(enable: Boolean) {
        if (enable) {
            val biometricManager = BiometricManager.from(requireContext())
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                biometricPrompt.authenticate(promptInfo)
            } else if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                promptToEnrollBiometrics()
                appLockPreference.isChecked = false // Сбросить
            } else {
                Toast.makeText(
                    requireContext(),
                    "Biometric authentication not available",
                    Toast.LENGTH_SHORT
                ).show()
                appLockPreference.isChecked = false // Сбросить
            }

        } else {
            Log.d("SettingsFragment", "Disabling app lock.")
            saveAppLockPreference(false) // Сохраняем false
            appLockPreference.isChecked = false // Обновляем UI
            Toast.makeText(context, "App Lock Disabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAppLockPreference(enabled: Boolean) {
        preferenceManager.sharedPreferences?.edit()?.putBoolean(appLockPreference.key, enabled)?.apply()
        Log.d("SettingsFragment", "App lock preference saved: $enabled")
    }

    private fun promptToEnrollBiometrics() {
        AlertDialog.Builder(requireContext())
            .setTitle("Enroll Biometrics")
            .setMessage("To use app lock, you need to set up biometric authentication (e.g., fingerprint, face) in your device's system settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(
                            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BiometricManager.Authenticators.BIOMETRIC_STRONG
                        )
                    }
                } else {
                    Intent(Settings.ACTION_SETTINGS)
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("SettingsFragment", "Could not open biometric settings", e)
                    Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    // --- КОНЕЦ: Логика для Биометрии ---


    // Вспомогательные методы для обновления summary (без изменений)
    private fun updateListPreferenceSummary(preference: ListPreference?) {
        preference?.value?.let { currentValue ->
            updateListPreferenceSummaryWithValue(preference, currentValue)
        } ?: run {
            updateListPreferenceSummaryWithValue(preference, null)
        }
    }
    private fun updateListPreferenceSummaryWithValue(preference: ListPreference?, value: String?) {
        preference?.let { listPref ->
            val targetValue = value ?: listPref.sharedPreferences?.getString(listPref.key, null) ?: getDefaultValueAsString(listPref)
            val index = listPref.findIndexOfValue(targetValue)
            val entry : CharSequence? = if (index >= 0) listPref.entries[index] else targetValue // Показываем значение, если запись не найдена
            if (entry != null) {
                listPref.summary = getFormattedSummary(listPref.key, entry.toString())
            } else {
                listPref.summary = ""
            }
        }
    }
    private fun getDefaultValueAsString(preference: ListPreference): String? {
        return preference.sharedPreferences?.getString(preference.key, null)
    }
    private fun getFormattedSummary(key: String?, entry: String): CharSequence {
        val themeKey = getString(R.string.settings_theme_key)
        val clipboardKey = getString(R.string.settings_clipboard_clear_key)
        val summaryResId = when(key) {
            themeKey -> R.string.settings_theme_summary
            clipboardKey -> R.string.settings_clipboard_clear_summary
            else -> 0
        }
        return if (summaryResId != 0 && entry.isNotEmpty()) {
            try { requireContext().getString(summaryResId, entry) } catch (e: Exception) { entry }
        } else { entry }
    }

    // Диалог подтверждения выхода (без изменений)
    private fun showConfirmLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_logout_title)
            .setMessage(R.string.confirm_logout_message)
            .setPositiveButton(R.string.logout) { _, _ ->
                Log.d("SettingsFragment", "Confirm logout")
                auth.signOut()
                activity?.let { currentActivity ->
                    val intent = Intent(currentActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    currentActivity.finishAffinity()
                } ?: Log.e("SettingsFragment", "Activity is null, cannot start LoginActivity")
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // Companion object с applyTheme (без изменений)
    companion object {
        fun applyTheme(themeValue: String) {
            val mode = when (themeValue) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            if (AppCompatDelegate.getDefaultNightMode() != mode) {
                Log.d("SettingsFragment", "Applying theme mode: $mode for value: $themeValue")
                AppCompatDelegate.setDefaultNightMode(mode)
            } else {
                Log.d("SettingsFragment", "Theme mode $mode already applied for value: $themeValue")
            }
        }
    }
}