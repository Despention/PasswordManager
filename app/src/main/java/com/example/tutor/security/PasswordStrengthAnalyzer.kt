package com.example.tutor.security // Или com.example.tutor.util

import java.util.Locale // Для toLowerCase

/**
 * Объект для анализа надежности пароля.
 * Предоставляет статический метод для расчета уровня надежности.
 */
object PasswordStrengthAnalyzer {

    // Набор специальных символов для проверки
    private const val SPECIAL_CHARACTERS = "!@#$%^&*()-_=+[]{};:'\",.<>/?\\|"

    /**
     * Вычисляет уровень надежности пароля на основе набора критериев.
     *
     * @param password Пароль для анализа.
     * @return Уровень надежности [PasswordStrengthLevel].
     */
    fun calculateStrength(password: String): PasswordStrengthLevel {
        // Начальный балл
        var score = 0

        // 0. Проверка на пустоту
        if (password.isEmpty()) {
            return PasswordStrengthLevel.VERY_WEAK
        }

        val length = password.length

        // 1. Оценка длины
        when {
            length >= 16 -> score += 3 // Очень длинный
            length >= 12 -> score += 2 // Длинный
            length >= 8 -> score += 1  // Минимально приемлемый
        }

        // 2. Проверка наличия разных типов символов
        var hasLower = false
        var hasUpper = false
        var hasDigit = false
        var hasSpecial = false

        password.forEach { char ->
            when {
                char.isLowerCase() -> hasLower = true
                char.isUpperCase() -> hasUpper = true
                char.isDigit() -> hasDigit = true
                SPECIAL_CHARACTERS.contains(char) -> hasSpecial = true
            }
            // Прерываем цикл раньше, если все типы уже найдены (небольшая оптимизация)
            if (hasLower && hasUpper && hasDigit && hasSpecial) return@forEach
        }

        var typesCount = 0
        if (hasLower) typesCount++
        if (hasUpper) typesCount++
        if (hasDigit) typesCount++
        if (hasSpecial) typesCount++

        if (length >= 8) {
            when (typesCount) {
                4 -> score += 2 // Все 4 типа
                3 -> score += 1 // 3 типа
                2 -> if (length >= 10) score += 1
            }
        } else {
            if (typesCount >= 3) score += 1
        }


        if (length > 1 && password.all { it.isDigit() }) {
            score = 0 // Сразу очень слабый
        }
        else if (length > 1 && password.all { it.isLetter() }) {
            val lowerPassword = password.lowercase(Locale.getDefault())
            if (password == lowerPassword || password == lowerPassword.uppercase(Locale.getDefault())) {
                score = maxOf(0, score - 1) // Снижаем балл, но не ниже 0
            }
        }
        // TODO: Добавить более сложные проверки (словарные слова, последовательности и т.д.)
        val commonPasswords = setOf("password", "123456", "12345", "qwerty", "111111")
        if (commonPasswords.contains(password.lowercase(Locale.ROOT))) {
            score = 0
        }


        val finalScore = score.coerceIn(
            PasswordStrengthLevel.VERY_WEAK.score,
            PasswordStrengthLevel.VERY_STRONG.score
        )

        return PasswordStrengthLevel.getByScore(finalScore)
    }
}