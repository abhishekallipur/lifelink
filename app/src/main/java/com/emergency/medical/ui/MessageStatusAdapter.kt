package com.emergency.medical.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.emergency.medical.R
import com.emergency.medical.data.MessageStatus

class MessageStatusAdapter : ListAdapter<MessageStatus, MessageStatusAdapter.MessageStatusViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageStatusViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message_status, parent, false)
        return MessageStatusViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageStatusViewHolder, position: Int) {
        val messageStatus = getItem(position)
        holder.bind(messageStatus)
    }

    class MessageStatusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val statusIndicator: View = itemView.findViewById(R.id.statusIndicator)
        private val messageTime: TextView = itemView.findViewById(R.id.messageTime)
        private val statusText: TextView = itemView.findViewById(R.id.statusText)
        private val methodBadge: TextView = itemView.findViewById(R.id.methodBadge)
        private val detailsText: TextView = itemView.findViewById(R.id.detailsText)
        private val retryCount: TextView = itemView.findViewById(R.id.retryCount)

        fun bind(messageStatus: MessageStatus) {
            // Set status indicator color
            statusIndicator.setBackgroundColor(messageStatus.getStatusColor())
            
            // Set time
            messageTime.text = messageStatus.getFormattedTime()
            
            // Set status text and color
            statusText.text = messageStatus.getStatusText()
            statusText.setTextColor(messageStatus.getStatusColor())
            
            // Set method badge
            methodBadge.text = messageStatus.getMethodText()
            
            // Set details
            detailsText.text = messageStatus.details
            
            // Show retry count if applicable
            if (messageStatus.retryCount > 0) {
                retryCount.visibility = View.VISIBLE
                retryCount.text = "Retries: ${messageStatus.retryCount}"
            } else {
                retryCount.visibility = View.GONE
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<MessageStatus>() {
            override fun areItemsTheSame(oldItem: MessageStatus, newItem: MessageStatus): Boolean {
                return oldItem.messageId == newItem.messageId
            }

            override fun areContentsTheSame(oldItem: MessageStatus, newItem: MessageStatus): Boolean {
                return oldItem == newItem
            }
        }
    }
}
