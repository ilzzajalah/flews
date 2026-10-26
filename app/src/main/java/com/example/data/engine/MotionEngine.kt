package com.example.data.engine

import com.example.data.model.EasingType
import com.example.data.model.EffectType
import com.example.data.model.KeyframeProperty
import com.example.data.model.MotionKeyframe
import com.example.data.model.MotionLayer
import kotlin.math.cos
import kotlin.math.sin

data class EvaluatedTransform(
    val posX: Float,
    val posY: Float,
    val scaleX: Float,
    val scaleY: Float,
    val rotationDeg: Float,
    val opacity: Float,
    val isTimeActive: Boolean,
    val effectValues: Map<EffectType, Float>
)

object MotionEngine {

    fun evaluateLayer(
        layer: MotionLayer,
        currentTimeMs: Long,
        allLayers: Map<String, MotionLayer> = emptyMap(),
        visitedParentIds: Set<String> = emptySet()
    ): EvaluatedTransform {
        val isTimeActive = currentTimeMs in layer.inTimeMs..layer.outTimeMs

        // 1. Evaluate local properties
        val localPosX = interpolateProperty(layer.keyframes, KeyframeProperty.POSITION_X, currentTimeMs, layer.positionX)
        val localPosY = interpolateProperty(layer.keyframes, KeyframeProperty.POSITION_Y, currentTimeMs, layer.positionY)
        val localScaleX = interpolateProperty(layer.keyframes, KeyframeProperty.SCALE_X, currentTimeMs, layer.scaleX)
        val localScaleY = interpolateProperty(layer.keyframes, KeyframeProperty.SCALE_Y, currentTimeMs, layer.scaleY)
        val localRotation = interpolateProperty(layer.keyframes, KeyframeProperty.ROTATION, currentTimeMs, layer.rotationDeg)
        val localOpacity = interpolateProperty(layer.keyframes, KeyframeProperty.OPACITY, currentTimeMs, layer.opacity).coerceIn(0f, 1f)

        // 2. Evaluate effects
        val evaluatedEffects = mutableMapOf<EffectType, Float>()
        layer.effects.filter { it.isEnabled }.forEach { fx ->
            val fxVal = interpolateProperty(layer.keyframes, KeyframeProperty.EFFECT_VALUE, currentTimeMs, fx.value)
            evaluatedEffects[fx.type] = fxVal
        }

        // 3. Resolve parent transform if layer is parented (Null Object or another layer)
        if (layer.parentId != null && allLayers.containsKey(layer.parentId) && !visitedParentIds.contains(layer.parentId)) {
            val parent = allLayers[layer.parentId]!!
            val parentTransform = evaluateLayer(parent, currentTimeMs, allLayers, visitedParentIds + layer.id)

            // Combined scale
            val finalScaleX = localScaleX * parentTransform.scaleX
            val finalScaleY = localScaleY * parentTransform.scaleY

            // Combined rotation
            val finalRotation = (localRotation + parentTransform.rotationDeg) % 360f

            // Rotate local offset by parent rotation
            val rad = Math.toRadians(parentTransform.rotationDeg.toDouble())
            val cosA = cos(rad).toFloat()
            val sinA = sin(rad).toFloat()
            val rotatedOffsetX = (localPosX * parentTransform.scaleX * cosA) - (localPosY * parentTransform.scaleY * sinA)
            val rotatedOffsetY = (localPosX * parentTransform.scaleX * sinA) + (localPosY * parentTransform.scaleY * cosA)

            val finalPosX = parentTransform.posX + rotatedOffsetX
            val finalPosY = parentTransform.posY + rotatedOffsetY
            val finalOpacity = (localOpacity * parentTransform.opacity).coerceIn(0f, 1f)

            return EvaluatedTransform(
                posX = finalPosX,
                posY = finalPosY,
                scaleX = finalScaleX,
                scaleY = finalScaleY,
                rotationDeg = finalRotation,
                opacity = finalOpacity,
                isTimeActive = isTimeActive && parentTransform.isTimeActive,
                effectValues = evaluatedEffects
            )
        }

        return EvaluatedTransform(
            posX = localPosX,
            posY = localPosY,
            scaleX = localScaleX,
            scaleY = localScaleY,
            rotationDeg = localRotation,
            opacity = localOpacity,
            isTimeActive = isTimeActive,
            effectValues = evaluatedEffects
        )
    }

    private fun interpolateProperty(
        keyframes: List<MotionKeyframe>,
        property: KeyframeProperty,
        currentTimeMs: Long,
        defaultValue: Float
    ): Float {
        val propertyKeyframes = keyframes.filter { it.property == property }.sortedBy { it.timeMs }
        if (propertyKeyframes.isEmpty()) return defaultValue
        if (propertyKeyframes.size == 1) return propertyKeyframes.first().value

        if (currentTimeMs <= propertyKeyframes.first().timeMs) {
            return propertyKeyframes.first().value
        }
        if (currentTimeMs >= propertyKeyframes.last().timeMs) {
            return propertyKeyframes.last().value
        }

        // Find surrounding keyframes
        for (i in 0 until propertyKeyframes.size - 1) {
            val k1 = propertyKeyframes[i]
            val k2 = propertyKeyframes[i + 1]
            if (currentTimeMs in k1.timeMs..k2.timeMs) {
                val duration = (k2.timeMs - k1.timeMs).toFloat()
                if (duration <= 0f) return k1.value
                val t = (currentTimeMs - k1.timeMs) / duration
                val easedT = applyEasing(t, k1.easing)
                return k1.value + (k2.value - k1.value) * easedT
            }
        }

        return defaultValue
    }

    fun applyEasing(t: Float, easing: EasingType): Float {
        val clampedT = t.coerceIn(0f, 1f)
        return when (easing) {
            EasingType.LINEAR -> clampedT
            EasingType.EASE_IN -> clampedT * clampedT * clampedT
            EasingType.EASE_OUT -> 1f - (1f - clampedT) * (1f - clampedT) * (1f - clampedT)
            EasingType.EASE_IN_OUT -> {
                if (clampedT < 0.5f) {
                    4f * clampedT * clampedT * clampedT
                } else {
                    1f - (-2f * clampedT + 2f) * (-2f * clampedT + 2f) * (-2f * clampedT + 2f) / 2f
                }
            }
        }
    }
}
