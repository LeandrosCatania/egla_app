package com.example.eglatracker.viewmodel

import com.example.eglatracker.data.LocationRecord

/**
 * UI State for the location tracking screen with comprehensive logging.
 *
 * This standalone data class was extracted from the deprecated `LocationTrackingViewModel` so that
 * both the legacy and the new `LocationTrackingViewModelV2` can depend on a single source of truth
 * without keeping the entire deprecated ViewModel in the codebase.
 */
data class LocationTrackingUiState(
    // Current location data
    val currentRecord: LocationRecord? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val originalAccuracy: Float = 0f,
    val accuracyImprovement: Float = 0f,

    // Movement data
    val direction: String = "No Data",
    val bearing: Float = 0f,
    val speed: Float = 0f,
    val isStationary: Boolean = true,

    // Environment and confidence
    val environment: String = "UNKNOWN",
    val confidence: Float = 0f,
    val processingTime: Long = 0L,

    // Metrics
    val averageAccuracy: Float = 0f,
    val averageImprovement: Float = 0f,
    val totalFixes: Int = 0,
    val successRate: Float = 0f,

    // History for sparkline visualisation (last 50 improvements)
    val accuracyHistory: List<Float> = emptyList(),

    // System state
    val systemStatus: String = "INITIALIZING",
    val eglaMode: String = "High Accuracy",
    val lastUpdateTime: Long = 0L,

    // Logging
    val currentLogFile: String? = null,
    val systemLogs: List<String> = emptyList(),

    // Error handling
    val errorMessage: String? = null
)