package com.example.tutor.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.example.tutor.db.*
import com.example.tutor.security.CryptoManager
import com.example.tutor.util.SecurityEventTypes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit
import java.lang.Exception

class PasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PasswordRepository
    private val auth: FirebaseAuth = Firebase.auth

    // LiveData для паролей, логов и т.д.
    val passwordCountsByCategory: LiveData<Map<String, Int>>
    private val _displayedPasswords = MediatorLiveData<List<PasswordEntity>>()
    val displayedPasswords: LiveData<List<PasswordEntity>> = _displayedPasswords
    private val _displayedTrashItems = MutableLiveData<List<PasswordEntity>>()
    val displayedTrashItems: LiveData<List<PasswordEntity>> = _displayedTrashItems
    val securityLog: LiveData<List<SecurityLogEvent>>
    private val _currentCategoryFilter = MutableLiveData("All")
    val currentCategoryFilter: LiveData<String> = _currentCategoryFilter
    val categories = listOf("All", "Social", "Gaming", "Work", "Finance", "Other")

    private lateinit var allPasswordsSource: LiveData<List<PasswordEntity>>
    private var passwordsByCategorySource: LiveData<List<PasswordEntity>>? = null
    private lateinit var trashItemsSource: LiveData<List<PasswordEntity>>

    private val _userProfile = MediatorLiveData<UserEntity?>()
    val userProfile: LiveData<UserEntity?> = _userProfile

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    init {
        Log.d("PasswordViewModel", "Инициализация...")
        val db = AppDatabase.getDatabase(application)
        val passwordDao = db.passwordDao()
        val securityLogDao = db.securityLogDao()
        val userDao = db.userDao()
        repository = PasswordRepository(passwordDao, securityLogDao, userDao)

        // Инициализация LiveData
        passwordCountsByCategory = repository.passwordCountsByCategory
        securityLog = repository.securityLogEvents
        allPasswordsSource = repository.allPasswords
        trashItemsSource = repository.trashItems

        setupMediator()
        observeTrashItems()

        // Наблюдение за профилем пользователя
        auth.currentUser?.uid?.let { userId ->
            val source = repository.getUserProfile(userId)
            _userProfile.addSource(source) { user ->
                if (_userProfile.value != user) {
                    _userProfile.value = user
                    Log.d("PasswordViewModel", "Профиль пользователя $userId обновлен в LiveData.")
                }
                if (user == null && _userProfile.value == null) {
                    Log.i("PasswordViewModel", "Профиль для пользователя $userId не найден, создаем...")
                    createInitialUserProfile(userId)
                }
            }
        } ?: Log.e("PasswordViewModel", "Текущий пользователь Firebase null при инициализации ViewModel!")

        Log.d("PasswordViewModel", "Инициализация завершена.")
    }

    private fun createInitialUserProfile(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUser = auth.currentUser
            if (currentUser != null && currentUser.uid == userId) {
                val existingProfile = repository.getUserProfileOnce(userId)
                if (existingProfile == null) {
                    val newUser = UserEntity( userId = userId, displayName = currentUser.displayName?.takeIf { it.isNotBlank() }, email = currentUser.email, avatarPath = null )
                    repository.saveUserProfile(newUser)
                    Log.i("PasswordViewModel", "Создан начальный профиль Room для $userId")
                } else {
                    Log.w("PasswordViewModel", "Попытка создать профиль Room для $userId, но он уже существует.")
                    // Можно добавить логику обновления существующего профиля из Firebase при необходимости
                }
            } else { Log.e("PasswordViewModel", "Не удалось получить FirebaseUser для создания профиля $userId") }
        }
    }

    fun updateUserProfile(displayName: String?, avatarUri: Uri?) {
        val currentUserId = auth.currentUser?.uid ?: return Unit.also { Log.e("PasswordViewModel", "Невозможно обновить профиль: пользователь не авторизован.") }
        val currentProfileSnapshot = _userProfile.value

        viewModelScope.launch(Dispatchers.IO) {
            var finalAvatarPath: String? = currentProfileSnapshot?.avatarPath
            if (avatarUri != null) {
                val savedPath = saveAvatarToFile(avatarUri, currentUserId)
                if (savedPath != null) {
                    if (!currentProfileSnapshot?.avatarPath.isNullOrEmpty() && currentProfileSnapshot?.avatarPath != savedPath) {
                        deleteOldAvatar(currentProfileSnapshot?.avatarPath)
                    }
                    finalAvatarPath = savedPath
                } else { Log.e("PasswordViewModel", "Не удалось сохранить новый аватар.") }
            }

            val profileToSave = currentProfileSnapshot?.copy(
                displayName = displayName, avatarPath = finalAvatarPath, email = auth.currentUser?.email
            ) ?: UserEntity( userId = currentUserId, displayName = displayName, email = auth.currentUser?.email, avatarPath = finalAvatarPath )

            repository.saveUserProfile(profileToSave)
            Log.i("PasswordViewModel", "Профиль пользователя $currentUserId обновлен в БД.")
        }
    }

    private suspend fun saveAvatarToFile(sourceUri: Uri, userId: String): String? = withContext(Dispatchers.IO) {
        val context = getApplication<Application>().applicationContext
        var outputFile: File? = null // Для удаления в случае ошибки
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                val fileName = "avatar_$userId.jpg"
                val outputDir = context.filesDir
                outputFile = File(outputDir, fileName)

                FileOutputStream(outputFile).use { outputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                        Log.d("PasswordViewModel", "Аватар сжат и сохранен в: ${outputFile?.absolutePath}")
                        return@withContext outputFile?.absolutePath
                    } else {
                        Log.e("PasswordViewModel", "Не удалось декодировать Bitmap из Uri: $sourceUri")
                        return@withContext null
                    }
                }
            } ?: run { Log.e("PasswordViewModel", "Не удалось открыть InputStream для Uri: $sourceUri"); return@withContext null }
        } catch (e: Exception) { // Ловим более общие ошибки тоже
            Log.e("PasswordViewModel", "Ошибка при сохранении файла аватара", e)
            try { outputFile?.delete() } catch (_: Exception) {} // Пытаемся удалить недописанный файл
            return@withContext null
        }
    }


    private fun deleteOldAvatar(filePath: String?) {
        if (filePath.isNullOrEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (file.exists() && file.delete()) {
                    Log.i("PasswordViewModel", "Старый файл аватара удален: $filePath")
                } else if (file.exists()) {
                    Log.w("PasswordViewModel", "Не удалось удалить старый файл аватара: $filePath")
                }
            } catch (e: Exception) {
                Log.e("PasswordViewModel", "Ошибка при удалении старого аватара: $filePath", e)
            }
        }
    }


    private fun setupMediator() {
        _displayedPasswords.addSource(allPasswordsSource) { list -> if (_currentCategoryFilter.value == "All") decryptAndSetPasswords(list, _displayedPasswords) }
        _displayedPasswords.addSource(_currentCategoryFilter) { category -> updateDataSource(category) }
    }

    private fun observeTrashItems() { trashItemsSource.observeForever { list -> decryptAndSetPasswords(list, _displayedTrashItems) } }

    private fun updateDataSource(category: String) {
        passwordsByCategorySource?.let { _displayedPasswords.removeSource(it) }
        passwordsByCategorySource = if (category == "All") {
            decryptAndSetPasswords(allPasswordsSource.value, _displayedPasswords); null
        } else {
            repository.getPasswordsByCategory(category).also { newSource ->
                _displayedPasswords.addSource(newSource) { list -> if (_currentCategoryFilter.value == category) decryptAndSetPasswords(list, _displayedPasswords) }
            }
        }
    }

    private fun decryptAndSetPasswords(encryptedList: List<PasswordEntity>?, targetLiveData: MutableLiveData<List<PasswordEntity>>) {
        viewModelScope.launch(Dispatchers.Default) {
            val decrypted = encryptedList?.mapNotNull { entity -> try { if (entity.encryptedPassword != "****** ERROR ******") CryptoManager.decryptFromBase64(entity.encryptedPassword)?.let { entity.copy(encryptedPassword = it) } ?: entity.copy(encryptedPassword = "****** ERROR ******").also { Log.e("PasswordViewModel", "Decryption returned null id=${entity.id}") } else entity } catch (e: Exception) { Log.e("PasswordViewModel", "Decryption failed id=${entity.id}", e); entity.copy(encryptedPassword = "****** ERROR ******") } } ?: emptyList()
            withContext(Dispatchers.Main) { targetLiveData.value = decrypted }
        }
    }

    fun setCategoryFilter(category: String) { if (categories.contains(category) && _currentCategoryFilter.value != category) { _currentCategoryFilter.value = category } }
    fun addPassword(title: String, username: String, passwordToEncrypt: String, category: String, websiteUrl: String?) { viewModelScope.launch(Dispatchers.IO) { try { val encPass = CryptoManager.encryptToBase64(passwordToEncrypt); if (encPass==null) return@launch; val newPass = PasswordEntity(title=title, username=username, encryptedPassword=encPass, category=category, lastModified=System.currentTimeMillis(), websiteUrl=websiteUrl?.trim()?.ifEmpty { null }); repository.insert(newPass); repository.logSecurityEvent(SecurityEventTypes.PASSWORD_ADDED, "Title: $title") } catch (e: Exception) { Log.e("PasswordViewModel", "Add password '$title' failed", e) } } }
    fun softDeletePassword(id: Int) { viewModelScope.launch(Dispatchers.IO) { try { repository.softDeleteById(id); repository.logSecurityEvent(SecurityEventTypes.PASSWORD_SOFT_DELETED, "ID: $id") } catch (e: Exception) { Log.e("PasswordViewModel", "Soft delete ID $id failed", e) } } }
    fun restorePassword(id: Int) { viewModelScope.launch(Dispatchers.IO) { try { repository.restoreById(id); repository.logSecurityEvent(SecurityEventTypes.PASSWORD_RESTORED, "ID: $id") } catch (e: Exception) { Log.e("PasswordViewModel", "Restore ID $id failed", e) } } }
    fun deletePasswordPermanently(id: Int) { viewModelScope.launch(Dispatchers.IO) { try { repository.deletePermanentlyById(id); repository.logSecurityEvent(SecurityEventTypes.PASSWORD_PERMANENTLY_DELETED, "ID: $id") } catch (e: Exception) { Log.e("PasswordViewModel", "Permanent delete ID $id failed", e) } } }
    fun emptyTrash() { viewModelScope.launch(Dispatchers.IO) { try { repository.emptyTrash(); repository.logSecurityEvent(SecurityEventTypes.TRASH_EMPTIED, null) } catch (e: Exception) { Log.e("PasswordViewModel", "Empty trash failed", e) } } }
    fun clearSecurityLog() { viewModelScope.launch(Dispatchers.IO) { try { repository.clearSecurityLog() } catch (e: Exception) { Log.e("PasswordViewModel", "Clear log failed", e) } } }
    fun fetchAndCacheFaviconIfNeeded(passwordEntity: PasswordEntity) { if (passwordEntity.websiteUrl.isNullOrEmpty() || passwordEntity.faviconData != null) return; viewModelScope.launch(Dispatchers.IO) { val bytes = downloadFavicon(passwordEntity.websiteUrl); if (bytes != null) repository.updateFavicon(passwordEntity.id, passwordEntity.websiteUrl, bytes) else Log.w("Favicon", "Download failed ID: ${passwordEntity.id}") } }

    private suspend fun downloadFavicon(baseUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        val googleFaviconUrl = try { val cleanUrl = ensureValidUrl(baseUrl) ?: return@withContext null; "https://www.google.com/s2/favicons?sz=128&domain_url=${URL(cleanUrl).host}" }
        catch (e: Exception) { Log.e("Favicon", "URL error: $baseUrl", e); return@withContext null }
        val request = Request.Builder().url(googleFaviconUrl).build()
        try { httpClient.newCall(request).execute().use { response -> if (!response.isSuccessful) return@withContext null; val bytes = response.body?.bytes(); if (bytes == null || bytes.size < 50 || BitmapFactory.decodeByteArray(bytes, 0, bytes.size) == null) return@withContext null; bytes } }
        catch (e: Exception) { Log.e("Favicon", "Download error: $googleFaviconUrl", e); return@withContext null }
    }

    private fun ensureValidUrl(url: String?): String? = try { url?.trim()?.takeIf { it.isNotBlank() }?.let { var v = it; if (!v.startsWith("http")) v = "https://$v"; URL(v).toURI(); v } } catch (e: Exception) { null }

    override fun onCleared() {
        super.onCleared()
        Log.d("PasswordViewModel", "onCleared")
        try { trashItemsSource.removeObserver { } } catch (e: Exception) { Log.w("PasswordViewModel", "Error removing trash observer", e) }
        // Отписываемся от источника профиля, если он был добавлен
        _userProfile.value?.userId?.let { uid -> auth.currentUser?.uid?.let { if (it == uid) _userProfile.removeSource(repository.getUserProfile(it)) } }
        // Отписываемся от источников паролей
        _displayedPasswords.removeSource(allPasswordsSource)
        passwordsByCategorySource?.let { _displayedPasswords.removeSource(it) }
        viewModelScope.cancel()
    }
}