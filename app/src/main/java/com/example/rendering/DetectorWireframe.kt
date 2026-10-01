package com.example.rendering

import androidx.compose.ui.graphics.Color
import com.example.physics.Vector3D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class WireframeLine(
    val start: Vector3D,
    val end: Vector3D,
    val color: Color,
    val strokeWidth: Float = 2f,
    val alpha: Float = 0.6f
)

object DetectorWireframe {

    /**
     * Generate 3D wireframe mesh model of particle physics detector (optimized segment count)
     */
    fun buildDetectorMesh(
        showTracker: Boolean = true,
        showEcal: Boolean = true,
        showHcal: Boolean = true,
        showMuon: Boolean = true,
        showGrid: Boolean = true,
        isDarkTheme: Boolean = true
    ): List<WireframeLine> {
        val lines = mutableListOf<WireframeLine>()

        val beamPipeColor = if (isDarkTheme) Color(0xFF00E5FF) else Color(0xFF0288D1)
        val trackerColor = if (isDarkTheme) Color(0xFF7C4DFF) else Color(0xFF512DA8)
        val ecalColor = if (isDarkTheme) Color(0xFF00E676) else Color(0xFF388E3C)
        val hcalColor = if (isDarkTheme) Color(0xFFFF9100) else Color(0xFFF57C00)
        val muonColor = if (isDarkTheme) Color(0xFFFF1744) else Color(0xFFD32F2F)
        val gridColor = if (isDarkTheme) Color(0xFF37474F) else Color(0xFFCFD8DC)

        // 1. BEAM PIPE (Cylinder radius = 0.8m, length = 20m)
        val pipeRadius = 0.8f
        val pipeLength = 10f
        val segments = 12

        for (z in listOf(-pipeLength, -pipeLength / 2f, 0f, pipeLength / 2f, pipeLength)) {
            for (i in 0 until segments) {
                val angle1 = (2f * PI.toFloat() * i) / segments
                val angle2 = (2f * PI.toFloat() * (i + 1)) / segments
                val p1 = Vector3D(pipeRadius * cos(angle1), pipeRadius * sin(angle1), z)
                val p2 = Vector3D(pipeRadius * cos(angle2), pipeRadius * sin(angle2), z)
                lines.add(WireframeLine(p1, p2, beamPipeColor, 1.5f, 0.5f))
            }
        }
        for (i in 0 until 6) {
            val angle = (2f * PI.toFloat() * i) / 6
            val pStart = Vector3D(pipeRadius * cos(angle), pipeRadius * sin(angle), -pipeLength)
            val pEnd = Vector3D(pipeRadius * cos(angle), pipeRadius * sin(angle), pipeLength)
            lines.add(WireframeLine(pStart, pEnd, beamPipeColor, 1.0f, 0.4f))
        }

        // 2. INNER SILICON PIXEL TRACKER (Radius = 2.5m)
        if (showTracker) {
            val rTracker = 2.5f
            for (z in listOf(-5f, 0f, 5f)) {
                for (i in 0 until segments) {
                    val angle1 = (2f * PI.toFloat() * i) / segments
                    val angle2 = (2f * PI.toFloat() * (i + 1)) / segments
                    val p1 = Vector3D(rTracker * cos(angle1), rTracker * sin(angle1), z)
                    val p2 = Vector3D(rTracker * cos(angle2), rTracker * sin(angle2), z)
                    lines.add(WireframeLine(p1, p2, trackerColor, 1.5f, 0.6f))
                }
            }
        }

        // 3. ELECTROMAGNETIC CALORIMETER (ECAL - Radius = 4.5m)
        if (showEcal) {
            val rEcal = 4.5f
            for (z in listOf(-6f, 0f, 6f)) {
                for (i in 0 until segments) {
                    val angle1 = (2f * PI.toFloat() * i) / segments
                    val angle2 = (2f * PI.toFloat() * (i + 1)) / segments
                    val p1 = Vector3D(rEcal * cos(angle1), rEcal * sin(angle1), z)
                    val p2 = Vector3D(rEcal * cos(angle2), rEcal * sin(angle2), z)
                    lines.add(WireframeLine(p1, p2, ecalColor, 2f, 0.7f))
                }
            }
        }

        // 4. HADRONIC CALORIMETER (HCAL - Radius = 6.8m)
        if (showHcal) {
            val rHcal = 6.8f
            for (z in listOf(-7f, 0f, 7f)) {
                for (i in 0 until segments) {
                    val angle1 = (2f * PI.toFloat() * i) / segments
                    val angle2 = (2f * PI.toFloat() * (i + 1)) / segments
                    val p1 = Vector3D(rHcal * cos(angle1), rHcal * sin(angle1), z)
                    val p2 = Vector3D(rHcal * cos(angle2), rHcal * sin(angle2), z)
                    lines.add(WireframeLine(p1, p2, hcalColor, 2.5f, 0.6f))
                }
            }
        }

        // 5. OUTER MUON DRIFT TUBES (Radius = 9.5m)
        if (showMuon) {
            val rMuon = 9.5f
            for (z in listOf(-8f, 0f, 8f)) {
                for (i in 0 until segments) {
                    val angle1 = (2f * PI.toFloat() * i) / segments
                    val angle2 = (2f * PI.toFloat() * (i + 1)) / segments
                    val p1 = Vector3D(rMuon * cos(angle1), rMuon * sin(angle1), z)
                    val p2 = Vector3D(rMuon * cos(angle2), rMuon * sin(angle2), z)
                    lines.add(WireframeLine(p1, p2, muonColor, 2f, 0.5f))
                }
            }
        }

        // 6. COORDINATE AXES (Origin 0,0,0)
        val axisLen = 3f
        lines.add(WireframeLine(Vector3D.ZERO, Vector3D(axisLen, 0f, 0f), Color(0xFFFF1744), 3.5f, 0.9f)) // X = Red
        lines.add(WireframeLine(Vector3D.ZERO, Vector3D(0f, axisLen, 0f), Color(0xFF00E676), 3.5f, 0.9f)) // Y = Green
        lines.add(WireframeLine(Vector3D.ZERO, Vector3D(0f, 0f, axisLen), Color(0xFF29B6F6), 3.5f, 0.9f)) // Z = Blue (Beam axis)

        // 7. BASE PLANE FIELD GRID OVERLAY
        if (showGrid) {
            val gridSize = 10f
            val gridStep = 2.5f
            var x = -gridSize
            while (x <= gridSize) {
                lines.add(WireframeLine(Vector3D(x, -2f, -gridSize), Vector3D(x, -2f, gridSize), gridColor, 1f, 0.25f))
                x += gridStep
            }
            var z = -gridSize
            while (z <= gridSize) {
                lines.add(WireframeLine(Vector3D(-gridSize, -2f, z), Vector3D(gridSize, -2f, z), gridColor, 1f, 0.25f))
                z += gridStep
            }
        }

        return lines
    }
}
