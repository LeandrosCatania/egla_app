package com.egla.location.core

import android.location.Location
import com.egla.location.EGLAConfiguration
import kotlin.math.*
import kotlin.random.Random

/**
 * Swarm Intelligence Optimizer for GPS position estimation
 * Uses Particle Swarm Optimization to find optimal position estimates
 */
class SwarmOptimizer(
    private val config: EGLAConfiguration.SwarmOptimizationConfig
) {
    
    private val particles = mutableListOf<Particle>()
    private var globalBest: Position? = null
    private var globalBestFitness = Double.MAX_VALUE
    
    // Search bounds (in meters from initial position)
    private val searchRadius = 100.0 // meters
    
    /**
     * Optimize location using particle swarm optimization
     */
    fun optimize(location: Location): Location {
        if (!config.enabled) return location
        
        // Initialize swarm if needed
        if (particles.isEmpty()) {
            initializeSwarm(location)
        }
        
        // Update search center to current location
        updateSearchCenter(location)
        
        // Run optimization iterations
        for (iteration in 0 until config.maxIterations) {
            updateParticles(location)
            
            // Check convergence
            if (hasConverged()) break
        }
        
        // Return optimized location
        return globalBest?.let { best ->
            val optimizedLocation = Location(location)
            val coords = cartesianToGps(best.x, best.y, location)
            optimizedLocation.latitude = coords.first
            optimizedLocation.longitude = coords.second
            
            // Improve accuracy based on optimization confidence
            val improvement = calculateOptimizationImprovement()
            optimizedLocation.accuracy = location.accuracy * (1.0f - improvement)
            
            optimizedLocation
        } ?: location
    }
    
    private fun initializeSwarm(referenceLocation: Location) {
        particles.clear()
        
        val center = gpsToCartesian(referenceLocation.latitude, referenceLocation.longitude)
        
        repeat(config.particleCount) {
            val particle = Particle(
                position = Position(
                    x = center.first + Random.nextDouble(-searchRadius, searchRadius),
                    y = center.second + Random.nextDouble(-searchRadius, searchRadius)
                ),
                velocity = Velocity(
                    vx = Random.nextDouble(-10.0, 10.0),
                    vy = Random.nextDouble(-10.0, 10.0)
                )
            )
            
            particle.personalBest = particle.position.copy()
            particle.personalBestFitness = evaluateFitness(particle.position, referenceLocation)
            
            particles.add(particle)
        }
        
        // Find initial global best
        updateGlobalBest(referenceLocation)
    }
    
    private fun updateSearchCenter(location: Location) {
        val center = gpsToCartesian(location.latitude, location.longitude)
        
        // Gradually shift search space toward new location
        globalBest?.let { best ->
            val shift = 0.1 // Shift factor
            best.x = best.x * (1 - shift) + center.first * shift
            best.y = best.y * (1 - shift) + center.second * shift
        }
    }
    
    private fun updateParticles(referenceLocation: Location) {
        particles.forEach { particle ->
            updateParticleVelocity(particle)
            updateParticlePosition(particle)
            
            // Evaluate fitness
            val fitness = evaluateFitness(particle.position, referenceLocation)
            
            // Update personal best
            if (fitness < particle.personalBestFitness) {
                particle.personalBest = particle.position.copy()
                particle.personalBestFitness = fitness
            }
        }
        
        // Update global best
        updateGlobalBest(referenceLocation)
    }
    
    private fun updateParticleVelocity(particle: Particle) {
        val r1 = Random.nextDouble()
        val r2 = Random.nextDouble()
        
        // Update velocity components
        particle.velocity.vx = config.inertiaWeight * particle.velocity.vx +
                config.cognitiveWeight * r1 * (particle.personalBest.x - particle.position.x) +
                config.socialWeight * r2 * (globalBest?.x?.minus(particle.position.x) ?: 0.0)
        
        particle.velocity.vy = config.inertiaWeight * particle.velocity.vy +
                config.cognitiveWeight * r1 * (particle.personalBest.y - particle.position.y) +
                config.socialWeight * r2 * (globalBest?.y?.minus(particle.position.y) ?: 0.0)
        
        // Limit velocity to prevent explosion
        val maxVelocity = 20.0
        particle.velocity.vx = particle.velocity.vx.coerceIn(-maxVelocity, maxVelocity)
        particle.velocity.vy = particle.velocity.vy.coerceIn(-maxVelocity, maxVelocity)
    }
    
    private fun updateParticlePosition(particle: Particle) {
        particle.position.x += particle.velocity.vx
        particle.position.y += particle.velocity.vy
        
        // Keep particles within search bounds
        val center = globalBest ?: particle.personalBest
        val distanceFromCenter = sqrt(
            (particle.position.x - center.x).pow(2) +
            (particle.position.y - center.y).pow(2)
        )
        
        if (distanceFromCenter > searchRadius) {
            // Bring particle back to bounds
            val angle = atan2(particle.position.y - center.y, particle.position.x - center.x)
            particle.position.x = center.x + searchRadius * cos(angle)
            particle.position.y = center.y + searchRadius * sin(angle)
        }
    }
    
    private fun evaluateFitness(position: Position, referenceLocation: Location): Double {
        // Multi-objective fitness function
        var fitness = 0.0
        
        // 1. Distance from GPS measurement (primary objective)
        val gpsPosition = gpsToCartesian(referenceLocation.latitude, referenceLocation.longitude)
        val gpsDistance = sqrt(
            (position.x - gpsPosition.first).pow(2) +
            (position.y - gpsPosition.second).pow(2)
        )
        fitness += gpsDistance * 10.0 // Weight GPS accuracy highly
        
        // 2. Consistency with particle swarm (secondary objective)
        val avgParticlePosition = calculateAverageParticlePosition()
        val swarmDistance = sqrt(
            (position.x - avgParticlePosition.first).pow(2) +
            (position.y - avgParticlePosition.second).pow(2)
        )
        fitness += swarmDistance * 2.0
        
        // 3. Stability penalty (avoid rapid changes)
        globalBest?.let { best ->
            val stabilityDistance = sqrt(
                (position.x - best.x).pow(2) +
                (position.y - best.y).pow(2)
            )
            fitness += stabilityDistance * 1.0
        }
        
        // 4. Quality based on GPS accuracy
        val accuracyPenalty = referenceLocation.accuracy / 5.0 // Normalize accuracy
        fitness += accuracyPenalty
        
        return fitness
    }
    
    private fun calculateAverageParticlePosition(): Pair<Double, Double> {
        if (particles.isEmpty()) return Pair(0.0, 0.0)
        
        val avgX = particles.map { it.position.x }.average()
        val avgY = particles.map { it.position.y }.average()
        
        return Pair(avgX, avgY)
    }
    
    private fun updateGlobalBest(referenceLocation: Location) {
        particles.forEach { particle ->
            val fitness = evaluateFitness(particle.position, referenceLocation)
            
            if (fitness < globalBestFitness) {
                globalBest = particle.position.copy()
                globalBestFitness = fitness
            }
        }
    }
    
    private fun hasConverged(): Boolean {
        if (particles.size < 2) return true
        
        // Calculate position variance among particles
        val positions = particles.map { it.position }
        val avgX = positions.map { it.x }.average()
        val avgY = positions.map { it.y }.average()
        
        val variance = positions.map { pos ->
            (pos.x - avgX).pow(2) + (pos.y - avgY).pow(2)
        }.average()
        
        return variance < config.convergenceThreshold.pow(2)
    }
    
    private fun calculateOptimizationImprovement(): Float {
        if (globalBestFitness == Double.MAX_VALUE) return 0.0f
        
        // Calculate improvement based on fitness convergence
        val maxExpectedFitness = 100.0 // Expected worst case
        val improvement = (maxExpectedFitness - globalBestFitness) / maxExpectedFitness
        
        return improvement.coerceIn(0.0, 0.5).toFloat() // Max 50% improvement
    }
    
    private fun gpsToCartesian(latitude: Double, longitude: Double): Pair<Double, Double> {
        // Simple conversion to local Cartesian coordinates
        val x = longitude * 111320.0 * cos(Math.toRadians(latitude))
        val y = latitude * 111320.0
        return Pair(x, y)
    }
    
    private fun cartesianToGps(x: Double, y: Double, referenceLocation: Location): Pair<Double, Double> {
        // Convert back from local Cartesian to GPS coordinates
        val latitude = y / 111320.0
        val longitude = x / (111320.0 * cos(Math.toRadians(referenceLocation.latitude)))
        return Pair(latitude, longitude)
    }
    
    /**
     * Stop the optimizer and cleanup
     */
    fun stop() {
        particles.clear()
        globalBest = null
        globalBestFitness = Double.MAX_VALUE
    }
    
    /**
     * Get optimization statistics
     */
    fun getOptimizationStats(): OptimizationStats {
        val convergence = if (particles.isNotEmpty()) {
            val variance = calculatePositionVariance()
            1.0 - (variance / (searchRadius * searchRadius))
        } else {
            0.0
        }
        
        return OptimizationStats(
            particleCount = particles.size,
            globalBestFitness = if (globalBestFitness != Double.MAX_VALUE) globalBestFitness else 0.0,
            convergence = convergence.coerceIn(0.0, 1.0),
            searchRadius = searchRadius
        )
    }
    
    private fun calculatePositionVariance(): Double {
        if (particles.size < 2) return 0.0
        
        val positions = particles.map { it.position }
        val avgX = positions.map { it.x }.average()
        val avgY = positions.map { it.y }.average()
        
        return positions.map { pos ->
            (pos.x - avgX).pow(2) + (pos.y - avgY).pow(2)
        }.average()
    }
    
    // Data classes
    
    private data class Particle(
        var position: Position,
        var velocity: Velocity,
        var personalBest: Position = Position(0.0, 0.0),
        var personalBestFitness: Double = Double.MAX_VALUE
    )
    
    private data class Position(
        var x: Double,
        var y: Double
    )
    
    private data class Velocity(
        var vx: Double,
        var vy: Double
    )
    
    data class OptimizationStats(
        val particleCount: Int,
        val globalBestFitness: Double,
        val convergence: Double,
        val searchRadius: Double
    )
} 