package com.example.physics

import androidx.compose.ui.graphics.Color
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Particle3D(
    val id: String,
    val species: ParticleSpecies,
    var position: Vector3D,
    var momentum: Vector3D, // in GeV/c
    val charge: Double,     // in elementary charges
    val generation: Int = 0, // 0 = initial beam, 1 = collision shower, 2 = secondary decay daughter
    val creationTimeNs: Double = 0.0,
    val lifetimeNs: Double = Double.POSITIVE_INFINITY,
    val colorOverride: Color? = null,
    val trajectoryHistory: MutableList<Vector3D> = mutableListOf(),
    var speedFractionOfC: Float = 0.95f,
    var ageSteps: Int = 0,
    var isDecayed: Boolean = false,
    var isEscaped: Boolean = false
) {
    init {
        if (trajectoryHistory.isEmpty()) {
            trajectoryHistory.add(position)
        }
    }

    /**
     * Relativistic total energy E = sqrt(p^2 + m0^2) in GeV
     */
    fun totalEnergyGeV(): Float {
        val p = momentum.magnitude()
        val m0 = species.restMassGeV.toFloat()
        return sqrt(p * p + m0 * m0)
    }

    /**
     * Relativistic velocity v = (p * c) / E in fraction of lightspeed c
     */
    fun velocityFractionOfC(): Vector3D {
        val energy = totalEnergyGeV()
        return if (energy > 0.0001f) momentum / energy else Vector3D.ZERO
    }

    /**
     * Transverse momentum pT = sqrt(px^2 + py^2)
     */
    fun transverseMomentum(): Float = momentum.transverseMagnitude()

    /**
     * Step particle forward in time under Lorentz force
     */
    fun step(
        dtSeconds: Float,
        magneticFieldTesla: Float,
        electricFieldMVm: Float,
        maxRadiusMeters: Float = 14f,
        maxTrailLength: Int = 20
    ) {
        if (isDecayed || isEscaped) return
        ageSteps++

        if (generation == 0) {
            // Incoming beam particles travel rapidly towards z = 0 with user-configured speed
            val pzSign = if (momentum.z >= 0f) 1f else -1f
            val moveStep = pzSign * dtSeconds * 90.0f * speedFractionOfC
            position = Vector3D(position.x, position.y, position.z + moveStep)

            // Limit history
            while (trajectoryHistory.size > maxTrailLength) trajectoryHistory.removeAt(0)
            trajectoryHistory.add(position)

            // Check if reached collision vertex
            if ((pzSign > 0 && position.z >= 0f) || (pzSign < 0 && position.z <= 0f)) {
                position = Vector3D(position.x, position.y, 0f)
            }
            return
        }

        // Relativistic Lorentz force F = q * (E + v x B)
        val v = velocityFractionOfC()
        val q = charge.toFloat()

        val magneticForceX = q * v.y * magneticFieldTesla * 0.12f
        val magneticForceY = -q * v.x * magneticFieldTesla * 0.12f
        val electricForceZ = q * electricFieldMVm * 0.001f

        val deltaMomentum = Vector3D(magneticForceX, magneticForceY, electricForceZ) * dtSeconds
        momentum += deltaMomentum

        val deltaPosition = velocityFractionOfC() * dtSeconds * 25.0f
        position += deltaPosition

        // Limit trail history length dynamically
        while (trajectoryHistory.size > maxTrailLength) {
            trajectoryHistory.removeAt(0)
        }
        trajectoryHistory.add(position)

        // Escaped detector boundary check
        if (position.magnitude() > maxRadiusMeters || abs(position.z) > maxRadiusMeters * 1.5f) {
            isEscaped = true
        }
    }

    /**
     * Check if short-lived particle triggers secondary displaced decay into orbiting daughters
     */
    fun checkAndTriggerSecondaryDecay(): List<Particle3D>? {
        if (isDecayed || isEscaped || generation != 1) return null
        if (species == StandardModelCatalog.PHOTON || species.category == ParticleCategory.LEPTON && species != StandardModelCatalog.TAU_MINUS) return null

        // Secondary decay occurs after a short displacement from vertex
        if (ageSteps >= 14 && position.magnitude() > 1.5f && position.magnitude() < 6.0f) {
            isDecayed = true

            val d1Species = if (charge > 0) StandardModelCatalog.PION_PLUS else (if (charge < 0) StandardModelCatalog.PION_MINUS else StandardModelCatalog.PION_ZERO)
            val d2Species = StandardModelCatalog.PHOTON

            val p1Mag = momentum.magnitude() * 0.5f
            val p2Mag = momentum.magnitude() * 0.45f

            val angle = 0.4f
            val p1Vec = Vector3D(
                momentum.x * cos(angle) - momentum.y * sin(angle),
                momentum.x * sin(angle) + momentum.y * cos(angle),
                momentum.z * 0.9f
            ).normalized() * p1Mag

            val p2Vec = Vector3D(
                momentum.x * cos(-angle) - momentum.y * sin(-angle),
                momentum.x * sin(-angle) + momentum.y * cos(-angle),
                momentum.z * 0.9f
            ).normalized() * p2Mag

            val daughter1 = Particle3D(
                id = "sec_decay_${UUID.randomUUID().toString().take(5)}",
                species = d1Species,
                position = position,
                momentum = p1Vec,
                charge = d1Species.charge,
                generation = 2
            )

            val daughter2 = Particle3D(
                id = "sec_decay_${UUID.randomUUID().toString().take(5)}",
                species = d2Species,
                position = position,
                momentum = p2Vec,
                charge = d2Species.charge,
                generation = 2
            )

            return listOf(daughter1, daughter2)
        }
        return null
    }
}
