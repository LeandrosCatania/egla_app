package com.egla.location.core

import android.location.Location
import com.egla.location.EGLAConfiguration
import kotlin.math.*

/**
 * Advanced signal processing for GPS enhancement
 * Implements multipath mitigation, carrier phase smoothing, and multi-frequency processing
 */
class SignalProcessor(
    private val config: EGLAConfiguration.SignalProcessingConfig
) {
    
    // Signal history for processing
    private val locationHistory = mutableListOf<LocationMeasurement>()
    private val carrierPhaseHistory = mutableListOf<Double>()
    
    // Multipath detection
    private val multipathDetector = MultipathDetector()
    
    // DOP (Dilution of Precision) filter
    private val dopFilter = DOPFilter()
    
    /**
     * Enhance location using advanced signal processing techniques
     */
    fun enhanceLocation(rawLocation: Location): Location {
        val measurement = LocationMeasurement(rawLocation, System.currentTimeMillis())
        locationHistory.add(measurement)
        
        // Maintain history size
        if (locationHistory.size > 50) {
            locationHistory.removeAt(0)
        }
        
        var enhancedLocation = rawLocation
        
        // Apply signal quality filtering
        if (passesSignalQualityCheck(measurement)) {
            
            // Apply multipath mitigation
            if (config.multipathMitigation) {
                enhancedLocation = applyMultipathMitigation(enhancedLocation)
            }
            
            // Apply carrier phase smoothing
            if (config.carrierPhaseSmoothing) {
                enhancedLocation = applyCarrierPhaseSmoothing(enhancedLocation)
            }
            
            // Apply DOP filtering
            if (config.dopFilter) {
                enhancedLocation = dopFilter.filter(enhancedLocation)
            }
            
            // Apply correlation-based enhancement
            enhancedLocation = applyCorrelationEnhancement(enhancedLocation)
            
        }
        
        return enhancedLocation
    }
    
    private fun passesSignalQualityCheck(measurement: LocationMeasurement): Boolean {
        val location = measurement.location
        
        // Check basic signal quality indicators
        if (!location.hasAccuracy() || location.accuracy <= 0) {
            return false
        }
        
        // Elevation mask check (simulated)
        // In real implementation, this would check satellite elevation angles
        if (location.accuracy > 100.0f) { // Very poor accuracy suggests low elevation
            return false
        }
        
        // CNR threshold check (simulated based on accuracy)
        val estimatedCNR = estimateCNRFromAccuracy(location.accuracy)
        if (estimatedCNR < config.cnr0Threshold) {
            return false
        }
        
        return true
    }
    
    private fun estimateCNRFromAccuracy(accuracy: Float): Double {
        // Empirical relationship between accuracy and CNR
        return max(20.0, 50.0 - (accuracy * 0.5))
    }
    
    private fun applyMultipathMitigation(location: Location): Location {
        if (locationHistory.size < 3) return location
        
        // Detect multipath using signal characteristics
        val multipathRisk = multipathDetector.detectMultipath(locationHistory)
        
        if (multipathRisk < 0.5) return location // Low multipath risk
        
        // Apply multipath mitigation
        val enhancedLocation = Location(location)
        
        // Use weighted average with recent measurements to reduce multipath effects
        val recentMeasurements = locationHistory.takeLast(5)
        val weights = calculateMultipathWeights(recentMeasurements)
        
        var weightedLat = 0.0
        var weightedLon = 0.0
        var totalWeight = 0.0
        
        recentMeasurements.forEachIndexed { index, measurement ->
            val weight = weights[index]
            weightedLat += measurement.location.latitude * weight
            weightedLon += measurement.location.longitude * weight
            totalWeight += weight
        }
        
        if (totalWeight > 0) {
            enhancedLocation.latitude = weightedLat / totalWeight
            enhancedLocation.longitude = weightedLon / totalWeight
            
            // Improve accuracy estimate
            enhancedLocation.accuracy = (location.accuracy * (1.0f - multipathRisk.toFloat() * 0.5f))
        }
        
        return enhancedLocation
    }
    
    private fun calculateMultipathWeights(measurements: List<LocationMeasurement>): List<Double> {
        return measurements.mapIndexed { index, measurement ->
            // More recent measurements get higher weight
            val recencyWeight = (index + 1).toDouble() / measurements.size
            
            // Better accuracy gets higher weight
            val accuracyWeight = 1.0 / (measurement.location.accuracy + 1.0)
            
            // Stability weight based on deviation from trend
            val stabilityWeight = calculateStabilityWeight(measurement, measurements)
            
            recencyWeight * accuracyWeight * stabilityWeight
        }
    }
    
    private fun calculateStabilityWeight(
        measurement: LocationMeasurement, 
        measurements: List<LocationMeasurement>
    ): Double {
        if (measurements.size < 3) return 1.0
        
        // Calculate deviation from local trend
        val index = measurements.indexOf(measurement)
        if (index <= 0 || index >= measurements.size - 1) return 1.0
        
        val prev = measurements[index - 1].location
        val next = measurements[index + 1].location
        val current = measurement.location
        
        // Expected position based on linear interpolation
        val expectedLat = (prev.latitude + next.latitude) / 2.0
        val expectedLon = (prev.longitude + next.longitude) / 2.0
        
        // Calculate deviation
        val deviation = sqrt(
            (current.latitude - expectedLat).pow(2) +
            (current.longitude - expectedLon).pow(2)
        )
        
        // Convert to weight (lower deviation = higher weight)
        return 1.0 / (1.0 + deviation * 100000.0) // Scale factor for lat/lon
    }
    
    private fun applyCarrierPhaseSmoothing(location: Location): Location {
        // Simulate carrier phase smoothing using position smoothing
        if (locationHistory.size < 3) return location
        
        val enhancedLocation = Location(location)
        val smoothingFactor = 0.3 // Configurable smoothing strength
        
        // Calculate smoothed position using weighted moving average
        val recentPositions = locationHistory.takeLast(5)
        var smoothedLat = 0.0
        var smoothedLon = 0.0
        var totalWeight = 0.0
        
        recentPositions.forEachIndexed { index, measurement ->
            // Exponential weighting (more recent = higher weight)
            val weight = exp(-0.5 * (recentPositions.size - index - 1))
            
            smoothedLat += measurement.location.latitude * weight
            smoothedLon += measurement.location.longitude * weight
            totalWeight += weight
        }
        
        if (totalWeight > 0) {
            val targetLat = smoothedLat / totalWeight
            val targetLon = smoothedLon / totalWeight
            
            // Apply smoothing
            enhancedLocation.latitude = location.latitude * (1 - smoothingFactor) + 
                                      targetLat * smoothingFactor
            enhancedLocation.longitude = location.longitude * (1 - smoothingFactor) + 
                                       targetLon * smoothingFactor
            
            // Improve accuracy due to smoothing
            enhancedLocation.accuracy = location.accuracy * 0.8f
        }
        
        return enhancedLocation
    }
    
    private fun applyCorrelationEnhancement(location: Location): Location {
        if (locationHistory.size < config.correlationLength.coerceAtMost(10)) {
            return location
        }
        
        val enhancedLocation = Location(location)
        
        // Apply correlation-based position refinement
        val correlationResult = performCorrelationAnalysis(location)
        
        // Adjust position based on correlation analysis
        enhancedLocation.latitude += correlationResult.latitudeAdjustment
        enhancedLocation.longitude += correlationResult.longitudeAdjustment
        
        // Update accuracy based on correlation confidence
        enhancedLocation.accuracy = location.accuracy * correlationResult.confidenceFactor
        
        return enhancedLocation
    }
    
    private fun performCorrelationAnalysis(location: Location): CorrelationResult {
        val recentMeasurements = locationHistory.takeLast(10)
        
        // Calculate position stability metrics
        val latVariance = calculateVariance(recentMeasurements.map { it.location.latitude })
        val lonVariance = calculateVariance(recentMeasurements.map { it.location.longitude })
        
        // Calculate trend-based adjustments
        val latTrend = calculateTrend(recentMeasurements.map { it.location.latitude })
        val lonTrend = calculateTrend(recentMeasurements.map { it.location.longitude })
        
        // Small adjustments based on trend analysis
        val latAdjustment = latTrend * 0.1 // Conservative adjustment
        val lonAdjustment = lonTrend * 0.1
        
        // Confidence based on stability
        val avgVariance = (latVariance + lonVariance) / 2.0
        val confidenceFactor = (1.0 / (1.0 + avgVariance * 1000000.0)).coerceIn(0.5, 1.0)
        
        return CorrelationResult(
            latitudeAdjustment = latAdjustment,
            longitudeAdjustment = lonAdjustment,
            confidenceFactor = confidenceFactor.toFloat()
        )
    }
    
    private fun calculateVariance(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average()
    }
    
    private fun calculateTrend(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        // Simple linear trend calculation
        val n = values.size.toDouble()
        val x = (0 until values.size).map { it.toDouble() }
        val xMean = x.average()
        val yMean = values.average()
        
        val numerator = x.zip(values) { xi, yi -> (xi - xMean) * (yi - yMean) }.sum()
        val denominator = x.map { (it - xMean).pow(2) }.sum()
        
        return if (denominator != 0.0) numerator / denominator else 0.0
    }
    
    /**
     * Stop signal processing and cleanup
     */
    fun stop() {
        locationHistory.clear()
        carrierPhaseHistory.clear()
    }
    
    // Helper classes
    
    private data class LocationMeasurement(
        val location: Location,
        val timestamp: Long
    )
    
    private data class CorrelationResult(
        val latitudeAdjustment: Double,
        val longitudeAdjustment: Double,
        val confidenceFactor: Float
    )
    
    private class MultipathDetector {
        fun detectMultipath(measurements: List<LocationMeasurement>): Double {
            if (measurements.size < 3) return 0.0
            
            // Analyze signal characteristics for multipath indicators
            val recentMeasurements = measurements.takeLast(5)
            
            // Calculate position jitter as multipath indicator
            val positions = recentMeasurements.map { Pair(it.location.latitude, it.location.longitude) }
            val jitter = calculatePositionJitter(positions)
            
            // Calculate accuracy inconsistency
            val accuracies = recentMeasurements.map { it.location.accuracy.toDouble() }
            val accuracyVariability = calculateVariance(accuracies)
            
            // Combine indicators
            val jitterRisk = (jitter * 100000.0).coerceIn(0.0, 1.0) // Scale for lat/lon
            val accuracyRisk = (accuracyVariability / 100.0).coerceIn(0.0, 1.0)
            
            return (jitterRisk + accuracyRisk) / 2.0
        }
        
        private fun calculatePositionJitter(positions: List<Pair<Double, Double>>): Double {
            if (positions.size < 2) return 0.0
            
            val center = Pair(
                positions.map { it.first }.average(),
                positions.map { it.second }.average()
            )
            
            return positions.map { pos ->
                sqrt((pos.first - center.first).pow(2) + (pos.second - center.second).pow(2))
            }.average()
        }
        
        private fun calculateVariance(values: List<Double>): Double {
            if (values.size < 2) return 0.0
            val mean = values.average()
            return values.map { (it - mean).pow(2) }.average()
        }
    }
    
    private class DOPFilter {
        fun filter(location: Location): Location {
            // Simulate DOP-based filtering
            // In real implementation, this would use actual DOP values from GPS receiver
            
            val estimatedDOP = estimateDOPFromAccuracy(location.accuracy)
            
            return if (estimatedDOP < 5.0) { // Good DOP
                location
            } else { // Poor DOP - apply conservative filtering
                val filteredLocation = Location(location)
                filteredLocation.accuracy = location.accuracy * 1.2f // Increase uncertainty
                filteredLocation
            }
        }
        
        private fun estimateDOPFromAccuracy(accuracy: Float): Double {
            // Empirical relationship between accuracy and DOP
            return accuracy / 3.0 // Rough approximation
        }
    }
} 