package com.egla.location.api;

import com.egla.location.api.EnhancedLocationData;

/**
 * Callback interface for receiving location updates from EGLA service
 */
interface ILocationCallback {
    
    /**
     * Called when a new enhanced location is available
     * @param location The enhanced location data
     */
    void onLocationUpdate(in EnhancedLocationData location);
    
    /**
     * Called when the service status changes
     * @param status The new status (0=idle, 1=active, 2=error)
     */
    void onStatusChanged(int status);
    
    /**
     * Called when an error occurs
     * @param errorCode The error code
     * @param message Human-readable error message
     */
    void onError(int errorCode, String message);
    
    /**
     * Called when accuracy metrics are updated
     * @param accuracyMetricsJson JSON string containing accuracy metrics
     */
    void onAccuracyMetricsUpdate(String accuracyMetricsJson);
} 