
package com.emergency.medical.data

import com.google.gson.Gson
import java.util.*

data class EmergencyPayload(
    val id: Int? = null, // For DB, not sent by app
    val name: String, // fullName
    val age: Int?,
    val phone: String?,
    val bloodGroup: String?,
    val phoneBattery: Int?,
    val latitude: Double,
    val longitude: Double,
    val message: String?,
    val currentMedicalIssue: String?,
    val status: String? = "Pending",
    val priority: String? = "Medium",
    val notes: String? = null,
    val timestamp: String? = null, // ISO 8601 or server-generated
    val solvedTimestamp: String? = null, // ISO 8601 or null
    
    // Relay functionality fields - now included in JSON for proper message tracking
    val isRelay: Boolean = false,
    val relayCount: Int = 0,
    val originalSender: String = "",
    val messageId: String = java.util.UUID.randomUUID().toString()
) {
    fun toJson(): String = Gson().toJson(this)

    fun createRelayPayload(): EmergencyPayload {
        return this.copy(
            isRelay = true,
            relayCount = relayCount + 1,
            timestamp = java.time.Instant.now().toString()
        )
    }

    companion object {
        fun fromJson(json: String): EmergencyPayload? {
            return try {
                val gson = Gson()
                val payload = gson.fromJson(json, EmergencyPayload::class.java)
                
                // Handle legacy JSON that might not have messageId or originalSender
                if (payload != null) {
                    // If messageId is missing or empty, generate a new one based on the message content
                    val finalMessageId = if (payload.messageId.isBlank()) {
                        "EMG_${payload.name}_${payload.timestamp?.hashCode() ?: System.currentTimeMillis()}"
                    } else {
                        payload.messageId
                    }
                    
                    // If originalSender is missing or empty, use name and timestamp
                    val finalOriginalSender = if (payload.originalSender.isBlank()) {
                        "${payload.name}_${payload.timestamp?.take(19) ?: System.currentTimeMillis()}"
                    } else {
                        payload.originalSender
                    }
                    
                    // Return a copy with properly set fields
                    payload.copy(
                        messageId = finalMessageId,
                        originalSender = finalOriginalSender
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}

// Keep these for internal device monitoring - not sent to dashboard
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class DeviceInfo(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val deviceModel: String = android.os.Build.MODEL,
    val deviceId: String = java.util.UUID.randomUUID().toString()
)
