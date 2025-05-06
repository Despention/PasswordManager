package com.example.tutor.ui.fragments

import android.content.pm.PackageInfo // <-- Импорт для PackageInfo
import android.net.Uri
import android.os.Build // <-- ДОБАВЛЕН ИМПОРТ Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
// import com.example.tutor.BuildConfig // Убедитесь, что этот импорт удален или закомментирован
import com.example.tutor.R
import com.example.tutor.databinding.FragmentProfileBinding
import com.example.tutor.viewmodels.PasswordViewModel
import java.io.File
import java.lang.Exception

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val passwordViewModel: PasswordViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        Log.d("ProfileFragment", "onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ProfileFragment", "onViewCreated")

        // Наблюдаем за профилем пользователя
        passwordViewModel.userProfile.observe(viewLifecycleOwner) { userEntity ->
            // ... (код отображения имени и аватара как раньше) ...
            if (userEntity != null) {
                binding.tvProfileEmail.text = userEntity.displayName?.takeIf { it.isNotBlank() } ?: userEntity.email ?: getString(R.string.no_email_available)
                loadAvatar(userEntity.avatarPath)
            } else {
                binding.tvProfileEmail.text = getString(R.string.loading_data)
                loadAvatar(null)
            }
        }

        // Наблюдение за статистикой паролей
        passwordViewModel.passwordCountsByCategory.observe(viewLifecycleOwner) { counts ->
            // ... (код отображения статистики как раньше) ...
            if (counts == null) return@observe
            val total = counts.values.sum(); binding.tvTotalPasswords.text = total.toString()
            val categoryText = if (counts.isEmpty()) { getString(R.string.no_passwords_yet) }
            else { counts.entries.sortedBy { it.key }.joinToString("\n") { "${it.key}: ${it.value}" } }
            binding.tvCategoryCounts.text = categoryText
        }

        // Обработчик кнопки редактирования
        binding.btnEditProfile.setOnClickListener {
            Log.d("ProfileFragment", "Нажата кнопка Редактировать профиль")
            try { findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment) } // Предполагаем, что это действие существует
            catch (e: Exception) { Log.e("ProfileFragment", "Ошибка навигации к EditProfileFragment", e) }
        }

        // Обработчик ссылки на журнал безопасности
        binding.tvSecurityLogLink?.setOnClickListener {
            Log.d("ProfileFragment", "Нажата ссылка на Журнал безопасности")
            try {
                // Используем ID глобального действия (если вы его создали в nav_graph.xml)
                findNavController().navigate(R.id.action_global_to_securityLogFragment)
            } catch (e: Exception) { Log.e("ProfileFragment", "Ошибка навигации к SecurityLogFragment", e) }
        }

        // Отображение версии приложения
        try {
            val pInfo: PackageInfo? = context?.packageManager?.getPackageInfo(requireContext().packageName, 0) // Используем context?. для безопасности
            if (pInfo != null) {
                val version = pInfo.versionName
                // --- ИСПРАВЛЕНО: Проверка версии API ---
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION") // Подавляем предупреждение
                    pInfo.versionCode.toLong()
                }
                // --- КОНЕЦ ИСПРАВЛЕНИЯ ---
                val versionText = "Версия приложения: $version ($versionCode)"
                binding.tvAppVersion?.text = versionText
            } else {
                binding.tvAppVersion?.text = "Версия приложения: ?"
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Не удалось получить версию приложения", e)
            binding.tvAppVersion?.text = "Версия приложения: ?"
        }
    }

    // Функция загрузки аватара (без изменений)
    private fun loadAvatar(avatarPath: String?) {
        val imageSource: Any? = avatarPath?.let { path -> try { File(path).takeIf { it.exists() }?.toUri() } catch (e: Exception) { null } }
        Log.d("ProfileFragment", "Загрузка аватара Glide, источник: ${imageSource ?: "Плейсхолдер/Ошибка"}")
        Glide.with(this)
            .load(imageSource)
            .apply( RequestOptions.circleCropTransform()
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_error)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE) )
            .into(binding.ivProfileAvatar)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("ProfileFragment", "onDestroyView")
        _binding = null
    }
}