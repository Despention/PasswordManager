package com.example.tutor.ui.fragments

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tutor.R
import com.example.tutor.databinding.FragmentPasswordDetailPanelBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PasswordDetailPanelFragment : Fragment() {

    private var _binding: FragmentPasswordDetailPanelBinding? = null
    private val binding get() = _binding!!

    private var isPasswordVisible = false

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_USERNAME = "arg_username"
        private const val ARG_PASSWORD = "arg_password"
        private const val ARG_CATEGORY = "arg_category"
        private const val ARG_LAST_MODIFIED = "arg_last_modified"

        fun newInstance(
            title: String,
            username: String,
            decryptedPassword: String,
            category: String,
            lastModified: Long
        ): PasswordDetailPanelFragment {
            val fragment = PasswordDetailPanelFragment()
            val args = Bundle().apply {
                putString(ARG_TITLE, title)
                putString(ARG_USERNAME, username)
                putString(ARG_PASSWORD, decryptedPassword)
                putString(ARG_CATEGORY, category)
                putLong(ARG_LAST_MODIFIED, lastModified)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasswordDetailPanelBinding.inflate(inflater, container, false)
        Log.d("DetailPanel", "onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("DetailPanel", "onViewCreated")

        // Применяем начальное состояние видимости (скрыто)
        setPasswordVisibility(false)

        arguments?.let {
            binding.tvDetailTitle.text = it.getString(ARG_TITLE, "No Title")
            binding.tvDetailUsername.text = it.getString(ARG_USERNAME, "No Username")
            binding.tvDetailPassword.text = it.getString(ARG_PASSWORD, "No Password")
            binding.tvDetailCategory.text = it.getString(ARG_CATEGORY, "No Category")

            val lastModifiedTimestamp = it.getLong(ARG_LAST_MODIFIED, 0)
            if (lastModifiedTimestamp > 0) {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                try {
                    binding.tvDetailLastModified.text = dateFormat.format(Date(lastModifiedTimestamp))
                    binding.tvDetailLastModified.visibility = View.VISIBLE
                } catch (e: Exception) {
                    Log.e("DetailPanel", "Error formatting date", e)
                    binding.tvDetailLastModified.text = "-"
                    binding.tvDetailLastModified.visibility = View.VISIBLE
                }
            } else {
                binding.tvDetailLastModified.text = "-"
                binding.tvDetailLastModified.visibility = View.VISIBLE
            }
        }

        binding.btnCloseDetail.setOnClickListener {
            Log.d("DetailPanel", "Close button clicked")
            closeFragment()
        }

        binding.btnToggleDetailPasswordVisibility.setOnClickListener {
            setPasswordVisibility(!isPasswordVisible)
        }
    }

    private fun setPasswordVisibility(show: Boolean) {
        isPasswordVisible = show
        if (show) {
            binding.tvDetailPassword.transformationMethod = null
            binding.btnToggleDetailPasswordVisibility.setImageResource(R.drawable.ic_visibility_off)
        } else {
            binding.tvDetailPassword.transformationMethod =
                PasswordTransformationMethod.getInstance()
            binding.btnToggleDetailPasswordVisibility.setImageResource(R.drawable.ic_visibility_on)
        }
    }

    fun closeFragment() {
        Log.d("DetailPanel", "closeFragment called")
        if (!isAdded || parentFragmentManager.isStateSaved) {
            Log.w(
                "DetailPanel",
                "closeFragment called but fragment not added or state saved. Aborting."
            )
            return
        }
        try {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(0, android.R.anim.fade_out)
                .remove(this)
                .commit()
            Log.d("DetailPanel", "Fragment removal transaction committed")
        } catch (e: IllegalStateException) {
            Log.e("DetailPanel", "Error committing fragment removal transaction", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("DetailPanel", "onDestroyView")
        _binding = null
    }
}