package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.physics.CalorimeterHit
import com.example.physics.CollisionChannelMode
import com.example.physics.CollisionEventResult
import com.example.physics.PacketDualityMode
import com.example.physics.PacketEjectionEngine
import com.example.physics.Particle3D
import com.example.physics.ParticleCategory
import com.example.physics.ParticlePacket3D
import com.example.physics.ParticleSpecies
import com.example.physics.RelativisticCollisionEngine
import com.example.physics.StandardModelCatalog
import com.example.physics.Vector3D
import com.example.physics.WavePacket3D
import com.example.rendering.Camera3D
import com.example.rendering.ViewProjectionMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class TrailColorMode(val displayName: String) {
    SPECIES_COLOR("Species Category"),
    ENERGY_HEATMAP("Energy Heatmap (pT)"),
    CHARGE_COLOR("Electric Charge (+ / - / 0)")
}

data class BackgroundPreset(
    val name: String,
    val color: Color,
    val isDark: Boolean
)

object BackgroundPresets {
    val PRESETS = listOf(
        BackgroundPreset("Deep Void", Color(0xFF090D16), true),
        BackgroundPreset("CERN Control", Color(0xFF0C1222), true),
        BackgroundPreset("Dark Slate", Color(0xFF1E293B), true),
        BackgroundPreset("Pure OLED Black", Color(0xFF000000), true),
        BackgroundPreset("Clean Lab", Color(0xFFECEFF1), false),
        BackgroundPreset("Polar White", Color(0xFFF8FAFC), false)
    )
}

data class SimulationState(
    val particleA: ParticleSpecies = StandardModelCatalog.PROTON,
    val particleB: ParticleSpecies = StandardModelCatalog.PROTON,
    val energyGeV: Double = 13600.0, // 13.6 TeV
    val magneticFieldTesla: Float = 3.8f, // LHC Solenoid field = 3.8T
    val electricFieldMVm: Float = 2.0f,
    val luminosityScale: Float = 1.0f,
    val impactParameterFm: Double = 0.2,
    val timeScale: Float = 0.008f, // slowmo in fractions of c
    val isPaused: Boolean = false,
    val wireframeEnabled: Boolean = true,
    val gridOverlayEnabled: Boolean = true,
    val showTracker: Boolean = true,
    val showEcal: Boolean = true,
    val showHcal: Boolean = true,
    val showMuon: Boolean = true,
    val glowIntensity: Float = 1.0f,
    val isDarkTheme: Boolean = true,
    val totalCollisionsFired: Long = 0L,
    val isBeamInFlight: Boolean = false,
    val trailLength: Int = 20,       // 5 to 40
    val trailWidth: Float = 1.0f,     // 0.5x to 4.0x
    val trailAlpha: Float = 0.85f,    // 0.2 to 1.0
    val trailColorMode: TrailColorMode = TrailColorMode.SPECIES_COLOR,
    val projectionMode: ViewProjectionMode = ViewProjectionMode.PERSPECTIVE,
    val channelMode: CollisionChannelMode = CollisionChannelMode.AUTO,
    val bgPresetIndex: Int = 0,
    val bgHue: Float = 220f,         // 0f to 360f
    val bgSaturation: Float = 0.45f, // 0f to 1.0f
    val bgBrightness: Float = 0.08f, // 0.02f to 1.0f
    val enableWavePacketEjection: Boolean = true,
    val enableParticlePacketEjection: Boolean = true,
    val wavePacketDispersionRate: Float = 1.0f,
    val wavePacketFrequencyScale: Float = 1.0f,
    val particlePacketConeScale: Float = 1.0f,
    val packetDualityMode: PacketDualityMode = PacketDualityMode.DUAL_WAVE_PARTICLE,
    val speedA: Float = 0.95f,         // 0.05 to 0.999 fraction of c
    val speedB: Float = 0.95f,         // 0.05 to 0.999 fraction of c
    val syncBeamSpeeds: Boolean = true // link A & B speeds symmetrically
)

class ColliderViewModel : ViewModel() {

    private val _state = MutableStateFlow(SimulationState())
    val state: StateFlow<SimulationState> = _state.asStateFlow()

    private val _liveParticles = MutableStateFlow<List<Particle3D>>(emptyList())
    val liveParticles: StateFlow<List<Particle3D>> = _liveParticles.asStateFlow()

    private val _liveWavePackets = MutableStateFlow<List<WavePacket3D>>(emptyList())
    val liveWavePackets: StateFlow<List<WavePacket3D>> = _liveWavePackets.asStateFlow()

    private val _liveParticlePackets = MutableStateFlow<List<ParticlePacket3D>>(emptyList())
    val liveParticlePackets: StateFlow<List<ParticlePacket3D>> = _liveParticlePackets.asStateFlow()

    private val _eventHistory = MutableStateFlow<List<CollisionEventResult>>(emptyList())
    val eventHistory: StateFlow<List<CollisionEventResult>> = _eventHistory.asStateFlow()

    private val _currentEvent = MutableStateFlow<CollisionEventResult?>(null)
    val currentEvent: StateFlow<CollisionEventResult?> = _currentEvent.asStateFlow()

    val camera = Camera3D()

    private var eventCounter = 1001L
    private var isPendingDetonation = false
    private val depositedParticleIds = mutableSetOf<String>()

    init {
        resetIncomingBeams()

        // 60 FPS physics tick loop
        viewModelScope.launch {
            while (true) {
                delay(16L) // ~60fps
                if (!_state.value.isPaused) {
                    stepPhysicsTick()
                }
            }
        }
    }

    private fun resetIncomingBeams() {
        val s = _state.value
        val beams = RelativisticCollisionEngine.createIncomingBeams(
            beamA = s.particleA,
            beamB = s.particleB,
            energyGeV = s.energyGeV / 2.0,
            impactParameterFm = s.impactParameterFm,
            speedA = s.speedA,
            speedB = s.speedB
        )
        _liveParticles.value = beams
        _liveWavePackets.value = emptyList()
        _liveParticlePackets.value = emptyList()
        depositedParticleIds.clear()
        isPendingDetonation = true
        _state.update { it.copy(isBeamInFlight = true) }
    }

    fun fireCollision() {
        _state.update { it.copy(isPaused = false, isBeamInFlight = true) }
        resetIncomingBeams()
    }

    private fun detonateCollision() {
        val s = _state.value
        eventCounter++

        val result = RelativisticCollisionEngine.simulateCollision(
            eventId = eventCounter,
            beamA = s.particleA,
            beamB = s.particleB,
            centerOfMassEnergyGeV = s.energyGeV,
            impactParameterFm = s.impactParameterFm,
            luminosityScale = s.luminosityScale,
            channelMode = s.channelMode
        )

        _currentEvent.value = result
        _eventHistory.update { listOf(result) + it.take(49) }
        _liveParticles.value = result.generatedParticles

        // Generate Quantum Wave Packets & Collimated Particle Packets ejected after main collision
        val (wavePkts, particlePkts) = PacketEjectionEngine.createCollisionPackets(
            generatedParticles = result.generatedParticles,
            sqrtSGeV = s.energyGeV,
            channelMode = s.channelMode
        )
        _liveWavePackets.value = wavePkts
        _liveParticlePackets.value = particlePkts

        depositedParticleIds.clear()
        isPendingDetonation = false
        _state.update {
            it.copy(
                totalCollisionsFired = it.totalCollisionsFired + 1,
                isBeamInFlight = false
            )
        }
    }

    private fun stepPhysicsTick() {
        val s = _state.value
        val particles = _liveParticles.value.toMutableList()
        if (particles.isEmpty()) return

        val dt = s.timeScale * 0.12f

        if (isPendingDetonation) {
            val beamA = particles.find { it.generation == 0 && it.momentum.z > 0 }
            val beamB = particles.find { it.generation == 0 && it.momentum.z < 0 }

            if (beamA != null && beamB != null) {
                beamA.step(dt, s.magneticFieldTesla, s.electricFieldMVm, maxTrailLength = s.trailLength)
                beamB.step(dt, s.magneticFieldTesla, s.electricFieldMVm, maxTrailLength = s.trailLength)

                if (abs(beamA.position.z) <= 0.3f || abs(beamB.position.z) <= 0.3f) {
                    detonateCollision()
                    return
                }
                _liveParticles.value = listOf(beamA, beamB)
                return
            } else {
                detonateCollision()
                return
            }
        }
        val updated = mutableListOf<Particle3D>()
        val newSecondary = mutableListOf<Particle3D>()

        for (p in particles) {
            p.step(dt, s.magneticFieldTesla, s.electricFieldMVm, maxTrailLength = s.trailLength)
            if (!p.isEscaped) {
                updated.add(p)
                p.checkAndTriggerSecondaryDecay()?.let { daughters ->
                    newSecondary.addAll(daughters)
                }
            }
        }
        updated.addAll(newSecondary)
        _liveParticles.value = updated

        // Step live Quantum Wave Packets
        val currentWavePackets = _liveWavePackets.value
        if (currentWavePackets.isNotEmpty()) {
            val updatedWavePackets = mutableListOf<WavePacket3D>()
            for (wp in currentWavePackets) {
                wp.step(
                    dtSeconds = dt,
                    dispersionRate = s.wavePacketDispersionRate,
                    frequencyScale = s.wavePacketFrequencyScale
                )
                if (!wp.isDissipated) {
                    updatedWavePackets.add(wp)
                }
            }
            _liveWavePackets.value = updatedWavePackets
        }

        // Step live Collimated Particle Packets
        val currentParticlePackets = _liveParticlePackets.value
        if (currentParticlePackets.isNotEmpty()) {
            val updatedParticlePackets = mutableListOf<ParticlePacket3D>()
            for (pp in currentParticlePackets) {
                pp.step(
                    dtSeconds = dt,
                    coneScale = s.particlePacketConeScale
                )
                if (!pp.isEscaped) {
                    updatedParticlePackets.add(pp)
                }
            }
            _liveParticlePackets.value = updatedParticlePackets
        }
    }

    fun stepSingleFrame() {
        stepPhysicsTick()
    }

    fun setParticleA(species: ParticleSpecies) {
        _state.update { it.copy(particleA = species) }
        resetIncomingBeams()
    }

    fun setParticleB(species: ParticleSpecies) {
        _state.update { it.copy(particleB = species) }
        resetIncomingBeams()
    }

    fun setEnergyGeV(energy: Double) {
        _state.update { it.copy(energyGeV = energy) }
        resetIncomingBeams()
    }

    fun setMagneticFieldTesla(b: Float) {
        _state.update { it.copy(magneticFieldTesla = b) }
    }

    fun setElectricFieldMVm(e: Float) {
        _state.update { it.copy(electricFieldMVm = e) }
    }

    fun setTimeScale(scale: Float) {
        _state.update { it.copy(timeScale = scale) }
    }

    fun setGlowIntensity(glow: Float) {
        _state.update { it.copy(glowIntensity = glow) }
    }

    fun setTrailLength(length: Int) {
        _state.update { it.copy(trailLength = length) }
    }

    fun setTrailWidth(width: Float) {
        _state.update { it.copy(trailWidth = width) }
    }

    fun setTrailAlpha(alpha: Float) {
        _state.update { it.copy(trailAlpha = alpha) }
    }

    fun setTrailColorMode(mode: TrailColorMode) {
        _state.update { it.copy(trailColorMode = mode) }
    }

    fun setChannelMode(mode: CollisionChannelMode) {
        _state.update { it.copy(channelMode = mode) }
    }

    fun setProjectionMode(mode: ViewProjectionMode) {
        camera.applyProjectionMode(mode)
        _state.update { it.copy(projectionMode = mode) }
    }

    fun setBgPreset(index: Int) {
        val preset = BackgroundPresets.PRESETS.getOrNull(index) ?: return
        val (h, s, v) = when (index) {
            0 -> Triple(220f, 0.45f, 0.08f) // Deep Void
            1 -> Triple(218f, 0.48f, 0.12f) // CERN Control
            2 -> Triple(215f, 0.33f, 0.18f) // Dark Slate
            3 -> Triple(0f, 0.0f, 0.02f)    // OLED Black
            4 -> Triple(200f, 0.06f, 0.93f) // Clean Lab
            5 -> Triple(210f, 0.03f, 0.98f) // Polar White
            else -> Triple(220f, 0.45f, 0.08f)
        }
        _state.update {
            it.copy(
                bgPresetIndex = index,
                isDarkTheme = preset.isDark,
                bgHue = h,
                bgSaturation = s,
                bgBrightness = v
            )
        }
    }

    fun setBgHue(hue: Float) {
        _state.update { it.copy(bgHue = hue) }
    }

    fun setBgSaturation(saturation: Float) {
        _state.update { it.copy(bgSaturation = saturation) }
    }

    fun setBgBrightness(brightness: Float) {
        _state.update {
            it.copy(
                bgBrightness = brightness,
                isDarkTheme = brightness < 0.5f
            )
        }
    }

    fun togglePause() {
        _state.update { it.copy(isPaused = !it.isPaused) }
    }

    fun toggleWireframe() {
        _state.update { it.copy(wireframeEnabled = !it.wireframeEnabled) }
    }

    fun toggleGrid() {
        _state.update { it.copy(gridOverlayEnabled = !it.gridOverlayEnabled) }
    }

    fun toggleTheme() {
        val nextDark = !_state.value.isDarkTheme
        _state.update {
            it.copy(
                isDarkTheme = nextDark,
                bgPresetIndex = if (nextDark) 0 else 5
            )
        }
    }

    fun resetCamera() {
        camera.reset()
        _state.update { it.copy(projectionMode = ViewProjectionMode.PERSPECTIVE) }
    }

    fun toggleTrackerLayer() {
        _state.update { it.copy(showTracker = !it.showTracker) }
    }

    fun toggleEcalLayer() {
        _state.update { it.copy(showEcal = !it.showEcal) }
    }

    fun toggleHcalLayer() {
        _state.update { it.copy(showHcal = !it.showHcal) }
    }

    fun toggleMuonLayer() {
        _state.update { it.copy(showMuon = !it.showMuon) }
    }

    fun setEnableWavePacketEjection(enabled: Boolean) {
        _state.update { it.copy(enableWavePacketEjection = enabled) }
    }

    fun setEnableParticlePacketEjection(enabled: Boolean) {
        _state.update { it.copy(enableParticlePacketEjection = enabled) }
    }

    fun setImpactParameterFm(value: Double) {
        _state.update { it.copy(impactParameterFm = value.coerceIn(0.0, 5.0)) }
    }

    fun setWavePacketDispersionRate(rate: Float) {
        _state.update { it.copy(wavePacketDispersionRate = rate.coerceIn(0.2f, 3.0f)) }
    }

    fun setWavePacketFrequencyScale(scale: Float) {
        _state.update { it.copy(wavePacketFrequencyScale = scale.coerceIn(0.3f, 3.0f)) }
    }

    fun setParticlePacketConeScale(scale: Float) {
        _state.update { it.copy(particlePacketConeScale = scale.coerceIn(0.4f, 3.0f)) }
    }

    fun setPacketDualityMode(mode: PacketDualityMode) {
        _state.update { it.copy(packetDualityMode = mode) }
    }

    fun setSpeedA(speed: Float) {
        val clamped = speed.coerceIn(0.05f, 0.999f)
        _state.update {
            if (it.syncBeamSpeeds) {
                it.copy(speedA = clamped, speedB = clamped)
            } else {
                it.copy(speedA = clamped)
            }
        }
        updateBeamParticleSpeeds()
    }

    fun setSpeedB(speed: Float) {
        val clamped = speed.coerceIn(0.05f, 0.999f)
        _state.update {
            if (it.syncBeamSpeeds) {
                it.copy(speedA = clamped, speedB = clamped)
            } else {
                it.copy(speedB = clamped)
            }
        }
        updateBeamParticleSpeeds()
    }

    fun setSyncBeamSpeeds(sync: Boolean) {
        _state.update {
            it.copy(syncBeamSpeeds = sync, speedB = if (sync) it.speedA else it.speedB)
        }
        updateBeamParticleSpeeds()
    }

    private fun updateBeamParticleSpeeds() {
        val s = _state.value
        val current = _liveParticles.value
        if (isPendingDetonation && current.isNotEmpty()) {
            val beamA = current.find { it.generation == 0 && it.momentum.z > 0 }
            val beamB = current.find { it.generation == 0 && it.momentum.z < 0 }
            beamA?.speedFractionOfC = s.speedA
            beamB?.speedFractionOfC = s.speedB
        }
    }

    fun clearLogs() {
        _eventHistory.value = emptyList()
        _currentEvent.value = null
        depositedParticleIds.clear()
    }

    fun formatAllLogsForExport(): String {
        val history = _eventHistory.value
        if (history.isEmpty()) {
            return "No particle collision event logs recorded yet.\nTrigger a collision in the dashboard to generate event data."
        }

        val sb = StringBuilder()
        sb.append("=====================================================\n")
        sb.append("  3D RELATIVISTIC PARTICLE COLLIDER DATA LOG EXPORT  \n")
        sb.append("  Export Timestamp: ${System.currentTimeMillis()}\n")
        sb.append("  Total Recorded Events: ${history.size}\n")
        sb.append("=====================================================\n\n")

        for ((idx, ev) in history.withIndex()) {
            sb.append("--- [EVENT RECORD #${idx + 1} / ID:${ev.eventId}] ---\n")
            sb.append(ev.decayTreeFormatted)
            sb.append("\n\n")
        }

        return sb.toString()
    }
}
