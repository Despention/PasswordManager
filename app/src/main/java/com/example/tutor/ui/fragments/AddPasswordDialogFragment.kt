package com.example.tutor.ui.fragments

import android.app.Dialog
import android.content.res.ColorStateList // <-- Импорт для цвета индикатора
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher // <-- Импорт TextWatcher
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat // <-- Импорт для получения цветов
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.tutor.R
import com.example.tutor.databinding.DialogAddPasswordBinding
import com.example.tutor.security.PasswordStrengthAnalyzer // <-- Импорт анализатора
import com.example.tutor.security.PasswordStrengthLevel // <-- Импорт Enum
import com.example.tutor.viewmodels.PasswordViewModel
import android.util.Log
import android.view.View // <-- Импорт View для visibility
import java.lang.Exception

class AddPasswordDialogFragment : DialogFragment() {

    private var _binding: DialogAddPasswordBinding? = null
    private val binding get() = _binding!!
    private val passwordViewModel: PasswordViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddPasswordBinding.inflate(requireActivity().layoutInflater)
        Log.d("AddPasswordDialog", "onCreateDialog")

        setupSpinner() // Настройка Spinner
        setupPasswordStrengthChecker() // !!! Настройка проверки пароля !!!

        val builder = AlertDialog.Builder(requireActivity())
            .setView(binding.root)
            .setTitle(R.string.add_new_password)
            .setPositiveButton(R.string.add_password_button, null)
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }

        val dialog = builder.create()
        dialog.setOnShowListener { dialogInterface ->
            val positiveButton = (dialogInterface as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener { savePassword() }
        }
        return dialog
    }

    // Настройка Spinner (без изменений)
    private fun setupSpinner() {
        val spinnerCategories = passwordViewModel.categories.filter { it != "All" }
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item_custom, spinnerCategories)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_custom)
        binding.spinnerDialogCategory.adapter = adapter
        val defaultSelection = spinnerCategories.indexOf("Other")
        if (defaultSelection >= 0) { binding.spinnerDialogCategory.setSelection(defaultSelection) }
    }

    // --- !!! НОВЫЙ МЕТОД: Настройка проверки надежности пароля !!! ---
    private fun setupPasswordStrengthChecker() {
        binding.etDialogPasswordValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { /* Не используется */ }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { /* Не используется */ }

            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                if (password.isEmpty()) {
                    // Скрыть индикаторы, если поле пустое
                    binding.tvPasswordStrength.visibility = View.GONE
                    binding.progressPasswordStrength.visibility = View.GONE
                } else {
                    // Показать индикаторы
                    binding.tvPasswordStrength.visibility = View.VISIBLE
                    binding.progressPasswordStrength.visibility = View.VISIBLE
                    // Рассчитать и отобразить надежность
                    val strength = PasswordStrengthAnalyzer.calculateStrength(password)
                    updateStrengthUI(strength)
                }
                // Сбрасываем ошибку валидации при изменении текста
                binding.tilDialogPassword.error = null
            }
        })
    }

    // --- !!! НОВЫЙ МЕТОД: Обновление UI оценки надежности !!! ---
    private fun updateStrengthUI(strength: PasswordStrengthLevel) {
        val context = requireContext()
        val strengthText = getString(strength.descriptionResId)
        val strengthColorRes = when (strength) {
            PasswordStrengthLevel.VERY_WEAK -> R.color.password_strength_very_weak
            PasswordStrengthLevel.WEAK -> R.color.password_strength_weak
            PasswordStrengthLevel.MEDIUM -> R.color.password_strength_medium
            PasswordStrengthLevel.STRONG -> R.color.password_strength_strong
            PasswordStrengthLevel.VERY_STRONG -> R.color.password_strength_very_strong
        }
        val strengthColor = ContextCompat.getColor(context, strengthColorRes)

        // Масштабируем оценку (0-4) в прогресс (0-100)
        val progress = when (strength) {
            PasswordStrengthLevel.VERY_WEAK -> 10 // Немного прогресса даже для слабого
            PasswordStrengthLevel.WEAK -> 30
            PasswordStrengthLevel.MEDIUM -> 55
            PasswordStrengthLevel.STRONG -> 80
            PasswordStrengthLevel.VERY_STRONG -> 100
        }

        // Обновляем TextView
        binding.tvPasswordStrength.text = strengthText
        binding.tvPasswordStrength.setTextColor(strengthColor)

        // Обновляем LinearProgressIndicator
        binding.progressPasswordStrength.progress = progress
        binding.progressPasswordStrength.setIndicatorColor(strengthColor)
        // Можно также установить цвет трека (фона индикатора)
        // binding.progressPasswordStrength.trackColor = ContextCompat.getColor(context, R.color.password_strength_default)

        Log.d("AddPasswordDialog","Надежность пароля: ${strength.name}, Progress: $progress")
    }


    // Метод сохранения пароля (без изменений по сути, но использует ID TextInputLayout для ошибки)
    private fun savePassword() {
        if (_binding == null) return

        val title = binding.etDialogPasswordTitle.text.toString().trim()
        val username = binding.etDialogPasswordUsername.text.toString().trim()
        val password = binding.etDialogPasswordValue.text.toString().trim()
        val websiteUrl = binding.etDialogWebsiteUrl.text.toString().trim()
        val selectedCategoryItem = binding.spinnerDialogCategory.selectedItem

        var isValid = true
        binding.etDialogPasswordTitle.error = if (title.isEmpty()) { isValid = false; getString(R.string.error_field_required) } else null
        binding.etDialogPasswordUsername.error = if (username.isEmpty()) { isValid = false; getString(R.string.error_field_required) } else null
        // Устанавливаем ошибку на TextInputLayout для пароля
        binding.tilDialogPassword.error = if (password.isEmpty()) { isValid = false; getString(R.string.error_password_required) } else null
        binding.etDialogWebsiteUrl.error = null

        val category = selectedCategoryItem as? String
        if (category == null) { isValid = false; Toast.makeText(context, "Ошибка: Категория не выбрана", Toast.LENGTH_SHORT).show() }

        if (!isValid) return

        try {
            passwordViewModel.addPassword( title, username, password, category!!, websiteUrl.ifEmpty { null } )
            Toast.makeText(context, R.string.password_added_success, Toast.LENGTH_SHORT).show()
            dismiss()
        } catch (e: Exception) {
            Log.e("AddPasswordDialog", "Ошибка добавления пароля", e)
            Toast.makeText(context, "Ошибка добавления пароля", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("AddPasswordDialog", "onDestroyView")
        _binding = null
    }
}