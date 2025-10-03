package com.emergency.medical.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import com.emergency.medical.flashlight.EmergencyFlashlightManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EmergencyAudioManager(private val context: Context) {
    
    companion object {
        private const val TAG = "EmergencyAudioManager"
        private const val TONE_DURATION_MS = 500L
        private const val PAUSE_DURATION_MS = 300L
        private const val MAX_CYCLES = 10
        
        // Dual-tone frequencies for emergency siren
        private const val HIGH_TONE = ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK
        private const val LOW_TONE = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
    }
    
    private var toneGenerator: ToneGenerator? = null
    private var sirenJob: Job? = null
    private var isPlaying = false
    private var cycleCount = 0
    private val flashlightManager = EmergencyFlashlightManager(context)
    
    interface AudioCallback {
        fun onSirenStarted()
        fun onSirenStopped(reason: String)
        fun onCycleCompleted(cycle: Int, totalCycles: Int)
        fun onFlashlightStarted()
        fun onFlashlightStopped(reason: String)
        fun onFlashlightError(error: String)
    }
    
    private var callback: AudioCallback? = null
    
    fun setCallback(callback: AudioCallback) {
        this.callback = callback
        
        // Setup flashlight callback
        flashlightManager.setCallback(object : EmergencyFlashlightManager.FlashlightCallback {
            override fun onFlashStarted() {
                callback.onFlashlightStarted()
            }
            
            override fun onFlashStopped(reason: String) {
                callback.onFlashlightStopped(reason)
            }
            
            override fun onFlashCycle(isOn: Boolean) {
                // Can be used for additional UI feedback if needed
            }
            
            override fun onFlashError(error: String) {
                callback.onFlashlightError(error)
            }
        })
    }
    
    fun startSiren() {
        if (isPlaying) {
            Log.w(TAG, "Siren already playing")
            return
        }
        
        try {
            // Initialize ToneGenerator with maximum volume
            toneGenerator = ToneGenerator(
                AudioManager.STREAM_ALARM, 
                ToneGenerator.MAX_VOLUME
            )
            
            isPlaying = true
            cycleCount = 0
            
            Log.i(TAG, "Starting emergency siren")
            callback?.onSirenStarted()
            
            // Start flashlight with siren
            if (flashlightManager.isFlashlightAvailable()) {
                flashlightManager.startFlashing()
                Log.i(TAG, "Started emergency flashlight")
            } else {
                Log.w(TAG, "Flashlight not available on this device")
                callback?.onFlashlightError("Flashlight not available")
            }
            
            sirenJob = CoroutineScope(Dispatchers.IO).launch {
                playSirenCycles()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting siren", e)
            stopSiren("Error: ${e.message}")
        }
    }
    
    private suspend fun playSirenCycles() {
        while (isPlaying && cycleCount < MAX_CYCLES) {
            cycleCount++
            
            try {
                // High tone
                toneGenerator?.startTone(HIGH_TONE, TONE_DURATION_MS.toInt())
                delay(TONE_DURATION_MS)
                
                if (!isPlaying) break
                
                // Pause
                delay(PAUSE_DURATION_MS)
                
                if (!isPlaying) break
                
                // Low tone
                toneGenerator?.startTone(LOW_TONE, TONE_DURATION_MS.toInt())
                delay(TONE_DURATION_MS)
                
                if (!isPlaying) break
                
                // Pause between cycles
                delay(PAUSE_DURATION_MS)
                
                Log.d(TAG, "Completed siren cycle $cycleCount of $MAX_CYCLES")
                callback?.onCycleCompleted(cycleCount, MAX_CYCLES)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during siren cycle $cycleCount", e)
                break
            }
        }
        
        if (cycleCount >= MAX_CYCLES) {
            Log.i(TAG, "Siren completed maximum cycles, auto-stopping")
            stopSiren("Completed $MAX_CYCLES cycles")
        }
    }
    
    fun stopSiren(reason: String = "Manual stop") {
        if (!isPlaying) {
            return
        }
        
        isPlaying = false
        sirenJob?.cancel()
        
        // Stop flashlight with siren
        flashlightManager.stopFlashing("Siren stopped: $reason")
        
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping tone generator", e)
        }
        
        Log.i(TAG, "Emergency siren stopped: $reason")
        callback?.onSirenStopped(reason)
    }
    
    fun toggleSiren() {
        if (isPlaying) {
            stopSiren("Manual toggle")
        } else {
            startSiren()
        }
    }
    
    fun isPlaying(): Boolean = isPlaying
    
    fun getCurrentCycle(): Int = cycleCount
    
    fun getRemainingCycles(): Int = MAX_CYCLES - cycleCount
    
    private fun setAlarmVolume() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
            Log.d(TAG, "Set alarm volume to maximum")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting alarm volume", e)
        }
    }
    
    fun release() {
        stopSiren("Manager released")
        flashlightManager.release()
        callback = null
    }
}
