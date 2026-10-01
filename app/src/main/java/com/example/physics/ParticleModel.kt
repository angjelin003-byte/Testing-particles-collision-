package com.example.physics

import androidx.compose.ui.graphics.Color

enum class ParticleCategory(val displayName: String) {
    QUARK("Quarks"),
    LEPTON("Leptons"),
    GAUGE_BOSON("Gauge Bosons"),
    HIGGS_BOSON("Higgs Boson"),
    HADRON("Hadrons"),
    NUCLEUS("Atomic Nuclei")
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
    val description: String
)

object StandardModelCatalog {
    // Colors
    val ColorQuark = Color(0xFFFF5252)
    val ColorAntiQuark = Color(0xFFFF79B0)
    val ColorLepton = Color(0xFF00E676)
    val ColorAntiLepton = Color(0xFF69F0AE)
    val ColorBoson = Color(0xFFFFD600)
    val ColorHiggs = Color(0xFFE040FB)
    val ColorHadron = Color(0xFF00E5FF)
    val ColorNucleus = Color(0xFFFF9100)

    val PROTON = ParticleSpecies(
        id = "proton",
        name = "Proton",
        symbol = "p",
        restMassGeV = 0.938272,
        charge = 1.0,
        spin = "1/2",
        isAntimatter = false,
        category = ParticleCategory.HADRON,
        color = ColorHadron,
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        baryonNumber = 1,
        description = "Stable composite hadron composed of uud valence quarks. Primary beam particle at LHC."
    )

    val ANTIPROTON = ParticleSpecies(
        id = "antiproton",
        name = "Antiproton",
        symbol = "p̅",
        restMassGeV = 0.938272,
        charge = -1.0,
        spin = "1/2",
        isAntimatter = true,
        category = ParticleCategory.HADRON,
        color = Color(0xFF00B0FF),
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        baryonNumber = -1,
        description = "Antimatter counterpart of proton composed of u̅u̅d̅ anti-quarks."
    )

    val ELECTRON = ParticleSpecies(
        id = "electron",
        name = "Electron",
        symbol = "e⁻",
        restMassGeV = 0.000511,
        charge = -1.0,
        spin = "1/2",
        isAntimatter = false,
        category = ParticleCategory.LEPTON,
        color = ColorLepton,
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        leptonNumber = 1,
        description = "First generation charged lepton. Fundamental point-like particle. Leaves ionization track in Inner Tracker."
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
        color = ColorAntiLepton,
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        leptonNumber = -1,
        description = "Antimatter counterpart of electron. Annihilates upon contact with matter."
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
        color = Color(0xFF1DE9B6),
        meanLifetimeNs = 2196.98,
        leptonNumber = 1,
        description = "Second generation lepton. Minimum ionizing particle penetrating through calorimeters into outer Muon Chambers."
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
        color = Color(0xFFA7FFEB),
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
        color = Color(0xFF00BFA5),
        meanLifetimeNs = 0.00029,
        leptonNumber = 1,
        description = "Heavy third generation lepton. Decays rapidly into hadrons or lighter leptons."
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
        color = Color(0xFFB9F6CA),
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        leptonNumber = 1,
        description = "Neutral light lepton. Escapes detector undetected as missing transverse energy (E_T_miss)."
    )

    val PION_PLUS = ParticleSpecies(
        id = "pion_plus",
        name = "Pion +",
        symbol = "π⁺",
        restMassGeV = 0.139570,
        charge = 1.0,
        spin = "0",
        isAntimatter = false,
        category = ParticleCategory.HADRON,
        color = Color(0xFF00E5FF),
        meanLifetimeNs = 26.03,
        description = "Lightest meson hadron composed of u d̅ quarks. Primary component of quark/gluon jet fragmentation."
    )

    val PION_MINUS = ParticleSpecies(
        id = "pion_minus",
        name = "Pion -",
        symbol = "π⁻",
        restMassGeV = 0.139570,
        charge = -1.0,
        spin = "0",
        isAntimatter = false,
        category = ParticleCategory.HADRON,
        color = Color(0xFF00B0FF),
        meanLifetimeNs = 26.03,
        description = "Negatively charged pion meson."
    )

    val PION_ZERO = ParticleSpecies(
        id = "pion_zero",
        name = "Neutral Pion",
        symbol = "π⁰",
        restMassGeV = 0.134977,
        charge = 0.0,
        spin = "0",
        isAntimatter = false,
        category = ParticleCategory.HADRON,
        color = Color(0xFF80DEEA),
        meanLifetimeNs = 0.000000084,
        description = "Neutral meson decaying almost instantly into photon pairs (π⁰ → γγ)."
    )

    val UP_QUARK = ParticleSpecies(
        id = "up_quark",
        name = "Up Quark",
        symbol = "u",
        restMassGeV = 0.00216,
        charge = 0.666667, // +2/3
        spin = "1/2",
        isAntimatter = false,
        category = ParticleCategory.QUARK,
        color = ColorQuark,
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        description = "Lightest quark with fractional electric charge +2/3 e."
    )

    val DOWN_QUARK = ParticleSpecies(
        id = "down_quark",
        name = "Down Quark",
        symbol = "d",
        restMassGeV = 0.00467,
        charge = -0.333333, // -1/3
        spin = "1/2",
        isAntimatter = false,
        category = ParticleCategory.QUARK,
        color = Color(0xFFFF1744),
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        description = "First generation quark with fractional electric charge -1/3 e."
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
        color = Color(0xFFD50000),
        meanLifetimeNs = 0.0000000005,
        description = "Heaviest known elementary particle. Decays electroweakly before hadronizing."
    )

    val PHOTON = ParticleSpecies(
        id = "photon",
        name = "Photon",
        symbol = "γ",
        restMassGeV = 0.0,
        charge = 0.0,
        spin = "1",
        isAntimatter = false,
        category = ParticleCategory.GAUGE_BOSON,
        color = ColorBoson,
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        description = "Massless gauge boson of electromagnetism. Deposits energy shower cluster in ECAL."
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
        color = Color(0xFFFFEA00),
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        description = "Massless gauge boson carrying color charge for quantum chromodynamics (QCD)."
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
        color = Color(0xFFFFC400),
        meanLifetimeNs = 0.000000003,
        description = "Heavy charged gauge boson mediating the weak force."
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
        color = Color(0xFFFFAB00),
        meanLifetimeNs = 0.000000003,
        description = "Heavy neutral gauge boson. Decays into dilepton pairs (e⁺e⁻, μ⁺μ⁻) or quark-antiquark jets."
    )

    val HIGGS_BOSON = ParticleSpecies(
        id = "higgs",
        name = "Higgs Boson",
        symbol = "H⁰",
        restMassGeV = 125.25,
        charge = 0.0,
        spin = "0",
        isAntimatter = false,
        category = ParticleCategory.HIGGS_BOSON,
        color = ColorHiggs,
        meanLifetimeNs = 0.000000156,
        description = "Scalar boson associated with the Brout-Englert-Higgs mass generation mechanism."
    )

    val NEUTRON = ParticleSpecies(
        id = "neutron",
        name = "Neutron",
        symbol = "n",
        restMassGeV = 0.939565,
        charge = 0.0,
        spin = "1/2",
        isAntimatter = false,
        category = ParticleCategory.HADRON,
        color = Color(0xFF80DEEA),
        meanLifetimeNs = 8.794e11,
        baryonNumber = 1,
        description = "Neutral hadron composite udd. Deposits energy cluster in Hadronic Calorimeter (HCAL)."
    )

    val ALPHA_PARTICLE = ParticleSpecies(
        id = "alpha",
        name = "Alpha Particle",
        symbol = "α (⁴He²⁺)",
        restMassGeV = 3.727379,
        charge = 2.0,
        spin = "0",
        isAntimatter = false,
        category = ParticleCategory.NUCLEUS,
        color = ColorNucleus,
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        baryonNumber = 4,
        description = "Helium-4 nucleus with 2 protons and 2 neutrons."
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
        color = Color(0xFFFF6D00),
        meanLifetimeNs = Double.POSITIVE_INFINITY,
        baryonNumber = 208,
        description = "Heavy lead ion nucleus used in ultra-relativistic heavy-ion collision runs at LHC."
    )

    val ALL_SPECIES = listOf(
        PROTON, ANTIPROTON, ELECTRON, POSITRON, MUON_MINUS, MUON_PLUS,
        TAU_MINUS, NEUTRINO_ELECTRON, PION_PLUS, PION_MINUS, PION_ZERO,
        UP_QUARK, DOWN_QUARK, TOP_QUARK, PHOTON, GLUON, W_PLUS_BOSON, Z_BOSON, HIGGS_BOSON,
        NEUTRON, ALPHA_PARTICLE, LEAD_ION
    )

    fun getById(id: String): ParticleSpecies {
        return ALL_SPECIES.find { it.id == id } ?: PROTON
    }
}
