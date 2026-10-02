package com.example.physics

import androidx.compose.ui.graphics.Color

enum class ParticleCategory(val displayName: String, val badgeColor: Color) {
    LEPTON("Leptons", Color(0xFF00E676)),
    MESON("Mesons", Color(0xFF2979FF)),
    BARYON("Baryons", Color(0xFFFF3D00)),
    GAUGE_BOSON("Gauge Bosons", Color(0xFFFFD600)),
    HIGGS_BOSON("Higgs Boson", Color(0xFFE040FB)),
    QUARK("Quarks", Color(0xFFFF4081)),
    NUCLEUS("Atomic Nuclei", Color(0xFFFF6D00))
}

data class ParticleSpecies(
    val id: String,
    val name: String,
    val symbol: String,
    val restMassGeV: Double,
    val charge: Double, // in units of elementary charge e
    val spin: String,
    val isAntimatter: Boolean,
    val category: ParticleCategory,
    val color: Color,
    val meanLifetimeNs: Double, // in nanoseconds, Double.POSITIVE_INFINITY for stable
    val baryonNumber: Int = 0,
    val leptonNumber: Int = 0,
    val physicalRadiusFm: Double = 0.0, // Charge radius in femtometers (0.0 for point-like particles)
    val description: String
) {
    /**
     * Relative render size scale based on realistic subatomic physics:
     * - Fundamental point-like particles (leptons, photons, quarks): r < 10^-18 m (scale = 0.72)
     * - Light composite mesons (pions, kaons): r ≈ 0.66 fm (scale = 1.05)
     * - Heavier composite baryons (protons, neutrons): r ≈ 0.84-0.87 fm (scale = 1.45)
     * - Massive gauge / scalar bosons (W, Z, Higgs): scale = 1.50
     * - Light nuclei (alpha He-4): r ≈ 1.68 fm (scale = 2.20)
     * - Heavy nuclei (Lead-208): r ≈ 5.50 fm (scale = 3.60)
     */
    val renderRadiusMultiplier: Float
        get() = when {
            category == ParticleCategory.NUCLEUS && restMassGeV > 100.0 -> 3.6f  // 208Pb
            category == ParticleCategory.NUCLEUS -> 2.2f                         // 4He (alpha)
            category == ParticleCategory.BARYON -> 1.45f                         // Proton, Neutron
            category == ParticleCategory.HIGGS_BOSON -> 1.50f                    // Higgs Boson
            category == ParticleCategory.GAUGE_BOSON && restMassGeV > 50.0 -> 1.40f // W, Z
            category == ParticleCategory.MESON -> 1.05f                          // Pions, Kaons
            else -> 0.72f                                                        // Leptons, Photons, Quarks
        }
}

object StandardModelCatalog {

    // --- LEPTONS (Electric Emerald Green, Mint, Cyan) ---
    val ELECTRON = ParticleSpecies(
        id = "electron",
        name = "Electron",
        symbol = "e⁻",
        restMassGeV = 0.000511,
        charge = -1.0,
        spin = "1/2",
        isAntimatter = false,
        category = ParticleCategory.LEPTON,
        color = Color(0xFF00E676), // Vivid Emerald Green
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        leptonNumber = 1,
        description = "First generation charged lepton. Fundamental point-like fermion."
    )

    val POSITRON = ParticleSpecies(
        id = "positron",
        name = "Positron",
        symbol = "e⁺",
        restMassGeV = 0.000511,
        charge = 1.0,
        spin = "1/2",
        isAntimatter = true,
        category = ParticleCategory.LEPTON,
        color = Color(0xFF69F0AE), // Light Mint Green
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        leptonNumber = -1,
        description = "Antimatter counterpart of electron with positive elementary charge."
    )

    val MUON_MINUS = ParticleSpecies(
        id = "muon_minus",
        name = "Muon",
        symbol = "μ⁻",
        restMassGeV = 0.105658,
        charge = -1.0,
        spin = "1/2",
        isAntimatter = false,
        category = ParticleCategory.LEPTON,
        color = Color(0xFF00E5FF), // Electric Cyan
        meanLifetimeNs = 2196.98,
        leptonNumber = 1,
        description = "Second generation lepton. Minimum ionizing particle penetrating to outer muon drift chambers."
    )

    val MUON_PLUS = ParticleSpecies(
        id = "muon_plus",
        name = "Anti-Muon",
        symbol = "μ⁺",
        restMassGeV = 0.105658,
        charge = 1.0,
        spin = "1/2",
        isAntimatter = true,
        category = ParticleCategory.LEPTON,
        color = Color(0xFFA7FFEB), // Pale Aqua
        meanLifetimeNs = 2196.98,
        leptonNumber = -1,
        description = "Positively charged anti-muon lepton."
    )

    val TAU_MINUS = ParticleSpecies(
        id = "tau_minus",
        name = "Tau",
        symbol = "τ⁻",
        restMassGeV = 1.77686,
        charge = -1.0,
        spin = "1/2",
        isAntimatter = false,
        category = ParticleCategory.LEPTON,
        color = Color(0xFF00BFA5), // Darker Cyan/Teal
        meanLifetimeNs = 0.00029,
        leptonNumber = 1,
        description = "Heavy third generation lepton. Decays within picoseconds."
    )

    val NEUTRINO_ELECTRON = ParticleSpecies(
        id = "neutrino_e",
        name = "Neutrino",
        symbol = "ν",
        restMassGeV = 0.000000001,
        charge = 0.0,
        spin = "1/2",
        isAntimatter = false,
        category = ParticleCategory.LEPTON,
        color = Color(0xFFB2DFDB), // Translucent Ghost Sage
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        leptonNumber = 1,
        description = "Neutral lepton. Escapes detector unmeasured as missing transverse energy (E_T_miss)."
    )

    // --- MESONS & LIGHT HADRONS (Cobalt Blue, Deep Indigo, Vivid Sky Blue) ---
    val PION_PLUS = ParticleSpecies(
        id = "pion_plus",
        name = "Pion +",
        symbol = "π⁺",
        restMassGeV = 0.139570,
        charge = 1.0,
        spin = "0",
        isAntimatter = false,
        category = ParticleCategory.MESON,
        color = Color(0xFF2979FF), // High-Energy Cobalt Blue
        meanLifetimeNs = 26.03,
        physicalRadiusFm = 0.66,
        description = "Lightest charged meson (u d̅). Dominant particle in QCD jet hadronization."
    )

    val PION_MINUS = ParticleSpecies(
        id = "pion_minus",
        name = "Pion -",
        symbol = "π⁻",
        restMassGeV = 0.139570,
        charge = -1.0,
        spin = "0",
        isAntimatter = false,
        category = ParticleCategory.MESON,
        color = Color(0xFF3D5AFE), // Deep Indigo Blue
        meanLifetimeNs = 26.03,
        physicalRadiusFm = 0.66,
        description = "Negatively charged pion meson (u̅ d)."
    )

    val PION_ZERO = ParticleSpecies(
        id = "pion_zero",
        name = "Neutral Pion",
        symbol = "π⁰",
        restMassGeV = 0.134977,
        charge = 0.0,
        spin = "0",
        isAntimatter = false,
        category = ParticleCategory.MESON,
        color = Color(0xFF00B0FF), // Light Sky Blue
        meanLifetimeNs = 0.000000084,
        physicalRadiusFm = 0.66,
        description = "Neutral meson decaying promptly into photon pairs (π⁰ → γγ) in ECAL."
    )

    val KAON_PLUS = ParticleSpecies(
        id = "kaon_plus",
        name = "Kaon +",
        symbol = "K⁺",
        restMassGeV = 0.493677,
        charge = 1.0,
        spin = "0",
        isAntimatter = false,
        category = ParticleCategory.MESON,
        color = Color(0xFF00B8D4), // Vivid Turquoise
        meanLifetimeNs = 12.38,
        physicalRadiusFm = 0.56,
        description = "Strange charged meson (u s̅)."
    )

    val KAON_MINUS = ParticleSpecies(
        id = "kaon_minus",
        name = "Kaon -",
        symbol = "K⁻",
        restMassGeV = 0.493677,
        charge = -1.0,
        spin = "0",
        isAntimatter = false,
        category = ParticleCategory.MESON,
        color = Color(0xFF0097A7), // Deep Ocean Teal
        meanLifetimeNs = 12.38,
        physicalRadiusFm = 0.56,
        description = "Negatively charged strange meson (u̅ s)."
    )

    val KAON_ZERO = ParticleSpecies(
        id = "kaon_zero",
        name = "Neutral Kaon",
        symbol = "K⁰",
        restMassGeV = 0.497611,
        charge = 0.0,
        spin = "0",
        isAntimatter = false,
        category = ParticleCategory.MESON,
        color = Color(0xFF26C6DA), // Cyan
        meanLifetimeNs = 51.16,
        physicalRadiusFm = 0.56,
        description = "Neutral strange meson (d s̅) exhibiting strangeness oscillations."
    )

    // --- BARYONS (Flame Crimson, Neon Red, Amber) ---
    val PROTON = ParticleSpecies(
        id = "proton",
        name = "Proton",
        symbol = "p",
        restMassGeV = 0.938272,
        charge = 1.0,
        spin = "1/2",
        isAntimatter = false,
        category = ParticleCategory.BARYON,
        color = Color(0xFFFF3D00), // Flame Crimson Orange
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        baryonNumber = 1,
        physicalRadiusFm = 0.841,
        description = "Stable composite baryon (uud). Primary beam particle at LHC."
    )

    val ANTIPROTON = ParticleSpecies(
        id = "antiproton",
        name = "Antiproton",
        symbol = "p̅",
        restMassGeV = 0.938272,
        charge = -1.0,
        spin = "1/2",
        isAntimatter = true,
        category = ParticleCategory.BARYON,
        color = Color(0xFFFF1744), // Vivid Neon Red
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        baryonNumber = -1,
        physicalRadiusFm = 0.841,
        description = "Antimatter counterpart of proton (u̅u̅d̅)."
    )

    val NEUTRON = ParticleSpecies(
        id = "neutron",
        name = "Neutron",
        symbol = "n",
        restMassGeV = 0.939565,
        charge = 0.0,
        spin = "1/2",
        isAntimatter = false,
        category = ParticleCategory.BARYON,
        color = Color(0xFFFFB300), // Warm Amber
        meanLifetimeNs = 8.794e11,
        baryonNumber = 1,
        physicalRadiusFm = 0.860,
        description = "Neutral composite baryon (udd). Deposits energy in HCAL."
    )

    // --- GAUGE BOSONS (Brilliant Gold, Royal Purple) ---
    val PHOTON = ParticleSpecies(
        id = "photon",
        name = "Photon",
        symbol = "γ",
        restMassGeV = 0.0,
        charge = 0.0,
        spin = "1",
        isAntimatter = false,
        category = ParticleCategory.GAUGE_BOSON,
        color = Color(0xFFFFD600), // Radiant Electric Gold
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        description = "Massless gauge boson of electromagnetism. Produces electromagnetic shower in ECAL."
    )

    val GLUON = ParticleSpecies(
        id = "gluon",
        name = "Gluon",
        symbol = "g",
        restMassGeV = 0.0,
        charge = 0.0,
        spin = "1",
        isAntimatter = false,
        category = ParticleCategory.GAUGE_BOSON,
        color = Color(0xFFFFEA00), // Pure Yellow
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        description = "Massless gauge boson mediating strong color interactions."
    )

    val W_PLUS_BOSON = ParticleSpecies(
        id = "w_plus",
        name = "W+ Boson",
        symbol = "W⁺",
        restMassGeV = 80.377,
        charge = 1.0,
        spin = "1",
        isAntimatter = false,
        category = ParticleCategory.GAUGE_BOSON,
        color = Color(0xFFAA00FF), // Vivid Royal Purple
        meanLifetimeNs = 0.000000003,
        description = "Charged weak gauge boson."
    )

    val Z_BOSON = ParticleSpecies(
        id = "z_boson",
        name = "Z⁰ Boson",
        symbol = "Z⁰",
        restMassGeV = 91.1876,
        charge = 0.0,
        spin = "1",
        isAntimatter = false,
        category = ParticleCategory.GAUGE_BOSON,
        color = Color(0xFF7C4DFF), // Deep Violet
        meanLifetimeNs = 0.000000003,
        description = "Neutral weak gauge boson."
    )

    // --- HIGGS BOSON (Electric Orchid Magenta) ---
    val HIGGS_BOSON = ParticleSpecies(
        id = "higgs",
        name = "Higgs Boson",
        symbol = "H⁰",
        restMassGeV = 125.25,
        charge = 0.0,
        spin = "0",
        isAntimatter = false,
        category = ParticleCategory.HIGGS_BOSON,
        color = Color(0xFFE040FB), // Electric Orchid Magenta
        meanLifetimeNs = 0.000000156,
        description = "Fundamental scalar boson conferring mass via the Higgs mechanism."
    )

    // --- QUARKS (Carmine Pink/Magenta) ---
    val UP_QUARK = ParticleSpecies(
        id = "up_quark",
        name = "Up Quark",
        symbol = "u",
        restMassGeV = 0.00216,
        charge = 0.666667,
        spin = "1/2",
        isAntimatter = false,
        category = ParticleCategory.QUARK,
        color = Color(0xFFFF4081),
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        description = "Lightest quark (+2/3 e)."
    )

    val DOWN_QUARK = ParticleSpecies(
        id = "down_quark",
        name = "Down Quark",
        symbol = "d",
        restMassGeV = 0.00467,
        charge = -0.333333,
        spin = "1/2",
        isAntimatter = false,
        category = ParticleCategory.QUARK,
        color = Color(0xFFF50057),
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        description = "First generation quark (-1/3 e)."
    )

    val TOP_QUARK = ParticleSpecies(
        id = "top_quark",
        name = "Top Quark",
        symbol = "t",
        restMassGeV = 172.69,
        charge = 0.666667,
        spin = "1/2",
        isAntimatter = false,
        category = ParticleCategory.QUARK,
        color = Color(0xFFC51162),
        meanLifetimeNs = 0.0000000005,
        description = "Heaviest elementary particle (172.7 GeV)."
    )

    // --- ATOMIC NUCLEI (Blaze Orange) ---
    val ALPHA_PARTICLE = ParticleSpecies(
        id = "alpha",
        name = "Alpha Particle",
        symbol = "α (⁴He²⁺)",
        restMassGeV = 3.727379,
        charge = 2.0,
        spin = "0",
        isAntimatter = false,
        category = ParticleCategory.NUCLEUS,
        color = Color(0xFFFF9100), // Amber-Orange
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        baryonNumber = 4,
        physicalRadiusFm = 1.68,
        description = "Helium-4 nucleus (2 protons, 2 neutrons)."
    )

    val LEAD_ION = ParticleSpecies(
        id = "lead_ion",
        name = "Lead Nucleus",
        symbol = "²⁰⁸Pb⁸²⁺",
        restMassGeV = 193.687,
        charge = 82.0,
        spin = "0",
        isAntimatter = false,
        category = ParticleCategory.NUCLEUS,
        color = Color(0xFFFF6D00), // Intense Blaze Orange
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        baryonNumber = 208,
        physicalRadiusFm = 5.50,
        description = "Ultra-relativistic heavy lead ion used in ALICE/CMS heavy-ion runs."
    )

    val ALL_SPECIES = listOf(
        PROTON, ANTIPROTON, ELECTRON, POSITRON, MUON_MINUS, MUON_PLUS,
        TAU_MINUS, NEUTRINO_ELECTRON, PION_PLUS, PION_MINUS, PION_ZERO,
        KAON_PLUS, KAON_MINUS, KAON_ZERO,
        UP_QUARK, DOWN_QUARK, TOP_QUARK, PHOTON, GLUON, W_PLUS_BOSON, Z_BOSON, HIGGS_BOSON,
        NEUTRON, ALPHA_PARTICLE, LEAD_ION
    )

    fun getById(id: String): ParticleSpecies {
        return ALL_SPECIES.find { it.id == id } ?: PROTON
    }
}
