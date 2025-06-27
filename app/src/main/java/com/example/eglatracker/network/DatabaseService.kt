package com.example.eglatracker.network

import android.content.Context
import android.provider.Settings
import com.example.eglatracker.data.LocationRecord
import com.example.eglatracker.data.LogLevel
import com.example.eglatracker.data.LogTag
import com.example.eglatracker.utils.DatabaseConfig
import com.example.eglatracker.utils.LoggingManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Unified database service for all HTTP operations
 * Handles configuration, requests, retries, and error management
 */
class DatabaseService private constructor(private val context: Context) {
    
    private val config = DatabaseConfig.getInstance(context)
    private val loggingManager = LoggingManager.getInstance()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    
    val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
    }
    
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(createLoggingInterceptor())
            .addInterceptor(createErrorLoggingInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    
    companion object {
        @Volatile
        private var INSTANCE: DatabaseService? = null
        
        fun getInstance(context: Context): DatabaseService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseService(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
    }
    
    /**
     * Test server connectivity
     */
    suspend fun testConnection(): DatabaseResult<HealthResponse> = withContext(Dispatchers.IO) {
        loggingManager.info(LogTag.NETWORK, "Testing database server connection", "URL: ${config.getFullServerUrl()}")
        
        executeWithRetry {
            val request = Request.Builder()
                .url("${config.getFullServerUrl()}/health")
                .get()
                .build()
            
            val response = httpClient.newCall(request).execute()
            response.use {
                if (it.isSuccessful) {
                    val healthResponse = gson.fromJson(it.body?.string(), HealthResponse::class.java)
                    loggingManager.success(LogTag.NETWORK, "Connection test successful", "Server status: ${healthResponse.status}")
                    DatabaseResult.Success(healthResponse)
                } else {
                    val error = "HTTP ${it.code}: ${it.message}"
                    loggingManager.error(LogTag.NETWORK, "Connection test failed", error)
                    DatabaseResult.Error(error, it.code)
                }
            }
        }
    }
    
    /**
     * Send location record to database
     */
    suspend fun sendLocationRecord(record: LocationRecord, sessionId: String): DatabaseResult<LocationResponse> = withContext(Dispatchers.IO) {
        val locationData = LocationDataRequest(
            timestamp = record.timestamp,
            latitude = record.latitude,
            longitude = record.longitude,
            originalLatitude = record.originalLatitude,
            originalLongitude = record.originalLongitude,
            accuracy = record.accuracy.toDouble(),
            originalAccuracy = record.originalAccuracy.toDouble(),
            altitude = record.altitude,
            bearing = record.bearing.toDouble(),
            speed = record.speed.toDouble(),
            direction = record.direction,
            isStationary = record.isStationary,
            environment = record.environment,
            operatingMode = record.operatingMode,
            accuracyImprovement = record.accuracyImprovement.toDouble(),
            confidence = record.confidence.toDouble(),
            processingTime = record.processingTime.toDouble(),
            deviceId = deviceId,
            sessionId = sessionId
        )
        
        executeWithRetry {
            val json = gson.toJson(locationData)
            val requestBody = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("${config.getFullServerUrl()}/api/location")
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(request).execute()
            response.use {
                if (it.isSuccessful) {
                    val locationResponse = gson.fromJson(it.body?.string(), LocationResponse::class.java)
                    DatabaseResult.Success(locationResponse)
                } else {
                    val error = "Failed to send location: HTTP ${it.code}"
                    val errorBody = it.body?.string()
                    loggingManager.warning(LogTag.DATABASE, error, errorBody)
                    DatabaseResult.Error(error, it.code)
                }
            }
        }
    }
    
    /**
     * Get recent location records
     */
    suspend fun getRecentRecords(limit: Int = 100): DatabaseResult<List<LocationRecord>> = withContext(Dispatchers.IO) {
        executeWithRetry {
            val request = Request.Builder()
                .url("${config.getFullServerUrl()}/api/location/recent?limit=$limit")
                .get()
                .build()
            
            val response = httpClient.newCall(request).execute()
            response.use {
                if (it.isSuccessful) {
                    val responseBody = it.body?.string()
                    // TODO: Parse actual location records
                    DatabaseResult.Success(emptyList<LocationRecord>())
                } else {
                    val error = "Failed to get recent records: HTTP ${it.code}"
                    DatabaseResult.Error(error, it.code)
                }
            }
        }
    }
    
    /**
     * Get device statistics
     */
    suspend fun getDeviceStats(): DatabaseResult<DeviceStats> = withContext(Dispatchers.IO) {
        executeWithRetry {
            val request = Request.Builder()
                .url("${config.getFullServerUrl()}/api/stats/device/$deviceId")
                .get()
                .build()
            
            val response = httpClient.newCall(request).execute()
            response.use {
                if (it.isSuccessful) {
                    val stats = gson.fromJson(it.body?.string(), DeviceStats::class.java)
                    DatabaseResult.Success(stats)
                } else {
                    val error = "Failed to get device stats: HTTP ${it.code}"
                    DatabaseResult.Error(error, it.code)
                }
            }
        }
    }
    
    /**
     * Update server URL
     */
    fun updateServerUrl(newUrl: String) {
        val formattedUrl = if (newUrl.contains(":")) newUrl else "$newUrl:3000"
        config.setServerUrl(formattedUrl)
        loggingManager.info(LogTag.DATABASE, "Server URL updated", "New URL: ${config.getFullServerUrl()}")
    }
    
    /**
     * Get current server URL
     */
    fun getCurrentServerUrl(): String = config.getFullServerUrl()
    
    /**
     * Execute request with retry logic
     */
    private suspend fun <T> executeWithRetry(
        maxRetries: Int = MAX_RETRIES,
        operation: suspend () -> DatabaseResult<T>
    ): DatabaseResult<T> {
        repeat(maxRetries) { attempt ->
            try {
                val result = operation()
                if (result is DatabaseResult.Success) {
                    return result
                }
                
                // If it's an error and not the last attempt, log and retry
                if (attempt < maxRetries - 1) {
                    loggingManager.warning(LogTag.NETWORK, "Request failed, retrying", "Attempt ${attempt + 1}/$maxRetries")
                    delay(RETRY_DELAY_MS * (attempt + 1)) // Exponential backoff
                } else {
                    return result
                }
            } catch (e: Exception) {
                if (attempt < maxRetries - 1) {
                    loggingManager.warning(LogTag.NETWORK, "Request exception, retrying", "Error: ${e.message}")
                    delay(RETRY_DELAY_MS * (attempt + 1))
                } else {
                    loggingManager.error(LogTag.NETWORK, "Request failed after all retries", e.message)
                    return DatabaseResult.Error("Network error: ${e.message}")
                }
            }
        }
        
        return DatabaseResult.Error("Max retries exceeded")
    }
    
    /**
     * Create logging interceptor for HTTP requests
     */
    private fun createLoggingInterceptor(): Interceptor {
        return HttpLoggingInterceptor { message ->
            Timber.d("HTTP: $message")
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }
    
    /**
     * Create error logging interceptor for detailed error tracking
     */
    private fun createErrorLoggingInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val startTime = System.currentTimeMillis()
            
            try {
                val response = chain.proceed(request)
                val duration = System.currentTimeMillis() - startTime
                
                if (!response.isSuccessful) {
                    loggingManager.warning(
                        LogTag.NETWORK, 
                        "HTTP request failed",
                        "URL: ${request.url}, Code: ${response.code}, Duration: ${duration}ms"
                    )
                } else {
                    if (duration > 5000) { // Log slow requests
                        loggingManager.warning(
                            LogTag.PERFORMANCE,
                            "Slow HTTP request",
                            "URL: ${request.url}, Duration: ${duration}ms"
                        )
                    }
                }
                
                response
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                loggingManager.error(
                    LogTag.NETWORK,
                    "HTTP request exception",
                    "URL: ${request.url}, Error: ${e.message}, Duration: ${duration}ms"
                )
                throw e
            }
        }
    }
}

/**
 * Sealed class for database operation results
 */
sealed class DatabaseResult<out T> {
    data class Success<T>(val data: T) : DatabaseResult<T>()
    data class Error(val message: String, val code: Int? = null) : DatabaseResult<Nothing>()
    
    inline fun onSuccess(action: (T) -> Unit): DatabaseResult<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onError(action: (String, Int?) -> Unit): DatabaseResult<T> {
        if (this is Error) action(message, code)
        return this
    }
}

/**
 * Data classes for API responses
 */
data class HealthResponse(
    val status: String,
    val timestamp: String,
    val message: String? = null
)

data class LocationResponse(
    val success: Boolean,
    val recordId: String? = null,
    val message: String? = null
)

data class DeviceStats(
    val totalRecords: Int,
    val currentSession: String?,
    val isConnected: Boolean,
    val deviceId: String,
    val serverUrl: String
)

/**
 * Data class for location API requests
 */
private data class LocationDataRequest(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val originalLatitude: Double,
    val originalLongitude: Double,
    val accuracy: Double,
    val originalAccuracy: Double,
    val altitude: Double,
    val bearing: Double,
    val speed: Double,
    val direction: String,
    val isStationary: Boolean,
    val environment: String,
    val operatingMode: String,
    val accuracyImprovement: Double,
    val confidence: Double,
    val processingTime: Double,
    val deviceId: String,
    val sessionId: String
) 