package com.egla.location.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import com.egla.location.EGLAConfiguration
import kotlin.math.sqrt

/**
 * Multi-sensor fusion engine combining GPS with inertial sensors
 * for enhanced accuracy and reduced position drift
 */
class SensorFusionEngine(
    private val context: Context,
    private val config: EGLAConfiguration.SensorFusionConfig,
    private val kalmanFilter: ExtendedKalmanFilter
) : SensorEventListener, LocationListener {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    // Sensors
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    
    // Sensor Data
    private var lastAcceleration = FloatArray(3)
    private var lastGyroscope = FloatArray(3)
    private var lastMagnetometer = FloatArray(3)
    private var lastGpsLocation: Location? = null
    
    // Motion Detection
    private var isStationary = false
    private var lastMotionTime = System.currentTimeMillis()
    
    // State
    private var isRunning = false
    
    fun start() {
        if (isRunning) return
        
        // Register sensor listeners
        accelerometer?.let {
            sensorManager.registerListener(
                this, it, 
                SensorManager.SENSOR_DELAY_GAME // ~50Hz
            )
        }
        
        gyroscope?.let {
            sensorManager.registerListener(
                this, it, 
                SensorManager.SENSOR_DELAY_GAME
            )
        }
        
        magnetometer?.let {
            sensorManager.registerListener(
                this, it, 
                SensorManager.SENSOR_DELAY_GAME
            )
        }
        
        // Register location listener
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                config.sensorSampleRate.toLong(),
                0f,
                this
            )
        } catch (e: SecurityException) {
            // Handle permission error
        }
        
        isRunning = true
    }
    
    fun stop() {
        if (!isRunning) return
        
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)
        
        isRunning = false
    }
    
    fun getCurrentLocation(): Location? {
        return lastGpsLocation?.let { gpsLocation ->
            // Apply sensor fusion enhancement
            enhanceLocationWithSensors(gpsLocation)
        }
    }
    
    fun getCurrentRawLocation(): Location? {
        return lastGpsLocation
    }
    
    private fun enhanceLocationWithSensors(gpsLocation: Location): Location {
        val enhancedLocation = Location(gpsLocation)
        
        // Apply motion compensation if device is moving
        if (!isStationary) {
            applyMotionCompensation(enhancedLocation)
        }
        
        // Apply orientation correction
        applyOrientationCorrection(enhancedLocation)
        
        // Calculate weighted accuracy improvement
        val weightedAccuracy = calculateWeightedAccuracy(gpsLocation.accuracy)
        enhancedLocation.accuracy = weightedAccuracy
        
        return enhancedLocation
    }
    
    private fun applyMotionCompensation(location: Location) {
        // Use accelerometer data to detect and compensate for motion
        val motionMagnitude = sqrt(
            lastAcceleration[0] * lastAcceleration[0] +
            lastAcceleration[1] * lastAcceleration[1] +
            lastAcceleration[2] * lastAcceleration[2]
        )
        
        // If significant motion detected, apply compensation
        if (motionMagnitude > config.motionDetectionThreshold) {
            // Calculate motion vector and apply to position
            // This is a simplified implementation
            val deltaTime = 0.02f // 50Hz sensor rate
            val velocity = motionMagnitude * deltaTime
            
            // Apply minimal motion compensation to reduce jitter
            val compensationFactor = 0.1f
            location.latitude += (lastAcceleration[1] * velocity * compensationFactor) / 111320.0
            location.longitude += (lastAcceleration[0] * velocity * compensationFactor) / 111320.0
        }
    }
    
    private fun applyOrientationCorrection(location: Location) {
        // Use magnetometer and gyroscope for orientation-based corrections
        if (lastMagnetometer.all { it != 0f } && lastGyroscope.all { it != 0f }) {
            // Calculate device orientation and apply position corrections
            // This is a simplified implementation
            val orientationCorrection = calculateOrientationCorrection()
            
            // Apply small corrections based on device orientation
            val correctionFactor = 0.05f
            location.latitude += orientationCorrection.first * correctionFactor / 111320.0
            location.longitude += orientationCorrection.second * correctionFactor / 111320.0
        }
    }
    
    private fun calculateOrientationCorrection(): Pair<Double, Double> {
        // Simplified orientation correction calculation
        val rotationMatrix = FloatArray(9)
        val inclinationMatrix = FloatArray(9)
        
        val success = SensorManager.getRotationMatrix(
            rotationMatrix, inclinationMatrix,
            lastAcceleration, lastMagnetometer
        )
        
        return if (success) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            
            // Convert orientation to position corrections
            val azimuth = orientation[0]
            val pitch = orientation[1]
            
            Pair(pitch.toDouble(), azimuth.toDouble())
        } else {
            Pair(0.0, 0.0)
        }
    }
    
    private fun calculateWeightedAccuracy(gpsAccuracy: Float): Float {
        var totalWeight = 0f
        var weightedSum = 0f
        
        // GPS contribution
        weightedSum += gpsAccuracy * config.gpsWeight
        totalWeight += config.gpsWeight
        
        // Sensor contributions (based on motion state)
        if (isStationary) {
            // When stationary, sensors provide better accuracy
            val sensorAccuracy = 2.0f // Estimated sensor accuracy when stationary
            
            weightedSum += sensorAccuracy * config.accelerometerWeight
            weightedSum += sensorAccuracy * config.gyroscopeWeight
            weightedSum += sensorAccuracy * config.magnetometerWeight
            
            totalWeight += config.accelerometerWeight
            totalWeight += config.gyroscopeWeight
            totalWeight += config.magnetometerWeight
        } else {
            // When moving, GPS is more reliable
            val movementPenalty = 1.5f
            weightedSum += (gpsAccuracy * movementPenalty) * (1.0f - config.gpsWeight)
            totalWeight += (1.0f - config.gpsWeight)
        }
        
        return if (totalWeight > 0) weightedSum / totalWeight else gpsAccuracy
    }
    
    private fun updateMotionState() {
        val currentTime = System.currentTimeMillis()
        
        // Calculate motion magnitude
        val motionMagnitude = sqrt(
            lastAcceleration[0] * lastAcceleration[0] +
            lastAcceleration[1] * lastAcceleration[1] +
            lastAcceleration[2] * lastAcceleration[2]
        )
        
        // Update motion state
        if (motionMagnitude > config.motionDetectionThreshold) {
            lastMotionTime = currentTime
            isStationary = false
        } else if (currentTime - lastMotionTime > config.stillnessTimeout) {
            isStationary = true
        }
    }
    
    // SensorEventListener implementation
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                lastAcceleration = event.values.clone()
                updateMotionState()
            }
            Sensor.TYPE_GYROSCOPE -> {
                lastGyroscope = event.values.clone()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                lastMagnetometer = event.values.clone()
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Handle sensor accuracy changes
    }
    
    // LocationListener implementation
    override fun onLocationChanged(location: Location) {
        lastGpsLocation = location
    }
    
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
} 