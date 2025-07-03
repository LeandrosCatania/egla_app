package com.egla.core

import android.location.Location
import kotlin.math.*

/**
 * Collection of utility functions for distance, bearing, and direction calculations.
 */
object DirectionCalculator {

    private const val EARTH_RADIUS = 6371000.0 // metres

    fun calculateDistance(start: Location, end: Location): Float {
        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLon = Math.toRadians(end.longitude - start.longitude)
        val lat1 = Math.toRadians(start.latitude)
        val lat2 = Math.toRadians(end.latitude)

        val a = sin(dLat / 2).pow(2.0) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (EARTH_RADIUS * c).toFloat() // metres
    }

    fun calculateBearing(start: Location, end: Location): Float {
        val lat1 = Math.toRadians(start.latitude)
        val lat2 = Math.toRadians(end.latitude)
        val dLon = Math.toRadians(end.longitude - start.longitude)

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
    }

    fun calculateSpeed(distanceMeters: Float, timeMillis: Long): Float {
        if (timeMillis == 0L) return 0f
        return distanceMeters / (timeMillis / 1000f) // m/s
    }

    fun msToKmh(ms: Float): Float = ms * 3.6f

    fun getDetailedDirection(bearing: Float): String {
        val directions = listOf(
            "N", "NNE", "NE", "ENE",
            "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW",
            "W", "WNW", "NW", "NNW"
        )
        val idx = ((bearing + 11.25) % 360 / 22.5).toInt()
        return directions[idx]
    }

    fun isSignificantMovement(distanceMeters: Float, threshold: Float = 1.0f): Boolean =
        distanceMeters > threshold
}