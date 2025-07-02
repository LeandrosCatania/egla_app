package com.egla.location.api

import android.os.Parcel
import android.os.Parcelable

/**
 * Parcelable data class for passing enhanced location data through AIDL
 */
data class EnhancedLocationData(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracy: Float,
    val bearing: Float?,
    val speed: Float?,
    val originalLatitude: Double,
    val originalLongitude: Double,
    val originalAccuracy: Float,
    val accuracyImprovement: Float,
    val confidence: Float,
    val environment: String?,
    val processingTimeMs: Long
) : Parcelable {
    
    constructor(parcel: Parcel) : this(
        timestamp = parcel.readLong(),
        latitude = parcel.readDouble(),
        longitude = parcel.readDouble(),
        altitude = parcel.readValue(Double::class.java.classLoader) as? Double,
        accuracy = parcel.readFloat(),
        bearing = parcel.readValue(Float::class.java.classLoader) as? Float,
        speed = parcel.readValue(Float::class.java.classLoader) as? Float,
        originalLatitude = parcel.readDouble(),
        originalLongitude = parcel.readDouble(),
        originalAccuracy = parcel.readFloat(),
        accuracyImprovement = parcel.readFloat(),
        confidence = parcel.readFloat(),
        environment = parcel.readString(),
        processingTimeMs = parcel.readLong()
    )
    
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(timestamp)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeValue(altitude)
        parcel.writeFloat(accuracy)
        parcel.writeValue(bearing)
        parcel.writeValue(speed)
        parcel.writeDouble(originalLatitude)
        parcel.writeDouble(originalLongitude)
        parcel.writeFloat(originalAccuracy)
        parcel.writeFloat(accuracyImprovement)
        parcel.writeFloat(confidence)
        parcel.writeString(environment)
        parcel.writeLong(processingTimeMs)
    }
    
    override fun describeContents(): Int = 0
    
    companion object CREATOR : Parcelable.Creator<EnhancedLocationData> {
        override fun createFromParcel(parcel: Parcel): EnhancedLocationData {
            return EnhancedLocationData(parcel)
        }
        
        override fun newArray(size: Int): Array<EnhancedLocationData?> {
            return arrayOfNulls(size)
        }
    }
} 