package com.example.physics

import kotlin.math.cos
import kotlin.math.sin

/**
 * 4x4 Transformation Matrix for 3D graphics camera projection, rotation, and translation.
 */
class Matrix3D private constructor(val m: FloatArray) {

    companion object {
        fun identity(): Matrix3D {
            val arr = FloatArray(16)
            arr[0] = 1f; arr[5] = 1f; arr[10] = 1f; arr[15] = 1f
            return Matrix3D(arr)
        }

        fun createRotationY(angleRad: Float): Matrix3D {
            val c = cos(angleRad)
            val s = sin(angleRad)
            val arr = identity().m.clone()
            arr[0] = c; arr[2] = s
            arr[8] = -s; arr[10] = c
            return Matrix3D(arr)
        }

        fun createRotationX(angleRad: Float): Matrix3D {
            val c = cos(angleRad)
            val s = sin(angleRad)
            val arr = identity().m.clone()
            arr[5] = c; arr[6] = -s
            arr[9] = s; arr[10] = c
            return Matrix3D(arr)
        }

        fun createTranslation(tx: Float, ty: Float, tz: Float): Matrix3D {
            val arr = identity().m.clone()
            arr[3] = tx
            arr[7] = ty
            arr[11] = tz
            return Matrix3D(arr)
        }

        fun createScale(sx: Float, sy: Float, sz: Float): Matrix3D {
            val arr = identity().m.clone()
            arr[0] = sx
            arr[5] = sy
            arr[10] = sz
            return Matrix3D(arr)
        }
    }

    operator fun times(other: Matrix3D): Matrix3D {
        val result = FloatArray(16)
        for (row in 0..3) {
            for (col in 0..3) {
                var sum = 0f
                for (i in 0..3) {
                    sum += m[row * 4 + i] * other.m[i * 4 + col]
                }
                result[row * 4 + col] = sum
            }
        }
        return Matrix3D(result)
    }

    fun transform(v: Vector3D): Vector3D {
        val x = m[0] * v.x + m[1] * v.y + m[2] * v.z + m[3]
        val y = m[4] * v.x + m[5] * v.y + m[6] * v.z + m[7]
        val z = m[8] * v.x + m[9] * v.y + m[10] * v.z + m[11]
        val w = m[12] * v.x + m[13] * v.y + m[14] * v.z + m[15]
        val invW = if (w != 0f) 1f / w else 1f
        return Vector3D(x * invW, y * invW, z * invW)
    }

    /**
     * Perspective projection into 2D viewport coordinates.
     */
    fun projectToScreen(
        v: Vector3D,
        screenWidth: Float,
        screenHeight: Float,
        fovFactor: Float = 600f
    ): Point2D? {
        val transformed = transform(v)
        // Depth check (z behind camera)
        val z = transformed.z + fovFactor
        if (z <= 1f) return null

        val scale = fovFactor / z
        val screenX = screenWidth / 2f + transformed.x * scale
        val screenY = screenHeight / 2f - transformed.y * scale
        return Point2D(screenX, screenY, z, scale)
    }

    /**
     * Fast zero-allocation projection writing into outResult FloatArray(3) [screenX, screenY, scale]
     */
    fun projectToScreenFast(
        vx: Float, vy: Float, vz: Float,
        screenWidth: Float,
        screenHeight: Float,
        outResult: FloatArray,
        fovFactor: Float = 600f
    ): Boolean {
        val tx = m[0] * vx + m[1] * vy + m[2] * vz + m[3]
        val ty = m[4] * vx + m[5] * vy + m[6] * vz + m[7]
        val tz = m[8] * vx + m[9] * vy + m[10] * vz + m[11]
        val tw = m[12] * vx + m[13] * vy + m[14] * vz + m[15]
        val invW = if (tw != 0f) 1f / tw else 1f

        val z = (tz * invW) + fovFactor
        if (z <= 1f) return false

        val scale = fovFactor / z
        outResult[0] = screenWidth / 2f + (tx * invW) * scale
        outResult[1] = screenHeight / 2f - (ty * invW) * scale
        outResult[2] = scale
        return true
    }
}

data class Point2D(
    val x: Float,
    val y: Float,
    val depth: Float,
    val scale: Float
)
