package com.example.eglatracker.utils

import android.content.Context
import android.os.Environment
import com.example.eglatracker.data.LocationRecord
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for logging location data to CSV files
 */
class CsvLogger(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val logDirectory = File(context.getExternalFilesDir(null), "egla_logs")
    private var currentLogFile: File? = null
    private var csvWriter: CSVWriter? = null
    
    init {
        createLogDirectory()
    }
    
    /**
     * Create log directory if it doesn't exist
     */
    private fun createLogDirectory() {
        if (!logDirectory.exists()) {
            val created = logDirectory.mkdirs()
            if (created) {
                Timber.d("Created log directory: ${logDirectory.absolutePath}")
            } else {
                Timber.e("Failed to create log directory")
            }
        }
    }
    
    /**
     * Start a new logging session
     */
    suspend fun startLogging(): Boolean = withContext(Dispatchers.IO) {
        try {
            stopLogging() // Stop any existing session
            
            val timestamp = dateFormat.format(Date())
            currentLogFile = File(logDirectory, "egla_location_log_$timestamp.csv")
            
            val fileWriter = FileWriter(currentLogFile, true)
            csvWriter = CSVWriter(fileWriter)
            
            // Write header if file is new
            if (currentLogFile?.length() == 0L) {
                val header = LocationRecord.getCsvHeader().split(",").toTypedArray()
                csvWriter?.writeNext(header)
                csvWriter?.flush()
            }
            
            Timber.i("Started logging to: ${currentLogFile?.absolutePath}")
            true
        } catch (e: IOException) {
            Timber.e(e, "Failed to start CSV logging")
            false
        }
    }
    
    /**
     * Log a location record to CSV
     */
    suspend fun logLocation(record: LocationRecord): Boolean = withContext(Dispatchers.IO) {
        try {
            csvWriter?.let { writer ->
                val csvData = record.toCsv().split(",").toTypedArray()
                writer.writeNext(csvData)
                writer.flush()
                true
            } ?: false
        } catch (e: IOException) {
            Timber.e(e, "Failed to write location record")
            false
        }
    }
    
    /**
     * Stop logging session
     */
    suspend fun stopLogging() = withContext(Dispatchers.IO) {
        try {
            csvWriter?.close()
            csvWriter = null
            
            currentLogFile?.let { file ->
                Timber.i("Stopped logging. File: ${file.absolutePath}, Size: ${file.length()} bytes")
            }
            
            currentLogFile = null
        } catch (e: IOException) {
            Timber.e(e, "Error stopping CSV logging")
        }
    }
    
    /**
     * Get current log file
     */
    fun getCurrentLogFile(): File? = currentLogFile
    
    /**
     * Get all log files
     */
    fun getAllLogFiles(): List<File> {
        return try {
            logDirectory.listFiles { file ->
                file.isFile && file.name.endsWith(".csv")
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Error getting log files")
            emptyList()
        }
    }
    
    /**
     * Get log directory path
     */
    fun getLogDirectoryPath(): String = logDirectory.absolutePath
    
    /**
     * Delete old log files (keep only last N files)
     */
    suspend fun cleanupOldLogs(keepCount: Int = 10) = withContext(Dispatchers.IO) {
        try {
            val allFiles = getAllLogFiles()
            if (allFiles.size > keepCount) {
                val filesToDelete = allFiles.drop(keepCount)
                filesToDelete.forEach { file ->
                    if (file.delete()) {
                        Timber.d("Deleted old log file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning up old logs")
        }
    }
    
    /**
     * Get total size of all log files in bytes
     */
    fun getTotalLogSize(): Long {
        return try {
            getAllLogFiles().sumOf { it.length() }
        } catch (e: Exception) {
            Timber.e(e, "Error calculating total log size")
            0L
        }
    }
    
    /**
     * Format file size for display
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    /**
     * Check if external storage is available for writing
     */
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
    
    /**
     * Get logging statistics
     */
    fun getLoggingStats(): LoggingStats {
        val files = getAllLogFiles()
        return LoggingStats(
            totalFiles = files.size,
            totalSizeBytes = getTotalLogSize(),
            currentLogFile = currentLogFile?.name,
            isLogging = csvWriter != null,
            logDirectory = logDirectory.absolutePath
        )
    }
    
    data class LoggingStats(
        val totalFiles: Int,
        val totalSizeBytes: Long,
        val currentLogFile: String?,
        val isLogging: Boolean,
        val logDirectory: String
    )
} 