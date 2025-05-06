package com.example.tutor.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.tutor.R
import com.example.tutor.databinding.ActivityMainBinding
import com.example.tutor.security.AppLockManager
import com.example.tutor.security.LockStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.lang.Exception // Уточнение

class MainActivity : AppCompatActivity() {

    // ViewBinding для доступа к макету
    private lateinit var binding: ActivityMainBinding
    // Контроллер навигации
    private lateinit var navController: NavController
    // Firebase Authentication
    private lateinit var auth: FirebaseAuth
    // Конфигурация для AppBar/Toolbar (если используется)
    private lateinit var appBarConfiguration: AppBarConfiguration

    // --- Переменные для BiometricPrompt ---
    private lateinit var executor: Executor // Исполнитель для колбэков биометрии
    private lateinit var biometricPrompt: BiometricPrompt // Сам диалог биометрии
    private lateinit var promptInfo: BiometricPrompt.PromptInfo // Информация для диалога (заголовок, кнопка)
    // --- Конец переменных для BiometricPrompt ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Инфлейт макета через ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("MainActivity", "onCreate запущен")

        // Инициализация Firebase Auth
        auth = Firebase.auth

        // --- Проверка аутентификации Firebase ---
        // Если пользователь не вошел в Firebase, перенаправляем на экран входа
        if (auth.currentUser == null) {
            Log.w("MainActivity", "Пользователь не вошел, перенаправление на LoginActivity")
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // Закрываем MainActivity, чтобы пользователь не мог вернуться назад
            return   // Прекращаем выполнение onCreate
        }
        // Если пользователь вошел, логируем его email
        Log.d("MainActivity", "Пользователь вошел: ${auth.currentUser?.email}")

        // --- Настройка компонентов ---
        setupBiometricPrompt() // Настраиваем BiometricPrompt
        setupNavigation()      // Настраиваем Navigation Component и BottomNavigationView
        observeLockStatus()    // Начинаем наблюдение за статусом блокировки приложения

        Log.d("MainActivity", "onCreate успешно завершен")
    }

    /**
     * Настраивает NavController и связывает его с BottomNavigationView.
     */
    private fun setupNavigation() {
        // Находим NavHostFragment в макете
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        // Получаем NavController из NavHostFragment
        navController = navHostFragment.navController
        Log.d("MainActivity", "NavController получен")

        // Определяем фрагменты верхнего уровня (для которых не будет кнопки "Назад" в Toolbar)
        // ID должны совпадать с ID элементов в bottom_nav_menu.xml и ID фрагментов в nav_graph.xml
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_profile, R.id.navigation_passwords, R.id.navigation_settings
            )
        )

        // --- Связываем BottomNavigationView с NavController ---
        // Эта строка автоматически обрабатывает нажатия на элементы нижнего меню
        // и выполняет навигацию к соответствующим destination в графе навигации.
        binding.bottomNavView.setupWithNavController(navController)
        // ---------------------------------------------------

        Log.d("MainActivity", "BottomNavView настроен с NavController")

        // Опционально: Настройка Toolbar (если он добавлен в activity_main.xml)
        // val toolbar = binding.toolbar // Получаем Toolbar из биндинга
        // setSupportActionBar(toolbar) // Устанавливаем его как ActionBar
        // setupActionBarWithNavController(navController, appBarConfiguration) // Связываем с NavController
        // Log.d("MainActivity", "ActionBar настроен с NavController")
    }

    /**
     * Настраивает BiometricPrompt для запроса аутентификации пользователя.
     */
    private fun setupBiometricPrompt() {
        // Получаем исполнителя для основного потока
        executor = ContextCompat.getMainExecutor(this)

        // Создаем экземпляр BiometricPrompt с колбэками
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                // Вызывается при ошибке аутентификации (не отмена пользователем)
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e("MainActivity", "Ошибка биометрии: $errorCode - $errString")
                    // Сообщаем менеджеру блокировки о неудаче
                    AppLockManager.notifyAuthenticationFailed()
                    Toast.makeText(applicationContext, "Ошибка аутентификации: $errString", Toast.LENGTH_SHORT).show()
                    // ВАЖНО: Решить, что делать при ошибке. Возможно, закрыть приложение.
                    // finishAffinity() // Закрывает все Activity приложения
                }

                // Вызывается при успешной аутентификации
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.i("MainActivity", "Биометрия подтверждена!")
                    // Сообщаем менеджеру блокировки об успехе (он изменит статус на UNLOCKED)
                    AppLockManager.unlockApp()
                    // Toast.makeText(applicationContext, "Приложение разблокировано", Toast.LENGTH_SHORT).show() // Можно убрать, т.к. UI обновится сам
                }

                // Вызывается, когда аутентификация не удалась (например, неверный отпечаток)
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w("MainActivity", "Биометрия не подтверждена.")
                    // Сообщаем менеджеру блокировки о неудаче
                    AppLockManager.notifyAuthenticationFailed()
                    // Диалог обычно остается видимым для повторной попытки
                }
            })

        // Создаем информацию для отображения в диалоге BiometricPrompt
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title, getString(R.string.app_name))) // Заголовок
            .setSubtitle(getString(R.string.biometric_prompt_subtitle_unlock)) // Подзаголовок
            // Указываем разрешенные типы аутентификаторов (только сильные биометрические)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            // Текст для отрицательной кнопки (Отмена или Использовать пароль)
            // Если использовать .setNegativeButtonText(), то при нажатии вызовется onAuthenticationError с кодом ERROR_NEGATIVE_BUTTON
            .setNegativeButtonText(getString(R.string.biometric_prompt_use_password_fallback)) // Или android.R.string.cancel
            // Можно также использовать setConfirmationRequired(false), чтобы не требовать нажатия OK после сканирования лица/отпечатка
            .build()
    }

    /**
     * Запускает наблюдение за статусом блокировки приложения из AppLockManager.
     */
    private fun observeLockStatus() {
        // Запускаем корутину в lifecycleScope активити
        lifecycleScope.launch {
            // Используем repeatOnLifecycle для автоматической отмены и возобновления подписки
            // Подписка будет активна только когда активити в состоянии STARTED или выше
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Собираем (слушаем) изменения из StateFlow AppLockManager.lockStatus
                AppLockManager.lockStatus.collect { status ->
                    Log.i("MainActivity", "Получен новый LockStatus: $status")
                    // Реагируем на изменение статуса
                    when (status) {
                        LockStatus.LOCKED -> showLockScreenAndAuthenticate() // Показать экран блокировки/запросить биометрию
                        LockStatus.UNLOCKED, LockStatus.DISABLED -> hideLockScreen() // Скрыть экран блокировки
                        LockStatus.UNINITIALIZED -> Log.d("MainActivity", "LockStatus еще не инициализирован.") // Начальное состояние, игнорируем
                    }
                }
            }
        }
    }

    /**
     * Показывает экран блокировки (если есть) и инициирует запрос биометрии.
     */
    private fun showLockScreenAndAuthenticate() {
        Log.i("MainActivity", "Требуется аутентификация для разблокировки.")
        // Здесь можно добавить логику показа оверлея или перехода на спец. экран блокировки
        // binding.lockOverlay.visibility = View.VISIBLE // Пример с оверлеем

        // Проверяем, доступна ли сильная биометрия ПЕРЕД показом диалога
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            == BiometricManager.BIOMETRIC_SUCCESS) {
            // Если биометрия доступна, показываем диалог
            Log.d("MainActivity", "Показ запроса биометрии.")
            AppLockManager.notifyAuthenticationAttemptStarted() // Уведомляем менеджер
            // Запускаем диалог аутентификации
            try {
                biometricPrompt.authenticate(promptInfo)
            } catch (e: Exception) {
                Log.e("MainActivity", "Ошибка при вызове biometricPrompt.authenticate()", e)
                // Обработка редкой ошибки, если система не может показать диалог
                Toast.makeText(this, "Не удалось показать запрос биометрии", Toast.LENGTH_SHORT).show()
                // Возможно, стоит закрыть приложение
                // finishAffinity()
            }
        } else {
            // Если биометрия недоступна (например, удалили отпечатки после включения настройки)
            Log.e("MainActivity", "Блокировка включена, но сильная биометрия недоступна!")
            // Показываем ошибку пользователю
            Toast.makeText(this, "Биометрическая аутентификация недоступна.", Toast.LENGTH_LONG).show()
            // Здесь нужно принять решение:
            // 1. Разрешить доступ без биометрии? (Небезопасно)
            // 2. Закрыть приложение? (Безопасно, но неудобно)
            // finishAffinity()
            // 3. Принудительно отключить настройку? (Требует доработки AppLockManager)
            // 4. Предложить перейти в настройки системы?
        }
    }

    /**
     * Скрывает экран блокировки (если он был показан).
     */
    private fun hideLockScreen() {
        // Здесь логика скрытия оверлея или ухода с экрана блокировки
        // binding.lockOverlay.visibility = View.GONE // Пример с оверлеем
        Log.d("MainActivity", "Экран блокировки скрыт (если был).")
    }

    /**
     * Переопределяется для обработки навигации "Вверх" с использованием NavController,
     * если вы используете Toolbar/ActionBar, связанный с NavController.
     */
    // override fun onSupportNavigateUp(): Boolean {
    //     // Пытаемся выполнить навигацию вверх через NavController
    //     // Или вызываем стандартную реализацию, если NavController не смог обработать
    //     return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    // }
}