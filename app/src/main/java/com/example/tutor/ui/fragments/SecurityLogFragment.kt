package com.example.tutor.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tutor.R
import com.example.tutor.databinding.FragmentSecurityLogBinding
import com.example.tutor.viewmodels.PasswordViewModel
import com.example.tutor.ui.adapters.SecurityLogAdapter
import com.google.android.material.snackbar.Snackbar


class SecurityLogFragment : Fragment() {

    private var _binding: FragmentSecurityLogBinding? = null
    private val binding get() = _binding!!
    private val passwordViewModel: PasswordViewModel by activityViewModels()
    private lateinit var logAdapter: SecurityLogAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecurityLogBinding.inflate(inflater, container, false)
        Log.d("SecurityLogFragment", "onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("SecurityLogFragment", "onViewCreated")

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbarSecurityLog.setNavigationOnClickListener {
            Log.d("SecurityLogFragment", "Navigation Up clicked")
            findNavController().navigateUp()
        }
        binding.toolbarSecurityLog.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_clear_log -> {
                    Log.d("SecurityLogFragment", "Clear Log menu item clicked")
                    showConfirmClearLogDialog()
                    true
                }
                else -> false
            }
        }
        Log.d("SecurityLogFragment", "Toolbar setup complete")
    }

    private fun setupRecyclerView() {
        logAdapter = SecurityLogAdapter()
        binding.securityLogRecyclerView.apply {
            adapter = logAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
        Log.d("SecurityLogFragment", "RecyclerView setup complete")
    }

    private fun observeViewModel() {
        passwordViewModel.securityLog.observe(viewLifecycleOwner) { events ->
            val eventCount = events?.size ?: 0
            Log.d("SecurityLogFragment", "Security log updated, count: $eventCount")
            logAdapter.submitList(events)

            binding.tvLogEmpty.visibility = if (eventCount == 0) View.VISIBLE else View.GONE
            binding.securityLogRecyclerView.visibility = if (eventCount > 0) View.VISIBLE else View.GONE

            binding.toolbarSecurityLog.menu.findItem(R.id.action_clear_log)?.isVisible = eventCount > 0
            Log.d("SecurityLogFragment", "Clear Log menu visibility set to: ${eventCount > 0}")
        }
    }

    private fun showConfirmClearLogDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_clear_log_title)
            .setMessage(R.string.confirm_clear_log_message)
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton(R.string.action_clear_log_confirm) { _, _ ->
                Log.d("SecurityLogFragment", "Confirmed clear log")
                passwordViewModel.clearSecurityLog()
                Snackbar.make(binding.root, R.string.security_log_cleared, Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("SecurityLogFragment", "onDestroyView")
        binding.securityLogRecyclerView.adapter = null
        _binding = null
    }
} 