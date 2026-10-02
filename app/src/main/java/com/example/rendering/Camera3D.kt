package com.example.rendering

import com.example.physics.Matrix3D
import kotlin.math.PI

enum class ViewProjectionMode(val displayName: String, val shortName: String) {
    PERSPECTIVE("3D Perspective", "3D Persp"),
    ISOMETRIC_3D("3D Isometric", "3D Iso"),
    TRANSVERSE_2D("2D Transverse (X-Y)", "2D X-Y"),
    LONGITUDINAL_2D("2D Longitudinal (Z-Y)", "2D Z-Y")
}

class Camera3D(
    var yawRad: Float = 0.5f,
    var pitchRad: Float = 0.35f,
    var zoom: Float = 3.5f,
    var panX: Float = 0f,
    var panY: Float = 0f,
    var projectionMode: ViewProjectionMode = ViewProjectionMode.PERSPECTIVE
) {
    val isIsometricOr2D: Boolean
        get() = projectionMode != ViewProjectionMode.PERSPECTIVE

    fun rotateBy(deltaYaw: Float, deltaPitch: Float) {
        if (projectionMode == ViewProjectionMode.TRANSVERSE_2D || projectionMode == ViewProjectionMode.LONGITUDINAL_2D) {
            return
        }
        yawRad += deltaYaw
        pitchRad = (pitchRad + deltaPitch).coerceIn(-1.45f, 1.45f)
    }

    fun zoomBy(factor: Float) {
        zoom = (zoom * factor).coerceIn(0.4f, 30.0f)
    }

    fun zoomIn() {
        zoom = (zoom * 1.25f).coerceIn(0.4f, 30.0f)
    }

    fun zoomOut() {
        zoom = (zoom / 1.25f).coerceIn(0.4f, 30.0f)
    }

    fun panBy(dx: Float, dy: Float) {
        panX += dx
        panY += dy
    }

    fun applyProjectionMode(mode: ViewProjectionMode) {
        projectionMode = mode
        when (mode) {
            ViewProjectionMode.TRANSVERSE_2D -> {
                // Looking directly down the beam axis (Z-axis): view X-Y transverse plane
                yawRad = 0f
                pitchRad = 0f
            }
            ViewProjectionMode.LONGITUDINAL_2D -> {
                // Looking from side perpendicular to beam axis: view Z-Y longitudinal plane
                yawRad = (PI / 2f).toFloat()
                pitchRad = 0f
            }
            ViewProjectionMode.ISOMETRIC_3D -> {
                // True isometric projection angle: 45° yaw, ~35.26° pitch
                yawRad = 0.7854f
                pitchRad = 0.6155f
            }
            ViewProjectionMode.PERSPECTIVE -> {
                yawRad = 0.5f
                pitchRad = 0.35f
            }
        }
    }

    fun reset() {
        yawRad = 0.5f
        pitchRad = 0.35f
        zoom = 3.5f
        panX = 0f
        panY = 0f
        projectionMode = ViewProjectionMode.PERSPECTIVE
    }

    fun buildTransformMatrix(screenWidth: Float, screenHeight: Float): Matrix3D {
        val rotY = Matrix3D.createRotationY(yawRad)
        val rotX = Matrix3D.createRotationX(pitchRad)
        val scale = Matrix3D.createScale(zoom, zoom, zoom)

        return scale * rotX * rotY
    }
}
