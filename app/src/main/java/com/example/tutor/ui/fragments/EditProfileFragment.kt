package com.example.tutor.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.distinctUntilChanged
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.tutor.R
import com.example.tutor.databinding.FragmentEditProfileBinding // Используем биндинг
import com.example.tutor.db.UserEntity
import com.example.tutor.viewmodels.PasswordViewModel
import java.io.File
import java.lang.Exception

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val passwordViewModel: PasswordViewModel by activityViewModels()

    private var currentUserProfile: UserEntity? = null
    private var selectedAvatarUri: Uri? = null

    private lateinit var pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                Log.d("EditProfileFragment", "Выбрано изображение: $uri")
                selectedAvatarUri = uri
                loadAvatarPreview(uri)
            } else {
                Log.d("EditProfileFragment", "Выбор изображения отменен.")
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("EditProfileFragment", "onViewCreated")
        setupToolbar()
        setupClickListeners()

        passwordViewModel.userProfile.distinctUntilChanged().observe(viewLifecycleOwner) { user ->
            if (user != null && (currentUserProfile == null || currentUserProfile?.userId != user.userId)) {
                Log.d("EditProfileFragment", "Предзаполнение данных профиля для ${user.userId}")
                currentUserProfile = user
                binding.etDisplayName.setText(user.displayName ?: "")
                binding.tvEditEmailValue.text = user.email ?: ""
                selectedAvatarUri = null
                val currentAvatarUri = user.avatarPath?.let { path -> try { File(path).takeIf { it.exists() }?.toUri() } catch (e: Exception) { null } }
                loadAvatarPreview(currentAvatarUri)
            } else if (user != null) {
                currentUserProfile = user
            } else {
                Log.w("EditProfileFragment", "UserEntity is null в observe")
                binding.etDisplayName.setText("")
                binding.tvEditEmailValue.text = ""
                loadAvatarPreview(null)
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbarEditProfile.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbarEditProfile.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_save_profile -> { saveProfileChanges(); true }
                else -> false
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnChangeAvatar.setOnClickListener { openImagePicker() }
        binding.ivEditAvatar.setOnClickListener { openImagePicker() }
    }

    private fun openImagePicker() {
        Log.d("EditProfileFragment", "Запуск выбора изображения")
        try { // Добавим try-catch на случай проблем с лаунчером
            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            Log.e("EditProfileFragment", "Ошибка запуска выбора изображения", e)
            Toast.makeText(context, "Не удалось открыть галерею", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAvatarPreview(uri: Uri?) {
        Glide.with(this)
            .load(uri)
            .apply(RequestOptions.circleCropTransform()
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_error)
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Не кешируем превью файла? Или AUTOMATIC? Решите сами.
                .skipMemoryCache(false) // Можно кешировать в памяти
            ).into(binding.ivEditAvatar)
        Log.d("EditProfileFragment", "Показан предпросмотр аватара: ${uri ?: "плейсхолдер"}")
    }

    private fun saveProfileChanges() {
        val newDisplayName = binding.etDisplayName.text.toString().trim().takeIf { it.isNotEmpty() }
        val avatarToSaveUri = selectedAvatarUri

        val nameChanged = newDisplayName != (currentUserProfile?.displayName ?: "")
        val avatarChanged = selectedAvatarUri != null

        if (!nameChanged && !avatarChanged) {
            Toast.makeText(context, "Нет изменений для сохранения", Toast.LENGTH_SHORT).show()
            return
        }

        binding.editProfileProgressBar.isVisible = true
        setEditingEnabled(false) // Блокируем UI

        Log.d("EditProfileFragment", "Сохранение изменений: Имя='$newDisplayName', Новый аватар URI='$avatarToSaveUri'")
        passwordViewModel.updateUserProfile(newDisplayName, avatarToSaveUri)

        view?.postDelayed({
            if (isAdded) {
                binding.editProfileProgressBar.isVisible = false
                // setEditingEnabled(true) // Разблокировать не нужно, т.к. уходим
                // Используем строку из ресурсов
                Toast.makeText(context, R.string.profile_saved_success, Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }, 700) // Немного увеличим задержку для наглядности
    }

    // Вспомогательная функция для включения/выключения полей
    private fun setEditingEnabled(enabled: Boolean) {
        binding.toolbarEditProfile.menu.findItem(R.id.action_save_profile)?.isEnabled = enabled
        binding.etDisplayName.isEnabled = enabled
        binding.btnChangeAvatar.isEnabled = enabled
        binding.ivEditAvatar.isEnabled = enabled
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("EditProfileFragment", "onDestroyView")
        _binding = null
    }
}