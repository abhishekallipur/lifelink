package com.emergency.medical.bluetooth.manager

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.emergency.medical.data.EmergencyPayload
import com.emergency.medical.network.DashboardClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class BluetoothRelayManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BluetoothRelayManager"
        private const val SERVICE_NAME = "EmergencyMedicalRelay"
        private val SERVICE_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val MAX_RELAY_COUNT = 5 // Prevent infinite loops
    }
    
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val dashboardClient = DashboardClient.getInstance()
    private val processedMessages = mutableSetOf<String>() // Track processed message IDs
    private val isDiscovering = AtomicBoolean(false)
    private var isDiscoveryReceiverRegistered = false

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let {
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            if (it.name != null) {
                                discoveredDevices.add(it)
                                Log.i(TAG, "🔍 Discovered device: ${it.name} (${it.address})")
                            }
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.i(TAG, "🏁 Bluetooth discovery finished.")
                    discoveryFinished?.complete(Unit)
                }
            }
        }
    }
    
    private var serverSocket: BluetoothServerSocket? = null
    private var serverJob: Job? = null
    private val clientSockets = ConcurrentHashMap<String, BluetoothSocket>()
    private var discoveryFinished: CompletableDeferred<Unit>? = null
    private val discoveredDevices = mutableSetOf<BluetoothDevice>()
    
    interface RelayCallback {
        fun onMessageReceived(payload: EmergencyPayload)
        fun onMessageSent(success: Boolean, error: String? = null)
        fun onRelayForwarded(payload: EmergencyPayload)
        fun onDiscoveryStatus(isDiscovering: Boolean)
        fun onServerStatus(started: Boolean)
        fun onClientConnected(deviceName: String?)
    }
    
    private var callback: RelayCallback? = null
    
    fun setCallback(callback: RelayCallback) {
        this.callback = callback
    }

    private fun cleanupBluetoothState() {
        Log.d(TAG, "Performing comprehensive Bluetooth state cleanup")

        // Cancel any ongoing discovery
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
                Log.d(TAG, "Cancelled ongoing Bluetooth discovery")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling discovery during cleanup: ${e.message}")
        }

        // Close all existing client sockets
        clientSockets.values.forEach { socket ->
            try {
                if (!socket.isConnected) {
                    socket.close()
                }
            } catch (e: IOException) {
                Log.w(TAG, "Error closing socket during cleanup: ${e.message}")
            }
        }
        clientSockets.clear()
        Log.d(TAG, "Cleared all client sockets")

        // Reset discovery state
        isDiscovering.set(false)
        discoveryFinished?.cancel()
        discoveryFinished = null
        discoveredDevices.clear()

        // Unregister discovery receiver if registered
        if (isDiscoveryReceiverRegistered) {
            try {
                context.unregisterReceiver(discoveryReceiver)
                isDiscoveryReceiverRegistered = false
                Log.d(TAG, "Unregistered discovery receiver")
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering receiver during cleanup: ${e.message}")
            }
        }

        // Clear processed messages cache
        processedMessages.clear()
        Log.d(TAG, "Bluetooth state cleanup completed")
    }
    
    fun startServer() {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported")
            callback?.onServerStatus(false)
            return
        }

        // Comprehensive, version-aware permission check for server
        val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            listOf(Manifest.permission.BLUETOOTH)
        }

        val missingPermissions = requiredPermissions.filter {
            ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            Log.e(TAG, "Missing Bluetooth permissions for server: ${missingPermissions.joinToString()}")
            return
        }
        
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
                Log.i(TAG, "Bluetooth server started, listening for connections...")
                callback?.onServerStatus(true)
                
                while (!Thread.currentThread().isInterrupted) {
                    try {
                        val socket = serverSocket?.accept()
                        if (socket != null) {
                            val deviceAddress = socket.remoteDevice?.address
                            if (deviceAddress != null) {
                                Log.i(TAG, "Client connected: ${socket.remoteDevice?.name}")
                                clientSockets[deviceAddress] = socket
                                callback?.onClientConnected(socket.remoteDevice?.name)
                                handleClientConnection(socket)
                            }
                        }
                    } catch (e: IOException) {
                        if (!Thread.currentThread().isInterrupted) {
                            Log.e(TAG, "Error accepting connection", e)
                        }
                        break
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error starting server", e)
                callback?.onServerStatus(false)
            }
        }
    }
    
    private fun handleClientConnection(socket: BluetoothSocket) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = socket.inputStream
                val buffer = ByteArray(4096)
                
                while (!Thread.currentThread().isInterrupted) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val receivedData = String(buffer, 0, bytesRead)
                        Log.d(TAG, "Received ${bytesRead} bytes: $receivedData")
                        
                        val payload = EmergencyPayload.fromJson(receivedData)
                        if (payload != null) {
                            handleReceivedEmergency(payload)
                        } else {
                            Log.w(TAG, "Failed to parse received data as EmergencyPayload")
                        }
                    } else if (bytesRead == -1) {
                        Log.i(TAG, "End of stream reached, client disconnected: ${socket.remoteDevice?.name}")
                        break
                    } else {
                        Log.w(TAG, "Unexpected bytesRead: $bytesRead from ${socket.remoteDevice?.name}")
                        break
                    }
                }
            } catch (e: IOException) {
                Log.d(TAG, "Client connection lost: ${socket.remoteDevice?.name}")
            } finally {
                try {
                    val deviceAddress = socket.remoteDevice?.address
                    if (deviceAddress != null) {
                        clientSockets.remove(deviceAddress)
                    }
                    socket.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing client socket", e)
                }
            }
        }
    }
    
    private fun handleReceivedEmergency(payload: EmergencyPayload) {
        Log.i(TAG, "📨 Received emergency message: ${payload.messageId} from ${payload.originalSender}")
        Log.d(TAG, "📨 Current processed messages count: ${processedMessages.size}")

        // Check if we've already processed this specific message to prevent loops
        // Use messageId instead of originalSender to allow multiple messages from same device
        if (processedMessages.contains(payload.messageId)) {
            Log.d(TAG, "Message ${payload.messageId} already processed, ignoring to prevent loop")
            return
        }

        Log.i(TAG, "📨 Processing new emergency message: ${payload.messageId}")
        
        // Check relay count to prevent infinite loops
        if (payload.relayCount >= MAX_RELAY_COUNT) {
            Log.w(TAG, "Maximum relay count reached, dropping message")
            return
        }
        
        // Mark this specific message as processed
        processedMessages.add(payload.messageId)
        Log.d(TAG, "📨 Added message ${payload.messageId} to processed cache. Cache size: ${processedMessages.size}")
        
        // Notify callback about received message
        callback?.onMessageReceived(payload)
        
        // ENHANCED LOGGING: Check internet status
        val hasInternet = isInternetAvailable()
        Log.i(TAG, "🌐 Internet availability check: $hasInternet")
        
        // If we have internet, forward to dashboard
        if (hasInternet) {
            Log.i(TAG, "🚀 ATTEMPTING to forward received emergency ${payload.messageId} to dashboard website")
            Log.i(TAG, "📦 Payload for website: ${payload.toJson()}")
            
            dashboardClient.sendRelayMessage(payload, object : DashboardClient.EmergencyCallback {
                override fun onSuccess() {
                    Log.i(TAG, "✅ WEBSITE FORWARD SUCCESS: Emergency ${payload.messageId} successfully forwarded to dashboard")
                    callback?.onRelayForwarded(payload)
                }
                
                override fun onFailure(error: String) {
                    Log.e(TAG, "❌ WEBSITE FORWARD FAILED: Failed to forward emergency ${payload.messageId} to dashboard: $error")
                    Log.e(TAG, "🔧 Attempting Bluetooth relay as fallback due to website failure")
                    // If dashboard fails, still try to relay via Bluetooth as a backup
                    relayToOtherDevices(payload.createRelayPayload())
                }
            })
        } else {
            // No internet, relay to other devices
            Log.i(TAG, "🔄 No internet available, relaying emergency ${payload.messageId} to other devices via Bluetooth")
            relayToOtherDevices(payload.createRelayPayload())
        }
    }
    
    fun broadcastEmergency(payload: EmergencyPayload) {
        Log.i(TAG, "🔄 Starting Bluetooth emergency broadcast")

        // Comprehensive cleanup before starting new broadcast
        cleanupBluetoothState()

        // Clear the processed messages set to allow new, distinct emergency events to be relayed.
        processedMessages.clear()
        Log.d(TAG, "Processed messages cache cleared for new broadcast.")

        if (bluetoothAdapter == null) {
            Log.e(TAG, "❌ Bluetooth not supported on this device")
            callback?.onMessageSent(false, "Bluetooth not supported on this device")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.e(TAG, "❌ Bluetooth is not enabled")
            callback?.onMessageSent(false, "Bluetooth is not enabled. Please turn on Bluetooth.")
            return
        }

        // Comprehensive, version-aware permission check for broadcasting
        val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            listOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        }

        val missingPermissions = requiredPermissions.filter {
            ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            val errorMsg = "Missing Bluetooth permissions for broadcast: ${missingPermissions.joinToString()}"
            Log.e(TAG, "❌ $errorMsg")
            callback?.onMessageSent(false, errorMsg)
            return
        }

        // Ensure server is running and in good state
        if (serverSocket == null) {
            Log.w(TAG, "Server socket is not available, restarting server")
            restartServer()
            // Give server time to start
            Thread.sleep(1000)
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            val jsonData = payload.toJson()
            Log.d(TAG, "📤 Broadcasting emergency data: ${jsonData.take(100)}...")
            Log.i(TAG, "📤 Message ID: ${payload.messageId}, Original Sender: ${payload.originalSender}")

            // Get paired devices immediately and start sending
            val pairedDevices = getPairedDevices()
            Log.i(TAG, "📱 Found ${pairedDevices.size} paired devices, starting immediate broadcast")
            
            // Use thread-safe counters for concurrent operations
            val successCount = java.util.concurrent.atomic.AtomicInteger(0)
            val totalAttempts = java.util.concurrent.atomic.AtomicInteger(pairedDevices.size)
            val failureReasons = java.util.concurrent.ConcurrentLinkedQueue<String>()
            val sentToDevices = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

            // Start sending to paired devices immediately (parallel)
            val pairedJobs = pairedDevices.map { device ->
                launch {
                    val sent = sendToDevice(device, jsonData)
                    if (sent) {
                        successCount.incrementAndGet()
                        sentToDevices.add(device.address)
                        Log.i(TAG, "✅ Successfully sent to paired device: ${device.name}")
                    } else {
                        if (failureReasons.size < 5) {
                            failureReasons.offer(device.name ?: device.address ?: "Unknown")
                        }
                    }
                }
            }

            // Simultaneously start discovery and send to new devices as they're found
            val discoveryJob = launch {
                val discoveredDevices = discoverDevices(15000) // Discover for 15 seconds
                Log.i(TAG, "🔍 Discovery complete, found ${discoveredDevices.size} new devices")
                
                // Filter out devices we already sent to
                val newDevices = discoveredDevices.filter { !sentToDevices.contains(it.address) }
                totalAttempts.addAndGet(newDevices.size)
                
                // Send to newly discovered devices (parallel)
                val discoveryJobs = newDevices.map { device ->
                    launch {
                        val sent = sendToDevice(device, jsonData)
                        if (sent) {
                            successCount.incrementAndGet()
                            sentToDevices.add(device.address)
                            Log.i(TAG, "✅ Successfully sent to discovered device: ${device.name}")
                        } else {
                            if (failureReasons.size < 5) {
                                failureReasons.offer(device.name ?: device.address ?: "Unknown")
                            }
                        }
                    }
                }
                
                // Wait for all discovery sends to complete
                discoveryJobs.forEach { it.join() }
            }

            // Wait for all operations to complete
            pairedJobs.forEach { it.join() }
            discoveryJob.join()

            val finalSuccessCount = successCount.get()
            val finalTotalDevices = totalAttempts.get()
            
            if (finalTotalDevices == 0) {
                Log.w(TAG, "⚠️ No paired or discoverable devices found for emergency broadcast")
                callback?.onMessageSent(false, "No paired or discoverable Bluetooth devices found.")
                return@launch
            }

            val success = finalSuccessCount > 0
            val message = if (success) {
                val failureList = failureReasons.toList()
                "✅ Sent to $finalSuccessCount of $finalTotalDevices devices" + 
                if (failureList.isNotEmpty()) " (failed: ${failureList.joinToString()})" else ""
            } else {
                "❌ Failed to send to any of $finalTotalDevices devices. Check that other devices have the app open and Bluetooth enabled."
            }
            
            Log.i(TAG, "📊 Parallel Bluetooth broadcast result: $message")
            callback?.onMessageSent(success, message)
        }
    }

    private suspend fun discoverDevices(timeoutMillis: Long): Set<BluetoothDevice> {
        if (isDiscovering.compareAndSet(false, true)) {
            Log.i(TAG, "Starting new discovery process.")
        } else {
            Log.w(TAG, "Discovery already in progress, skipping.")
            return emptySet()
        }

        discoveredDevices.clear()
        discoveryFinished = CompletableDeferred()

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        if (!isDiscoveryReceiverRegistered) {
            context.registerReceiver(discoveryReceiver, filter)
            isDiscoveryReceiverRegistered = true
        }
        
        // Version-aware permission check for scanning
        val hasScanPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasScanPermission) {
            Log.e(TAG, "Missing BLUETOOTH_SCAN or BLUETOOTH_ADMIN permission for discovery.")
            stopDiscovery()
            return emptySet()
        }

        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception while cancelling previous discovery", e)
            stopDiscovery()
            return emptySet()
        }
        
        Log.i(TAG, "🚀 Starting Bluetooth discovery...")
        callback?.onDiscoveryStatus(true)
        try {
            val started = bluetoothAdapter?.startDiscovery() ?: false
            if (!started) {
                Log.e(TAG, "Failed to start discovery")
                stopDiscovery()
                return emptySet()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting discovery", e)
            stopDiscovery()
            return emptySet()
        }

        withTimeoutOrNull(timeoutMillis) {
            discoveryFinished?.await()
        }

        stopDiscovery()
        
        Log.i(TAG, "Discovered ${discoveredDevices.size} devices in total.")
        return discoveredDevices
    }
    
    private fun getPairedDevices(): Set<BluetoothDevice> {
        // Version-aware permission check
        val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasPermission) {
            Log.e(TAG, "Missing Bluetooth permission to get paired devices.")
            return emptySet()
        }
        return bluetoothAdapter?.bondedDevices ?: emptySet()
    }
    
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
    
    private fun relayToOtherDevices(relayPayload: EmergencyPayload) {
        broadcastEmergency(relayPayload)
    }
    
    fun stopServer() {
        Log.i(TAG, "Stopping Bluetooth server")
        serverJob?.cancel()
        serverJob = null

        try {
            serverSocket?.close()
            serverSocket = null
        } catch (e: IOException) {
            Log.e(TAG, "Error closing server socket", e)
        }

        // Close all client connections
        clientSockets.values.forEach { socket ->
            try {
                socket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing client socket", e)
            }
        }
        clientSockets.clear()

        Log.i(TAG, "Bluetooth server stopped")
    }

    fun restartServer() {
        Log.i(TAG, "Restarting Bluetooth server")
        stopServer()
        // Small delay to ensure cleanup is complete
        Thread.sleep(500)
        startServer()
    }
    
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    fun getPairedDevicesCount(): Int {
        val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermission) return 0
        return bluetoothAdapter?.bondedDevices?.size ?: 0
    }

    fun stopDiscovery() {
        if (isDiscovering.getAndSet(false)) {
            try {
                if (bluetoothAdapter?.isDiscovering == true) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        bluetoothAdapter.cancelDiscovery()
                    }
                }
                if (isDiscoveryReceiverRegistered) {
                    context.unregisterReceiver(discoveryReceiver)
                    isDiscoveryReceiverRegistered = false
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error during stopDiscovery", e)
            }
            callback?.onDiscoveryStatus(false)
            Log.i(TAG, "Discovery explicitly stopped and receiver unregistered.")
        }
    }

    private suspend fun sendToDevice(device: BluetoothDevice, jsonData: String): Boolean {
        val hasConnectPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasConnectPermission) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission for device ${device.name}")
            return false
        }

        var attempt = 0
        val maxAttempts = 5 // Increased to 5 attempts
        val deviceName = device.name ?: device.address
        
        while (attempt < maxAttempts) {
            attempt++
            var socket: BluetoothSocket? = null
            try {
                Log.d(TAG, "🔗 Attempt $attempt/$maxAttempts connecting to: $deviceName")

                // Cancel any ongoing discovery to free up Bluetooth resources
                try {
                    if (bluetoothAdapter?.isDiscovering == true) {
                        bluetoothAdapter.cancelDiscovery()
                        Thread.sleep(100) // Give it a moment
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to cancel discovery: ${e.message}")
                }

                // Try multiple socket creation strategies
                socket = createSocketWithFallback(device)
                if (socket == null) {
                    Log.w(TAG, "Failed to create any socket for $deviceName")
                    continue
                }

                Log.d(TAG, "Socket created for $deviceName, attempting connection...")
                
                // Set a reasonable timeout and connect
                withTimeoutOrNull(5000) { // 5 second timeout
                    socket.connect()
                } ?: run {
                    Log.w(TAG, "Connection timeout for $deviceName")
                    socket.close()
                    throw IOException("Connection timeout")
                }

                Log.i(TAG, "✅ Connection successful to $deviceName on attempt $attempt")

                // Send data with retry logic
                var writeSuccess = false
                for (writeAttempt in 1..3) {
                    try {
                        socket.outputStream.use { out ->
                            Log.d(TAG, "Writing data to $deviceName (attempt $writeAttempt)...")
                            out.write(jsonData.toByteArray())
                            out.flush()
                            
                            // Give the other device a moment to process
                            Thread.sleep(100)
                            
                            Log.d(TAG, "Data written and flushed to $deviceName")
                        }
                        writeSuccess = true
                        break
                    } catch (e: IOException) {
                        Log.w(TAG, "Write attempt $writeAttempt failed for $deviceName: ${e.message}")
                        if (writeAttempt < 3) {
                            Thread.sleep(200)
                        }
                    }
                }

                if (writeSuccess) {
                    Log.i(TAG, "✅ Emergency sent to device: $deviceName on attempt $attempt")
                    return true // Success!
                } else {
                    throw IOException("Failed to write data after 3 attempts")
                }

            } catch (e: IOException) {
                Log.w(TAG, "Attempt $attempt failed for $deviceName: ${e.message}")
                if (attempt >= maxAttempts) {
                    Log.e(TAG, "❌ Giving up on $deviceName after $maxAttempts attempts")
                } else {
                    // Progressive backoff delay
                    val delay = attempt * 300L // 300ms, 600ms, 900ms, 1200ms
                    Thread.sleep(delay)
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "❌ Security exception sending to device: $deviceName", e)
                return false // Don't retry on security exceptions
            } catch (e: Exception) {
                Log.e(TAG, "❌ Unexpected exception sending to $deviceName: ${e.message}", e)
                if (attempt >= maxAttempts) {
                    return false
                }
                Thread.sleep(500)
            } finally {
                try {
                    socket?.close()
                } catch (e: IOException) {
                    Log.w(TAG, "Error closing socket for $deviceName: ${e.message}")
                }
            }
        }
        return false // All attempts failed
    }

    private fun createSocketWithFallback(device: BluetoothDevice): BluetoothSocket? {
        val deviceName = device.name ?: device.address
        
        // Strategy 1: Try secure RFCOMM socket
        try {
            Log.d(TAG, "Trying secure socket for $deviceName")
            return device.createRfcommSocketToServiceRecord(SERVICE_UUID)
        } catch (e: Exception) {
            Log.w(TAG, "Secure socket failed for $deviceName: ${e.message}")
        }

        // Strategy 2: Try insecure RFCOMM socket
        try {
            Log.d(TAG, "Trying insecure socket for $deviceName")
            return device.createInsecureRfcommSocketToServiceRecord(SERVICE_UUID)
        } catch (e: Exception) {
            Log.w(TAG, "Insecure socket failed for $deviceName: ${e.message}")
        }

        // Strategy 3: Try reflection-based socket (for problematic devices)
        try {
            Log.d(TAG, "Trying reflection socket for $deviceName")
            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            return method.invoke(device, 1) as BluetoothSocket
        } catch (e: Exception) {
            Log.w(TAG, "Reflection socket failed for $deviceName: ${e.message}")
        }

        return null
    }
}
