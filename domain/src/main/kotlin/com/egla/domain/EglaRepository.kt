package com.egla.domain

import com.egla.core.DirectionCalculator
import kotlinx.coroutines.flow.Flow

interface EglaRepository {
    fun startLocationUpdates(): Boolean
    fun stopLocationUpdates()
    fun configureHighAccuracy(): Boolean
    val locationFlow: Flow<Any> // placeholder – define proper model later
}