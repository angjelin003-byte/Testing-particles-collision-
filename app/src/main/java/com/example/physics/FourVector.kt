package com.example.physics

import kotlin.math.sqrt

/**
 * Relativistic Minkowski 4-Vector P^μ = (E/c, px, py, pz)
 * Metric signature: (+1, -1, -1, -1)
 */
data class FourVector(
    val e: Double,  // Energy in GeV
    val px: Double, // Momentum X in GeV/c
    val py: Double, // Momentum Y in GeV/c
    val pz: Double  // Momentum Z in GeV/c
) {
    /**
     * Invariant mass squared s = P_μ P^μ = E^2 - |p|^2 c^2
     */
    fun invariantMassSquared(): Double = e * e - (px * px + py * py + pz * pz)

    /**
     * Invariant mass M = sqrt(P_μ P^μ) in GeV/c²
     */
    fun invariantMass(): Double {
        val s = invariantMassSquared()
        return if (s > 0.0) sqrt(s) else 0.0
    }

    /**
     * 3-Momentum magnitude |p| = sqrt(px^2 + py^2 + pz^2)
     */
    fun momentum3D(): Vector3D = Vector3D(px.toFloat(), py.toFloat(), pz.toFloat())

    /**
     * Transverse momentum pT = sqrt(px^2 + py^2)
     */
    fun transverseMomentum(): Double = sqrt(px * px + py * py)

    /**
     * Pseudo-rapidity η = -ln(tan(θ/2)) = 0.5 * ln((|p| + pz) / (|p| - pz))
     */
    fun pseudoRapidity(): Double {
        val p = momentum3D().magnitude().toDouble()
        if (p == 0.0) return 0.0
        val denom = p - pz
        val num = p + pz
        return if (denom > 1e-9 && num > 0.0) 0.5 * kotlin.math.ln(num / denom) else 0.0
    }

    /**
     * Azimuthal angle φ = atan2(py, px) in radians
     */
    fun phi(): Double = kotlin.math.atan2(py, px)

    operator fun plus(other: FourVector) = FourVector(
        e = e + other.e,
        px = px + other.px,
        py = py + other.py,
        pz = pz + other.pz
    )

    operator fun minus(other: FourVector) = FourVector(
        e = e - other.e,
        px = px - other.px,
        py = py - other.py,
        pz = pz - other.pz
    )

    operator fun times(scalar: Double) = FourVector(e * scalar, px * scalar, py * scalar, pz * scalar)

    companion object {
        val ZERO = FourVector(0.0, 0.0, 0.0, 0.0)

        fun fromParticle(species: ParticleSpecies, p3d: Vector3D): FourVector {
            val px = p3d.x.toDouble()
            val py = p3d.y.toDouble()
            val pz = p3d.z.toDouble()
            val m0 = species.restMassGeV
            val energy = sqrt(px * px + py * py + pz * pz + m0 * m0)
            return FourVector(energy, px, py, pz)
        }
    }
}
