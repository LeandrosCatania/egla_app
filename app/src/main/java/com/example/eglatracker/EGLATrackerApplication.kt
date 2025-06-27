package com.example.eglatracker

import android.app.Application
import timber.log.Timber

class EGLATrackerApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize logging - always plant debug tree for now
        Timber.plant(Timber.DebugTree())
        
        Timber.d("EGLA Tracker Application initialized")
    }
} 