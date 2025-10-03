package com.emergency.medical.data

import android.content.SharedPreferences
import com.emergency.medical.EmergencyApplication
import com.google.gson.Gson

data class MedicalInfo(
    val fullName: String = "",
    val age: String = "",
    val bloodGroup: String = "",
    val medicalConditions: String = "",
    val emergencyContact: String = ""
) {
    fun isComplete(): Boolean {
        return fullName.isNotBlank() && 
               age.isNotBlank() && 
               bloodGroup.isNotBlank() && 
               emergencyContact.isNotBlank()
    }
}

class MedicalDataManager {
    
    companion object {
        private const val KEY_MEDICAL_INFO = "medical_info"
        private const val KEY_FIRST_SETUP = "first_setup_complete"
        
        @Volatile
        private var INSTANCE: MedicalDataManager? = null
        
        fun getInstance(): MedicalDataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MedicalDataManager().also { INSTANCE = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = EmergencyApplication.getSecuredPreferences()
    private val gson = Gson()
    
    fun saveMedicalInfo(medicalInfo: MedicalInfo) {
        val json = gson.toJson(medicalInfo)
        prefs.edit().putString(KEY_MEDICAL_INFO, json).apply()
        
        if (medicalInfo.isComplete()) {
            prefs.edit().putBoolean(KEY_FIRST_SETUP, true).apply()
        }
    }
    
    fun getMedicalInfo(): MedicalInfo {
        val json = prefs.getString(KEY_MEDICAL_INFO, null)
        return if (json != null) {
            try {
                gson.fromJson(json, MedicalInfo::class.java)
            } catch (e: Exception) {
                MedicalInfo()
            }
        } else {
            MedicalInfo()
        }
    }
    
    fun isFirstSetupComplete(): Boolean {
        return prefs.getBoolean(KEY_FIRST_SETUP, false)
    }
    
    fun clearMedicalInfo() {
        prefs.edit()
            .remove(KEY_MEDICAL_INFO)
            .remove(KEY_FIRST_SETUP)
            .apply()
    }
}
