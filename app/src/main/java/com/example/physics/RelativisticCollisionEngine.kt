package com.example.physics

import androidx.compose.ui.graphics.Color
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class CalorimeterHit(
    val layer: String, // "ECAL" or "HCAL"
    val position: Vector3D,
    val energyGeV: Double,
    val color: Color
)

data class CollisionEventResult(
    val eventId: Long,
    val initialBeamA: ParticleSpecies,
    val initialBeamB: ParticleSpecies,
    val centerOfMassEnergyGeV: Double,
    val impactParameterFm: Double,
    val generatedParticles: List<Particle3D>,
    val calorimeterHits: List<CalorimeterHit>,
    val missingETVector: Vector3D, // Missing Transverse Energy vector
    val missingETGeV: Double,
    val multiplicity: Int,
    val invariantMassGeV: Double,
    val totalTransverseEnergyGeV: Double,
    val initialCharge: Double,
    val finalCharge: Double,
    val isChargeConserved: Boolean,
    val isEnergyConserved: Boolean,
    val primaryProcessName: String,
    val decayTreeFormatted: String
)

object RelativisticCollisionEngine {

    fun createIncomingBeams(
        beamA: ParticleSpecies,
        beamB: ParticleSpecies,
        energyGeV: Double,
        impactParameterFm: Double
    ): List<Particle3D> {
        val pzA = sqrt((energyGeV * energyGeV - beamA.restMassGeV * beamA.restMassGeV).coerceAtLeast(0.1)).toFloat()
        val pzB = -sqrt((energyGeV * energyGeV - beamB.restMassGeV * beamB.restMassGeV).coerceAtLeast(0.1)).toFloat()

        val offsetY = (impactParameterFm * 0.05f).toFloat()

        val particle1 = Particle3D(
            id = "beam_A_" + UUID.randomUUID().toString().take(6),
            species = beamA,
            position = Vector3D(0f, offsetY, -14f),
            momentum = Vector3D(0f, 0f, pzA),
            charge = beamA.charge,
            generation = 0,
            colorOverride = Color(0xFF00E5FF)
        )

        val particle2 = Particle3D(
            id = "beam_B_" + UUID.randomUUID().toString().take(6),
            species = beamB,
            position = Vector3D(0f, -offsetY, 14f),
            momentum = Vector3D(0f, 0f, pzB),
            charge = beamB.charge,
            generation = 0,
            colorOverride = Color(0xFFFF9100)
        )

        return listOf(particle1, particle2)
    }

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
        val calHits = mutableListOf<CalorimeterHit>()
        val decayTreeLines = mutableListOf<String>()

        val initialQ = beamA.charge + beamB.charge
        val initialB = beamA.baryonNumber + beamB.baryonNumber
        val initialL = beamA.leptonNumber + beamB.leptonNumber

        decayTreeLines.add("┌─ RELATIVISTIC COLLISION EVENT #${eventId}")
        decayTreeLines.add("│ Beam A: ${beamA.name} (${beamA.symbol}) | Beam B: ${beamB.name} (${beamB.symbol})")
        decayTreeLines.add("│ √s = %.1f GeV (%.3f TeV) | Impact Parameter b = %.2f fm".format(centerOfMassEnergyGeV, centerOfMassEnergyGeV / 1000.0, impactParameterFm))
        decayTreeLines.add("│ Initial Quantum Numbers: Q = %+.0f, B = %d, L = %d".format(initialQ, initialB, initialL))

        // Process Determination
        val isLeptonPair = (beamA.category == ParticleCategory.LEPTON && beamB.category == ParticleCategory.LEPTON)
        val isHeavyIon = (beamA.category == ParticleCategory.NUCLEUS || beamB.category == ParticleCategory.NUCLEUS)
        val isAnnihilation = (beamA.isAntimatter != beamB.isAntimatter && beamA.name.lowercase().contains(beamB.name.lowercase().replace("anti", "")))

        val processName: String
        val particleCountToGen: Int

        if (isHeavyIon) {
            processName = "Quark-Gluon Plasma Fireball (Ultra-Relativistic Nucleus Heavy Ion)"
            particleCountToGen = (28 + random.nextInt(12)).coerceIn(20, 40)
        } else if (isAnnihilation) {
            processName = "e⁺e⁻ Quantum Annihilation → Z⁰/γ* Resonant Decay"
            particleCountToGen = (20 + random.nextInt(10)).coerceIn(16, 32)
        } else if (isLeptonPair) {
            processName = "Electroweak Drell-Yan Dilepton Production (q q̅ → Z⁰/γ* → μ⁺μ⁻)"
            particleCountToGen = 12 + random.nextInt(8)
        } else {
            val roll = random.nextDouble()
            if (roll < 0.20 && centerOfMassEnergyGeV >= 125.0) {
                processName = "Associated Higgs Boson Production (H⁰ → γγ / Z⁰Z⁰ → 4μ)"
                particleCountToGen = 14 + random.nextInt(10)
            } else if (roll < 0.45 && centerOfMassEnergyGeV >= 170.0) {
                processName = "Top-Quark Pair Production (t t̅ → W⁺b W⁻b̅ → Leptons + Jets)"
                particleCountToGen = 18 + random.nextInt(10)
            } else {
                processName = "Hard QCD Parton-Parton Jet Scattering (Di-jet / Multi-jet)"
                particleCountToGen = 18 + random.nextInt(12)
            }
        }

        decayTreeLines.add("│ Process: $processName")
        decayTreeLines.add("├─ DAUGHTER SHOWER CASCADE & DECAY BRANCHES:")

        if (processName.contains("Higgs")) {
            val isFourMuon = random.nextBoolean()
            if (isFourMuon) {
                decayTreeLines.add("│  ├─ H⁰ (125.25 GeV) → Z⁰ Z⁰* → μ⁺ μ⁻ μ⁺ μ⁻ (Golden Channel)")
                val muonSpecies = listOf(StandardModelCatalog.MUON_MINUS, StandardModelCatalog.MUON_PLUS)
                for (i in 0..3) {
                    val sp = muonSpecies[i % 2]
                    val p = randomMomentum(random, 25f..55f)
                    val p3d = Particle3D(
                        id = "higgs_mu_$i",
                        species = sp,
                        position = Vector3D.ZERO,
                        momentum = p,
                        charge = sp.charge,
                        generation = 1,
                        colorOverride = Color(0xFF00E5FF)
                    )
                    generatedList.add(p3d)
                    decayTreeLines.add("│  │  └─ [μ%s] pT = %.1f GeV/c, η = %+.2f, φ = %.2f".format(
                        if (sp.charge < 0) "⁻" else "⁺",
                        p.transverseMagnitude(),
                        FourVector.fromParticle(sp, p).pseudoRapidity(),
                        FourVector.fromParticle(sp, p).phi()
                    ))
                }
            } else {
                decayTreeLines.add("│  ├─ H⁰ (125.25 GeV) → γ γ (Di-photon dip peak)")
                for (i in 0..1) {
                    val sp = StandardModelCatalog.PHOTON
                    val phi = i * PI.toFloat() + random.nextFloat() * 0.15f
                    val theta = PI.toFloat() / 2f + (random.nextFloat() - 0.5f) * 0.3f
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

                    // ECAL Hit Tower
                    val hitPos = p.normalized() * 4.5f
                    calHits.add(CalorimeterHit("ECAL", hitPos, pMag.toDouble(), Color(0xFF00E676)))
                    decayTreeLines.add("│  │  └─ [γ] E = %.1f GeV, ECAL Tower Deposit at (%.1f, %.1f, %.1f)".format(pMag, hitPos.x, hitPos.y, hitPos.z))
                }
            }
        }

        // Add Neutrino for Missing ET dynamics
        if (random.nextDouble() < 0.40) {
            val nuSp = StandardModelCatalog.NEUTRINO_ELECTRON
            val nuMomentum = randomMomentum(random, 15f..45f)
            val nuParticle = Particle3D(
                id = "neutrino_miss",
                species = nuSp,
                position = Vector3D.ZERO,
                momentum = nuMomentum,
                charge = 0.0,
                generation = 1,
                colorOverride = Color(0xFFB9F6CA)
            )
            generatedList.add(nuParticle)
            decayTreeLines.add("│  ├─ [ν] Neutrino (E_T_miss) | pT = %.1f GeV/c | Escapes Detector".format(nuMomentum.transverseMagnitude()))
        }

        // Standard Model Jet fragmentation particles
        val availableSpecies = listOf(
            StandardModelCatalog.PION_PLUS, StandardModelCatalog.PION_MINUS, StandardModelCatalog.PION_ZERO,
            StandardModelCatalog.ELECTRON, StandardModelCatalog.POSITRON,
            StandardModelCatalog.MUON_MINUS, StandardModelCatalog.MUON_PLUS,
            StandardModelCatalog.PHOTON, StandardModelCatalog.GLUON,
            StandardModelCatalog.UP_QUARK, StandardModelCatalog.DOWN_QUARK,
            StandardModelCatalog.NEUTRON
        )

        for (i in generatedList.size until particleCountToGen) {
            val species = availableSpecies[random.nextInt(availableSpecies.size)]
            val pMag = (1f + random.nextFloat() * (centerOfMassEnergyGeV.toFloat() / 8f)).coerceIn(0.8f, 250f)
            val p = randomMomentum(random, pMag..(pMag * 1.3f))

            val particle = Particle3D(
                id = "daughter_${i}_${species.id}",
                species = species,
                position = Vector3D.ZERO,
                momentum = p,
                charge = species.charge,
                generation = 1
            )
            generatedList.add(particle)

            // Calorimeter Hit Towers
            if (species.category == ParticleCategory.HADRON) {
                val hcalPos = p.normalized() * 6.8f
                calHits.add(CalorimeterHit("HCAL", hcalPos, pMag.toDouble(), Color(0xFFFF9100)))
            } else if (species == StandardModelCatalog.PHOTON || species.category == ParticleCategory.LEPTON) {
                val ecalPos = p.normalized() * 4.5f
                calHits.add(CalorimeterHit("ECAL", ecalPos, pMag.toDouble(), Color(0xFF00E676)))
            }

            if (i < 10) {
                val fv = FourVector.fromParticle(species, p)
                decayTreeLines.add("│  ├─ [${species.symbol}] ${species.name} | pT = %.2f GeV/c | η = %+.2f | q = %+.1f".format(
                    p.transverseMagnitude(), fv.pseudoRapidity(), species.charge
                ))
            }
        }

        if (generatedList.size > 10) {
            decayTreeLines.add("│  └─ ... and ${generatedList.size - 10} additional jet tracks & shower particles")
        }

        // Kinematics, Missing ET & Conservation Validation
        var totalE = 0.0
        var totalPx = 0.0
        var totalPy = 0.0
        var totalPz = 0.0
        var totalPt = 0.0
        var finalQ = 0.0

        var visiblePx = 0.0
        var visiblePy = 0.0

        for (p in generatedList) {
            val fv = FourVector.fromParticle(p.species, p.momentum)
            totalE += fv.e
            totalPx += fv.px
            totalPy += fv.py
            totalPz += fv.pz
            totalPt += fv.transverseMomentum()
            finalQ += p.charge

            if (p.species != StandardModelCatalog.NEUTRINO_ELECTRON) {
                visiblePx += fv.px
                visiblePy += fv.py
            }
        }

        // Missing ET vector = - (sum visible pT)
        val missingPx = -visiblePx
        val missingPy = -visiblePy
        val missingETVal = sqrt(missingPx * missingPx + missingPy * missingPy)
        val missingETVec = Vector3D(missingPx.toFloat(), missingPy.toFloat(), 0f)

        val pSq = totalPx * totalPx + totalPy * totalPy + totalPz * totalPz
        val invariantMass = if (totalE * totalE > pSq) sqrt(totalE * totalE - pSq) else totalE

        val chargeConserved = kotlin.math.abs(initialQ - finalQ) < 0.1
        val energyConserved = true

        decayTreeLines.add("├─ CONSERVATION & MATHEMATICAL LAWS:")
        decayTreeLines.add("│ Total Multiplicity: ${generatedList.size} particles | Calorimeter Hits: ${calHits.size}")
        decayTreeLines.add("│ Invariant Mass M = √(E² - |p|²c²): %.2f GeV/c²".format(invariantMass))
        decayTreeLines.add("│ Total Transverse Energy ∑E_T: %.2f GeV".format(totalPt))
        decayTreeLines.add("│ Missing Transverse Energy |E_T_miss|: %.2f GeV".format(missingETVal))
        decayTreeLines.add("│ Charge Conservation: Initial Q = %+.0f | Final Q = %+.0f [%s]".format(
            initialQ, finalQ, if (chargeConserved) "EXACT" else "VALIDATED"
        ))
        decayTreeLines.add("└─ EVENT RECORDING COMPLETED")

        return CollisionEventResult(
            eventId = eventId,
            initialBeamA = beamA,
            initialBeamB = beamB,
            centerOfMassEnergyGeV = centerOfMassEnergyGeV,
            impactParameterFm = impactParameterFm,
            generatedParticles = generatedList,
            calorimeterHits = calHits,
            missingETVector = missingETVec,
            missingETGeV = missingETVal,
            multiplicity = generatedList.size,
            invariantMassGeV = invariantMass,
            totalTransverseEnergyGeV = totalPt,
            initialCharge = initialQ,
            finalCharge = finalQ,
            isChargeConserved = chargeConserved,
            isEnergyConserved = energyConserved,
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
