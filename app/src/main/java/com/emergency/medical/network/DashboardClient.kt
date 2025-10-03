package com.emergency.medical.network

import android.util.Log
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.emergency.medical.data.EmergencyPayload
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class DashboardClient {
    
    companion object {
        private const val TAG = "DashboardClient"
        // PRODUCTION: Your Render website with backend - correct endpoint
        private const val DASHBOARD_URL = "https://lifelink-90wf.onrender.com/api/alert"
        private const val TIMEOUT_SECONDS = 5L  // Reduced timeout for faster failure detection
        
        // Debug flags for testing
        var SIMULATE_DASHBOARD_SUCCESS = false  // Set to true for testing without real server
        var SIMULATE_NETWORK_FAILURE = false   // Set to true to test Bluetooth fallback
        
        @Volatile
        private var INSTANCE: DashboardClient? = null
        
        fun getInstance(): DashboardClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DashboardClient().also { INSTANCE = it }
            }
        }
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    interface EmergencyCallback {
        fun onSuccess()
        fun onFailure(error: String)
    }
    
    fun sendEmergency(payload: EmergencyPayload, callback: EmergencyCallback) {
        // Final network availability check before attempting to send
        // Note: This requires a Context, so we'll skip this check here and rely on the caller
        // to have already verified network availability
        
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = payload.toJson().toRequestBody(jsonMediaType)
        
        val request = Request.Builder()
            .url(DASHBOARD_URL)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "LifeLink/1.0.0")
            .build()
        
        Log.d(TAG, "🚀 Sending emergency payload to LIVE dashboard endpoint: ${payload.toJson()}")
        Log.i(TAG, "📍 Target URL: $DASHBOARD_URL")
        
        // Debug simulation for testing
        if (SIMULATE_NETWORK_FAILURE) {
            Log.w(TAG, "🚫 Simulating network failure for testing")
            callback.onFailure("Simulated network failure (for testing)")
            return
        }
        
        // Always try to send to dashboard if URL is configured
        Log.i(TAG, "🌐 Attempting to send emergency to LIVE dashboard: $DASHBOARD_URL")
        Log.i(TAG, "📦 Payload size: ${payload.toJson().length} bytes")
        Log.i(TAG, "📋 Payload content: ${payload.toJson()}")
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "❌ Dashboard connection failed: ${e.message}", e)
                callback.onFailure("Network error: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                
                if (response.isSuccessful) {
                    Log.i(TAG, "✅ Emergency successfully sent to LIVE dashboard (HTTP ${response.code})")
                    Log.i(TAG, "✅ Server response: $responseBody")
                    callback.onSuccess()
                } else {
                    val error = "Server error: ${response.code} ${response.message}"
                    Log.e(TAG, "❌ Dashboard server error: $error")
                    Log.e(TAG, "❌ Server response body: $responseBody")
                    callback.onFailure("$error - Response: $responseBody")
                }
                response.close()
            }
        })
    }
    
    fun sendRelayMessage(payload: EmergencyPayload, callback: EmergencyCallback) {
        Log.i(TAG, "Forwarding relay message to dashboard")
        sendEmergency(payload, callback)
    }
    
    fun testConnection(callback: EmergencyCallback) {
        val request = Request.Builder()
            .url("$DASHBOARD_URL/health")
            .get()
            .build()
            
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure("Connection test failed: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    callback.onSuccess()
                } else {
                    callback.onFailure("Server unavailable: ${response.code}")
                }
                response.close()
            }
        })
    }
}
