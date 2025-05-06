package com.example.tutor.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapInfo
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PasswordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(password: PasswordEntity)

    @Update
    suspend fun update(password: PasswordEntity) // Добавляем стандартный Update

    // --- Изменения для Soft Delete ---
    @Query("SELECT * FROM passwords WHERE isDeleted = 0 ORDER BY title ASC") // Исключаем удаленные
    fun getAllPasswords(): LiveData<List<PasswordEntity>>

    @Query("SELECT * FROM passwords WHERE category = :category AND isDeleted = 0 ORDER BY title ASC") // Исключаем удаленные
    fun getPasswordsByCategory(category: String): LiveData<List<PasswordEntity>>

    @MapInfo(keyColumn = "category", valueColumn = "count")
    @Query("SELECT category, COUNT(*) as count FROM passwords WHERE isDeleted = 0 GROUP BY category") // Исключаем удаленные
    fun getPasswordCountsByCategory(): LiveData<Map<String, Int>>

    // Новые методы для корзины
    @Query("SELECT * FROM passwords WHERE isDeleted = 1 ORDER BY deletionTimestamp DESC") // Показываем удаленные
    fun getTrashItems(): LiveData<List<PasswordEntity>>

    @Query("UPDATE passwords SET isDeleted = 1, deletionTimestamp = :timestamp WHERE id = :id")
    suspend fun softDeleteById(id: Int, timestamp: Long) // Мягкое удаление

    @Query("UPDATE passwords SET isDeleted = 0, deletionTimestamp = NULL WHERE id = :id")
    suspend fun restoreById(id: Int) // Восстановление

    @Query("DELETE FROM passwords WHERE id = :id")
    suspend fun deletePermanentlyById(id: Int) // Полное удаление (из корзины)

    @Query("DELETE FROM passwords WHERE isDeleted = 1")
    suspend fun emptyTrash() // Очистка корзины
    // --- Конец изменений для Soft Delete ---

    // --- Метод для обновления Favicon ---
    @Query("UPDATE passwords SET websiteUrl = :url, faviconData = :favicon WHERE id = :id")
    suspend fun updateFavicon(id: Int, url: String?, favicon: ByteArray?)
    // --- Конец метода для Favicon ---

    // Старый deleteById больше не нужен, заменен на softDeleteById
    // @Query("DELETE FROM passwords WHERE id = :id")
    // suspend fun deleteById(id: Int)
}