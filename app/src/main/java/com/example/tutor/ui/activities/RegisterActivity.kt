package com.example.tutor.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.tutor.R
import com.example.tutor.databinding.ActivityRegisterBinding // Используем биндинг
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

class RegisterActivity : FragmentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityRegisterBinding // Биндинг
    private lateinit var securityLogDao: SecurityLogDao
    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater) // Инициализация биндинга
        setContentView(binding.root)
        Log.d("RegisterActivity", "onCreate() вызвано")

        auth = Firebase.auth
        val db = AppDatabase.getDatabase(this)
        securityLogDao = db.securityLogDao()
        userDao = db.userDao()

        // Показ GIF фона
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, GifFragment())
                .commit()
        }

        // Обработчики кликов через биндинг
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            if (validateForm(email, password, confirmPassword)) {
                registerUser(email, password)
            }
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    // Валидация формы
    private fun validateForm(email: String, password: String, confirmPassword: String): Boolean {
        // Установка ошибок через TextInputLayout (доступ через binding)
        binding.tilRegEmail.error = null
        binding.tilRegPassword.error = null
        binding.tilRegConfirmPassword.error = null

        var isValid = true
        if (email.isEmpty()) { binding.tilRegEmail.error = getString(R.string.error_email_required); isValid = false }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.tilRegEmail.error = getString(R.string.error_invalid_email); isValid = false }

        if (password.isEmpty()) { binding.tilRegPassword.error = getString(R.string.error_password_required); isValid = false }
        else if (password.length < 6) { binding.tilRegPassword.error = getString(R.string.error_password_too_short); isValid = false }

        if (confirmPassword.isEmpty()) { binding.tilRegConfirmPassword.error = getString(R.string.error_confirm_password_required); isValid = false }
        else if (password != confirmPassword) { binding.tilRegConfirmPassword.error = getString(R.string.error_passwords_do_not_match); isValid = false }

        return isValid
    }

    // Регистрация пользователя
    private fun registerUser(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true
                val timestamp = System.currentTimeMillis()

                if (task.isSuccessful) { // Успешная регистрация
                    Log.d("RegisterActivity", "Пользователь успешно создан")
                    val firebaseUser = auth.currentUser

                    lifecycleScope.launch(Dispatchers.IO) { // Запускаем корутину для БД и логов
                        try { // Логирование успеха
                            securityLogDao.insertEvent(SecurityLogEvent( eventType = SecurityEventTypes.REGISTRATION_SUCCESS, description = "User: $email", timestamp = timestamp))
                            Log.i("SecurityLog", "REGISTRATION_SUCCESS залогирован для $email")
                        } catch (e: Exception) { Log.e("SecurityLog", "Ошибка логирования REGISTRATION_SUCCESS", e) }

                        // Создание профиля Room
                        if (firebaseUser != null) {
                            try {
                                val newUser = UserEntity( userId = firebaseUser.uid, displayName = firebaseUser.displayName, email = firebaseUser.email, avatarPath = null )
                                userDao.insertOrUpdateUser(newUser)
                                Log.i("RegisterActivity", "Создан профиль Room для нового пользователя ${firebaseUser.uid}")
                            } catch (e: Exception) { Log.e("RegisterActivity", "Ошибка создания профиля Room для ${firebaseUser.uid}", e) }
                        } else { Log.e("RegisterActivity", "FirebaseUser null после успешной регистрации!") }
                    } // Корутина завершится

                    // Переход в MainActivity (выполняется в основном потоке)
                    val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                } else { // Ошибка регистрации
                    val errorMsg = task.exception?.localizedMessage ?: getString(R.string.unknown_error)
                    Log.w("RegisterActivity", "createUserWithEmail:failure", task.exception)
                    // Логирование ошибки
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            securityLogDao.insertEvent(SecurityLogEvent( eventType = SecurityEventTypes.REGISTRATION_FAILURE, description = "User: $email, Reason: $errorMsg", timestamp = timestamp))
                            Log.w("SecurityLog", "REGISTRATION_FAILURE залогирован для $email")
                        } catch (e: Exception) { Log.e("SecurityLog", "Ошибка логирования REGISTRATION_FAILURE", e) }
                    }
                    Toast.makeText(baseContext, getString(R.string.error_registration_failed, errorMsg), Toast.LENGTH_LONG).show()
                }
            }
    }
}