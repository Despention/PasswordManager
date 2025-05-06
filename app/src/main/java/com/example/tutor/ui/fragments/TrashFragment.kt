package com.example.tutor.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tutor.R
import com.example.tutor.databinding.FragmentTrashBinding
import com.example.tutor.ui.adapters.PasswordAdapter
import com.example.tutor.viewmodels.PasswordViewModel
import com.google.android.material.snackbar.Snackbar

class TrashFragment : Fragment() {

    private var _binding: FragmentTrashBinding? = null
    private val binding get() = _binding!!
    private val passwordViewModel: PasswordViewModel by activityViewModels()
    private lateinit var trashAdapter: PasswordAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbarTrash.setNavigationOnClickListener {
            // Возвращаемся назад по стеку навигации
            findNavController().navigateUp()
        }
        binding.toolbarTrash.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_empty_trash -> {
                    showConfirmEmptyTrashDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        // Создаем адаптер, но клик пока не обрабатываем (или можно добавить диалог Restore/Delete)
        trashAdapter = PasswordAdapter(passwordViewModel) { clickedItem ->
            // Пока ничего не делаем по клику, используем свайп
            Log.d("TrashFragment", "Clicked on trashed item: ${clickedItem.title}")
            // Можно показать диалог с опциями:
            // showItemActionDialog(clickedItem.id)
        }

        binding.trashRecyclerView.apply {
            adapter = trashAdapter
            layoutManager = LinearLayoutManager(requireContext())
            attachSwipeActions(this) // Прикрепляем обработчик свайпов
        }
    }

    private fun observeViewModel() {
        passwordViewModel.displayedTrashItems.observe(viewLifecycleOwner) { trashItems ->
            Log.d("TrashFragment", "Trash items updated, size: ${trashItems?.size ?: 0}")
            trashAdapter.submitList(trashItems)
            // Показываем/скрываем текст "Корзина пуста"
            binding.tvTrashEmpty.visibility = if (trashItems.isNullOrEmpty()) View.VISIBLE else View.GONE
            // Обновляем видимость кнопки "Очистить корзину" в меню (если используется Toolbar)
            binding.toolbarTrash.menu.findItem(R.id.action_empty_trash)?.isVisible = !trashItems.isNullOrEmpty()
        }
    }

    // Настройка действий по свайпу
    private fun attachSwipeActions(recyclerView: RecyclerView) {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = trashAdapter.currentList[position]
                    if (direction == ItemTouchHelper.LEFT) {
                        // Свайп влево - Удалить навсегда
                        showConfirmDeleteItemDialog(item.id)
                        // Возвращаем элемент на место, т.к. удаление будет после подтверждения
                        trashAdapter.notifyItemChanged(position)
                    } else if (direction == ItemTouchHelper.RIGHT) {
                        // Свайп вправо - Восстановить
                        passwordViewModel.restorePassword(item.id)
                        Snackbar.make(binding.root, R.string.item_restored, Snackbar.LENGTH_SHORT).show()
                        // Адаптер обновится через LiveData
                    }
                }
            }
            // TODO: Можно добавить фон и иконки для свайпа (onChildDraw)
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)
    }

    private fun showConfirmEmptyTrashDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_empty_trash_title)
            .setMessage(R.string.confirm_empty_trash_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                passwordViewModel.emptyTrash()
                Snackbar.make(binding.root, R.string.trash_emptied, Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showConfirmDeleteItemDialog(itemId: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_delete_item_title)
            .setMessage(R.string.confirm_delete_item_message)
            .setPositiveButton(R.string.action_delete_permanently) { _, _ ->
                passwordViewModel.deletePasswordPermanently(itemId)
                Snackbar.make(
                    binding.root,
                    R.string.item_deleted_permanently,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}