package com.example.tutor.viewmodels

import androidx.lifecycle.LiveData
import com.example.tutor.db.*
import com.example.tutor.util.SecurityEventTypes
import android.util.Log
import java.lang.Exception

class PasswordRepository(
    private val passwordDao: PasswordDao,
    private val securityLogDao: SecurityLogDao,
    private val userDao: UserDao
) {

    // --- LiveData ---
    val allPasswords: LiveData<List<PasswordEntity>> = passwordDao.getAllPasswords()
    val passwordCountsByCategory: LiveData<Map<String, Int>> = passwordDao.getPasswordCountsByCategory()
    val trashItems: LiveData<List<PasswordEntity>> = passwordDao.getTrashItems()
    val securityLogEvents: LiveData<List<SecurityLogEvent>> = securityLogDao.getAllEvents()

    // --- Методы для паролей (без изменений) ---
    fun getPasswordsByCategory(category: String): LiveData<List<PasswordEntity>> = passwordDao.getPasswordsByCategory(category)
    suspend fun insert(password: PasswordEntity) = tryOrLog("insert") { passwordDao.insert(password) }
    suspend fun update(password: PasswordEntity) = tryOrLog("update") { passwordDao.update(password) }
    suspend fun softDeleteById(id: Int) = tryOrLog("softDeleteById", id) { passwordDao.softDeleteById(id, System.currentTimeMillis()) }
    suspend fun restoreById(id: Int) = tryOrLog("restoreById", id) { passwordDao.restoreById(id) }
    suspend fun deletePermanentlyById(id: Int) = tryOrLog("deletePermanentlyById", id) { passwordDao.deletePermanentlyById(id) }
    suspend fun emptyTrash() = tryOrLog("emptyTrash") { passwordDao.emptyTrash() }
    suspend fun updateFavicon(id: Int, url: String?, favicon: ByteArray?) = tryOrLog("updateFavicon", id) { passwordDao.updateFavicon(id, url, favicon) }

    // --- Методы для журнала безопасности (без изменений) ---
    suspend fun logSecurityEvent(eventType: String, description: String?) {
        try {
            securityLogDao.insertEvent(SecurityLogEvent(eventType = eventType, description = description, timestamp = System.currentTimeMillis()))
        } catch (e: Exception) {
            Log.e("PasswordRepository", "Не удалось записать событие безопасности '$eventType': ${e.message}")
        }
    }
    suspend fun clearSecurityLog() {
        try {
            val deletedCount = securityLogDao.clearLog()
            Log.i("PasswordRepository", "Журнал безопасности очищен, удалено записей: $deletedCount.")
            logSecurityEvent(SecurityEventTypes.SECURITY_LOG_CLEARED, "Пользователь очистил журнал безопасности.")
        } catch (e: Exception) {
            Log.e("PasswordRepository", "Не удалось очистить журнал безопасности", e)
        }
    }

    // --- Методы для профиля пользователя ---
    fun getUserProfile(userId: String): LiveData<UserEntity?> = userDao.getUserById(userId)
    suspend fun getUserProfileOnce(userId: String): UserEntity? = tryOrLog("getUserProfileOnce", userId) { userDao.getUserByIdOnce(userId) }
    suspend fun saveUserProfile(user: UserEntity) = tryOrLog("saveUserProfile", user.userId) { userDao.insertOrUpdateUser(user) }
    suspend fun updateUserDisplayName(userId: String, displayName: String?) = tryOrLog("updateUserDisplayName", userId) { userDao.updateDisplayName(userId, displayName) }
    suspend fun updateAvatarPath(userId: String, avatarPath: String?) = tryOrLog("updateAvatarPath", userId) { userDao.updateAvatarPath(userId, avatarPath) }

    // Вспомогательная функция для логирования ошибок DAO
    private suspend inline fun <T> tryOrLog(operation: String, id: Any? = null, block: () -> T): T? {
        return try {
            val result = block()
            Log.d("PasswordRepository", "Операция '$operation'${if(id != null) " для ID $id" else ""} выполнена успешно.")
            result
        } catch (e: Exception) {
            Log.e("PasswordRepository", "Ошибка выполнения операции '$operation'${if(id != null) " для ID $id" else ""}", e)
            null
        }
    }
}