package com.example.physics

import androidx.compose.ui.graphics.Color
import kotlin.math.sqrt

data class Particle3D(
    val id: String,
    val species: ParticleSpecies,
    var position: Vector3D,
    var momentum: Vector3D, // in GeV/c
    val charge: Double,     // in elementary charges
    val generation: Int = 0, // 0 = initial beam, 1 = collision shower, 2 = decay daughter
    val creationTimeNs: Double = 0.0,
    val lifetimeNs: Double = Double.POSITIVE_INFINITY,
    val colorOverride: Color? = null,
    val trajectoryHistory: MutableList<Vector3D> = mutableListOf(),
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
     * Step the particle forward in time under relativistic magnetic field B_z (Tesla)
     */
    fun step(
        dtSeconds: Float,
        magneticFieldTesla: Float,
        electricFieldMVm: Float,
        maxRadiusMeters: Float = 15f
    ) {
        if (isDecayed || isEscaped) return

        // Speed of light scaling constant in detector meter units
        val c = 3.0e8f

        // Calculate relativistic Lorentz force F = q * (E + v x B)
        val v = velocityFractionOfC() // fraction of c
        val q = charge.toFloat()

        // Magnetic field along Z-axis (Bz)
        // Lorentz force perpendicular component: F_x = q * v_y * B, F_y = -q * v_x * B
        val magneticForceX = q * v.y * magneticFieldTesla * 0.15f
        val magneticForceY = -q * v.x * magneticFieldTesla * 0.15f
        val electricForceZ = q * electricFieldMVm * 0.001f

        val deltaMomentum = Vector3D(magneticForceX, magneticForceY, electricForceZ) * dtSeconds

        // Update momentum vector
        momentum += deltaMomentum

        // Update position: dx = v * c * dt
        val deltaPosition = velocityFractionOfC() * dtSeconds * 12.0f
        position += deltaPosition

        // Record history for trajectory rendering
        if (trajectoryHistory.size > 100) {
            trajectoryHistory.removeAt(0)
        }
        trajectoryHistory.add(position)

        // Escaped detector radius boundary check
        if (position.magnitude() > maxRadiusMeters || kotlin.math.abs(position.z) > maxRadiusMeters * 1.5f) {
            isEscaped = true
        }
    }
}
