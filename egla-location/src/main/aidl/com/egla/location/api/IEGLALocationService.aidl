package com.egla.location.api;

import com.egla.location.api.ILocationCallback;
import com.egla.location.api.LocationConfiguration;
import com.egla.location.api.EnhancedLocationData;

/**
 * AIDL interface for EGLA Location Service API
 * This defines the contract for communication between client apps and the EGLA service
 */
interface IEGLALocationService {
    
    /**
     * Register a callback for location updates
     * @param callback The callback to receive location updates
     * @return A unique session ID for this client
     */
    String registerLocationCallback(ILocationCallback callback);
    
    /**
     * Unregister a previously registered callback
     * @param sessionId The session ID returned from registerLocationCallback
     */
    void unregisterLocationCallback(String sessionId);
    
    /**
     * Configure the location service
     * @param sessionId The session ID for this client
     * @param config The configuration to apply
     */
    void configure(String sessionId, in LocationConfiguration config);
    
    /**
     * Start location updates for a specific session
     * @param sessionId The session ID for this client
     * @return true if successfully started
     */
    boolean startLocationUpdates(String sessionId);
    
    /**
     * Stop location updates for a specific session
     * @param sessionId The session ID for this client
     */
    void stopLocationUpdates(String sessionId);
    
    /**
     * Get the current service status
     * @return Status code (0=idle, 1=active, 2=error)
     */
    int getServiceStatus();
    
    /**
     * Get the last known enhanced location
     * @param sessionId The session ID for this client
     * @return The last enhanced location or null if not available
     */
    EnhancedLocationData getLastLocation(String sessionId);
    
    /**
     * Request an immediate location update
     * @param sessionId The session ID for this client
     */
    void requestImmediateUpdate(String sessionId);
    
    /**
     * Get performance metrics for the service
     * @return JSON string containing performance metrics
     */
    String getPerformanceMetrics();
} 