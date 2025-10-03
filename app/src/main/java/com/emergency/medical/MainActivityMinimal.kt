package com.emergency.medical

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.emergency.medical.audio.EmergencyAudioManager
// import com.emergency.medical.bluetooth.ble.BleRelayService
import com.emergency.medical.bluetooth.manager.BluetoothRelayManager
import com.emergency.medical.data.MedicalInfo
import com.emergency.medical.data.MedicalProfileManager
import com.emergency.medical.data.MedicalDataManager
import com.emergency.medical.data.MessageStatusManager
import com.emergency.medical.data.EmergencyPayload
import com.emergency.medical.transmission.MultiChannelTransmissionManager
import com.emergency.medical.databinding.ActivityMainSimpleBinding
import com.emergency.medical.flashlight.EmergencyFlashlightManager
import com.emergency.medical.network.DashboardClient
import com.emergency.medical.notifications.EmergencyNotificationManager
import com.emergency.medical.ui.MessageStatusAdapter
import com.emergency.medical.utils.DeviceStatusMonitor
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class MainActivityMinimal : AppCompatActivity() {
    
    companion object {
        private const val TAG = "EmergencyMedicalRelay"
    }
    
    private lateinit var binding: ActivityMainSimpleBinding
    private lateinit var bluetoothRelayManager: BluetoothRelayManager
    private lateinit var deviceStatusMonitor: DeviceStatusMonitor
    private lateinit var medicalDataManager: MedicalDataManager
    private lateinit var messageStatusManager: MessageStatusManager
    private lateinit var audioManager: EmergencyAudioManager
    private lateinit var dashboardClient: DashboardClient
    private lateinit var messageStatusAdapter: MessageStatusAdapter
    private lateinit var emergencyNotificationManager: EmergencyNotificationManager
    private lateinit var multiChannelTransmissionManager: MultiChannelTransmissionManager
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private val uiScope = CoroutineScope(Dispatchers.Main)
    
    // AtomicBoolean is used for thread-safe operations, preventing race conditions
    private val isSendingEmergency = AtomicBoolean(false)
    
    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Log.i(TAG, "All permissions granted")
            Toast.makeText(this, "✅ All permissions granted", Toast.LENGTH_SHORT).show()
            initializeApp()
        } else {
            val deniedPermissions = permissions.filter { !it.value }.keys
            Log.w(TAG, "Permissions denied: ${deniedPermissions.joinToString()}")
            Toast.makeText(this, "⚠️ Some permissions denied. App functionality may be limited.", Toast.LENGTH_LONG).show()
            initializeApp() // Initialize anyway with limited functionality
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainSimpleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            checkAndRequestPermissions()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "❌ Error starting app: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        
        // Location permissions (always required)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        
        // WiFi permissions (always required)
        permissions.add(Manifest.permission.ACCESS_WIFI_STATE)
        permissions.add(Manifest.permission.CHANGE_WIFI_STATE)
        
        // Bluetooth permissions based on Android version
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Android 12+ permissions
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            // Pre-Android 12 permissions
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        
        Log.i(TAG, "Requesting permissions: ${permissions.joinToString(", ")}")
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun initializeApp() {
        try {
            // Initialize managers
            deviceStatusMonitor = DeviceStatusMonitor(this)
            medicalDataManager = MedicalDataManager.getInstance()
            messageStatusManager = MessageStatusManager.getInstance()
            audioManager = EmergencyAudioManager(this)
            dashboardClient = DashboardClient()
            bluetoothRelayManager = BluetoothRelayManager(this)
            emergencyNotificationManager = EmergencyNotificationManager(this)
            multiChannelTransmissionManager = MultiChannelTransmissionManager(this)

            // Start Bluetooth server to accept incoming messages (if permissions OK)
            try {
                bluetoothRelayManager.startServer()
                Log.i(TAG, "Bluetooth server start invoked during init")
            } catch (e: Exception) {
                Log.e(TAG, "Failed starting Bluetooth server: ${e.message}")
            }

            setupUI()
            updateDeviceStatus()
            setupMedicalFormListeners()
            startPeriodicUIUpdates()

            val medicalInfo = medicalDataManager.getMedicalInfo()
            updateMedicalInfoDisplay(medicalInfo)

            // Set up message status list
            messageStatusAdapter = MessageStatusAdapter()

            Log.i(TAG, "App initialized successfully")
            updateEmergencyStatus("✅ App ready - Emergency system active", R.color.status_ready)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing app", e)
            updateEmergencyStatus("❌ App initialization error: ${e.message}", R.color.emergency_red)
        }
    }

    private fun setupBloodGroupSpinner() {
        try {
            val bloodGroups = arrayOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, bloodGroups)
            val autoCompleteTextView = binding.bloodGroupSpinner
            autoCompleteTextView.setAdapter(adapter)
            
            autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
                if (position >= 0) {
                    val selectedBloodGroup = bloodGroups[position]
                    saveBloodGroup(selectedBloodGroup)
                }
            }
            
            // Also listen for text changes in case user types
            autoCompleteTextView.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    saveMedicalInfo()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up blood group spinner", e)
        }
    }

    private fun setupMedicalFormListeners() {
        try {
            // Add TextWatcher for immediate saving
            binding.nameEditText.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    saveMedicalInfo()
                }
            })

            binding.ageEditText.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    saveMedicalInfo()
                }
            })

            binding.medicalConditionsEditText.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    saveMedicalInfo()
                }
            })

            binding.emergencyContactEditText.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    saveMedicalInfo()
                }
            })

            // Also add focus change listeners as backup
            binding.nameEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    saveMedicalInfo()
                }
            }

            binding.ageEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    saveMedicalInfo()
                }
            }

            binding.medicalConditionsEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    saveMedicalInfo()
                }
            }

            binding.emergencyContactEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    saveMedicalInfo()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up medical form listeners", e)
        }
    }

    private fun setupUI() {
        try {
            // SOS Emergency Button
            binding.sosButton.setOnClickListener {
                handleEmergencyButtonPress()
            }

            // Siren Button
            binding.sirenButton.setOnClickListener {
                toggleSiren()
            }

            setupBloodGroupSpinner()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up UI", e)
        }
    }

    private fun startPeriodicUIUpdates() {
        uiScope.launch {
            while (true) {
                updateDeviceStatus()
                delay(5000) // Update every 5 seconds
            }
        }
    }

    private fun testLocationCapture() {
        try {
            deviceStatusMonitor.getLastKnownLocation { locationData ->
                if (locationData != null) {
                    Log.i(TAG, "Location test successful: ${locationData.latitude}, ${locationData.longitude}")
                    updateEmergencyStatus("📍 Location: ${locationData.latitude}, ${locationData.longitude}", android.R.color.holo_blue_bright)
                } else {
                    Log.w(TAG, "Location test failed - no location data available")
                    updateEmergencyStatus("⚠️ Location unavailable - GPS may be disabled", android.R.color.holo_orange_dark)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error testing location capture", e)
        }
    }

    private fun handleEmergencyButtonPress() {
        // Use compareAndSet for atomic check-and-set operation to prevent double-sending
        if (!isSendingEmergency.compareAndSet(false, true)) {
            Log.w(TAG, "Emergency sending already in progress - ignoring button press")
            return
        }

        try {
            Log.i(TAG, "Emergency button pressed - starting emergency process")
            val medicalInfo = medicalDataManager.getMedicalInfo()
            
            Log.i(TAG, "Medical info validation - Name: '${medicalInfo.fullName}', Contact: '${medicalInfo.emergencyContact}'")
            
            if (medicalInfo.fullName.isBlank()) {
                Log.w(TAG, "Full name is missing")
                updateEmergencyStatus("⚠️ Please enter your full name", R.color.status_warning)
                showErrorNotification("Full Name Required", "Please enter your full name in the medical form")
                isSendingEmergency.set(false)
                return
            }
            
            if (medicalInfo.emergencyContact.isBlank()) {
                Log.w(TAG, "Emergency contact is missing")
                updateEmergencyStatus("⚠️ Please enter emergency contact", R.color.status_warning)
                showErrorNotification("Emergency Contact Required", "Please enter an emergency contact number")
                isSendingEmergency.set(false)
                return
            }

            val statusMessage = "Sending Emergency Alert..."
            updateEmergencyStatus(statusMessage, R.color.status_warning)
            binding.sosButton.isEnabled = false

            deviceStatusMonitor.getLastKnownLocation { locationData ->
                try {
                    val deviceInfo = deviceStatusMonitor.getCurrentDeviceInfo()

                    val emergencyPayload = EmergencyPayload(
                        name = medicalInfo.fullName,
                        age = medicalInfo.age.toIntOrNull(),
                        phone = medicalInfo.emergencyContact,
                        bloodGroup = medicalInfo.bloodGroup,
                        phoneBattery = deviceInfo?.batteryLevel ?: 0,
                        latitude = locationData?.latitude ?: 0.0,
                        longitude = locationData?.longitude ?: 0.0,
                        message = medicalInfo.medicalConditions.takeIf { it.isNotBlank() },
                        currentMedicalIssue = medicalInfo.medicalConditions.takeIf { it.isNotBlank() },
                        status = "Pending",
                        priority = "Medium",
                        notes = null,
                        timestamp = java.time.Instant.now().toString(),
                        solvedTimestamp = null,
                        isRelay = false,
                        relayCount = 0,
                        originalSender = "EMG_${System.currentTimeMillis()}_${medicalInfo.fullName}",
                        messageId = "EMG_${System.currentTimeMillis()}"
                    )

                    // Check internet status
                    val internetAvailable = deviceStatusMonitor.isInternetAvailable()
                    val networkType = deviceStatusMonitor.getNetworkType()

                    if (internetAvailable && networkType != "None") {
                        updateEmergencyStatus("📡 Sending via Internet ($networkType)...", R.color.status_sending)
                        sendEmergencyViaDashboard(emergencyPayload)
                    } else {
                        updateEmergencyStatus("📻 No Internet - Using WiFi/Bluetooth backup...", R.color.status_sending)
                        sendEmergencyViaBluetooth(emergencyPayload)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing location data", e)
                    enableEmergencyButton()
                    isSendingEmergency.set(false)
                    updateEmergencyStatus("Error: ${e.message}", R.color.emergency_red)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling emergency button press", e)
            enableEmergencyButton()
            isSendingEmergency.set(false)
            updateEmergencyStatus("Error: ${e.message}", R.color.emergency_red)
        }
    }

    private fun sendEmergencyViaDashboard(payload: EmergencyPayload) {
        try {
            dashboardClient.sendEmergency(payload, object : DashboardClient.EmergencyCallback {
                override fun onSuccess() {
                    mainHandler.post {
                        showSuccessNotification("Emergency Alert Sent Successfully", "Your emergency alert has been sent to the dashboard!")
                        updateEmergencyStatus("✅ Emergency alert sent to dashboard!", android.R.color.holo_green_dark)
                        updateLastEmergencyTime()
                        enableEmergencyButton()
                        isSendingEmergency.set(false)
                    }
                }

                override fun onFailure(error: String) {
                    mainHandler.post {
                        updateEmergencyStatus("❌ Failed to send to dashboard: $error", android.R.color.holo_red_dark)
                        enableEmergencyButton()
                        isSendingEmergency.set(false)
                    }
                }
            })
        } catch (e: Exception) {
            enableEmergencyButton()
            isSendingEmergency.set(false)
            updateEmergencyStatus("❌ Error connecting to dashboard", android.R.color.holo_red_dark)
        }
    }

    private fun sendEmergencyViaBluetooth(payload: EmergencyPayload) {
        try {
            // Check Bluetooth permissions before attempting transmission
            if (!hasBluetoothPermissions()) {
                Log.e(TAG, "Missing Bluetooth permissions for transmission")
                updateEmergencyStatus("❌ Bluetooth permissions required", android.R.color.holo_red_dark)
                showErrorNotification("Bluetooth Permissions", "Please grant Bluetooth permissions to use backup transmission")
                enableEmergencyButton()
                isSendingEmergency.set(false)
                
                // Re-request permissions
                Toast.makeText(this, "Re-requesting Bluetooth permissions...", Toast.LENGTH_SHORT).show()
                checkAndRequestPermissions()
                return
            }

            // BLE replacement with auto-connection:
            Log.i(TAG, "[BT-FALLBACK] Initiating BLE emergency transmission")
            updateEmergencyStatus("📡 Connecting to nearby devices...", R.color.status_sending)
            
            // Convert full EmergencyPayload to JSON for complete transmission
            val gson = com.google.gson.Gson()
            val payloadJson = gson.toJson(payload)
            val payloadBytes = payloadJson.toByteArray()
            
            Log.i(TAG, "Sending complete emergency payload: $payloadJson")
            Log.i(TAG, "Payload size: ${payloadBytes.size} bytes")
            
            // TODO: Re-enable when BleRelayService is fixed
            // val serviceIntent = Intent(this, com.emergency.medical.bluetooth.ble.BleRelayService::class.java).apply {
            //     putExtra("MESSAGE_PAYLOAD", payloadBytes)
            //     putExtra("EXTRA_SEND_MESSAGE", true)
            // }
            
            try {
                // TODO: Re-enable when BleRelayService is fixed
                // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //     startForegroundService(serviceIntent)
                // } else {
                //     startService(serviceIntent)
                // }
                
                Log.i(TAG, "BLE Relay Service disabled - using legacy Bluetooth instead")
                
                // For now, use the existing bluetooth manager as fallback
                val emergencyPayload = gson.fromJson(payloadJson, EmergencyPayload::class.java)
                bluetoothRelayManager.broadcastEmergency(emergencyPayload)
                
                updateEmergencyStatus("✅ Emergency sent via legacy Bluetooth", R.color.status_sending)
                showSuccessNotification("Emergency Sent", "Emergency data sent via Bluetooth")
                updateLastEmergencyTime()
                
                // Auto-stop after 5 minutes to save battery
                // mainHandler.postDelayed({
                //     stopService(serviceIntent)
                //     updateEmergencyStatus("BLE relay stopped automatically", android.R.color.holo_orange_dark)
                // }, 300000) // 5 minutes
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send emergency via Bluetooth", e)
                updateEmergencyStatus("❌ Failed to start BLE relay: ${e.message}", android.R.color.holo_red_dark)
            }
            
            enableEmergencyButton()
            isSendingEmergency.set(false)
        } catch (e: Exception) {
            updateEmergencyStatus("❌ Error starting BLE relay", android.R.color.holo_red_dark)
            enableEmergencyButton()
            isSendingEmergency.set(false)
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ permissions
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            // Pre-Android 12 permissions
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Helper functions
    private fun updateEmergencyStatus(message: String, colorResId: Int) {
        try {
            binding.emergencyStatus.text = message
            binding.emergencyStatus.setTextColor(ContextCompat.getColor(this, colorResId))
            Log.i(TAG, "Status updated: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating emergency status", e)
        }
    }

    private fun updateDeviceStatus() {
        try {
            val deviceInfo = deviceStatusMonitor.getCurrentDeviceInfo()
            if (deviceInfo != null) {
                binding.batteryStatus.text = "Battery: ${deviceInfo.batteryLevel}%"
                
                val networkType = deviceStatusMonitor.getNetworkType()
                binding.internetStatus.text = "Network: $networkType"
                
                val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                val bluetoothStatus = when {
                    bluetoothAdapter == null -> "Not supported"
                    bluetoothAdapter.isEnabled -> "Enabled"
                    else -> "Disabled"
                }
                binding.bluetoothStatus.text = "Bluetooth: $bluetoothStatus"
                
                // Update colors based on status
                val networkColor = when (networkType) {
                    "WiFi", "Mobile" -> android.R.color.holo_green_dark
                    "None" -> android.R.color.holo_red_dark
                    else -> android.R.color.holo_orange_dark
                }
                binding.internetStatus.setTextColor(ContextCompat.getColor(this, networkColor))
                
                val bluetoothColor = when (bluetoothStatus) {
                    "Enabled" -> android.R.color.holo_green_dark
                    "Disabled" -> android.R.color.holo_orange_dark
                    else -> android.R.color.holo_red_dark
                }
                binding.bluetoothStatus.setTextColor(ContextCompat.getColor(this, bluetoothColor))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating device status", e)
        }
    }

    private fun updateMedicalInfoDisplay(medicalInfo: MedicalInfo) {
        try {
            binding.nameEditText.setText(medicalInfo.fullName)
            binding.ageEditText.setText(medicalInfo.age)
            binding.medicalConditionsEditText.setText(medicalInfo.medicalConditions)
            binding.emergencyContactEditText.setText(medicalInfo.emergencyContact)
            
            // Set blood group for MaterialAutoCompleteTextView
            if (medicalInfo.bloodGroup.isNotBlank()) {
                binding.bloodGroupSpinner.setText(medicalInfo.bloodGroup, false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating medical info display", e)
        }
    }

    private fun saveMedicalInfo() {
        try {
            val selectedBloodGroup = binding.bloodGroupSpinner.text.toString().trim().takeIf { 
                it.isNotBlank() && it != "Select" 
            } ?: ""
            
            val medicalInfo = MedicalInfo(
                fullName = binding.nameEditText.text.toString().trim(),
                age = binding.ageEditText.text.toString().trim(),
                bloodGroup = selectedBloodGroup,
                medicalConditions = binding.medicalConditionsEditText.text.toString().trim(),
                emergencyContact = binding.emergencyContactEditText.text.toString().trim()
            )
            medicalDataManager.saveMedicalInfo(medicalInfo)
            Log.i(TAG, "Medical info saved: Name='${medicalInfo.fullName}', Contact='${medicalInfo.emergencyContact}', BloodGroup='${medicalInfo.bloodGroup}'")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving medical info", e)
        }
    }

    private fun saveBloodGroup(bloodGroup: String) {
        try {
            val currentInfo = medicalDataManager.getMedicalInfo()
            val updatedInfo = currentInfo.copy(bloodGroup = bloodGroup)
            medicalDataManager.saveMedicalInfo(updatedInfo)
            Log.i(TAG, "Blood group saved: $bloodGroup")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving blood group", e)
        }
    }

    private fun toggleSiren() {
        try {
            if (audioManager.isPlaying()) {
                audioManager.stopSiren()
                binding.sirenStatus.text = "Siren: OFF"
                binding.sirenStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            } else {
                audioManager.startSiren()
                binding.sirenStatus.text = "Siren: ON"
                binding.sirenStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling siren", e)
        }
    }

    private fun enableEmergencyButton() {
        try {
            binding.sosButton.isEnabled = true
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling emergency button", e)
        }
    }

    private fun showSuccessNotification(title: String, message: String) {
        try {
            val notificationPayload = EmergencyPayload(
                name = title,
                age = null,
                phone = null,
                bloodGroup = null,
                phoneBattery = null,
                latitude = 0.0,
                longitude = 0.0,
                message = message,
                currentMedicalIssue = message
            )
            emergencyNotificationManager.showEmergencyForwardedNotification(notificationPayload)
            Toast.makeText(this, "$title: $message", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing success notification", e)
        }
    }

    private fun showErrorNotification(title: String, message: String) {
        try {
            Log.e(TAG, "Error notification: $title - $message")
            Toast.makeText(this, "$title: $message", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing error notification", e)
        }
    }

    private fun updateLastEmergencyTime() {
        try {
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            Log.i(TAG, "Last emergency time: $currentTime")
            updateEmergencyStatus("Last alert: $currentTime", android.R.color.holo_green_dark)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating last emergency time", e)
        }
    }

    private val bleStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra("STATUS_MESSAGE") ?: "Unknown status"
            val color = intent?.getIntExtra("STATUS_COLOR", android.R.color.darker_gray) ?: android.R.color.darker_gray
            updateEmergencyStatus(status, color)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (::deviceStatusMonitor.isInitialized) {
                updateDeviceStatus()
            }
            
            // Register BLE status receiver
            val filter = IntentFilter("ACTION_BLE_STATUS")
            registerReceiver(bleStatusReceiver, filter)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume", e)
        }
    }
    
    override fun onPause() {
        super.onPause()
        try {
            // Unregister receiver to avoid leaks
            unregisterReceiver(bleStatusReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Log.i(TAG, "Cleaning up resources")
            audioManager.release()
            
            // Stop BLE service when activity is destroyed
            stopBleRelayService()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }
    
    private fun stopBleRelayService() {
        try {
            // TODO: Re-enable when BleRelayService is fixed
            // val serviceIntent = Intent(this, com.emergency.medical.bluetooth.ble.BleRelayService::class.java)
            // stopService(serviceIntent)
            Log.d(TAG, "BLE Relay Service stop requested (currently disabled)")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping BLE service", e)
        }
    }
}
