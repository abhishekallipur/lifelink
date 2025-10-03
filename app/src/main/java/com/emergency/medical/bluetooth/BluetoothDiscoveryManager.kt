package com.emergency.medical.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.emergency.medical.data.EmergencyPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

class BluetoothDiscoveryManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BluetoothDiscovery"
        private const val DISCOVERY_DURATION_MS = 15000L // 15 seconds
        private const val SERVICE_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }
    
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val discoveredDevices = mutableSetOf<BluetoothDevice>()
    private var discoveryJob: Job? = null
    private var isDiscovering = false
    
    interface DiscoveryCallback {
        fun onDeviceFound(device: BluetoothDevice)
        fun onDiscoveryStarted()
        fun onDiscoveryCompleted(devicesFound: Int)
        fun onTransmissionComplete(successCount: Int, totalAttempts: Int)
        fun onError(error: String)
    }
    
    private var callback: DiscoveryCallback? = null
    
    fun setCallback(callback: DiscoveryCallback) {
        this.callback = callback
    }
    
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        Log.d(TAG, "📱 Discovered device: ${it.name ?: "Unknown"} (${it.address})")
                        discoveredDevices.add(it)
                        callback?.onDeviceFound(it)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.i(TAG, "🔍 Discovery completed. Found ${discoveredDevices.size} devices")
                    callback?.onDiscoveryCompleted(discoveredDevices.size)
                }
            }
        }
    }
    
    fun startDiscoveryAndBroadcast(payload: EmergencyPayload) {
        if (bluetoothAdapter == null) {
            callback?.onError("Bluetooth not supported")
            return
        }
        
        if (!bluetoothAdapter.isEnabled) {
            callback?.onError("Bluetooth is disabled. Please enable Bluetooth.")
            return
        }
        
        if (!hasBluetoothPermissions()) {
            callback?.onError("Bluetooth permissions not granted")
            return
        }
        
        discoveryJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Clear previous discoveries
                discoveredDevices.clear()
                
                // Register discovery receiver
                val filter = IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }
                context.registerReceiver(discoveryReceiver, filter)
                
                // Make device discoverable
                makeDeviceDiscoverable()
                
                // Start discovery
                startDeviceDiscovery()
                
                // Wait for discovery to complete
                delay(DISCOVERY_DURATION_MS)
                
                // Stop discovery if still running
                if (bluetoothAdapter.isDiscovering) {
                    bluetoothAdapter.cancelDiscovery()
                }
                
                // Unregister receiver
                try {
                    context.unregisterReceiver(discoveryReceiver)
                } catch (e: Exception) {
                    Log.w(TAG, "Error unregistering receiver", e)
                }
                
                // Now broadcast to discovered devices + paired devices
                val allDevices = mutableSetOf<BluetoothDevice>()
                allDevices.addAll(discoveredDevices)
                
                // Also include paired devices
                bluetoothAdapter.bondedDevices?.let { pairedDevices ->
                    allDevices.addAll(pairedDevices)
                }
                
                broadcastToDevices(payload, allDevices)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during discovery and broadcast", e)
                callback?.onError("Discovery failed: ${e.message}")
            }
        }
    }
    
    private fun makeDeviceDiscoverable() {
        try {
            // Make this device discoverable so others can find it
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30)
            discoverableIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            // Note: This requires activity context, will handle in calling activity
            
            Log.i(TAG, "🔆 Device set to discoverable mode")
        } catch (e: Exception) {
            Log.w(TAG, "Could not set discoverable mode", e)
        }
    }
    
    private fun startDeviceDiscovery() {
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            
            val discoveryStarted = bluetoothAdapter?.startDiscovery() ?: false
            if (discoveryStarted) {
                isDiscovering = true
                Log.i(TAG, "🔍 Started Bluetooth device discovery")
                callback?.onDiscoveryStarted()
            } else {
                Log.e(TAG, "❌ Failed to start device discovery")
                callback?.onError("Failed to start Bluetooth discovery")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting discovery", e)
            callback?.onError("Discovery start failed: ${e.message}")
        }
    }
    
    private suspend fun broadcastToDevices(payload: EmergencyPayload, devices: Set<BluetoothDevice>) {
        if (devices.isEmpty()) {
            callback?.onError("No devices found to broadcast to. Make sure other emergency apps are nearby and Bluetooth is enabled.")
            return
        }
        
        Log.i(TAG, "📡 Broadcasting emergency to ${devices.size} devices")
        var successCount = 0
        val totalAttempts = devices.size
        
        val jsonData = payload.toJson()
        
        for (device in devices) {
            try {
                Log.d(TAG, "🔗 Connecting to: ${device.name ?: "Unknown"} (${device.address})")
                
                // Try to connect and send
                val socket = device.createRfcommSocketToServiceRecord(java.util.UUID.fromString(SERVICE_UUID))
                socket.connect()
                
                val outputStream = socket.outputStream
                val data = jsonData.toByteArray()
                outputStream.write(data)
                outputStream.flush()
                
                socket.close()
                successCount++
                Log.i(TAG, "✅ Emergency sent to: ${device.name ?: "Unknown"}")
                
                // Small delay between connections
                delay(500)
                
            } catch (e: IOException) {
                Log.w(TAG, "❌ Failed to send to: ${device.name ?: "Unknown"} - ${e.message}")
                // Continue with other devices
            } catch (e: Exception) {
                Log.e(TAG, "❌ Unexpected error with: ${device.name ?: "Unknown"}", e)
            }
        }
        
        Log.i(TAG, "📊 Broadcast complete: $successCount/$totalAttempts devices reached")
        callback?.onTransmissionComplete(successCount, totalAttempts)
    }
    
    private fun hasBluetoothPermissions(): Boolean {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
        
        return permissions.all { permission ->
            ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun stopDiscovery() {
        try {
            discoveryJob?.cancel()
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            isDiscovering = false
            
            try {
                context.unregisterReceiver(discoveryReceiver)
            } catch (e: Exception) {
                // Receiver might not be registered
            }
            
            Log.i(TAG, "🛑 Discovery stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping discovery", e)
        }
    }
    
    fun isDiscovering(): Boolean = isDiscovering
    
    fun getDiscoveredDevicesCount(): Int = discoveredDevices.size
}
