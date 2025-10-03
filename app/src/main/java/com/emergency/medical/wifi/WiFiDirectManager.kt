package com.emergency.medical.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.*
import android.net.wifi.p2p.WifiP2pManager.*
import android.util.Log
import androidx.core.app.ActivityCompat
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

class WiFiDirectManager(private val context: Context) {
    
    companion object {
        private const val TAG = "WiFiDirect"
        private const val SERVER_PORT = 8888
        private const val DISCOVERY_TIMEOUT = 15000L
        private const val CONNECTION_TIMEOUT = 10000L
    }
    
    private val wifiP2pManager: WifiP2pManager? = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private val channel: Channel? = wifiP2pManager?.initialize(context, context.mainLooper, null)
    private val peers = mutableListOf<WifiP2pDevice>()
    private var discoveryJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private var isGroupOwner = false
    
    interface WiFiDirectCallback {
        fun onPeersDiscovered(peerCount: Int)
        fun onConnectionEstablished(groupInfo: WifiP2pGroup)
        fun onTransmissionComplete(successCount: Int, totalAttempts: Int)
        fun onError(error: String)
    }
    
    private var callback: WiFiDirectCallback? = null
    
    fun setCallback(callback: WiFiDirectCallback) {
        this.callback = callback
    }
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    val isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    Log.d(TAG, "WiFi P2P state changed: ${if (isEnabled) "Enabled" else "Disabled"}")
                }
                
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    if (hasWiFiPermissions()) {
                        wifiP2pManager?.requestPeers(channel) { peerList ->
                            val deviceList = peerList.deviceList.toList()
                            peers.clear()
                            peers.addAll(deviceList)
                            Log.d(TAG, "📱 Found ${peers.size} WiFi Direct peers")
                            callback?.onPeersDiscovered(peers.size)
                        }
                    }
                }
                
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = intent.getParcelableExtra<android.net.NetworkInfo>(
                        WifiP2pManager.EXTRA_NETWORK_INFO
                    )
                    
                    if (networkInfo?.isConnected == true) {
                        wifiP2pManager?.requestConnectionInfo(channel) { info ->
                            Log.i(TAG, "🔗 WiFi Direct connection established")
                            isGroupOwner = info.isGroupOwner
                            
                            if (isGroupOwner) {
                                Log.i(TAG, "📡 This device is Group Owner, starting server...")
                                startServer()
                            }
                            
                            // Request group info
                            wifiP2pManager?.requestGroupInfo(channel) { group ->
                                group?.let { callback?.onConnectionEstablished(it) }
                            }
                        }
                    }
                }
            }
        }
    }
    
    fun startDiscoveryAndBroadcast(payload: EmergencyPayload) {
        if (wifiP2pManager == null || channel == null) {
            callback?.onError("WiFi Direct not supported")
            return
        }
        
        if (!hasWiFiPermissions()) {
            callback?.onError("WiFi permissions not granted")
            return
        }
        
        discoveryJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Register receiver
                val intentFilter = IntentFilter().apply {
                    addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
                }
                context.registerReceiver(receiver, intentFilter)
                
                // Start peer discovery
                startPeerDiscovery()
                
                // Wait for discovery
                delay(DISCOVERY_TIMEOUT)
                
                // Try to connect and broadcast to discovered peers
                broadcastToPeers(payload)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during WiFi Direct broadcast", e)
                callback?.onError("WiFi Direct failed: ${e.message}")
            } finally {
                cleanup()
            }
        }
    }
    
    private fun startPeerDiscovery() {
        if (!hasWiFiPermissions()) return
        
        wifiP2pManager?.discoverPeers(channel, object : ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "🔍 WiFi Direct peer discovery started")
            }
            
            override fun onFailure(reason: Int) {
                val reasonText = when (reason) {
                    P2P_UNSUPPORTED -> "P2P unsupported"
                    ERROR -> "Internal error"
                    BUSY -> "Framework busy"
                    else -> "Unknown error ($reason)"
                }
                Log.e(TAG, "❌ Peer discovery failed: $reasonText")
                callback?.onError("WiFi Direct discovery failed: $reasonText")
            }
        })
    }
    
    private suspend fun broadcastToPeers(payload: EmergencyPayload) {
        if (peers.isEmpty()) {
            Log.w(TAG, "No WiFi Direct peers found")
            callback?.onTransmissionComplete(0, 0)
            return
        }
        
        Log.i(TAG, "📡 Broadcasting to ${peers.size} WiFi Direct peers")
        var successCount = 0
        val jsonData = payload.toJson()
        
        for (device in peers) {
            try {
                Log.d(TAG, "🔗 Connecting to WiFi Direct peer: ${device.deviceName}")
                
                // Connect to this peer
                val config = WifiP2pConfig().apply {
                    deviceAddress = device.deviceAddress
                }
                
                var connectionSuccess = false
                wifiP2pManager?.connect(channel, config, object : ActionListener {
                    override fun onSuccess() {
                        Log.d(TAG, "✅ Connection initiated to ${device.deviceName}")
                        connectionSuccess = true
                    }
                    
                    override fun onFailure(reason: Int) {
                        Log.w(TAG, "❌ Connection failed to ${device.deviceName}: $reason")
                    }
                })
                
                // Wait a bit for connection
                delay(CONNECTION_TIMEOUT)
                
                if (connectionSuccess) {
                    // Send data via HTTP to the peer
                    if (sendDataToPeer(device, jsonData)) {
                        successCount++
                    }
                }
                
            } catch (e: Exception) {
                Log.w(TAG, "❌ Failed to broadcast to ${device.deviceName}: ${e.message}")
            }
        }
        
        Log.i(TAG, "📊 WiFi Direct broadcast complete: $successCount/${peers.size}")
        callback?.onTransmissionComplete(successCount, peers.size)
    }
    
    private suspend fun sendDataToPeer(device: WifiP2pDevice, jsonData: String): Boolean {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            
            // Try common group owner IP
            val peerIP = "192.168.49.1" // Standard WiFi Direct group owner IP
            val url = "http://$peerIP:$SERVER_PORT/emergency"
            
            val requestBody = jsonData.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            response.close()
            
            if (success) {
                Log.i(TAG, "✅ Emergency sent to ${device.deviceName} via WiFi Direct")
            } else {
                Log.w(TAG, "❌ Failed to send to ${device.deviceName}: HTTP ${response.code}")
            }
            
            success
        } catch (e: Exception) {
            Log.w(TAG, "❌ Network error sending to ${device.deviceName}: ${e.message}")
            false
        }
    }
    
    private fun startServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(SERVER_PORT)
                Log.i(TAG, "🌐 WiFi Direct server started on port $SERVER_PORT")
                
                while (!Thread.currentThread().isInterrupted && serverSocket?.isClosed == false) {
                    try {
                        val clientSocket = serverSocket?.accept()
                        clientSocket?.let { handleIncomingConnection(it) }
                    } catch (e: SocketException) {
                        if (!Thread.currentThread().isInterrupted) {
                            Log.w(TAG, "Server socket error: ${e.message}")
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "WiFi Direct server error", e)
            }
        }
    }
    
    private fun handleIncomingConnection(socket: Socket) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                
                // Read HTTP request
                val requestLine = reader.readLine()
                
                if (requestLine?.contains("POST /emergency") == true) {
                    // Skip headers until empty line
                    var line: String?
                    var contentLength = 0
                    do {
                        line = reader.readLine()
                        if (line?.startsWith("Content-Length:") == true) {
                            contentLength = line.split(":")[1].trim().toInt()
                        }
                    } while (line?.isNotEmpty() == true)
                    
                    // Read JSON body
                    val buffer = CharArray(contentLength)
                    reader.read(buffer, 0, contentLength)
                    val jsonData = String(buffer)
                    
                    // Send HTTP response
                    writer.write("HTTP/1.1 200 OK\r\n")
                    writer.write("Content-Length: 2\r\n")
                    writer.write("\r\n")
                    writer.write("OK")
                    writer.flush()
                    
                    Log.i(TAG, "📨 Received emergency via WiFi Direct: $jsonData")
                    
                    // Parse and handle emergency
                    try {
                        val payload = EmergencyPayload.fromJson(jsonData)
                        payload?.let { handleReceivedEmergency(it) }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing received emergency", e)
                    }
                }
                
                socket.close()
                
            } catch (e: Exception) {
                Log.w(TAG, "Error handling incoming connection", e)
                try {
                    socket.close()
                } catch (closeEx: Exception) {
                    // Ignore
                }
            }
        }
    }
    
    private fun handleReceivedEmergency(payload: EmergencyPayload) {
        // This would integrate with your notification system
        Log.i(TAG, "🚨 Emergency received: ${payload.name} at ${payload.latitude}, ${payload.longitude}")
        // TODO: Integrate with EmergencyNotificationManager
    }
    
    private fun hasWiFiPermissions(): Boolean {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
            )
        }
        
        return permissions.all { permission ->
            ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun stopDiscovery() {
        discoveryJob?.cancel()
        cleanup()
    }
    
    private fun cleanup() {
        try {
            serverSocket?.close()
            serverSocket = null
            
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Receiver might not be registered
            }
            
            // Disconnect from group
            wifiP2pManager?.removeGroup(channel, null)
            
            Log.i(TAG, "🛑 WiFi Direct cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}
