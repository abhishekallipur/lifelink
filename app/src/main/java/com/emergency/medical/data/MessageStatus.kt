package com.emergency.medical.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MessageStatus(
    val messageId: String,
    val timestamp: Long,
    val status: Status,
    val method: TransmissionMethod,
    val details: String,
    val retryCount: Int = 0
) {
    enum class Status {
        SENDING,
        SENT_SUCCESS,
        SENT_FAILED,
        RELAY_RECEIVED,
        RELAY_FORWARDED
    }
    
    enum class TransmissionMethod {
        INTERNET,
        BLUETOOTH,
        RELAY,
        RECEIVED,
        MULTIPLE,
        HTTP,
        WIFI_DIRECT,
        WIFI_HOTSPOT,
        OTHER
    }
    
    fun getFormattedTime(): String {
        return SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }
    
    fun getStatusText(): String {
        return when (status) {
            Status.SENDING -> "Sending..."
            Status.SENT_SUCCESS -> "Sent Successfully"
            Status.SENT_FAILED -> "Send Failed"
            Status.RELAY_RECEIVED -> "Relay Received"
            Status.RELAY_FORWARDED -> "Relay Forwarded"
        }
    }
    
    fun getMethodText(): String {
        return when (method) {
            TransmissionMethod.INTERNET -> "Internet"
            TransmissionMethod.BLUETOOTH -> "Bluetooth"
            TransmissionMethod.RELAY -> "Relay"
            TransmissionMethod.RECEIVED -> "Received"
            TransmissionMethod.MULTIPLE -> "Multi-Channel"
            TransmissionMethod.HTTP -> "HTTP"
            TransmissionMethod.WIFI_DIRECT -> "WiFi Direct"
            TransmissionMethod.WIFI_HOTSPOT -> "WiFi Hotspot"
            TransmissionMethod.OTHER -> "Other"
        }
    }
    
    fun getStatusColor(): Int {
        return when (status) {
            Status.SENDING -> android.graphics.Color.parseColor("#FF9800") // Orange
            Status.SENT_SUCCESS -> android.graphics.Color.parseColor("#4CAF50") // Green
            Status.SENT_FAILED -> android.graphics.Color.parseColor("#F44336") // Red
            Status.RELAY_RECEIVED -> android.graphics.Color.parseColor("#2196F3") // Blue
            Status.RELAY_FORWARDED -> android.graphics.Color.parseColor("#9C27B0") // Purple
        }
    }
}
