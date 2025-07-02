# EGLA API Architecture

## Overview

The Enhanced GNSS Location Accuracy Library (EGLA) has been restructured as a standalone API service that runs on the device. This architecture provides better separation of concerns, process isolation, and allows multiple applications to share the same location enhancement service.

## Architecture Components

### 1. EGLA API Service (`EGLAApiService`)
- Runs in a separate process (`:egla_api`)
- Manages the core EGLA location enhancement algorithms
- Provides AIDL-based IPC interface for client communication
- Handles multiple client sessions simultaneously
- Automatically manages resource lifecycle

### 2. Client Library (`EGLALocationClient`)
- Simple interface for applications to connect to EGLA
- Handles service binding and communication
- Provides both RxJava and Kotlin Flow APIs
- Automatic error handling and recovery

### 3. AIDL Interfaces
- `IEGLALocationService`: Main service interface
- `ILocationCallback`: Callback for location updates
- `EnhancedLocationData`: Parcelable location data
- `LocationConfiguration`: Configuration options

## Benefits of API Architecture

1. **Process Isolation**: EGLA runs in its own process, preventing crashes from affecting the main app
2. **Resource Sharing**: Multiple apps can use the same EGLA service instance
3. **Clean Separation**: UI applications don't need to include EGLA implementation code
4. **Memory Efficiency**: Single instance serves multiple clients
5. **Easy Updates**: EGLA can be updated independently of client applications

## Integration Guide

### Step 1: Add EGLA Client Dependency

In your app's `build.gradle`:

```gradle
dependencies {
    implementation project(':egla-location')
    
    // Required dependencies
    implementation 'io.reactivex.rxjava3:rxjava:3.1.6'
    implementation 'io.reactivex.rxjava3:rxandroid:3.0.2'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1'
}
```

### Step 2: Initialize Client

```kotlin
class MyLocationActivity : AppCompatActivity() {
    private lateinit var eglaClient: EGLALocationClient
    private val disposables = CompositeDisposable()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create EGLA client
        eglaClient = EGLALocationClient(this)
        
        // Connect to EGLA service
        if (!eglaClient.connect()) {
            Log.e(TAG, "Failed to connect to EGLA service")
        }
        
        // Monitor connection state
        disposables.add(
            eglaClient.connectionState.subscribe { state ->
                when (state) {
                    ConnectionState.CONNECTED -> onEGLAConnected()
                    ConnectionState.DISCONNECTED -> onEGLADisconnected()
                }
            }
        )
    }
}
```

### Step 3: Configure and Start Location Updates

```kotlin
private fun onEGLAConnected() {
    // Configure EGLA
    eglaClient.configure(LocationConfiguration.highAccuracyMode())
    
    // Subscribe to location updates (RxJava)
    disposables.add(
        eglaClient.locationUpdates
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { location ->
                updateUI(location)
            }
    )
    
    // Or use Kotlin Flow
    lifecycleScope.launch {
        eglaClient.locationFlow.collect { location ->
            updateUI(location)
        }
    }
    
    // Start receiving updates
    eglaClient.startLocationUpdates()
}
```

### Step 4: Handle Location Updates

```kotlin
private fun updateUI(location: EnhancedLocationData) {
    // Update your UI with enhanced location
    latitudeText.text = "Lat: ${location.latitude}"
    longitudeText.text = "Lng: ${location.longitude}"
    accuracyText.text = "Accuracy: ${location.accuracy}m"
    improvementText.text = "Improvement: ${location.accuracyImprovement}%"
    
    // Check environment
    environmentText.text = "Environment: ${location.environment}"
}
```

### Step 5: Clean Up

```kotlin
override fun onDestroy() {
    super.onDestroy()
    
    // Stop updates and disconnect
    eglaClient.stopLocationUpdates()
    eglaClient.disconnect()
    
    // Dispose subscriptions
    disposables.clear()
}
```

## Configuration Options

### Pre-defined Modes

```kotlin
// Power Save - Minimal processing
LocationConfiguration.powerSaveMode()

// Balanced - Good accuracy with reasonable power
LocationConfiguration.balancedMode()

// High Accuracy - Maximum accuracy
LocationConfiguration.highAccuracyMode()

// Ultra High - All features enabled
LocationConfiguration.ultraHighAccuracyMode()
```

### Custom Configuration

```kotlin
val customConfig = LocationConfiguration(
    mode = "CUSTOM",
    updateIntervalMs = 2000L,
    enableKalmanFilter = true,
    enableSensorFusion = true,
    enableMachineLearning = false,
    enableSwarmOptimization = false,
    batteryOptimization = true
)

eglaClient.configure(customConfig)
```

## Error Handling

```kotlin
// Subscribe to errors
disposables.add(
    eglaClient.errors.subscribe { error ->
        Log.e(TAG, "EGLA Error ${error.code}: ${error.message}")
        
        when (error.code) {
            100 -> handleServiceNotFound()
            101 -> handleConnectionFailed()
            103 -> handleStartFailed()
            // ... handle other errors
        }
    }
)
```

## Service Status Monitoring

```kotlin
// Monitor service status
disposables.add(
    eglaClient.serviceStatus.subscribe { status ->
        when (status) {
            ServiceStatus.IDLE -> statusText.text = "Ready"
            ServiceStatus.ACTIVE -> statusText.text = "Tracking"
            ServiceStatus.ERROR -> statusText.text = "Error"
        }
    }
)
```

## Performance Metrics

```kotlin
// Get performance metrics
val metricsJson = eglaClient.getPerformanceMetrics()
val metrics = Gson().fromJson(metricsJson, PerformanceMetrics::class.java)

Log.d(TAG, "Average accuracy: ${metrics.averageAccuracy}m")
Log.d(TAG, "Average improvement: ${metrics.averageImprovement}%")
```

## Migration from Direct EGLA Usage

If you were previously using EGLA directly:

### Before (Direct Usage):
```kotlin
// Old way - direct instantiation
val eglaManager = EGLALocationManager.getInstance(context)
eglaManager.configure(EGLAConfiguration.highAccuracyMode())
eglaManager.startLocationUpdates()

eglaManager.locationUpdates.subscribe { enhancedLocation ->
    // Handle location
}
```

### After (API Client):
```kotlin
// New way - API client
val eglaClient = EGLALocationClient(context)
eglaClient.connect()
eglaClient.configure(LocationConfiguration.highAccuracyMode())
eglaClient.startLocationUpdates()

eglaClient.locationUpdates.subscribe { location ->
    // Handle location
}
```

## Troubleshooting

### Service Not Found
- Ensure EGLA module is included in your project
- Check that the service is declared in AndroidManifest.xml
- Verify location permissions are granted

### Connection Failed
- Check if the device supports AIDL services
- Ensure the service package name is correct
- Try reconnecting after a delay

### No Location Updates
- Verify location permissions (including background)
- Check GPS is enabled on device
- Ensure startLocationUpdates() was called after connection

## Best Practices

1. **Lifecycle Management**: Always disconnect in onDestroy()
2. **Error Handling**: Subscribe to error stream for robust apps
3. **Configuration**: Choose appropriate mode for your use case
4. **Battery**: Use power save mode when high accuracy isn't critical
5. **Permissions**: Request permissions before connecting to service

## Security Considerations

The EGLA API service requires `ACCESS_FINE_LOCATION` permission to bind. This ensures only apps with location permission can use the service.

## Future Enhancements

- REST API for remote EGLA service
- WebSocket support for real-time updates
- Multi-device coordination for improved accuracy
- Cloud-based enhancement algorithms 