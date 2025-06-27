package com.example.eglatracker.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Configuration manager for database settings
 * Handles persistent storage of database server URL
 */
class DatabaseConfig private constructor(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("database_config", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val DEFAULT_EMULATOR_URL = "10.0.2.2:3000"
        
        @Volatile
        private var INSTANCE: DatabaseConfig? = null
        
        fun getInstance(context: Context): DatabaseConfig {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseConfig(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Get the currently configured server URL
     */
    fun getServerUrl(): String {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_EMULATOR_URL) ?: DEFAULT_EMULATOR_URL
    }
    
    /**
     * Set the server URL
     */
    fun setServerUrl(url: String) {
        prefs.edit()
            .putString(KEY_SERVER_URL, url)
            .apply()
    }
    
    /**
     * Get the full HTTP URL for the server
     */
    fun getFullServerUrl(): String {
        val baseUrl = getServerUrl()
        return if (baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            baseUrl.removeSuffix("/")
        } else {
            "http://$baseUrl"
        }
    }
    
    /**
     * Check if current configuration is for emulator
     */
    fun isEmulatorConfig(): Boolean {
        return getServerUrl().startsWith("10.0.2.2")
    }
    
    /**
     * Reset to default emulator configuration
     */
    fun resetToDefault() {
        setServerUrl(DEFAULT_EMULATOR_URL)
    }
} 