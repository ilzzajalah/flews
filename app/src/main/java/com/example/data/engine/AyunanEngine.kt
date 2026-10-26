package com.example.data.engine

import androidx.compose.ui.graphics.Matrix
import com.example.data.model.EffectType
import com.example.data.model.MotionEffect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

enum class WaveShape {
    SINUS,
    SEGITIGA,
    HALUS
}

object AyunanEngine {

    /**
     * Evaluates pure wave output in range [-1.0, 1.0] given wave shape and phase in radians.
     */
    fun wave(shape: WaveShape, phase: Float): Float {
        val twoPi = (2.0 * PI).toFloat()
        // Normalize phase to [0, 2π)
        val p = ((phase % twoPi) + twoPi) % twoPi

        return when (shape) {
            WaveShape.SINUS -> sin(p)
            WaveShape.SEGITIGA -> {
                val u = p / twoPi // [0, 1)
                when {
                    u < 0.25f -> 4.0f * u
                    u < 0.75f -> 2.0f - 4.0f * u
                    else -> 4.0f * u - 4.0f
                }
            }
            WaveShape.HALUS -> {
                val s = sin(p)
                // Smooth cosine easing curve
                s * (1.5f - 0.5f * s * s)
            }
        }
    }

    /**
     * Calculates integrated phase φ(t) for smooth frequency interpolation without jumps.
     * φ(t) = 2π * frequency * (tMs / 1000.0)
     */
    fun calculateIntegratedPhase(tMs: Long, frequencyHz: Float = 1.0f): Float {
        val tSec = tMs / 1000.0f
        return (2.0 * PI * frequencyHz * tSec).toFloat()
    }

    /**
     * Computes procedural 4x4 matrix transform for an Ayunan motion effect.
     * Returns identity Matrix() if effect is disabled or amplitude = 0.
     */
    fun motionMatrix(
        effect: MotionEffect,
        tMs: Long,
        layerWidth: Float = 240f,
        layerHeight: Float = 240f,
        canvasWidth: Float = 1920f
    ): Matrix {
        val matrix = Matrix()

        if (!effect.isEnabled || effect.value == 0.0f) {
            return matrix // Pure identity matrix
        }

        val amp = effect.value
        val ampPx = (amp / 100.0f) * canvasWidth

        when (effect.type) {
            EffectType.OMBANG_AMBING -> {
                val phaseX = calculateIntegratedPhase(tMs, 1.0f)
                val phaseY = calculateIntegratedPhase(tMs, 1.0f) + (PI / 4.0).toFloat()
                val dx = wave(WaveShape.SINUS, phaseX) * ampPx
                val dy = wave(WaveShape.HALUS, phaseY) * (ampPx * 0.5f)
                matrix.translate(dx, dy)
            }
            EffectType.AYUN -> {
                val phase = calculateIntegratedPhase(tMs, 1.2f)
                val angleDeg = wave(WaveShape.SINUS, phase) * amp
                matrix.translate(0f, -layerHeight / 2f)
                matrix.rotateZ(angleDeg)
                matrix.translate(0f, layerHeight / 2f)
            }
            EffectType.GOYANG -> {
                val phase = calculateIntegratedPhase(tMs, 2.5f)
                val dx = wave(WaveShape.SINUS, phase) * ampPx
                val dy = wave(WaveShape.SEGITIGA, phase * 1.3f) * (ampPx * 0.4f)
                val rot = wave(WaveShape.HALUS, phase * 0.8f) * (amp * 0.5f)
                matrix.translate(dx, dy)
                matrix.rotateZ(rot)
            }
            EffectType.MENGAMBANG -> {
                val phase = calculateIntegratedPhase(tMs, 0.8f)
                val dy = wave(WaveShape.SINUS, phase) * ampPx
                val scaleFactor = 1.0f + wave(WaveShape.HALUS, phase * 2.0f) * 0.03f
                matrix.translate(0f, dy)
                matrix.scale(scaleFactor, scaleFactor)
            }
            EffectType.OLENG -> {
                val phase = calculateIntegratedPhase(tMs, 1.0f)
                val rotDeg = wave(WaveShape.SINUS, phase) * amp
                matrix.rotateZ(rotDeg)
            }
            EffectType.JELLY -> {
                val phase = calculateIntegratedPhase(tMs, 2.0f)
                val s = wave(WaveShape.SINUS, phase) * amp
                val scaleX = (1.0f + s).coerceAtLeast(0.01f)
                val scaleY = (1.0f - s).coerceAtLeast(0.01f)
                matrix.scale(scaleX, scaleY)
            }
            EffectType.PEGAS -> {
                val phase = calculateIntegratedPhase(tMs, 1.8f)
                val periodMs = 2000L
                val timeInCycleMs = (tMs % periodMs)
                val decay = exp(-0.0015f * timeInCycleMs)
                val dy = wave(WaveShape.SINUS, phase) * ampPx * decay
                matrix.translate(0f, dy)
            }
            EffectType.ORBIT -> {
                val phase = calculateIntegratedPhase(tMs, 1.0f)
                val dx = cos(phase) * ampPx
                val dy = sin(phase) * ampPx
                matrix.translate(dx, dy)
            }
            EffectType.PUTAR -> {
                val rotDeg = ((amp * (tMs / 1000.0f)) % 360.0f)
                matrix.rotateZ(rotDeg)
            }
            EffectType.MELAYANG -> {
                val phase = calculateIntegratedPhase(tMs, 0.6f)
                val dx = wave(WaveShape.SINUS, phase) * ampPx
                val dy = wave(WaveShape.SINUS, phase * 2.0f) * (ampPx * 0.5f)
                matrix.translate(dx, dy)
            }
            else -> {
                // Non-ayunan effects return identity matrix for matrix transform
            }
        }

        return matrix
    }
}
