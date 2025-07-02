package com.example.eglatracker.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.egla.location.EGLAConfiguration
import com.egla.location.EGLALocationManager
import com.egla.location.core.Environment
import com.example.eglatracker.data.LocationRecord
import com.example.eglatracker.data.LogLevel
import com.example.eglatracker.data.LogTag
import com.example.eglatracker.utils.DatabaseLogger
import com.example.eglatracker.utils.DirectionCalculator
import com.example.eglatracker.utils.LoggingManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced ViewModel with real EGLA integration and comprehensive logging
 * 
 * @deprecated Use LocationTrackingViewModelV2 instead, which uses the new EGLA API architecture
 */
@Deprecated(
    message = "Use LocationTrackingViewModelV2 which uses the EGLA Client API",
    replaceWith = ReplaceWith("LocationTrackingViewModelV2")
)
class LocationTrackingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val databaseLogger = DatabaseLogger(application)
    private val loggingManager = LoggingManager.getInstance()
    
    /**
     * Update database server URL (for when user changes it in Analysis screen)
     */
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
    private val disposables = CompositeDisposable()
    
    // EGLA components
    private var eglaManager: EGLALocationManager? = null
    private var isEGLAInitialized = false
    
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
        log("ViewModel initialized - Starting EGLA setup...")
        
        // Add some initial structured logs to demonstrate the system
        loggingManager.info(LogTag.SYSTEM, "EGLA Location Tracker initialized")
        loggingManager.info(LogTag.SESSION, "New tracking session starting")
        loggingManager.debug(LogTag.EGLA, "Initializing EGLA Location Manager")
        loggingManager.info(LogTag.DATABASE, "Database connection will be established when tracking starts")
        
        initializeEGLAGradually()
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
        
        // Log to structured logging system based on message content
        val logLevel = when {
            message.contains("❌") -> LogLevel.ERROR
            message.contains("⚠️") -> LogLevel.WARNING
            message.contains("✓") || message.contains("✅") || message.contains("🎉") -> LogLevel.SUCCESS
            message.contains("🚀") || message.contains("📍") -> LogLevel.INFO
            else -> LogLevel.DEBUG
        }
        
        val logTag = when {
            message.contains("EGLA") -> LogTag.EGLA
            message.contains("GPS") || message.contains("location") -> LogTag.LOCATION
            message.contains("Database") || message.contains("database") -> LogTag.DATABASE
            message.contains("sensor") -> LogTag.SENSOR
            message.contains("tracking") || message.contains("Tracking") -> LogTag.SESSION
            message.contains("Error") || message.contains("Failed") -> LogTag.ERROR_RECOVERY
            else -> LogTag.SYSTEM
        }
        
        loggingManager.log(logLevel, logTag, message.replace(Regex("[🔍ℹ️✅⚠️❌🚨📍💾🌐📡🔬⚙️👤⚡🎯🔄]"), "").trim())
    }
    
    private fun initializeEGLAGradually() {
        viewModelScope.launch {
            try {
                log("Step 1/5: Checking EGLA library availability...")
                delay(500) // Give UI time to update
                
                withContext(Dispatchers.IO) {
                    log("Step 2/5: Initializing EGLA LocationManager...")
                    
                    withTimeout(10000) { // 10 second timeout
                        eglaManager = EGLALocationManager.getInstance(getApplication())
                        log("✓ EGLA LocationManager created successfully")
                    }
                }
                
                log("Step 3/5: Configuring EGLA for high accuracy mode...")
                delay(200)
                
                withContext(Dispatchers.IO) {
                    withTimeout(5000) {
                        eglaManager?.configure(EGLAConfiguration.highAccuracyMode())
                        log("✓ EGLA configured for high accuracy mode")
                    }
                }
                
                log("Step 4/5: Setting up location update subscriptions...")
                delay(200)
                setupEGLASubscriptions()
                
                log("Step 5/5: EGLA initialization complete!")
                isEGLAInitialized = true
                updateUiState { it.copy(systemStatus = "EGLA Ready") }
                log("🎉 System ready for location tracking")
                
            } catch (e: Exception) {
                log("❌ EGLA initialization failed: ${e.message}")
                log("🔄 Falling back to enhanced simulation mode...")
                isEGLAInitialized = false
                updateUiState { it.copy(systemStatus = "Simulation Mode (EGLA Failed)") }
            }
        }
    }
    
    private fun setupEGLASubscriptions() {
        if (eglaManager == null) {
            log("❌ Cannot setup subscriptions - EGLA manager is null")
            return
        }
        
        try {
            log("Setting up location updates subscription...")
            
            disposables.add(
                eglaManager!!.locationUpdates
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { enhancedLocation -> 
                            log("📍 Received REAL GPS location update")
                            log("📊 Real GPS accuracy: ${enhancedLocation.originalLocation.accuracy}m")
                            processEGLALocationUpdate(enhancedLocation) 
                        },
                        { error -> 
                            log("❌ Location update error: ${error.message}")
                            handleError("Location error: ${error.message}") 
                        }
                    )
            )
            
            log("Setting up system status subscription...")
            
            disposables.add(
                eglaManager!!.systemStatus
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { status ->
                        log("📊 EGLA status changed: $status")
                        _isTracking.value = (status == EGLALocationManager.SystemStatus.ACTIVE)
                        updateUiState { it.copy(systemStatus = status.name) }
                    }
            )
            
            log("✓ All subscriptions set up successfully")
            log("⏳ Waiting for device GPS to acquire location fix...")
            
        } catch (e: Exception) {
            log("❌ Failed to setup subscriptions: ${e.message}")
        }
    }
    
    fun startTracking() {
        viewModelScope.launch {
            try {
                log("🚀 Starting location tracking...")
                loggingManager.info(LogTag.SESSION, "Starting location tracking session")
                
                val loggingStarted = databaseLogger.startLogging()
                if (!loggingStarted) {
                    log("❌ Failed to start database logging")
                    log("⚠️ Make sure the local database server is running!")
                    loggingManager.error(LogTag.DATABASE, "Failed to start database logging", "Check server connection at configured IP address")
                    handleError("Failed to start database logging - check server connection")
                    return@launch
                }
                
                log("✅ Database logging started")
                log("📊 Device ID: ${databaseLogger.deviceId}")
                log("📝 Session: ${databaseLogger.getCurrentSession()}")
                loggingManager.success(LogTag.DATABASE, "Database logging session started", "Device: ${databaseLogger.deviceId}, Session: ${databaseLogger.getCurrentSession()}")
                updateUiState { it.copy(currentLogFile = "Database: ${databaseLogger.getCurrentSession()}") }
                
                if (isEGLAInitialized && eglaManager != null) {
                    log("🎯 Starting EGLA location updates...")
                    log("📱 Connecting to device GPS and sensors...")
                    
                    withTimeout(5000) {
                        val started = eglaManager!!.startLocationUpdates()
                        if (started) {
                            log("✓ EGLA location updates started successfully")
                            log("📡 Waiting for real GPS data from device...")
                        } else {
                            log("❌ EGLA failed to start - check permissions")
                            handleError("Failed to start EGLA - check location permissions")
                        }
                    }
                } else {
                    log("❌ EGLA not available - initialization failed")
                    handleError("EGLA initialization failed")
                }
                
            } catch (e: Exception) {
                log("❌ Error starting tracking: ${e.message}")
                handleError("Failed to start tracking: ${e.message}")
            }
        }
    }
    
    private fun startSimulationMode() {
        log("🎮 Starting enhanced simulation mode...")
        _isTracking.value = true
        
        viewModelScope.launch {
            while (_isTracking.value) {
                try {
                    log("📍 Generating simulated location update...")
                    simulateRealisticLocationUpdate()
                    delay(3000) // Update every 3 seconds
                } catch (e: Exception) {
                    log("❌ Error in simulation: ${e.message}")
                    delay(5000)
                }
            }
        }
    }
    
    private fun processEGLALocationUpdate(enhancedLocation: EGLALocationManager.EnhancedLocation) {
        val currentLocation = enhancedLocation.enhancedLocation
        val currentTime = System.currentTimeMillis()
        
        log("🔄 Processing EGLA location update...")
        log("📊 Original accuracy: ${enhancedLocation.originalLocation.accuracy}m")
        log("📊 Enhanced accuracy: ${currentLocation.accuracy}m")
        log("📊 Improvement: ${String.format("%.1f", enhancedLocation.accuracyImprovement)}%")
        
        // Calculate direction and movement
        val (direction, bearing, isStationary, speed) = calculateMovement(
            currentLocation, currentTime
        )
        
        log("🧭 Direction: $direction, Speed: ${String.format("%.1f", speed * 3.6f)} km/h")
        
        // Create location record
        val record = LocationRecord(
            timestamp = System.currentTimeMillis(),
            latitude = enhancedLocation.enhancedLocation.latitude,
            longitude = enhancedLocation.enhancedLocation.longitude,
            originalLatitude = enhancedLocation.originalLocation.latitude,
            originalLongitude = enhancedLocation.originalLocation.longitude,
            accuracy = enhancedLocation.enhancedLocation.accuracy,
            originalAccuracy = enhancedLocation.originalLocation.accuracy,
            altitude = enhancedLocation.enhancedLocation.altitude ?: 0.0,
            bearing = enhancedLocation.enhancedLocation.bearing,
            speed = enhancedLocation.enhancedLocation.speed,
            direction = DirectionCalculator.getDetailedDirection(enhancedLocation.enhancedLocation.bearing),
            isStationary = enhancedLocation.enhancedLocation.speed < 0.5f,
            environment = enhancedLocation.environment?.type?.name ?: "UNKNOWN",
            operatingMode = _uiState.value.eglaMode,
            accuracyImprovement = enhancedLocation.accuracyImprovement,
            confidence = enhancedLocation.confidence,
            processingTime = enhancedLocation.processingTime
        )
        
        // Log to Database
        viewModelScope.launch {
            val success = databaseLogger.logLocation(record)
            if (success) {
                log("💾 Location data sent to database")
                loggingManager.debug(LogTag.DATABASE, "Location record saved to database", "Lat: ${record.latitude}, Lng: ${record.longitude}, Accuracy: ${record.accuracy}m")
            } else {
                log("❌ Failed to send location data to database")
                loggingManager.error(LogTag.DATABASE, "Failed to save location record", "Check database server connection")
            }
        }
        
        // Update UI state
        updateUiState { state ->
            state.copy(
                currentRecord = record,
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                accuracy = currentLocation.accuracy,
                originalAccuracy = enhancedLocation.originalLocation.accuracy,
                accuracyImprovement = enhancedLocation.accuracyImprovement,
                direction = direction,
                bearing = bearing,
                speed = speed,
                isStationary = isStationary,
                environment = enhancedLocation.environment?.type?.name ?: "UNKNOWN",
                confidence = enhancedLocation.confidence,
                processingTime = enhancedLocation.processingTime,
                lastUpdateTime = currentTime
            )
        }
        
        // Update previous location for next calculation
        previousLocation = Location(currentLocation)
        previousTimestamp = currentTime
        
        log("✓ Location update processed successfully")
    }
    
    private fun simulateRealisticLocationUpdate() {
        val baseAccuracy = 4.0f + (Math.random() * 8).toFloat()
        val enhancementFactor = 0.35f + (Math.random() * 0.45f).toFloat()
        
        val mockLocation = Location("enhanced_simulation").apply {
            latitude = 40.7128 + (Math.random() - 0.5) * 0.003
            longitude = -74.0060 + (Math.random() - 0.5) * 0.003
            accuracy = baseAccuracy
            time = System.currentTimeMillis()
            
            if (Math.random() > 0.7) {
                speed = (Math.random() * 6).toFloat()
            }
        }
        
        val enhancedAccuracy = baseAccuracy * enhancementFactor
        val improvement = ((baseAccuracy - enhancedAccuracy) / baseAccuracy) * 100f
        
        log("🎮 Simulated: ${String.format("%.1f", baseAccuracy)}m → ${String.format("%.1f", enhancedAccuracy)}m (${String.format("%.1f", improvement)}% improvement)")
        
        processSimulatedLocationUpdate(mockLocation, baseAccuracy, enhancementFactor)
    }
    
    private fun processSimulatedLocationUpdate(currentLocation: Location, originalAccuracy: Float, enhancementFactor: Float) {
        val currentTime = System.currentTimeMillis()
        
        val (direction, bearing, isStationary, speed) = calculateMovement(
            currentLocation, currentTime
        )
        
        val enhancedAccuracy = originalAccuracy * enhancementFactor
        val improvement = ((originalAccuracy - enhancedAccuracy) / originalAccuracy) * 100f
        
        val environment = when {
            enhancedAccuracy < 2.0f -> "OPEN_SKY"
            enhancedAccuracy < 5.0f -> "SUBURBAN" 
            enhancedAccuracy < 12.0f -> "URBAN"
            else -> "CHALLENGING"
        }
        
        val record = LocationRecord(
            timestamp = currentTime,
            latitude = currentLocation.latitude,
            longitude = currentLocation.longitude,
            originalLatitude = currentLocation.latitude,
            originalLongitude = currentLocation.longitude,
            accuracy = enhancedAccuracy,
            originalAccuracy = originalAccuracy,
            altitude = if (currentLocation.hasAltitude()) currentLocation.altitude else 0.0,
            bearing = bearing,
            speed = speed,
            direction = direction,
            isStationary = isStationary,
            environment = environment,
            operatingMode = _uiState.value.eglaMode,
            accuracyImprovement = improvement,
            confidence = 0.75f + (Math.random() * 0.2f).toFloat(),
            processingTime = (10 + Math.random() * 15).toLong()
        )
        
        viewModelScope.launch {
            val success = databaseLogger.logLocation(record)
            if (!success) {
                log("❌ Failed to send simulated location to database")
            }
        }
        
        updateUiState { state ->
            state.copy(
                currentRecord = record,
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                accuracy = enhancedAccuracy,
                originalAccuracy = originalAccuracy,
                accuracyImprovement = improvement,
                direction = direction,
                bearing = bearing,
                speed = speed,
                isStationary = isStationary,
                environment = environment,
                confidence = record.confidence,
                processingTime = record.processingTime,
                lastUpdateTime = currentTime
            )
        }
        
        previousLocation = Location(currentLocation)
        previousTimestamp = currentTime
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
    
    fun stopTracking() {
        viewModelScope.launch {
            log("⏹️ Stopping location tracking...")
            
            _isTracking.value = false
            
            if (isEGLAInitialized && eglaManager != null) {
                try {
                    withTimeout(3000) {
                        eglaManager!!.stopLocationUpdates()
                        log("✓ EGLA location updates stopped")
                    }
                } catch (e: Exception) {
                    log("⚠️ Error stopping EGLA: ${e.message}")
                }
            }
            
            databaseLogger.stopLogging()
            log("✅ Database logging stopped")
            
            previousLocation = null
            previousTimestamp = 0L
            
            updateUiState { 
                it.copy(
                    currentLogFile = null,
                    direction = "Stopped",
                    systemStatus = "STOPPED"
                )
            }
            
            log("🛑 Location tracking stopped")
        }
    }
    
    private fun handleError(message: String) {
        log("❌ Error: $message")
        updateUiState { it.copy(errorMessage = message) }
    }
    
    suspend fun getDatabaseStats(): DatabaseLogger.DatabaseStats {
        return databaseLogger.getDeviceStats()
    }
    
    fun clearError() {
        updateUiState { it.copy(errorMessage = null) }
    }
    
    fun changeToBalancedMode() {
        viewModelScope.launch {
            log("🔄 Changing to Balanced mode...")
            try {
                if (isEGLAInitialized && eglaManager != null) {
                    eglaManager!!.configure(EGLAConfiguration.balancedMode())
                    log("✓ EGLA configured for Balanced mode")
                }
                updateUiState { it.copy(eglaMode = "Balanced") }
            } catch (e: Exception) {
                log("❌ Failed to change mode: ${e.message}")
            }
        }
    }
    
    fun changeToHighAccuracyMode() {
        viewModelScope.launch {
            log("🔄 Changing to High Accuracy mode...")
            try {
                if (isEGLAInitialized && eglaManager != null) {
                    eglaManager!!.configure(EGLAConfiguration.highAccuracyMode())
                    log("✓ EGLA configured for High Accuracy mode")
                }
                updateUiState { it.copy(eglaMode = "High Accuracy") }
            } catch (e: Exception) {
                log("❌ Failed to change mode: ${e.message}")
            }
        }
    }
    
    fun changeToUltraHighMode() {
        viewModelScope.launch {
            log("🔄 Changing to Ultra High mode...")
            try {
                if (isEGLAInitialized && eglaManager != null) {
                    eglaManager!!.configure(EGLAConfiguration.ultraHighAccuracyMode())
                    log("✓ EGLA configured for Ultra High mode")
                }
                updateUiState { it.copy(eglaMode = "Ultra High") }
            } catch (e: Exception) {
                log("❌ Failed to change mode: ${e.message}")
            }
        }
    }
    
    private fun updateUiState(update: (LocationTrackingUiState) -> LocationTrackingUiState) {
        _uiState.value = update(_uiState.value)
    }
    
    override fun onCleared() {
        super.onCleared()
        log("🧹 Cleaning up ViewModel...")
        
        disposables.clear()
        
        viewModelScope.launch {
            databaseLogger.stopLogging()
            
            if (eglaManager != null) {
                try {
                    withTimeout(2000) {
                        eglaManager!!.release()
                        log("✓ EGLA resources released")
                    }
                } catch (e: Exception) {
                    log("⚠️ Error during cleanup: ${e.message}")
                }
            }
        }
        
        Timber.d("LocationTrackingViewModel cleared")
    }
    
    private data class MovementData(
        val direction: String,
        val bearing: Float,
        val isStationary: Boolean,
        val speed: Float
    )
}

/**
 * UI State for the location tracking screen with comprehensive logging
 */
data class LocationTrackingUiState(
    // Current location data
    val currentRecord: LocationRecord? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val originalAccuracy: Float = 0f,
    val accuracyImprovement: Float = 0f,
    
    // Movement data
    val direction: String = "No Data",
    val bearing: Float = 0f,
    val speed: Float = 0f,
    val isStationary: Boolean = true,
    
    // Environment and confidence
    val environment: String = "UNKNOWN",
    val confidence: Float = 0f,
    val processingTime: Long = 0L,
    
    // Metrics
    val averageAccuracy: Float = 0f,
    val averageImprovement: Float = 0f,
    val totalFixes: Int = 0,
    val successRate: Float = 0f,
    
    // System state
    val systemStatus: String = "INITIALIZING",
    val eglaMode: String = "High Accuracy",
    val lastUpdateTime: Long = 0L,
    
    // Logging
    val currentLogFile: String? = null,
    val systemLogs: List<String> = emptyList(),
    
    // Error handling
    val errorMessage: String? = null
) 