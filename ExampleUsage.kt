package com.example.egla.usage

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.egla.location.api.LocationConfiguration
import com.egla.location.client.EGLALocationClient
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable

/**
 * Example usage of the EGLA Client API
 * 
 * This demonstrates how to use the new EGLA API architecture where EGLA runs
 * as a separate service and applications communicate through the client library.
 */
class ExampleActivity : Activity() {
    
    private lateinit var eglaClient: EGLALocationClient
    private val disposables = CompositeDisposable()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Step 1: Create EGLA client
        eglaClient = EGLALocationClient(this)
        
        // Step 2: Monitor connection state
        disposables.add(
            eglaClient.connectionState
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { state ->
                    when (state) {
                        EGLALocationClient.ConnectionState.CONNECTED -> {
                            Log.d(TAG, "Connected to EGLA service")
                            onEGLAConnected()
                        }
                        EGLALocationClient.ConnectionState.DISCONNECTED -> {
                            Log.d(TAG, "Disconnected from EGLA service")
                        }
                        EGLALocationClient.ConnectionState.CONNECTING -> {
                            Log.d(TAG, "Connecting to EGLA service...")
                        }
                    }
                }
        )
        
        // Step 3: Handle errors
        disposables.add(
            eglaClient.errors.subscribe { error ->
                Log.e(TAG, "EGLA Error ${error.code}: ${error.message}")
            }
        )
        
        // Step 4: Connect to service
        if (!eglaClient.connect()) {
            Log.e(TAG, "Failed to connect to EGLA service")
        }
    }
    
    private fun onEGLAConnected() {
        // Configure EGLA for high accuracy
        eglaClient.configure(LocationConfiguration.highAccuracyMode())
        
        // Subscribe to location updates
        disposables.add(
            eglaClient.locationUpdates
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { location ->
                    Log.d(TAG, "Enhanced Location:")
                    Log.d(TAG, "  Lat: ${location.latitude}, Lng: ${location.longitude}")
                    Log.d(TAG, "  Accuracy: ${location.accuracy}m (was ${location.originalAccuracy}m)")
                    Log.d(TAG, "  Improvement: ${location.accuracyImprovement}%")
                    Log.d(TAG, "  Environment: ${location.environment}")
                    
                    // Use the enhanced location in your app
                    updateMapLocation(location.latitude, location.longitude)
                }
        )
        
        // Monitor service status
        disposables.add(
            eglaClient.serviceStatus.subscribe { status ->
                Log.d(TAG, "Service status: $status")
            }
        )
        
        // Start location updates
        if (!eglaClient.startLocationUpdates()) {
            Log.e(TAG, "Failed to start location updates")
        }
    }
    
    private fun updateMapLocation(latitude: Double, longitude: Double) {
        // Update your UI with the enhanced location
    }
    
    fun switchToBalancedMode() {
        // Change configuration at runtime
        eglaClient.configure(LocationConfiguration.balancedMode())
    }
    
    fun requestImmediateUpdate() {
        // Force an immediate location update
        eglaClient.requestImmediateUpdate()
    }
    
    fun checkPerformance() {
        // Get performance metrics
        val metricsJson = eglaClient.getPerformanceMetrics()
        Log.d(TAG, "Performance metrics: $metricsJson")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up
        eglaClient.stopLocationUpdates()
        eglaClient.disconnect()
        disposables.clear()
    }
    
    companion object {
        private const val TAG = "ExampleEGLA"
    }
} 