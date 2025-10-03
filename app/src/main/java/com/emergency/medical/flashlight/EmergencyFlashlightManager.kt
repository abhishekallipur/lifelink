package com.emergency.medical.flashlight

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EmergencyFlashlightManager(private val context: Context) {
    
    companion object {
        private const val TAG = "EmergencyFlashlight"
        private const val FLASH_ON_DURATION = 300L  // Flash on for 300ms
        private const val FLASH_OFF_DURATION = 200L  // Flash off for 200ms
    }
    
    private val cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null
    private var flashJob: Job? = null
    private var isFlashing = false
    
    interface FlashlightCallback {
        fun onFlashStarted()
        fun onFlashStopped(reason: String)
        fun onFlashCycle(isOn: Boolean)
        fun onFlashError(error: String)
    }
    
    private var callback: FlashlightCallback? = null
    
    init {
        initializeCamera()
    }
    
    fun setCallback(callback: FlashlightCallback) {
        this.callback = callback
    }
    
    private fun initializeCamera() {
        try {
            val cameraIds = cameraManager.cameraIdList
            for (id in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)
                if (hasFlash == true) {
                    cameraId = id
                    Log.d(TAG, "Found camera with flash: $id")
                    break
                }
            }
            if (cameraId == null) {
                Log.w(TAG, "No camera with flash found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing camera", e)
        }
    }
    
    fun startFlashing() {
        if (isFlashing) {
            Log.w(TAG, "Flashlight already flashing")
            return
        }
        
        if (cameraId == null) {
            val error = "No flashlight available on this device"
            Log.e(TAG, error)
            callback?.onFlashError(error)
            return
        }
        
        isFlashing = true
        Log.i(TAG, "Starting emergency flashlight")
        callback?.onFlashStarted()
        
        flashJob = CoroutineScope(Dispatchers.IO).launch {
            flashLoop()
        }
    }
    
    private suspend fun flashLoop() {
        while (isFlashing) {
            try {
                // Turn flash ON
                setFlashlight(true)
                callback?.onFlashCycle(true)
                delay(FLASH_ON_DURATION)
                
                if (!isFlashing) break
                
                // Turn flash OFF
                setFlashlight(false)
                callback?.onFlashCycle(false)
                delay(FLASH_OFF_DURATION)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in flash loop", e)
                callback?.onFlashError("Flash error: ${e.message}")
                break
            }
        }
        
        // Ensure flash is off when stopping
        try {
            setFlashlight(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error turning off flash", e)
        }
    }
    
    private fun setFlashlight(on: Boolean) {
        try {
            cameraId?.let { id ->
                cameraManager.setTorchMode(id, on)
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Camera access exception", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error setting flashlight", e)
            throw e
        }
    }
    
    fun stopFlashing(reason: String = "Manual stop") {
        if (!isFlashing) {
            return
        }
        
        isFlashing = false
        flashJob?.cancel()
        
        try {
            setFlashlight(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error turning off flashlight", e)
        }
        
        Log.i(TAG, "Emergency flashlight stopped: $reason")
        callback?.onFlashStopped(reason)
    }
    
    fun toggleFlashing() {
        if (isFlashing) {
            stopFlashing("Manual toggle")
        } else {
            startFlashing()
        }
    }
    
    fun isFlashing(): Boolean = isFlashing
    
    fun isFlashlightAvailable(): Boolean = cameraId != null
    
    fun release() {
        stopFlashing("Manager released")
        callback = null
    }
}
