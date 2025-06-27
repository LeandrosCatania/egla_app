package com.example.eglatracker.utils

import com.example.eglatracker.data.LogEntry
import com.example.eglatracker.data.LogLevel
import com.example.eglatracker.data.LogTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Advanced logging manager with structured logs, filtering, and visual improvements
 */
class LoggingManager {
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()
    
    private val _filteredLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val filteredLogs: StateFlow<List<LogEntry>> = _filteredLogs.asStateFlow()
    
    private val _activeFilters = MutableStateFlow(LogFilters())
    val activeFilters: StateFlow<LogFilters> = _activeFilters.asStateFlow()
    
    private val maxLogEntries = 1000
    
    companion object {
        @Volatile
        private var INSTANCE: LoggingManager? = null
        
        fun getInstance(): LoggingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LoggingManager().also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Add a new log entry
     */
    fun log(
        level: LogLevel,
        tag: LogTag,
        message: String,
        details: String? = null,
        data: Map<String, Any>? = null
    ) {
        val entry = LogEntry(
            level = level,
            tag = tag,
            message = message,
            details = details,
            data = data
        )
        
        // Also log to Timber for debugging
        when (level) {
            LogLevel.DEBUG -> Timber.d("${tag.emoji} [${tag.displayName}] $message")
            LogLevel.INFO -> Timber.i("${tag.emoji} [${tag.displayName}] $message")
            LogLevel.SUCCESS -> Timber.i("${tag.emoji} [${tag.displayName}] $message")
            LogLevel.WARNING -> Timber.w("${tag.emoji} [${tag.displayName}] $message")
            LogLevel.ERROR -> Timber.e("${tag.emoji} [${tag.displayName}] $message")
            LogLevel.CRITICAL -> Timber.e("${tag.emoji} [${tag.displayName}] $message")
        }
        
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(0, entry) // Add to beginning for latest-first
        
        // Limit log entries to prevent memory issues
        if (currentLogs.size > maxLogEntries) {
            repeat(currentLogs.size - maxLogEntries) {
                currentLogs.removeAt(maxLogEntries)
            }
        }
        
        _logs.value = currentLogs
        applyFilters()
    }
    
    /**
     * Convenience methods for different log levels
     */
    fun debug(tag: LogTag, message: String, details: String? = null, data: Map<String, Any>? = null) =
        log(LogLevel.DEBUG, tag, message, details, data)
    
    fun info(tag: LogTag, message: String, details: String? = null, data: Map<String, Any>? = null) =
        log(LogLevel.INFO, tag, message, details, data)
    
    fun success(tag: LogTag, message: String, details: String? = null, data: Map<String, Any>? = null) =
        log(LogLevel.SUCCESS, tag, message, details, data)
    
    fun warning(tag: LogTag, message: String, details: String? = null, data: Map<String, Any>? = null) =
        log(LogLevel.WARNING, tag, message, details, data)
    
    fun error(tag: LogTag, message: String, details: String? = null, data: Map<String, Any>? = null) =
        log(LogLevel.ERROR, tag, message, details, data)
    
    fun critical(tag: LogTag, message: String, details: String? = null, data: Map<String, Any>? = null) =
        log(LogLevel.CRITICAL, tag, message, details, data)
    
    /**
     * Update filters
     */
    fun updateFilters(filters: LogFilters) {
        _activeFilters.value = filters
        applyFilters()
    }
    
    /**
     * Apply current filters to logs
     */
    private fun applyFilters() {
        val filters = _activeFilters.value
        val allLogs = _logs.value
        
        val filtered = allLogs.filter { log ->
            // Level filter
            val levelMatch = filters.levels.isEmpty() || filters.levels.contains(log.level)
            
            // Tag filter
            val tagMatch = filters.tags.isEmpty() || filters.tags.contains(log.tag)
            
            // Search filter
            val searchMatch = filters.searchQuery.isBlank() || 
                log.message.contains(filters.searchQuery, ignoreCase = true) ||
                log.details?.contains(filters.searchQuery, ignoreCase = true) == true
            
            // Time filter
            val timeMatch = log.timestamp >= filters.startTime && log.timestamp <= filters.endTime
            
            levelMatch && tagMatch && searchMatch && timeMatch
        }
        
        _filteredLogs.value = filtered
    }
    
    /**
     * Clear all logs
     */
    fun clearLogs() {
        _logs.value = emptyList()
        _filteredLogs.value = emptyList()
    }
    
    /**
     * Get log statistics
     */
    fun getLogStats(): LogStats {
        val allLogs = _logs.value
        return LogStats(
            total = allLogs.size,
            byLevel = LogLevel.values().associateWith { level ->
                allLogs.count { it.level == level }
            },
            byTag = LogTag.values().associateWith { tag ->
                allLogs.count { it.tag == tag }
            },
            lastHour = allLogs.count { 
                it.timestamp >= System.currentTimeMillis() - 3600000 
            }
        )
    }
}

/**
 * Filter configuration for logs
 */
data class LogFilters(
    val levels: Set<LogLevel> = emptySet(),
    val tags: Set<LogTag> = emptySet(),
    val searchQuery: String = "",
    val startTime: Long = 0L,
    val endTime: Long = Long.MAX_VALUE
)

/**
 * Log statistics
 */
data class LogStats(
    val total: Int,
    val byLevel: Map<LogLevel, Int>,
    val byTag: Map<LogTag, Int>,
    val lastHour: Int
) 