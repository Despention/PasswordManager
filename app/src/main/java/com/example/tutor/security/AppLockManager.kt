package com.example.tutor.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import com.example.tutor.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object AppLockManager : DefaultLifecycleObserver, SharedPreferences.OnSharedPreferenceChangeListener {

    private const val TAG = "AppLockManager"
    private const val LOCK_TIMEOUT_MS = 5 * 60 * 1000

    // Coroutine scope для фоновых задач
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Контекст приложения - инициализируется лениво
    private lateinit var appContext: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var appLockPreferenceKey: String

    // State Flow для публикации текущего статуса блокировки
    private val _lockStatus = MutableStateFlow(LockStatus.UNINITIALIZED)
    val lockStatus: StateFlow<LockStatus> = _lockStatus.asStateFlow()

    // Внутреннее отслеживание состояния
    private var isAppLockEnabled = false
    private var appWentToBackground = false
    private var lastBackgroundTime: Long = 0
    private val isInitialized = AtomicBoolean(false)

    // --- Инициализация ---
    fun init(context: Context) {
        if (isInitialized.getAndSet(true)) {
            Log.w(TAG, "AppLockManager уже инициализирован.")
            return
        }
        Log.d(TAG, "Инициализация AppLockManager...")
        appContext = context.applicationContext
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        appLockPreferenceKey = appContext.getString(R.string.settings_app_lock_key)

        // Читаем начальное значение настройки
        updateLockEnablement()

        // Регистрируем наблюдатель жизненного цикла для отслеживания перехода в фон/на передний план
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Регистрируем слушатель изменений настроек
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        // Устанавливаем начальное состояние на основе текущих настроек
        _lockStatus.value = if (isAppLockEnabled) LockStatus.UNLOCKED else LockStatus.DISABLED
        Log.d(TAG, "Начальный статус блокировки: ${_lockStatus.value}")
    }

    // --- Callbacks жизненного цикла (через DefaultLifecycleObserver) ---
    override fun onStart(owner: LifecycleOwner) {
        // Приложение перешло на передний план
        Log.d(TAG, "onStart (Приложение на переднем плане)")
        val wasInBackground = appWentToBackground
        appWentToBackground = false // Сбрасываем флаг фона

        if (!isAppLockEnabled) {
            Log.d(TAG, "Блокировка отключена, устанавливаем состояние UNLOCKED.")
            _lockStatus.value = LockStatus.DISABLED
            return
        }

        // Проверяем, был ли переход в фон и истек ли таймаут
        if (wasInBackground && lockTimeoutExpired()) {
            Log.i(TAG, "Приложение возобновлено после таймаута, устанавливаем состояние LOCKED.")
            _lockStatus.value = LockStatus.LOCKED
        } else if (wasInBackground) {
            Log.d(TAG, "Приложение возобновлено в пределах таймаута, остаемся UNLOCKED.")
            if (_lockStatus.value != LockStatus.UNLOCKED) {
                _lockStatus.value = LockStatus.UNLOCKED
            }
        } else {
            Log.d(TAG, "Приложение запущено или возобновлено без недавнего перехода в фон.")
            if (_lockStatus.value != LockStatus.UNLOCKED) {
                _lockStatus.value = LockStatus.UNLOCKED
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        // Приложение перешло в фон
        Log.d(TAG, "onStop (Приложение в фоне)")
        if (isAppLockEnabled) {
            appWentToBackground = true
            lastBackgroundTime = System.currentTimeMillis()
            Log.d(TAG, "Блокировка включена, отмечаем переход в фон в $lastBackgroundTime")
        } else {
            appWentToBackground = false
            Log.d(TAG, "Блокировка отключена, не отслеживаем переход в фон.")
        }
    }

    // --- Слушатель SharedPreferences ---
    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        if (key == appLockPreferenceKey) {
            Log.d(TAG, "Настройка блокировки изменена.")
            updateLockEnablement()
            scope.launch {
                if (isAppLockEnabled) {
                    if (_lockStatus.value == LockStatus.DISABLED) {
                        _lockStatus.value = LockStatus.UNLOCKED
                        Log.i(TAG, "Блокировка включена, устанавливаем статус UNLOCKED.")
                    } else {
                        Log.d(TAG, "Блокировка включена, статус остается ${_lockStatus.value}.")
                    }
                } else {
                    _lockStatus.value = LockStatus.DISABLED
                    appWentToBackground = false
                    Log.i(TAG, "Блокировка отключена, устанавливаем статус DISABLED.")
                }
            }
        }
    }

    // --- Публичные методы ---
    /** Вызывать после успешной биометрической аутентификации. */
    fun unlockApp() {
        Log.i(TAG, "unlockApp() вызван.")
        if (isAppLockEnabled) {
            _lockStatus.value = LockStatus.UNLOCKED
        } else {
            _lockStatus.value = LockStatus.DISABLED
            Log.w(TAG, "unlockApp вызван, но блокировка отключена.")
        }
    }

    /** Вызывать, когда приложение *намеренно* показывает экран блокировки и начинает аутентификацию */
    fun notifyAuthenticationAttemptStarted() {
        if (_lockStatus.value == LockStatus.LOCKED) {
            Log.d(TAG, "notifyAuthenticationAttemptStarted()")
        }
    }

    /** Вызывать, если биометрическая аутентификация не удалась или пользователь отменил */
    fun notifyAuthenticationFailed() {
        if (_lockStatus.value == LockStatus.LOCKED) {
            Log.w(TAG, "notifyAuthenticationFailed()")
        }
    }

    // --- Приватные вспомогательные методы ---
    private fun updateLockEnablement() {
        isAppLockEnabled = sharedPreferences.getBoolean(appLockPreferenceKey, false)
        Log.d(TAG, "Функция блокировки включена: $isAppLockEnabled")
    }

    private fun lockTimeoutExpired(): Boolean {
        if (lastBackgroundTime <= 0) return false
        val timeElapsed = System.currentTimeMillis() - lastBackgroundTime
        val expired = timeElapsed >= LOCK_TIMEOUT_MS
        Log.d(TAG, "Проверка таймаута: Прошло=${timeElapsed}мс, Таймаут=${LOCK_TIMEOUT_MS}мс, Истек=$expired")
        return expired
    }

    // --- Очистка ---
    fun cleanup() {
        Log.d(TAG, "Очистка AppLockManager...")
        if (isInitialized.get()) {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            isInitialized.set(false)
        }
    }
} 