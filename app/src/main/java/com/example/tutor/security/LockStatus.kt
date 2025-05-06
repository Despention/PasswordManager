package com.example.tutor.security

enum class LockStatus {
    UNINITIALIZED, // Начальное состояние до проверки
    LOCKED,        // Приложение требует аутентификации
    UNLOCKED,      // Приложение разблокировано и готово к использованию
    DISABLED       // Функция блокировки отключена в настройках
} 