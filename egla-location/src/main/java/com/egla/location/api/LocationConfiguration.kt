package com.egla.location.api

import android.os.Parcel
import android.os.Parcelable

/**
 * Parcelable configuration class for EGLA service settings
 */
data class LocationConfiguration(
    val mode: String, // "POWER_SAVE", "BALANCED", "HIGH_ACCURACY", "ULTRA_HIGH_ACCURACY"
    val updateIntervalMs: Long = 1000L,
    val enableKalmanFilter: Boolean = true,
    val enableSensorFusion: Boolean = true,
    val enableMachineLearning: Boolean = true,
    val enableSwarmOptimization: Boolean = false,
    val batteryOptimization: Boolean = false
) : Parcelable {
    
    constructor(parcel: Parcel) : this(
        mode = parcel.readString() ?: "BALANCED",
        updateIntervalMs = parcel.readLong(),
        enableKalmanFilter = parcel.readByte() != 0.toByte(),
        enableSensorFusion = parcel.readByte() != 0.toByte(),
        enableMachineLearning = parcel.readByte() != 0.toByte(),
        enableSwarmOptimization = parcel.readByte() != 0.toByte(),
        batteryOptimization = parcel.readByte() != 0.toByte()
    )
    
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(mode)
        parcel.writeLong(updateIntervalMs)
        parcel.writeByte(if (enableKalmanFilter) 1 else 0)
        parcel.writeByte(if (enableSensorFusion) 1 else 0)
        parcel.writeByte(if (enableMachineLearning) 1 else 0)
        parcel.writeByte(if (enableSwarmOptimization) 1 else 0)
        parcel.writeByte(if (batteryOptimization) 1 else 0)
    }
    
    override fun describeContents(): Int = 0
    
    companion object CREATOR : Parcelable.Creator<LocationConfiguration> {
        override fun createFromParcel(parcel: Parcel): LocationConfiguration {
            return LocationConfiguration(parcel)
        }
        
        override fun newArray(size: Int): Array<LocationConfiguration?> {
            return arrayOfNulls(size)
        }
        
        // Convenience factory methods
        fun powerSaveMode() = LocationConfiguration(
            mode = "POWER_SAVE",
            updateIntervalMs = 5000L,
            enableKalmanFilter = true,
            enableSensorFusion = false,
            enableMachineLearning = false,
            enableSwarmOptimization = false,
            batteryOptimization = true
        )
        
        fun balancedMode() = LocationConfiguration(
            mode = "BALANCED",
            updateIntervalMs = 2000L,
            enableKalmanFilter = true,
            enableSensorFusion = true,
            enableMachineLearning = false,
            enableSwarmOptimization = false,
            batteryOptimization = true
        )
        
        fun highAccuracyMode() = LocationConfiguration(
            mode = "HIGH_ACCURACY",
            updateIntervalMs = 1000L,
            enableKalmanFilter = true,
            enableSensorFusion = true,
            enableMachineLearning = true,
            enableSwarmOptimization = false,
            batteryOptimization = false
        )
        
        fun ultraHighAccuracyMode() = LocationConfiguration(
            mode = "ULTRA_HIGH_ACCURACY",
            updateIntervalMs = 500L,
            enableKalmanFilter = true,
            enableSensorFusion = true,
            enableMachineLearning = true,
            enableSwarmOptimization = true,
            batteryOptimization = false
        )
    }
} 