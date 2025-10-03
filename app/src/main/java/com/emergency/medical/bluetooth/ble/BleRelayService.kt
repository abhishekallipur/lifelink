package com.emergency.medical.bluetooth.blepackage com.emergency.medical.bluetooth.blepackage com.emergency.medical.bluetooth.blepackage com.emergency.medical.bluetooth.ble



import android.app.Service

import android.content.Intent

import android.os.IBinderimport android.Manifest

import android.util.Log

import android.app.Notification

/**

 * Basic BLE Relay Service for Emergency Messagesimport android.app.NotificationChannelimport android.Manifestimport android.Manifest

 * This service will handle Bluetooth auto-discovery and message forwarding

 */import android.app.NotificationManager

class BleRelayService : Service() {

import android.app.Serviceimport android.app.Notificationimport android.app.Notifi        // Initialize Bluetooth components

    private val TAG = "BleRelay"

import android.bluetooth.BluetoothAdapter

    override fun onCreate() {

        super.onCreate()import android.bluetooth.BluetoothDeviceimport android.app.NotificationChannel        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        Log.d(TAG, "BLE Relay Service Created")

    }import android.bluetooth.BluetoothGatt



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {import android.bluetooth.BluetoothGattCallbackimport android.app.NotificationManager        bluetoothAdapter = bluetoothManager.adapter

        Log.d(TAG, "BLE Relay Service Started")

        val message = intent?.getStringExtra("emergency_message")import android.bluetooth.BluetoothGattCharacteristic

        if (message != null) {

            Log.d(TAG, "Processing emergency message: $message")import android.bluetooth.BluetoothGattServerimport android.app.Service        

            // TODO: Add BLE functionality here

        }import android.bluetooth.BluetoothGattServerCallback

        return START_STICKY

    }import android.bluetooth.BluetoothGattServiceimport android.bluetooth.BluetoothAdapter        // Check if Bluetooth is supported and enabled with detailed logging



    override fun onDestroy() {import android.bluetooth.BluetoothManager

        super.onDestroy()

        Log.d(TAG, "BLE Relay Service Stopped")import android.bluetooth.BluetoothProfileimport android.bluetooth.BluetoothDevice        Log.d(TAG, "🔍 Checking Bluetooth availability...")

    }

import android.bluetooth.le.*

    override fun onBind(intent: Intent?): IBinder? = null

}import android.content.Contextimport android.bluetooth.BluetoothGatt        

import android.content.Intent

import android.content.pm.PackageManagerimport android.bluetooth.BluetoothGattCallback        if (bluetoothAdapter == null) {

import android.os.Handler

import android.os.IBinderimport android.bluetooth.BluetoothGattCharacteristic            Log.e(TAG, "❌ CRITICAL: Bluetooth not supported on this device!")

import android.os.Looper

import android.util.Logimport android.bluetooth.BluetoothGattServer            Log.e(TAG, "💡 This app requires Bluetooth LE support")

import androidx.core.app.ActivityCompat

import androidx.core.app.NotificationCompatimport android.bluetooth.BluetoothGattServerCallback            return

import com.emergency.medical.DashboardClient

import com.emergency.medical.models.EmergencyPayloadimport android.bluetooth.BluetoothGattService        }

import com.google.gson.Gson

import java.nio.charset.Charsetimport android.bluetooth.BluetoothManager        

import java.util.*

import java.util.concurrent.ConcurrentHashMapimport android.bluetooth.BluetoothProfile        if (!bluetoothAdapter!!.isEnabled) {



class BleRelayService : Service() {import android.bluetooth.le.AdvertiseCallback            Log.e(TAG, "❌ BLUETOOTH IS DISABLED!")



    private val TAG = "🩺 BleRelay"import android.bluetooth.le.AdvertiseData            Log.e(TAG, "📱 Please enable Bluetooth: Settings → Bluetooth → Turn ON")

    

    // Bluetooth componentsimport android.bluetooth.le.AdvertiseSettings            Log.e(TAG, "🔄 Service will retry when Bluetooth is enabled")

    private var bluetoothAdapter: BluetoothAdapter? = null

    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = nullimport android.bluetooth.le.BluetoothLeAdvertiser            return

    private var bluetoothLeScanner: BluetoothLeScanner? = null

    private var bluetoothGattServer: BluetoothGattServer? = nullimport android.bluetooth.le.BluetoothLeScanner        }

    

    // State trackingimport android.bluetooth.le.ScanCallback        

    private var isAdvertising = false

    private var isScanning = falseimport android.bluetooth.le.ScanFilter        Log.d(TAG, "✅ Bluetooth adapter available and enabled")

    

    // Device managementimport android.bluetooth.le.ScanResult        Log.d(TAG, "📱 Device name: ${bluetoothAdapter!!.name}")

    private val discoveredDevices = ConcurrentHashMap<String, BluetoothDevice>()

    private val connectedDevices = ConcurrentHashMap<String, BluetoothGatt>()import android.bluetooth.le.ScanSettings        Log.d(TAG, "🆔 Device address: ${bluetoothAdapter!!.address}")

    private val processedMessageIds = mutableSetOf<String>()

    import android.content.Context        

    // Handlers and clients

    private val mainHandler = Handler(Looper.getMainLooper())import android.content.Intent        // Check BLE support

    private val dashboardClient = DashboardClient()

    private val gson = Gson()import android.content.SharedPreferences        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {

    

    companion object {import android.content.pm.PackageManager            Log.e(TAG, "❌ CRITICAL: Bluetooth LE not supported on this device!")

        private const val NOTIFICATION_ID = 12345

        private const val CHANNEL_ID = "ble_emergency_channel"import android.net.ConnectivityManager            return

        

        // Emergency BLE service UUID - unique for emergency appsimport android.net.NetworkCapabilities        }

        val EMERGENCY_SERVICE_UUID: UUID = UUID.fromString("12345678-1234-5678-9ABC-123456789ABC")

        val EMERGENCY_CHARACTERISTIC_UUID: UUID = UUID.fromString("87654321-4321-8765-CBA9-987654321CBA")import android.os.Build        

    }

import android.os.Handler        Log.d(TAG, "✅ Bluetooth LE supported")

    override fun onCreate() {

        super.onCreate()import android.os.IBinder        

        Log.d(TAG, "🚀 BleRelayService starting...")

        createNotificationChannel()import android.os.Looper        // Initialize BLE components

        initializeBluetooth()

    }import android.os.ParcelUuid        bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {import android.util.Log        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        startForeground(NOTIFICATION_ID, createNotification())

        import androidx.core.app.ActivityCompat        

        val messagePayload = intent?.getStringExtra("emergency_message")

        if (messagePayload != null) {import androidx.core.app.NotificationCompat        if (bluetoothLeAdvertiser == null) {

            Log.d(TAG, "📤 Sending emergency message: $messagePayload")

            broadcastEmergencyMessage(messagePayload)import com.emergency.medical.network.DashboardClient            Log.e(TAG, "❌ BluetoothLeAdvertiser is null - advertising will fail")

        }

        import com.emergency.medical.data.EmergencyPayload        } else {

        startEmergencyServices()

        return START_STICKYimport com.google.gson.Gson            Log.d(TAG, "✅ BluetoothLeAdvertiser initialized")

    }

import java.nio.charset.Charset        }

    private fun createNotificationChannel() {

        val channel = NotificationChannel(import java.util.UUID        

            CHANNEL_ID,

            "Emergency BLE Service",        if (bluetoothLeScanner == null) {

            NotificationManager.IMPORTANCE_LOW

        ).apply {class BleRelayService : Service() {            Log.e(TAG, "❌ BluetoothLeScanner is null - scanning will fail")

            description = "Emergency Bluetooth LE mesh networking"

        }        } else {

        

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager    companion object {            Log.d(TAG, "✅ BluetoothLeScanner initialized")

        notificationManager.createNotificationChannel(channel)

    }        private const val TAG = "BleRelayService"        }



    private fun createNotification(): Notification {                

        return NotificationCompat.Builder(this, CHANNEL_ID)

            .setContentTitle("Emergency BLE Active")        // Notification constants        // Check permissions

            .setContentText("Scanning and advertising for emergency devices")

            .setSmallIcon(android.R.drawable.ic_dialog_info)        private const val CHANNEL_ID = "ble_relay_channel"        checkBluetoothPermissions()

            .build()

    }        private const val NOTIFICATION_ID = 1        



    private fun initializeBluetooth() {                // Create notification channel

        Log.d(TAG, "🔍 Initializing Bluetooth...")

                // Intent extras        createNotificationChannel()

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        bluetoothAdapter = bluetoothManager.adapter        const val EXTRA_PAYLOAD = "MESSAGE_PAYLOAD"        

        

        if (bluetoothAdapter == null) {        const val EXTRA_SEND_MESSAGE = "EXTRA_SEND_MESSAGE"        // Setup GATT server

            Log.e(TAG, "❌ Bluetooth not supported!")

            return        const val EXTRA_STATUS_MESSAGE = "STATUS_MESSAGE"        setupGattServer()oid.app.NotificationChannel

        }

                const val EXTRA_STATUS_COLOR = "STATUS_COLOR"import android.app.NotificationManager

        if (!bluetoothAdapter!!.isEnabled) {

            Log.e(TAG, "❌ Bluetooth disabled!")        const val ACTION_BLE_STATUS = "ACTION_BLE_STATUS"import android.app.Service

            return

        }        import android.bluetooth.BluetoothAdapter

        

        bluetoothLeAdvertiser = bluetoothAdapter!!.bluetoothLeAdvertiser        // BLE Service and Characteristic UUIDs for Emergency Relayimport android.bluetooth.BluetoothDevice

        bluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner

                private val EMERGENCY_SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789ABC")import android.bluetooth.BluetoothGatt

        Log.d(TAG, "✅ Bluetooth initialized successfully")

    }        private val EMERGENCY_CHARACTERISTIC_UUID = UUID.fromString("87654321-4321-4321-4321-CBA987654321")import android.bluetooth.BluetoothGattCallback



    private fun startEmergencyServices() {        import android.bluetooth.BluetoothGattCharacteristic

        Log.d(TAG, "🔄 Starting emergency services...")

        startAdvertising()        // Message constantsimport android.bluetooth.BluetoothGattServer

        startScanning()

    }        private const val MAX_MESSAGE_LENGTH = 512import android.bluetooth.BluetoothGattServerCallback



    private fun startAdvertising() {    }import android.bluetooth.BluetoothGattService

        if (isAdvertising) {

            Log.d(TAG, "📡 Already advertising")import android.bluetooth.BluetoothManager

            return

        }    // Bluetooth componentsimport android.bluetooth.BluetoothProfile

        

        if (bluetoothLeAdvertiser == null) {    private var bluetoothAdapter: BluetoothAdapter? = nullimport android.bluetooth.le.AdvertiseCallback

            Log.e(TAG, "❌ Advertiser not available")

            return    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = nullimport android.bluetooth.le.AdvertiseData

        }

            private var bluetoothLeScanner: BluetoothLeScanner? = nullimport android.bluetooth.le.AdvertiseSettings

        if (!checkBluetoothPermissions()) return

            private var bluetoothGattServer: BluetoothGattServer? = nullimport android.bluetooth.le.BluetoothLeAdvertiser

        Log.d(TAG, "📡 Starting BLE advertising...")

            private var isAdvertising = falseimport android.bluetooth.le.BluetoothLeScanner

        val settings = AdvertiseSettings.Builder()

            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)    private var isScanning = falseimport android.bluetooth.le.ScanCallback

            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)

            .setConnectable(true)    import android.bluetooth.le.ScanFilter

            .build()

    // Message handlingimport android.bluetooth.le.ScanResult

        val data = AdvertiseData.Builder()

            .setIncludeDeviceName(true)    private var messagePayload: ByteArray = byteArrayOf(0x53, 0x4F, 0x53, 0x21) // default "SOS!"import android.bluetooth.le.ScanSettings

            .setIncludeTxPowerLevel(false)

            .addServiceUuid(android.os.ParcelUuid(EMERGENCY_SERVICE_UUID))    private val receivedMessages = mutableSetOf<String>()import android.content.Context

            .build()

    private val connectedDevices = mutableMapOf<String, BluetoothGatt>()import android.content.Intent

        bluetoothLeAdvertiser!!.startAdvertising(settings, data, advertiseCallback)

    }    private val discoveredDevices = mutableSetOf<String>()import android.content.SharedPreferences



    private fun startScanning() {    private val myMessageIds = mutableSetOf<String>()import android.content.pm.PackageManager

        if (isScanning) {

            Log.d(TAG, "🔍 Already scanning")    import android.net.ConnectivityManager

            return

        }    private lateinit var sharedPreferences: SharedPreferencesimport android.net.NetworkCapabilities

        

        if (bluetoothLeScanner == null) {    private lateinit var dashboardClient: DashboardClientimport android.os.Build

            Log.e(TAG, "❌ Scanner not available")

            return    private val mainHandler = Handler(Looper.getMainLooper())import android.os.Handler

        }

        import android.os.IBinder

        if (!checkBluetoothPermissions()) return

            override fun onCreate() {import android.os.Looper

        Log.d(TAG, "🔍 Starting BLE scanning...")

                super.onCreate()import android.os.ParcelUuid

        val filter = ScanFilter.Builder()

            .setServiceUuid(android.os.ParcelUuid(EMERGENCY_SERVICE_UUID))        Log.d(TAG, "🚀 BleRelayService created")import android.util.Log

            .build()

                import androidx.core.app.ActivityCompat

        val settings = ScanSettings.Builder()

            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)        sharedPreferences = getSharedPreferences("BleRelay", Context.MODE_PRIVATE)import androidx.core.app.NotificationCompat

            .build()

        dashboardClient = DashboardClient.getInstance()import com.emergency.medical.network.DashboardClient

        bluetoothLeScanner!!.startScan(listOf(filter), settings, scanCallback)

        isScanning = true        import com.emergency.medical.data.EmergencyPayload

        Log.d(TAG, "✅ Scanning started successfully")

    }        initializeBluetooth()import com.google.gson.Gson



    private val advertiseCallback = object : AdvertiseCallback() {    }import com.google.gson.reflect.TypeToken

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {

            super.onStartSuccess(settingsInEffect)import java.nio.charset.Charset

            isAdvertising = true

            Log.d(TAG, "✅ Advertising started successfully")    private fun initializeBluetooth() {import java.util.UUID

        }

        Log.d(TAG, "🔍 Initializing Bluetooth...")

        override fun onStartFailure(errorCode: Int) {

            super.onStartFailure(errorCode)        class BleRelayService : Service() {

            isAdvertising = false

            Log.e(TAG, "❌ Advertising failed with error: $errorCode")        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        }

    }        bluetoothAdapter = bluetoothManager.adapter    companion object {



    private val scanCallback = object : ScanCallback() {                private const val TAG = "BleRelayService"

        override fun onScanResult(callbackType: Int, result: ScanResult?) {

            super.onScanResult(callbackType, result)        if (bluetoothAdapter == null) {        

            result?.let { scanResult ->

                val device = scanResult.device            Log.e(TAG, "❌ CRITICAL: Bluetooth not supported!")        // Notification constants

                Log.d(TAG, "🔍 Found emergency device: ${device.address}")

                            return        private const val CHANNEL_ID = "ble_relay_channel"

                if (!discoveredDevices.containsKey(device.address)) {

                    discoveredDevices[device.address] = device        }        private const val NOTIFICATION_ID = 1

                    Log.d(TAG, "📱 Connecting to new device: ${device.address}")

                    connectToDevice(device)                

                }

            }        if (!bluetoothAdapter!!.isEnabled) {        // Intent extras

        }

            Log.e(TAG, "❌ BLUETOOTH IS DISABLED!")        const val EXTRA_PAYLOAD = "MESSAGE_PAYLOAD"

        override fun onScanFailed(errorCode: Int) {

            super.onScanFailed(errorCode)            Log.e(TAG, "📱 Please enable Bluetooth in settings")        const val EXTRA_SEND_MESSAGE = "EXTRA_SEND_MESSAGE"

            isScanning = false

            Log.e(TAG, "❌ Scan failed with error: $errorCode")            return        const val EXTRA_STATUS_MESSAGE = "STATUS_MESSAGE"

        }

    }        }        const val EXTRA_STATUS_COLOR = "STATUS_COLOR"



    private fun connectToDevice(device: BluetoothDevice) {                const val ACTION_BLE_STATUS = "ACTION_BLE_STATUS"

        Log.d(TAG, "🔗 Connecting to device: ${device.address}")

                Log.d(TAG, "✅ Bluetooth enabled")        

        if (!checkBluetoothPermissions()) return

                        // BLE Service and Characteristic UUIDs for Emergency Relay

        device.connectGatt(this, false, gattCallback)

    }        // Initialize BLE components        private val EMERGENCY_SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789ABC")



    private val gattCallback = object : BluetoothGattCallback() {        bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser        private val EMERGENCY_CHARACTERISTIC_UUID = UUID.fromString("87654321-4321-4321-4321-CBA987654321")

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {

            super.onConnectionStateChange(gatt, status, newState)        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner        

            

            when (newState) {                // Message constants

                BluetoothProfile.STATE_CONNECTED -> {

                    Log.d(TAG, "✅ Connected to ${gatt?.device?.address}")        if (bluetoothLeAdvertiser == null) {        private const val MAX_MESSAGE_LENGTH = 512 // BLE characteristic max safe size

                    gatt?.let { 

                        connectedDevices[it.device.address] = it            Log.e(TAG, "❌ BLE Advertiser not available")    }

                        it.discoverServices()

                    }        }

                }

                BluetoothProfile.STATE_DISCONNECTED -> {            // Properties

                    Log.d(TAG, "❌ Disconnected from ${gatt?.device?.address}")

                    gatt?.let { connectedDevices.remove(it.device.address) }        if (bluetoothLeScanner == null) {    private var bluetoothAdapter: BluetoothAdapter? = null

                }

            }            Log.e(TAG, "❌ BLE Scanner not available")    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null

        }

        }    private var bluetoothLeScanner: BluetoothLeScanner? = null

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {

            super.onServicesDiscovered(gatt, status)            private var bluetoothGattServer: BluetoothGattServer? = null

            if (status == BluetoothGatt.GATT_SUCCESS) {

                Log.d(TAG, "✅ Services discovered for ${gatt?.device?.address}")        createNotificationChannel()    private var isAdvertising = false

            }

        }    }    private var isScanning = false



        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {    

            super.onCharacteristicRead(gatt, characteristic, status)

            if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {    // Message handling

                val messageData = String(characteristic.value, Charset.forName("UTF-8"))

                Log.d(TAG, "📨 Received message: $messageData")        Log.d(TAG, "📞 Service started")    private var messagePayload: ByteArray = byteArrayOf(0x53, 0x4F, 0x53, 0x21) // default "SOS!"

                handleReceivedMessage(messageData)

            }            private val receivedMessages = mutableSetOf<String>() // Track to prevent loops

        }

    }        startForegroundService()    private val connectedDevices = mutableMapOf<String, BluetoothGatt>() // Track connected devices



    private fun handleReceivedMessage(messageData: String) {            private val discoveredDevices = mutableSetOf<String>() // Track discovered app devices

        try {

            Log.d(TAG, "🔄 Processing received message...")        when (intent?.action) {    private val myMessageIds = mutableSetOf<String>() // Track our own messages to prevent loops

            

            // Parse the message            "SEND_MESSAGE" -> {    private lateinit var sharedPreferences: SharedPreferences

            val payload = parseMessageToPayload(messageData)

                            val messageBytes = intent.getByteArrayExtra(EXTRA_PAYLOAD)    private lateinit var dashboardClient: DashboardClient

            // Check for message loop prevention

            val messageId = "${payload.timestamp}_${payload.phone}"                val isDirectMessage = intent.getBooleanExtra(EXTRA_SEND_MESSAGE, false)    private val mainHandler = Handler(Looper.getMainLooper())

            if (processedMessageIds.contains(messageId)) {

                Log.d(TAG, "🔄 Message already processed, skipping: $messageId")                

                return

            }                if (messageBytes != null) {    override fun onCreate() {

            

            processedMessageIds.add(messageId)                    messagePayload = messageBytes        super.onCreate()

            

            // Forward to dashboard                    Log.d(TAG, "📨 Message updated: ${String(messagePayload, Charset.forName("UTF-8"))}")        Log.d(TAG, "BleRelayService created")

            forwardMessageToDashboard(payload)

                                        

            // Forward to other connected devices

            forwardToConnectedDevices(messageData)                    if (isDirectMessage) {        // Initialize components

            

        } catch (e: Exception) {                        // Track our own messages to prevent loops        sharedPreferences = getSharedPreferences("EmergencyApp", Context.MODE_PRIVATE)

            Log.e(TAG, "❌ Error handling message: ${e.message}")

        }                        trackOwnMessage(messageBytes)        dashboardClient = DashboardClient()

    }

                    }        

    private fun parseMessageToPayload(messageData: String): EmergencyPayload {

        return try {                            // Initialize Bluetooth

            gson.fromJson(messageData, EmergencyPayload::class.java)

        } catch (e: Exception) {                    startAdvertising()        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

            // Fallback for legacy format

            EmergencyPayload(                    startScanning()        bluetoothAdapter = bluetoothManager.adapter

                timestamp = System.currentTimeMillis(),

                phone = "Unknown",                    startPeriodicConnection()        

                currentMedicalIssue = messageData,

                latitude = 0.0,                }        // Check if Bluetooth is supported and enabled

                longitude = 0.0

            )            }        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {

        }

    }            "STOP_SERVICE" -> {            Log.e(TAG, "Bluetooth not available or not enabled")



    private fun forwardMessageToDashboard(payload: EmergencyPayload) {                stopBleOperations()            return

        dashboardClient.sendEmergency(payload, object : DashboardClient.EmergencyCallback {

            override fun onSuccess() {                stopSelf()        }

                Log.d(TAG, "✅ Message forwarded to dashboard successfully")

            }            }        



            override fun onFailure(error: String) {            else -> {        // Initialize BLE components

                Log.e(TAG, "❌ Failed to forward to dashboard: $error")

            }                startAdvertising()        bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser

        })

    }                startScanning()        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner



    private fun forwardToConnectedDevices(messageData: String) {                startPeriodicConnection()        

        Log.d(TAG, "📡 Forwarding message to ${connectedDevices.size} connected devices")

                    }        // Create notification channel

        connectedDevices.values.forEach { gatt ->

            try {        }        createNotificationChannel()

                val service = gatt.getService(EMERGENCY_SERVICE_UUID)

                val characteristic = service?.getCharacteristic(EMERGENCY_CHARACTERISTIC_UUID)                

                

                if (characteristic != null) {        return START_STICKY        // Setup GATT server

                    characteristic.value = messageData.toByteArray(Charset.forName("UTF-8"))

                    if (checkBluetoothPermissions()) {    }        setupGattServer()

                        gatt.writeCharacteristic(characteristic)

                        Log.d(TAG, "📤 Message sent to ${gatt.device.address}")    }

                    }

                }    private fun trackOwnMessage(messageBytes: ByteArray) {

            } catch (e: Exception) {

                Log.e(TAG, "❌ Error forwarding to ${gatt.device.address}: ${e.message}")        try {    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

            }

        }            val messageString = String(messageBytes, Charset.forName("UTF-8"))        Log.d(TAG, "BleRelayService started with intent: $intent")

    }

            val payload = Gson().fromJson(messageString, EmergencyPayload::class.java)        

    private fun broadcastEmergencyMessage(messagePayload: String) {

        Log.d(TAG, "📡 Broadcasting emergency message to network...")            payload?.messageId?.let { messageId ->        // Start as foreground service

        forwardToConnectedDevices(messagePayload)

    }                myMessageIds.add(messageId)        startForegroundService()



    private fun checkBluetoothPermissions(): Boolean {                Log.d(TAG, "🔒 Tracked own message: $messageId")        

        val permissions = listOf(

            Manifest.permission.BLUETOOTH,            }        when (intent?.action) {

            Manifest.permission.BLUETOOTH_ADMIN,

            Manifest.permission.ACCESS_FINE_LOCATION,        } catch (e: Exception) {            "SEND_MESSAGE" -> {

            Manifest.permission.BLUETOOTH_ADVERTISE,

            Manifest.permission.BLUETOOTH_SCAN,            Log.w(TAG, "Could not track message ID: $e")                Log.d(TAG, "Received SEND_MESSAGE action")

            Manifest.permission.BLUETOOTH_CONNECT

        )        }                val messageBytes = intent.getByteArrayExtra(EXTRA_PAYLOAD)

        

        return permissions.all { permission ->    }                val isDirectMessage = intent.getBooleanExtra(EXTRA_SEND_MESSAGE, false)

            ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

        }                

    }

    private fun startAdvertising() {                Log.d(TAG, "Message bytes size: ${messageBytes?.size}, isDirectMessage: $isDirectMessage")

    override fun onDestroy() {

        super.onDestroy()        if (isAdvertising) {                

        Log.d(TAG, "🛑 BleRelayService stopping...")

                    Log.d(TAG, "Already advertising")                if (messageBytes != null) {

        if (isAdvertising && bluetoothLeAdvertiser != null && checkBluetoothPermissions()) {

            bluetoothLeAdvertiser!!.stopAdvertising(advertiseCallback)            return                    messagePayload = messageBytes

            isAdvertising = false

        }        }                    Log.d(TAG, "Updated message payload: ${String(messagePayload, Charset.forName("UTF-8"))}")

        

        if (isScanning && bluetoothLeScanner != null && checkBluetoothPermissions()) {                    

            bluetoothLeScanner!!.stopScan(scanCallback)

            isScanning = false        if (bluetoothLeAdvertiser == null) {                    if (isDirectMessage) {

        }

                    Log.e(TAG, "❌ BLE Advertiser not available")                        Log.d(TAG, "This is a direct message from our device, adding to myMessageIds to prevent loop")

        connectedDevices.values.forEach { gatt ->

            gatt.close()            return                        // Track this as our own message to prevent forwarding loops

        }

        connectedDevices.clear()        }                        val messageString = String(messagePayload, Charset.forName("UTF-8"))

        

        bluetoothGattServer?.close()                        try {

        

        Log.d(TAG, "✅ BleRelayService stopped successfully")        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {                            val payload = Gson().fromJson(messageString, EmergencyPayload::class.java)

    }

            Log.e(TAG, "❌ BLUETOOTH_ADVERTISE permission denied")                            if (payload?.messageId != null) {

    override fun onBind(intent: Intent?): IBinder? = null

}            return                                myMessageIds.add(payload.messageId)

        }                                Log.d(TAG, "Added message ID to myMessageIds: ${payload.messageId}")

                            }

        Log.d(TAG, "📡 Starting BLE advertising...")                        } catch (e: Exception) {

                            Log.w(TAG, "Could not parse message as JSON for ID extraction: $e")

        val settings = AdvertiseSettings.Builder()                        }

            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)                    }

            .setConnectable(true)                    

            .setTimeout(0)                    // Start advertising and scanning

            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)                    startAdvertising()

            .build()                    startScanning()

                    

        val data = AdvertiseData.Builder()                    // Start periodic device connection attempts

            .setIncludeDeviceName(true)                    startPeriodicDeviceConnection()

            .setIncludeTxPowerLevel(true)                } else {

            .addServiceUuid(ParcelUuid(EMERGENCY_SERVICE_UUID))                    Log.w(TAG, "No message payload provided")

            .build()                }

            }

        bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)            "STOP_SERVICE" -> {

    }                Log.d(TAG, "Received STOP_SERVICE action")

                stopBleOperations()

    private val advertiseCallback = object : AdvertiseCallback() {                stopSelf()

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {            }

            isAdvertising = true            else -> {

            Log.d(TAG, "✅ BLE advertising started successfully!")                Log.d(TAG, "Starting BLE operations with default message")

        }                // Default start - begin advertising and scanning

                startAdvertising()

        override fun onStartFailure(errorCode: Int) {                startScanning()

            isAdvertising = false                startPeriodicDeviceConnection()

            Log.e(TAG, "❌ BLE advertising failed: $errorCode")            }

        }        }

    }        

        return START_STICKY

    private fun startScanning() {    }

        if (isScanning) {

            Log.d(TAG, "Already scanning")    override fun onDestroy() {

            return        Log.d(TAG, "BleRelayService destroyed")

        }        stopBleOperations()

        bluetoothGattServer?.close()

        if (bluetoothLeScanner == null) {        super.onDestroy()

            Log.e(TAG, "❌ BLE Scanner not available")    }

            return

        }    override fun onBind(intent: Intent?): IBinder? {

        return null // Not a bound service

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {    }

            Log.e(TAG, "❌ BLUETOOTH_SCAN permission denied")

            return    private fun createNotificationChannel() {

        }        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(

        Log.d(TAG, "🔍 Starting BLE scanning...")                CHANNEL_ID,

                "BLE Emergency Relay",

        val settings = ScanSettings.Builder()                NotificationManager.IMPORTANCE_LOW

            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)            ).apply {

            .build()                description = "Emergency message relay via Bluetooth"

                setSound(null, null)

        val filter = ScanFilter.Builder()                enableVibration(false)

            .setServiceUuid(ParcelUuid(EMERGENCY_SERVICE_UUID))            }

            .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        bluetoothLeScanner?.startScan(listOf(filter), settings, scanCallback)            notificationManager.createNotificationChannel(channel)

        isScanning = true        }

    }    }



    private val scanCallback = object : ScanCallback() {    private fun startForegroundService() {

        override fun onScanResult(callbackType: Int, result: ScanResult?) {        val notification = createNotification()

            result?.let { scanResult ->        startForeground(NOTIFICATION_ID, notification)

                val device = scanResult.device    }

                val deviceAddress = device.address

                val deviceName = device.name ?: "Unknown"    private fun createNotification(): Notification {

                val rssi = scanResult.rssi        return NotificationCompat.Builder(this, CHANNEL_ID)

                            .setContentTitle("Emergency BLE Relay Active")

                Log.d(TAG, "🎯 FOUND EMERGENCY DEVICE!")            .setContentText("Relaying emergency messages via Bluetooth")

                Log.d(TAG, "📱 Device: $deviceName ($deviceAddress)")            .setSmallIcon(android.R.drawable.ic_dialog_info)

                Log.d(TAG, "📶 Signal: ${rssi}dBm")            .setPriority(NotificationCompat.PRIORITY_LOW)

                            .setOngoing(true)

                if (!discoveredDevices.contains(deviceAddress) && !connectedDevices.containsKey(deviceAddress)) {            .setSilent(true)

                    discoveredDevices.add(deviceAddress)            .build()

                    Log.d(TAG, "➕ Added new device: $deviceAddress")    }

                    connectToDevice(device)

                }    private fun setupGattServer() {

            }        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        }        bluetoothGattServer = bluetoothManager.openGattServer(this, gattServerCallback)



        override fun onScanFailed(errorCode: Int) {        // Create the emergency service

            isScanning = false        val service = BluetoothGattService(

            Log.e(TAG, "❌ BLE scan failed: $errorCode")            EMERGENCY_SERVICE_UUID,

        }            BluetoothGattService.SERVICE_TYPE_PRIMARY

    }        )



    private fun connectToDevice(device: BluetoothDevice) {        // Create the emergency characteristic

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {        val characteristic = BluetoothGattCharacteristic(

            Log.e(TAG, "❌ BLUETOOTH_CONNECT permission denied")            EMERGENCY_CHARACTERISTIC_UUID,

            return            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,

        }            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE

        )

        Log.d(TAG, "🔗 Connecting to ${device.address}...")

        device.connectGatt(this, false, gattCallback)        service.addCharacteristic(characteristic)

    }        bluetoothGattServer?.addService(service)

        

    private val gattCallback = object : BluetoothGattCallback() {        Log.d(TAG, "GATT server setup complete with emergency service")

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {    }

            when (newState) {

                BluetoothProfile.STATE_CONNECTED -> {    private val gattServerCallback = object : BluetoothGattServerCallback() {

                    val deviceAddress = gatt?.device?.address ?: "Unknown"        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {

                    connectedDevices[deviceAddress] = gatt!!            super.onConnectionStateChange(device, status, newState)

                    Log.d(TAG, "✅ CONNECTED to $deviceAddress")            

                                val deviceAddress = device?.address ?: "unknown"

                    if (ActivityCompat.checkSelfPermission(this@BleRelayService, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {            Log.d(TAG, "GATT Server connection state changed for device $deviceAddress: status=$status, newState=$newState")

                        gatt.discoverServices()            

                    }            when (newState) {

                }                BluetoothProfile.STATE_CONNECTED -> {

                BluetoothProfile.STATE_DISCONNECTED -> {                    Log.d(TAG, "Device $deviceAddress connected to our GATT server")

                    val deviceAddress = gatt?.device?.address ?: "Unknown"                }

                    connectedDevices.remove(deviceAddress)                BluetoothProfile.STATE_DISCONNECTED -> {

                    Log.d(TAG, "❌ DISCONNECTED from $deviceAddress")                    Log.d(TAG, "Device $deviceAddress disconnected from our GATT server")

                }                }

            }            }

        }        }



        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {        override fun onCharacteristicWriteRequest(

            if (status == BluetoothGatt.GATT_SUCCESS) {            device: BluetoothDevice?,

                Log.d(TAG, "🔍 Services discovered, ready to send message")            requestId: Int,

                sendMessageToDevice(gatt!!)            characteristic: BluetoothGattCharacteristic?,

            }            preparedWrite: Boolean,

        }            responseNeeded: Boolean,

    }            offset: Int,

            value: ByteArray?

    private fun sendMessageToDevice(gatt: BluetoothGatt) {        ) {

        val service = gatt.getService(EMERGENCY_SERVICE_UUID) ?: return            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)

        val characteristic = service.getCharacteristic(EMERGENCY_CHARACTERISTIC_UUID) ?: return            

                    val deviceAddress = device?.address ?: "unknown"

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {            Log.d(TAG, "Received characteristic write request from device $deviceAddress")

            return            

        }            if (characteristic?.uuid == EMERGENCY_CHARACTERISTIC_UUID && value != null) {

                Log.d(TAG, "Received emergency message from device $deviceAddress: ${String(value, Charset.forName("UTF-8"))}")

        characteristic.value = messagePayload                

        gatt.writeCharacteristic(characteristic)                // Process the received message

        Log.d(TAG, "📤 Message sent to ${gatt.device.address}")                handleReceivedMessage(value, deviceAddress)

    }                

                // Send response if needed

    private fun handleReceivedMessage(messageString: String) {                if (responseNeeded) {

        Log.d(TAG, "📨 Received message: $messageString")                    bluetoothGattServer?.sendResponse(

                                device,

        // Prevent loops                        requestId,

        if (receivedMessages.contains(messageString)) {                        BluetoothGatt.GATT_SUCCESS,

            Log.d(TAG, "⏭️ Duplicate message ignored")                        0,

            return                        byteArrayOf()

        }                    )

                        }

        receivedMessages.add(messageString)            }

                }

        // Parse message

        val payload = parseMessageToPayload(messageString)        override fun onCharacteristicReadRequest(

                    device: BluetoothDevice?,

        // Check if this is our own message            requestId: Int,

        if (payload?.messageId != null && myMessageIds.contains(payload.messageId)) {            offset: Int,

            Log.d(TAG, "🔒 This is our own message, not forwarding")            characteristic: BluetoothGattCharacteristic?

            return        ) {

        }            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)

                    

        // Forward to dashboard            val deviceAddress = device?.address ?: "unknown"

        payload?.let { forwardMessageToDashboard(it) }            Log.d(TAG, "Received characteristic read request from device $deviceAddress")

                    

        // Relay to other devices            if (characteristic?.uuid == EMERGENCY_CHARACTERISTIC_UUID) {

        relayMessageToConnectedDevices(messageString)                // Send our current message as response

    }                bluetoothGattServer?.sendResponse(

                    device,

    private fun parseMessageToPayload(message: String): EmergencyPayload? {                    requestId,

        return try {                    BluetoothGatt.GATT_SUCCESS,

            Gson().fromJson(message, EmergencyPayload::class.java)                    0,

        } catch (e: Exception) {                    messagePayload

            Log.w(TAG, "Failed to parse as JSON: $e")                )

            // Fallback for legacy format                Log.d(TAG, "Sent message to device $deviceAddress: ${String(messagePayload, Charset.forName("UTF-8"))}")

            EmergencyPayload(            }

                name = "Unknown",        }

                age = null,    }

                phone = null,

                bloodGroup = null,    private fun handleReceivedMessage(messageBytes: ByteArray, fromDevice: String) {

                phoneBattery = null,        val messageString = String(messageBytes, Charset.forName("UTF-8"))

                latitude = 0.0,        Log.d(TAG, "Processing received message: $messageString")

                longitude = 0.0,        

                message = message,        // Check if we've already seen this message to prevent loops

                currentMedicalIssue = null,        val messageHash = messageString.hashCode().toString()

                timestamp = System.currentTimeMillis().toString(),        if (receivedMessages.contains(messageHash)) {

                messageId = UUID.randomUUID().toString()            Log.d(TAG, "Message already received, skipping: $messageHash")

            )            return

        }        }

    }        

        // Parse the message to get payload details

    private fun forwardMessageToDashboard(payload: EmergencyPayload) {        val payload = parseMessageToPayload(messageString)

        Log.d(TAG, "🌐 Forwarding to dashboard...")        

                // Check if this is one of our own messages (prevent forwarding our own messages)

        if (!isNetworkAvailable()) {        if (payload?.messageId != null && myMessageIds.contains(payload.messageId)) {

            Log.w(TAG, "No network available")            Log.d(TAG, "This is our own message (ID: ${payload.messageId}), not forwarding")

            return            return

        }        }

                

        dashboardClient.sendRelayMessage(payload, object : DashboardClient.EmergencyCallback {        // Add to received messages to prevent loops

            override fun onSuccess() {        receivedMessages.add(messageHash)

                Log.d(TAG, "✅ Dashboard forward successful")        Log.d(TAG, "Added message to received set: $messageHash")

            }        

                    // Forward to dashboard

            override fun onFailure(error: String) {        forwardMessageToDashboard(payload ?: EmergencyPayload(

                Log.e(TAG, "❌ Dashboard forward failed: $error")            name = "Unknown",

            }            age = null,

        })            phone = null,

    }            bloodGroup = null,

            phoneBattery = null,

    private fun relayMessageToConnectedDevices(message: String) {            latitude = 0.0,

        Log.d(TAG, "📡 Relaying to ${connectedDevices.size} connected devices")            longitude = 0.0,

        // Implementation would send message to all connected devices            message = messageString,

    }            currentMedicalIssue = null,

            timestamp = System.currentTimeMillis().toString(),

    private fun startPeriodicConnection() {            messageId = UUID.randomUUID().toString()

        mainHandler.postDelayed(object : Runnable {        ))

            override fun run() {        

                Log.d(TAG, "📊 Status: Connected=${connectedDevices.size}, Discovered=${discoveredDevices.size}")        // Relay to other connected devices

                        relayMessageToConnectedDevices(messageBytes, fromDevice)

                if (connectedDevices.isEmpty() && discoveredDevices.isEmpty()) {    }

                    Log.w(TAG, "⚠️ No devices found - check Bluetooth permissions")

                }    private fun parseMessageToPayload(message: String): EmergencyPayload? {

                        return try {

                // Restart advertising/scanning if stopped            Log.d(TAG, "Attempting to parse message as JSON: $message")

                if (!isAdvertising) startAdvertising()            val payload = Gson().fromJson(message, EmergencyPayload::class.java)

                if (!isScanning) startScanning()            Log.d(TAG, "Successfully parsed JSON payload: ${payload}")

                            payload

                mainHandler.postDelayed(this, 30000)        } catch (e: Exception) {

            }            Log.d(TAG, "Failed to parse as JSON, treating as legacy format: $e")

        }, 5000)            

    }            // Handle legacy "SOS:name:lat,lon" format

            if (message.startsWith("SOS:") && message.contains(":")) {

    private fun isNetworkAvailable(): Boolean {                try {

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager                    val parts = message.split(":")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {                    if (parts.size >= 3) {

            val network = connectivityManager.activeNetwork ?: return false                        val name = parts[1]

            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false                        val location = parts[2]

            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||                        val coords = location.split(",")

                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)                        if (coords.size == 2) {

        } else {                            return EmergencyPayload(

            @Suppress("DEPRECATION")                                name = name,

            return connectivityManager.activeNetworkInfo?.isConnected == true                                age = null,

        }                                phone = null,

    }                                bloodGroup = null,

                                phoneBattery = null,

    private fun createNotificationChannel() {                                latitude = coords[0].toDoubleOrNull() ?: 0.0,

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {                                longitude = coords[1].toDoubleOrNull() ?: 0.0,

            val channel = NotificationChannel(                                message = "Emergency SOS",

                CHANNEL_ID,                                currentMedicalIssue = null,

                "Emergency BLE Relay",                                timestamp = System.currentTimeMillis().toString(),

                NotificationManager.IMPORTANCE_LOW                                messageId = UUID.randomUUID().toString()

            )                            )

            val notificationManager = getSystemService(NotificationManager::class.java)                        }

            notificationManager?.createNotificationChannel(channel)                    }

        }                } catch (e: Exception) {

    }                    Log.w(TAG, "Failed to parse legacy format: $e")

                }

    private fun startForegroundService() {            }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)            

            .setContentTitle("Emergency BLE Relay")            // Fallback: treat as plain message

            .setContentText("Scanning for emergency devices...")            EmergencyPayload(

            .setSmallIcon(android.R.drawable.ic_dialog_info)                name = "Unknown",

            .build()                age = null,

                phone = null,

        startForeground(NOTIFICATION_ID, notification)                bloodGroup = null,

    }                phoneBattery = null,

                latitude = 0.0,

    private fun stopBleOperations() {                longitude = 0.0,

        Log.d(TAG, "🛑 Stopping BLE operations")                message = message,

                        currentMedicalIssue = null,

        if (isAdvertising && bluetoothLeAdvertiser != null) {                timestamp = System.currentTimeMillis().toString(),

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {                messageId = UUID.randomUUID().toString()

                bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)            )

            }        }

            isAdvertising = false    }

        }

    private fun forwardMessageToDashboard(payload: EmergencyPayload) {

        if (isScanning && bluetoothLeScanner != null) {        Log.d(TAG, "Forwarding message to dashboard...")

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {        Log.d(TAG, "Payload details:")

                bluetoothLeScanner?.stopScan(scanCallback)        Log.d(TAG, "  Message ID: ${payload.messageId}")

            }        Log.d(TAG, "  Message: ${payload.message}")

            isScanning = false        Log.d(TAG, "  Name: ${payload.name}")

        }        Log.d(TAG, "  Location: ${payload.latitude}, ${payload.longitude}")

        Log.d(TAG, "  Timestamp: ${payload.timestamp}")

        connectedDevices.values.forEach { gatt ->        Log.d(TAG, "  Phone: ${payload.phone}")

            try {        Log.d(TAG, "  Medical Issue: ${payload.currentMedicalIssue}")

                gatt.close()        

            } catch (e: Exception) {        // Check if we have internet connectivity

                Log.e(TAG, "Error closing GATT: $e")        if (!isNetworkAvailable()) {

            }            Log.w(TAG, "No network connectivity available for dashboard forwarding")

        }            return

        connectedDevices.clear()        }

                

        Log.d(TAG, "✅ BLE operations stopped")        // Send to dashboard API

    }        try {

            dashboardClient.sendRelayMessage(payload, object : DashboardClient.EmergencyCallback {

    override fun onDestroy() {                override fun onSuccess() {

        Log.d(TAG, "🔥 Service destroyed")                    mainHandler.post {

        stopBleOperations()                        Log.d(TAG, "Successfully forwarded message to dashboard")

        bluetoothGattServer?.close()                    }

        super.onDestroy()                }

    }                

                override fun onFailure(error: String) {

    override fun onBind(intent: Intent?): IBinder? {                    mainHandler.post {

        return null                        Log.e(TAG, "Failed to forward message to dashboard: $error")

    }                    }

}                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Exception while forwarding to dashboard: $e")
        }
    }

    private fun relayMessageToConnectedDevices(messageBytes: ByteArray, excludeDevice: String) {
        Log.d(TAG, "Relaying message to ${connectedDevices.size} connected devices (excluding $excludeDevice)")
        
        connectedDevices.forEach { (deviceAddress, gatt) ->
            if (deviceAddress != excludeDevice) {
                try {
                    Log.d(TAG, "Attempting to relay message to device: $deviceAddress")
                    val service = gatt.getService(EMERGENCY_SERVICE_UUID)
                    val characteristic = service?.getCharacteristic(EMERGENCY_CHARACTERISTIC_UUID)
                    
                    if (characteristic != null) {
                        characteristic.value = messageBytes
                        val writeSuccess = gatt.writeCharacteristic(characteristic)
                        Log.d(TAG, "Write characteristic result for device $deviceAddress: $writeSuccess")
                    } else {
                        Log.w(TAG, "Emergency characteristic not found for device $deviceAddress")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to relay message to device $deviceAddress: $e")
                }
            }
        }
    }

    private fun startAdvertising() {
        if (isAdvertising) {
            Log.d(TAG, "Already advertising - skipping")
            return
        }

        if (bluetoothLeAdvertiser == null) {
            Log.e(TAG, "❌ BluetoothLeAdvertiser not available - Bluetooth might be disabled")
            Log.e(TAG, "📱 Please check: Settings → Bluetooth → Turn ON")
            return
        }

        // Check permissions with detailed logging
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "❌ BLUETOOTH_ADVERTISE permission not granted - advertising will fail!")
            Log.e(TAG, "Please grant Bluetooth permissions in app settings")
            return
        }

        Log.d(TAG, "📡 Starting BLE advertising...")

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0) // Advertise indefinitely
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(true)
            .addServiceUuid(ParcelUuid(EMERGENCY_SERVICE_UUID))
            .build()

        try {
            bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
            Log.d(TAG, "🚀 BLE advertising request sent with emergency service UUID: $EMERGENCY_SERVICE_UUID")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception while starting BLE advertising: $e")
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            isAdvertising = true
            Log.d(TAG, "✅ BLE ADVERTISING STARTED SUCCESSFULLY!")
            Log.d(TAG, "📡 Device is now discoverable by other emergency apps")
            Log.d(TAG, "🆔 Broadcasting emergency service UUID: $EMERGENCY_SERVICE_UUID")
            Log.d(TAG, "⚡ Power level: ${settingsInEffect?.txPowerLevel}")
            Log.d(TAG, "🔄 Mode: ${settingsInEffect?.mode}")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            isAdvertising = false
            val errorMsg = when(errorCode) {
                ADVERTISE_FAILED_ALREADY_STARTED -> "Advertising already started"
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "Advertising data too large"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Advertising feature unsupported"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal error"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
                else -> "Unknown error ($errorCode)"
            }
            Log.e(TAG, "❌ BLE ADVERTISING FAILED: $errorMsg")
            Log.e(TAG, "🔧 Troubleshooting:")
            Log.e(TAG, "   1. Check Bluetooth is ON in phone settings")
            Log.e(TAG, "   2. Check app has Bluetooth permissions")
            Log.e(TAG, "   3. Try turning Bluetooth OFF and ON")
            
            // Retry advertising after delay
            mainHandler.postDelayed({
                if (!isAdvertising) {
                    Log.d(TAG, "🔄 Retrying BLE advertising...")
                    startAdvertising()
                }
            }, 3000)
        }
    }

    private fun startScanning() {
        if (isScanning) {
            Log.d(TAG, "Already scanning - skipping")
            return
        }

        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BluetoothLeScanner not available - Bluetooth might be disabled")
            return
        }

        // Check permissions with detailed logging
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "❌ BLUETOOTH_SCAN permission not granted - scanning will fail!")
            Log.e(TAG, "Please grant Bluetooth permissions in app settings")
            return
        }

        Log.d(TAG, "🔍 Starting BLE scan with LOW_LATENCY mode...")

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setReportDelay(0)
            .build()

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(EMERGENCY_SERVICE_UUID))
            .build()

        try {
            bluetoothLeScanner?.startScan(listOf(filter), settings, scanCallback)
            isScanning = true
            Log.d(TAG, "✅ BLE scanning started successfully for emergency service UUID: $EMERGENCY_SERVICE_UUID")
            Log.d(TAG, "📡 Looking for devices advertising emergency services...")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start BLE scanning: $e")
            isScanning = false
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            
            result?.let { scanResult ->
                val device = scanResult.device
                val deviceAddress = device.address
                val deviceName = device.name ?: "Unknown"
                val rssi = scanResult.rssi
                
                Log.d(TAG, "🎯 FOUND EMERGENCY DEVICE!")
                Log.d(TAG, "  📱 Device: $deviceName ($deviceAddress)")
                Log.d(TAG, "  📶 Signal: ${rssi}dBm")
                Log.d(TAG, "  🆔 Service UUID found: $EMERGENCY_SERVICE_UUID")
                
                // Check if we haven't discovered this device yet
                if (!discoveredDevices.contains(deviceAddress) && !connectedDevices.containsKey(deviceAddress)) {
                    discoveredDevices.add(deviceAddress)
                    Log.d(TAG, "➕ Added new device to discovered list: $deviceAddress")
                    Log.d(TAG, "📊 Total discovered devices: ${discoveredDevices.size}")
                    
                    // Attempt to connect to the device
                    Log.d(TAG, "🔗 Attempting auto-connection to $deviceAddress...")
                    connectToDevice(device)
                } else {
                    Log.d(TAG, "⏭️ Device $deviceAddress already known - skipping")
                }
            } ?: Log.w(TAG, "⚠️ Received null scan result")
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            isScanning = false
            val errorMsg = when(errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE scan feature unsupported"
                SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
                else -> "Unknown error ($errorCode)"
            }
            Log.e(TAG, "❌ BLE SCAN FAILED: $errorMsg")
            Log.e(TAG, "🔄 Will retry scanning in 5 seconds...")
            
            // Retry scanning after a delay
            mainHandler.postDelayed({
                if (!isScanning) {
                    Log.d(TAG, "🔄 Retrying BLE scan...")
                    startScanning()
                }
            }, 5000)
        }
            isScanning = false
            Log.e(TAG, "BLE scan failed with error: $errorCode")
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Log.d(TAG, "Attempting to connect to device: ${device.address}")
        
        // Check permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "BLUETOOTH_CONNECT permission not granted")
            return
        }
        
        try {
            val gatt = device.connectGatt(this, true, gattClientCallback)
            if (gatt != null) {
                Log.d(TAG, "GATT connection initiated for device: ${device.address}")
            } else {
                Log.e(TAG, "Failed to initiate GATT connection for device: ${device.address}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while connecting to device ${device.address}: $e")
        }
    }

    private val gattClientCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            
            val deviceAddress = gatt?.device?.address ?: "unknown"
            Log.d(TAG, "GATT Client connection state changed for device $deviceAddress: status=$status, newState=$newState")
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to device: $deviceAddress")
                    connectedDevices[deviceAddress] = gatt!!
                    
                    // Check permissions before discovering services
                    if (ActivityCompat.checkSelfPermission(this@BleRelayService, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt.discoverServices()
                    } else {
                        Log.e(TAG, "BLUETOOTH_CONNECT permission not granted for service discovery")
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from device: $deviceAddress")
                    connectedDevices.remove(deviceAddress)
                    discoveredDevices.remove(deviceAddress) // Allow rediscovery
                    gatt?.close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            
            val deviceAddress = gatt?.device?.address ?: "unknown"
            Log.d(TAG, "Services discovered for device $deviceAddress with status: $status")
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(EMERGENCY_SERVICE_UUID)
                if (service != null) {
                    Log.d(TAG, "Found emergency service on device $deviceAddress")
                    val characteristic = service.getCharacteristic(EMERGENCY_CHARACTERISTIC_UUID)
                    if (characteristic != null) {
                        Log.d(TAG, "Found emergency characteristic on device $deviceAddress")
                        
                        // Send our current message to the connected device
                        sendMessageToDevice(gatt, characteristic)
                    } else {
                        Log.w(TAG, "Emergency characteristic not found on device $deviceAddress")
                    }
                } else {
                    Log.w(TAG, "Emergency service not found on device $deviceAddress")
                }
            } else {
                Log.e(TAG, "Service discovery failed for device $deviceAddress")
            }
        }
    }

    private fun sendMessageToDevice(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val deviceAddress = gatt.device?.address ?: "unknown"
        
        try {
            characteristic.value = messagePayload
            val writeSuccess = gatt.writeCharacteristic(characteristic)
            Log.d(TAG, "Sent message to device $deviceAddress: $writeSuccess")
            Log.d(TAG, "Message content: ${String(messagePayload, Charset.forName("UTF-8"))}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message to device $deviceAddress: $e")
        }
    }

    private fun startPeriodicDeviceConnection() {
        Log.d(TAG, "Starting periodic device connection checks...")
        
        mainHandler.postDelayed(object : Runnable {
            override fun run() {
                Log.d(TAG, "📊 PERIODIC STATUS CHECK:")
                Log.d(TAG, "  🔗 Connected devices: ${connectedDevices.size}")
                Log.d(TAG, "  🎯 Discovered devices: ${discoveredDevices.size}")
                Log.d(TAG, "  📡 Advertising: $isAdvertising")
                Log.d(TAG, "  🔍 Scanning: $isScanning")
                
                if (connectedDevices.isEmpty() && discoveredDevices.isEmpty()) {
                    Log.w(TAG, "⚠️  NO DEVICES FOUND - Check:")
                    Log.w(TAG, "   1. Is Bluetooth ON?")
                    Log.w(TAG, "   2. Are permissions granted?")
                    Log.w(TAG, "   3. Is another emergency app nearby?")
                }
                
                // Try to reconnect to any discovered devices that aren't currently connected
                discoveredDevices.forEach { deviceAddress ->
                    if (!connectedDevices.containsKey(deviceAddress)) {
                        Log.d(TAG, "🔄 Attempting to reconnect to device: $deviceAddress")
                        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
                        device?.let { connectToDevice(it) }
                    }
                }
                
                // Restart advertising and scanning if they stopped
                if (!isAdvertising) {
                    Log.w(TAG, "📡 Advertising stopped - restarting...")
                    startAdvertising()
                }
                
                if (!isScanning) {
                    Log.w(TAG, "🔍 Scanning stopped - restarting...")
                    startScanning()
                }
                
                // Schedule next check
                mainHandler.postDelayed(this, 30000) // Check every 30 seconds
            }
        }, 5000) // Initial delay of 5 seconds
    }

    private fun stopBleOperations() {
        Log.d(TAG, "Stopping BLE operations...")
        
        // Stop advertising
        if (isAdvertising && bluetoothLeAdvertiser != null) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
                }
                isAdvertising = false
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping advertising: $e")
            }
        }
        
        // Stop scanning
        if (isScanning && bluetoothLeScanner != null) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothLeScanner?.stopScan(scanCallback)
                }
                isScanning = false
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping scan: $e")
            }
        }
        
        // Close GATT connections
        connectedDevices.values.forEach { gatt ->
            try {
                gatt.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing GATT connection: $e")
            }
        }
        connectedDevices.clear()
        
        Log.d(TAG, "BLE operations stopped")
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }
    
    private fun checkBluetoothPermissions() {
        Log.d(TAG, "🔒 Checking Bluetooth permissions...")
        
        val permissions = listOf(
            Manifest.permission.BLUETOOTH_ADVERTISE to "ADVERTISE",
            Manifest.permission.BLUETOOTH_CONNECT to "CONNECT", 
            Manifest.permission.BLUETOOTH_SCAN to "SCAN",
            Manifest.permission.ACCESS_FINE_LOCATION to "FINE_LOCATION"
        )
        
        var allGranted = true
        
        permissions.forEach { (permission, name) ->
            val granted = ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                Log.d(TAG, "✅ $name permission: GRANTED")
            } else {
                Log.e(TAG, "❌ $name permission: DENIED")
                allGranted = false
            }
        }
        
        if (allGranted) {
            Log.d(TAG, "🎉 All Bluetooth permissions are granted!")
        } else {
            Log.e(TAG, "⚠️  Some Bluetooth permissions are missing!")
            Log.e(TAG, "📱 Please grant all permissions in: Settings → Apps → Emergency Medical → Permissions")
        }
    }
}
