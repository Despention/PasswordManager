package com.example.tutor.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tutor.R
import com.example.tutor.databinding.FragmentPasswordsBinding
import com.example.tutor.db.PasswordEntity
import com.example.tutor.ui.adapters.PasswordAdapter
import com.example.tutor.viewmodels.PasswordViewModel
import com.google.android.material.snackbar.Snackbar

class PasswordsFragment : Fragment() {

    private var _binding: FragmentPasswordsBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    // Используем activityViewModels для получения ViewModel, общей для активности
    private val passwordViewModel: PasswordViewModel by activityViewModels()

    private lateinit var passwordAdapter: PasswordAdapter
    private var spinnerAdapter: ArrayAdapter<String>? = null // Для опционального хранения ссылки на адаптер Spinner

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasswordsBinding.inflate(inflater, container, false)
        Log.d("PasswordsFragment", "onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("PasswordsFragment", "onViewCreated")

        setupRecyclerView() // Сначала настраиваем RecyclerView и адаптер
        setupFilterSpinner()
        setupFab()
        observeViewModel() // Затем начинаем наблюдать за данными
    }

    // Наблюдение за изменениями данных в ViewModel
    private fun observeViewModel() {
        // Наблюдаем за списком паролей для отображения
        passwordViewModel.displayedPasswords.observe(viewLifecycleOwner) { passwords ->
            Log.d("PasswordsFragment", "Password list updated, size: ${passwords?.size ?: 0}")
            passwordAdapter.submitList(passwords)
            // Опционально: показать/скрыть заглушку для пустого списка
            // binding.emptyView.visibility = if (passwords.isNullOrEmpty()) View.VISIBLE else View.GONE
        }

        // Наблюдаем за текущим фильтром категории для обновления Spinner
        passwordViewModel.currentCategoryFilter.observe(viewLifecycleOwner) { filter ->
            Log.d("PasswordsFragment", "Category filter changed to: $filter")
            // Используем сохраненную ссылку на адаптер или безопасное приведение типа
            val position = spinnerAdapter?.getPosition(filter) ?: (binding.spinnerCategoryFilter.adapter as? ArrayAdapter<String>)?.getPosition(filter) ?: 0
            // Устанавливаем выбранный элемент в Spinner без вызова onItemSelectedListener
            if (binding.spinnerCategoryFilter.selectedItemPosition != position) {
                binding.spinnerCategoryFilter.setSelection(position, false)
            }
        }
    }

    // Настройка RecyclerView и его адаптера
    private fun setupRecyclerView() {
        // Создаем адаптер, передавая ViewModel и обработчик клика
        passwordAdapter = PasswordAdapter(passwordViewModel) { clickedPassword ->
            Log.d("PasswordsFragment", "Item clicked: ${clickedPassword.title}")
            // Показываем панель деталей при клике
            showDetailPanel(clickedPassword)
        }

        binding.passwordRecyclerView.apply {
            adapter = passwordAdapter
            layoutManager = LinearLayoutManager(requireContext()) // Используем requireContext()
            // Прикрепляем обработчик свайпов для удаления
            attachSwipeToDelete(this)
        }
        Log.d("PasswordsFragment", "RecyclerView setup complete")
    }

    // Показ панели с деталями пароля
    private fun showDetailPanel(passwordData: PasswordEntity) {
        // Используем childFragmentManager, так как контейнер находится внутри этого фрагмента
        val fragmentManager = childFragmentManager
        val transaction = fragmentManager.beginTransaction()

        // Создаем экземпляр фрагмента деталей, передавая данные
        val detailFragment = PasswordDetailPanelFragment.newInstance(
            title = passwordData.title,
            username = passwordData.username,
            // Передаем дешифрованный пароль (он уже такой в displayedPasswords)
            decryptedPassword = passwordData.encryptedPassword,
            category = passwordData.category,
            lastModified = passwordData.lastModified
        )

        transaction.setCustomAnimations(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )

        // Заменяем содержимое контейнера R.id.detailPanelContainer
        transaction.replace(R.id.detailPanelContainer, detailFragment, "detail_panel") // Используем тег

        // Не добавляем в Back Stack, закрытие через кнопку внутри панели
        transaction.commit()
        Log.d("PasswordsFragment", "Detail panel shown for ${passwordData.title}")
    }


    // Настройка обработчика свайпов для удаления
    private fun attachSwipeToDelete(recyclerView: RecyclerView) {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, // dragDirs: не поддерживаем перетаскивание
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT // swipeDirs: влево и вправо
        ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false // Не нужно для свайпа
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition // Более надежный способ получить позицию
                // Проверяем, что позиция валидна
                if (position != RecyclerView.NO_POSITION) {
                    try {
                        val passwordToDelete = passwordAdapter.currentList[position]
                        Log.d(
                            "PasswordsFragment",
                            "Swiped item at position $position, ID: ${passwordToDelete.id}"
                        )

                        // Выполняем мягкое удаление через ViewModel
                        passwordViewModel.softDeletePassword(passwordToDelete.id)

                        // Закрываем панель деталей, если она показывала удаленный элемент
                        closeDetailPanelIfShowing()

                        // Показываем Snackbar с возможностью отмены (восстановления)
                        Snackbar.make(
                            binding.root,
                            R.string.password_moved_to_trash,
                            Snackbar.LENGTH_LONG
                        )
                            .setAction(R.string.undo) {
                                Log.d(
                                    "PasswordsFragment",
                                    "Undo delete for ID: ${passwordToDelete.id}"
                                )
                                // Восстанавливаем через ViewModel
                                passwordViewModel.restorePassword(passwordToDelete.id)
                            }
                            .show()
                    } catch (e: IndexOutOfBoundsException) {
                        // Эта ошибка может возникнуть, если список обновляется во время свайпа
                        Log.e(
                            "PasswordsFragment",
                            "Error getting item for swipe delete at position $position",
                            e
                        )
                    } catch (e: Exception) {
                        Log.e(
                            "PasswordsFragment",
                            "Unexpected error during swipe delete at position $position",
                            e
                        )
                    }
                } else {
                    Log.w("PasswordsFragment", "Swiped item at invalid position: NO_POSITION")
                }
            }
        }
        // Прикрепляем helper к RecyclerView
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)
        Log.d("PasswordsFragment", "Swipe to delete attached")
    }

    // Метод для закрытия панели деталей, если она показана
    private fun closeDetailPanelIfShowing() {
        // Используем childFragmentManager, так как панель находится внутри этого фрагмента
        val existingDetailFragment = childFragmentManager.findFragmentByTag("detail_panel") as? PasswordDetailPanelFragment
        // Если фрагмент найден, вызываем его метод закрытия
        existingDetailFragment?.let {
            Log.d("PasswordsFragment", "Closing existing detail panel fragment")
            it.closeFragment() // Вызываем публичный метод дочернего фрагмента
        }
    }

    // Настройка Spinner для фильтрации категорий
    private fun setupFilterSpinner() {
        val categories = passwordViewModel.categories
        // Сохраняем ссылку на адаптер для возможного использования в observeViewModel
        spinnerAdapter =
            ArrayAdapter(requireContext(), R.layout.simple_spinner_item_custom, categories)
        spinnerAdapter?.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_custom)
        binding.spinnerCategoryFilter.adapter = spinnerAdapter

        // Устанавливаем слушатель выбора элемента
        binding.spinnerCategoryFilter.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                Log.d("PasswordsFragment", "Spinner item selected: $selectedCategory")
                // Обновляем фильтр в ViewModel, только если он изменился
                if (passwordViewModel.currentCategoryFilter.value != selectedCategory) {
                    passwordViewModel.setCategoryFilter(selectedCategory)
                    closeDetailPanelIfShowing() // Закрываем панель при смене фильтра
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d("PasswordsFragment", "Spinner nothing selected")
                // Можно также закрыть панель, если нужно
                // closeDetailPanelIfShowing()
            }
        }
        Log.d("PasswordsFragment", "Filter spinner setup complete")
    }

    // Настройка FloatingActionButton для добавления пароля
    private fun setupFab() {
        binding.fabAddPassword.setOnClickListener {
            Log.d("PasswordsFragment", "FAB clicked")
            closeDetailPanelIfShowing() // Закрываем панель перед открытием диалога
            // Переходим к диалогу добавления пароля через NavController
            findNavController().navigate(R.id.action_passwords_to_addPasswordDialog)
        }
        Log.d("PasswordsFragment", "FAB setup complete")
    }

    // Очистка binding при уничтожении View фрагмента
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("PasswordsFragment", "onDestroyView")
        _binding = null // Важно для избежания утечек памяти
    }
}