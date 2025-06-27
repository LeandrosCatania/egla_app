package com.example.eglatracker.utils

import android.location.Location
import kotlin.math.*

/**
 * Utility class for calculating direction of movement from GPS coordinates
 */
object DirectionCalculator {
    
    /**
     * Calculate bearing between two locations in degrees (0-360)
     */
    fun calculateBearing(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Float {
        val lat1Rad = Math.toRadians(fromLat)
        val lat2Rad = Math.toRadians(toLat)
        val deltaLonRad = Math.toRadians(toLon - fromLon)
        
        val y = sin(deltaLonRad) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLonRad)
        
        val bearingRad = atan2(y, x)
        val bearingDeg = Math.toDegrees(bearingRad)
        
        return ((bearingDeg + 360) % 360).toFloat()
    }
    
    /**
     * Calculate bearing between two Location objects
     */
    fun calculateBearing(from: Location, to: Location): Float {
        return calculateBearing(from.latitude, from.longitude, to.latitude, to.longitude)
    }
    
    /**
     * Convert bearing to compass direction string
     */
    fun bearingToDirection(bearing: Float): String {
        return when ((bearing + 11.25) % 360) {
            in 0.0..22.5 -> "N"
            in 22.5..67.5 -> "NE"
            in 67.5..112.5 -> "E"
            in 112.5..157.5 -> "SE"
            in 157.5..202.5 -> "S"
            in 202.5..247.5 -> "SW"
            in 247.5..292.5 -> "W"
            in 292.5..337.5 -> "NW"
            else -> "N"
        }
    }
    
    /**
     * Get detailed direction description
     */
    fun getDetailedDirection(bearing: Float): String {
        return when ((bearing + 5.625) % 360) {
            in 0.0..11.25 -> "North"
            in 11.25..33.75 -> "North-Northeast"
            in 33.75..56.25 -> "Northeast"
            in 56.25..78.75 -> "East-Northeast"
            in 78.75..101.25 -> "East"
            in 101.25..123.75 -> "East-Southeast"
            in 123.75..146.25 -> "Southeast"
            in 146.25..168.75 -> "South-Southeast"
            in 168.75..191.25 -> "South"
            in 191.25..213.75 -> "South-Southwest"
            in 213.75..236.25 -> "Southwest"
            in 236.25..258.75 -> "West-Southwest"
            in 258.75..281.25 -> "West"
            in 281.25..303.75 -> "West-Northwest"
            in 303.75..326.25 -> "Northwest"
            in 326.25..348.75 -> "North-Northwest"
            else -> "North"
        }
    }
    
    /**
     * Calculate distance between two locations in meters
     */
    fun calculateDistance(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(fromLat, fromLon, toLat, toLon, results)
        return results[0]
    }
    
    /**
     * Calculate distance between two Location objects
     */
    fun calculateDistance(from: Location, to: Location): Float {
        return from.distanceTo(to)
    }
    
    /**
     * Determine if movement is significant enough to calculate direction
     * Returns true if distance moved is above threshold
     */
    fun isSignificantMovement(distance: Float, threshold: Float = 2.0f): Boolean {
        return distance >= threshold
    }
    
    /**
     * Calculate speed from distance and time
     */
    fun calculateSpeed(distance: Float, timeMs: Long): Float {
        if (timeMs <= 0) return 0f
        return distance / (timeMs / 1000f) // meters per second
    }
    
    /**
     * Convert speed from m/s to km/h
     */
    fun msToKmh(speedMs: Float): Float {
        return speedMs * 3.6f
    }
    
    /**
     * Convert speed from m/s to mph
     */
    fun msToMph(speedMs: Float): Float {
        return speedMs * 2.237f
    }
    
    /**
     * Get movement status based on speed
     */
    fun getMovementStatus(speed: Float): String {
        return when {
            speed < 0.5f -> "Stationary"
            speed < 1.5f -> "Walking"
            speed < 5.0f -> "Jogging"
            speed < 15.0f -> "Running"
            speed < 25.0f -> "Cycling"
            speed < 50.0f -> "Driving"
            else -> "High Speed"
        }
    }
    
    /**
     * Get direction arrow character for UI display
     */
    fun getDirectionArrow(bearing: Float): String {
        return when ((bearing + 22.5) % 360) {
            in 0.0..45.0 -> "↑"
            in 45.0..135.0 -> "→"
            in 135.0..225.0 -> "↓"
            in 225.0..315.0 -> "←"
            else -> "↑"
        }
    }
} 