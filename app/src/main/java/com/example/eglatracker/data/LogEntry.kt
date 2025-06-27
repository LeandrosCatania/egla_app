package com.example.eglatracker.data

import java.text.SimpleDateFormat
import java.util.*

/**
 * Structured log entry for improved logging UI
 */
data class LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: LogTag,
    val message: String,
    val details: String? = null,
    val data: Map<String, Any>? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
    
    val formattedDate: String
        get() = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
}

/**
 * Log severity levels
 */
enum class LogLevel(val displayName: String, val emoji: String, val priority: Int) {
    DEBUG("Debug", "🔍", 1),
    INFO("Info", "ℹ️", 2),
    SUCCESS("Success", "✅", 3),
    WARNING("Warning", "⚠️", 4),
    ERROR("Error", "❌", 5),
    CRITICAL("Critical", "🚨", 6);
    
    companion object {
        fun fromString(level: String): LogLevel {
            return values().find { it.name.equals(level, ignoreCase = true) } ?: INFO
        }
    }
}

/**
 * Log categories/tags for filtering
 */
enum class LogTag(val displayName: String, val emoji: String, val color: String) {
    LOCATION("Location", "📍", "#4CAF50"),
    DATABASE("Database", "💾", "#2196F3"),
    NETWORK("Network", "🌐", "#FF9800"),
    SENSOR("Sensor", "📡", "#9C27B0"),
    EGLA("EGLA Engine", "🔬", "#FF5722"),
    SYSTEM("System", "⚙️", "#607D8B"),
    USER("User Action", "👤", "#E91E63"),
    PERFORMANCE("Performance", "⚡", "#FFC107"),
    SESSION("Session", "🎯", "#00BCD4"),
    ERROR_RECOVERY("Recovery", "🔄", "#795548");
    
    companion object {
        fun fromString(tag: String): LogTag {
            return values().find { it.name.equals(tag, ignoreCase = true) } ?: SYSTEM
        }
    }
} 