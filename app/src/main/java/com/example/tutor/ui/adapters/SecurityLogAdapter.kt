package com.example.tutor.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tutor.databinding.ItemSecurityLogBinding
import com.example.tutor.db.SecurityLogEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SecurityLogAdapter : ListAdapter<SecurityLogEvent, SecurityLogAdapter.LogViewHolder>(LogDiffCallback()) {

    private val timestampFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemSecurityLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding, timestampFormat)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LogViewHolder(
        private val binding: ItemSecurityLogBinding,
        private val formatter: SimpleDateFormat
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(logEvent: SecurityLogEvent) {
            binding.tvLogEventType.text = logEvent.eventType

            if (!logEvent.description.isNullOrBlank()) {
                binding.tvLogDescription.text = logEvent.description
                binding.tvLogDescription.visibility = View.VISIBLE
            } else {
                binding.tvLogDescription.visibility = View.GONE
            }

            try {
                binding.tvLogTimestamp.text = formatter.format(Date(logEvent.timestamp))
            } catch (e: Exception) {
                binding.tvLogTimestamp.text = "Invalid Timestamp"
            }
        }
    }

    class LogDiffCallback : DiffUtil.ItemCallback<SecurityLogEvent>() {
        override fun areItemsTheSame(oldItem: SecurityLogEvent, newItem: SecurityLogEvent): Boolean {
            return oldItem.eventId == newItem.eventId
        }

        override fun areContentsTheSame(oldItem: SecurityLogEvent, newItem: SecurityLogEvent): Boolean {
            return oldItem == newItem
        }
    }
} 