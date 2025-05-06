package com.example.tutor.ui.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.tutor.R
import com.example.tutor.databinding.ActivityLoginBinding // Используем биндинг
import com.example.tutor.db.AppDatabase
import com.example.tutor.db.SecurityLogDao
import com.example.tutor.db.SecurityLogEvent
import com.example.tutor.db.UserDao
import com.example.tutor.db.UserEntity
import com.example.tutor.ui.fragments.GifFragment
import com.example.tutor.util.SecurityEventTypes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
// import kotlinx.coroutines.withContext // Не нужен
import java.lang.Exception

class LoginActivity : FragmentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding // Биндинг
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var securityLogDao: SecurityLogDao
    private lateinit var userDao: UserDao

    companion object {
        private const val PREFS_NAME = "TutorPrefs"
        private const val PREF_REMEMBER_ME = "rememberMe"
        private const val PREF_EMAIL = "savedEmail"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater) // Инициализация биндинга
        setContentView(binding.root)
        Log.d("LoginActivity", "onCreate() вызвано")

        auth = Firebase.auth
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val db = AppDatabase.getDatabase(this)
        securityLogDao = db.securityLogDao()
        userDao = db.userDao()

        loadPreferences() // Загрузка "Запомнить меня"

        // Показ GIF фона
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, GifFragment())
                .commit()
        }

        // Обработчики кликов через биндинг
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (validateForm(email, password)) {
                loginUser(email, password)
            }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // Загрузка настроек
    private fun loadPreferences() {
        val rememberMe = sharedPreferences.getBoolean(PREF_REMEMBER_ME, false)
        binding.cbRememberMe.isChecked = rememberMe
        if (rememberMe) {
            binding.etEmail.setText(sharedPreferences.getString(PREF_EMAIL, ""))
        }
    }

    // Сохранение настроек
    private fun savePreferences(email: String) {
        val rememberMeChecked = binding.cbRememberMe.isChecked
        sharedPreferences.edit {
            putBoolean(PREF_REMEMBER_ME, rememberMeChecked)
            if (rememberMeChecked) putString(PREF_EMAIL, email) else remove(PREF_EMAIL)
        }
        Log.d("LoginActivity", "Настройки сохранены: rememberMe=$rememberMeChecked")
    }

    // Валидация формы
    private fun validateForm(email: String, password: String): Boolean {
        binding.tilEmail.error = null // Используем ID TextInputLayout из XML
        binding.tilPassword.error = null // Используем ID TextInputLayout из XML

        var isValid = true
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_email_required); isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_invalid_email); isValid = false
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_password_required); isValid = false
        }
        return isValid
    }

    // Вход пользователя
    private fun loginUser(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                val timestamp = System.currentTimeMillis()

                if (task.isSuccessful) { // Успешный вход
                    Log.d("LoginActivity", "Успешный вход")
                    val firebaseUser = auth.currentUser
                    savePreferences(email)

                    lifecycleScope.launch(Dispatchers.IO) {
                        // Логирование успеха
                        try {
                            securityLogDao.insertEvent(SecurityLogEvent(eventType = SecurityEventTypes.LOGIN_SUCCESS, description = "User: $email", timestamp = timestamp))
                            Log.i("SecurityLog", "LOGIN_SUCCESS залогирован для $email")
                        } catch (e: Exception) { Log.e("SecurityLog", "Ошибка логирования LOGIN_SUCCESS", e) }

                        // Создание/обновление профиля Room
                        if (firebaseUser != null) {
                            try {
                                val existingUser = userDao.getUserByIdOnce(firebaseUser.uid)
                                val userToSave = existingUser?.copy(email = firebaseUser.email, displayName = firebaseUser.displayName)
                                    ?: UserEntity(userId = firebaseUser.uid, displayName = firebaseUser.displayName, email = firebaseUser.email, avatarPath = null)
                                userDao.insertOrUpdateUser(userToSave)
                                Log.i("LoginActivity", "Профиль Room создан/обновлен для ${firebaseUser.uid}")
                            } catch (e: Exception) { Log.e("LoginActivity", "Ошибка создания/обновления профиля Room для ${firebaseUser.uid}", e) }
                        } else { Log.e("LoginActivity", "FirebaseUser null после успешного входа!") }
                    } // Корутина завершится

                    // Переход в MainActivity (выполняется в основном потоке)
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                } else { // Ошибка входа
                    val errorMsg = task.exception?.localizedMessage ?: getString(R.string.unknown_error)
                    Log.e("LoginActivity", "Ошибка входа: ${task.exception?.message}")
                    // Логирование ошибки
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            securityLogDao.insertEvent(SecurityLogEvent( eventType = SecurityEventTypes.LOGIN_FAILURE, description = "User: $email, Reason: $errorMsg", timestamp = timestamp))
                            Log.w("SecurityLog", "LOGIN_FAILURE залогирован для $email")
                        } catch (e: Exception) { Log.e("SecurityLog", "Ошибка логирования LOGIN_FAILURE", e) }
                    }
                    Toast.makeText(baseContext, getString(R.string.error_login_failed, errorMsg), Toast.LENGTH_LONG).show()
                }
            }
    }
}