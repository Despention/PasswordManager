package com.example.tutor.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * DAO для работы с журналом событий безопасности.
 */
@Dao
interface SecurityLogDao {

    /**
     * Вставляет новое событие в журнал.
     */
    @Insert
    suspend fun insertEvent(event: SecurityLogEvent)

    /**
     * Получает все события журнала, отсортированные по времени (сначала новые).
     * Возвращает LiveData для UI.
     */
    @Query("SELECT * FROM security_log ORDER BY timestamp DESC")
    fun getAllEvents(): LiveData<List<SecurityLogEvent>>

    /**
     * Очищает весь журнал событий.
     */
    @Query("DELETE FROM security_log")
    suspend fun clearLog()

    /**
     * (Опционально) Удаляет события старше указанного времени.
     * @param timestamp Время в миллисекундах (Unix timestamp).
     */
    @Query("DELETE FROM security_log WHERE timestamp < :timestamp")
    suspend fun deleteEventsBefore(timestamp: Long)
}