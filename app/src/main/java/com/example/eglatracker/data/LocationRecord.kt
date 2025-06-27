package com.example.eglatracker.data

import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class representing a single location record for CSV storage
 */
data class LocationRecord(
    val timestamp: Long,
    val latitude: Double, // Enhanced latitude
    val longitude: Double, // Enhanced longitude
    val originalLatitude: Double, // Original GPS latitude
    val originalLongitude: Double, // Original GPS longitude  
    val accuracy: Float, // Enhanced accuracy
    val originalAccuracy: Float, // Original GPS accuracy
    val enhancedLatitude: Double = latitude, // Alias for compatibility
    val enhancedLongitude: Double = longitude, // Alias for compatibility
    val enhancedAccuracy: Float = accuracy, // Alias for compatibility
    val altitude: Double,
    val bearing: Float,
    val speed: Float,
    val direction: String,
    val isStationary: Boolean,
    val environment: String,
    val operatingMode: String,
    val accuracyImprovement: Float,
    val confidence: Float,
    val processingTime: Long
) {
    
    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        
        /**
         * CSV Header for the location data file
         */
        fun getCsvHeader(): String {
            return "timestamp,datetime,original_latitude,original_longitude,latitude,longitude," +
                   "original_accuracy,enhanced_accuracy,altitude,bearing,speed,direction," +
                   "environment,operating_mode,accuracy_improvement,confidence,is_stationary,processing_time"
        }
    }
    
    /**
     * Convert this record to CSV format
     */
    fun toCsv(): String {
        val datetime = dateFormat.format(Date(timestamp))
        return "$timestamp,$datetime,$originalLatitude,$originalLongitude,$latitude,$longitude," +
               "$originalAccuracy,$enhancedAccuracy,$altitude,$bearing,$speed,$direction," +
               "$environment,$operatingMode,$accuracyImprovement,$confidence,$isStationary,$processingTime"
    }
    
    /**
     * Get formatted datetime string
     */
    fun getFormattedTime(): String {
        return dateFormat.format(Date(timestamp))
    }
} 