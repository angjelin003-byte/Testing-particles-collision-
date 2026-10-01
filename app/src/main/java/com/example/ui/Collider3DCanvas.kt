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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physics.Point2D
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
    val camera = viewModel.camera

    val isDark = simState.isDarkTheme
    val bgColor = if (isDark) Color(0xFF080D1A) else Color(0xFFF0F4F8)

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
                        // 1-finger drag rotates camera
                        camera.rotateBy(pan.x * 0.005f, pan.y * 0.005f)
                    }
                    cameraChangeCounter++
                }
            }
    ) {
        // 3D Canvas Renderer
        Canvas(modifier = Modifier.fillMaxSize().testTag("3d_collider_canvas")) {
            val width = size.width
            val height = size.height

            // Trigger recomposition on gesture updates
            @Suppress("UNUSED_VARIABLE")
            val trigger = cameraChangeCounter

            val matrix = camera.buildTransformMatrix(width, height)

            // 1. Draw 3D Wireframe Detector Lines
            if (simState.wireframeEnabled) {
                for (line in wireframeLines) {
                    val p1: Point2D? = matrix.projectToScreen(line.start, width, height)
                    val p2: Point2D? = matrix.projectToScreen(line.end, width, height)

                    if (p1 != null && p2 != null) {
                        drawLine(
                            color = line.color.copy(alpha = line.alpha),
                            start = Offset(p1.x, p1.y),
                            end = Offset(p2.x, p2.y),
                            strokeWidth = line.strokeWidth * p1.scale.coerceIn(0.5f, 2.0f),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // 2. Draw Origin Collision Spark Effect
            val originProjected: Point2D? = matrix.projectToScreen(Vector3D.ZERO, width, height)
            if (originProjected != null) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.9f),
                            Color(0xFFFF0844).copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(originProjected.x, originProjected.y),
                        radius = 24f * originProjected.scale
                    ),
                    center = Offset(originProjected.x, originProjected.y),
                    radius = 24f * originProjected.scale
                )
            }

            // 3. Draw Particle Trajectories and Glowing Trails
            for (p in particles) {
                val pColor = p.colorOverride ?: p.species.color
                val history = p.trajectoryHistory

                if (history.size > 1) {
                    val path = Path()
                    var firstPoint = true

                    for (pt in history) {
                        val proj: Point2D? = matrix.projectToScreen(pt, width, height)
                        if (proj != null) {
                            if (firstPoint) {
                                path.moveTo(proj.x, proj.y)
                                firstPoint = false
                            } else {
                                path.lineTo(proj.x, proj.y)
                            }
                        }
                    }

                    if (!firstPoint) {
                        drawPath(
                            path = path,
                            color = pColor.copy(alpha = (0.75f * simState.glowIntensity).coerceIn(0.1f, 1.0f)),
                            style = Stroke(
                                width = (3.0f * simState.glowIntensity).coerceIn(1f, 8f),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // Draw Particle Head Sphere
                val headProj: Point2D? = matrix.projectToScreen(p.position, width, height)
                if (headProj != null) {
                    val radius = (6f * headProj.scale * simState.glowIntensity).coerceIn(4f, 20f)
                    drawCircle(
                        color = pColor,
                        center = Offset(headProj.x, headProj.y),
                        radius = radius
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        center = Offset(headProj.x, headProj.y),
                        radius = radius * 0.4f
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
                        text = "3D VIEW | 2-FINGER ZOOM & PAN",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
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
