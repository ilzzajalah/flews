package com.example.data.engine

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.data.model.EffectType
import com.example.data.model.MotionEffect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

object EffectRenderer {

    /**
     * Builds a combined ColorMatrix for all active visual color effects on a layer.
     * Returns null if no color effects are active.
     */
    fun buildCombinedColorMatrix(effects: List<MotionEffect>): ColorMatrix? {
        val activeEffects = effects.filter { it.isEnabled }
        if (activeEffects.isEmpty()) return null

        var combinedMatrix: ColorMatrix? = null

        for (fx in activeEffects) {
            val valF = fx.value
            val cm = when (fx.type) {
                EffectType.BRIGHTNESS -> {
                    // Brightness: shift RGB channels by valF (-1..1 mapped to -255..255)
                    val shift = valF * 255f
                    ColorMatrix(floatArrayOf(
                        1f, 0f, 0f, 0f, shift,
                        0f, 1f, 0f, 0f, shift,
                        0f, 0f, 1f, 0f, shift,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                EffectType.CONTRAST -> {
                    // Contrast: scale around midpoint 128
                    val scale = valF.coerceAtLeast(0f)
                    val translate = (-0.5f * scale + 0.5f) * 255f
                    ColorMatrix(floatArrayOf(
                        scale, 0f, 0f, 0f, translate,
                        0f, scale, 0f, 0f, translate,
                        0f, 0f, scale, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                EffectType.SATURATION -> {
                    // Saturation
                    ColorMatrix().apply { setSaturation(valF.coerceAtLeast(0f)) }
                }
                EffectType.HUE -> {
                    // Hue Rotation (-180..180 deg)
                    val matrix = ColorMatrix()
                    setHueRotation(matrix, valF)
                    matrix
                }
                EffectType.INVERT -> {
                    // Invert colors
                    val inv = valF.coerceIn(0f, 1f)
                    val k = 1f - 2f * inv
                    val offset = inv * 255f
                    ColorMatrix(floatArrayOf(
                        k, 0f, 0f, 0f, offset,
                        0f, k, 0f, 0f, offset,
                        0f, 0f, k, 0f, offset,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                EffectType.TINT -> {
                    // Tint color filter (blend with target tint color)
                    val alpha = valF.coerceIn(0f, 1f)
                    val parseCol = try { android.graphics.Color.parseColor(fx.colorHex) } catch (e: Exception) { android.graphics.Color.BLUE }
                    val r = android.graphics.Color.red(parseCol) / 255f
                    val g = android.graphics.Color.green(parseCol) / 255f
                    val b = android.graphics.Color.blue(parseCol) / 255f
                    ColorMatrix(floatArrayOf(
                        1f - alpha + alpha * r, 0f, 0f, 0f, 0f,
                        0f, 1f - alpha + alpha * g, 0f, 0f, 0f,
                        0f, 0f, 1f - alpha + alpha * b, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                EffectType.DUOTONE -> {
                    // Duotone mapping
                    val factor = valF.coerceIn(0f, 1f)
                    val parseCol = try { android.graphics.Color.parseColor(fx.colorHex) } catch (e: Exception) { android.graphics.Color.YELLOW }
                    val r = android.graphics.Color.red(parseCol) / 255f
                    val g = android.graphics.Color.green(parseCol) / 255f
                    val b = android.graphics.Color.blue(parseCol) / 255f
                    ColorMatrix(floatArrayOf(
                        0.2126f * factor * r * 2f, 0.7152f * factor * r * 2f, 0.0722f * factor * r * 2f, 0f, 20f * factor,
                        0.2126f * factor * g * 2f, 0.7152f * factor * g * 2f, 0.0722f * factor * g * 2f, 0f, 40f * factor,
                        0.2126f * factor * b * 2f, 0.7152f * factor * b * 2f, 0.0722f * factor * b * 2f, 0f, 60f * factor,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                EffectType.POSTERIZE -> {
                    // Posterize: color level quantization matrix
                    val levels = valF.coerceIn(2f, 32f)
                    val step = 255f / levels
                    ColorMatrix(floatArrayOf(
                        1f, 0f, 0f, 0f, step / 2f,
                        0f, 1f, 0f, 0f, step / 2f,
                        0f, 0f, 1f, 0f, step / 2f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                else -> null
            }

            if (cm != null) {
                if (combinedMatrix == null) {
                    combinedMatrix = cm
                } else {
                    combinedMatrix.postConcat(cm)
                }
            }
        }

        return combinedMatrix
    }

    private fun setHueRotation(matrix: ColorMatrix, degrees: Float) {
        val rad = Math.toRadians(degrees.toDouble()).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)
        val lumR = 0.213f
        val lumG = 0.715f
        val lumB = 0.072f

        val array = floatArrayOf(
            lumR + cosA * (1f - lumR) + sinA * (-lumR),
            lumG + cosA * (-lumG) + sinA * (-lumG),
            lumB + cosA * (-lumB) + sinA * (1f - lumB),
            0f, 0f,
            lumR + cosA * (-lumR) + sinA * 0.143f,
            lumG + cosA * (1f - lumG) + sinA * 0.14f,
            lumB + cosA * (-lumB) + sinA * (-0.283f),
            0f, 0f,
            lumR + cosA * (-lumR) + sinA * (-(1f - lumR)),
            lumG + cosA * (-lumG) + sinA * lumG,
            lumB + cosA * (1f - lumB) + sinA * lumB,
            0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        matrix.set(array)
    }

    /**
     * Applies all active Transform & Ayunan effects to the native Android canvas matrix.
     */
    fun applyMatrixTransformEffects(
        nativeCanvas: Canvas,
        effects: List<MotionEffect>,
        tMs: Long,
        layerWidth: Float,
        layerHeight: Float,
        canvasWidth: Float
    ) {
        val activeEffects = effects.filter { it.isEnabled }
        if (activeEffects.isEmpty()) return

        for (fx in activeEffects) {
            val valF = fx.value

            // 1. Check Ayunan Effects (Safe 4x4 -> 3x3 matrix conversion)
            if (fx.type.category == EffectRegistry.CATEGORY_AYUNAN) {
                val composeMatrix = AyunanEngine.motionMatrix(fx, tMs, layerWidth, layerHeight, canvasWidth)
                val v = composeMatrix.values
                val androidMatrix = android.graphics.Matrix().apply {
                    setValues(floatArrayOf(
                        v[0],  v[4],  v[12],
                        v[1],  v[5],  v[13],
                        0f,    0f,    1f
                    ))
                }
                nativeCanvas.concat(androidMatrix)
                continue
            }

            // 2. Check Transform Effects
            when (fx.type) {
                EffectType.SKEW -> {
                    val skewX = tan(Math.toRadians(valF.toDouble())).toFloat()
                    nativeCanvas.skew(skewX, 0f)
                }
                EffectType.WARP -> {
                    val skewX = (valF / 100f)
                    val skewY = (valF / 200f)
                    nativeCanvas.skew(skewX, skewY)
                }
                EffectType.MIRROR -> {
                    if (valF > 0.5f) {
                        nativeCanvas.scale(-1f, 1f)
                    }
                }
                EffectType.SHAKE -> {
                    val freq = 0.05f
                    val dx = sin(tMs * freq) * valF
                    val dy = cos(tMs * (freq * 1.3f)) * valF
                    nativeCanvas.translate(dx, dy)
                }
                EffectType.FISHEYE -> {
                    val factor = 1f + (valF * 0.5f)
                    nativeCanvas.scale(factor, factor)
                }
                EffectType.WAVE_WARP -> {
                    val phase = tMs * 0.008f
                    val dy = sin(phase) * valF
                    nativeCanvas.translate(0f, dy)
                }
                EffectType.DISPLACEMENT -> {
                    val dx = cos(tMs * 0.01f) * valF
                    val dy = sin(tMs * 0.01f) * valF
                    nativeCanvas.translate(dx, dy)
                }
                EffectType.RADIAL_BLUR -> {
                    val pulse = 1f + (valF / 150f) * kotlin.math.abs(sin(tMs * 0.005f))
                    nativeCanvas.scale(pulse, pulse)
                }
                EffectType.SEPARATE_RGB -> {
                    val dx = sin(tMs * 0.01f) * (valF * 0.4f)
                    nativeCanvas.translate(dx, 0f)
                }
                else -> Unit
            }
        }
    }

    /**
     * Checks if BLUR is active and returns a BlurMaskFilter.
     */
    fun getBlurMaskFilter(effects: List<MotionEffect>): BlurMaskFilter? {
        val blurFx = effects.find { it.type == EffectType.BLUR && it.isEnabled } ?: return null
        val radius = blurFx.value.coerceIn(0.1f, 50f)
        return try {
            BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if Drop Shadow is active and returns (dx, dy, blur, shadowColor)
     */
    fun getDropShadowParams(effects: List<MotionEffect>): DropShadowParams? {
        val shadowFx = effects.find { it.type == EffectType.SHADOW && it.isEnabled } ?: return null
        val dist = shadowFx.value
        val parseColor = try {
            val baseCol = android.graphics.Color.parseColor(shadowFx.colorHex)
            android.graphics.Color.argb(180, android.graphics.Color.red(baseCol), android.graphics.Color.green(baseCol), android.graphics.Color.blue(baseCol))
        } catch (e: Exception) {
            android.graphics.Color.argb(180, 0, 0, 0)
        }
        return DropShadowParams(
            dx = dist * 0.5f,
            dy = dist * 0.5f,
            blurRadius = dist,
            color = parseColor
        )
    }

    data class DropShadowParams(
        val dx: Float,
        val dy: Float,
        val blurRadius: Float,
        val color: Int
    )

    /**
     * Checks if Glow is active and returns (glowRadius, glowColor)
     */
    fun getGlowParams(effects: List<MotionEffect>): GlowParams? {
        val glowFx = effects.find { it.type == EffectType.GLOW && it.isEnabled } ?: return null
        val radius = glowFx.value.coerceIn(1f, 40f)
        val parseColor = try {
            val baseCol = android.graphics.Color.parseColor(glowFx.colorHex)
            android.graphics.Color.argb(220, android.graphics.Color.red(baseCol), android.graphics.Color.green(baseCol), android.graphics.Color.blue(baseCol))
        } catch (e: Exception) {
            android.graphics.Color.argb(220, 0, 150, 255)
        }
        return GlowParams(
            radius = radius,
            color = parseColor
        )
    }

    data class GlowParams(
        val radius: Float,
        val color: Int
    )

    /**
     * Renders special overlay passes like Vignette, Glitch, Chromatic Aberration, Noise.
     */
    fun renderOverlayEffects(
        drawScope: DrawScope,
        effects: List<MotionEffect>,
        width: Float,
        height: Float,
        tMs: Long
    ) {
        val activeEffects = effects.filter { it.isEnabled }
        if (activeEffects.isEmpty()) return

        for (fx in activeEffects) {
            when (fx.type) {
                EffectType.VIGNETTE -> {
                    val alpha = (fx.value * 0.8f).coerceIn(0f, 0.9f)
                    drawScope.drawRect(
                        color = Color.Black.copy(alpha = alpha),
                        topLeft = Offset(-width / 2f, -height / 2f),
                        size = Size(width, height)
                    )
                }
                EffectType.NOISE -> {
                    val alpha = (fx.value * 0.3f).coerceIn(0f, 0.5f)
                    val dotCount = (fx.value * 30).toInt().coerceAtMost(100)
                    for (i in 0 until dotCount) {
                        val rx = ((sin((tMs + i * 17).toDouble()) * 0.5 + 0.5) * width - width / 2f).toFloat()
                        val ry = ((cos((tMs + i * 31).toDouble()) * 0.5 + 0.5) * height - height / 2f).toFloat()
                        drawScope.drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = 1.5f,
                            center = Offset(rx, ry)
                        )
                    }
                }
                EffectType.GLITCH -> {
                    if ((tMs / 100) % 3L == 0L) {
                        val alpha = (fx.value * 0.5f).coerceIn(0f, 0.8f)
                        val sliceY = ((sin(tMs.toDouble()) * 0.5 + 0.5) * height - height / 2f).toFloat()
                        drawScope.drawRect(
                            color = Color.Cyan.copy(alpha = alpha),
                            topLeft = Offset(-width / 2f + fx.value * 10f, sliceY),
                            size = Size(width, 8f)
                        )
                    }
                }
                EffectType.CHROMATIC -> {
                    val offset = (fx.value * 0.6f).coerceIn(1f, 25f)
                    drawScope.drawRect(
                        color = Color.Red.copy(alpha = 0.25f),
                        topLeft = Offset(-width / 2f - offset, -height / 2f),
                        size = Size(width, height)
                    )
                    drawScope.drawRect(
                        color = Color.Cyan.copy(alpha = 0.25f),
                        topLeft = Offset(-width / 2f + offset, -height / 2f),
                        size = Size(width, height)
                    )
                }
                EffectType.SEPARATE_RGB -> {
                    val offset = (fx.value * 0.5f).coerceIn(1f, 20f)
                    drawScope.drawRect(
                        color = Color.Magenta.copy(alpha = 0.22f),
                        topLeft = Offset(-width / 2f + offset, -height / 2f + offset * 0.5f),
                        size = Size(width, height)
                    )
                }
                EffectType.PIXELATE -> {
                    val blockSize = fx.value.coerceIn(8f, 32f)
                    val countX = (width / blockSize).toInt().coerceAtMost(16)
                    val countY = (height / blockSize).toInt().coerceAtMost(16)
                    for (ix in 0 until countX) {
                        for (iy in 0 until countY) {
                            if ((ix + iy) % 2 == 0) {
                                drawScope.drawRect(
                                    color = Color.Black.copy(alpha = 0.06f),
                                    topLeft = Offset(-width / 2f + ix * blockSize, -height / 2f + iy * blockSize),
                                    size = Size(blockSize, blockSize)
                                )
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}
