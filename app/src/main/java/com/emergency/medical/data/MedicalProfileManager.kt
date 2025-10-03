package com.emergency.medical.data

import android.content.SharedPreferences
import com.emergency.medical.EmergencyApplication
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

data class MedicalProfile(
    val id: String = UUID.randomUUID().toString(),
    val profileName: String = "",
    val fullName: String = "",
    val age: String = "",
    val bloodGroup: String = "",
    val medicalConditions: String = "",
    val emergencyContact: String = "",
    val relationship: String = "", // Self, Family Member, Friend, etc.
    val createdDate: Long = System.currentTimeMillis(),
    val isSelected: Boolean = false // For multi-profile transmission
) {
    fun isComplete(): Boolean {
        return profileName.isNotBlank() &&
               fullName.isNotBlank() && 
               age.isNotBlank() && 
               bloodGroup.isNotBlank() && 
               emergencyContact.isNotBlank()
    }
    
    fun toMedicalInfo(): MedicalInfo {
        return MedicalInfo(
            fullName = fullName,
            age = age,
            bloodGroup = bloodGroup,
            medicalConditions = medicalConditions,
            emergencyContact = emergencyContact
        )
    }
}

class MedicalProfileManager {
    
    companion object {
        private const val KEY_MEDICAL_PROFILES = "medical_profiles"
        private const val KEY_CURRENT_PROFILE_ID = "current_profile_id"
        private const val KEY_PROFILES_ENABLED = "profiles_enabled"
        
        @Volatile
        private var INSTANCE: MedicalProfileManager? = null
        
        fun getInstance(): MedicalProfileManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MedicalProfileManager().also { INSTANCE = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = EmergencyApplication.getSecuredPreferences()
    private val gson = Gson()
    
    fun getAllProfiles(): List<MedicalProfile> {
        val json = prefs.getString(KEY_MEDICAL_PROFILES, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<MedicalProfile>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    fun saveProfiles(profiles: List<MedicalProfile>) {
        val json = gson.toJson(profiles)
        prefs.edit().putString(KEY_MEDICAL_PROFILES, json).apply()
    }
    
    fun addProfile(profile: MedicalProfile): Boolean {
        val profiles = getAllProfiles().toMutableList()
        
        // Check if profile name already exists
        if (profiles.any { it.profileName.equals(profile.profileName, ignoreCase = true) }) {
            return false
        }
        
        profiles.add(profile)
        saveProfiles(profiles)
        return true
    }
    
    fun updateProfile(updatedProfile: MedicalProfile): Boolean {
        val profiles = getAllProfiles().toMutableList()
        val index = profiles.indexOfFirst { it.id == updatedProfile.id }
        
        if (index != -1) {
            // Check if profile name conflicts with other profiles
            val nameConflict = profiles.any { 
                it.id != updatedProfile.id && 
                it.profileName.equals(updatedProfile.profileName, ignoreCase = true) 
            }
            
            if (nameConflict) {
                return false
            }
            
            profiles[index] = updatedProfile
            saveProfiles(profiles)
            return true
        }
        return false
    }
    
    fun deleteProfile(profileId: String): Boolean {
        val profiles = getAllProfiles().toMutableList()
        val removed = profiles.removeAll { it.id == profileId }
        
        if (removed) {
            saveProfiles(profiles)
            
            // If current profile was deleted, clear current profile
            if (getCurrentProfileId() == profileId) {
                clearCurrentProfile()
            }
        }
        
        return removed
    }
    
    fun getProfile(profileId: String): MedicalProfile? {
        return getAllProfiles().find { it.id == profileId }
    }
    
    fun getCurrentProfile(): MedicalProfile? {
        val currentId = getCurrentProfileId()
        return if (currentId.isNotEmpty()) {
            getProfile(currentId)
        } else {
            null
        }
    }
    
    fun setCurrentProfile(profileId: String) {
        prefs.edit().putString(KEY_CURRENT_PROFILE_ID, profileId).apply()
    }
    
    fun getCurrentProfileId(): String {
        return prefs.getString(KEY_CURRENT_PROFILE_ID, "") ?: ""
    }
    
    fun clearCurrentProfile() {
        prefs.edit().remove(KEY_CURRENT_PROFILE_ID).apply()
    }
    
    fun getSelectedProfiles(): List<MedicalProfile> {
        return getAllProfiles().filter { it.isSelected }
    }
    
    fun updateProfileSelection(profileId: String, isSelected: Boolean) {
        val profiles = getAllProfiles().toMutableList()
        val index = profiles.indexOfFirst { it.id == profileId }
        
        if (index != -1) {
            profiles[index] = profiles[index].copy(isSelected = isSelected)
            saveProfiles(profiles)
        }
    }
    
    fun selectAllProfiles(select: Boolean) {
        val profiles = getAllProfiles().map { it.copy(isSelected = select) }
        saveProfiles(profiles)
    }
    
    fun isProfileSystemEnabled(): Boolean {
        return prefs.getBoolean(KEY_PROFILES_ENABLED, false)
    }
    
    fun setProfileSystemEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PROFILES_ENABLED, enabled).apply()
    }
    
    fun createDefaultProfile(): MedicalProfile? {
        // Create a default profile from current medical info
        val medicalDataManager = MedicalDataManager.getInstance()
        val currentInfo = medicalDataManager.getMedicalInfo()
        
        if (currentInfo.isComplete()) {
            return MedicalProfile(
                profileName = "My Profile",
                fullName = currentInfo.fullName,
                age = currentInfo.age,
                bloodGroup = currentInfo.bloodGroup,
                medicalConditions = currentInfo.medicalConditions,
                emergencyContact = currentInfo.emergencyContact,
                relationship = "Self"
            )
        }
        return null
    }
    
    fun migrateFromSingleProfile(): Boolean {
        // Migrate existing single profile to profile system
        if (isProfileSystemEnabled() || getAllProfiles().isNotEmpty()) {
            return false // Already migrated or no migration needed
        }
        
        val defaultProfile = createDefaultProfile()
        if (defaultProfile != null) {
            addProfile(defaultProfile)
            setCurrentProfile(defaultProfile.id)
            setProfileSystemEnabled(true)
            return true
        }
        return false
    }
    
    fun clearAllProfiles() {
        prefs.edit()
            .remove(KEY_MEDICAL_PROFILES)
            .remove(KEY_CURRENT_PROFILE_ID)
            .remove(KEY_PROFILES_ENABLED)
            .apply()
    }
}
