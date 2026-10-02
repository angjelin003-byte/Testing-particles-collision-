package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
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
import com.example.physics.Particle3D
import com.example.rendering.DetectorWireframe
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun Collider3DCanvas(
    viewModel: ColliderViewModel,
    modifier: Modifier = Modifier,
    onExitClick: () -> Unit
) {
    val simState by viewModel.state.collectAsState()
    val particles by viewModel.liveParticles.collectAsState()
    val currentEvent by viewModel.currentEvent.collectAsState()
    val camera = viewModel.camera

    val isDark = simState.isDarkTheme
    val bgColor = if (isDark) Color(0xFF090D16) else Color(0xFFF1F5F9)

    // Continuous Frame Clock for uninterrupted 60FPS animation rendering
    var frameTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(simState.isPaused) {
        while (!simState.isPaused) {
            withFrameNanos { nanos ->
                frameTick = nanos
            }
        }
    }

    // Selected particle for Name Bubble Inspection
    var selectedParticleId by remember { mutableStateOf<String?>(null) }
    var selectedParticleScreenPos by remember { mutableStateOf<Offset?>(null) }

    // Reusable buffers (Zero heap allocation during 60FPS draw)
    val projBuffer1 = remember { FloatArray(3) }
    val projBuffer2 = remember { FloatArray(3) }
    val reusablePath = remember { Path() }
    val dashedEffect = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) }

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
                        // User performed a clean TAP! Check if tapped on a particle
                        val matrix = camera.buildTransformMatrix(canvasWidth, canvasHeight)
                        var closestParticle: Particle3D? = null
                        var minDistance = Float.MAX_VALUE
                        val tapThreshold = 55f // generous touch radius in pixels

                        for (p in particles) {
                            if (matrix.projectToScreenFast(
                                    p.position.x, p.position.y, p.position.z,
                                    canvasWidth, canvasHeight, projBuffer1,
                                    camera.panX, camera.panY
                                )
                            ) {
                                val dx = projBuffer1[0] - initialDownPos.x
                                val dy = projBuffer1[1] - initialDownPos.y
                                val dist = sqrt(dx * dx + dy * dy)
                                if (dist < tapThreshold && dist < minDistance) {
                                    minDistance = dist
                                    closestParticle = p
                                }
                            }
                        }

                        if (closestParticle != null) {
                            selectedParticleId = closestParticle.id
                        } else {
                            selectedParticleId = null
                        }
                    }
                }
            }
    ) {
        // High-Precision Crisp CERN-Style 3D Event Display (No Glow)
        Canvas(modifier = Modifier.fillMaxSize().testTag("3d_collider_canvas")) {
            canvasWidth = size.width
            canvasHeight = size.height

            @Suppress("UNUSED_VARIABLE")
            val currentFrame = frameTick
            @Suppress("UNUSED_VARIABLE")
            val trigger = cameraChangeCounter

            val matrix = camera.buildTransformMatrix(canvasWidth, canvasHeight)

            // 1. Draw 3D Detector Wireframe Mesh
            if (simState.wireframeEnabled) {
                for (line in wireframeLines) {
                    val ok1 = matrix.projectToScreenFast(
                        line.start.x, line.start.y, line.start.z,
                        canvasWidth, canvasHeight, projBuffer1,
                        camera.panX, camera.panY
                    )
                    val ok2 = matrix.projectToScreenFast(
                        line.end.x, line.end.y, line.end.z,
                        canvasWidth, canvasHeight, projBuffer2,
                        camera.panX, camera.panY
                    )

                    if (ok1 && ok2) {
                        drawLine(
                            color = line.color.copy(alpha = line.alpha * 0.75f),
                            start = Offset(projBuffer1[0], projBuffer1[1]),
                            end = Offset(projBuffer2[0], projBuffer2[1]),
                            strokeWidth = (line.strokeWidth * projBuffer1[2]).coerceIn(1.0f, 2.5f),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // 2. Draw Calorimeter Energy Hit Cell Towers (Clean solid geometric blocks, no glow)
            currentEvent?.calorimeterHits?.let { hits ->
                for (hit in hits) {
                    if (matrix.projectToScreenFast(
                            hit.position.x, hit.position.y, hit.position.z,
                            canvasWidth, canvasHeight, projBuffer1,
                            camera.panX, camera.panY
                        )
                    ) {
                        val hx = projBuffer1[0]
                        val hy = projBuffer1[1]
                        val hScale = projBuffer1[2]
                        val towerRadius = (hit.energyGeV.toFloat() * 0.22f * hScale).coerceIn(6f, 22f)

                        // Clean cell deposit marker
                        drawCircle(
                            color = hit.color.copy(alpha = 0.85f),
                            center = Offset(hx, hy),
                            radius = towerRadius
                        )
                        drawCircle(
                            color = Color.White,
                            center = Offset(hx, hy),
                            radius = towerRadius * 0.4f,
                            style = Stroke(width = 1.5f)
                        )
                    }
                }
            }

            // 3. Draw Missing Transverse Energy Vector Arrow (Neutrino E_T_miss)
            currentEvent?.let { ev ->
                if (ev.missingETGeV > 2.0) {
                    val missVec = ev.missingETVector.normalized() * 9.0f
                    val ok0 = matrix.projectToScreenFast(
                        0f, 0f, 0f,
                        canvasWidth, canvasHeight, projBuffer1,
                        camera.panX, camera.panY
                    )
                    val ok1 = matrix.projectToScreenFast(
                        missVec.x, missVec.y, missVec.z,
                        canvasWidth, canvasHeight, projBuffer2,
                        camera.panX, camera.panY
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

            // 4. Draw Collision Vertex (0,0,0) Origin Mark (Crisp crosshair star, NO blurry glow)
            if (matrix.projectToScreenFast(
                    0f, 0f, 0f,
                    canvasWidth, canvasHeight, projBuffer1,
                    camera.panX, camera.panY
                )
            ) {
                val ox = projBuffer1[0]
                val oy = projBuffer1[1]
                val starSize = 10f * projBuffer1[2].coerceIn(0.8f, 2.5f)

                // Clean vertex crosshair
                drawLine(
                    color = Color.White,
                    start = Offset(ox - starSize, oy),
                    end = Offset(ox + starSize, oy),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(ox, oy - starSize),
                    end = Offset(ox, oy + starSize),
                    strokeWidth = 2f
                )
                drawCircle(
                    color = Color(0xFF00E5FF),
                    center = Offset(ox, oy),
                    radius = 3.5f * projBuffer1[2]
                )
            }

            // 5. Draw Particle Trajectories and Crisp Particle Heads (NO GLOW)
            var currentSelectedScreenPos: Offset? = null

            for (p in particles) {
                val isSelected = (p.id == selectedParticleId)

                // Determine crisp track color
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
                                camera.panX, camera.panY
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

                        drawPath(
                            path = reusablePath,
                            color = pColor.copy(alpha = if (isSelected) 1.0f else simState.trailAlpha),
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // Draw Clean Particle Head Sphere (Solid sphere + highlight, NO blurry glow halo)
                if (matrix.projectToScreenFast(
                        p.position.x, p.position.y, p.position.z,
                        canvasWidth, canvasHeight, projBuffer1,
                        camera.panX, camera.panY
                    )
                ) {
                    val hx = projBuffer1[0]
                    val hy = projBuffer1[1]
                    val hScale = projBuffer1[2]
                    val baseRadius = if (isSelected) 9f else 6.5f
                    val radius = (baseRadius * hScale).coerceIn(4.5f, 22f)

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
                        color = pColor,
                        center = Offset(hx, hy),
                        radius = radius
                    )
                    // Sharp specular highlight dot
                    drawCircle(
                        color = Color.White,
                        center = Offset(hx - radius * 0.3f, hy - radius * 0.3f),
                        radius = radius * 0.35f
                    )
                }
            }

            selectedParticleScreenPos = currentSelectedScreenPos
        }

        // Particle Name Bubble (Interactive Inspector on Tap)
        val inspectedParticle = particles.find { it.id == selectedParticleId }
        val bubblePos = selectedParticleScreenPos

        if (inspectedParticle != null && bubblePos != null) {
            ParticleNameBubble(
                particle = inspectedParticle,
                magneticFieldTesla = simState.magneticFieldTesla,
                screenPos = bubblePos,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                isDark = isDark,
                onDismiss = { selectedParticleId = null }
            )
        }

        // Overlay Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF131C31).copy(alpha = 0.90f) else Color.White.copy(alpha = 0.92f),
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

                    // Reset Camera & Pan
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

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (simState.isBeamInFlight) "BEAMS IN FLIGHT → COLLISION PENDING" else "ZOOM: %.1fx".format(camera.zoom),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (simState.isBeamInFlight) Color(0xFFFF0844) else (if (isDark) Color(0xFF80DEEA) else Color(0xFF0277BD))
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

        // Bottom Left Control Overlay (Play/Pause/Step & Physics Indicators)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = if (isDark) Color(0xFF131C31).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.94f),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.togglePause() },
                    modifier = Modifier.testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (simState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Play/Pause Simulation",
                        tint = if (isDark) Color(0xFF00E5FF) else Color(0xFF0288D1)
                    )
                }

                IconButton(
                    onClick = { viewModel.stepSingleFrame() },
                    enabled = simState.isPaused,
                    modifier = Modifier.testTag("step_frame_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Step Single Frame",
                        tint = if (simState.isPaused) (if (isDark) Color.White else Color.Black) else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "TIME SCALE: %.4f c".format(simState.timeScale),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFFFD600) else Color(0xFFE65100)
                        )
                    )
                    Text(
                        text = "ACTIVE PARTICLES: ${particles.size} (Tap track to inspect)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = if (isDark) Color.LightGray else Color.DarkGray
                        )
                    )
                }
            }
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

    // Curvature radius in solenoidal B-field: R = p_T / (|q| * B)
    val curvatureMeters = if (abs(particle.charge) > 0.01 && magneticFieldTesla > 0.05f) {
        (pt / (abs(particle.charge).toFloat() * magneticFieldTesla * 0.3f))
    } else {
        Float.POSITIVE_INFINITY
    }

    val bubbleWidth = 230f
    val bubbleHeight = 160f

    // Clamp bubble to stay within screen boundaries
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
                .widthIn(max = 240.dp)
                .testTag("particle_name_bubble"),
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF131D33).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.96f),
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, particle.species.color)
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
                                fontSize = 13.sp
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
                    text = when (particle.generation) {
                        0 -> "Incoming Beam Particle"
                        1 -> "Primary Collision Shower Track"
                        else -> "Secondary Displaced Decay Daughter"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Physical properties readout
                BubbleDetailRow("Electric Charge:", "%+.1f e".format(particle.charge))
                BubbleDetailRow("Rest Mass m₀:", "%.4f GeV/c²".format(particle.species.restMassGeV))
                BubbleDetailRow("Transverse p_T:", "%.2f GeV/c".format(pt))
                BubbleDetailRow("Total Momentum |p|:", "%.2f GeV/c".format(pMag))
                BubbleDetailRow("Total Energy E:", "%.2f GeV".format(energy))
                BubbleDetailRow("Pseudo-Rapidity η:", "%+.2f (φ: %.2f)".format(fv.pseudoRapidity(), fv.phi()))
                if (!curvatureMeters.isInfinite()) {
                    BubbleDetailRow("Track Curvature R:", "%.1f meters".format(curvatureMeters))
                }
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
                fontSize = 10.sp,
                color = Color.Gray
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
