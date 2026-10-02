package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.physics.CollisionEventResult
import com.example.physics.Particle3D
import com.example.physics.ParticleSpecies
import com.example.physics.RelativisticCollisionEngine
import com.example.physics.StandardModelCatalog
import com.example.rendering.Camera3D
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
    val glowIntensity: Float = 1.2f,
    val isDarkTheme: Boolean = true,
    val totalCollisionsFired: Long = 0L,
    val isBeamInFlight: Boolean = false,
    val trailLength: Int = 20,       // 5 to 40
    val trailWidth: Float = 1.0f,     // 0.5x to 4.0x
    val trailAlpha: Float = 0.8f,     // 0.2 to 1.0
    val trailColorMode: TrailColorMode = TrailColorMode.SPECIES_COLOR
)

class ColliderViewModel : ViewModel() {

    private val _state = MutableStateFlow(SimulationState())
    val state: StateFlow<SimulationState> = _state.asStateFlow()

    private val _liveParticles = MutableStateFlow<List<Particle3D>>(emptyList())
    val liveParticles: StateFlow<List<Particle3D>> = _liveParticles.asStateFlow()

    private val _eventHistory = MutableStateFlow<List<CollisionEventResult>>(emptyList())
    val eventHistory: StateFlow<List<CollisionEventResult>> = _eventHistory.asStateFlow()

    private val _currentEvent = MutableStateFlow<CollisionEventResult?>(null)
    val currentEvent: StateFlow<CollisionEventResult?> = _currentEvent.asStateFlow()

    val camera = Camera3D()

    private var eventCounter = 1001L
    private var isPendingDetonation = false

    init {
        resetIncomingBeams()

        // 60 FPS high-performance physics tick loop
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
            s.particleA,
            s.particleB,
            s.energyGeV / 2.0,
            s.impactParameterFm
        )
        _liveParticles.value = beams
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
            luminosityScale = s.luminosityScale
        )

        _currentEvent.value = result
        _eventHistory.update { listOf(result) + it.take(49) }
        _liveParticles.value = result.generatedParticles
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
            if (!p.isEscaped && !p.isDecayed) {
                updated.add(p)
                val daughters = p.checkAndTriggerSecondaryDecay()
                if (daughters != null) {
                    newSecondary.addAll(daughters)
                }
            }
        }
        updated.addAll(newSecondary)
        _liveParticles.value = updated
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
        _state.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun resetCamera() {
        camera.reset()
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

    fun clearLogs() {
        _eventHistory.value = emptyList()
        _currentEvent.value = null
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
