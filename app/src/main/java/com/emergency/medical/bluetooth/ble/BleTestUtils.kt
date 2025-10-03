package com.emergency.medical.bluetooth.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log

/**
 * Utility class to test BLE functionality
 */
object BleTestUtils {
    private const val TAG = "BleTestUtils"
    
    fun checkBleSupport(context: Context): BleCheckResult {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter
        
        return when {
            bluetoothAdapter == null -> {
                Log.e(TAG, "Bluetooth not supported on this device")
                BleCheckResult.NOT_SUPPORTED
            }
            !bluetoothAdapter.isEnabled -> {
                Log.w(TAG, "Bluetooth is disabled")
                BleCheckResult.DISABLED
            }
            bluetoothAdapter.bluetoothLeAdvertiser == null -> {
                Log.e(TAG, "BLE advertising not supported")
                BleCheckResult.ADVERTISE_NOT_SUPPORTED
            }
            bluetoothAdapter.bluetoothLeScanner == null -> {
                Log.e(TAG, "BLE scanning not supported")
                BleCheckResult.SCAN_NOT_SUPPORTED
            }
            else -> {
                Log.i(TAG, "BLE fully supported and ready")
                BleCheckResult.SUPPORTED_AND_READY
            }
        }
    }
    
    enum class BleCheckResult {
        NOT_SUPPORTED,
        DISABLED,
        ADVERTISE_NOT_SUPPORTED,
        SCAN_NOT_SUPPORTED,
        SUPPORTED_AND_READY
    }
}
