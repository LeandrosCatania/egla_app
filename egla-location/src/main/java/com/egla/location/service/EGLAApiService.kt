package com.egla.location.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import com.egla.location.EGLAConfiguration
import com.egla.location.EGLALocationManager
import com.egla.location.api.EnhancedLocationData
import com.egla.location.api.IEGLALocationService
import com.egla.location.api.ILocationCallback
import com.egla.location.api.LocationConfiguration
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

/**
 * EGLA API Service that provides location enhancement services to client applications
 * This service runs in its own process and communicates via AIDL
 */
class EGLAApiService : Service() {
    
    private lateinit var eglaManager: EGLALocationManager
    private val gson = Gson()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Track current system status
    private var currentSystemStatus = EGLALocationManager.SystemStatus.INACTIVE
    
    // Track client sessions
    private val clientSessions = ConcurrentHashMap<String, ClientSession>()
    
    // Remote callbacks for all clients
    private val callbacks = RemoteCallbackList<ILocationCallback>()
    
    // Service implementation
    private val binder = object : IEGLALocationService.Stub() {
        
        override fun registerLocationCallback(callback: ILocationCallback): String {
            val sessionId = UUID.randomUUID().toString()
            
            callbacks.register(callback)
            
            val session = ClientSession(
                sessionId = sessionId,
                callback = callback,
                configuration = LocationConfiguration.balancedMode()
            )
            
            clientSessions[sessionId] = session
            
            Timber.d("Registered new client session: $sessionId")
            return sessionId
        }
        
        override fun unregisterLocationCallback(sessionId: String) {
            clientSessions.remove(sessionId)?.let { session ->
                callbacks.unregister(session.callback)
                session.dispose()
                Timber.d("Unregistered client session: $sessionId")
            }
        }
        
        override fun configure(sessionId: String, config: LocationConfiguration) {
            clientSessions[sessionId]?.let { session ->
                session.configuration = config
                
                // Apply configuration to EGLA if this is the active session
                if (session.isActive) {
                    applyConfiguration(config)
                }
                
                Timber.d("Configured session $sessionId with mode: ${config.mode}")
            }
        }
        
        override fun startLocationUpdates(sessionId: String): Boolean {
            return clientSessions[sessionId]?.let { session ->
                try {
                    // Apply session configuration
                    applyConfiguration(session.configuration)
                    
                    // Subscribe to EGLA updates for this session
                    session.locationJob = serviceScope.launch {
                        eglaManager.locationUpdates.collect { enhanced ->
                            broadcastLocationUpdate(session, enhanced)
                        }
                    }
                    session.statusJob = serviceScope.launch {
                        eglaManager.systemStatus.collect { status ->
                            broadcastStatusChange(session, mapStatus(status))
                        }
                    }
                    
                    session.isActive = true
                    
                    // Start EGLA updates
                    eglaManager.startLocationUpdates()
                    
                    Timber.d("Started location updates for session: $sessionId")
                    true
                } catch (e: Exception) {
                    Timber.e(e, "Failed to start location updates")
                    broadcastError(session, 2, "Failed to start: ${e.message}")
                    false
                }
            } ?: false
        }
        
        override fun stopLocationUpdates(sessionId: String) {
            clientSessions[sessionId]?.let { session ->
                session.isActive = false
                session.locationJob?.cancel()
                session.statusJob?.cancel()
                
                // Stop EGLA if no active sessions
                if (clientSessions.values.none { it.isActive }) {
                    eglaManager.stopLocationUpdates()
                }
                
                Timber.d("Stopped location updates for session: $sessionId")
            }
        }
        
        override fun getServiceStatus(): Int {
            // Since systemStatus is an Observable, we need to track the current status
            return when (currentSystemStatus) {
                EGLALocationManager.SystemStatus.INACTIVE -> 0
                EGLALocationManager.SystemStatus.ACTIVE -> 1
                else -> 2
            }
        }
        
        override fun getLastLocation(sessionId: String): EnhancedLocationData? {
            return clientSessions[sessionId]?.lastLocation
        }
        
        override fun requestImmediateUpdate(sessionId: String) {
            if (clientSessions.containsKey(sessionId)) {
                eglaManager.requestImmediateUpdate()
            }
        }
        
        override fun getPerformanceMetrics(): String {
            val metrics = eglaManager.getPerformanceMetrics()
            return gson.toJson(metrics)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        Timber.d("EGLA API Service created")
        
        eglaManager = EGLALocationManager.getInstance(this)
        
        // Subscribe to system status updates to keep track of current status
        serviceScope.launch {
            eglaManager.systemStatus.collect { status ->
                currentSystemStatus = status
                Timber.d("EGLA system status changed to: $status")
            }
        }
    }
    
    override fun onBind(intent: Intent): IBinder {
        Timber.d("Client binding to EGLA API Service")
        return binder
    }
    
    override fun onUnbind(intent: Intent): Boolean {
        Timber.d("All clients unbound from EGLA API Service")
        return super.onUnbind(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up all sessions
        clientSessions.values.forEach { it.dispose() }
        clientSessions.clear()
        
        // Clean up service subscriptions
        serviceScope.cancel()
        
        callbacks.kill()
        
        eglaManager.release()
        
        Timber.d("EGLA API Service destroyed")
    }
    
    private fun applyConfiguration(config: LocationConfiguration) {
        val eglaConfig = when (config.mode) {
            "POWER_SAVE" -> EGLAConfiguration.powerSaveMode()
            "BALANCED" -> EGLAConfiguration.balancedMode()
            "HIGH_ACCURACY" -> EGLAConfiguration.highAccuracyMode()
            "ULTRA_HIGH_ACCURACY" -> EGLAConfiguration.ultraHighAccuracyMode()
            else -> EGLAConfiguration.balancedMode()
        }.let { baseConfig ->
            // Apply custom settings
            baseConfig.copy(
                performanceConfig = baseConfig.performanceConfig.copy(
                    updateIntervalMs = config.updateIntervalMs,
                    batteryOptimization = config.batteryOptimization
                ),
                kalmanConfig = baseConfig.kalmanConfig.copy(
                    enabled = config.enableKalmanFilter
                ),
                sensorFusionConfig = baseConfig.sensorFusionConfig.copy(
                    enabled = config.enableSensorFusion
                ),
                mlConfig = baseConfig.mlConfig.copy(
                    enabled = config.enableMachineLearning
                ),
                swarmConfig = baseConfig.swarmConfig.copy(
                    enabled = config.enableSwarmOptimization
                )
            )
        }
        
        eglaManager.configure(eglaConfig)
    }
    
    private fun broadcastLocationUpdate(
        session: ClientSession,
        enhancedLocation: EGLALocationManager.EnhancedLocation
    ) {
        val data = EnhancedLocationData(
            timestamp = System.currentTimeMillis(),
            latitude = enhancedLocation.enhancedLocation.latitude,
            longitude = enhancedLocation.enhancedLocation.longitude,
            altitude = if (enhancedLocation.enhancedLocation.hasAltitude()) 
                enhancedLocation.enhancedLocation.altitude else null,
            accuracy = enhancedLocation.enhancedLocation.accuracy,
            bearing = if (enhancedLocation.enhancedLocation.hasBearing()) 
                enhancedLocation.enhancedLocation.bearing else null,
            speed = if (enhancedLocation.enhancedLocation.hasSpeed()) 
                enhancedLocation.enhancedLocation.speed else null,
            originalLatitude = enhancedLocation.originalLocation.latitude,
            originalLongitude = enhancedLocation.originalLocation.longitude,
            originalAccuracy = enhancedLocation.originalLocation.accuracy,
            accuracyImprovement = enhancedLocation.accuracyImprovement,
            confidence = enhancedLocation.confidence,
            environment = enhancedLocation.environment?.type?.name,
            processingTimeMs = enhancedLocation.processingTime
        )
        
        session.lastLocation = data
        
        try {
            session.callback.onLocationUpdate(data)
        } catch (e: RemoteException) {
            Timber.e(e, "Failed to send location update to client")
        }
    }
    
    private fun broadcastStatusChange(session: ClientSession, status: Int) {
        try {
            session.callback.onStatusChanged(status)
        } catch (e: RemoteException) {
            Timber.e(e, "Failed to send status change to client")
        }
    }
    
    private fun broadcastError(session: ClientSession, errorCode: Int, message: String) {
        try {
            session.callback.onError(errorCode, message)
        } catch (e: RemoteException) {
            Timber.e(e, "Failed to send error to client")
        }
    }
    
    private fun mapStatus(status: EGLALocationManager.SystemStatus): Int {
        currentSystemStatus = status // Update our tracked status
        return when (status) {
            EGLALocationManager.SystemStatus.INACTIVE -> 0
            EGLALocationManager.SystemStatus.ACTIVE -> 1
            else -> 2
        }
    }
    
    /**
     * Client session tracking
     */
    private data class ClientSession(
        val sessionId: String,
        val callback: ILocationCallback,
        var configuration: LocationConfiguration,
        var isActive: Boolean = false,
        var lastLocation: EnhancedLocationData? = null,
        var locationJob: Job? = null,
        var statusJob: Job? = null
    ) {
        fun dispose() {
            locationJob?.cancel()
            statusJob?.cancel()
        }
    }
} 