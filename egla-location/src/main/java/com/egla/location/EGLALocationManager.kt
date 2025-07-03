package com.egla.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.egla.location.core.*
import com.egla.location.service.EGLALocationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Enhanced GNSS Location Accuracy Manager
 * 
 * Main singleton interface for the EGLA library providing:
 * - Advanced GPS accuracy enhancement
 * - Multi-sensor fusion
 * - Machine learning-based filtering
 * - Real-time optimization
 * 
 * Usage:
 * ```kotlin
 * val eglaManager = EGLALocationManager.getInstance(context)
 * eglaManager.configure(EGLAConfiguration.highAccuracyMode())
 * eglaManager.startLocationUpdates()
 * eglaManager.locationUpdates.collect { enhancedLocation ->
 *     // Use enhanced location with improved accuracy
 * }
 * ```
 */
class EGLALocationManager private constructor(
    private val context: Context
) {
    
    // Core Components
    private var fusionEngine: SensorFusionEngine? = null
    private var signalProcessor: SignalProcessor? = null
    private var environmentClassifier: EnvironmentClassifier? = null
    private var swarmOptimizer: SwarmOptimizer? = null
    private var kalmanFilter: ExtendedKalmanFilter? = null
    
    // Configuration and State
    private var configuration: EGLAConfiguration = EGLAConfiguration.balancedMode()
    private var isActive: Boolean = false
    private var lastKnownLocation: EnhancedLocation? = null
    
    // Flows – preferred reactive API
    private val _locationUpdates = MutableSharedFlow<EnhancedLocation>(extraBufferCapacity = 1)
    private val _accuracyMetrics = MutableStateFlow(AccuracyMetrics(0f,0f,0f,0f,0,0f))
    private val _systemStatus = MutableStateFlow(SystemStatus.INACTIVE)
    
    // Coroutine Management
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var processingJob: Job? = null
    
    // Performance Monitoring
    private val performanceMetrics = PerformanceMetrics()
    private var startTime: Long = 0L
    
    /**
     * Stream of enhanced location updates
     */
    val locationUpdates: SharedFlow<EnhancedLocation> = _locationUpdates.asSharedFlow()
    
    /**
     * Stream of accuracy metrics and statistics
     */
    val accuracyMetrics: StateFlow<AccuracyMetrics> = _accuracyMetrics
    
    /**
     * Stream of system status updates
     */
    val systemStatus: StateFlow<SystemStatus> = _systemStatus
    
    /**
     * Current enhanced location (null if no fix available)
     */
    val currentLocation: EnhancedLocation?
        get() = lastKnownLocation
    
    /**
     * Whether the location manager is currently active
     */
    val isRunning: Boolean
        get() = isActive
    
    /**
     * Configure the EGLA system with specified parameters
     */
    fun configure(config: EGLAConfiguration) {
        Timber.d("Configuring EGLA with mode: ${config.mode}")
        
        this.configuration = config
        
        // Reinitialize components with new configuration
        if (isActive) {
            stopLocationUpdates()
            initializeComponents()
            startLocationUpdates()
        } else {
            initializeComponents()
        }
        
        _systemStatus.value = SystemStatus.CONFIGURED
    }
    
    /**
     * Start enhanced location updates
     */
    fun startLocationUpdates(): Boolean {
        if (!hasLocationPermissions()) {
            Timber.e("Location permissions not granted")
            _systemStatus.value = SystemStatus.PERMISSION_DENIED
            return false
        }
        
        if (isActive) {
            Timber.w("Location updates already running")
            return true
        }
        
        try {
            initializeComponents()
            startProcessingLoop()
            
            isActive = true
            startTime = System.currentTimeMillis()
            
            _systemStatus.value = SystemStatus.ACTIVE
            Timber.i("Enhanced location updates started")
            
            return true
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to start location updates")
            _systemStatus.value = SystemStatus.ERROR
            return false
        }
    }
    
    /**
     * Stop enhanced location updates
     */
    fun stopLocationUpdates() {
        if (!isActive) return
        
        processingJob?.cancel()
        isActive = false
        
        // Stop all components
        fusionEngine?.stop()
        signalProcessor?.stop()
        environmentClassifier?.stop()
        swarmOptimizer?.stop()
        
        _systemStatus.value = SystemStatus.STOPPED
        Timber.i("Enhanced location updates stopped")
    }
    
    /**
     * Get current performance metrics
     */
    fun getPerformanceMetrics(): PerformanceMetrics {
        return performanceMetrics.copy(
            uptime = if (isActive) System.currentTimeMillis() - startTime else 0L
        )
    }
    
    /**
     * Force an immediate location update
     */
    fun requestImmediateUpdate() {
        if (!isActive) return
        
        scope.launch {
            try {
                val rawLocation = fusionEngine?.getCurrentRawLocation()
                if (rawLocation != null) {
                    processLocationUpdate(rawLocation)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get immediate location update")
            }
        }
    }
    
    /**
     * Release all resources
     */
    fun release() {
        stopLocationUpdates()
        scope.cancel()
        
        // No further emissions after canceling scope
        
        Timber.d("EGLA LocationManager released")
    }
    
    // Private Implementation
    
    private fun initializeComponents() {
        try {
            // Initialize Kalman Filter
            kalmanFilter = ExtendedKalmanFilter(configuration.kalmanConfig)
            
            // Initialize Sensor Fusion Engine
            fusionEngine = SensorFusionEngine(
                context = context,
                config = configuration.sensorFusionConfig,
                kalmanFilter = kalmanFilter!!
            )
            
            // Initialize Signal Processor
            signalProcessor = SignalProcessor(configuration.signalProcessingConfig)
            
            // Initialize Environment Classifier (if ML enabled)
            if (configuration.mlConfig.enabled) {
                environmentClassifier = EnvironmentClassifier(
                    context = context,
                    config = configuration.mlConfig
                )
            }
            
            // Initialize Swarm Optimizer (if enabled)
            if (configuration.swarmConfig.enabled) {
                swarmOptimizer = SwarmOptimizer(configuration.swarmConfig)
            }
            
            Timber.d("All EGLA components initialized successfully")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize EGLA components")
            throw e
        }
    }
    
    private fun startProcessingLoop() {
        processingJob = scope.launch {
            while (isActive) {
                try {
                    val processingStart = System.currentTimeMillis()
                    
                    // Get raw location from fusion engine
                    val rawLocation = fusionEngine?.getCurrentLocation()
                    if (rawLocation != null) {
                        processLocationUpdate(rawLocation)
                    }
                    
                    // Update performance metrics
                    val processingTime = System.currentTimeMillis() - processingStart
                    performanceMetrics.addProcessingTime(processingTime)
                    
                    // Adaptive delay based on configuration
                    delay(configuration.performanceConfig.updateIntervalMs)
                    
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Timber.e(e, "Error in processing loop")
                    delay(1000) // Brief delay before retry
                }
            }
        }
    }
    
    private suspend fun processLocationUpdate(rawLocation: Location) = withContext(Dispatchers.Default) {
        try {
            val processingStart = System.currentTimeMillis()
            
            // Step 1: Signal Processing Enhancement
            val processedLocation = signalProcessor?.enhanceLocation(rawLocation) ?: rawLocation
            
            // Step 2: Environment Classification (if enabled)
            val environment = environmentClassifier?.classifyEnvironment(processedLocation)
            
            // Step 3: Adaptive Kalman Filtering
            val filteredLocation = kalmanFilter?.update(processedLocation, environment)
                ?: processedLocation
            
            // Step 4: Swarm Optimization (if enabled)
            val optimizedLocation = if (configuration.swarmConfig.enabled) {
                swarmOptimizer?.optimize(filteredLocation) ?: filteredLocation
            } else {
                filteredLocation
            }
            
            // Step 5: Create Enhanced Location
            val enhancedLocation = EnhancedLocation(
                originalLocation = rawLocation,
                enhancedLocation = optimizedLocation,
                accuracyImprovement = calculateAccuracyImprovement(rawLocation, optimizedLocation),
                confidence = calculateConfidence(optimizedLocation, environment),
                environment = environment,
                processingTime = System.currentTimeMillis() - processingStart,
                timestamp = System.currentTimeMillis()
            )
            
            // Update state and emit
            lastKnownLocation = enhancedLocation
            _locationUpdates.tryEmit(enhancedLocation)
            
            // Update metrics
            updateAccuracyMetrics(enhancedLocation)
            
        } catch (e: Exception) {
            Timber.e(e, "Error processing location update")
        }
    }
    
    private fun calculateAccuracyImprovement(original: Location, enhanced: Location): Float {
        val originalAccuracy = original.accuracy
        val enhancedAccuracy = enhanced.accuracy
        
        return if (originalAccuracy > 0 && enhancedAccuracy > 0) {
            ((originalAccuracy - enhancedAccuracy) / originalAccuracy) * 100f
        } else {
            0f
        }
    }
    
    private fun calculateConfidence(location: Location, environment: Environment?): Float {
        var confidence = 0.5f // Base confidence
        
        // Factor in accuracy
        if (location.accuracy < 5.0f) confidence += 0.3f
        else if (location.accuracy < 10.0f) confidence += 0.2f
        else if (location.accuracy < 20.0f) confidence += 0.1f
        
        // Factor in environment
        environment?.let { env ->
            when (env.type) {
                Environment.Type.OPEN_SKY -> confidence += 0.2f
                Environment.Type.URBAN -> confidence += 0.0f
                Environment.Type.INDOOR -> confidence -= 0.2f
                Environment.Type.UNKNOWN -> confidence -= 0.1f
            }
        }
        
        return confidence.coerceIn(0f, 1f)
    }
    
    private fun updateAccuracyMetrics(enhancedLocation: EnhancedLocation) {
        performanceMetrics.addAccuracyMeasurement(
            enhancedLocation.enhancedLocation.accuracy,
            enhancedLocation.accuracyImprovement
        )
        
        val metrics = AccuracyMetrics(
            averageAccuracy = performanceMetrics.averageAccuracy,
            averageImprovement = performanceMetrics.averageImprovement,
            bestAccuracy = performanceMetrics.bestAccuracy,
            worstAccuracy = performanceMetrics.worstAccuracy,
            totalFixes = performanceMetrics.totalFixes,
            successRate = performanceMetrics.successRate
        )
        
        _accuracyMetrics.value = metrics
    }
    
    private fun hasLocationPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        return fineLocation || coarseLocation
    }
    
    companion object {
        @Volatile
        private var INSTANCE: EGLALocationManager? = null
        
        private val instances = ConcurrentHashMap<String, EGLALocationManager>()
        
        /**
         * Get the singleton instance of EGLALocationManager
         */
        fun getInstance(context: Context): EGLALocationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EGLALocationManager(context.applicationContext).also { 
                    INSTANCE = it
                    
                    // Initialize Timber logging
                    if (Timber.treeCount == 0) {
                        Timber.plant(Timber.DebugTree())
                    }
                }
            }
        }
        
        /**
         * Get a named instance for multi-instance scenarios
         */
        fun getInstance(context: Context, instanceName: String): EGLALocationManager {
            return instances.getOrPut(instanceName) {
                EGLALocationManager(context.applicationContext)
            }
        }
    }
    
    // Data Classes
    
    data class EnhancedLocation(
        val originalLocation: Location,
        val enhancedLocation: Location,
        val accuracyImprovement: Float,
        val confidence: Float,
        val environment: Environment?,
        val processingTime: Long,
        val timestamp: Long
    )
    
    data class AccuracyMetrics(
        val averageAccuracy: Float,
        val averageImprovement: Float,
        val bestAccuracy: Float,
        val worstAccuracy: Float,
        val totalFixes: Int,
        val successRate: Float
    )
    
    enum class SystemStatus {
        INACTIVE,
        CONFIGURED,
        ACTIVE,
        STOPPED,
        ERROR,
        PERMISSION_DENIED
    }
    
    data class PerformanceMetrics(
        private val accuracyMeasurements: MutableList<Float> = mutableListOf(),
        private val improvementMeasurements: MutableList<Float> = mutableListOf(),
        private val processingTimes: MutableList<Long> = mutableListOf(),
        var uptime: Long = 0L
    ) {
        val averageAccuracy: Float
            get() = if (accuracyMeasurements.isNotEmpty()) {
                accuracyMeasurements.average().toFloat()
            } else 0f
            
        val averageImprovement: Float
            get() = if (improvementMeasurements.isNotEmpty()) {
                improvementMeasurements.average().toFloat()
            } else 0f
            
        val bestAccuracy: Float
            get() = accuracyMeasurements.minOrNull() ?: 0f
            
        val worstAccuracy: Float
            get() = accuracyMeasurements.maxOrNull() ?: 0f
            
        val totalFixes: Int
            get() = accuracyMeasurements.size
            
        val successRate: Float
            get() = if (totalFixes > 0) {
                (improvementMeasurements.count { it > 0 }.toFloat() / totalFixes) * 100f
            } else 0f
            
        fun addAccuracyMeasurement(accuracy: Float, improvement: Float) {
            accuracyMeasurements.add(accuracy)
            improvementMeasurements.add(improvement)
            
            // Keep only last 1000 measurements for memory efficiency
            if (accuracyMeasurements.size > 1000) {
                accuracyMeasurements.removeAt(0)
                improvementMeasurements.removeAt(0)
            }
        }
        
        fun addProcessingTime(time: Long) {
            processingTimes.add(time)
            if (processingTimes.size > 100) {
                processingTimes.removeAt(0)
            }
        }
    }
} 