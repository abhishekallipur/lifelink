package com.emergency.medical.wifi

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.emergency.medical.data.EmergencyPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.*
import java.net.*
import java.util.concurrent.TimeUnit

class WiFiHotspotManager(private val context: Context) {
    
    companion object {
        private const val TAG = "WiFiHotspot"
        private const val HOTSPOT_NAME = "EMERGENCY_MEDICAL_RELAY"
        private const val HOTSPOT_PASSWORD = "EMERGENCY123"
        private const val SERVER_PORT = 8889
        private const val BROADCAST_DURATION = 30000L // 30 seconds
    }
    
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var serverSocket: ServerSocket? = null
    private var broadcastJob: Job? = null
    private var isHotspotActive = false
    
    interface HotspotCallback {
        fun onHotspotStarted()
        fun onHotspotStopped()
        fun onClientConnected(clientCount: Int)
        fun onEmergencyReceived(payload: EmergencyPayload)
        fun onError(error: String)
    }
    
    private var callback: HotspotCallback? = null
    
    fun setCallback(callback: HotspotCallback) {
        this.callback = callback
    }
    
    fun startEmergencyHotspot(payload: EmergencyPayload) {
        broadcastJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Start hotspot
                if (startHotspot()) {
                    // Start HTTP server
                    startEmergencyServer(payload)
                    
                    // Keep hotspot active for broadcast duration
                    delay(BROADCAST_DURATION)
                    
                } else {
                    callback?.onError("Failed to start emergency hotspot")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error with emergency hotspot", e)
                callback?.onError("Hotspot error: ${e.message}")
            } finally {
                stopHotspot()
            }
        }
    }
    
    private suspend fun startHotspot(): Boolean {
        return try {
            // Note: Starting WiFi hotspot programmatically requires system-level permissions
            // For API 26+, this needs to use WifiManager.LocalOnlyHotspotReservation
            
            Log.i(TAG, "🌐 Attempting to start emergency WiFi hotspot: $HOTSPOT_NAME")
            
            // Disable WiFi first (required for hotspot)
            if (wifiManager.isWifiEnabled) {
                wifiManager.isWifiEnabled = false
                delay(2000) // Wait for WiFi to turn off
            }
            
            // For newer Android versions, we can't start hotspot directly
            // Instead, we'll use WiFi Direct or ask user to enable hotspot manually
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Use LocalOnlyHotspot for newer versions
                startLocalOnlyHotspot()
            } else {
                // For older versions, would need reflection (not recommended)
                Log.w(TAG, "⚠️ Hotspot creation requires manual user action on this Android version")
                callback?.onError("Please manually enable WiFi hotspot named '$HOTSPOT_NAME' with password '$HOTSPOT_PASSWORD'")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start hotspot", e)
            callback?.onError("Hotspot start failed: ${e.message}")
            false
        }
    }
    
    private suspend fun startLocalOnlyHotspot(): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                var hotspotStarted = false
                
                wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation?) {
                        Log.i(TAG, "✅ Local hotspot started")
                        isHotspotActive = true
                        hotspotStarted = true
                        callback?.onHotspotStarted()
                        
                        reservation?.wifiConfiguration?.let { config ->
                            Log.i(TAG, "📡 Hotspot SSID: ${config.SSID}")
                            Log.i(TAG, "🔐 Hotspot Password: ${config.preSharedKey}")
                        }
                    }
                    
                    override fun onStopped() {
                        Log.i(TAG, "🛑 Local hotspot stopped")
                        isHotspotActive = false
                        callback?.onHotspotStopped()
                    }
                    
                    override fun onFailed(reason: Int) {
                        val reasonText = when (reason) {
                            ERROR_NO_CHANNEL -> "No channel available"
                            ERROR_GENERIC -> "Generic error"
                            ERROR_INCOMPATIBLE_MODE -> "Incompatible mode"
                            ERROR_TETHERING_DISALLOWED -> "Tethering not allowed"
                            else -> "Unknown error ($reason)"
                        }
                        Log.e(TAG, "❌ Hotspot failed: $reasonText")
                        callback?.onError("Hotspot failed: $reasonText")
                    }
                }, null)
                
                // Wait for callback
                delay(5000)
                hotspotStarted
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting local hotspot", e)
            false
        }
    }
    
    private suspend fun startEmergencyServer(payload: EmergencyPayload) {
        try {
            serverSocket = ServerSocket(SERVER_PORT)
            Log.i(TAG, "🌐 Emergency server started on port $SERVER_PORT")
            
            val jsonData = payload.toJson()
            var clientCount = 0
            
            while (!Thread.currentThread().isInterrupted && serverSocket?.isClosed == false) {
                try {
                    val clientSocket = serverSocket?.accept()
                    clientSocket?.let { 
                        clientCount++
                        callback?.onClientConnected(clientCount)
                        handleEmergencyClient(it, jsonData, payload)
                    }
                } catch (e: SocketException) {
                    if (!Thread.currentThread().isInterrupted) {
                        Log.w(TAG, "Server socket error: ${e.message}")
                    }
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Emergency server error", e)
            callback?.onError("Server error: ${e.message}")
        }
    }
    
    private fun handleEmergencyClient(socket: Socket, emergencyData: String, payload: EmergencyPayload) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                
                // Read HTTP request
                val requestLine = reader.readLine() ?: ""
                Log.d(TAG, "📱 Client request: $requestLine")
                
                when {
                    requestLine.contains("GET /emergency") -> {
                        // Client requesting emergency info
                        sendEmergencyResponse(writer, emergencyData)
                        Log.i(TAG, "📤 Sent emergency data to client")
                    }
                    
                    requestLine.contains("POST /emergency") -> {
                        // Client sending emergency info
                        val receivedData = readPostData(reader)
                        handleIncomingEmergency(receivedData)
                        sendOkResponse(writer)
                        Log.i(TAG, "📨 Received emergency from client")
                    }
                    
                    else -> {
                        // Default: send emergency info
                        sendEmergencyResponse(writer, emergencyData)
                    }
                }
                
                socket.close()
                
            } catch (e: Exception) {
                Log.w(TAG, "Error handling client", e)
                try {
                    socket.close()
                } catch (closeEx: Exception) {
                    // Ignore
                }
            }
        }
    }
    
    private fun sendEmergencyResponse(writer: BufferedWriter, emergencyData: String) {
        writer.write("HTTP/1.1 200 OK\r\n")
        writer.write("Content-Type: application/json\r\n")
        writer.write("Content-Length: ${emergencyData.length}\r\n")
        writer.write("Access-Control-Allow-Origin: *\r\n")
        writer.write("\r\n")
        writer.write(emergencyData)
        writer.flush()
    }
    
    private fun sendOkResponse(writer: BufferedWriter) {
        writer.write("HTTP/1.1 200 OK\r\n")
        writer.write("Content-Length: 2\r\n")
        writer.write("\r\n")
        writer.write("OK")
        writer.flush()
    }
    
    private fun readPostData(reader: BufferedReader): String {
        // Skip headers
        var line: String?
        var contentLength = 0
        do {
            line = reader.readLine()
            if (line?.startsWith("Content-Length:") == true) {
                contentLength = line.split(":")[1].trim().toInt()
            }
        } while (line?.isNotEmpty() == true)
        
        // Read body
        val buffer = CharArray(contentLength)
        reader.read(buffer, 0, contentLength)
        return String(buffer)
    }
    
    private fun handleIncomingEmergency(jsonData: String) {
        try {
            val payload = EmergencyPayload.fromJson(jsonData)
            payload?.let {
                Log.i(TAG, "🚨 Emergency received via hotspot: ${it.name}")
                callback?.onEmergencyReceived(it)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing received emergency", e)
        }
    }
    
    fun stopHotspot() {
        try {
            broadcastJob?.cancel()
            
            serverSocket?.close()
            serverSocket = null
            
            isHotspotActive = false
            
            Log.i(TAG, "🛑 Emergency hotspot stopped")
            callback?.onHotspotStopped()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping hotspot", e)
        }
    }
    
    fun isActive(): Boolean = isHotspotActive
    
    // Helper function for other devices to scan and connect to emergency hotspots
    fun scanForEmergencyHotspots(): List<String> {
        return try {
            val scanResults = wifiManager.scanResults
            scanResults?.filter { result ->
                result.SSID.contains("EMERGENCY", ignoreCase = true) ||
                result.SSID.contains("MEDICAL", ignoreCase = true) ||
                result.SSID == HOTSPOT_NAME
            }?.map { it.SSID } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning for emergency hotspots", e)
            emptyList()
        }
    }
    
    // Connect to discovered emergency hotspot and fetch emergency data
    suspend fun connectToEmergencyHotspot(ssid: String): EmergencyPayload? {
        return try {
            Log.i(TAG, "🔗 Attempting to connect to emergency hotspot: $ssid")
            
            // Note: Actual WiFi connection requires user interaction on modern Android
            // This is a simplified approach
            
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            
            // Try to fetch emergency data from the hotspot
            val request = Request.Builder()
                .url("http://192.168.43.1:$SERVER_PORT/emergency") // Standard hotspot gateway
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonData = response.body?.string()
                response.close()
                
                jsonData?.let { EmergencyPayload.fromJson(it) }
            } else {
                response.close()
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error connecting to emergency hotspot", e)
            null
        }
    }
}
