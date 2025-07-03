package com.egla.core

import android.location.Location
import org.junit.Assert.assertEquals
import org.junit.Test

class DirectionCalculatorTest {

    private fun location(lat: Double, lon: Double): Location = Location("test").apply {
        latitude = lat
        longitude = lon
    }

    @Test
    fun distance_zeroWhenSamePoint() {
        val a = location(0.0, 0.0)
        val b = location(0.0, 0.0)
        val d = DirectionCalculator.calculateDistance(a, b)
        assertEquals(0f, d, 0.0001f)
    }

    @Test
    fun distance_knownReference() {
        // Approx distance between (0,0) and (0,1) ~ 111.319 km
        val a = location(0.0, 0.0)
        val b = location(0.0, 1.0)
        val d = DirectionCalculator.calculateDistance(a, b)
        assertEquals(111_319f, d, 500f) // ±500 m tolerance
    }

    @Test
    fun bearing_basicQuadrant() {
        val a = location(0.0, 0.0)
        val b = location(1.0, 1.0)
        val bearing = DirectionCalculator.calculateBearing(a, b)
        // Expected bearing ~45° (NE)
        assertEquals(45f, bearing, 5f)
    }

    @Test
    fun speed_conversion() {
        val speedms = 10f // 10 m/s
        val kmh = DirectionCalculator.msToKmh(speedms)
        assertEquals(36f, kmh, 0.001f)
    }
}