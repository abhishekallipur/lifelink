package com.emergency.medical.transmission

import android.content.Context
import android.util.Log
import com.emergency.medical.bluetooth.BluetoothDiscoveryManager
import com.emergency.medical.bluetooth.manager.BluetoothRelayManager
import com.emergency.medical.data.EmergencyPayload
import com.emergency.medical.wifi.WiFiDirectManager
import com.emergency.medical.wifi.WiFiHotspotManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class MultiChannelTransmissionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "MultiTransmission"
        private const val HTTP_TIMEOUT = 10000L
    }
    
    private val bluetoothRelayManager = BluetoothRelayManager(context)
    private val bluetoothDiscoveryManager = BluetoothDiscoveryManager(context)
    private val wifiDirectManager = WiFiDirectManager(context)
    private val wifiHotspotManager = WiFiHotspotManager(context)
    
    private var transmissionJob: Job? = null
    
    interface MultiChannelCallback {
        fun onTransmissionStarted(channels: List<String>)
        fun onChannelProgress(channel: String, status: String)
        fun onTransmissionComplete(results: TransmissionResults)
        fun onError(error: String)
    }
    
    data class TransmissionResults(
        val httpSuccessCount: Int,
        val bluetoothPairedCount: Int,
        val bluetoothDiscoveredCount: Int,
        val wifiDirectCount: Int,
        val hotspotActive: Boolean,
        val totalReached: Int,
        val channels: List<ChannelResult>
    )
    
    data class ChannelResult(
        val channel: String,
        val success: Boolean,
        val count: Int,
        val message: String
    )
    
    private var callback: MultiChannelCallback? = null
    
    fun setCallback(callback: MultiChannelCallback) {
        this.callback = callback
    }
    
    fun startMultiChannelBroadcast(payload: EmergencyPayload) {
        transmissionJob = CoroutineScope(Dispatchers.IO).launch {
            val results = mutableListOf<ChannelResult>()
            val activeChannels = mutableListOf<String>()
            
            try {
                Log.i(TAG, "🚨 Starting multi-channel emergency broadcast")
                
                // Determine available channels
                val channels = getAvailableChannels()
                activeChannels.addAll(channels)
                callback?.onTransmissionStarted(activeChannels)
                
                // Launch all transmission methods simultaneously
                val jobs = mutableListOf<Job>()
                
                // 1. HTTP to emergency services (if configured)
                if (channels.contains("HTTP")) {
                    jobs.add(launch {
                        val result = broadcastViaHTTP(payload)
                        results.add(result)
                        callback?.onChannelProgress("HTTP", result.message)
                    })
                }
                
                // 2. Bluetooth to paired devices
                if (channels.contains("Bluetooth-Paired")) {
                    jobs.add(launch {
                        val result = broadcastViaBluetooth(payload)
                        results.add(result)
                        callback?.onChannelProgress("Bluetooth-Paired", result.message)
                    })
                }
                
                // 3. Bluetooth discovery and broadcast
                if (channels.contains("Bluetooth-Discovery")) {
                    jobs.add(launch {
                        val result = broadcastViaBluetoothDiscovery(payload)
                        results.add(result)
                        callback?.onChannelProgress("Bluetooth-Discovery", result.message)
                    })
                }
                
                // 4. WiFi Direct
                if (channels.contains("WiFi-Direct")) {
                    jobs.add(launch {
                        val result = broadcastViaWiFiDirect(payload)
                        results.add(result)
                        callback?.onChannelProgress("WiFi-Direct", result.message)
                    })
                }
                
                // 5. WiFi Hotspot
                if (channels.contains("WiFi-Hotspot")) {
                    jobs.add(launch {
                        val result = broadcastViaWiFiHotspot(payload)
                        results.add(result)
                        callback?.onChannelProgress("WiFi-Hotspot", result.message)
                    })
                }
                
                // Wait for all transmission attempts to complete
                jobs.forEach { it.join() }
                
                // Calculate final results
                val finalResults = calculateResults(results)
                Log.i(TAG, "📊 Multi-channel broadcast complete: ${finalResults.totalReached} devices reached")
                callback?.onTransmissionComplete(finalResults)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during multi-channel broadcast", e)
                callback?.onError("Multi-channel broadcast failed: ${e.message}")
            }
        }
    }
    
    private fun getAvailableChannels(): List<String> {
        val channels = mutableListOf<String>()
        
        // HTTP is always available (internet/cellular)
        channels.add("HTTP")
        
        // Check Bluetooth availability
        try {
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                channels.add("Bluetooth-Paired")
                channels.add("Bluetooth-Discovery")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Bluetooth not available", e)
        }
        
        // Check WiFi availability
        try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            if (wifiManager.isWifiEnabled || wifiManager.isWifiEnabled) {
                channels.add("WiFi-Direct")
                channels.add("WiFi-Hotspot")
            }
        } catch (e: Exception) {
            Log.w(TAG, "WiFi not available", e)
        }
        
        Log.i(TAG, "📡 Available channels: ${channels.joinToString(", ")}")
        return channels
    }
    
    private suspend fun broadcastViaHTTP(payload: EmergencyPayload): ChannelResult {
        return try {
            Log.d(TAG, "🌐 Broadcasting via HTTP...")
            
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            
            val jsonData = payload.toJson()
            val requestBody = jsonData.toRequestBody("application/json".toMediaType())
            
            // Try multiple emergency service endpoints
            val endpoints = listOf(
                "https://emergency-relay-service.herokuapp.com/api/emergency",
                "https://httpbin.org/post" // Test endpoint
            )
            
            var successCount = 0
            for (endpoint in endpoints) {
                try {
                    val request = Request.Builder()
                        .url(endpoint)
                        .post(requestBody)
                        .build()
                    
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        successCount++
                        Log.d(TAG, "✅ HTTP sent to: $endpoint")
                    } else {
                        Log.w(TAG, "❌ HTTP failed to: $endpoint (${response.code})")
                    }
                    response.close()
                    
                    delay(1000) // Delay between requests
                } catch (e: Exception) {
                    Log.w(TAG, "❌ HTTP error to $endpoint: ${e.message}")
                }
            }
            
            ChannelResult(
                channel = "HTTP",
                success = successCount > 0,
                count = successCount,
                message = if (successCount > 0) "✅ Sent to $successCount HTTP endpoints" else "❌ HTTP transmission failed"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "HTTP broadcast error", e)
            ChannelResult("HTTP", false, 0, "❌ HTTP error: ${e.message}")
        }
    }
    
    private suspend fun broadcastViaBluetooth(payload: EmergencyPayload): ChannelResult {
        return try {
            Log.d(TAG, "📱 Broadcasting via Bluetooth to paired devices...")
            
            // Simple approach - just call broadcast and wait
            bluetoothRelayManager.broadcastEmergency(payload)
            delay(15000) // Wait for Bluetooth transmission to complete
            
            // For now, assume success if no exception thrown
            ChannelResult(
                channel = "Bluetooth-Paired",
                success = true,
                count = 1, // Simplified - actual count would need callback integration
                message = "✅ Bluetooth broadcast attempted"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Bluetooth broadcast error", e)
            ChannelResult("Bluetooth-Paired", false, 0, "❌ Bluetooth error: ${e.message}")
        }
    }
    
    private suspend fun broadcastViaBluetoothDiscovery(payload: EmergencyPayload): ChannelResult {
        return try {
            Log.d(TAG, "🔍 Broadcasting via Bluetooth discovery...")
            
            // Use discovery manager
            bluetoothDiscoveryManager.startDiscoveryAndBroadcast(payload)
            delay(20000) // Wait for discovery and transmission
            
            val devicesFound = bluetoothDiscoveryManager.getDiscoveredDevicesCount()
            
            ChannelResult(
                channel = "Bluetooth-Discovery",
                success = devicesFound > 0,
                count = devicesFound,
                message = if (devicesFound > 0) "✅ Found $devicesFound nearby devices" else "❌ No discoverable devices found"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Bluetooth discovery error", e)
            ChannelResult("Bluetooth-Discovery", false, 0, "❌ Discovery error: ${e.message}")
        }
    }
    
    private suspend fun broadcastViaWiFiDirect(payload: EmergencyPayload): ChannelResult {
        return try {
            Log.d(TAG, "📡 Broadcasting via WiFi Direct...")
            
            wifiDirectManager.startDiscoveryAndBroadcast(payload)
            delay(20000) // Wait for WiFi Direct discovery and transmission
            
            // Simplified for now
            ChannelResult(
                channel = "WiFi-Direct",
                success = false, // Most devices don't support WiFi Direct well
                count = 0,
                message = "❌ WiFi Direct not widely supported"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "WiFi Direct error", e)
            ChannelResult("WiFi-Direct", false, 0, "❌ WiFi Direct error: ${e.message}")
        }
    }
    
    private suspend fun broadcastViaWiFiHotspot(payload: EmergencyPayload): ChannelResult {
        return try {
            Log.d(TAG, "🌐 Broadcasting via WiFi Hotspot...")
            
            wifiHotspotManager.startEmergencyHotspot(payload)
            delay(30000) // Wait for hotspot to be active
            
            val isActive = wifiHotspotManager.isActive()
            
            ChannelResult(
                channel = "WiFi-Hotspot",
                success = isActive,
                count = if (isActive) 1 else 0,
                message = if (isActive) "✅ Emergency hotspot active" else "❌ Could not start emergency hotspot (requires manual permission on Android 8+)"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "WiFi Hotspot error", e)
            ChannelResult("WiFi-Hotspot", false, 0, "❌ Hotspot error: ${e.message}")
        }
    }
    
    private fun calculateResults(channelResults: List<ChannelResult>): TransmissionResults {
        var totalReached = 0
        var httpCount = 0
        var bluetoothPaired = 0
        var bluetoothDiscovered = 0
        var wifiDirect = 0
        var hotspotActive = false
        
        for (result in channelResults) {
            totalReached += result.count
            when (result.channel) {
                "HTTP" -> httpCount = result.count
                "Bluetooth-Paired" -> bluetoothPaired = result.count
                "Bluetooth-Discovery" -> bluetoothDiscovered = result.count
                "WiFi-Direct" -> wifiDirect = result.count
                "WiFi-Hotspot" -> hotspotActive = result.success
            }
        }
        
        return TransmissionResults(
            httpSuccessCount = httpCount,
            bluetoothPairedCount = bluetoothPaired,
            bluetoothDiscoveredCount = bluetoothDiscovered,
            wifiDirectCount = wifiDirect,
            hotspotActive = hotspotActive,
            totalReached = totalReached,
            channels = channelResults
        )
    }
    
    fun stopAllTransmissions() {
        try {
            transmissionJob?.cancel()
            bluetoothDiscoveryManager.stopDiscovery()
            wifiDirectManager.stopDiscovery()
            wifiHotspotManager.stopHotspot()
            
            Log.i(TAG, "🛑 All transmissions stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping transmissions", e)
        }
    }
}
