package com.example.tutor.db // Помещаем в пакет db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey
    val userId: String, // Firebase User UID

    var displayName: String?, // Отображаемое имя, может быть null

    var email: String?, // Email (можно дублировать из Auth для удобства)

    // Вариант 1: Хранить путь к файлу аватара
    var avatarPath: String? = null

    // Вариант 2: Хранить аватар как ByteArray (BLOB) - подходит для небольших изображений
    // @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    // var avatarData: ByteArray? = null

    // Можно добавить и другие поля: дата создания профиля и т.д.
    // val profileCreatedAt: Long = System.currentTimeMillis()
) {
    // Не забываем equals/hashCode, если будем хранить ByteArray
    /*
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as UserEntity
        if (userId != other.userId) return false
        if (displayName != other.displayName) return false
        if (email != other.email) return false
        // Сравнение пути или данных аватара
        if (avatarPath != other.avatarPath) return false
        // if (avatarData != null) {
        //     if (other.avatarData == null) return false
        //     if (!avatarData.contentEquals(other.avatarData)) return false
        // } else if (other.avatarData != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + (displayName?.hashCode() ?: 0)
        result = 31 * result + (email?.hashCode() ?: 0)
        result = 31 * result + (avatarPath?.hashCode() ?: 0)
        // result = 31 * result + (avatarData?.contentHashCode() ?: 0)
        return result
    }
    */
}