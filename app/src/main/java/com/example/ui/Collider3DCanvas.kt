package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physics.Vector3D
import com.example.rendering.Camera3D
import com.example.rendering.DetectorWireframe

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
    val bgColor = if (isDark) Color(0xFF080D1A) else Color(0xFFF0F4F8)

    // Continuous Frame Ticker for uninterrupted 60FPS animation rendering
    var frameTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(simState.isPaused) {
        while (!simState.isPaused) {
            withFrameNanos { nanos ->
                frameTick = nanos
            }
        }
    }

    // Reusable projection buffers and path object (Zero heap allocation during 60FPS draw)
    val projBuffer1 = remember { FloatArray(3) }
    val projBuffer2 = remember { FloatArray(3) }
    val reusablePath = remember { Path() }
    val dashedEffect = remember { PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f) }

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        camera.reset()
                        cameraChangeCounter++
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    if (zoom != 1f) {
                        camera.zoomBy(zoom)
                    }
                    if (pan != Offset.Zero) {
                        camera.rotateBy(pan.x * 0.005f, pan.y * 0.005f)
                    }
                    cameraChangeCounter++
                }
            }
    ) {
        // High-Performance Continuous 3D Canvas Renderer
        Canvas(modifier = Modifier.fillMaxSize().testTag("3d_collider_canvas")) {
            val width = size.width
            val height = size.height

            @Suppress("UNUSED_VARIABLE")
            val currentFrame = frameTick
            @Suppress("UNUSED_VARIABLE")
            val trigger = cameraChangeCounter

            val matrix = camera.buildTransformMatrix(width, height)

            // 1. Draw 3D Detector Wireframe Lines
            if (simState.wireframeEnabled) {
                for (line in wireframeLines) {
                    val ok1 = matrix.projectToScreenFast(line.start.x, line.start.y, line.start.z, width, height, projBuffer1)
                    val ok2 = matrix.projectToScreenFast(line.end.x, line.end.y, line.end.z, width, height, projBuffer2)

                    if (ok1 && ok2) {
                        drawLine(
                            color = line.color.copy(alpha = line.alpha),
                            start = Offset(projBuffer1[0], projBuffer1[1]),
                            end = Offset(projBuffer2[0], projBuffer2[1]),
                            strokeWidth = line.strokeWidth * projBuffer1[2].coerceIn(0.8f, 2.8f),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // 2. Draw Calorimeter Energy Hit Towers (ECAL & HCAL Sparkling Clusters)
            currentEvent?.calorimeterHits?.let { hits ->
                for (hit in hits) {
                    if (matrix.projectToScreenFast(hit.position.x, hit.position.y, hit.position.z, width, height, projBuffer1)) {
                        val hx = projBuffer1[0]
                        val hy = projBuffer1[1]
                        val hScale = projBuffer1[2]
                        val towerRadius = (hit.energyGeV.toFloat() * 0.2f * hScale).coerceIn(8f, 28f)

                        drawCircle(
                            color = hit.color.copy(alpha = 0.85f),
                            center = Offset(hx, hy),
                            radius = towerRadius
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            center = Offset(hx, hy),
                            radius = towerRadius * 0.35f
                        )
                    }
                }
            }

            // 3. Draw Missing Transverse Energy Vector Arrow (Neutrino E_T_miss)
            currentEvent?.let { ev ->
                if (ev.missingETGeV > 2.0) {
                    val missVec = ev.missingETVector.normalized() * 8.0f
                    val ok0 = matrix.projectToScreenFast(0f, 0f, 0f, width, height, projBuffer1)
                    val ok1 = matrix.projectToScreenFast(missVec.x, missVec.y, missVec.z, width, height, projBuffer2)

                    if (ok0 && ok1) {
                        drawLine(
                            color = Color(0xFFB9F6CA),
                            start = Offset(projBuffer1[0], projBuffer1[1]),
                            end = Offset(projBuffer2[0], projBuffer2[1]),
                            strokeWidth = 4f * projBuffer1[2],
                            pathEffect = dashedEffect,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // 4. Draw Collision Vertex Spark Flare
            if (matrix.projectToScreenFast(0f, 0f, 0f, width, height, projBuffer1)) {
                val ox = projBuffer1[0]
                val oy = projBuffer1[1]
                val oScale = projBuffer1[2]
                val sparkRadius = 65f * oScale

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.95f),
                            Color(0xFFFF0844).copy(alpha = 0.65f),
                            Color(0xFFFFD600).copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(ox, oy),
                        radius = sparkRadius
                    ),
                    center = Offset(ox, oy),
                    radius = sparkRadius
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.95f),
                    center = Offset(ox, oy),
                    radius = sparkRadius * 0.2f
                )
            }

            // 5. Draw Particle Trajectories and Custom Glowing Trails
            for (p in particles) {
                // Determine particle trail color based on selected TrailColorMode
                val pColor = when (simState.trailColorMode) {
                    TrailColorMode.SPECIES_COLOR -> p.colorOverride ?: p.species.color
                    TrailColorMode.ENERGY_HEATMAP -> {
                        val pT = p.transverseMomentum()
                        when {
                            pT > 80f -> Color(0xFFFF1744)  // High pT = Red
                            pT > 30f -> Color(0xFFFFD600)  // Medium pT = Yellow
                            pT > 10f -> Color(0xFF00E5FF)  // Moderate pT = Cyan
                            else -> Color(0xFF7C4DFF)      // Low pT = Purple
                        }
                    }
                    TrailColorMode.CHARGE_COLOR -> {
                        when {
                            p.charge > 0 -> Color(0xFFFF1744)   // Positive = Red
                            p.charge < 0 -> Color(0xFF29B6F6)   // Negative = Blue
                            else -> Color(0xFFFFD600)           // Neutral = Yellow
                        }
                    }
                }

                val history = p.trajectoryHistory

                if (history.size > 1) {
                    reusablePath.reset()
                    var firstPoint = true

                    for (pt in history) {
                        if (matrix.projectToScreenFast(pt.x, pt.y, pt.z, width, height, projBuffer1)) {
                            if (firstPoint) {
                                reusablePath.moveTo(projBuffer1[0], projBuffer1[1])
                                firstPoint = false
                            } else {
                                reusablePath.lineTo(projBuffer1[0], projBuffer1[1])
                            }
                        }
                    }

                    if (!firstPoint) {
                        val widthScale = simState.trailWidth
                        val alphaScale = simState.trailAlpha

                        // Pass 1: Glowing outer aura
                        drawPath(
                            path = reusablePath,
                            color = pColor.copy(alpha = (0.35f * simState.glowIntensity * alphaScale).coerceIn(0.05f, 1.0f)),
                            style = Stroke(
                                width = (12.0f * simState.glowIntensity * widthScale).coerceIn(2f, 32f),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        // Pass 2: Intense core beam
                        drawPath(
                            path = reusablePath,
                            color = pColor.copy(alpha = (0.95f * simState.glowIntensity * alphaScale).coerceIn(0.2f, 1.0f)),
                            style = Stroke(
                                width = (4.5f * simState.glowIntensity * widthScale).coerceIn(1.5f, 16f),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // Draw Particle Head Sphere
                if (matrix.projectToScreenFast(p.position.x, p.position.y, p.position.z, width, height, projBuffer1)) {
                    val hx = projBuffer1[0]
                    val hy = projBuffer1[1]
                    val hScale = projBuffer1[2]
                    val radius = (10f * hScale * simState.glowIntensity).coerceIn(6f, 32f)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                pColor.copy(alpha = 0.9f),
                                pColor.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            center = Offset(hx, hy),
                            radius = radius * 2.2f
                        ),
                        center = Offset(hx, hy),
                        radius = radius * 2.2f
                    )
                    drawCircle(
                        color = pColor,
                        center = Offset(hx, hy),
                        radius = radius
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        center = Offset(hx - radius * 0.25f, hy - radius * 0.25f),
                        radius = radius * 0.35f
                    )
                }
            }
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
                color = if (isDark) Color(0xFF131C31).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.9f),
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
                            contentDescription = "Zoom In 3D",
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
                            contentDescription = "Zoom Out 3D",
                            tint = Color(0xFFFF9100)
                        )
                    }

                    // Reset Camera
                    IconButton(
                        onClick = {
                            viewModel.resetCamera()
                            cameraChangeCounter++
                        },
                        modifier = Modifier.testTag("reset_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset 3D Camera",
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
                color = Color(0xFFFF1744).copy(alpha = 0.85f),
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
            color = if (isDark) Color(0xFF131C31).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.92f),
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
                        text = "ACTIVE PARTICLES: ${particles.size}",
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
