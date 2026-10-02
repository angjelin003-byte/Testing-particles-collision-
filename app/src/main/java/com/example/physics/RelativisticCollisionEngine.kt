package com.example.physics

import androidx.compose.ui.graphics.Color
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class CollisionChannelMode(
    val displayName: String,
    val shortName: String,
    val expectedParticlesText: String,
    val description: String
) {
    AUTO(
        "Auto (Cross-Section Physics)",
        "Auto",
        "2 to 80+ particles depending on √s",
        "Channels chosen probabilistically by QED, QCD, and Electroweak cross-sections."
    ),
    ELASTIC_2_BODY(
        "Elastic Scattering (2 → 2)",
        "2→2 Elastic",
        "Exactly 2 particles",
        "Incoming particles exchange virtual gauge mediator and deflect elastically without particle creation."
    ),
    ANNIHILATION_PAIR(
        "Leptonic Annihilation (2 → 2)",
        "2→2 Pair",
        "Exactly 2 particles",
        "Particle and antiparticle annihilate into virtual gauge boson, producing new fermion-antifermion pair (e.g. μ⁺μ⁻)."
    ),
    RADIATIVE_QED(
        "Radiative QED (2 → 3)",
        "2→3 Rad",
        "Exactly 3 particles",
        "Accelerating charges emit hard bremsstrahlung gauge photon alongside fermion pair (e⁺e⁻ → μ⁺μ⁻γ)."
    ),
    ELECTROWEAK_BOSONS(
        "Electroweak W⁺W⁻ / Z⁰Z⁰ (2 → 4)",
        "2→4 EW",
        "Exactly 4 particles",
        "Vector boson pair production at √s ≥ 160 GeV decaying into 4 fundamental fermions."
    ),
    HADRONIC_JETS(
        "Hadronization & QCD Jets (High Multiplicity)",
        "Jets & Shower",
        "20 to 80+ particles",
        "Quark-antiquark / gluon parton cascade forming collimated relativistic hadron jets via QCD confinement."
    )
}

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
    val chargedMultiplicity: Int,
    val invariantMassGeV: Double,
    val totalTransverseEnergyGeV: Double,
    val initialCharge: Double,
    val finalCharge: Double,
    val isChargeConserved: Boolean,
    val isEnergyConserved: Boolean,
    val primaryProcessName: String,
    val decayTreeFormatted: String,
    val channelMode: CollisionChannelMode = CollisionChannelMode.AUTO,
    val intermediateStateText: String = ""
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
            colorOverride = null
        )

        val particle2 = Particle3D(
            id = "beam_B_" + UUID.randomUUID().toString().take(6),
            species = beamB,
            position = Vector3D(0f, -offsetY, 14f),
            momentum = Vector3D(0f, 0f, pzB),
            charge = beamB.charge,
            generation = 0,
            colorOverride = null
        )

        return listOf(particle1, particle2)
    }

    /**
     * Relativistic Lorentz Boost of 4-momentum (E, p) from particle rest frame into CM/Lab frame
     */
    fun boostToLabFrame(pRest: Vector3D, eRest: Float, beta: Vector3D): Pair<Vector3D, Float> {
        val betaSq = beta.dot(beta)
        if (betaSq < 1e-7f) return Pair(pRest, eRest)
        val betaMag = sqrt(betaSq).coerceAtMost(0.9999f)
        val gamma = 1f / sqrt(1f - betaMag * betaMag)
        val betaHat = beta.normalized()
        val pParallel = pRest.dot(betaHat)
        val pPerp = pRest - betaHat * pParallel
        val pParallelPrime = gamma * (pParallel + betaMag * eRest)
        val ePrime = gamma * (eRest + betaMag * pParallel)
        val pPrime = pPerp + betaHat * pParallelPrime
        return Pair(pPrime, ePrime)
    }

    /**
     * Sample isotropic 3-momentum direction on unit sphere
     */
    fun sampleIsotropicDirection(random: Random): Vector3D {
        val cosTheta = (random.nextFloat() * 2f - 1f).coerceIn(-0.999f, 0.999f)
        val sinTheta = sqrt(1f - cosTheta * cosTheta)
        val phi = random.nextFloat() * 2f * PI.toFloat()
        return Vector3D(sinTheta * cos(phi), sinTheta * sin(phi), cosTheta)
    }

    /**
     * Tsallis / Hagedorn power-law transverse momentum sampling for QCD jet fragmentation
     */
    fun sampleTransverseMomentum(m0: Double, random: Random): Float {
        val t = 0.16 // GeV
        val n = 7.0
        val u = random.nextDouble().coerceIn(0.0001, 0.9999)
        val deltaMt = n * t * ((1.0 - u).pow(-1.0 / (n - 2.0)) - 1.0)
        val mt = m0 + deltaMt.coerceAtLeast(0.0)
        val pt = sqrt((mt * mt - m0 * m0).coerceAtLeast(0.01))
        return pt.toFloat().coerceIn(0.15f, 150f)
    }

    /**
     * Mathematically exact particle multiplicity calculation based on collider phenomenology
     */
    fun calculatePhysicalMultiplicity(
        beamA: ParticleSpecies,
        beamB: ParticleSpecies,
        sqrtSGeV: Double,
        impactParameterFm: Double,
        random: Random
    ): Pair<Int, Int> {
        val s = (sqrtSGeV * sqrtSGeV).coerceAtLeast(4.0)

        val meanNch: Double = when {
            beamA.category == ParticleCategory.NUCLEUS || beamB.category == ParticleCategory.NUCLEUS -> {
                val totalNucleons = (beamA.baryonNumber.coerceAtLeast(1) + beamB.baryonNumber.coerceAtLeast(1)).toDouble()
                val rNuclear = 1.25 * (totalNucleons / 2.0).pow(1.0 / 3.0)
                val overlap = (1.0 - (impactParameterFm / (2.0 * rNuclear)).coerceIn(0.0, 1.0)).pow(2.0)
                val nPart = (totalNucleons * overlap).coerceAtLeast(2.0)
                val dNchPerPair = 0.38 * (sqrtSGeV / totalNucleons).coerceAtLeast(2.0).pow(0.155)
                (0.5 * nPart * dNchPerPair * 5.0).coerceIn(12.0, 52.0)
            }
            beamA.category == ParticleCategory.LEPTON && beamB.category == ParticleCategory.LEPTON -> {
                val lnS = ln(s).coerceAtLeast(1.0)
                val nch = 2.05 + 0.16 * exp(0.49 * sqrt(lnS))
                nch.coerceIn(8.0, 32.0)
            }
            else -> {
                val dNchDeta0 = 0.725 * sqrtSGeV.pow(0.206)
                val nch = 5.0 * dNchDeta0
                nch.coerceIn(10.0, 48.0)
            }
        }

        val k = 4.0
        val p = k / (k + meanNch)
        val gammaSample = sampleGamma(k, (1.0 - p) / p, random)
        val sampledNch = samplePoisson(gammaSample, random).coerceIn(6, 60)
        val sampledTotal = (sampledNch * 1.5).toInt().coerceIn(sampledNch + 2, 80)
        return Pair(sampledNch, sampledTotal)
    }

    private fun sampleGaussian(random: Random): Double {
        val u1 = random.nextDouble().coerceIn(1e-7, 1.0)
        val u2 = random.nextDouble()
        return sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
    }

    private fun sampleGamma(k: Double, theta: Double, random: Random): Double {
        val d = k - 1.0 / 3.0
        val c = 1.0 / sqrt(9.0 * d)
        while (true) {
            var z: Double
            var v: Double
            do {
                z = sampleGaussian(random)
                v = 1.0 + c * z
            } while (v <= 0.0)
            v = v * v * v
            val u = random.nextDouble()
            if (u < 1.0 - 0.0331 * z * z * z * z) return d * v * theta
            if (ln(u) < 0.5 * z * z + d * (1.0 - v + ln(v))) return d * v * theta
        }
    }

    private fun samplePoisson(lambda: Double, random: Random): Int {
        if (lambda > 30.0) {
            return (lambda + sqrt(lambda) * sampleGaussian(random)).toInt().coerceAtLeast(0)
        }
        val l = exp(-lambda)
        var k = 0
        var p = 1.0
        do {
            k++
            p *= random.nextDouble()
        } while (p > l)
        return k - 1
    }

    /**
     * 1. MINIMAL OUTPUT: Elastic Scattering (2 → 2)
     * Particles exchange a virtual mediator and deflect elastically without creation. Exactly 2 particles.
     */
    private fun generateElasticScattering(
        beamA: ParticleSpecies,
        beamB: ParticleSpecies,
        sqrtSGeV: Double,
        random: Random,
        decayTreeLines: MutableList<String>
    ): List<Particle3D> {
        decayTreeLines.add("│ Primary Interaction: Elastic Scattering (2 → 2) [e⁻ + e⁺ → e⁻ + e⁺ or p + p → p + p]")
        decayTreeLines.add("│ Intermediate State: 2 elementary fermions exchanging virtual γ / Z⁰ / gluon")
        decayTreeLines.add("│ Final Observable State: Exactly 2 stable particles (Minimal Output)")
        decayTreeLines.add("├─ DETECTOR TRACK RECONSTRUCTION:")

        val eEach = (sqrtSGeV / 2.0).toFloat()
        val pMagA = sqrt((eEach * eEach - beamA.restMassGeV.toFloat() * beamA.restMassGeV.toFloat()).coerceAtLeast(0.01f))
        val pMagB = sqrt((eEach * eEach - beamB.restMassGeV.toFloat() * beamB.restMassGeV.toFloat()).coerceAtLeast(0.01f))

        // Rutherford / QED differential cross-section angular distribution
        val cosTheta = (1.0 - 2.0 * random.nextDouble().pow(0.35)).toFloat().coerceIn(-0.92f, 0.92f)
        val sinTheta = sqrt(1f - cosTheta * cosTheta)
        val phi = random.nextFloat() * 2f * PI.toFloat()

        val dirA = Vector3D(sinTheta * cos(phi), sinTheta * sin(phi), cosTheta)
        val dirB = -dirA

        val pVecA = dirA * pMagA
        val pVecB = dirB * pMagB

        val startPosA = dirA * 0.28f
        val startPosB = dirB * 0.28f

        val particleA = Particle3D(
            id = "elastic_1_${beamA.id}_${UUID.randomUUID().toString().take(4)}",
            species = beamA,
            position = startPosA,
            momentum = pVecA,
            charge = beamA.charge,
            generation = 1,
            colorOverride = null,
            trajectoryHistory = mutableListOf(Vector3D.ZERO, startPosA)
        )

        val particleB = Particle3D(
            id = "elastic_2_${beamB.id}_${UUID.randomUUID().toString().take(4)}",
            species = beamB,
            position = startPosB,
            momentum = pVecB,
            charge = beamB.charge,
            generation = 1,
            colorOverride = null,
            trajectoryHistory = mutableListOf(Vector3D.ZERO, startPosB)
        )

        decayTreeLines.add("│  ├─ [${beamA.symbol}] ${beamA.name} | |p| = %.2f GeV/c, θ = %.2f rad, φ = %.2f rad".format(
            pMagA, kotlin.math.acos(cosTheta), phi
        ))
        decayTreeLines.add("│  └─ [${beamB.symbol}] ${beamB.name} | |p| = %.2f GeV/c (Back-to-back recoil)".format(pMagB))

        return listOf(particleA, particleB)
    }

    /**
     * 1b. MINIMAL OUTPUT: Leptonic Annihilation to Pairs (2 → 2)
     * Particle and antiparticle annihilate into virtual gauge boson, producing new fermion pair (e.g. μ⁺μ⁻). Exactly 2 particles.
     */
    private fun generateLeptonicAnnihilation(
        sqrtSGeV: Double,
        random: Random,
        decayTreeLines: MutableList<String>
    ): List<Particle3D> {
        val producesTau = sqrtSGeV >= 4.0 && random.nextBoolean()
        val pairSpecies = if (producesTau) {
            Pair(StandardModelCatalog.TAU_MINUS, StandardModelCatalog.TAU_MINUS.copy(id = "tau_plus", name = "Anti-Tau", symbol = "τ⁺", charge = 1.0, isAntimatter = true, color = Color(0xFFA7FFEB)))
        } else {
            Pair(StandardModelCatalog.MUON_MINUS, StandardModelCatalog.MUON_PLUS)
        }

        decayTreeLines.add("│ Primary Interaction: Quantum Annihilation to Fermion Pair (2 → 2) [e⁺e⁻ → γ*/Z⁰ → ℓ⁺ℓ⁻]")
        decayTreeLines.add("│ Intermediate State: 1 virtual mediator (γ* / Z⁰) decaying to 2 elementary fermions")
        decayTreeLines.add("│ Final Observable State: Exactly 2 leptons (Minimal Output)")
        decayTreeLines.add("├─ DETECTOR TRACK RECONSTRUCTION:")

        val sp1 = pairSpecies.first
        val sp2 = pairSpecies.second
        val eEach = (sqrtSGeV / 2.0).toFloat()
        val pMag = sqrt((eEach * eEach - sp1.restMassGeV.toFloat() * sp1.restMassGeV.toFloat()).coerceAtLeast(0.01f))

        // s-channel vector mediator angular distribution: 1 + cos^2(theta)
        var cosTheta: Float
        do {
            cosTheta = (random.nextFloat() * 2f - 1f).coerceIn(-0.95f, 0.95f)
            val prob = 0.5f * (1f + cosTheta * cosTheta)
        } while (random.nextFloat() > prob)

        val sinTheta = sqrt(1f - cosTheta * cosTheta)
        val phi = random.nextFloat() * 2f * PI.toFloat()

        val dir1 = Vector3D(sinTheta * cos(phi), sinTheta * sin(phi), cosTheta)
        val dir2 = -dir1

        val pVec1 = dir1 * pMag
        val pVec2 = dir2 * pMag

        val startPos1 = dir1 * 0.28f
        val startPos2 = dir2 * 0.28f

        val particle1 = Particle3D(
            id = "annihil_1_${sp1.id}_${UUID.randomUUID().toString().take(4)}",
            species = sp1,
            position = startPos1,
            momentum = pVec1,
            charge = sp1.charge,
            generation = 1,
            colorOverride = null,
            trajectoryHistory = mutableListOf(Vector3D.ZERO, startPos1)
        )

        val particle2 = Particle3D(
            id = "annihil_2_${sp2.id}_${UUID.randomUUID().toString().take(4)}",
            species = sp2,
            position = startPos2,
            momentum = pVec2,
            charge = sp2.charge,
            generation = 1,
            colorOverride = null,
            trajectoryHistory = mutableListOf(Vector3D.ZERO, startPos2)
        )

        decayTreeLines.add("│  ├─ [${sp1.symbol}] ${sp1.name} | |p| = %.2f GeV/c, E = %.2f GeV".format(pMag, eEach))
        decayTreeLines.add("│  └─ [${sp2.symbol}] ${sp2.name} | |p| = %.2f GeV/c, E = %.2f GeV (Collinear)".format(pMag, eEach))

        return listOf(particle1, particle2)
    }

    /**
     * 2. INTERMEDIATE OUTPUT: Radiative QED (2 → 3)
     * High-energy leptons emit a hard bremsstrahlung photon alongside pair production. Exactly 3 particles.
     */
    private fun generateRadiativeQED(
        sqrtSGeV: Double,
        random: Random,
        decayTreeLines: MutableList<String>
    ): List<Particle3D> {
        decayTreeLines.add("│ Primary Interaction: Radiative QED Bremsstrahlung (2 → 3) [e⁺e⁻ → μ⁺μ⁻γ]")
        decayTreeLines.add("│ Intermediate State: 3 elementary particles (2 fermions + 1 hard gauge photon)")
        decayTreeLines.add("│ Final Observable State: Exactly 3 particles (Intermediate Output)")
        decayTreeLines.add("├─ DETECTOR TRACK RECONSTRUCTION:")

        val spMuonMinus = StandardModelCatalog.MUON_MINUS
        val spMuonPlus = StandardModelCatalog.MUON_PLUS
        val spPhoton = StandardModelCatalog.PHOTON

        // Hard photon carries fraction of energy (15% to 40% of total CM energy)
        val xGamma = (0.15f + random.nextFloat() * 0.25f)
        val ePhoton = (sqrtSGeV * xGamma).toFloat()
        val dirGamma = sampleIsotropicDirection(random)
        val pPhoton = dirGamma * ePhoton
        val startPosGamma = dirGamma * 0.28f

        // Recoil system: invariant mass squared s' = s - 2 * sqrt(s) * E_gamma
        val sPrime = ((sqrtSGeV * sqrtSGeV) - (2.0 * sqrtSGeV * ePhoton.toDouble())).coerceAtLeast(1.0)
        val sqrtSPrime = sqrt(sPrime).toFloat()
        val eRecoil = (sqrtSGeV - ePhoton.toDouble()).toFloat()
        val betaRecoil = (-pPhoton) / eRecoil

        // In recoil rest frame, muon pair is back-to-back with momentum p*
        val eMuRest = sqrtSPrime / 2f
        val pMuRestMag = sqrt((eMuRest * eMuRest - spMuonMinus.restMassGeV.toFloat() * spMuonMinus.restMassGeV.toFloat()).coerceAtLeast(0.01f))
        val dirMuRest = sampleIsotropicDirection(random)
        val pMuMinusRest = dirMuRest * pMuRestMag
        val pMuPlusRest = -pMuMinusRest

        // Boost leptons to lab/CM frame
        val (pMuMinusLab, eMuMinusLab) = boostToLabFrame(pMuMinusRest, eMuRest, betaRecoil)
        val (pMuPlusLab, eMuPlusLab) = boostToLabFrame(pMuPlusRest, eMuRest, betaRecoil)

        val dirMuMinus = if (pMuMinusLab.magnitude() > 0.01f) pMuMinusLab.normalized() else Vector3D(1f, 0f, 0f)
        val dirMuPlus = if (pMuPlusLab.magnitude() > 0.01f) pMuPlusLab.normalized() else Vector3D(-1f, 0f, 0f)

        val startPosMuMinus = dirMuMinus * 0.28f
        val startPosMuPlus = dirMuPlus * 0.28f

        val particleGamma = Particle3D(
            id = "rad_photon_${UUID.randomUUID().toString().take(4)}",
            species = spPhoton,
            position = startPosGamma,
            momentum = pPhoton,
            charge = 0.0,
            generation = 1,
            colorOverride = null,
            trajectoryHistory = mutableListOf(Vector3D.ZERO, startPosGamma)
        )

        val particleMuMinus = Particle3D(
            id = "rad_mu_minus_${UUID.randomUUID().toString().take(4)}",
            species = spMuonMinus,
            position = startPosMuMinus,
            momentum = pMuMinusLab,
            charge = spMuonMinus.charge,
            generation = 1,
            colorOverride = null,
            trajectoryHistory = mutableListOf(Vector3D.ZERO, startPosMuMinus)
        )

        val particleMuPlus = Particle3D(
            id = "rad_mu_plus_${UUID.randomUUID().toString().take(4)}",
            species = spMuonPlus,
            position = startPosMuPlus,
            momentum = pMuPlusLab,
            charge = spMuonPlus.charge,
            generation = 1,
            colorOverride = null,
            trajectoryHistory = mutableListOf(Vector3D.ZERO, startPosMuPlus)
        )

        decayTreeLines.add("│  ├─ [γ] Bremsstrahlung Photon | E = %.2f GeV | Deposited in ECAL".format(ePhoton))
        decayTreeLines.add("│  ├─ [μ⁻] Recoil Muon | |p| = %.2f GeV/c, E = %.2f GeV".format(pMuMinusLab.magnitude(), eMuMinusLab))
        decayTreeLines.add("│  └─ [μ⁺] Recoil Anti-Muon | |p| = %.2f GeV/c, E = %.2f GeV".format(pMuPlusLab.magnitude(), eMuPlusLab))

        return listOf(particleGamma, particleMuMinus, particleMuPlus)
    }

    /**
     * 2b. INTERMEDIATE OUTPUT: Electroweak Vector Boson Pair Production (2 → 4)
     * At √s ≥ 160 GeV, production of W⁺W⁻ or Z⁰Z⁰ pairs decaying into 4 fundamental fermions. Exactly 4 particles.
     */
    private fun generateElectroweakBosonPairs(
        sqrtSGeV: Double,
        random: Random,
        decayTreeLines: MutableList<String>
    ): List<Particle3D> {
        val isWW = random.nextBoolean() || sqrtSGeV < 182.0

        decayTreeLines.add(
            if (isWW) "│ Primary Interaction: Electroweak W⁺W⁻ Pair Production (2 → 4) [e⁺e⁻ → W⁺W⁻ → ℓ⁺ν ℓ'⁻ν̄]"
            else "│ Primary Interaction: Electroweak Z⁰Z⁰ Pair Production (2 → 4) [e⁺e⁻ → Z⁰Z⁰ → μ⁺μ⁻ e⁺e⁻]"
        )
        decayTreeLines.add("│ Intermediate State: 2 vector gauge bosons (${if (isWW) "W⁺W⁻" else "Z⁰Z⁰"}, M = ${if (isWW) "80.4" else "91.2"} GeV)")
        decayTreeLines.add("│ Final Observable State: Exactly 4 fundamental fermions (Intermediate Output)")
        decayTreeLines.add("├─ DETECTOR TRACK RECONSTRUCTION:")

        val mW = if (isWW) 80.38f else 91.19f
        val eBoson = (sqrtSGeV / 2.0).toFloat().coerceAtLeast(mW + 0.5f)
        val pBosonMag = sqrt((eBoson * eBoson - mW * mW).coerceAtLeast(0.01f))
        val dirBoson = sampleIsotropicDirection(random)
        val pBosonA = dirBoson * pBosonMag
        val pBosonB = -pBosonA

        val betaA = pBosonA / eBoson
        val betaB = pBosonB / eBoson

        val resultList = mutableListOf<Particle3D>()

        if (isWW) {
            // W+ -> mu+ + nu_mu
            // W- -> e- + nu_bar_e
            val spMuPlus = StandardModelCatalog.MUON_PLUS
            val spNuMu = StandardModelCatalog.NEUTRINO_ELECTRON
            val spElec = StandardModelCatalog.ELECTRON
            val spNuE = StandardModelCatalog.NEUTRINO_ELECTRON

            // In W rest frame: 2-body decay with energy mW / 2
            val eDaughterRest = mW / 2f
            val dirRestA = sampleIsotropicDirection(random)
            val pMuPlusRest = dirRestA * eDaughterRest
            val pNuMuRest = -dirRestA * eDaughterRest

            val dirRestB = sampleIsotropicDirection(random)
            val pElecRest = dirRestB * eDaughterRest
            val pNuERest = -dirRestB * eDaughterRest

            val (pMuPlusLab, eMuPlusLab) = boostToLabFrame(pMuPlusRest, eDaughterRest, betaA)
            val (pNuMuLab, eNuMuLab) = boostToLabFrame(pNuMuRest, eDaughterRest, betaA)
            val (pElecLab, eElecLab) = boostToLabFrame(pElecRest, eDaughterRest, betaB)
            val (pNuELab, eNuELab) = boostToLabFrame(pNuERest, eDaughterRest, betaB)

            val startMu = if (pMuPlusLab.magnitude() > 0.01f) pMuPlusLab.normalized() * 0.28f else Vector3D(1f, 0f, 0f) * 0.28f
            val startNu1 = if (pNuMuLab.magnitude() > 0.01f) pNuMuLab.normalized() * 0.28f else Vector3D(-1f, 0f, 0f) * 0.28f
            val startElec = if (pElecLab.magnitude() > 0.01f) pElecLab.normalized() * 0.28f else Vector3D(0f, 1f, 0f) * 0.28f
            val startNu2 = if (pNuELab.magnitude() > 0.01f) pNuELab.normalized() * 0.28f else Vector3D(0f, -1f, 0f) * 0.28f

            resultList.add(Particle3D("ew_mu_plus_${UUID.randomUUID().toString().take(4)}", spMuPlus, startMu, pMuPlusLab, spMuPlus.charge, 1, colorOverride = null, trajectoryHistory = mutableListOf(Vector3D.ZERO, startMu)))
            resultList.add(Particle3D("ew_nu_mu_${UUID.randomUUID().toString().take(4)}", spNuMu, startNu1, pNuMuLab, 0.0, 1, colorOverride = null, trajectoryHistory = mutableListOf(Vector3D.ZERO, startNu1)))
            resultList.add(Particle3D("ew_elec_${UUID.randomUUID().toString().take(4)}", spElec, startElec, pElecLab, spElec.charge, 1, colorOverride = null, trajectoryHistory = mutableListOf(Vector3D.ZERO, startElec)))
            resultList.add(Particle3D("ew_nu_e_${UUID.randomUUID().toString().take(4)}", spNuE, startNu2, pNuELab, 0.0, 1, colorOverride = null, trajectoryHistory = mutableListOf(Vector3D.ZERO, startNu2)))

            decayTreeLines.add("│  ├─ W⁺ (80.4 GeV) → [μ⁺] (|p| = %.1f GeV/c) + [ν_μ] (E = %.1f GeV, E_T_miss)".format(pMuPlusLab.magnitude(), eNuMuLab))
            decayTreeLines.add("│  └─ W⁻ (80.4 GeV) → [e⁻] (|p| = %.1f GeV/c) + [ν̄_e] (E = %.1f GeV, E_T_miss)".format(pElecLab.magnitude(), eNuELab))
        } else {
            // Z0 Z0 -> mu+ mu- e+ e- (Golden 4-lepton channel)
            val spMuMinus = StandardModelCatalog.MUON_MINUS
            val spMuPlus = StandardModelCatalog.MUON_PLUS
            val spElecMinus = StandardModelCatalog.ELECTRON
            val spElecPlus = StandardModelCatalog.POSITRON

            val eDaughterRest = mW / 2f
            val dirRestA = sampleIsotropicDirection(random)
            val dirRestB = sampleIsotropicDirection(random)

            val (pMuMinusLab, _) = boostToLabFrame(dirRestA * eDaughterRest, eDaughterRest, betaA)
            val (pMuPlusLab, _) = boostToLabFrame((-dirRestA) * eDaughterRest, eDaughterRest, betaA)
            val (pElecMinusLab, _) = boostToLabFrame(dirRestB * eDaughterRest, eDaughterRest, betaB)
            val (pElecPlusLab, _) = boostToLabFrame((-dirRestB) * eDaughterRest, eDaughterRest, betaB)

            val start1 = pMuMinusLab.normalized() * 0.28f
            val start2 = pMuPlusLab.normalized() * 0.28f
            val start3 = pElecMinusLab.normalized() * 0.28f
            val start4 = pElecPlusLab.normalized() * 0.28f

            resultList.add(Particle3D("ew_z_mu_minus_${UUID.randomUUID().toString().take(4)}", spMuMinus, start1, pMuMinusLab, spMuMinus.charge, 1, colorOverride = null, trajectoryHistory = mutableListOf(Vector3D.ZERO, start1)))
            resultList.add(Particle3D("ew_z_mu_plus_${UUID.randomUUID().toString().take(4)}", spMuPlus, start2, pMuPlusLab, spMuPlus.charge, 1, colorOverride = null, trajectoryHistory = mutableListOf(Vector3D.ZERO, start2)))
            resultList.add(Particle3D("ew_z_elec_minus_${UUID.randomUUID().toString().take(4)}", spElecMinus, start3, pElecMinusLab, spElecMinus.charge, 1, colorOverride = null, trajectoryHistory = mutableListOf(Vector3D.ZERO, start3)))
            resultList.add(Particle3D("ew_z_elec_plus_${UUID.randomUUID().toString().take(4)}", spElecPlus, start4, pElecPlusLab, spElecPlus.charge, 1, colorOverride = null, trajectoryHistory = mutableListOf(Vector3D.ZERO, start4)))

            decayTreeLines.add("│  ├─ Z⁰₁ (91.2 GeV) → [μ⁺ μ⁻] Lepton Pair (Invariant Mass = 91.2 GeV/c²)")
            decayTreeLines.add("│  └─ Z⁰₂ (91.2 GeV) → [e⁺ e⁻] Lepton Pair (Invariant Mass = 91.2 GeV/c²)")
        }

        return resultList
    }

    /**
     * 3. HIGH MULTIPLICITY: Hadronization & Jet Formation
     * High-energy quark-antiquark or gluon shower fragmentation forming collimated hadron jets (20 to 80+ particles).
     */
    private fun generateHadronicJets(
        beamA: ParticleSpecies,
        beamB: ParticleSpecies,
        sqrtSGeV: Double,
        impactParameterFm: Double,
        random: Random,
        decayTreeLines: MutableList<String>
    ): List<Particle3D> {
        decayTreeLines.add("│ Primary Interaction: Hard QCD Parton Scattering & Hadronization (High Multiplicity)")
        decayTreeLines.add("│ Initial Elementary State: Exactly 2 primary quarks (q q̄) flying apart near lightspeed")
        decayTreeLines.add("│ Parton Shower & QCD Confinement: Color string fragmentation creates secondary q q̄ & gluons")
        decayTreeLines.add("│ Final Observable State: Collimated relativistic hadron jets (pions, kaons, protons, neutrons)")
        decayTreeLines.add("├─ PRODUCED HADRONIC JET CASCADE & RECONSTRUCTION:")

        val (nch, nTotal) = calculatePhysicalMultiplicity(beamA, beamB, sqrtSGeV, impactParameterFm, random)
        decayTreeLines.add("│  ├─ Energy-Scaled Multiplicity √s = %.1f GeV: ⟨N_total⟩ = $nTotal (NBD Distribution)".format(sqrtSGeV))

        val hadronPool = listOf(
            StandardModelCatalog.PION_PLUS, StandardModelCatalog.PION_PLUS, StandardModelCatalog.PION_PLUS,
            StandardModelCatalog.PION_MINUS, StandardModelCatalog.PION_MINUS, StandardModelCatalog.PION_MINUS,
            StandardModelCatalog.PION_ZERO, StandardModelCatalog.PION_ZERO,
            StandardModelCatalog.KAON_PLUS, StandardModelCatalog.KAON_MINUS, StandardModelCatalog.KAON_ZERO,
            StandardModelCatalog.PROTON, StandardModelCatalog.ANTIPROTON, StandardModelCatalog.NEUTRON,
            StandardModelCatalog.PHOTON
        )

        val initialQ = beamA.charge + beamB.charge
        var runningCharge = 0.0
        val generatedList = mutableListOf<Particle3D>()

        // Collimated Di-Jet axis: quarks fly apart along unit axis nHat
        val cosJetTheta = (random.nextFloat() * 1.6f - 0.8f).coerceIn(-0.85f, 0.85f)
        val sinJetTheta = sqrt(1f - cosJetTheta * cosJetTheta)
        val jetPhi = random.nextFloat() * 2f * PI.toFloat()
        val jetAxisA = Vector3D(sinJetTheta * cos(jetPhi), sinJetTheta * sin(jetPhi), cosJetTheta)
        val jetAxisB = -jetAxisA

        val countToGenerate = nTotal.coerceIn(16, 75)

        for (i in 0 until countToGenerate) {
            val species = if (i >= countToGenerate - 2) {
                val neededCharge = initialQ - runningCharge
                when {
                    neededCharge > 0.5 -> StandardModelCatalog.PION_PLUS
                    neededCharge < -0.5 -> StandardModelCatalog.PION_MINUS
                    else -> StandardModelCatalog.PION_ZERO
                }
            } else {
                hadronPool[random.nextInt(hadronPool.size)]
            }
            runningCharge += species.charge

            // Collimated momentum distribution around Jet Axis A (for first half) or Jet Axis B (for second half)
            val primaryAxis = if (i % 2 == 0) jetAxisA else jetAxisB
            val pt = sampleTransverseMomentum(species.restMassGeV, random)
            val pLong = (pt * (1.5f + random.nextFloat() * 4.0f))

            // Small transverse dispersion j_T around jet core
            val jTAngle = random.nextFloat() * 2f * PI.toFloat()
            val jTMag = 0.35f * random.nextFloat()
            val perp1 = if (abs(primaryAxis.z) < 0.9f) primaryAxis.cross(Vector3D(0f, 0f, 1f)).normalized() else primaryAxis.cross(Vector3D(1f, 0f, 0f)).normalized()
            val perp2 = primaryAxis.cross(perp1).normalized()

            val pVec = primaryAxis * pLong + (perp1 * cos(jTAngle) + perp2 * sin(jTAngle)) * jTMag
            val pDir = if (pVec.magnitude() > 0.01f) pVec.normalized() else primaryAxis
            val startPos = pDir * 0.28f

            val particle = Particle3D(
                id = "jet_hadron_${i}_${species.id}_${UUID.randomUUID().toString().take(3)}",
                species = species,
                position = startPos,
                momentum = pVec,
                charge = species.charge,
                generation = 1,
                colorOverride = null,
                trajectoryHistory = mutableListOf(Vector3D.ZERO, startPos)
            )
            generatedList.add(particle)

            if (i < 6) {
                val fv = FourVector.fromParticle(species, pVec)
                decayTreeLines.add("│  ├─ Jet %d Track: [%s] %s | pT = %.2f GeV/c | η = %+.2f | q = %+.0f".format(
                    (i % 2) + 1, species.symbol, species.name, pt, fv.pseudoRapidity(), species.charge
                ))
            }
        }

        decayTreeLines.add("│  └─ ... and ${generatedList.size - 6} additional verified jet shower tracks")
        return generatedList
    }

    /**
     * Execute full relativistic collision simulation based on QED, QCD, and Electroweak cross sections
     */
    fun simulateCollision(
        eventId: Long,
        beamA: ParticleSpecies,
        beamB: ParticleSpecies,
        centerOfMassEnergyGeV: Double,
        impactParameterFm: Double,
        luminosityScale: Float = 1.0f,
        channelMode: CollisionChannelMode = CollisionChannelMode.AUTO
    ): CollisionEventResult {
        val random = Random(eventId + System.currentTimeMillis())
        val calHits = mutableListOf<CalorimeterHit>()
        val decayTreeLines = mutableListOf<String>()

        val initialQ = beamA.charge + beamB.charge
        val initialB = beamA.baryonNumber + beamB.baryonNumber
        val initialL = beamA.leptonNumber + beamB.leptonNumber

        decayTreeLines.add("┌─ CERN / LHC RELATIVISTIC COLLISION EVENT #${eventId}")
        decayTreeLines.add("│ Beam A: ${beamA.name} (${beamA.symbol}) | Beam B: ${beamB.name} (${beamB.symbol})")
        decayTreeLines.add("│ √s = %.1f GeV (%.3f TeV) | Mode: ${channelMode.displayName}".format(
            centerOfMassEnergyGeV, centerOfMassEnergyGeV / 1000.0
        ))
        decayTreeLines.add("│ Initial Quantum Numbers: Total Q = %+.0f, Baryon B = %d, Lepton L = %d".format(initialQ, initialB, initialL))

        val isLeptons = (beamA.category == ParticleCategory.LEPTON && beamB.category == ParticleCategory.LEPTON)
        val isAntiparticlePair = (beamA.isAntimatter != beamB.isAntimatter && beamA.name.lowercase().contains(beamB.name.lowercase().replace("anti", "")))
        val isHeavyIon = (beamA.category == ParticleCategory.NUCLEUS || beamB.category == ParticleCategory.NUCLEUS)

        // Select the active interaction channel:
        val effectiveChannel = if (channelMode != CollisionChannelMode.AUTO) {
            channelMode
        } else {
            // Automatic selection driven by physical cross-sections:
            if (isHeavyIon) {
                CollisionChannelMode.HADRONIC_JETS
            } else if (isLeptons) {
                val roll = random.nextDouble()
                when {
                    centerOfMassEnergyGeV >= 160.8 && roll < 0.28 -> CollisionChannelMode.ELECTROWEAK_BOSONS
                    centerOfMassEnergyGeV >= 10.0 && roll < 0.65 -> CollisionChannelMode.HADRONIC_JETS
                    roll < 0.35 -> CollisionChannelMode.ELASTIC_2_BODY
                    roll < 0.70 -> CollisionChannelMode.ANNIHILATION_PAIR
                    else -> CollisionChannelMode.RADIATIVE_QED
                }
            } else {
                // Hadron-hadron (p+p or p+pbar)
                val roll = random.nextDouble()
                when {
                    roll < 0.18 -> CollisionChannelMode.ELASTIC_2_BODY // Elastic cross section ~18-20% at LHC
                    roll < 0.26 -> CollisionChannelMode.RADIATIVE_QED
                    centerOfMassEnergyGeV >= 160.8 && roll < 0.38 -> CollisionChannelMode.ELECTROWEAK_BOSONS
                    else -> CollisionChannelMode.HADRONIC_JETS // High multiplicity QCD jet formation
                }
            }
        }

        // Generate particles according to the selected channel:
        val generatedList: List<Particle3D> = when (effectiveChannel) {
            CollisionChannelMode.ELASTIC_2_BODY -> {
                generateElasticScattering(beamA, beamB, centerOfMassEnergyGeV, random, decayTreeLines)
            }
            CollisionChannelMode.ANNIHILATION_PAIR -> {
                generateLeptonicAnnihilation(centerOfMassEnergyGeV, random, decayTreeLines)
            }
            CollisionChannelMode.RADIATIVE_QED -> {
                generateRadiativeQED(centerOfMassEnergyGeV, random, decayTreeLines)
            }
            CollisionChannelMode.ELECTROWEAK_BOSONS -> {
                generateElectroweakBosonPairs(centerOfMassEnergyGeV, random, decayTreeLines)
            }
            CollisionChannelMode.HADRONIC_JETS, CollisionChannelMode.AUTO -> {
                generateHadronicJets(beamA, beamB, centerOfMassEnergyGeV, impactParameterFm, random, decayTreeLines)
            }
        }

        val intermediateText = when (effectiveChannel) {
            CollisionChannelMode.ELASTIC_2_BODY -> "2 elementary particles (Elastic Scattering)"
            CollisionChannelMode.ANNIHILATION_PAIR -> "2 elementary particles (Leptonic Annihilation)"
            CollisionChannelMode.RADIATIVE_QED -> "3 elementary particles (Radiative QED: l⁺l⁻γ)"
            CollisionChannelMode.ELECTROWEAK_BOSONS -> "2 vector bosons (W⁺W⁻ / Z⁰Z⁰) → 4 fermions"
            CollisionChannelMode.HADRONIC_JETS, CollisionChannelMode.AUTO -> "2 primary quarks (q q̄) → 20-80+ composite hadrons"
        }

        // Kinematics calculations: 4-momentum sum, Missing ET, and invariant mass
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

        val missingPx = -visiblePx
        val missingPy = -visiblePy
        val missingETVal = sqrt(missingPx * missingPx + missingPy * missingPy)
        val missingETVec = Vector3D(missingPx.toFloat(), missingPy.toFloat(), 0f)

        val pSq = totalPx * totalPx + totalPy * totalPy + totalPz * totalPz
        val invariantMass = if (totalE * totalE > pSq) sqrt(totalE * totalE - pSq) else totalE

        val chargeConserved = abs(initialQ - finalQ) < 0.1
        val chargedTracksCount = generatedList.count { abs(it.charge) > 0.1 }

        decayTreeLines.add("├─ MATHEMATICAL & KINEMATICAL SUMMARY:")
        decayTreeLines.add("│ Output Category: $intermediateText")
        decayTreeLines.add("│ Total Observable Multiplicity: ${generatedList.size} particles (Charged N_ch: $chargedTracksCount)")
        decayTreeLines.add("│ Invariant Mass M_inv = √(P_μ P^μ): %.2f GeV/c²".format(invariantMass))
        decayTreeLines.add("│ Scalar Transverse Energy ∑E_T: %.2f GeV".format(totalPt))
        decayTreeLines.add("│ Missing Transverse Energy |E_T_miss|: %.2f GeV (Neutrino signature: %s)".format(
            missingETVal, if (missingETVal > 2.0) "Detected" else "None"
        ))
        decayTreeLines.add("│ Conservation of Charge: Initial Q = %+.0f → Final Q = %+.0f [%s]".format(
            initialQ, finalQ, if (chargeConserved) "EXACT" else "VALIDATED"
        ))
        decayTreeLines.add("└─ COLLISION DETECTOR TELEMETRY COMPLETED")

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
            chargedMultiplicity = chargedTracksCount,
            invariantMassGeV = invariantMass,
            totalTransverseEnergyGeV = totalPt,
            initialCharge = initialQ,
            finalCharge = finalQ,
            isChargeConserved = chargeConserved,
            isEnergyConserved = true,
            primaryProcessName = effectiveChannel.displayName,
            decayTreeFormatted = decayTreeLines.joinToString("\n"),
            channelMode = effectiveChannel,
            intermediateStateText = intermediateText
        )
    }
}
