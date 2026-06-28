package com.ansh.ytmusicpreimum

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object DynamicCipherManager {
    private val _cipherRules = MutableStateFlow<Map<String, String>>(emptyMap())
    val cipherRules: StateFlow<Map<String, String>> = _cipherRules

    private const val DEFAULT_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
    
    fun init() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // Fetch every hour
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)

        // Set default values
        val defaults = mapOf(
            "youtube_ua" to DEFAULT_UA,
            "cipher_regex" to "signature=([a-zA-Z0-9.-]+)",
            "decryption_rules" to "{}",
            "use_fallback_extractor" to "false",
        )
        remoteConfig.setDefaultsAsync(defaults)

        fetchLatestRules()
    }

    fun fetchLatestRules() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val updatedRules = mutableMapOf<String, String>()
                updatedRules["youtube_ua"] = remoteConfig.getString("youtube_ua")
                updatedRules["cipher_regex"] = remoteConfig.getString("cipher_regex")
                updatedRules["decryption_rules"] = remoteConfig.getString("decryption_rules")
                updatedRules["use_fallback_extractor"] = remoteConfig.getString("use_fallback_extractor")
                
                _cipherRules.value = updatedRules
                
                // Update NewPipeDownloader dynamically if possible
                NewPipeDownloader.updateUserAgent(updatedRules["youtube_ua"] ?: DEFAULT_UA)
                
                android.util.Log.d("CipherManager", "Dynamic rules updated: $updatedRules")
            }
        }
    }

    fun getRule(key: String): String? = _cipherRules.value[key]
}
