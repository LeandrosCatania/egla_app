package com.egla.location.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import com.egla.location.api.EnhancedLocationData
import com.egla.location.api.IEGLALocationService
import com.egla.location.api.ILocationCallback
import com.egla.location.api.LocationConfiguration
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

/**
 * Client library for connecting to EGLA Location API Service
 * 
 * This provides a clean interface for applications to use EGLA without
 * directly depending on the implementation.
 * 
 * Usage:
 * ```kotlin
 * val client = EGLALocationClient(context)
 * 
 * // Connect to service
 * client.connect()
 * 
 * // Configure
 * client.configure(LocationConfiguration.highAccuracyMode())
 * 
 * // Start receiving updates
 * client.startLocationUpdates()
 * 
 * // Subscribe to location updates (RxJava)
 * client.locationUpdates.subscribe { location ->
 *     // Handle enhanced location
 * }
 * 
 * // Or use Kotlin Flow
 * client.locationFlow.collect { location ->
 *     // Handle enhanced location
 * }
 * ```
 */
class EGLALocationClient(private val context: Context) {
    
    private var service: IEGLALocationService? = null
    private var sessionId: String? = null
    private var isConnected = false
    
    // Observable streams
    private val _connectionState = BehaviorSubject.createDefault(ConnectionState.DISCONNECTED)
    val connectionState: Observable<ConnectionState> = _connectionState
    
    private val _locationUpdates = PublishSubject.create<EnhancedLocationData>()
    val locationUpdates: Observable<EnhancedLocationData> = _locationUpdates
    
    private val _serviceStatus = BehaviorSubject.createDefault(ServiceStatus.IDLE)
    val serviceStatus: Observable<ServiceStatus> = _serviceStatus
    
    private val _errors = PublishSubject.create<LocationError>()
    val errors: Observable<LocationError> = _errors
    
    // Kotlin Flow alternative
    val locationFlow: Flow<EnhancedLocationData> = callbackFlow {
        val disposable = locationUpdates.subscribe { location ->
            trySend(location)
        }
        awaitClose { disposable.dispose() }
    }
    
    /**
     * Flow equivalents for reactive streams – enables Kotlin Coroutines only
     * consumers without pulling in RxJava.
     */
    val connectionStateFlow: Flow<ConnectionState> = callbackFlow {
        val disposable = connectionState.subscribe { trySend(it) }
        awaitClose { disposable.dispose() }
    }
    
    val serviceStatusFlow: Flow<ServiceStatus> = callbackFlow {
        val disposable = serviceStatus.subscribe { trySend(it) }
        awaitClose { disposable.dispose() }
    }
    
    val errorsFlow: Flow<LocationError> = callbackFlow {
        val disposable = errors.subscribe { trySend(it) }
        awaitClose { disposable.dispose() }
    }
    
    // Service connection
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IEGLALocationService.Stub.asInterface(binder)
            isConnected = true
            _connectionState.onNext(ConnectionState.CONNECTED)
            
            // Register callback
            registerCallback()
            
            Timber.d("Connected to EGLA API Service")
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            sessionId = null
            isConnected = false
            _connectionState.onNext(ConnectionState.DISCONNECTED)
            
            Timber.d("Disconnected from EGLA API Service")
        }
    }
    
    // Location callback
    private val locationCallback = object : ILocationCallback.Stub() {
        override fun onLocationUpdate(location: EnhancedLocationData) {
            _locationUpdates.onNext(location)
        }
        
        override fun onStatusChanged(status: Int) {
            _serviceStatus.onNext(
                when (status) {
                    0 -> ServiceStatus.IDLE
                    1 -> ServiceStatus.ACTIVE
                    else -> ServiceStatus.ERROR
                }
            )
        }
        
        override fun onError(errorCode: Int, message: String) {
            _errors.onNext(LocationError(errorCode, message))
        }
        
        override fun onAccuracyMetricsUpdate(accuracyMetricsJson: String) {
            // Can be parsed and exposed if needed
        }
    }
    
    /**
     * Connect to EGLA API Service
     */
    fun connect(): Boolean {
        if (isConnected) return true
        
        val intent = Intent("com.egla.location.api.IEGLALocationService").apply {
            setPackage("com.egla.location")
        }
        
        try {
            val bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            if (!bound) {
                _errors.onNext(LocationError(100, "Failed to bind to EGLA service"))
            }
            return bound
        } catch (e: Exception) {
            _errors.onNext(LocationError(101, "Failed to connect: ${e.message}"))
            return false
        }
    }
    
    /**
     * Disconnect from EGLA API Service
     */
    fun disconnect() {
        try {
            sessionId?.let { id ->
                service?.unregisterLocationCallback(id)
            }
        } catch (e: RemoteException) {
            Timber.e(e, "Failed to unregister callback")
        }
        
        if (isConnected) {
            context.unbindService(connection)
            isConnected = false
            service = null
            sessionId = null
            _connectionState.onNext(ConnectionState.DISCONNECTED)
        }
    }
    
    /**
     * Configure EGLA service
     */
    fun configure(configuration: LocationConfiguration): Boolean {
        return try {
            val session = sessionId ?: return false
            service?.configure(session, configuration)
            true
        } catch (e: RemoteException) {
            _errors.onNext(LocationError(102, "Failed to configure: ${e.message}"))
            false
        }
    }
    
    /**
     * Start receiving location updates
     */
    fun startLocationUpdates(): Boolean {
        return try {
            val session = sessionId ?: return false
            service?.startLocationUpdates(session) ?: false
        } catch (e: RemoteException) {
            _errors.onNext(LocationError(103, "Failed to start updates: ${e.message}"))
            false
        }
    }
    
    /**
     * Stop receiving location updates
     */
    fun stopLocationUpdates() {
        try {
            sessionId?.let { id ->
                service?.stopLocationUpdates(id)
            }
        } catch (e: RemoteException) {
            _errors.onNext(LocationError(104, "Failed to stop updates: ${e.message}"))
        }
    }
    
    /**
     * Get last known location
     */
    fun getLastLocation(): EnhancedLocationData? {
        return try {
            val session = sessionId ?: return null
            service?.getLastLocation(session)
        } catch (e: RemoteException) {
            _errors.onNext(LocationError(105, "Failed to get last location: ${e.message}"))
            null
        }
    }
    
    /**
     * Request immediate location update
     */
    fun requestImmediateUpdate() {
        try {
            sessionId?.let { id ->
                service?.requestImmediateUpdate(id)
            }
        } catch (e: RemoteException) {
            _errors.onNext(LocationError(106, "Failed to request update: ${e.message}"))
        }
    }
    
    /**
     * Get performance metrics as JSON
     */
    fun getPerformanceMetrics(): String? {
        return try {
            service?.getPerformanceMetrics()
        } catch (e: RemoteException) {
            _errors.onNext(LocationError(107, "Failed to get metrics: ${e.message}"))
            null
        }
    }
    
    private fun registerCallback() {
        try {
            sessionId = service?.registerLocationCallback(locationCallback)
            if (sessionId == null) {
                _errors.onNext(LocationError(108, "Failed to register callback"))
            }
        } catch (e: RemoteException) {
            _errors.onNext(LocationError(109, "Failed to register: ${e.message}"))
        }
    }
    
    /**
     * Connection states
     */
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
    
    /**
     * Service status
     */
    enum class ServiceStatus {
        IDLE,
        ACTIVE,
        ERROR
    }
    
    /**
     * Location error
     */
    data class LocationError(
        val code: Int,
        val message: String
    )
} 