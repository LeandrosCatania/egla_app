package com.example.eglatracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.eglatracker.utils.CsvDataAnalyzer
import com.example.eglatracker.utils.DatabaseLogger
import com.example.eglatracker.utils.DatabaseConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class AnalysisUiState(
    val isLoading: Boolean = false,
    val availableFiles: List<File> = emptyList(),
    val selectedFile: File? = null,
    val locationAnalyses: List<CsvDataAnalyzer.LocationAnalysis> = emptyList(),
    val sessionSummary: CsvDataAnalyzer.SessionSummary? = null,
    val errorMessage: String? = null,
    val selectedPointIndex: Int? = null,
    val databaseModeMessage: String? = "📊 Your app now uses the database! CSV analysis is temporarily unavailable. Check the database at http://localhost:3000/api/location/recent",
    val currentServerUrl: String = "10.0.2.2:3000",
    val isConnectedToDatabase: Boolean = false,
    val serverConnectionStatus: String = "Not tested"
)

class AnalysisViewModel(application: Application) : AndroidViewModel(application) {
    
    private val csvAnalyzer = CsvDataAnalyzer(application)
    private val databaseLogger = DatabaseLogger(application)
    private val databaseConfig = DatabaseConfig.getInstance(application)
    
    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState = _uiState.asStateFlow()
    
    init {
        // Initialize with current server URL
        _uiState.value = _uiState.value.copy(
            currentServerUrl = databaseConfig.getServerUrl()
        )
        loadAvailableFiles()
    }
    
    fun loadAvailableFiles() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                val files = csvAnalyzer.getAvailableFiles()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    availableFiles = files,
                    selectedFile = files.firstOrNull(),
                    databaseModeMessage = "📊 Database Mode Active! Your location data is now stored in the database instead of CSV files. " +
                            "Check live data at: http://localhost:3000/api/location/recent"
                )
                
                // Show info about database mode instead of loading files
                if (files.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "💡 No CSV files found - this is expected! Your app now uses the database. " +
                                "Start location tracking to see data in the database."
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Database mode is active. CSV analysis is no longer used. Check your database server at http://localhost:3000"
                )
            }
        }
    }
    
    fun loadFile(file: File) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true, 
                    errorMessage = null,
                    selectedFile = file,
                    locationAnalyses = emptyList(),
                    sessionSummary = null,
                    selectedPointIndex = null
                )
                
                println("Loading file: ${file.absolutePath}")
                println("File exists: ${file.exists()}")
                println("File size: ${file.length()} bytes")
                
                val analyses = csvAnalyzer.analyzeFile(file)
                println("Parsed ${analyses.size} location records")
                
                val summary = csvAnalyzer.generateSummary(analyses)
                println("Generated summary: ${summary.totalPoints} points")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    locationAnalyses = analyses,
                    sessionSummary = summary
                )
                
            } catch (e: Exception) {
                println("Error loading file: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to analyze file: ${e.message}"
                )
            }
        }
    }
    
    fun selectPoint(index: Int) {
        _uiState.value = _uiState.value.copy(selectedPointIndex = index)
    }
    
    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedPointIndex = null)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun refreshData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true, 
                    errorMessage = null,
                    locationAnalyses = emptyList(),
                    sessionSummary = null,
                    selectedPointIndex = null
                )
                
                val files = csvAnalyzer.getAvailableFiles()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    availableFiles = files,
                    selectedFile = files.firstOrNull(),
                    currentServerUrl = databaseConfig.getServerUrl()
                )
                
                // Auto-load the most recent file if available
                files.firstOrNull()?.let { loadFile(it) }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to refresh files: ${e.message}"
                )
            }
        }
    }
    
    fun updateServerUrl(newUrl: String) {
        viewModelScope.launch {
            try {
                // Update the database logger's URL (this will also update the config)
                databaseLogger.updateServerUrl(newUrl)
                
                _uiState.value = _uiState.value.copy(
                    currentServerUrl = databaseConfig.getServerUrl(),
                    serverConnectionStatus = "Updated - test connection to verify",
                    isConnectedToDatabase = false
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update server URL: ${e.message}"
                )
            }
        }
    }
    
    fun testDatabaseConnection() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    serverConnectionStatus = "Testing..."
                )
                
                // Test database connection using a health check
                val testResult = testConnection()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isConnectedToDatabase = testResult,
                    serverConnectionStatus = if (testResult) "✅ Connected" else "❌ Connection failed"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isConnectedToDatabase = false,
                    serverConnectionStatus = "❌ Error: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun testConnection(): Boolean {
        return try {
            // Use the unified DatabaseService for consistency
            val databaseService = com.example.eglatracker.network.DatabaseService.getInstance(getApplication())
            when (databaseService.testConnection()) {
                is com.example.eglatracker.network.DatabaseResult.Success -> true
                is com.example.eglatracker.network.DatabaseResult.Error -> false
            }
        } catch (e: Exception) {
            false
        }
    }
} 