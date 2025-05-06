package com.example.tutor.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность для хранения записей журнала событий безопасности.
 */
@Entity(
    tableName = "security_log",
    indices = [Index(value = ["timestamp"])] // Индекс по времени для сортировки
)
data class SecurityLogEvent(
    @PrimaryKey(autoGenerate = true)
    val eventId: Long = 0, // ID события

    val eventType: String, // Тип события (используйте константы из SecurityEventTypes)
    val description: String?, // Опциональное описание (например, имя измененного пароля, причина ошибки)
    val timestamp: Long // Время события (System.currentTimeMillis())
)