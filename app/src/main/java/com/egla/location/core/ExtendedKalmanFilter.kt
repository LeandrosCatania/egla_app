package com.egla.location.core

import android.location.Location
import com.egla.location.EGLAConfiguration
import org.ejml.simple.SimpleMatrix
import kotlin.math.*

/**
 * Extended Kalman Filter for optimal GPS position estimation
 * Handles non-linear motion models and adaptive noise estimation
 */
class ExtendedKalmanFilter(
    private val config: EGLAConfiguration.KalmanFilterConfig
) {
    
    // State vector: [x, y, vx, vy] (position and velocity)
    private var state = SimpleMatrix(4, 1)
    
    // Covariance matrix
    private var covariance = SimpleMatrix(4, 4)
    
    // Process noise matrix
    private var processNoise = SimpleMatrix(4, 4)
    
    // Measurement noise matrix
    private var measurementNoise = SimpleMatrix(2, 2)
    
    // State transition matrix
    private var stateTransition = SimpleMatrix(4, 4)
    
    // Measurement matrix
    private var measurementMatrix = SimpleMatrix(2, 4)
    
    // Innovation (residual) tracking for adaptive filtering
    private val innovationHistory = mutableListOf<SimpleMatrix>()
    
    // Initialization flag
    private var isInitialized = false
    
    // Last update time
    private var lastUpdateTime = 0L
    
    init {
        initializeMatrices()
    }
    
    private fun initializeMatrices() {
        // Initialize state transition matrix (constant velocity model)
        stateTransition.set(0, 0, 1.0)  // x = x + vx*dt
        stateTransition.set(0, 2, 1.0)  // dt will be multiplied later
        stateTransition.set(1, 1, 1.0)  // y = y + vy*dt
        stateTransition.set(1, 3, 1.0)  // dt will be multiplied later
        stateTransition.set(2, 2, 1.0)  // vx = vx
        stateTransition.set(3, 3, 1.0)  // vy = vy
        
        // Initialize measurement matrix (observe position only)
        measurementMatrix.set(0, 0, 1.0)  // observe x
        measurementMatrix.set(1, 1, 1.0)  // observe y
        
        // Initialize process noise
        processNoise.set(0, 0, config.processNoiseVariance)
        processNoise.set(1, 1, config.processNoiseVariance)
        processNoise.set(2, 2, config.processNoiseVariance * 0.1) // velocity noise
        processNoise.set(3, 3, config.processNoiseVariance * 0.1)
        
        // Initialize measurement noise
        measurementNoise.set(0, 0, config.measurementNoiseVariance)
        measurementNoise.set(1, 1, config.measurementNoiseVariance)
        
        // Initialize covariance matrix
        covariance.set(0, 0, config.initialPositionCovariance)
        covariance.set(1, 1, config.initialPositionCovariance)
        covariance.set(2, 2, config.initialVelocityCovariance)
        covariance.set(3, 3, config.initialVelocityCovariance)
    }
    
    /**
     * Update the filter with a new GPS measurement
     */
    fun update(location: Location, environment: Environment? = null): Location {
        val currentTime = System.currentTimeMillis()
        
        if (!isInitialized) {
            initialize(location, currentTime)
            return location
        }
        
        val deltaTime = (currentTime - lastUpdateTime) / 1000.0 // Convert to seconds
        lastUpdateTime = currentTime
        
        // Prediction step
        predict(deltaTime)
        
        // Convert location to measurement
        val measurement = locationToMeasurement(location)
        
        // Apply adaptive noise adjustment based on environment
        adjustNoiseForEnvironment(environment, location.accuracy)
        
        // Innovation gating (outlier detection)
        if (config.innovationGating && !passesInnovationGate(measurement)) {
            // Reject this measurement as an outlier
            return measurementToLocation(state, location)
        }
        
        // Update step
        val updatedState = updateStep(measurement)
        
        // Update innovation history for adaptive filtering
        updateInnovationHistory(measurement)
        
        return measurementToLocation(updatedState, location)
    }
    
    private fun initialize(location: Location, time: Long) {
        // Convert GPS coordinates to local Cartesian coordinates
        val coords = gpsToCartesian(location.latitude, location.longitude)
        
        // Initialize state with position and zero velocity
        state.set(0, 0, coords.first)   // x position
        state.set(1, 0, coords.second)  // y position
        state.set(2, 0, 0.0)            // x velocity
        state.set(3, 0, 0.0)            // y velocity
        
        lastUpdateTime = time
        isInitialized = true
    }
    
    private fun predict(deltaTime: Double) {
        // Update state transition matrix with current deltaTime
        val F = stateTransition.copy()
        F.set(0, 2, deltaTime)  // x = x + vx*dt
        F.set(1, 3, deltaTime)  // y = y + vy*dt
        
        // Predict state: x_k|k-1 = F * x_k-1|k-1
        state = F.mult(state)
        
        // Predict covariance: P_k|k-1 = F * P_k-1|k-1 * F^T + Q
        covariance = F.mult(covariance).mult(F.transpose()).plus(processNoise)
    }
    
    private fun updateStep(measurement: SimpleMatrix): SimpleMatrix {
        // Innovation: y = z - H * x_k|k-1
        val innovation = measurement.minus(measurementMatrix.mult(state))
        
        // Innovation covariance: S = H * P_k|k-1 * H^T + R
        val innovationCovariance = measurementMatrix
            .mult(covariance)
            .mult(measurementMatrix.transpose())
            .plus(measurementNoise)
        
        // Kalman gain: K = P_k|k-1 * H^T * S^-1
        val kalmanGain = covariance
            .mult(measurementMatrix.transpose())
            .mult(innovationCovariance.invert())
        
        // Update state: x_k|k = x_k|k-1 + K * y
        state = state.plus(kalmanGain.mult(innovation))
        
        // Update covariance: P_k|k = (I - K * H) * P_k|k-1
        val identity = SimpleMatrix.identity(4)
        covariance = identity.minus(kalmanGain.mult(measurementMatrix)).mult(covariance)
        
        return state
    }
    
    private fun passesInnovationGate(measurement: SimpleMatrix): Boolean {
        if (!config.innovationGating) return true
        
        // Calculate innovation
        val innovation = measurement.minus(measurementMatrix.mult(state))
        
        // Calculate innovation covariance
        val innovationCovariance = measurementMatrix
            .mult(covariance)
            .mult(measurementMatrix.transpose())
            .plus(measurementNoise)
        
        // Calculate Mahalanobis distance
        val mahalanobisDistance = innovation.transpose()
            .mult(innovationCovariance.invert())
            .mult(innovation)
            .get(0, 0)
        
        // Check against threshold (chi-squared distribution)
        return mahalanobisDistance <= config.gatingThreshold
    }
    
    private fun adjustNoiseForEnvironment(environment: Environment?, accuracy: Float) {
        if (!config.adaptiveNoise || environment == null) return
        
        // Adjust measurement noise based on environment
        val noiseFactor = when (environment.type) {
            Environment.Type.OPEN_SKY -> 0.8  // Better conditions
            Environment.Type.URBAN -> 1.5     // Urban canyon effects
            Environment.Type.INDOOR -> 3.0    // Poor GPS conditions
            Environment.Type.UNKNOWN -> 1.2   // Slightly worse
        }
        
        // Adjust based on reported accuracy
        val accuracyFactor = (accuracy / 5.0).coerceIn(0.5, 3.0)
        
        val totalFactor = noiseFactor * accuracyFactor
        
        // Update measurement noise matrix
        measurementNoise.set(0, 0, config.measurementNoiseVariance * totalFactor)
        measurementNoise.set(1, 1, config.measurementNoiseVariance * totalFactor)
    }
    
    private fun updateInnovationHistory(measurement: SimpleMatrix) {
        val innovation = measurement.minus(measurementMatrix.mult(state))
        innovationHistory.add(innovation)
        
        // Keep only recent history
        if (innovationHistory.size > 10) {
            innovationHistory.removeAt(0)
        }
        
        // Adaptive process noise adjustment
        if (config.adaptiveNoise && innovationHistory.size >= 5) {
            adjustProcessNoise()
        }
    }
    
    private fun adjustProcessNoise() {
        // Calculate innovation variance over recent history
        val recentInnovations = innovationHistory.takeLast(5)
        val innovationVariance = calculateInnovationVariance(recentInnovations)
        
        // Adjust process noise based on innovation trends
        val adaptationFactor = (innovationVariance / config.measurementNoiseVariance)
            .coerceIn(0.5, 2.0)
        
        val newProcessNoise = config.processNoiseVariance * adaptationFactor
        
        processNoise.set(0, 0, newProcessNoise)
        processNoise.set(1, 1, newProcessNoise)
        processNoise.set(2, 2, newProcessNoise * 0.1)
        processNoise.set(3, 3, newProcessNoise * 0.1)
    }
    
    private fun calculateInnovationVariance(innovations: List<SimpleMatrix>): Double {
        if (innovations.isEmpty()) return config.measurementNoiseVariance
        
        val mean = SimpleMatrix(2, 1)
        innovations.forEach { innovation ->
            mean.set(0, 0, mean.get(0, 0) + innovation.get(0, 0))
            mean.set(1, 0, mean.get(1, 0) + innovation.get(1, 0))
        }
        mean.scale(1.0 / innovations.size)
        
        var variance = 0.0
        innovations.forEach { innovation ->
            val diff = innovation.minus(mean)
            variance += diff.dot(diff)
        }
        
        return variance / innovations.size
    }
    
    private fun locationToMeasurement(location: Location): SimpleMatrix {
        val coords = gpsToCartesian(location.latitude, location.longitude)
        val measurement = SimpleMatrix(2, 1)
        measurement.set(0, 0, coords.first)
        measurement.set(1, 0, coords.second)
        return measurement
    }
    
    private fun measurementToLocation(state: SimpleMatrix, originalLocation: Location): Location {
        val coords = cartesianToGps(state.get(0, 0), state.get(1, 0))
        
        val enhancedLocation = Location(originalLocation)
        enhancedLocation.latitude = coords.first
        enhancedLocation.longitude = coords.second
        
        // Calculate enhanced accuracy based on covariance
        val positionUncertainty = sqrt(covariance.get(0, 0) + covariance.get(1, 1))
        enhancedLocation.accuracy = positionUncertainty.toFloat().coerceAtMost(originalLocation.accuracy)
        
        return enhancedLocation
    }
    
    private fun gpsToCartesian(latitude: Double, longitude: Double): Pair<Double, Double> {
        // Simple conversion to local Cartesian coordinates
        // For more accuracy, consider using UTM or other projections
        val x = longitude * 111320.0 * cos(Math.toRadians(latitude))
        val y = latitude * 111320.0
        return Pair(x, y)
    }
    
    private fun cartesianToGps(x: Double, y: Double): Pair<Double, Double> {
        // Convert back from local Cartesian to GPS coordinates
        val latitude = y / 111320.0
        val longitude = x / (111320.0 * cos(Math.toRadians(latitude)))
        return Pair(latitude, longitude)
    }
    
    /**
     * Get current velocity estimate
     */
    fun getCurrentVelocity(): Pair<Double, Double> {
        return if (isInitialized) {
            Pair(state.get(2, 0), state.get(3, 0))
        } else {
            Pair(0.0, 0.0)
        }
    }
    
    /**
     * Get current position uncertainty
     */
    fun getPositionUncertainty(): Double {
        return if (isInitialized) {
            sqrt(covariance.get(0, 0) + covariance.get(1, 1))
        } else {
            Double.MAX_VALUE
        }
    }
    
    /**
     * Reset the filter
     */
    fun reset() {
        isInitialized = false
        innovationHistory.clear()
        initializeMatrices()
    }
} 