package com.example.tutor.security // Или com.example.tutor.util

import androidx.annotation.StringRes
import com.example.tutor.R // Убедитесь, что R импортирован для доступа к строкам

/**
 * Перечисление, представляющее уровни надежности пароля.
 *
 * @property score Условный балл надежности (например, от 0 до 4).
 * @property descriptionResId ID строкового ресурса для описания уровня надежности (например, "Слабый").
 */
enum class PasswordStrengthLevel(val score: Int, @StringRes val descriptionResId: Int) {
    /** Пароль очень слабый (пустой, слишком короткий, очень предсказуемый). */
    VERY_WEAK(0, R.string.strength_very_weak),

    /** Пароль слабый (короткий, использует только один тип символов). */
    WEAK(1, R.string.strength_weak),

    /** Пароль средней надежности (достаточная длина, несколько типов символов). */
    MEDIUM(2, R.string.strength_medium),

    /** Надежный пароль (хорошая длина, разные типы символов). */
    STRONG(3, R.string.strength_strong),

    /** Очень надежный пароль (длинный, включает все типы символов). */
    VERY_STRONG(4, R.string.strength_very_strong);

    companion object {
        /**
         * Получает уровень надежности по числовому баллу.
         * Если балл вне диапазона, возвращает MEDIUM.
         */
        fun getByScore(score: Int): PasswordStrengthLevel {
            return values().find { it.score == score.coerceIn(VERY_WEAK.score, VERY_STRONG.score) }
                ?: MEDIUM // Возвращаем MEDIUM по умолчанию, если что-то пошло не так
        }
    }
}