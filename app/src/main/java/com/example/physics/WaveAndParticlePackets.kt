package com.example.physics

import androidx.compose.ui.graphics.Color
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class PacketDualityMode(
    val displayName: String,
    val shortName: String,
    val description: String
) {
    DUAL_WAVE_PARTICLE(
        "Dual Wave-Particle",
        "Wave+Particle",
        "Visualizes both quantum de Broglie wave packets and collimated particle bunch packets."
    ),
    WAVE_PACKETS_ONLY(
        "Wave Packets Only",
        "Waves Only",
        "Displays quantum probability density envelopes, wavefront ripples, and de Broglie phase oscillations."
    ),
    PARTICLE_PACKETS_ONLY(
        "Particle Packets Only",
        "Packets Only",
        "Displays collimated parton cluster envelopes, jet cones, and particle bunch centroids."
    ),
    CLASSICAL_TRACKS(
        "Classical Tracks",
        "Tracks",
        "Displays discrete particle points and relativistic trajectories only."
    )
}

/**
 * Quantum De Broglie Wave Packet in 3D spacetime:
 * ψ(r, t) = A(t) * exp(-|r - rc|^2 / (2*σ^2)) * exp(i*(k·r - ω*t))
 */
data class WavePacket3D(
    val id: String,
    val name: String,
    val packetType: String, // "De Broglie Wave Packet", "Photon EM Wavefront", "Gluon Color Field Packet", "Spherical Quantum Vacuum Wave"
    var center: Vector3D,
    var velocity: Vector3D, // fraction of light speed c
    var momentum: Vector3D, // GeV/c
    val deBroglieWavelengthMeters: Float, // scaled for detector scale ~ 0.4m to 1.8m
    val frequency: Float, // ω = E/ħ scale
    var phase: Float = 0f, // oscillating phase
    var envelopeWidth: Float = 0.8f, // σ(t) in meters (Gaussian wave packet width)
    var amplitude: Float = 1.0f, // probability amplitude |ψ|
    val color: Color,
    val species: ParticleSpecies? = null,
    val trajectoryHistory: MutableList<Vector3D> = mutableListOf(),
    var ageSteps: Int = 0,
    var isDissipated: Boolean = false,
    val isSphericalCoherentWave: Boolean = false
) {
    init {
        if (trajectoryHistory.isEmpty()) {
            trajectoryHistory.add(center)
        }
    }

    /**
     * Advance wave packet state by dtSeconds with quantum dispersion
     */
    fun step(
        dtSeconds: Float,
        dispersionRate: Float = 1.0f,
        frequencyScale: Float = 1.0f,
        maxRadiusMeters: Float = 14.0f
    ) {
        if (isDissipated) return
        ageSteps++

        // 1. Advance spatial centroid
        val deltaPos = velocity * dtSeconds * 24.0f
        center += deltaPos

        // 2. Oscillate wave phase: φ = ω*t
        phase += frequency * dtSeconds * 12.0f * frequencyScale
        if (phase > 2f * PI.toFloat()) {
            phase -= 2f * PI.toFloat()
        }

        // 3. Quantum wavepacket dispersion: envelope spreading σ(t)
        envelopeWidth += dtSeconds * 0.40f * dispersionRate

        // 4. Amplitude attenuation as wavepacket spreads across detector volume
        amplitude = (amplitude * (1.0f - dtSeconds * 0.035f)).coerceAtLeast(0.08f)

        // 5. Update history
        while (trajectoryHistory.size > 18) {
            trajectoryHistory.removeAt(0)
        }
        trajectoryHistory.add(center)

        // 6. Boundary check
        if (center.magnitude() > maxRadiusMeters || abs(center.z) > maxRadiusMeters * 1.5f || amplitude <= 0.08f) {
            isDissipated = true
        }
    }
}

/**
 * Collimated Particle Packet / Bunch in High-Energy Collider Physics:
 * Represents a coherent cluster of subatomic particles ejected as a collimated bunch
 * (e.g. quark-gluon jet cone bunches, back-to-back lepton pair packets, hard bremsstrahlung packets).
 */
data class ParticlePacket3D(
    val id: String,
    val name: String,
    val packetCategory: String, // "QCD Hadronic Jet Packet", "Lepton Pair Packet", "Vector Boson Cluster", "Bremsstrahlung Packet"
    var centroid: Vector3D,
    var velocity: Vector3D,
    var totalMomentum: Vector3D,
    val totalEnergyGeV: Double,
    val constituentIds: List<String>,
    val constituentSpeciesNames: List<String>,
    var bunchRadius: Float = 0.65f, // transverse cluster spread in meters
    val openingAngleRad: Float = 0.28f, // jet cone opening angle
    val color: Color,
    val trajectoryHistory: MutableList<Vector3D> = mutableListOf(),
    var ageSteps: Int = 0,
    var isEscaped: Boolean = false
) {
    init {
        if (trajectoryHistory.isEmpty()) {
            trajectoryHistory.add(centroid)
        }
    }

    /**
     * Advance particle bunch centroid and update transverse cluster radius
     */
    fun step(
        dtSeconds: Float,
        coneScale: Float = 1.0f,
        maxRadiusMeters: Float = 14.0f
    ) {
        if (isEscaped) return
        ageSteps++

        // Advance bunch centroid
        val deltaPos = velocity * dtSeconds * 25.0f
        centroid += deltaPos

        // Expand bunch radius slightly along opening cone
        bunchRadius += dtSeconds * 0.22f * coneScale

        while (trajectoryHistory.size > 20) {
            trajectoryHistory.removeAt(0)
        }
        trajectoryHistory.add(centroid)

        if (centroid.magnitude() > maxRadiusMeters || abs(centroid.z) > maxRadiusMeters * 1.5f) {
            isEscaped = true
        }
    }
}

/**
 * Ejection Engine for Quantum Wave Packets & Particle Packets
 */
object PacketEjectionEngine {

    /**
     * Generate wave packets and particle packets ejected right after main collision
     */
    fun createCollisionPackets(
        generatedParticles: List<Particle3D>,
        sqrtSGeV: Double,
        channelMode: CollisionChannelMode
    ): Pair<List<WavePacket3D>, List<ParticlePacket3D>> {
        val wavePackets = mutableListOf<WavePacket3D>()
        val particlePackets = mutableListOf<ParticlePacket3D>()

        if (generatedParticles.isEmpty()) {
            return Pair(emptyList(), emptyList())
        }

        // 1. Central Spherical Coherent Vacuum Wave Packet (Quantum Field Perturbation at Vertex)
        val centralWave = WavePacket3D(
            id = "wave_vacuum_spherical_${UUID.randomUUID().toString().take(4)}",
            name = "Coherent Vacuum Polarization Wave",
            packetType = "Spherical Quantum Vacuum Wave",
            center = Vector3D.ZERO,
            velocity = Vector3D(0f, 0.05f, 0f),
            momentum = Vector3D(0f, 1f, 0f),
            deBroglieWavelengthMeters = 0.95f,
            frequency = 3.2f,
            envelopeWidth = 0.70f,
            amplitude = 1.0f,
            color = Color(0xFF00E5FF),
            isSphericalCoherentWave = true
        )
        wavePackets.add(centralWave)

        // 2. Collimated Wave Packets along leading particle directions:
        // Pick the most energetic or representative particle tracks to create localized wave packets
        val sortedParticles = generatedParticles.sortedByDescending { it.totalEnergyGeV() }
        val leadingParticles = sortedParticles.take(8)

        for ((idx, p) in leadingParticles.withIndex()) {
            val pMag = p.momentum.magnitude().coerceAtLeast(0.1f)
            val dir = if (pMag > 0.01f) p.momentum.normalized() else Vector3D(0f, 1f, 0f)
            val vFrac = p.velocityFractionOfC()

            // de Broglie wavelength λ = h/p scaled to detector meter scale
            // High energy -> shorter wavelength; lower energy -> longer wavelength
            val wavelength = (1.8f / sqrt(pMag.coerceIn(0.5f, 50.0f))).coerceIn(0.35f, 1.9f)
            val freq = (1.5f + sqrt(p.totalEnergyGeV().coerceIn(1.0f, 100.0f)) * 0.45f).coerceIn(1.2f, 6.0f)
            val startCenter = dir * 0.35f

            val wavePacket = WavePacket3D(
                id = "wave_pkt_${idx}_${p.species.id}_${UUID.randomUUID().toString().take(4)}",
                name = "${p.species.name} De Broglie Packet",
                packetType = when (p.species.category) {
                    ParticleCategory.GAUGE_BOSON -> "Relativistic EM / Gauge Wave Envelope"
                    ParticleCategory.HIGGS_BOSON -> "Higgs Field Quantum Wavepacket"
                    ParticleCategory.LEPTON -> "Leptonic De Broglie Wave Packet"
                    else -> "Hadronic Wave Envelope"
                },
                center = startCenter,
                velocity = vFrac,
                momentum = p.momentum,
                deBroglieWavelengthMeters = wavelength,
                frequency = freq,
                envelopeWidth = 0.85f + (idx * 0.05f),
                amplitude = 1.0f,
                color = p.species.color,
                species = p.species
            )
            wavePackets.add(wavePacket)
        }

        // 3. Particle Packets (Bunches / Collimated Clusters)
        when (channelMode) {
            CollisionChannelMode.HADRONIC_JETS, CollisionChannelMode.AUTO -> {
                // High multiplicity di-jet shower: cluster into Jet 1 and Jet 2 bunches
                val halfSize = generatedParticles.size / 2
                val jet1Particles = generatedParticles.take(halfSize)
                val jet2Particles = generatedParticles.drop(halfSize)

                if (jet1Particles.isNotEmpty()) {
                    var sumMom1 = Vector3D.ZERO
                    var sumE1 = 0.0
                    for (part in jet1Particles) {
                        sumMom1 += part.momentum
                        sumE1 += part.totalEnergyGeV()
                    }
                    val dir1 = if (sumMom1.magnitude() > 0.01f) sumMom1.normalized() else Vector3D(1f, 0f, 0f)
                    val v1 = if (sumE1 > 0.01) sumMom1 / sumE1.toFloat() else dir1 * 0.95f

                    particlePackets.add(
                        ParticlePacket3D(
                            id = "pkt_hadron_jet_1_${UUID.randomUUID().toString().take(4)}",
                            name = "QCD Di-Jet Packet α (N=${jet1Particles.size})",
                            packetCategory = "QCD Hadronic Jet Packet",
                            centroid = dir1 * 0.40f,
                            velocity = v1,
                            totalMomentum = sumMom1,
                            totalEnergyGeV = sumE1,
                            constituentIds = jet1Particles.map { it.id },
                            constituentSpeciesNames = jet1Particles.take(6).map { it.species.symbol },
                            bunchRadius = 0.70f,
                            openingAngleRad = 0.32f,
                            color = Color(0xFFFF9100) // Solar amber for QCD Jet
                        )
                    )
                }

                if (jet2Particles.isNotEmpty()) {
                    var sumMom2 = Vector3D.ZERO
                    var sumE2 = 0.0
                    for (part in jet2Particles) {
                        sumMom2 += part.momentum
                        sumE2 += part.totalEnergyGeV()
                    }
                    val dir2 = if (sumMom2.magnitude() > 0.01f) sumMom2.normalized() else Vector3D(-1f, 0f, 0f)
                    val v2 = if (sumE2 > 0.01) sumMom2 / sumE2.toFloat() else dir2 * 0.95f

                    particlePackets.add(
                        ParticlePacket3D(
                            id = "pkt_hadron_jet_2_${UUID.randomUUID().toString().take(4)}",
                            name = "QCD Di-Jet Packet β (N=${jet2Particles.size})",
                            packetCategory = "QCD Hadronic Jet Packet",
                            centroid = dir2 * 0.40f,
                            velocity = v2,
                            totalMomentum = sumMom2,
                            totalEnergyGeV = sumE2,
                            constituentIds = jet2Particles.map { it.id },
                            constituentSpeciesNames = jet2Particles.take(6).map { it.species.symbol },
                            bunchRadius = 0.70f,
                            openingAngleRad = 0.32f,
                            color = Color(0xFF00E5FF) // Cyan for recoil Jet
                        )
                    )
                }
            }

            CollisionChannelMode.ELASTIC_2_BODY -> {
                // 2 recoil particle bunches
                for ((idx, p) in generatedParticles.take(2).withIndex()) {
                    val dir = if (p.momentum.magnitude() > 0.01f) p.momentum.normalized() else Vector3D(0f, 1f, 0f)
                    particlePackets.add(
                        ParticlePacket3D(
                            id = "pkt_elastic_${idx}_${UUID.randomUUID().toString().take(4)}",
                            name = "Elastic Recoil Packet ${if (idx == 0) "A" else "B"} (${p.species.symbol})",
                            packetCategory = "Elastic Recoil Bunch",
                            centroid = dir * 0.35f,
                            velocity = p.velocityFractionOfC(),
                            totalMomentum = p.momentum,
                            totalEnergyGeV = p.totalEnergyGeV().toDouble(),
                            constituentIds = listOf(p.id),
                            constituentSpeciesNames = listOf(p.species.symbol),
                            bunchRadius = 0.45f,
                            openingAngleRad = 0.12f,
                            color = p.species.color
                        )
                    )
                }
            }

            CollisionChannelMode.ANNIHILATION_PAIR -> {
                // 2 lepton pair bunches
                for ((idx, p) in generatedParticles.take(2).withIndex()) {
                    val dir = if (p.momentum.magnitude() > 0.01f) p.momentum.normalized() else Vector3D(0f, 1f, 0f)
                    particlePackets.add(
                        ParticlePacket3D(
                            id = "pkt_annihil_${idx}_${UUID.randomUUID().toString().take(4)}",
                            name = "Lepton Pair Packet [${p.species.symbol}]",
                            packetCategory = "Lepton Pair Packet",
                            centroid = dir * 0.35f,
                            velocity = p.velocityFractionOfC(),
                            totalMomentum = p.momentum,
                            totalEnergyGeV = p.totalEnergyGeV().toDouble(),
                            constituentIds = listOf(p.id),
                            constituentSpeciesNames = listOf(p.species.symbol),
                            bunchRadius = 0.48f,
                            openingAngleRad = 0.14f,
                            color = p.species.color
                        )
                    )
                }
            }

            CollisionChannelMode.RADIATIVE_QED -> {
                // 3 packets: 2 recoil leptons + 1 hard photon packet
                for (p in generatedParticles.take(3)) {
                    val dir = if (p.momentum.magnitude() > 0.01f) p.momentum.normalized() else Vector3D(0f, 1f, 0f)
                    val isGamma = p.species == StandardModelCatalog.PHOTON
                    particlePackets.add(
                        ParticlePacket3D(
                            id = "pkt_rad_${p.species.id}_${UUID.randomUUID().toString().take(4)}",
                            name = if (isGamma) "Hard Bremsstrahlung Packet (γ)" else "Recoil Lepton Packet (${p.species.symbol})",
                            packetCategory = if (isGamma) "Bremsstrahlung Packet" else "Lepton Recoil Packet",
                            centroid = dir * 0.35f,
                            velocity = p.velocityFractionOfC(),
                            totalMomentum = p.momentum,
                            totalEnergyGeV = p.totalEnergyGeV().toDouble(),
                            constituentIds = listOf(p.id),
                            constituentSpeciesNames = listOf(p.species.symbol),
                            bunchRadius = if (isGamma) 0.55f else 0.45f,
                            openingAngleRad = 0.15f,
                            color = p.species.color
                        )
                    )
                }
            }

            CollisionChannelMode.ELECTROWEAK_BOSONS -> {
                // Group 4 fermions into 2 boson decay bunches
                val half = generatedParticles.size / 2
                val bunch1 = generatedParticles.take(half)
                val bunch2 = generatedParticles.drop(half)

                if (bunch1.isNotEmpty()) {
                    var sumMom = Vector3D.ZERO
                    var sumE = 0.0
                    for (part in bunch1) {
                        sumMom += part.momentum
                        sumE += part.totalEnergyGeV()
                    }
                    val dir = if (sumMom.magnitude() > 0.01f) sumMom.normalized() else Vector3D(1f, 0f, 0f)
                    particlePackets.add(
                        ParticlePacket3D(
                            id = "pkt_ew_1_${UUID.randomUUID().toString().take(4)}",
                            name = "Vector Boson Daughter Packet 1 (${bunch1.joinToString(" ") { it.species.symbol }})",
                            packetCategory = "Vector Boson Cluster",
                            centroid = dir * 0.38f,
                            velocity = if (sumE > 0.01) sumMom / sumE.toFloat() else dir * 0.95f,
                            totalMomentum = sumMom,
                            totalEnergyGeV = sumE,
                            constituentIds = bunch1.map { it.id },
                            constituentSpeciesNames = bunch1.map { it.species.symbol },
                            bunchRadius = 0.60f,
                            openingAngleRad = 0.25f,
                            color = Color(0xFF7C4DFF)
                        )
                    )
                }

                if (bunch2.isNotEmpty()) {
                    var sumMom = Vector3D.ZERO
                    var sumE = 0.0
                    for (part in bunch2) {
                        sumMom += part.momentum
                        sumE += part.totalEnergyGeV()
                    }
                    val dir = if (sumMom.magnitude() > 0.01f) sumMom.normalized() else Vector3D(-1f, 0f, 0f)
                    particlePackets.add(
                        ParticlePacket3D(
                            id = "pkt_ew_2_${UUID.randomUUID().toString().take(4)}",
                            name = "Vector Boson Daughter Packet 2 (${bunch2.joinToString(" ") { it.species.symbol }})",
                            packetCategory = "Vector Boson Cluster",
                            centroid = dir * 0.38f,
                            velocity = if (sumE > 0.01) sumMom / sumE.toFloat() else dir * 0.95f,
                            totalMomentum = sumMom,
                            totalEnergyGeV = sumE,
                            constituentIds = bunch2.map { it.id },
                            constituentSpeciesNames = bunch2.map { it.species.symbol },
                            bunchRadius = 0.60f,
                            openingAngleRad = 0.25f,
                            color = Color(0xFFFF4081)
                        )
                    )
                }
            }
        }

        return Pair(wavePackets, particlePackets)
    }
}
