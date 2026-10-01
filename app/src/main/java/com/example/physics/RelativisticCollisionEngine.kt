package com.example.physics

import androidx.compose.ui.graphics.Color
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class CollisionEventResult(
    val eventId: Long,
    val initialBeamA: ParticleSpecies,
    val initialBeamB: ParticleSpecies,
    val centerOfMassEnergyGeV: Double,
    val impactParameterFm: Double,
    val generatedParticles: List<Particle3D>,
    val multiplicity: Int,
    val invariantMassGeV: Double,
    val totalTransverseEnergyGeV: Double,
    val primaryProcessName: String,
    val decayTreeFormatted: String
)

object RelativisticCollisionEngine {

    /**
     * Create incoming head-on beam particles traveling towards center (0,0,0)
     */
    fun createIncomingBeams(
        beamA: ParticleSpecies,
        beamB: ParticleSpecies,
        energyGeV: Double,
        impactParameterFm: Double
    ): List<Particle3D> {
        val pzA = sqrt(energyGeV * energyGeV - beamA.restMassGeV * beamA.restMassGeV).toFloat()
        val pzB = -sqrt(energyGeV * energyGeV - beamB.restMassGeV * beamB.restMassGeV).toFloat()

        val offsetY = (impactParameterFm * 0.05f).toFloat()

        val particle1 = Particle3D(
            id = "beam_A_" + UUID.randomUUID().toString().take(6),
            species = beamA,
            position = Vector3D(0f, offsetY, -12f),
            momentum = Vector3D(0f, 0f, pzA),
            charge = beamA.charge,
            generation = 0,
            colorOverride = Color(0xFF00E5FF)
        )

        val particle2 = Particle3D(
            id = "beam_B_" + UUID.randomUUID().toString().take(6),
            species = beamB,
            position = Vector3D(0f, -offsetY, 12f),
            momentum = Vector3D(0f, 0f, pzB),
            charge = beamB.charge,
            generation = 0,
            colorOverride = Color(0xFFFF9100)
        )

        return listOf(particle1, particle2)
    }

    /**
     * Trigger head-on relativistic collision at center (0,0,0) generating daughter particles
     */
    fun simulateCollision(
        eventId: Long,
        beamA: ParticleSpecies,
        beamB: ParticleSpecies,
        centerOfMassEnergyGeV: Double,
        impactParameterFm: Double,
        luminosityScale: Float = 1.0f
    ): CollisionEventResult {
        val random = Random(eventId + System.currentTimeMillis())
        val generatedList = mutableListOf<Particle3D>()
        val decayTreeLines = mutableListOf<String>()

        decayTreeLines.add("┌─ COLLISION EVENT #${eventId}")
        decayTreeLines.add("│ Beam A: ${beamA.name} (${beamA.symbol}) | Beam B: ${beamB.name} (${beamB.symbol})")
        decayTreeLines.add("│ Center-of-Mass Energy √s: %.1f GeV | Impact Parameter: %.2f fm".format(centerOfMassEnergyGeV, impactParameterFm))

        // Determine primary interaction channel
        val isLeptonPair = (beamA.category == ParticleCategory.LEPTON && beamB.category == ParticleCategory.LEPTON)
        val isHeavyIon = (beamA.category == ParticleCategory.NUCLEUS || beamB.category == ParticleCategory.NUCLEUS)
        val isAnnihilation = (beamA.isAntimatter != beamB.isAntimatter && beamA.name.lowercase().contains(beamB.name.lowercase().replace("anti", "")))

        val processName: String
        val particleCountToGen: Int

        if (isHeavyIon) {
            processName = "Quark-Gluon Plasma Fireball (Ultra-Relativistic Nucleus Heavy Ion)"
            particleCountToGen = (35 + random.nextInt(30) * luminosityScale.toInt().coerceAtLeast(1)).coerceAtMost(90)
        } else if (isAnnihilation) {
            processName = "Matter-Antimatter Quantum Annihilation (Full Energy Conversion)"
            particleCountToGen = (20 + random.nextInt(20)).coerceAtMost(60)
        } else if (isLeptonPair) {
            processName = "Electroweak Drell-Yan / Dilepton Resonant Production"
            particleCountToGen = 8 + random.nextInt(10)
        } else {
            // Proton-Proton or Hadron Hard Parton Scattering
            val roll = random.nextDouble()
            if (roll < 0.15 && centerOfMassEnergyGeV >= 120.0) {
                processName = "Associated Higgs Boson Vector Boson Fusion (H⁰ → γγ / Z⁰Z⁰ → 4μ)"
                particleCountToGen = 12 + random.nextInt(12)
            } else if (roll < 0.40 && centerOfMassEnergyGeV >= 170.0) {
                processName = "Top-Quark Pair Production (t t̅ → W⁺b W⁻b̅ → Hadrons + Leptons)"
                particleCountToGen = 16 + random.nextInt(14)
            } else {
                processName = "Hard QCD Parton-Parton Jet Scattering (Di-jet / Multi-jet)"
                particleCountToGen = 18 + random.nextInt(22)
            }
        }

        decayTreeLines.add("│ Primary Process: $processName")
        decayTreeLines.add("├─ DAUGHTER SHOWER CASCADE:")

        // Energy allocation per daughter particle
        var remainingEnergy = centerOfMassEnergyGeV
        val momentumComponents = mutableListOf<Vector3D>()

        // Ensure Higgs or Z/W boson candidates if present
        if (processName.contains("Higgs")) {
            // Higgs boson decay into 2 photons or 4 muons
            val isGoldenFourMuon = random.nextBoolean()
            if (isGoldenFourMuon) {
                decayTreeLines.add("│  ├─ H⁰ (125.2 GeV) → Z⁰ Z⁰* → μ⁺ μ⁻ μ⁺ μ⁻ (Golden Channel)")
                val muonSpecies = listOf(StandardModelCatalog.MUON_MINUS, StandardModelCatalog.MUON_PLUS)
                for (i in 0..3) {
                    val sp = muonSpecies[i % 2]
                    val p = randomMomentum(random, 30f..60f)
                    val p3d = Particle3D(
                        id = "higgs_daughter_mu_$i",
                        species = sp,
                        position = Vector3D.ZERO,
                        momentum = p,
                        charge = sp.charge,
                        generation = 1,
                        colorOverride = Color(0xFF00E5FF)
                    )
                    generatedList.add(p3d)
                    momentumComponents.add(p)
                    decayTreeLines.add("│  │  └─ [μ%s] pT = %.1f GeV/c, q = %+.0f".format(if (sp.charge < 0) "⁻" else "⁺", p.transverseMagnitude(), sp.charge))
                }
            } else {
                decayTreeLines.add("│  ├─ H⁰ (125.2 GeV) → γ γ (Di-photon dip peak)")
                for (i in 0..1) {
                    val sp = StandardModelCatalog.PHOTON
                    val phi = i * PI.toFloat() + random.nextFloat() * 0.2f
                    val theta = PI.toFloat() / 2f + (random.nextFloat() - 0.5f) * 0.4f
                    val pMag = 62.6f
                    val p = Vector3D(pMag * sin(theta) * cos(phi), pMag * sin(theta) * sin(phi), pMag * cos(theta))
                    val p3d = Particle3D(
                        id = "higgs_photon_$i",
                        species = sp,
                        position = Vector3D.ZERO,
                        momentum = p,
                        charge = 0.0,
                        generation = 1,
                        colorOverride = Color(0xFFFFD600)
                    )
                    generatedList.add(p3d)
                    momentumComponents.add(p)
                    decayTreeLines.add("│  │  └─ [γ] E = %.1f GeV, Photon ECAL Cluster".format(pMag))
                }
            }
        }

        // Fill remaining shower particles from standard model catalog
        val availableSpecies = listOf(
            StandardModelCatalog.ELECTRON, StandardModelCatalog.POSITRON,
            StandardModelCatalog.MUON_MINUS, StandardModelCatalog.MUON_PLUS,
            StandardModelCatalog.PHOTON, StandardModelCatalog.GLUON,
            StandardModelCatalog.UP_QUARK, StandardModelCatalog.DOWN_QUARK,
            StandardModelCatalog.W_PLUS_BOSON, StandardModelCatalog.Z_BOSON,
            StandardModelCatalog.NEUTRON, StandardModelCatalog.NEUTRINO_ELECTRON
        )

        for (i in generatedList.size until particleCountToGen) {
            val species = availableSpecies[random.nextInt(availableSpecies.size)]
            val pMag = (1f + random.nextFloat() * (centerOfMassEnergyGeV.toFloat() / 6f)).coerceIn(0.5f, 500f)
            val p = randomMomentum(random, pMag..(pMag * 1.5f))

            val particle = Particle3D(
                id = "daughter_${i}_${species.id}",
                species = species,
                position = Vector3D.ZERO,
                momentum = p,
                charge = species.charge,
                generation = 1
            )
            generatedList.add(particle)
            momentumComponents.add(p)

            if (i < 12) {
                decayTreeLines.add("│  ├─ [${species.symbol}] ${species.name} | pT = %.2f GeV/c | q = %+.1f".format(p.transverseMagnitude(), species.charge))
            }
        }

        if (generatedList.size > 12) {
            decayTreeLines.add("│  └─ ... and ${generatedList.size - 12} additional fragment tracks/jets")
        }

        // Calculate invariant mass M = sqrt((sum E)^2 - (sum p)^2)
        var totalE = 0.0
        var totalPx = 0.0
        var totalPy = 0.0
        var totalPz = 0.0
        var totalPt = 0.0

        for (p in generatedList) {
            val energy = p.totalEnergyGeV().toDouble()
            totalE += energy
            totalPx += p.momentum.x.toDouble()
            totalPy += p.momentum.y.toDouble()
            totalPz += p.momentum.z.toDouble()
            totalPt += p.transverseMomentum().toDouble()
        }

        val pSq = totalPx * totalPx + totalPy * totalPy + totalPz * totalPz
        val invariantMass = if (totalE * totalE > pSq) sqrt(totalE * totalE - pSq) else totalE

        decayTreeLines.add("├─ SUMMARY & KINEMATICS:")
        decayTreeLines.add("│ Total Multiplicity: ${generatedList.size} particles")
        decayTreeLines.add("│ Invariant Mass M: %.2f GeV/c²".format(invariantMass))
        decayTreeLines.add("│ Total Transverse Energy ET: %.2f GeV".format(totalPt))
        decayTreeLines.add("└─ EVENT RECORD COMPLETED")

        return CollisionEventResult(
            eventId = eventId,
            initialBeamA = beamA,
            initialBeamB = beamB,
            centerOfMassEnergyGeV = centerOfMassEnergyGeV,
            impactParameterFm = impactParameterFm,
            generatedParticles = generatedList,
            multiplicity = generatedList.size,
            invariantMassGeV = invariantMass,
            totalTransverseEnergyGeV = totalPt,
            primaryProcessName = processName,
            decayTreeFormatted = decayTreeLines.joinToString("\n")
        )
    }

    private fun randomMomentum(random: Random, magRange: ClosedRange<Float>): Vector3D {
        val mag = magRange.start + random.nextFloat() * (magRange.endInclusive - magRange.start)
        val theta = random.nextFloat() * PI.toFloat()
        val phi = random.nextFloat() * 2f * PI.toFloat()

        val sinT = sin(theta)
        return Vector3D(
            mag * sinT * cos(phi),
            mag * sinT * sin(phi),
            mag * cos(theta)
        )
    }
}
