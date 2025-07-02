package com.egla.location.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.egla.location.EGLALocationManager
import com.egla.location.R

/**
 * Background service for continuous EGLA location processing
 * Provides foreground service capability for long-running location tracking
 */
class EGLALocationService : Service() {
    
    private lateinit var eglaManager: EGLALocationManager
    private val binder = LocalBinder()
    
    inner class LocalBinder : Binder() {
        fun getService(): EGLALocationService = this@EGLALocationService
    }
    
    override fun onCreate() {
        super.onCreate()
        eglaManager = EGLALocationManager.getInstance(this)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LOCATION_UPDATES -> {
                startForegroundService()
                eglaManager.startLocationUpdates()
            }
            ACTION_STOP_LOCATION_UPDATES -> {
                eglaManager.stopLocationUpdates()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent): IBinder = binder
    
    override fun onDestroy() {
        super.onDestroy()
        eglaManager.stopLocationUpdates()
    }
    
    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "EGLA Location Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Enhanced GPS location tracking"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val stopIntent = Intent(this, EGLALocationService::class.java).apply {
            action = ACTION_STOP_LOCATION_UPDATES
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, flags)
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Enhanced GPS Active")
            .setContentText("EGLA is improving location accuracy")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .build()
    }
    
    companion object {
        private const val CHANNEL_ID = "EGLA_SERVICE_CHANNEL"
        private const val NOTIFICATION_ID = 1001
        
        const val ACTION_START_LOCATION_UPDATES = "START_LOCATION_UPDATES"
        const val ACTION_STOP_LOCATION_UPDATES = "STOP_LOCATION_UPDATES"
        
        fun startService(context: Context) {
            val intent = Intent(context, EGLALocationService::class.java).apply {
                action = ACTION_START_LOCATION_UPDATES
            }
            context.startForegroundService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, EGLALocationService::class.java).apply {
                action = ACTION_STOP_LOCATION_UPDATES
            }
            context.startService(intent)
        }
    }
} 