package com.egla.location.core

/**
 * Environment classification result from ML analysis
 */
data class Environment(
    val type: Type,
    val confidence: Float,
    val characteristics: Map<String, Float> = emptyMap()
) {
    enum class Type {
        OPEN_SKY,    // Clear view of satellites
        URBAN,       // Urban canyon with buildings
        INDOOR,      // Indoor environment
        UNKNOWN      // Unable to classify
    }
} 