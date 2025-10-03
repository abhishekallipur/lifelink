package com.emergency.medical.data

import android.content.SharedPreferences
import com.emergency.medical.EmergencyApplication
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MessageStatusManager {
    
    companion object {
        private const val KEY_MESSAGE_HISTORY = "message_history"
        private const val MAX_HISTORY_SIZE = 50
        
        @Volatile
        private var INSTANCE: MessageStatusManager? = null
        
        fun getInstance(): MessageStatusManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MessageStatusManager().also { INSTANCE = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = EmergencyApplication.getSecuredPreferences()
    private val gson = Gson()
    private val messageHistory = mutableListOf<MessageStatus>()
    
    interface StatusUpdateListener {
        fun onStatusUpdated(messageHistory: List<MessageStatus>)
        fun onNewMessageAdded(messageStatus: MessageStatus)
    }
    
    private val listeners = mutableSetOf<StatusUpdateListener>()
    
    init {
        loadMessageHistory()
    }
    
    fun addListener(listener: StatusUpdateListener) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: StatusUpdateListener) {
        listeners.remove(listener)
    }
    
    fun addMessageStatus(messageStatus: MessageStatus) {
        messageHistory.add(0, messageStatus) // Add to beginning
        
        // Keep only the latest MAX_HISTORY_SIZE messages
        if (messageHistory.size > MAX_HISTORY_SIZE) {
            messageHistory.removeAt(messageHistory.size - 1)
        }
        
        saveMessageHistory()
        notifyListeners()
        notifyNewMessage(messageStatus)
    }
    
    fun updateMessageStatus(messageId: String, newStatus: MessageStatus.Status, details: String, retryCount: Int = 0) {
        val index = messageHistory.indexOfFirst { it.messageId == messageId }
        if (index != -1) {
            val updatedMessage = messageHistory[index].copy(
                status = newStatus,
                details = details,
                retryCount = retryCount
            )
            messageHistory[index] = updatedMessage
            saveMessageHistory()
            notifyListeners()
        }
    }
    
    fun getMessageHistory(): List<MessageStatus> {
        return messageHistory.toList()
    }
    
    fun getCurrentStatus(): String {
        if (messageHistory.isEmpty()) {
            return "No messages sent"
        }
        
        val latestMessage = messageHistory.first()
        val totalMessages = messageHistory.size
        val successCount = messageHistory.count { it.status == MessageStatus.Status.SENT_SUCCESS }
        val failedCount = messageHistory.count { it.status == MessageStatus.Status.SENT_FAILED }
        val relayCount = messageHistory.count { it.status == MessageStatus.Status.RELAY_RECEIVED || it.status == MessageStatus.Status.RELAY_FORWARDED }
        
        return when {
            latestMessage.status == MessageStatus.Status.SENDING -> "Sending message..."
            successCount == totalMessages -> "$totalMessages messages sent successfully"
            failedCount > 0 && relayCount == 0 -> "$successCount sent, $failedCount failed"
            relayCount > 0 -> "$successCount sent, $relayCount relayed"
            else -> "$totalMessages messages in history"
        }
    }
    
    fun getOverallStatus(): MessageStatus.Status {
        if (messageHistory.isEmpty()) {
            return MessageStatus.Status.SENT_SUCCESS // Default ready state
        }
        
        val latestMessage = messageHistory.first()
        return latestMessage.status
    }
    
    fun clearHistory() {
        messageHistory.clear()
        saveMessageHistory()
        notifyListeners()
    }
    
    private fun loadMessageHistory() {
        val json = prefs.getString(KEY_MESSAGE_HISTORY, null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<MessageStatus>>() {}.type
                val loadedHistory: List<MessageStatus> = gson.fromJson(json, type)
                messageHistory.addAll(loadedHistory)
            } catch (e: Exception) {
                // If loading fails, start with empty history
                messageHistory.clear()
            }
        }
    }
    
    private fun saveMessageHistory() {
        val json = gson.toJson(messageHistory)
        prefs.edit().putString(KEY_MESSAGE_HISTORY, json).apply()
    }
    
    private fun notifyListeners() {
        listeners.forEach { it.onStatusUpdated(messageHistory.toList()) }
    }
    
    private fun notifyNewMessage(messageStatus: MessageStatus) {
        listeners.forEach { it.onNewMessageAdded(messageStatus) }
    }
}
