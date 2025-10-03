package com.emergency.medical.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.emergency.medical.MainActivityMinimal
import com.emergency.medical.R
import com.emergency.medical.data.EmergencyPayload

class EmergencyNotificationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "EmergencyNotifications"
        private const val CHANNEL_ID = "EMERGENCY_CHANNEL"
        private const val NOTIFICATION_ID = 1001
        private const val RECEIVED_NOTIFICATION_ID = 1002
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Emergency Alerts"
            val descriptionText = "Critical emergency notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showEmergencyReceivedNotification(payload: EmergencyPayload) {
        try {
            Log.i(TAG, "Showing emergency received notification")
            
            val intent = Intent(context, MainActivityMinimal::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val locationText = if (payload.latitude != null && payload.longitude != null) { 
                "Location: ${String.format("%.6f", payload.latitude)}, ${String.format("%.6f", payload.longitude)}"
            } else "Location: Not available"
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_emergency)
                .setContentTitle("🚨 EMERGENCY RECEIVED")
                .setContentText("Emergency from ${payload.name}")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("🚨 EMERGENCY ALERT 🚨\n\n" +
                        "From: ${payload.name}\n" +
                        "Age: ${payload.age ?: "Unknown"}\n" +
                        "Blood: ${payload.bloodGroup ?: "Unknown"}\n" +
                        "Contact: ${payload.phone ?: "No contact"}\n\n" +
                        locationText + "\n\n" +
                        "Medical Conditions: ${payload.currentMedicalIssue ?: "None specified"}\n\n" +
                        "Received: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(false) // Keep notification visible
                .setOngoing(false)
                .setContentIntent(pendingIntent)
                .setColor(context.getColor(R.color.emergency_red))
                .build()
            
            notificationManager.notify(RECEIVED_NOTIFICATION_ID, notification)
            
            // Add vibration and sound
            vibrateEmergencyPattern()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing emergency notification", e)
        }
    }
    
    fun showEmergencyForwardedNotification(payload: EmergencyPayload) {
        try {
            Log.i(TAG, "Showing emergency forwarded notification")
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_emergency)
                .setContentTitle("📡 Emergency Forwarded")
                .setContentText("Emergency from ${payload.name} forwarded to dashboard")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setColor(context.getColor(R.color.status_ready))
                .build()
            
            notificationManager.notify(RECEIVED_NOTIFICATION_ID + 1, notification)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing forwarded notification", e)
        }
    }
    
    private fun vibrateEmergencyPattern() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
            
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Emergency pattern: Long-Short-Short-Long-Long
                    val pattern = longArrayOf(0, 800, 200, 300, 200, 300, 200, 800, 200, 800)
                    val effect = VibrationEffect.createWaveform(pattern, -1)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    val pattern = longArrayOf(0, 800, 200, 300, 200, 300, 200, 800, 200, 800)
                    vibrator.vibrate(pattern, -1)
                }
                Log.d(TAG, "Emergency vibration pattern activated")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating vibration", e)
        }
    }
    
    fun dismissAllNotifications() {
        try {
            notificationManager.cancelAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing notifications", e)
        }
    }
}
