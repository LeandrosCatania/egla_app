package com.example.eglatracker.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.egla.location.api.EnhancedLocationData
import com.egla.location.api.LocationConfiguration
import com.egla.location.client.EGLALocationClient
import com.example.eglatracker.data.LocationRecord
import com.example.eglatracker.data.LogLevel
import com.example.eglatracker.data.LogTag
import com.example.eglatracker.utils.DatabaseLogger
import com.egla.core.DirectionCalculator
import com.example.eglatracker.utils.LoggingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 * Refactored ViewModel using EGLA Client API
 * 
 * This demonstrates how to use the new EGLA API architecture where EGLA runs
 * as a separate service and the app communicates through the client library.
 */
@HiltViewModel
class LocationTrackingViewModelV2 @Inject constructor(
    private val eglaClient: EGLALocationClient,
    private val databaseLogger: DatabaseLogger,
    private val loggingManager: LoggingManager
) : ViewModel() {
    
    // System logging
    private val systemLogs = mutableListOf<String>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    
    // Previous location for direction calculation
    private var previousLocation: Location? = null
    private var previousTimestamp: Long = 0L
    
    // UI State
    private val _uiState = MutableStateFlow(LocationTrackingUiState())
    val uiState: StateFlow<LocationTrackingUiState> = _uiState.asStateFlow()
    
    // Location tracking state
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    init {
        log("ViewModel initialized - Using EGLA API Client...")
        loggingManager.info(LogTag.SYSTEM, "EGLA Location Tracker initialized with API architecture")
        
        connectToEGLAService()
    }
    
    private fun connectToEGLAService() {
        log("Connecting to EGLA API Service...")
        
        // Launch Flow collectors in viewModelScope
        viewModelScope.launch {
            eglaClient.connectionStateFlow.collect { state ->
                when (state) {
                    EGLALocationClient.ConnectionState.CONNECTED -> {
                        log("✓ Connected to EGLA API Service")
                        updateUiState { it.copy(systemStatus = "EGLA API Connected") }
                        setupEGLASubscriptions()
                    }
                    EGLALocationClient.ConnectionState.DISCONNECTED -> {
                        log("❌ Disconnected from EGLA API Service")
                        updateUiState { it.copy(systemStatus = "EGLA API Disconnected") }
                    }
                    EGLALocationClient.ConnectionState.CONNECTING -> {
                        log("🔄 Connecting to EGLA API Service...")
                        updateUiState { it.copy(systemStatus = "Connecting...") }
                    }
                }
            }
        }

        viewModelScope.launch {
            eglaClient.errorsFlow.collect { error ->
                log("❌ EGLA API Error ${error.code}: ${error.message}")
                handleError("EGLA API Error: ${error.message}")
            }
        }

        // Initiate connection
        if (!eglaClient.connect()) {
            log("❌ Failed to initiate connection to EGLA API Service")
            updateUiState { it.copy(systemStatus = "EGLA API Connection Failed") }
        }
    }
    
    private fun setupEGLASubscriptions() {
        log("Setting up EGLA API subscriptions…")
        
        // Location updates
        viewModelScope.launch {
            eglaClient.locationFlow.collect { location ->
                log("📍 Received location update from EGLA API")
                processLocationUpdate(location)
            }
        }

        // Service status updates
        viewModelScope.launch {
            eglaClient.serviceStatusFlow.collect { status ->
                log("📊 EGLA Service status: $status")
                _isTracking.value = (status == EGLALocationClient.ServiceStatus.ACTIVE)

                val statusText = when (status) {
                    EGLALocationClient.ServiceStatus.IDLE -> "Ready"
                    EGLALocationClient.ServiceStatus.ACTIVE -> "Tracking"
                    EGLALocationClient.ServiceStatus.ERROR -> "Error"
                }
                updateUiState { it.copy(systemStatus = "EGLA: $statusText") }
            }
        }

        log("✓ EGLA API subscriptions ready")
    }
    
    fun startTracking() {
        viewModelScope.launch {
            try {
                log("🚀 Starting location tracking via EGLA API...")
                loggingManager.info(LogTag.SESSION, "Starting location tracking session")
                
                // Start database logging
                val loggingStarted = databaseLogger.startLogging()
                if (!loggingStarted) {
                    log("❌ Failed to start database logging")
                    loggingManager.error(LogTag.DATABASE, "Failed to start database logging")
                    handleError("Failed to start database logging - check server connection")
                    return@launch
                }
                
                log("✅ Database logging started")
                log("📊 Device ID: ${databaseLogger.deviceId}")
                log("📝 Session: ${databaseLogger.getCurrentSession()}")
                updateUiState { it.copy(currentLogFile = "Database: ${databaseLogger.getCurrentSession()}") }
                
                // Configure EGLA
                log("⚙️ Configuring EGLA for high accuracy mode...")
                val configured = eglaClient.configure(LocationConfiguration.highAccuracyMode())
                
                if (configured) {
                    log("✓ EGLA configured successfully")
                    
                    // Start location updates
                    log("🎯 Starting EGLA location updates...")
                    val started = eglaClient.startLocationUpdates()
                    
                    if (started) {
                        log("✓ EGLA location updates started")
                        log("📡 Waiting for GPS data...")
                    } else {
                        log("❌ Failed to start EGLA updates")
                        handleError("Failed to start location updates")
                    }
                } else {
                    log("❌ Failed to configure EGLA")
                    handleError("Failed to configure EGLA service")
                }
                
            } catch (e: Exception) {
                log("❌ Error starting tracking: ${e.message}")
                handleError("Failed to start tracking: ${e.message}")
            }
        }
    }
    
    fun stopTracking() {
        viewModelScope.launch {
            log("⏹️ Stopping location tracking...")
            
            _isTracking.value = false
            
            databaseLogger.stopLogging()
            log("✓ Database logging stopped")
            
            updateUiState { it.copy(systemStatus = "Stopped") }
            
            loggingManager.info(LogTag.SESSION, "Location tracking session ended")
        }
    }
    
    fun updateDatabaseServerUrl(newUrl: String) {
        viewModelScope.launch {
            try {
                databaseLogger.updateServerUrl(newUrl)
                loggingManager.success(LogTag.DATABASE, "Database server URL updated", "New URL: $newUrl")
            } catch (e: Exception) {
                loggingManager.error(LogTag.DATABASE, "Failed to update database server URL", e.message)
            }
        }
    }
    
    suspend fun getDatabaseStats() = databaseLogger.getDeviceStats()
    
    fun changeToBalancedMode() {
        eglaClient.configure(LocationConfiguration.balancedMode())
        updateUiState { it.copy(eglaMode = "Balanced") }
        log("⚙️ Switched to Balanced mode")
    }
    
    fun changeToHighAccuracyMode() {
        eglaClient.configure(LocationConfiguration.highAccuracyMode())
        updateUiState { it.copy(eglaMode = "High Accuracy") }
        log("⚙️ Switched to High Accuracy mode")
    }
    
    fun changeToUltraHighMode() {
        eglaClient.configure(LocationConfiguration.ultraHighAccuracyMode())
        updateUiState { it.copy(eglaMode = "Ultra High") }
        log("⚙️ Switched to Ultra High Accuracy mode")
    }
    
    fun clearError() {
        updateUiState { it.copy(errorMessage = null) }
    }
    
    private fun processLocationUpdate(location: EnhancedLocationData) {
        val currentTime = System.currentTimeMillis()
        
        log("🔄 Processing EGLA API location update...")
        log("📊 Enhanced accuracy: ${location.accuracy}m")
        log("📊 Original accuracy: ${location.originalAccuracy}m")
        log("📊 Improvement: ${String.format("%.1f", location.accuracyImprovement)}%")
        
        // Create Location object for direction calculation
        val currentLocation = Location("egla").apply {
            latitude = location.latitude
            longitude = location.longitude
            accuracy = location.accuracy
            time = location.timestamp
            location.altitude?.let { altitude = it }
            location.bearing?.let { bearing = it }
            location.speed?.let { speed = it }
        }
        
        // Calculate direction and movement
        val (direction, bearing, isStationary, speed) = calculateMovement(currentLocation, currentTime)
        
        log("🧭 Direction: $direction, Speed: ${String.format("%.1f", speed * 3.6f)} km/h")
        
        // Create location record
        val record = LocationRecord(
            timestamp = location.timestamp,
            latitude = location.latitude,
            longitude = location.longitude,
            originalLatitude = location.originalLatitude,
            originalLongitude = location.originalLongitude,
            accuracy = location.accuracy,
            originalAccuracy = location.originalAccuracy,
            altitude = location.altitude ?: 0.0,
            bearing = location.bearing ?: 0f,
            speed = location.speed ?: 0f,
            direction = direction,
            isStationary = isStationary,
            environment = location.environment ?: "UNKNOWN",
            operatingMode = _uiState.value.eglaMode,
            accuracyImprovement = location.accuracyImprovement,
            confidence = location.confidence,
            processingTime = location.processingTimeMs
        )
        
        // Log to Database
        viewModelScope.launch {
            val success = databaseLogger.logLocation(record)
            if (success) {
                log("💾 Location data sent to database")
            } else {
                log("❌ Failed to send location data to database")
            }
        }
        
        // Update UI state
        updateUiState { state ->
            state.copy(
                currentRecord = record,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                originalAccuracy = location.originalAccuracy,
                accuracyImprovement = location.accuracyImprovement,
                direction = direction,
                bearing = bearing,
                speed = speed,
                isStationary = isStationary,
                environment = location.environment ?: "UNKNOWN",
                confidence = location.confidence,
                processingTime = location.processingTimeMs,
                lastUpdateTime = currentTime
            )
        }
        
        // Update previous location
        previousLocation = currentLocation
        previousTimestamp = currentTime
        
        log("✓ Location update processed successfully")
    }
    
    private fun calculateMovement(
        currentLocation: Location, 
        currentTime: Long
    ): MovementData {
        val previousLoc = previousLocation
        
        return if (previousLoc != null && previousTimestamp > 0) {
            val distance = DirectionCalculator.calculateDistance(previousLoc, currentLocation)
            val timeDelta = currentTime - previousTimestamp
            val speed = DirectionCalculator.calculateSpeed(distance, timeDelta)
            
            if (DirectionCalculator.isSignificantMovement(distance, threshold = 1.5f)) {
                val bearing = DirectionCalculator.calculateBearing(previousLoc, currentLocation)
                val direction = DirectionCalculator.getDetailedDirection(bearing)
                
                MovementData(
                    direction = direction,
                    bearing = bearing,
                    isStationary = false,
                    speed = speed
                )
            } else {
                MovementData(
                    direction = "Stationary",
                    bearing = 0f,
                    isStationary = true,
                    speed = 0f
                )
            }
        } else {
            MovementData(
                direction = "Initial Position",
                bearing = 0f,
                isStationary = true,
                speed = 0f
            )
        }
    }
    
    private fun log(message: String) {
        val timestamp = timeFormat.format(Date())
        val logEntry = "[$timestamp] $message"
        
        systemLogs.add(logEntry)
        
        // Keep only last 50 log entries
        if (systemLogs.size > 50) {
            systemLogs.removeAt(0)
        }
        
        // Update UI with new logs
        updateUiState { it.copy(systemLogs = systemLogs.toList()) }
        
        // Also log to Timber
        Timber.d(message)
    }
    
    private fun handleError(message: String) {
        updateUiState { it.copy(errorMessage = message) }
        log("❌ Error: $message")
    }
    
    private fun updateUiState(update: (LocationTrackingUiState) -> LocationTrackingUiState) {
        _uiState.value = update(_uiState.value)
    }
    
    override fun onCleared() {
        super.onCleared()
        
        // Clean up
        eglaClient.stopLocationUpdates()
        eglaClient.disconnect()
        
        log("ViewModel cleared - EGLA API disconnected")
    }
    
    private data class MovementData(
        val direction: String,
        val bearing: Float,
        val isStationary: Boolean,
        val speed: Float
    )
} 