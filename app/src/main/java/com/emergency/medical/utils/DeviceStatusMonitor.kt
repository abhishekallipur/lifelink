package com.emergency.medical.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.emergency.medical.data.DeviceInfo
import com.emergency.medical.data.LocationData
import com.google.android.gms.location.*

class DeviceStatusMonitor(private val context: Context) {
    
    companion object {
        private const val TAG = "DeviceStatusMonitor"
    }
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    fun getBatteryInfo(): Pair<Int, Boolean> {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        
        val batteryPercent = if (level >= 0 && scale > 0) {
            (level.toFloat() / scale.toFloat() * 100).toInt()
        } else {
            0
        }
        
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
        
        return Pair(batteryPercent, isCharging)
    }
    
    fun isInternetAvailable(): Boolean {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            
            if (network == null) {
                Log.w(TAG, "🌐 No active network found")
                return false
            }
            
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            if (capabilities == null) {
                Log.w(TAG, "🌐 No network capabilities found")
                return false
            }
            
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val hasValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val notMetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) // WiFi usually
            val hasTransport = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                              capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                              capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            
            // More strict checking - require both internet capability AND validation
            val result = hasInternet && hasValidated && hasTransport
            
            Log.i(TAG, "🌐 Network check: hasInternet=$hasInternet, validated=$hasValidated, transport=$hasTransport, result=$result")
            
            if (!result) {
                Log.w(TAG, "🌐 Internet not available - will use offline transmission methods")
            }
            
            return result
        } catch (e: Exception) {
            Log.e(TAG, "🌐 Error checking internet availability", e)
            return false
        }
    }
    
    /**
     * Double-check internet connectivity with a quick ping test
     */
    fun performConnectivityTest(callback: (Boolean) -> Unit) {
        Thread {
            try {
                val runtime = Runtime.getRuntime()
                val process = runtime.exec("/system/bin/ping -c 1 8.8.8.8")
                val exitValue = process.waitFor()
                val isReachable = exitValue == 0
                
                Log.i(TAG, "🌐 Ping test result: $isReachable")
                callback(isReachable)
            } catch (e: Exception) {
                Log.w(TAG, "🌐 Ping test failed", e)
                callback(false)
            }
        }.start()
    }
    
    fun getNetworkType(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return "None"
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "None"
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Unknown"
        }
    }
    
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    fun getCurrentDeviceInfo(): DeviceInfo {
        val (batteryLevel, isCharging) = getBatteryInfo()
        return DeviceInfo(
            batteryLevel = batteryLevel,
            isCharging = isCharging
        )
    }
    
    fun getLastKnownLocation(callback: (LocationData?) -> Unit) {
        Log.d(TAG, "Requesting location data...")
        
        // Check permissions first
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permissions not granted")
            callback(null)
            return
        }
        
        // Check if location is enabled
        if (!isLocationEnabled()) {
            Log.w(TAG, "Location services are disabled")
            callback(null)
            return
        }
        
        // Try to get fresh location first
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(1000)
            .setMaxUpdates(1)
            .build()
            
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    Log.i(TAG, "Fresh location obtained: Lat ${location.latitude}, Lng ${location.longitude}, Accuracy: ${location.accuracy}m")
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy
                    )
                    callback(locationData)
                } else {
                    Log.w(TAG, "Fresh location request returned null, trying last known location")
                    getLastKnownLocationFallback(callback)
                }
                // Remove location updates after getting result
                fusedLocationClient.removeLocationUpdates(this)
            }
        }
        
        // Request fresh location with timeout
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        
        // Fallback to last known location after 3 seconds
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.i(TAG, "Fresh location timeout, trying last known location")
            getLastKnownLocationFallback(callback)
        }, 3000)
    }
    
    private fun getLastKnownLocationFallback(callback: (LocationData?) -> Unit) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.i(TAG, "Last known location obtained: Lat ${location.latitude}, Lng ${location.longitude}, Accuracy: ${location.accuracy}m")
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy
                    )
                    callback(locationData)
                } else {
                    Log.w(TAG, "No last known location available")
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting last known location", exception)
                callback(null)
            }
    }
    
    fun getBatteryColorCode(batteryLevel: Int): Int {
        return when {
            batteryLevel >= 50 -> android.graphics.Color.GREEN
            batteryLevel >= 20 -> android.graphics.Color.parseColor("#FFA500") // Orange
            else -> android.graphics.Color.RED
        }
    }
    
    fun getConnectionStatusColor(isConnected: Boolean): Int {
        return if (isConnected) android.graphics.Color.GREEN else android.graphics.Color.RED
    }
}
