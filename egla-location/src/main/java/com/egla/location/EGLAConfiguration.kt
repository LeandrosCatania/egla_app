package com.egla.location

import android.location.LocationManager

/**
 * Comprehensive configuration system for Enhanced GNSS Location Accuracy Library
 * Provides multiple preset modes and fine-grained control over all enhancement algorithms
 */
data class EGLAConfiguration(
    // === Operating Mode ===
    val mode: OperatingMode = OperatingMode.BALANCED,
    
    // === Kalman Filter Parameters ===
    val kalmanConfig: KalmanFilterConfig = KalmanFilterConfig(),
    
    // === Sensor Fusion Settings ===
    val sensorFusionConfig: SensorFusionConfig = SensorFusionConfig(),
    
    // === Signal Processing ===
    val signalProcessingConfig: SignalProcessingConfig = SignalProcessingConfig(),
    
    // === Machine Learning ===
    val mlConfig: MachineLearningConfig = MachineLearningConfig(),
    
    // === Swarm Optimization ===
    val swarmConfig: SwarmOptimizationConfig = SwarmOptimizationConfig(),
    
    // === Advanced Features ===
    val advancedConfig: AdvancedConfig = AdvancedConfig(),
    
    // === Performance & Power ===
    val performanceConfig: PerformanceConfig = PerformanceConfig()
) {
    
    enum class OperatingMode {
        POWER_SAVE,     // Minimal processing, longest battery life
        BALANCED,       // Good accuracy with reasonable power consumption
        HIGH_ACCURACY,  // Maximum accuracy with moderate power usage
        ULTRA_HIGH      // All algorithms enabled, highest power consumption
    }
    
    data class KalmanFilterConfig(
        val enabled: Boolean = true,
        val processNoiseVariance: Double = 0.1,
        val measurementNoiseVariance: Double = 5.0,
        val initialPositionCovariance: Double = 100.0,
        val initialVelocityCovariance: Double = 10.0,
        val adaptiveNoise: Boolean = true,
        val innovationGating: Boolean = true,
        val gatingThreshold: Double = 9.21 // Chi-squared 99% confidence
    )
    
    data class SensorFusionConfig(
        val enabled: Boolean = true,
        val accelerometerWeight: Float = 0.3f,
        val gyroscopeWeight: Float = 0.25f,
        val magnetometerWeight: Float = 0.2f,
        val gpsWeight: Float = 0.25f,
        val sensorSampleRate: Int = 50, // Hz
        val motionDetectionThreshold: Float = 0.5f,
        val stillnessTimeout: Long = 5000L // milliseconds
    )
    
    data class SignalProcessingConfig(
        val multiFrequencyEnabled: Boolean = true,
        val carrierPhaseSmoothing: Boolean = true,
        val multipathMitigation: Boolean = true,
        val correlationLength: Int = 1023,
        val integrationTime: Double = 0.02, // seconds
        val cnr0Threshold: Double = 35.0, // dB-Hz
        val elevationMask: Double = 15.0, // degrees
        val dopFilter: Boolean = true
    )
    
    data class MachineLearningConfig(
        val enabled: Boolean = true,
        val environmentClassification: Boolean = true,
        val multipathDetection: Boolean = true,
        val anomalyDetection: Boolean = true,
        val adaptiveLearning: Boolean = true,
        val modelUpdateInterval: Long = 300000L, // 5 minutes
        val confidenceThreshold: Float = 0.8f
    )
    
    data class SwarmOptimizationConfig(
        val enabled: Boolean = false, // Computationally intensive
        val particleCount: Int = 20,
        val maxIterations: Int = 50,
        val inertiaWeight: Double = 0.729,
        val cognitiveWeight: Double = 1.494,
        val socialWeight: Double = 1.494,
        val convergenceThreshold: Double = 0.001
    )
    
    data class AdvancedConfig(
        val rtkEnabled: Boolean = false, // Requires base station
        val pppEnabled: Boolean = false, // Requires corrections
        val threeDMappingAssist: Boolean = false, // Requires map data
        val ionosphericCorrection: Boolean = true,
        val troposphericCorrection: Boolean = true,
        val relativisticCorrection: Boolean = true,
        val satelliteClockCorrection: Boolean = true
    )
    
    data class PerformanceConfig(
        val updateIntervalMs: Long = 1000L,
        val maxProcessingTimeMs: Long = 500L,
        val backgroundProcessing: Boolean = true,
        val batteryOptimization: Boolean = true,
        val thermalThrottling: Boolean = true,
        val memoryLimit: Int = 50 * 1024 * 1024, // 50MB
        val cacheSize: Int = 1000 // positions
    )
    
    companion object {
        /**
         * Creates configuration optimized for power saving
         */
        fun powerSaveMode(): EGLAConfiguration {
            return EGLAConfiguration(
                mode = OperatingMode.POWER_SAVE,
                kalmanConfig = KalmanFilterConfig(
                    processNoiseVariance = 0.2,
                    adaptiveNoise = false
                ),
                sensorFusionConfig = SensorFusionConfig(
                    sensorSampleRate = 25,
                    accelerometerWeight = 0.4f,
                    gpsWeight = 0.6f
                ),
                signalProcessingConfig = SignalProcessingConfig(
                    multiFrequencyEnabled = false,
                    carrierPhaseSmoothing = false,
                    multipathMitigation = false
                ),
                mlConfig = MachineLearningConfig(
                    enabled = false
                ),
                swarmConfig = SwarmOptimizationConfig(
                    enabled = false
                ),
                performanceConfig = PerformanceConfig(
                    updateIntervalMs = 2000L,
                    batteryOptimization = true,
                    thermalThrottling = true
                )
            )
        }
        
        /**
         * Creates balanced configuration for most use cases
         */
        fun balancedMode(): EGLAConfiguration {
            return EGLAConfiguration(
                mode = OperatingMode.BALANCED
                // Uses default values which are already balanced
            )
        }
        
        /**
         * Creates configuration for high accuracy applications
         */
        fun highAccuracyMode(): EGLAConfiguration {
            return EGLAConfiguration(
                mode = OperatingMode.HIGH_ACCURACY,
                kalmanConfig = KalmanFilterConfig(
                    processNoiseVariance = 0.05,
                    measurementNoiseVariance = 3.0,
                    adaptiveNoise = true,
                    innovationGating = true
                ),
                sensorFusionConfig = SensorFusionConfig(
                    sensorSampleRate = 100,
                    accelerometerWeight = 0.35f,
                    gyroscopeWeight = 0.3f,
                    magnetometerWeight = 0.25f,
                    gpsWeight = 0.1f
                ),
                signalProcessingConfig = SignalProcessingConfig(
                    multiFrequencyEnabled = true,
                    carrierPhaseSmoothing = true,
                    multipathMitigation = true,
                    cnr0Threshold = 30.0,
                    elevationMask = 10.0
                ),
                mlConfig = MachineLearningConfig(
                    enabled = true,
                    environmentClassification = true,
                    multipathDetection = true,
                    anomalyDetection = true
                ),
                performanceConfig = PerformanceConfig(
                    updateIntervalMs = 500L,
                    maxProcessingTimeMs = 800L
                )
            )
        }
        
        /**
         * Creates configuration for ultra-high accuracy (research/surveying)
         */
        fun ultraHighAccuracyMode(): EGLAConfiguration {
            return EGLAConfiguration(
                mode = OperatingMode.ULTRA_HIGH,
                kalmanConfig = KalmanFilterConfig(
                    processNoiseVariance = 0.01,
                    measurementNoiseVariance = 1.0,
                    adaptiveNoise = true,
                    innovationGating = true,
                    gatingThreshold = 6.25 // 95% confidence
                ),
                sensorFusionConfig = SensorFusionConfig(
                    sensorSampleRate = 200,
                    accelerometerWeight = 0.4f,
                    gyroscopeWeight = 0.35f,
                    magnetometerWeight = 0.2f,
                    gpsWeight = 0.05f
                ),
                signalProcessingConfig = SignalProcessingConfig(
                    multiFrequencyEnabled = true,
                    carrierPhaseSmoothing = true,
                    multipathMitigation = true,
                    cnr0Threshold = 25.0,
                    elevationMask = 5.0,
                    dopFilter = true
                ),
                mlConfig = MachineLearningConfig(
                    enabled = true,
                    environmentClassification = true,
                    multipathDetection = true,
                    anomalyDetection = true,
                    adaptiveLearning = true,
                    confidenceThreshold = 0.95f
                ),
                swarmConfig = SwarmOptimizationConfig(
                    enabled = true,
                    particleCount = 50,
                    maxIterations = 100
                ),
                advancedConfig = AdvancedConfig(
                    rtkEnabled = true,
                    pppEnabled = true,
                    threeDMappingAssist = true,
                    ionosphericCorrection = true,
                    troposphericCorrection = true
                ),
                performanceConfig = PerformanceConfig(
                    updateIntervalMs = 100L,
                    maxProcessingTimeMs = 2000L,
                    batteryOptimization = false,
                    thermalThrottling = false
                )
            )
        }
    }
} 