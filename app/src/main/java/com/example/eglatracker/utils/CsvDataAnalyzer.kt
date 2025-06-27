package com.example.eglatracker.utils

import android.content.Context
import com.example.eglatracker.data.LocationRecord
import java.io.File
import java.io.IOException
import kotlin.math.*

/**
 * Analyzer for CSV location data providing insights and calculations
 */
class CsvDataAnalyzer(private val context: Context) {
    
    data class LocationAnalysis(
        val record: LocationRecord,
        val distanceFromPrevious: Double = 0.0,
        val speedFromPrevious: Double = 0.0,
        val accuracyImprovement: Double = 0.0,
        val bearingFromPrevious: Double = 0.0,
        val timeDelta: Long = 0L,
        val enhancementSteps: EnhancementSteps? = null
    )
    
    data class EnhancementSteps(
        val originalAccuracy: Double,
        val signalProcessingAccuracy: Double,
        val kalmanFilterAccuracy: Double,
        val swarmOptimizationAccuracy: Double,
        val finalAccuracy: Double,
        val environmentClassification: String,
        val confidenceScore: Double
    )
    
    data class SessionSummary(
        val totalPoints: Int,
        val totalDistance: Double,
        val averageAccuracy: Double,
        val averageImprovement: Double,
        val bestAccuracy: Double,
        val worstAccuracy: Double,
        val sessionDuration: Long,
        val averageSpeed: Double,
        val maxSpeed: Double
    )
    
    /**
     * Load and analyze CSV data from a file
     */
    fun analyzeFile(file: File): List<LocationAnalysis> {
        println("CsvDataAnalyzer: Starting to analyze file: ${file.absolutePath}")
        println("CsvDataAnalyzer: File exists: ${file.exists()}")
        println("CsvDataAnalyzer: File size: ${file.length()} bytes")
        
        if (!file.exists() || !file.canRead()) {
            throw IOException("Cannot read file: ${file.path}")
        }
        
        val records = mutableListOf<LocationRecord>()
        
        try {
            val lines = file.readLines()
            println("CsvDataAnalyzer: Total lines in file: ${lines.size}")
            
            lines.drop(1).forEachIndexed { index, line ->  // Skip header
                if (line.isNotBlank()) {
                    val record = parseLocationRecord(line)
                    if (record != null) {
                        records.add(record)
                    } else {
                        println("CsvDataAnalyzer: Failed to parse line ${index + 2}: $line")
                    }
                }
            }
            
            println("CsvDataAnalyzer: Parsed ${records.size} records from ${lines.size - 1} data lines")
            
        } catch (e: Exception) {
            println("CsvDataAnalyzer: Error parsing CSV file: ${e.message}")
            throw IOException("Error parsing CSV file: ${e.message}", e)
        }
        
        return analyzeRecords(records)
    }
    
    /**
     * Get list of available CSV files
     */
    fun getAvailableFiles(): List<File> {
        val logDir = File(context.getExternalFilesDir(null), "egla_logs")
        if (!logDir.exists()) return emptyList()
        
        return logDir.listFiles { file ->
            file.isFile && file.extension.lowercase() == "csv"
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    /**
     * Generate session summary statistics
     */
    fun generateSummary(analyses: List<LocationAnalysis>): SessionSummary {
        if (analyses.isEmpty()) {
            return SessionSummary(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0, 0.0)
        }
        
        val totalDistance = analyses.sumOf { it.distanceFromPrevious }
        val accuracies = analyses.map { it.record.enhancedAccuracy.toDouble() }
        val improvements = analyses.mapNotNull { 
            if (it.accuracyImprovement > 0) it.accuracyImprovement else null 
        }
        val speeds = analyses.mapNotNull { 
            if (it.speedFromPrevious > 0) it.speedFromPrevious else null 
        }
        
        val firstTime = analyses.first().record.timestamp
        val lastTime = analyses.last().record.timestamp
        val duration = lastTime - firstTime
        
        return SessionSummary(
            totalPoints = analyses.size,
            totalDistance = totalDistance,
            averageAccuracy = accuracies.average(),
            averageImprovement = improvements.takeIf { it.isNotEmpty() }?.average() ?: 0.0,
            bestAccuracy = accuracies.minOrNull() ?: 0.0,
            worstAccuracy = accuracies.maxOrNull() ?: 0.0,
            sessionDuration = duration,
            averageSpeed = speeds.takeIf { it.isNotEmpty() }?.average() ?: 0.0,
            maxSpeed = speeds.maxOrNull() ?: 0.0
        )
    }
    
    private fun analyzeRecords(records: List<LocationRecord>): List<LocationAnalysis> {
        if (records.isEmpty()) return emptyList()
        
        val analyses = mutableListOf<LocationAnalysis>()
        
        records.forEachIndexed { index, record ->
            val analysis = if (index == 0) {
                LocationAnalysis(
                    record = record,
                    enhancementSteps = createEnhancementSteps(record)
                )
            } else {
                val previousRecord = records[index - 1]
                val distance = calculateDistance(
                    previousRecord.originalLatitude, previousRecord.originalLongitude,
                    record.originalLatitude, record.originalLongitude
                )
                val timeDelta = record.timestamp - previousRecord.timestamp
                val speed = if (timeDelta > 0) {
                    (distance * 1000) / (timeDelta / 1000.0) // m/s
                } else 0.0
                
                val bearing = calculateBearing(
                    previousRecord.originalLatitude, previousRecord.originalLongitude,
                    record.originalLatitude, record.originalLongitude
                )
                
                val improvement = calculateAccuracyImprovement(
                    record.originalAccuracy, record.enhancedAccuracy
                )
                
                LocationAnalysis(
                    record = record,
                    distanceFromPrevious = distance,
                    speedFromPrevious = speed,
                    accuracyImprovement = improvement,
                    bearingFromPrevious = bearing,
                    timeDelta = timeDelta,
                    enhancementSteps = createEnhancementSteps(record)
                )
            }
            analyses.add(analysis)
        }
        
        return analyses
    }
    
    private fun parseLocationRecord(line: String): LocationRecord? {
        return try {
            val parts = line.split(",").map { it.trim().removeSurrounding("\"") } // Remove quotes from all parts
            println("CsvDataAnalyzer: Parsing line with ${parts.size} parts")
            
            if (parts.size >= 18) { // Updated to match actual CSV format
                // Add debug logging for the first few critical fields
                println("CsvDataAnalyzer: Timestamp: '${parts[0]}'")
                println("CsvDataAnalyzer: Original Lat: '${parts[2]}', Original Lon: '${parts[3]}'")
                println("CsvDataAnalyzer: Enhanced Lat: '${parts[4]}', Enhanced Lon: '${parts[5]}'")
                
                val record = LocationRecord(
                    timestamp = parts[0].toLongOrNull() ?: run {
                        println("CsvDataAnalyzer: Failed to parse timestamp: '${parts[0]}'")
                        return null
                    },
                    // Skip datetime field at parts[1]
                    latitude = parts[4].toDoubleOrNull() ?: run {
                        println("CsvDataAnalyzer: Failed to parse enhanced latitude: '${parts[4]}'")
                        return null
                    }, // Enhanced latitude (column 5)
                    longitude = parts[5].toDoubleOrNull() ?: run {
                        println("CsvDataAnalyzer: Failed to parse enhanced longitude: '${parts[5]}'")
                        return null
                    }, // Enhanced longitude (column 6)
                    originalLatitude = parts[2].toDoubleOrNull() ?: run {
                        println("CsvDataAnalyzer: Failed to parse original latitude: '${parts[2]}'")
                        return null
                    }, // Original latitude (column 3)
                    originalLongitude = parts[3].toDoubleOrNull() ?: run {
                        println("CsvDataAnalyzer: Failed to parse original longitude: '${parts[3]}'")
                        return null
                    }, // Original longitude (column 4)
                    accuracy = parts[7].toFloatOrNull() ?: run {
                        println("CsvDataAnalyzer: Failed to parse enhanced accuracy: '${parts[7]}'")
                        return null
                    }, // Enhanced accuracy (column 8)
                    originalAccuracy = parts[6].toFloatOrNull() ?: run {
                        println("CsvDataAnalyzer: Failed to parse original accuracy: '${parts[6]}'")
                        return null
                    }, // Original accuracy (column 7)
                    altitude = parts[8].toDoubleOrNull() ?: 0.0, // Altitude (column 9)
                    bearing = parts[9].toFloatOrNull() ?: 0f, // Bearing (column 10)
                    speed = parts[10].toFloatOrNull() ?: 0f, // Speed (column 11)
                    direction = parts[11], // Direction (column 12)
                    isStationary = parts[16].toBooleanStrictOrNull() ?: false, // is_stationary (column 17)
                    environment = parts[12], // Environment (column 13)
                    operatingMode = parts[13], // Operating mode (column 14)
                    accuracyImprovement = parts[14].toFloatOrNull() ?: 0f, // Accuracy improvement (column 15)
                    confidence = parts[15].toFloatOrNull() ?: 0f, // Confidence (column 16)
                    processingTime = parts[17].toLongOrNull() ?: 0L // Processing time (column 18)
                )
                println("CsvDataAnalyzer: Successfully parsed record at ${record.timestamp}")
                record
            } else {
                println("CsvDataAnalyzer: Line has only ${parts.size} parts, expected 18+")
                println("CsvDataAnalyzer: Parts: ${parts.take(5)}")  // Show first 5 parts for debugging
                null
            }
        } catch (e: Exception) {
            println("CsvDataAnalyzer: Error parsing line: ${e.message}")
            println("CsvDataAnalyzer: Problematic line: ${line.take(100)}...")  // Show first 100 chars
            e.printStackTrace()
            null
        }
    }
    
    private fun createEnhancementSteps(record: LocationRecord): EnhancementSteps {
        // Simulate the enhancement pipeline steps based on improvement
        val improvementFactor = record.accuracyImprovement / 100f
        val originalAccuracy = record.originalAccuracy.toDouble()
        
        // Simulate intermediate steps (these would be actual values in real implementation)
        val signalProcessingImprovement = improvementFactor * 0.3
        val kalmanFilterImprovement = improvementFactor * 0.4
        val swarmOptimizationImprovement = improvementFactor * 0.3
        
        return EnhancementSteps(
            originalAccuracy = originalAccuracy,
            signalProcessingAccuracy = originalAccuracy * (1 - signalProcessingImprovement),
            kalmanFilterAccuracy = originalAccuracy * (1 - signalProcessingImprovement - kalmanFilterImprovement),
            swarmOptimizationAccuracy = originalAccuracy * (1 - signalProcessingImprovement - kalmanFilterImprovement - swarmOptimizationImprovement),
            finalAccuracy = record.enhancedAccuracy.toDouble(),
            environmentClassification = record.environment,
            confidenceScore = record.confidence.toDouble()
        )
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters
        
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLatRad = Math.toRadians(lat2 - lat1)
        val deltaLonRad = Math.toRadians(lon2 - lon1)
        
        val a = sin(deltaLatRad / 2).pow(2.0) + 
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLonRad / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLonRad = Math.toRadians(lon2 - lon1)
        
        val y = sin(deltaLonRad) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLonRad)
        
        val bearingRad = atan2(y, x)
        return (Math.toDegrees(bearingRad) + 360) % 360
    }
    
    private fun calculateAccuracyImprovement(original: Float, enhanced: Float): Double {
        return if (original > 0) {
            ((original - enhanced) / original * 100).toDouble()
        } else 0.0
    }
} 