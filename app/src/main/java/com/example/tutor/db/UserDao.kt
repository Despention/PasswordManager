package com.example.tutor.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {

    // Вставить или заменить профиль (если пользователь уже есть)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserEntity)

    // Получить профиль пользователя по ID (Firebase UID)
    // Возвращает LiveData для наблюдения в UI
    @Query("SELECT * FROM user_profile WHERE userId = :userId LIMIT 1")
    fun getUserById(userId: String): LiveData<UserEntity?> // Nullable, т.к. профиля может еще не быть

    // Получить профиль пользователя (не LiveData, для однократных операций)
    @Query("SELECT * FROM user_profile WHERE userId = :userId LIMIT 1")
    suspend fun getUserByIdOnce(userId: String): UserEntity?

    // Обновить только отображаемое имя
    @Query("UPDATE user_profile SET displayName = :displayName WHERE userId = :userId")
    suspend fun updateDisplayName(userId: String, displayName: String?)

    // Обновить только путь к аватару
    @Query("UPDATE user_profile SET avatarPath = :avatarPath WHERE userId = :userId")
    suspend fun updateAvatarPath(userId: String, avatarPath: String?)

    // Или общая функция обновления (если UserEntity содержит только нужные поля)
    @Update
    suspend fun updateUser(user: UserEntity)

    // Опционально: Удалить профиль (при удалении аккаунта Firebase)
    @Query("DELETE FROM user_profile WHERE userId = :userId")
    suspend fun deleteUser(userId: String)
}