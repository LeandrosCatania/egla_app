package com.egla.config

/**
 * Configuration class for Enhanced GNSS Location Accuracy (EGLA) Library
 */
data class EGLAConfiguration(
    // Operating Mode
    val mode: EGLAMode = EGLAMode.BALANCED,
    
    // Kalman Filter Settings
    val kalmanProcessNoise: Double = 1.0,
    val kalmanMeasurementNoise: Double = 4.0,
    val kalmanPositionCovariance: Double = 500.0,
    val kalmanVelocityCovariance: Double = 100.0,
    
    // Sensor Fusion Settings
    val enableAccelerometer: Boolean = true,
    val enableGyroscope: Boolean = true,
    val enableMagnetometer: Boolean = true,
    val sensorFrequencyHz: Int = 50,
    val accelerometerWeight: Double = 0.3,
    val gyroscopeWeight: Double = 0.2,
    val magnetometerWeight: Double = 0.1,
    
    // GPS Enhancement Settings
    val enableMultiFrequency: Boolean = true,
    val enableMultiConstellation: Boolean = true,
    val minSatelliteCount: Int = 4,
    val maxHorizontalAccuracy: Float = 100.0f,
    val minSignalStrength: Float = 20.0f,
    
    // Machine Learning Settings
    val enableMLEnvironmentClassification: Boolean = true,
    val enableAdaptiveFiltering: Boolean = true,
    val mlModelUpdateIntervalMs: Long = 30000,
    
    // Signal Processing Settings
    val enableMultipathMitigation: Boolean = true,
    val enableCarrierPhaseSmoothing: Boolean = true,
    val smoothingWindowSize: Int = 10,
    val outlierDetectionThreshold: Double = 3.0,
    
    // Optimization Settings
    val enableSwarmOptimization: Boolean = false,
    val swarmPopulationSize: Int = 20,
    val swarmIterations: Int = 50,
    val convergenceThreshold: Double = 0.01,
    
    // Performance Settings
    val processingIntervalMs: Long = 100,
    val metricsUpdateIntervalMs: Long = 1000,
    val maxLocationAge: Long = 5000,
    val enableBatteryOptimization: Boolean = true,
    
    // Advanced Features
    val enableRTKCorrections: Boolean = false,
    val enablePPPCorrections: Boolean = false,
    val enable3DMappingAssisted: Boolean = false,
    val rtkServerUrl: String? = null,
    val mappingDataPath: String? = null,
    
    // Logging and Debugging
    val enableDetailedLogging: Boolean = false,
    val enableMetricsCollection: Boolean = true,
    val logToFile: Boolean = false,
    val logFilePath: String? = null
) {
    companion object {
        /**
         * Create default configuration optimized for balanced performance
         */
        fun createDefault(): EGLAConfiguration = EGLAConfiguration()
        
        /**
         * Create configuration optimized for maximum accuracy
         */
        fun createHighAccuracy(): EGLAConfiguration = EGLAConfiguration(
            mode = EGLAMode.HIGH_ACCURACY,
            kalmanProcessNoise = 0.5,
            kalmanMeasurementNoise = 2.0,
            enableSwarmOptimization = true,
            enableMLEnvironmentClassification = true,
            enableAdaptiveFiltering = true,
            enableMultipathMitigation = true,
            enableCarrierPhaseSmoothing = true,
            processingIntervalMs = 50,
            sensorFrequencyHz = 100,
            enableBatteryOptimization = false
        )
        
        /**
         * Create configuration optimized for power saving
         */
        fun createPowerSave(): EGLAConfiguration = EGLAConfiguration(
            mode = EGLAMode.POWER_SAVE,
            kalmanProcessNoise = 2.0,
            kalmanMeasurementNoise = 8.0,
            enableSwarmOptimization = false,
            enableMLEnvironmentClassification = false,
            enableAdaptiveFiltering = false,
            enableMultipathMitigation = false,
            enableCarrierPhaseSmoothing = false,
            processingIntervalMs = 500,
            sensorFrequencyHz = 25,
            enableBatteryOptimization = true,
            metricsUpdateIntervalMs = 5000
        )
        
        /**
         * Create configuration for ultra-high accuracy applications
         */
        fun createUltraHighAccuracy(): EGLAConfiguration = EGLAConfiguration(
            mode = EGLAMode.ULTRA_HIGH_ACCURACY,
            kalmanProcessNoise = 0.1,
            kalmanMeasurementNoise = 1.0,
            enableSwarmOptimization = true,
            swarmPopulationSize = 50,
            swarmIterations = 100,
            enableMLEnvironmentClassification = true,
            enableAdaptiveFiltering = true,
            enableMultipathMitigation = true,
            enableCarrierPhaseSmoothing = true,
            enable3DMappingAssisted = true,
            enableRTKCorrections = true,
            enablePPPCorrections = true,
            processingIntervalMs = 25,
            sensorFrequencyHz = 200,
            smoothingWindowSize = 20,
            outlierDetectionThreshold = 2.0,
            enableBatteryOptimization = false,
            enableDetailedLogging = true
        )
        
        /**
         * Create configuration for indoor/urban environments
         */
        fun createUrbanOptimized(): EGLAConfiguration = EGLAConfiguration(
            mode = EGLAMode.HIGH_ACCURACY,
            kalmanProcessNoise = 0.8,
            kalmanMeasurementNoise = 6.0,
            enableSwarmOptimization = true,
            enableMLEnvironmentClassification = true,
            enableAdaptiveFiltering = true,
            enableMultipathMitigation = true,
            enableCarrierPhaseSmoothing = true,
            enable3DMappingAssisted = true,
            accelerometerWeight = 0.5,
            gyroscopeWeight = 0.3,
            magnetometerWeight = 0.2,
            outlierDetectionThreshold = 2.5,
            smoothingWindowSize = 15
        )
    }
    
    /**
     * Validate configuration parameters
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (kalmanProcessNoise <= 0) {
            errors.add("Kalman process noise must be positive")
        }
        
        if (kalmanMeasurementNoise <= 0) {
            errors.add("Kalman measurement noise must be positive")
        }
        
        if (sensorFrequencyHz <= 0 || sensorFrequencyHz > 1000) {
            errors.add("Sensor frequency must be between 1 and 1000 Hz")
        }
        
        if (minSatelliteCount < 3) {
            errors.add("Minimum satellite count must be at least 3")
        }
        
        if (processingIntervalMs <= 0) {
            errors.add("Processing interval must be positive")
        }
        
        if (swarmPopulationSize <= 0) {
            errors.add("Swarm population size must be positive")
        }
        
        if (swarmIterations <= 0) {
            errors.add("Swarm iterations must be positive")
        }
        
        if (smoothingWindowSize <= 0) {
            errors.add("Smoothing window size must be positive")
        }
        
        if (accelerometerWeight < 0 || accelerometerWeight > 1) {
            errors.add("Accelerometer weight must be between 0 and 1")
        }
        
        if (gyroscopeWeight < 0 || gyroscopeWeight > 1) {
            errors.add("Gyroscope weight must be between 0 and 1")
        }
        
        if (magnetometerWeight < 0 || magnetometerWeight > 1) {
            errors.add("Magnetometer weight must be between 0 and 1")
        }
        
        return errors
    }
    
    /**
     * Check if configuration is valid
     */
    fun isValid(): Boolean = validate().isEmpty()
}

/**
 * Operating modes for EGLA library
 */
enum class EGLAMode {
    /**
     * Power saving mode - reduced accuracy but better battery life
     */
    POWER_SAVE,
    
    /**
     * Balanced mode - good accuracy with reasonable power consumption
     */
    BALANCED,
    
    /**
     * High accuracy mode - maximum accuracy with higher power consumption
     */
    HIGH_ACCURACY,
    
    /**
     * Ultra high accuracy mode - experimental features enabled,
     * highest power consumption but best possible accuracy
     */
    ULTRA_HIGH_ACCURACY
} 