package com.example.eglatracker.network

import android.content.Context
import com.example.eglatracker.data.LocationRecord
import com.example.eglatracker.data.LogTag
import com.example.eglatracker.utils.LoggingManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.*

/**
 * Manages database logging sessions with proper state handling
 */
class SessionManager private constructor(context: Context) {
    
    private val databaseService = DatabaseService.getInstance(context)
    private val loggingManager = LoggingManager.getInstance()
    private val sessionMutex = Mutex()
    
    private var currentSessionId: String? = null
    private var totalRecordsSent = 0
    private var isActive = false
    private var sessionStartTime: Long = 0
    
    companion object {
        @Volatile
        private var INSTANCE: SessionManager? = null
        
        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Start a new logging session
     */
    suspend fun startSession(): SessionResult {
        return sessionMutex.withLock {
            try {
                if (isActive) {
                    return@withLock SessionResult.AlreadyActive(currentSessionId!!)
                }
                
                // Test connection first
                when (val connectionResult = databaseService.testConnection()) {
                    is DatabaseResult.Success -> {
                        // Generate new session ID
                        currentSessionId = "session_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
                        totalRecordsSent = 0
                        isActive = true
                        sessionStartTime = System.currentTimeMillis()
                        
                        loggingManager.success(
                            LogTag.SESSION, 
                            "Logging session started", 
                            "Session: $currentSessionId"
                        )
                        
                        Timber.i("✅ Database logging session started: $currentSessionId")
                        SessionResult.Started(currentSessionId!!)
                    }
                    is DatabaseResult.Error -> {
                        loggingManager.error(
                            LogTag.SESSION, 
                            "Failed to start session - connection test failed", 
                            connectionResult.message
                        )
                        SessionResult.ConnectionFailed(connectionResult.message)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to start logging session")
                loggingManager.error(LogTag.SESSION, "Session start failed", e.message)
                SessionResult.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Log a location record in the current session
     */
    suspend fun logLocation(record: LocationRecord): LogResult {
        return sessionMutex.withLock {
            if (!isActive || currentSessionId == null) {
                return@withLock LogResult.NoActiveSession
            }
            
            try {
                when (val result = databaseService.sendLocationRecord(record, currentSessionId!!)) {
                    is DatabaseResult.Success -> {
                        totalRecordsSent++
                        
                        // Log milestones
                        if (totalRecordsSent % 10 == 0) {
                            loggingManager.info(
                                LogTag.DATABASE, 
                                "Location logging milestone", 
                                "Sent $totalRecordsSent records in session"
                            )
                        }
                        
                        LogResult.Success(totalRecordsSent)
                    }
                    is DatabaseResult.Error -> {
                        loggingManager.warning(
                            LogTag.DATABASE, 
                            "Failed to log location", 
                            result.message
                        )
                        LogResult.Failed(result.message)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error logging location")
                LogResult.Failed(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Stop the current logging session
     */
    suspend fun stopSession(): SessionResult {
        return sessionMutex.withLock {
            try {
                if (!isActive) {
                    return@withLock SessionResult.NotActive
                }
                
                val sessionDuration = System.currentTimeMillis() - sessionStartTime
                val finalSessionId = currentSessionId
                val finalRecordCount = totalRecordsSent
                
                // Reset state
                currentSessionId = null
                totalRecordsSent = 0
                isActive = false
                sessionStartTime = 0
                
                loggingManager.success(
                    LogTag.SESSION, 
                    "Logging session stopped", 
                    "Session: $finalSessionId, Records: $finalRecordCount, Duration: ${sessionDuration}ms"
                )
                
                Timber.i("🛑 Database logging session stopped - Records sent: $finalRecordCount")
                SessionResult.Stopped(finalSessionId!!, finalRecordCount, sessionDuration)
            } catch (e: Exception) {
                Timber.e(e, "Error stopping logging session")
                SessionResult.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Get current session information
     */
    suspend fun getSessionInfo(): SessionInfo {
        return sessionMutex.withLock {
            SessionInfo(
                sessionId = currentSessionId,
                isActive = isActive,
                recordsSent = totalRecordsSent,
                startTime = if (isActive) sessionStartTime else null,
                duration = if (isActive) System.currentTimeMillis() - sessionStartTime else null
            )
        }
    }
    
    /**
     * Check if there's an active session
     */
    fun hasActiveSession(): Boolean = isActive
    
    /**
     * Get current session ID (null if no active session)
     */
    fun getCurrentSessionId(): String? = currentSessionId
    
    /**
     * Get total records sent in current session
     */
    fun getRecordsSent(): Int = totalRecordsSent
}

/**
 * Sealed class for session operation results
 */
sealed class SessionResult {
    data class Started(val sessionId: String) : SessionResult()
    data class Stopped(val sessionId: String, val recordsSent: Int, val duration: Long) : SessionResult()
    data class AlreadyActive(val sessionId: String) : SessionResult()
    data class ConnectionFailed(val error: String) : SessionResult()
    object NotActive : SessionResult()
    data class Error(val message: String) : SessionResult()
}

/**
 * Sealed class for location logging results
 */
sealed class LogResult {
    data class Success(val totalRecords: Int) : LogResult()
    data class Failed(val error: String) : LogResult()
    object NoActiveSession : LogResult()
}

/**
 * Data class for session information
 */
data class SessionInfo(
    val sessionId: String?,
    val isActive: Boolean,
    val recordsSent: Int,
    val startTime: Long?,
    val duration: Long?
) 