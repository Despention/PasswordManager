package com.example.tutor.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "passwords", indices = [Index(value = ["isDeleted"])])
data class PasswordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val username: String,
    val encryptedPassword: String,
    val category: String,
    val lastModified: Long,

    @ColumnInfo(defaultValue = "0") // Default to false (0) in the database schema
    val isDeleted: Boolean = false,
    val deletionTimestamp: Long? = null, // Nullable Long

    val websiteUrl: String? = null, // Nullable String for website URL
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) // Specify BLOB type for byte array
    val faviconData: ByteArray? = null // Nullable ByteArray for image data
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PasswordEntity

        if (id != other.id) return false
        if (title != other.title) return false
        if (username != other.username) return false
        if (encryptedPassword != other.encryptedPassword) return false
        if (category != other.category) return false
        if (lastModified != other.lastModified) return false
        if (isDeleted != other.isDeleted) return false
        if (deletionTimestamp != other.deletionTimestamp) return false
        if (websiteUrl != other.websiteUrl) return false
        if (faviconData != null) {
            if (other.faviconData == null) return false
            if (!faviconData.contentEquals(other.faviconData)) return false
        } else if (other.faviconData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + title.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + encryptedPassword.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + lastModified.hashCode()
        result = 31 * result + isDeleted.hashCode()
        result = 31 * result + (deletionTimestamp?.hashCode() ?: 0)
        result = 31 * result + (websiteUrl?.hashCode() ?: 0)
        result = 31 * result + (faviconData?.contentHashCode() ?: 0)
        return result
    }
}