package com.egla.data

import com.egla.domain.EglaRepository
import com.egla.location.client.EGLALocationClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

class DefaultEglaRepository @Inject constructor(
    private val client: EGLALocationClient
) : EglaRepository {

    override fun startLocationUpdates(): Boolean = client.startLocationUpdates()

    override fun stopLocationUpdates() = client.stopLocationUpdates()

    override fun configureHighAccuracy(): Boolean =
        client.configure(com.egla.location.api.LocationConfiguration.highAccuracyMode())

    override val locationFlow: Flow<Any> = client.locationFlow
}