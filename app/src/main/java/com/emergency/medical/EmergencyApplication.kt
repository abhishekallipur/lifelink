package com.emergency.medical

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EmergencyApplication : Application() {
    
    companion object {
        private const val PREFS_NAME = "emergency_medical_prefs"
        
        lateinit var instance: EmergencyApplication
            private set
        
        fun getSecuredPreferences(): SharedPreferences {
            val masterKey = MasterKey.Builder(instance)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
                
            return EncryptedSharedPreferences.create(
                instance,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
