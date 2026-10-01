package com.example.physics

import kotlin.math.sqrt

data class Vector3D(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    operator fun plus(other: Vector3D) = Vector3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3D) = Vector3D(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3D(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vector3D(x / scalar, y / scalar, z / scalar)
    operator fun unaryMinus() = Vector3D(-x, -y, -z)

    fun dot(other: Vector3D): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3D): Vector3D = Vector3D(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun magnitude(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): Vector3D {
        val mag = magnitude()
        return if (mag > 0.00001f) this / mag else ZERO
    }

    fun transverseMagnitude(): Float = sqrt(x * x + y * y)

    companion object {
        val ZERO = Vector3D(0f, 0f, 0f)
        val UNIT_X = Vector3D(1f, 0f, 0f)
        val UNIT_Y = Vector3D(0f, 1f, 0f)
        val UNIT_Z = Vector3D(0f, 0f, 1f)
    }
}
