package com.example.tutor.ui.adapters

import android.graphics.drawable.Drawable
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.tutor.R
import com.example.tutor.databinding.ItemPasswordBinding
import com.example.tutor.db.PasswordEntity
import com.example.tutor.viewmodels.PasswordViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Передаем ViewModel в конструктор для вызова загрузки иконки
class PasswordAdapter(
    private val viewModel: PasswordViewModel, // Для вызова fetchAndCacheFaviconIfNeeded
    private val onItemClicked: (PasswordEntity) -> Unit
) : ListAdapter<PasswordEntity, PasswordAdapter.PasswordViewHolder>(DiffCallback()) {

    // Форматтер для даты (создаем один раз для эффективности)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    // ViewHolder для каждого элемента списка
    inner class PasswordViewHolder(private val binding: ItemPasswordBinding) : RecyclerView.ViewHolder(binding.root) {

        // Переменная для отслеживания состояния видимости пароля для этого конкретного ViewHolder
        private var isPasswordVisible = false
        private val defaultFavicon: Drawable? =
            ContextCompat.getDrawable(binding.root.context, R.drawable.ic_default_favicon)


        init {
            // Настройка переключателя видимости один раз при создании ViewHolder
            setupPasswordVisibilityToggle()
        }

        // Заполнение ViewHolder данными из PasswordEntity
        fun bind(item: PasswordEntity) {
            binding.tvTitle.text = item.title
            binding.tvUsername.text = item.username
            binding.chipCategory.text = item.category

            // Отображаем дешифрованный пароль (приходит из ViewModel)
            binding.tvPassword.text = item.encryptedPassword
            // Применяем начальное скрытие пароля при биндинге/переиспользовании ViewHolder
            setPasswordVisibility(false)

            // Отображаем дату последнего изменения
            try {
                // Показывать дату только если она была установлена (не 0)
                if (item.lastModified > 0) {
                    binding.tvLastModified.text = itemView.context.getString(
                        R.string.last_modified_format, // Используем строку с форматом
                        dateFormat.format(Date(item.lastModified))
                    )
                    binding.tvLastModified.visibility = View.VISIBLE
                } else {
                    // Скрыть, если дата 0 или некорректна
                    binding.tvLastModified.visibility = View.GONE
                }
            } catch (e: Exception) {
                binding.tvLastModified.visibility = View.GONE
                Log.e("PasswordAdapter", "Error formatting date for item ${item.id}", e)
            }

            // Загрузка Favicon
            loadFavicon(item)

            // Устанавливаем обработчик клика на весь элемент
            binding.root.setOnClickListener {
                onItemClicked(item)
            }
        }

        // Метод для загрузки иконки сайта (Favicon)
        private fun loadFavicon(item: PasswordEntity) {
            // Если есть сохраненные данные иконки и они не пустые
            if (item.faviconData != null && item.faviconData.isNotEmpty()) {
                Glide.with(binding.ivFavicon.context)
                    .load(item.faviconData) // Загружаем ByteArray
                    .placeholder(defaultFavicon) // Плейсхолдер
                    .error(defaultFavicon) // Иконка при ошибке
                    .circleCrop() // Делаем круглой
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Не кешируем ByteArray в Glide, т.к. он уже в БД
                    .skipMemoryCache(true) // И из памяти тоже пропускаем
                    .into(binding.ivFavicon)
                binding.ivFavicon.visibility = View.VISIBLE
            }
            // Если данных нет, но есть URL, инициируем проверку/загрузку через ViewModel
            else if (!item.websiteUrl.isNullOrEmpty()) {
                // Показываем плейсхолдер, пока ViewModel пытается загрузить
                Glide.with(binding.ivFavicon.context)
                    .load(defaultFavicon) // Явно показываем плейсхолдер
                    .circleCrop()
                    .into(binding.ivFavicon)
                binding.ivFavicon.visibility = View.VISIBLE

                // Запускаем загрузку и кеширование в ViewModel
                // ViewModel сам решит, нужно ли скачивать или нет
                viewModel.fetchAndCacheFaviconIfNeeded(item)
            }
            // Если нет ни данных иконки, ни URL сайта
            else {
                // Скрываем ImageView или показываем плейсхолдер по умолчанию
                binding.ivFavicon.setImageDrawable(defaultFavicon)
                binding.ivFavicon.visibility =
                    View.VISIBLE // Или View.GONE, если не хотите плейсхолдер
            }
        }


        // Настраивает обработчик клика на кнопку показа/скрытия пароля
        private fun setupPasswordVisibilityToggle() {
            binding.btnTogglePasswordVisibility.setOnClickListener {
                setPasswordVisibility(!isPasswordVisible) // Инвертируем состояние видимости
            }
        }

        // Управляет видимостью текстового поля пароля и иконкой кнопки
        private fun setPasswordVisibility(show: Boolean) {
            isPasswordVisible = show
            if (show) {
                binding.tvPassword.transformationMethod = null // Показать пароль (убрать маску)
                binding.btnTogglePasswordVisibility.setImageResource(R.drawable.ic_visibility_off) // Иконка "скрыть"
            } else {
                binding.tvPassword.transformationMethod =
                    PasswordTransformationMethod.getInstance() // Скрыть пароль (поставить маску)
                binding.btnTogglePasswordVisibility.setImageResource(R.drawable.ic_visibility_on) // Иконка "показать"
            }
        }
    }

    // Создание нового ViewHolder (вызывается RecyclerView)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PasswordViewHolder {
        // Инфлейтим макет элемента с помощью ViewBinding
        val binding =
            ItemPasswordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PasswordViewHolder(binding)
    }

    // Привязка данных к ViewHolder (вызывается RecyclerView)
    override fun onBindViewHolder(holder: PasswordViewHolder, position: Int) {
        // Получаем элемент данных для этой позиции
        val item = getItem(position)
        // Вызываем метод bind у ViewHolder для заполнения View
        holder.bind(item)
    }

    // DiffCallback для эффективного обновления списка в RecyclerView
    class DiffCallback : DiffUtil.ItemCallback<PasswordEntity>() {
        // Проверяет, один и тот же ли это элемент (обычно по ID)
        override fun areItemsTheSame(oldItem: PasswordEntity, newItem: PasswordEntity): Boolean {
            return oldItem.id == newItem.id
        }

        // Проверяет, изменилось ли содержимое элемента
        override fun areContentsTheSame(oldItem: PasswordEntity, newItem: PasswordEntity): Boolean {
            // Используем стандартное сравнение data класса (с учетом переопределенных equals/hashCode для ByteArray)
            return oldItem == newItem
        }
    }
}