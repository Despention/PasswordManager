package com.example.tutor.util
/**
 * Объект, содержащий константы для различных типов событий безопасности,
 * записываемых в журнал. Использование констант улучшает читаемость
 * и позволяет избежать опечаток при логировании.
 */
object SecurityEventTypes {

    // --- События аутентификации пользователя ---
    /** Пользователь успешно вошел в систему. */
    const val LOGIN_SUCCESS = "LOGIN_SUCCESS"
    /** Ошибка при входе пользователя в систему. */
    const val LOGIN_FAILURE = "LOGIN_FAILURE"
    /** Пользователь успешно зарегистрирован. */
    const val REGISTRATION_SUCCESS = "REGISTRATION_SUCCESS"
    /** Ошибка при регистрации пользователя. */
    const val REGISTRATION_FAILURE = "REGISTRATION_FAILURE"
    /** Пользователь вышел из системы. */
    const val LOGOUT = "LOGOUT"

    // --- События управления паролями ---
    /** Новый пароль добавлен. */
    const val PASSWORD_ADDED = "PASSWORD_ADDED"
    /** Пароль перемещен в корзину (мягкое удаление). */
    const val PASSWORD_SOFT_DELETED = "PASSWORD_SOFT_DELETED"
    /** Пароль восстановлен из корзины. */
    const val PASSWORD_RESTORED = "PASSWORD_RESTORED"
    /** Пароль окончательно удален (из корзины). */
    const val PASSWORD_PERMANENTLY_DELETED = "PASSWORD_PERMANENTLY_DELETED"
    /** Корзина была очищена (все удаленные пароли удалены окончательно). */
    const val TRASH_EMPTIED = "TRASH_EMPTIED"

    // --- События блокировки приложения ---
    /** Блокировка приложения по биометрии включена. */
    const val APP_LOCK_ENABLED = "APP_LOCK_ENABLED"
    /** Блокировка приложения по биометрии отключена. */
    const val APP_LOCK_DISABLED = "APP_LOCK_DISABLED"

    // --- События журнала безопасности ---
    /** Журнал безопасности был очищен пользователем. */
    const val SECURITY_LOG_CLEARED = "SECURITY_LOG_CLEARED"

    // --- Другие возможные события (примеры) ---
    // const val PASSWORD_UPDATED = "PASSWORD_UPDATED" // Если решите логировать изменения паролей
    // const val SETTINGS_CHANGED = "SETTINGS_CHANGED" // Общее событие изменения настроек
    // const val BIOMETRIC_AUTH_SUCCESS = "BIOMETRIC_AUTH_SUCCESS" // Успешная биометрия для разблокировки
    // const val BIOMETRIC_AUTH_FAILURE = "BIOMETRIC_AUTH_FAILURE" // Неудачная биометрия для разблокировки
}