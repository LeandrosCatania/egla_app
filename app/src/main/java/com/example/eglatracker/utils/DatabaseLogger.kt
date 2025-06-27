package com.example.eglatracker.utils

import android.content.Context
import com.example.eglatracker.data.LocationRecord
import com.example.eglatracker.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Simplified database logger that uses the new unified architecture
 * This is now a simple wrapper around SessionManager and DatabaseService
 */
class DatabaseLogger(private val context: Context) {
    
    private val sessionManager = SessionManager.getInstance(context)
    private val databaseService = DatabaseService.getInstance(context)
    
    val deviceId: String get() = databaseService.deviceId
    
    /**
     * Start a new logging session
     */
    suspend fun startLogging(): Boolean = withContext(Dispatchers.IO) {
        when (val result = sessionManager.startSession()) {
            is SessionResult.Started -> {
                Timber.i("✅ Database logging started - Session: ${result.sessionId}")
                true
            }
            is SessionResult.AlreadyActive -> {
                Timber.w("⚠️ Logging session already active: ${result.sessionId}")
                true
            }
            is SessionResult.ConnectionFailed -> {
                Timber.e("❌ Failed to start logging - Connection failed: ${result.error}")
                false
            }
            is SessionResult.Error -> {
                Timber.e("❌ Failed to start logging: ${result.message}")
                false
            }
            else -> {
                Timber.e("❌ Unexpected result when starting logging")
                false
            }
        }
    }
    
    /**
     * Log a location record to the database
     */
    suspend fun logLocation(record: LocationRecord): Boolean = withContext(Dispatchers.IO) {
        when (val result = sessionManager.logLocation(record)) {
            is LogResult.Success -> {
                // Only log every 10th record to avoid spam
                if (result.totalRecords % 10 == 0) {
                    Timber.d("📊 Sent ${result.totalRecords} location records to database")
                }
                true
            }
            is LogResult.Failed -> {
                Timber.w("⚠️ Failed to log location: ${result.error}")
                false
            }
            is LogResult.NoActiveSession -> {
                Timber.w("⚠️ No active logging session")
                false
            }
        }
    }
    
    /**
     * Stop the current logging session
     */
    suspend fun stopLogging() = withContext(Dispatchers.IO) {
        when (val result = sessionManager.stopSession()) {
            is SessionResult.Stopped -> {
                Timber.i("🛑 Database logging stopped")
                Timber.i("📊 Session: ${result.sessionId}")
                Timber.i("📊 Total records sent: ${result.recordsSent}")
                Timber.i("📊 Session duration: ${result.duration}ms")
            }
            is SessionResult.NotActive -> {
                Timber.i("ℹ️ No active logging session to stop")
            }
            is SessionResult.Error -> {
                Timber.e("❌ Error stopping logging: ${result.message}")
            }
            else -> {
                Timber.w("⚠️ Unexpected result when stopping logging")
            }
        }
    }
    
    /**
     * Check if database logging is active
     */
    fun isLogging(): Boolean = sessionManager.hasActiveSession()
    
    /**
     * Get current session info
     */
    fun getCurrentSession(): String? = sessionManager.getCurrentSessionId()
    
    /**
     * Get total records sent in current session
     */
    fun getTotalRecordsSent(): Int = sessionManager.getRecordsSent()
    
    /**
     * Update the database server URL
     */
    fun updateServerUrl(newUrl: String) {
        databaseService.updateServerUrl(newUrl)
    }
    
    /**
     * Get current server URL
     */
    fun getCurrentServerUrl(): String = databaseService.getCurrentServerUrl()
    
    /**
     * Get session statistics
     */
    suspend fun getSessionStats(): SessionStats = withContext(Dispatchers.IO) {
        val sessionInfo = sessionManager.getSessionInfo()
        SessionStats(
            sessionId = sessionInfo.sessionId,
            isActive = sessionInfo.isActive,
            recordsSent = sessionInfo.recordsSent,
            startTime = sessionInfo.startTime,
            duration = sessionInfo.duration,
            deviceId = deviceId,
            serverUrl = getCurrentServerUrl()
        )
    }
    
    /**
     * Test connection to database server
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        when (val result = databaseService.testConnection()) {
            is DatabaseResult.Success -> {
                Timber.d("✅ Database connection test successful")
                true
            }
            is DatabaseResult.Error -> {
                Timber.e("❌ Database connection test failed: ${result.message}")
                false
            }
        }
    }
    
    /**
     * Data class for session statistics
     */
    data class SessionStats(
        val sessionId: String?,
        val isActive: Boolean,
        val recordsSent: Int,
        val startTime: Long?,
        val duration: Long?,
        val deviceId: String,
        val serverUrl: String
    )
    
    /**
     * Backward compatibility - keeping the old data class name
     */
    data class DatabaseStats(
        val totalRecords: Int,
        val currentSession: String?,
        val isConnected: Boolean,
        val deviceId: String,
        val serverUrl: String
    )
    
    /**
     * Get device statistics (backward compatibility method)
     */
    suspend fun getDeviceStats(): DatabaseStats = withContext(Dispatchers.IO) {
        val sessionStats = getSessionStats()
        DatabaseStats(
            totalRecords = sessionStats.recordsSent,
            currentSession = sessionStats.sessionId,
            isConnected = sessionStats.isActive,
            deviceId = sessionStats.deviceId,
            serverUrl = sessionStats.serverUrl
        )
    }
} 