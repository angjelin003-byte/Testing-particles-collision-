package com.example.rendering

import com.example.physics.Matrix3D

class Camera3D(
    var yawRad: Float = 0.5f,
    var pitchRad: Float = 0.35f,
    var zoom: Float = 3.8f,
    var panX: Float = 0f,
    var panY: Float = 0f
) {
    fun rotateBy(deltaYaw: Float, deltaPitch: Float) {
        yawRad += deltaYaw
        pitchRad = (pitchRad + deltaPitch).coerceIn(-1.45f, 1.45f)
    }

    fun zoomBy(factor: Float) {
        zoom = (zoom * factor).coerceIn(0.4f, 25.0f)
    }

    fun zoomIn() {
        zoom = (zoom * 1.25f).coerceIn(0.4f, 25.0f)
    }

    fun zoomOut() {
        zoom = (zoom / 1.25f).coerceIn(0.4f, 25.0f)
    }

    fun panBy(dx: Float, dy: Float) {
        panX += dx
        panY += dy
    }

    fun reset() {
        yawRad = 0.5f
        pitchRad = 0.35f
        zoom = 3.8f
        panX = 0f
        panY = 0f
    }

    fun buildTransformMatrix(screenWidth: Float, screenHeight: Float): Matrix3D {
        val rotY = Matrix3D.createRotationY(yawRad)
        val rotX = Matrix3D.createRotationX(pitchRad)
        val scale = Matrix3D.createScale(zoom, zoom, zoom)

        return scale * rotX * rotY
    }
}
