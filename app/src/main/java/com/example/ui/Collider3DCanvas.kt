package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physics.FourVector
import com.example.physics.PacketDualityMode
import com.example.physics.Particle3D
import com.example.physics.ParticleCategory
import com.example.physics.ParticlePacket3D
import com.example.physics.Vector3D
import com.example.physics.WavePacket3D
import com.example.rendering.DetectorWireframe
import com.example.rendering.ViewProjectionMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

sealed interface InspectedEntity {
    data class Particle(val p: Particle3D) : InspectedEntity
    data class WavePacket(val wp: WavePacket3D) : InspectedEntity
    data class ParticlePacket(val pp: ParticlePacket3D) : InspectedEntity
}

@Composable
fun Collider3DCanvas(
    viewModel: ColliderViewModel,
    modifier: Modifier = Modifier,
    onExitClick: () -> Unit
) {
    val simState by viewModel.state.collectAsState()
    val particles by viewModel.liveParticles.collectAsState()
    val wavePackets by viewModel.liveWavePackets.collectAsState()
    val particlePackets by viewModel.liveParticlePackets.collectAsState()
    val currentEvent by viewModel.currentEvent.collectAsState()
    val activeCalHits by viewModel.activeCalorimeterHits.collectAsState()
    val camera = viewModel.camera

    val isDark = simState.isDarkTheme

    // Compute background color dynamically from presets or live HSV sliders
    val bgColor = remember(simState.bgPresetIndex, simState.bgHue, simState.bgSaturation, simState.bgBrightness, simState.isDarkTheme) {
        val preset = BackgroundPresets.PRESETS.getOrNull(simState.bgPresetIndex)
        if (preset != null && abs(simState.bgBrightness - (if (preset.isDark) 0.08f else 0.95f)) < 0.03f) {
            preset.color
        } else {
            Color.hsv(
                simState.bgHue.coerceIn(0f, 360f),
                simState.bgSaturation.coerceIn(0f, 1f),
                simState.bgBrightness.coerceIn(0.01f, 1f)
            )
        }
    }

    // Continuous Frame Clock for uninterrupted 60FPS animation rendering
    var frameTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(simState.isPaused) {
        while (!simState.isPaused) {
            withFrameNanos { nanos ->
                frameTick = nanos
            }
        }
    }

    // Selected entity (Particle, Wave Packet, or Particle Packet) for Inspection Bubble
    var selectedEntity by remember { mutableStateOf<InspectedEntity?>(null) }
    var selectedEntityScreenPos by remember { mutableStateOf<Offset?>(null) }

    // Selected category filter in environment HUD (null = all visible)
    var selectedCategoryFilter by remember { mutableStateOf<ParticleCategory?>(null) }

    // Reusable buffers (Zero heap allocation during 60FPS draw)
    val projBuffer1 = remember { FloatArray(3) }
    val projBuffer2 = remember { FloatArray(3) }
    val projBuffer3 = remember { FloatArray(3) }
    val reusablePath = remember { Path() }
    val dashedEffect = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) }
    val fineDashedEffect = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f) }

    // Detector Wireframe geometry cache
    val wireframeLines = remember(
        simState.showTracker, simState.showEcal, simState.showHcal,
        simState.showMuon, simState.gridOverlayEnabled, simState.isDarkTheme
    ) {
        DetectorWireframe.buildDetectorMesh(
            showTracker = simState.showTracker,
            showEcal = simState.showEcal,
            showHcal = simState.showHcal,
            showMuon = simState.showMuon,
            showGrid = simState.gridOverlayEnabled,
            isDarkTheme = simState.isDarkTheme
        )
    }

    var cameraChangeCounter by remember { mutableIntStateOf(0) }
    var canvasWidth by remember { mutableStateOf(1000f) }
    var canvasHeight by remember { mutableStateOf(1000f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            // Combined Multi-touch Gestures: 1-finger orbit rotate, 2-finger pan & pinch zoom, tap selection
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var isDrag = false
                    val touchSlop = viewConfiguration.touchSlop
                    val initialDownPos = down.position

                    do {
                        val event = awaitPointerEvent()
                        val pressedPointers = event.changes.filter { it.pressed }
                        val pointerCount = pressedPointers.size

                        if (pointerCount == 1) {
                            val change = pressedPointers.firstOrNull()
                            if (change != null) {
                                val delta = change.position - change.previousPosition
                                val totalMove = (change.position - initialDownPos).getDistance()

                                if (!isDrag && totalMove > touchSlop) {
                                    isDrag = true
                                }

                                if (isDrag) {
                                    // 1-finger drag rotates 3D scene (orbit)
                                    camera.rotateBy(delta.x * 0.005f, delta.y * 0.005f)
                                    cameraChangeCounter++
                                    change.consume()
                                }
                            }
                        } else if (pointerCount >= 2) {
                            isDrag = true
                            // 2-finger gesture: simultaneous pan and pinch-to-zoom
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            if (zoomChange != 1f) {
                                camera.zoomBy(zoomChange)
                            }
                            if (panChange != Offset.Zero) {
                                camera.panBy(panChange.x, panChange.y)
                            }
                            event.changes.forEach { it.consume() }
                            cameraChangeCounter++
                        }
                    } while (event.changes.any { it.pressed })

                    if (!isDrag) {
                        // User performed a clean TAP! Check if tapped on a particle, wave packet, or particle packet
                        val matrix = camera.buildTransformMatrix(canvasWidth, canvasHeight)
                        val isIso = camera.isIsometricOr2D
                        var closestEntity: InspectedEntity? = null
                        var minDistance = Float.MAX_VALUE
                        val tapThreshold = 65f // touch radius in pixels

                        // Check Particles
                        for (p in particles) {
                            if (matrix.projectToScreenFast(
                                    p.position.x, p.position.y, p.position.z,
                                    canvasWidth, canvasHeight, projBuffer1,
                                    camera.panX, camera.panY,
                                    isIsometric = isIso
                                )
                            ) {
                                val dx = projBuffer1[0] - initialDownPos.x
                                val dy = projBuffer1[1] - initialDownPos.y
                                val dist = sqrt(dx * dx + dy * dy)
                                if (dist < tapThreshold && dist < minDistance) {
                                    minDistance = dist
                                    closestEntity = InspectedEntity.Particle(p)
                                }
                            }
                        }

                        // Check Wave Packets (if enabled)
                        if (simState.enableWavePacketEjection) {
                            for (wp in wavePackets) {
                                if (matrix.projectToScreenFast(
                                        wp.center.x, wp.center.y, wp.center.z,
                                        canvasWidth, canvasHeight, projBuffer1,
                                        camera.panX, camera.panY,
                                        isIsometric = isIso
                                    )
                                ) {
                                    val dx = projBuffer1[0] - initialDownPos.x
                                    val dy = projBuffer1[1] - initialDownPos.y
                                    val dist = sqrt(dx * dx + dy * dy)
                                    if (dist < tapThreshold + 15f && dist < minDistance) {
                                        minDistance = dist
                                        closestEntity = InspectedEntity.WavePacket(wp)
                                    }
                                }
                            }
                        }

                        // Check Particle Packets (if enabled)
                        if (simState.enableParticlePacketEjection) {
                            for (pp in particlePackets) {
                                if (matrix.projectToScreenFast(
                                        pp.centroid.x, pp.centroid.y, pp.centroid.z,
                                        canvasWidth, canvasHeight, projBuffer1,
                                        camera.panX, camera.panY,
                                        isIsometric = isIso
                                    )
                                ) {
                                    val dx = projBuffer1[0] - initialDownPos.x
                                    val dy = projBuffer1[1] - initialDownPos.y
                                    val dist = sqrt(dx * dx + dy * dy)
                                    if (dist < tapThreshold + 20f && dist < minDistance) {
                                        minDistance = dist
                                        closestEntity = InspectedEntity.ParticlePacket(pp)
                                    }
                                }
                            }
                        }

                        selectedEntity = closestEntity
                    }
                }
            }
    ) {
        // High-Precision Crisp CERN-Style 3D/2D Event Display (No Blurry Glow)
        Canvas(modifier = Modifier.fillMaxSize().testTag("3d_collider_canvas")) {
            canvasWidth = size.width
            canvasHeight = size.height

            @Suppress("UNUSED_VARIABLE")
            val currentFrame = frameTick
            @Suppress("UNUSED_VARIABLE")
            val trigger = cameraChangeCounter

            val matrix = camera.buildTransformMatrix(canvasWidth, canvasHeight)
            val isIso = camera.isIsometricOr2D

            // 1. Draw Detector Wireframe Mesh
            if (simState.wireframeEnabled) {
                for (segment in wireframeLines) {
                    val p0 = segment.start
                    val p1 = segment.end
                    val ok0 = matrix.projectToScreenFast(
                        p0.x, p0.y, p0.z,
                        canvasWidth, canvasHeight, projBuffer1,
                        camera.panX, camera.panY,
                        isIsometric = isIso
                    )
                    val ok1 = matrix.projectToScreenFast(
                        p1.x, p1.y, p1.z,
                        canvasWidth, canvasHeight, projBuffer2,
                        camera.panX, camera.panY,
                        isIsometric = isIso
                    )

                    if (ok0 && ok1) {
                        drawLine(
                            color = segment.color,
                            start = Offset(projBuffer1[0], projBuffer1[1]),
                            end = Offset(projBuffer2[0], projBuffer2[1]),
                            strokeWidth = segment.strokeWidth
                        )
                    }
                }
            }

            // 2. Draw Dynamic Calorimeter Hits (Only appears as particles arrive at detector cylinders)
            for (hit in activeCalHits) {
                if (matrix.projectToScreenFast(
                        hit.position.x, hit.position.y, hit.position.z,
                        canvasWidth, canvasHeight, projBuffer1,
                        camera.panX, camera.panY,
                        isIsometric = isIso
                    )
                ) {
                    val hx = projBuffer1[0]
                    val hy = projBuffer1[1]
                    val hScale = projBuffer1[2]
                    val towerRadius = (4.0f + (hit.energyGeV.toFloat() * 0.35f).coerceIn(2f, 18f)) * hScale

                    drawCircle(
                        color = hit.color.copy(alpha = 0.85f),
                        center = Offset(hx, hy),
                        radius = towerRadius
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.90f),
                        center = Offset(hx, hy),
                        radius = towerRadius * 0.4f,
                        style = Stroke(width = 1.5f)
                    )
                }
            }

            // 3. Draw Missing Transverse Energy Vector Arrow (Neutrino E_T_miss)
            currentEvent?.let { ev ->
                if (ev.missingETGeV > 2.0) {
                    val missVec = ev.missingETVector.normalized() * 9.0f
                    val ok0 = matrix.projectToScreenFast(
                        0f, 0f, 0f,
                        canvasWidth, canvasHeight, projBuffer1,
                        camera.panX, camera.panY,
                        isIsometric = isIso
                    )
                    val ok1 = matrix.projectToScreenFast(
                        missVec.x, missVec.y, missVec.z,
                        canvasWidth, canvasHeight, projBuffer2,
                        camera.panX, camera.panY,
                        isIsometric = isIso
                    )

                    if (ok0 && ok1) {
                        drawLine(
                            color = Color(0xFF69F0AE),
                            start = Offset(projBuffer1[0], projBuffer1[1]),
                            end = Offset(projBuffer2[0], projBuffer2[1]),
                            strokeWidth = 3f * projBuffer1[2].coerceIn(0.8f, 2.5f),
                            pathEffect = dashedEffect,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // 4. Draw Collision Vertex (0,0,0) Origin Mark (Crisp crosshair star)
            val vertexOk = matrix.projectToScreenFast(
                0f, 0f, 0f,
                canvasWidth, canvasHeight, projBuffer1,
                camera.panX, camera.panY,
                isIsometric = isIso
            )
            var vertexScreenX = 0f
            var vertexScreenY = 0f
            if (vertexOk) {
                vertexScreenX = projBuffer1[0]
                vertexScreenY = projBuffer1[1]
                val starSize = 10f * projBuffer1[2].coerceIn(0.8f, 2.5f)

                // Clean vertex crosshair
                drawLine(
                    color = Color.White,
                    start = Offset(vertexScreenX - starSize, vertexScreenY),
                    end = Offset(vertexScreenX + starSize, vertexScreenY),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(vertexScreenX, vertexScreenY - starSize),
                    end = Offset(vertexScreenX, vertexScreenY + starSize),
                    strokeWidth = 2f
                )
                drawCircle(
                    color = Color(0xFF00E5FF),
                    center = Offset(vertexScreenX, vertexScreenY),
                    radius = 3.5f * projBuffer1[2]
                )
            }

            var currentSelectedScreenPos: Offset? = null

            // 5. DRAW COLLIMATED PARTICLE PACKETS / BUNCHES EJECTED AFTER COLLISION
            val showParticlePackets = simState.enableParticlePacketEjection &&
                    simState.packetDualityMode != PacketDualityMode.WAVE_PACKETS_ONLY &&
                    simState.packetDualityMode != PacketDualityMode.CLASSICAL_TRACKS

            if (showParticlePackets) {
                for (pp in particlePackets) {
                    val isSelected = selectedEntity is InspectedEntity.ParticlePacket &&
                            (selectedEntity as InspectedEntity.ParticlePacket).pp.id == pp.id

                    val okCentroid = matrix.projectToScreenFast(
                        pp.centroid.x, pp.centroid.y, pp.centroid.z,
                        canvasWidth, canvasHeight, projBuffer1,
                        camera.panX, camera.panY,
                        isIsometric = isIso
                    )

                    if (okCentroid) {
                        val cx = projBuffer1[0]
                        val cy = projBuffer1[1]
                        val cScale = projBuffer1[2]
                        val bunchPixelRadius = (pp.bunchRadius * 28.0f * cScale).coerceIn(16f, 110f)

                        if (isSelected) {
                            currentSelectedScreenPos = Offset(cx, cy)
                            // White targeting reticle
                            drawCircle(
                                color = Color.White,
                                center = Offset(cx, cy),
                                radius = bunchPixelRadius + 10f,
                                style = Stroke(width = 2.0f, pathEffect = dashedEffect)
                            )
                        }

                        // Draw Jet Cone from Vertex (0,0,0) to bunch perimeter if vertex is visible
                        if (vertexOk) {
                            val dirScreen = Offset(cx - vertexScreenX, cy - vertexScreenY)
                            val distScreen = dirScreen.getDistance()
                            if (distScreen > 10f) {
                                val normScreen = Offset(-dirScreen.y / distScreen, dirScreen.x / distScreen)
                                val edge1 = Offset(cx + normScreen.x * bunchPixelRadius, cy + normScreen.y * bunchPixelRadius)
                                val edge2 = Offset(cx - normScreen.x * bunchPixelRadius, cy - normScreen.y * bunchPixelRadius)

                                drawLine(
                                    color = pp.color.copy(alpha = if (isSelected) 0.55f else 0.28f),
                                    start = Offset(vertexScreenX, vertexScreenY),
                                    end = edge1,
                                    strokeWidth = 1.5f,
                                    pathEffect = fineDashedEffect
                                )
                                drawLine(
                                    color = pp.color.copy(alpha = if (isSelected) 0.55f else 0.28f),
                                    start = Offset(vertexScreenX, vertexScreenY),
                                    end = edge2,
                                    strokeWidth = 1.5f,
                                    pathEffect = fineDashedEffect
                                )
                            }
                        }

                        // Draw Transverse Bunch Cluster Boundary Ring
                        drawCircle(
                            color = pp.color.copy(alpha = if (isSelected) 0.85f else 0.50f),
                            center = Offset(cx, cy),
                            radius = bunchPixelRadius,
                            style = Stroke(width = if (isSelected) 2.5f else 1.8f, pathEffect = dashedEffect)
                        )

                        // Draw Packet Centroid Marker (Diamond)
                        val diamondSize = 6.0f * cScale
                        val diamondPath = reusablePath
                        diamondPath.reset()
                        diamondPath.moveTo(cx, cy - diamondSize)
                        diamondPath.lineTo(cx + diamondSize, cy)
                        diamondPath.lineTo(cx, cy + diamondSize)
                        diamondPath.lineTo(cx - diamondSize, cy)
                        diamondPath.close()

                        drawPath(
                            path = diamondPath,
                            color = pp.color.copy(alpha = 0.95f)
                        )
                        drawPath(
                            path = diamondPath,
                            color = Color.White,
                            style = Stroke(width = 1.2f)
                        )
                    }
                }
            }

            // 6. DRAW QUANTUM WAVE PACKETS EJECTED AFTER COLLISION
            val showWavePackets = simState.enableWavePacketEjection &&
                    simState.packetDualityMode != PacketDualityMode.PARTICLE_PACKETS_ONLY &&
                    simState.packetDualityMode != PacketDualityMode.CLASSICAL_TRACKS

            if (showWavePackets) {
                for (wp in wavePackets) {
                    val isSelected = selectedEntity is InspectedEntity.WavePacket &&
                            (selectedEntity as InspectedEntity.WavePacket).wp.id == wp.id

                    if (wp.isSphericalCoherentWave) {
                        // Draw expanding spherical coherent vacuum wave from vertex
                        if (vertexOk) {
                            val baseRadius = (wp.center.magnitude() + wp.envelopeWidth * 1.8f) * 35.0f * projBuffer1[2]
                            val waveAlpha = (0.55f * wp.amplitude).coerceIn(0.12f, 0.75f)

                            for (i in 0..2) {
                                val rippleOffset = ((wp.phase + i * 2.0f) % 6.0f) * 6.0f
                                val ringR = (baseRadius + rippleOffset).coerceIn(5f, 250f)
                                drawCircle(
                                    color = wp.color.copy(alpha = waveAlpha * (1f - (i * 0.25f))),
                                    center = Offset(vertexScreenX, vertexScreenY),
                                    radius = ringR,
                                    style = Stroke(width = 1.5f, pathEffect = dashedEffect)
                                )
                            }
                        }
                    } else {
                        // Directional De Broglie Wave Packet
                        val okCenter = matrix.projectToScreenFast(
                            wp.center.x, wp.center.y, wp.center.z,
                            canvasWidth, canvasHeight, projBuffer1,
                            camera.panX, camera.panY,
                            isIsometric = isIso
                        )

                        // Project forward along momentum vector
                        val forwardDir = if (wp.momentum.magnitude() > 0.01f) wp.momentum.normalized() else Vector3D(0f, 1f, 0f)
                        val forwardPoint = wp.center + (forwardDir * 0.8f)
                        val okForward = matrix.projectToScreenFast(
                            forwardPoint.x, forwardPoint.y, forwardPoint.z,
                            canvasWidth, canvasHeight, projBuffer2,
                            camera.panX, camera.panY,
                            isIsometric = isIso
                        )

                        if (okCenter && okForward) {
                            val wx = projBuffer1[0]
                            val wy = projBuffer1[1]
                            val wScale = projBuffer1[2]

                            if (isSelected) {
                                currentSelectedScreenPos = Offset(wx, wy)
                            }

                            val dirX = projBuffer2[0] - wx
                            val dirY = projBuffer2[1] - wy
                            val dirLen = sqrt(dirX * dirX + dirY * dirY).coerceAtLeast(1.0f)
                            val uDirX = dirX / dirLen
                            val uDirY = dirY / dirLen
                            // Perpendicular unit vector on screen
                            val perpX = -uDirY
                            val perpY = uDirX

                            val packetPixelWidth = (wp.envelopeWidth * 32.0f * wScale).coerceIn(14f, 80f)
                            val packetPixelTransverse = packetPixelWidth * 0.65f
                            val waveAlpha = (0.75f * wp.amplitude).coerceIn(0.18f, 0.95f)

                            // Draw Oscillating Wavefront Crests / Phase Ripples (sinusoidal crest lines)
                            val wavelengthPx = (wp.deBroglieWavelengthMeters * 24.0f * wScale).coerceIn(10f, 45f)
                            val crestCount = 4

                            for (c in -2..2) {
                                // Phase shift moves crests continuously outward through the Gaussian envelope
                                val phaseOffset = ((wp.phase / (2f * PI.toFloat())) * wavelengthPx)
                                val crestDist = (c * wavelengthPx + phaseOffset)
                                val crestCenter = Offset(wx + uDirX * crestDist, wy + uDirY * crestDist)

                                // Gaussian amplitude modulation factor: exp(-d^2 / 2σ^2)
                                val normDist = (crestDist / packetPixelWidth).toDouble()
                                val gaussWeight = kotlin.math.exp(-(normDist * normDist) * 1.5).toFloat().coerceIn(0.15f, 1.0f)
                                val crestHalfWidth = packetPixelTransverse * gaussWeight

                                val pLeft = Offset(crestCenter.x + perpX * crestHalfWidth, crestCenter.y + perpY * crestHalfWidth)
                                val pRight = Offset(crestCenter.x - perpX * crestHalfWidth, crestCenter.y - perpY * crestHalfWidth)

                                // Wavefront arc with slight curve
                                val arcMid = Offset(crestCenter.x + uDirX * (crestHalfWidth * 0.25f), crestCenter.y + uDirY * (crestHalfWidth * 0.25f))
                                reusablePath.reset()
                                reusablePath.moveTo(pLeft.x, pLeft.y)
                                reusablePath.quadraticTo(arcMid.x, arcMid.y, pRight.x, pRight.y)

                                drawPath(
                                    path = reusablePath,
                                    color = wp.color.copy(alpha = (waveAlpha * gaussWeight).coerceIn(0.01f, 1.0f)),
                                    style = Stroke(
                                        width = ((if (isSelected) 2.5f else 1.8f) * gaussWeight).coerceIn(1.0f, 6.0f),
                                        cap = StrokeCap.Round
                                    )
                                )
                            }

                            // Draw Gaussian Wavepacket Boundary Ellipse (dashed envelope)
                            drawCircle(
                                color = wp.color.copy(alpha = if (isSelected) 0.85f else 0.35f * waveAlpha),
                                center = Offset(wx, wy),
                                radius = packetPixelWidth,
                                style = Stroke(width = if (isSelected) 2.2f else 1.2f, pathEffect = fineDashedEffect)
                            )

                            // Quantum Core Point
                            drawCircle(
                                color = Color.White.copy(alpha = 0.95f),
                                center = Offset(wx, wy),
                                radius = 3.5f * wScale
                            )
                            drawCircle(
                                color = wp.color.copy(alpha = 0.90f),
                                center = Offset(wx, wy),
                                radius = 2.0f * wScale
                            )

                            if (isSelected) {
                                drawCircle(
                                    color = Color.White,
                                    center = Offset(wx, wy),
                                    radius = packetPixelWidth + 8f,
                                    style = Stroke(width = 2.0f, pathEffect = dashedEffect)
                                )
                            }
                        }
                    }
                }
            }

            // 7. DRAW PARTICLES AND CRISP REALISTICALLY SIZED PARTICLES (NO BLURRY GLOW)
            val showParticles = simState.packetDualityMode != PacketDualityMode.WAVE_PACKETS_ONLY

            if (showParticles) {
                for (p in particles) {
                    val isSelected = selectedEntity is InspectedEntity.Particle &&
                            (selectedEntity as InspectedEntity.Particle).p.id == p.id
                    val matchesFilter = selectedCategoryFilter == null || p.species.category == selectedCategoryFilter

                    // Determine crisp track color from classification system
                    val pColor = when (simState.trailColorMode) {
                        TrailColorMode.SPECIES_COLOR -> p.colorOverride ?: p.species.color
                        TrailColorMode.ENERGY_HEATMAP -> {
                            val pT = p.transverseMomentum()
                            when {
                                pT > 60f -> Color(0xFFFF1744)  // High pT = Crimson Red
                                pT > 20f -> Color(0xFFFFD600)  // Medium pT = Gold Yellow
                                pT > 5f -> Color(0xFF00E5FF)   // Moderate pT = Cyan
                                else -> Color(0xFF7C4DFF)      // Low pT = Violet
                            }
                        }
                        TrailColorMode.CHARGE_COLOR -> {
                            when {
                                p.charge > 0.05 -> Color(0xFFFF5252)   // Positive = Red
                                p.charge < -0.05 -> Color(0xFF40C4FF)  // Negative = Cyan/Blue
                                else -> Color(0xFFFFD600)             // Neutral = Yellow
                            }
                        }
                    }

                    val history = p.trajectoryHistory

                    // Draw clean crisp trajectory line (Single crisp pass, NO blurry outer glow)
                    if (history.size > 1) {
                        reusablePath.reset()
                        var firstPoint = true

                        for (pt in history) {
                            if (matrix.projectToScreenFast(
                                    pt.x, pt.y, pt.z,
                                    canvasWidth, canvasHeight, projBuffer1,
                                    camera.panX, camera.panY,
                                    isIsometric = isIso
                                )
                            ) {
                                if (firstPoint) {
                                    reusablePath.moveTo(projBuffer1[0], projBuffer1[1])
                                    firstPoint = false
                                } else {
                                    reusablePath.lineTo(projBuffer1[0], projBuffer1[1])
                                }
                            }
                        }

                        if (!firstPoint) {
                            val baseWidth = if (isSelected) 4.5f else 2.5f
                            val strokeWidth = (baseWidth * simState.trailWidth).coerceIn(1.5f, 10f)
                            val trackAlpha = if (matchesFilter) {
                                if (isSelected) 1.0f else simState.trailAlpha
                            } else {
                                0.15f // Dim non-matching categories
                            }

                            drawPath(
                                path = reusablePath,
                                color = pColor.copy(alpha = trackAlpha),
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    // Draw Clean Particle Head Sphere with Mathematically Correct Physical Size
                    val distFromVertex = p.position.magnitude()
                    val isAtVertexInstant = p.generation != 0 && distFromVertex < 0.22f

                    if ((!isAtVertexInstant || isSelected) && matrix.projectToScreenFast(
                            p.position.x, p.position.y, p.position.z,
                            canvasWidth, canvasHeight, projBuffer1,
                            camera.panX, camera.panY,
                            isIsometric = isIso
                        )
                    ) {
                        val hx = projBuffer1[0]
                        val hy = projBuffer1[1]
                        val hScale = projBuffer1[2]

                        // Realistic particle size based on subatomic physics classification:
                        val baseRadius = if (isSelected) 8.5f else 5.5f
                        val sizeMultiplier = p.species.renderRadiusMultiplier
                        val radius = (baseRadius * sizeMultiplier * hScale).coerceIn(3.0f, 26f)

                        val headAlpha = if (matchesFilter) 1.0f else 0.20f

                        // Track selected particle screen position for Name Bubble
                        if (isSelected) {
                            currentSelectedScreenPos = Offset(hx, hy)

                            // Draw clean selection targeting reticle
                            drawCircle(
                                color = Color.White,
                                center = Offset(hx, hy),
                                radius = radius + 8f,
                                style = Stroke(width = 2.0f, pathEffect = dashedEffect)
                            )
                        }

                        // Solid clean particle head
                        drawCircle(
                            color = pColor.copy(alpha = headAlpha),
                            center = Offset(hx, hy),
                            radius = radius
                        )
                        // Sharp specular highlight dot
                        if (headAlpha > 0.5f) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.9f),
                                center = Offset(hx - radius * 0.3f, hy - radius * 0.3f),
                                radius = radius * 0.35f
                            )
                        }
                    }
                }
            }

            selectedEntityScreenPos = currentSelectedScreenPos
        }

        // Unified Name Bubble Inspector on Tap (Handles Particle, Wave Packet, or Particle Packet)
        selectedEntity?.let { entity: InspectedEntity ->
            selectedEntityScreenPos?.let { bubblePos: Offset ->
                UnifiedInspectorBubble(
                    entity = entity,
                    magneticFieldTesla = simState.magneticFieldTesla,
                    screenPos = bubblePos,
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    isDark = isDark,
                    onDismiss = { selectedEntity = null }
                )
            }
        }

        // Overlay Top Bar & Environmental Color Classification Legend
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .align(Alignment.TopStart)
        ) {
            // Main Control Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF131C31).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.94f),
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Theme Toggle
                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                                contentDescription = "Toggle Theme",
                                tint = if (isDark) Color(0xFFFFD600) else Color(0xFF37474F)
                            )
                        }

                        // Wireframe Toggle
                        IconButton(
                            onClick = { viewModel.toggleWireframe() },
                            modifier = Modifier.testTag("wireframe_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = "Toggle Wireframe",
                                tint = if (simState.wireframeEnabled) Color(0xFF00E5FF) else Color.Gray
                            )
                        }

                        // Wave Packet Ejection Toggle Button
                        IconButton(
                            onClick = { viewModel.setEnableWavePacketEjection(!simState.enableWavePacketEjection) },
                            modifier = Modifier.testTag("toggle_wave_packets_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Waves,
                                contentDescription = "Toggle Wave Packets",
                                tint = if (simState.enableWavePacketEjection) Color(0xFF00E5FF) else Color.Gray
                            )
                        }

                        // Particle Packet Ejection Toggle Button
                        IconButton(
                            onClick = { viewModel.setEnableParticlePacketEjection(!simState.enableParticlePacketEjection) },
                            modifier = Modifier.testTag("toggle_particle_packets_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Grain,
                                contentDescription = "Toggle Particle Packets",
                                tint = if (simState.enableParticlePacketEjection) Color(0xFFFF9100) else Color.Gray
                            )
                        }

                        // Cycle Packet Duality Mode
                        IconButton(
                            onClick = {
                                val nextMode = when (simState.packetDualityMode) {
                                    PacketDualityMode.DUAL_WAVE_PARTICLE -> PacketDualityMode.WAVE_PACKETS_ONLY
                                    PacketDualityMode.WAVE_PACKETS_ONLY -> PacketDualityMode.PARTICLE_PACKETS_ONLY
                                    PacketDualityMode.PARTICLE_PACKETS_ONLY -> PacketDualityMode.CLASSICAL_TRACKS
                                    PacketDualityMode.CLASSICAL_TRACKS -> PacketDualityMode.DUAL_WAVE_PARTICLE
                                    else -> PacketDualityMode.DUAL_WAVE_PARTICLE
                                }
                                viewModel.setPacketDualityMode(nextMode)
                            },
                            modifier = Modifier.testTag("cycle_duality_mode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AllInclusive,
                                contentDescription = "Cycle Duality Mode",
                                tint = Color(0xFFE040FB)
                            )
                        }

                        // Zoom In Button
                        IconButton(
                            onClick = {
                                camera.zoomIn()
                                cameraChangeCounter++
                            },
                            modifier = Modifier.testTag("zoom_in_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Zoom In",
                                tint = Color(0xFF00E5FF)
                            )
                        }

                        // Zoom Out Button
                        IconButton(
                            onClick = {
                                camera.zoomOut()
                                cameraChangeCounter++
                            },
                            modifier = Modifier.testTag("zoom_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = "Zoom Out",
                                tint = Color(0xFFFF9100)
                            )
                        }

                        // Reset Camera & Viewport
                        IconButton(
                            onClick = {
                                viewModel.resetCamera()
                                cameraChangeCounter++
                            },
                            modifier = Modifier.testTag("reset_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Camera View",
                                tint = if (isDark) Color.White else Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Projection, Zoom & Duality Mode Readout
                        Text(
                            text = "[${simState.projectionMode.shortName}] %.1fx • ${simState.packetDualityMode.shortName}".format(camera.zoom),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (isDark) Color(0xFF80DEEA) else Color(0xFF0277BD)
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Exit Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFF1744).copy(alpha = 0.90f),
                    onClick = onExitClick,
                    modifier = Modifier.testTag("exit_app_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Application",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "EXIT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ========================================================
            // REVAMPED INFORMATIVE VIEWPORT TABS (HIGH-TECH CERN HUD)
            // ========================================================
            var activeViewportTab by remember { mutableStateOf<ViewportInfoTab?>(null) }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.90f) else Color.White.copy(alpha = 0.92f),
                tonalElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                    // Viewport Tab Chips Row
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "HUD TABS:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                color = Color.Gray
                            )
                        )

                        // Tab 1: Telemetry
                        ViewportTabChip(
                            title = "📊 TELEMETRY",
                            isSelected = activeViewportTab == ViewportInfoTab.TELEMETRY,
                            accentColor = Color(0xFF00E5FF),
                            onClick = {
                                activeViewportTab = if (activeViewportTab == ViewportInfoTab.TELEMETRY) null else ViewportInfoTab.TELEMETRY
                            }
                        )

                        // Tab 2: Beams & Speeds
                        ViewportTabChip(
                            title = "🚀 BEAMS (%.2fc / %.2fc)".format(simState.speedA, simState.speedB),
                            isSelected = activeViewportTab == ViewportInfoTab.BEAMS,
                            accentColor = Color(0xFFFFD600),
                            onClick = {
                                activeViewportTab = if (activeViewportTab == ViewportInfoTab.BEAMS) null else ViewportInfoTab.BEAMS
                            }
                        )

                        // Tab 3: Quantum Packets
                        ViewportTabChip(
                            title = "⚛ PACKETS (ψ=${wavePackets.size} / ::=${particlePackets.size})",
                            isSelected = activeViewportTab == ViewportInfoTab.PACKETS,
                            accentColor = Color(0xFFE040FB),
                            onClick = {
                                activeViewportTab = if (activeViewportTab == ViewportInfoTab.PACKETS) null else ViewportInfoTab.PACKETS
                            }
                        )

                        // Tab 4: Detector Layers
                        ViewportTabChip(
                            title = "🛡 LAYERS (${activeCalHits.size} Hits)",
                            isSelected = activeViewportTab == ViewportInfoTab.DETECTORS,
                            accentColor = Color(0xFF00E676),
                            onClick = {
                                activeViewportTab = if (activeViewportTab == ViewportInfoTab.DETECTORS) null else ViewportInfoTab.DETECTORS
                            }
                        )

                        // Tab 5: Species Filter
                        ViewportTabChip(
                            title = "SPECIES (${particles.size})",
                            isSelected = activeViewportTab == ViewportInfoTab.SPECIES,
                            accentColor = Color(0xFFFF4081),
                            onClick = {
                                activeViewportTab = if (activeViewportTab == ViewportInfoTab.SPECIES) null else ViewportInfoTab.SPECIES
                            }
                        )
                    }

                    // Expanded Informative Tab Pane
                    activeViewportTab?.let { tab ->
                        Spacer(modifier = Modifier.height(4.dp))
                        when (tab) {
                            ViewportInfoTab.TELEMETRY -> {
                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ViewportStatBadge("√s ENERGY", "%.1f GeV (%.2f TeV)".format(simState.energyGeV, simState.energyGeV / 1000.0), Color(0xFF00E5FF))
                                    ViewportStatBadge("PROCESS", currentEvent?.primaryProcessName ?: "Incoming Flight", Color(0xFF00E5FF))
                                    ViewportStatBadge("TRACKS", "${particles.size} (${currentEvent?.chargedMultiplicity ?: 0} ch)", Color(0xFF00E676))
                                    ViewportStatBadge("M_inv", "%.2f GeV/c²".format(currentEvent?.invariantMassGeV ?: 0.0), Color(0xFFE040FB))
                                    ViewportStatBadge("E_T,miss", "%.1f GeV".format(currentEvent?.missingETGeV ?: 0.0), Color(0xFF69F0AE))
                                    ViewportStatBadge("CHARGE ΔQ", if (currentEvent != null) "%+.0f→%+.0f [EXACT]".format(currentEvent?.initialCharge ?: 0.0, currentEvent?.finalCharge ?: 0.0) else "EXACT", Color(0xFFFF9100))
                                }
                            }
                            ViewportInfoTab.BEAMS -> {
                                val gammaA = 1.0f / sqrt((1.0f - simState.speedA * simState.speedA).coerceAtLeast(0.0001f))
                                val gammaB = 1.0f / sqrt((1.0f - simState.speedB * simState.speedB).coerceAtLeast(0.0001f))
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ViewportStatBadge("BEAM A (${simState.particleA.symbol})", "v_A = %.3f c (γ=%.2f)".format(simState.speedA, gammaA), Color(0xFF00E5FF))
                                        ViewportStatBadge("BEAM B (${simState.particleB.symbol})", "v_B = %.3f c (γ=%.2f)".format(simState.speedB, gammaB), Color(0xFFFF9100))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = (if (simState.syncBeamSpeeds) Color(0xFF00E5FF) else Color.Gray).copy(alpha = 0.20f),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { viewModel.setSyncBeamSpeeds(!simState.syncBeamSpeeds) }
                                        ) {
                                            Text(
                                                text = if (simState.syncBeamSpeeds) "SYNC: LINKED" else "SYNC: ASYMMETRIC",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 8.5.sp,
                                                    color = if (simState.syncBeamSpeeds) Color(0xFF00E5FF) else Color.Gray
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    // Quick Speed Preset Chips
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("SET SPEED:", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = Color.Gray))
                                        listOf("0.50c" to 0.50f, "0.85c" to 0.85f, "0.95c" to 0.95f, "0.999c" to 0.999f).forEach { (lbl, spd) ->
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF334155),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .clickable {
                                                        viewModel.setSpeedA(spd)
                                                        viewModel.setSpeedB(spd)
                                                    }
                                            ) {
                                                Text(
                                                    text = lbl,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.5.sp, color = Color.White),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            ViewportInfoTab.PACKETS -> {
                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ViewportStatBadge("ψ WAVES", "${wavePackets.size} active (λ_dB = 0.4-1.8m)", Color(0xFF00E5FF))
                                    ViewportStatBadge(":: PACKETS", "${particlePackets.size} collimated bunches", Color(0xFFFF9100))
                                    ViewportStatBadge("DUALITY MODE", simState.packetDualityMode.displayName, Color(0xFFE040FB))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFE040FB).copy(alpha = 0.20f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable {
                                                val nextMode = when (simState.packetDualityMode) {
                                                    PacketDualityMode.DUAL_WAVE_PARTICLE -> PacketDualityMode.WAVE_PACKETS_ONLY
                                                    PacketDualityMode.WAVE_PACKETS_ONLY -> PacketDualityMode.PARTICLE_PACKETS_ONLY
                                                    PacketDualityMode.PARTICLE_PACKETS_ONLY -> PacketDualityMode.CLASSICAL_TRACKS
                                                    else -> PacketDualityMode.DUAL_WAVE_PARTICLE
                                                }
                                                viewModel.setPacketDualityMode(nextMode)
                                            }
                                    ) {
                                        Text(
                                            text = "CYCLE DUALITY",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = Color(0xFFE040FB)),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            ViewportInfoTab.DETECTORS -> {
                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DetectorLayerToggleChip("TRACKER", simState.showTracker, Color(0xFF7C4DFF)) { viewModel.toggleTrackerLayer() }
                                    DetectorLayerToggleChip("ECAL", simState.showEcal, Color(0xFF00E676)) { viewModel.toggleEcalLayer() }
                                    DetectorLayerToggleChip("HCAL", simState.showHcal, Color(0xFFFF9100)) { viewModel.toggleHcalLayer() }
                                    DetectorLayerToggleChip("MUON", simState.showMuon, Color(0xFFFF1744)) { viewModel.toggleMuonLayer() }
                                    DetectorLayerToggleChip("GRID", simState.gridOverlayEnabled, Color(0xFF90A4AE)) { viewModel.toggleGrid() }
                                    DetectorLayerToggleChip("WIREFRAME", simState.wireframeEnabled, Color(0xFF00E5FF)) { viewModel.toggleWireframe() }
                                }
                            }
                            ViewportInfoTab.SPECIES -> {
                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ClassificationFilterChip("ALL", particles.size, if (isDark) Color.White else Color.Black, selectedCategoryFilter == null) {
                                        selectedCategoryFilter = null
                                    }
                                    listOf(ParticleCategory.LEPTON, ParticleCategory.MESON, ParticleCategory.BARYON, ParticleCategory.GAUGE_BOSON, ParticleCategory.HIGGS_BOSON, ParticleCategory.NUCLEUS).forEach { cat ->
                                        val count = particles.count { it.species.category == cat }
                                        ClassificationFilterChip(cat.displayName, count, cat.badgeColor, selectedCategoryFilter == cat) {
                                            selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Left Control Overlay (Play/Pause/Step & Physics Indicators)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp)),
            color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.94f),
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.togglePause() },
                    modifier = Modifier.size(32.dp).testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (simState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Play/Pause Simulation",
                        tint = if (isDark) Color(0xFF00E5FF) else Color(0xFF0288D1),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.stepSingleFrame() },
                    enabled = simState.isPaused,
                    modifier = Modifier.size(32.dp).testTag("step_frame_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Step Single Frame",
                        tint = if (simState.isPaused) (if (isDark) Color.White else Color.Black) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Text(
                        text = "TIME: %.4f c • v_A: %.2fc, v_B: %.2fc".format(simState.timeScale, simState.speedA, simState.speedB),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            color = if (isDark) Color(0xFFFFD600) else Color(0xFFE65100)
                        )
                    )
                    Text(
                        text = "TRACKS: ${particles.size} | ψ WAVES: ${wavePackets.size} | PKTS: ${particlePackets.size} | √s: %.1f TeV".format(simState.energyGeV / 1000.0),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            color = if (isDark) Color.LightGray else Color.DarkGray
                        )
                    )
                }
            }
        }
    }
}

enum class ViewportInfoTab(val label: String) {
    TELEMETRY("TELEMETRY"),
    BEAMS("BEAMS & SPEEDS"),
    PACKETS("QUANTUM PACKETS"),
    DETECTORS("DETECTORS"),
    SPECIES("SPECIES FILTER")
}

@Composable
fun ViewportTabChip(
    title: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.25f) else Color(0xFF1E293B).copy(alpha = 0.6f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, accentColor) else null,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 9.sp,
                color = if (isSelected) accentColor else Color.LightGray
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
        )
    }
}

@Composable
fun ViewportStatBadge(
    label: String,
    value: String,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(0.6.dp, accentColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = Color.Gray
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.5.sp,
                    color = accentColor
                )
            )
        }
    }
}

@Composable
fun DetectorLayerToggleChip(
    name: String,
    isEnabled: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isEnabled) color.copy(alpha = 0.22f) else Color(0xFF1E293B).copy(alpha = 0.6f),
        border = if (isEnabled) androidx.compose.foundation.BorderStroke(1.dp, color) else null,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isEnabled) color else Color.Gray)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 8.5.sp,
                    color = if (isEnabled) color else Color.Gray
                )
            )
        }
    }
}

/**
 * Interactive Species Classification Chip shown in 3D environment HUD
 */
@Composable
fun ClassificationFilterChip(
    name: String,
    count: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color.copy(alpha = 0.25f) else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, color) else null,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (count > 0) "$name ($count)" else name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 9.5.sp,
                    color = color
                )
            )
        }
    }
}

/**
 * Unified Inspector Bubble: Dispatches to Particle, Wave Packet, or Particle Packet Inspection Card
 */
@Composable
fun UnifiedInspectorBubble(
    entity: InspectedEntity,
    magneticFieldTesla: Float,
    screenPos: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    when (entity) {
        is InspectedEntity.Particle -> {
            ParticleNameBubble(
                particle = entity.p,
                magneticFieldTesla = magneticFieldTesla,
                screenPos = screenPos,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                isDark = isDark,
                onDismiss = onDismiss
            )
        }
        is InspectedEntity.WavePacket -> {
            WavePacketNameBubble(
                wavePacket = entity.wp,
                screenPos = screenPos,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                isDark = isDark,
                onDismiss = onDismiss
            )
        }
        is InspectedEntity.ParticlePacket -> {
            ParticlePacketNameBubble(
                packet = entity.pp,
                screenPos = screenPos,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                isDark = isDark,
                onDismiss = onDismiss
            )
        }
    }
}

/**
 * Interactive Particle Name Bubble displayed after tapping any particle track in 3D
 */
@Composable
fun ParticleNameBubble(
    particle: Particle3D,
    magneticFieldTesla: Float,
    screenPos: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val fv = FourVector.fromParticle(particle.species, particle.momentum)
    val pt = particle.transverseMomentum()
    val pMag = particle.momentum.magnitude()
    val energy = particle.totalEnergyGeV()

    val curvatureMeters = if (abs(particle.charge) > 0.01 && magneticFieldTesla > 0.05f) {
        (pt / (abs(particle.charge).toFloat() * magneticFieldTesla * 0.3f))
    } else {
        Float.POSITIVE_INFINITY
    }

    val bubbleWidth = 240f
    val bubbleHeight = 185f

    val clampX = (screenPos.x - bubbleWidth / 2f).coerceIn(16f, (canvasWidth - bubbleWidth - 16f).coerceAtLeast(16f))
    val clampY = if (screenPos.y > bubbleHeight + 40f) {
        screenPos.y - bubbleHeight - 20f
    } else {
        screenPos.y + 25f
    }.coerceIn(16f, (canvasHeight - bubbleHeight - 16f).coerceAtLeast(16f))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    ) {
        Surface(
            modifier = Modifier
                .offset { IntOffset(clampX.roundToInt(), clampY.roundToInt()) }
                .widthIn(max = 250.dp)
                .testTag("particle_name_bubble"),
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF131D33).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.96f),
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, particle.species.color)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Header with Species Dot, Name, Symbol and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(particle.species.color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${particle.species.name} (${particle.species.symbol})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = particle.species.color,
                                fontSize = 12.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Bubble",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = "${particle.species.category.displayName} • " + when (particle.generation) {
                        0 -> "Incoming Beam Track"
                        1 -> "Primary Collision Shower"
                        else -> "Secondary Displaced Daughter"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                BubbleDetailRow("Electric Charge:", "%+.1f e".format(particle.charge))
                BubbleDetailRow("Rest Mass m₀:", "%.4f GeV/c²".format(particle.species.restMassGeV))
                BubbleDetailRow("Transverse p_T:", "%.2f GeV/c".format(pt))
                BubbleDetailRow("Total Momentum |p|:", "%.2f GeV/c".format(pMag))
                BubbleDetailRow("Total Energy E:", "%.2f GeV".format(energy))
                BubbleDetailRow("Pseudo-Rapidity η:", "%+.2f (φ: %.2f)".format(fv.pseudoRapidity(), fv.phi()))
                BubbleDetailRow("Physical Size r_ch:", if (particle.species.physicalRadiusFm > 0.0) "%.3f fm".format(particle.species.physicalRadiusFm) else "Point-like (< 10⁻¹⁸ m)")
                BubbleDetailRow("Render Size Scale:", "%.2f× (Physical Relative)".format(particle.species.renderRadiusMultiplier))
                if (!curvatureMeters.isInfinite()) {
                    BubbleDetailRow("Track Curvature R:", "%.1f meters".format(curvatureMeters))
                }
            }
        }
    }
}

/**
 * Quantum De Broglie Wave Packet Inspector Bubble
 */
@Composable
fun WavePacketNameBubble(
    wavePacket: WavePacket3D,
    screenPos: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val bubbleWidth = 245f
    val bubbleHeight = 180f

    val clampX = (screenPos.x - bubbleWidth / 2f).coerceIn(16f, (canvasWidth - bubbleWidth - 16f).coerceAtLeast(16f))
    val clampY = if (screenPos.y > bubbleHeight + 40f) {
        screenPos.y - bubbleHeight - 20f
    } else {
        screenPos.y + 25f
    }.coerceIn(16f, (canvasHeight - bubbleHeight - 16f).coerceAtLeast(16f))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    ) {
        Surface(
            modifier = Modifier
                .offset { IntOffset(clampX.roundToInt(), clampY.roundToInt()) }
                .widthIn(max = 260.dp)
                .testTag("wave_packet_name_bubble"),
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF0F1A2E).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.96f),
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, wavePacket.color)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Header with Wave Icon, Title, and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Waves,
                            contentDescription = "Quantum Wave Packet",
                            tint = wavePacket.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = wavePacket.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = wavePacket.color,
                                fontSize = 11.5.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Bubble",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = wavePacket.packetType,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                BubbleDetailRow("de Broglie λ_dB:", "%.2f meters (Scaled)".format(wavePacket.deBroglieWavelengthMeters))
                BubbleDetailRow("Oscillation Phase φ:", "%.2f rad (ω = %.1f)".format(wavePacket.phase, wavePacket.frequency))
                BubbleDetailRow("Gaussian Width σ(t):", "%.2f m (Dispersing)".format(wavePacket.envelopeWidth))
                BubbleDetailRow("Prob. Amplitude |ψ|:", "%.2f".format(wavePacket.amplitude))
                BubbleDetailRow("Group Velocity v_g:", "%.3f c".format(wavePacket.velocity.magnitude().coerceIn(0f, 1f)))
                BubbleDetailRow("Momentum ħk:", "%.2f GeV/c".format(wavePacket.momentum.magnitude()))
                BubbleDetailRow("Quantum State:", if (wavePacket.isSphericalCoherentWave) "Vacuum Polarization" else "Localized Wave Packet")
            }
        }
    }
}

/**
 * Collimated Particle Packet / Bunch Inspector Bubble
 */
@Composable
fun ParticlePacketNameBubble(
    packet: ParticlePacket3D,
    screenPos: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val bubbleWidth = 250f
    val bubbleHeight = 180f

    val clampX = (screenPos.x - bubbleWidth / 2f).coerceIn(16f, (canvasWidth - bubbleWidth - 16f).coerceAtLeast(16f))
    val clampY = if (screenPos.y > bubbleHeight + 40f) {
        screenPos.y - bubbleHeight - 20f
    } else {
        screenPos.y + 25f
    }.coerceIn(16f, (canvasHeight - bubbleHeight - 16f).coerceAtLeast(16f))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    ) {
        Surface(
            modifier = Modifier
                .offset { IntOffset(clampX.roundToInt(), clampY.roundToInt()) }
                .widthIn(max = 265.dp)
                .testTag("particle_packet_name_bubble"),
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF1A1528).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.96f),
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, packet.color)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Header with Bunch Icon, Title, and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Grain,
                            contentDescription = "Particle Bunch Packet",
                            tint = packet.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = packet.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = packet.color,
                                fontSize = 11.5.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Bubble",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = "${packet.packetCategory} • Collimated Bunch",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                BubbleDetailRow("Constituent Quanta:", "${packet.constituentIds.size} particles")
                BubbleDetailRow("Key Hadron Species:", packet.constituentSpeciesNames.joinToString(" ").take(24))
                BubbleDetailRow("Total Bunch Energy ∑E:", "%.2f GeV".format(packet.totalEnergyGeV))
                BubbleDetailRow("Bunch Momentum |P|:", "%.2f GeV/c".format(packet.totalMomentum.magnitude()))
                BubbleDetailRow("Bunch Radius R_bunch:", "%.2f meters".format(packet.bunchRadius))
                BubbleDetailRow("Opening Cone Angle θ:", "%.1f° (%.2f rad)".format(packet.openingAngleRad * 180f / PI.toFloat(), packet.openingAngleRad))
                BubbleDetailRow("Bunch Centroid Speed:", "%.3f c".format(packet.velocity.magnitude().coerceIn(0f, 1f)))
            }
        }
    }
}

@Composable
fun BubbleDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 9.5.sp,
                color = Color.Gray
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
