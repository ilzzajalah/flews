package com.example.data.engine

import com.example.data.model.EffectType
import com.example.data.model.MotionEffect
import java.util.UUID

data class EffectDescriptor(
    val type: EffectType,
    val displayName: String,
    val category: String,
    val minVal: Float,
    val maxVal: Float,
    val defaultVal: Float
)

object EffectRegistry {

    val CATEGORY_VISUAL = "Visual"
    val CATEGORY_TRANSFORM = "Transform"
    val CATEGORY_AYUNAN = "Ayunan"

    val categories = listOf(CATEGORY_VISUAL, CATEGORY_TRANSFORM, CATEGORY_AYUNAN)

    private val descriptors: List<EffectDescriptor> by lazy {
        EffectType.values().map { type ->
            EffectDescriptor(
                type = type,
                displayName = type.displayName,
                category = type.category,
                minVal = type.minVal,
                maxVal = type.maxVal,
                defaultVal = type.defaultVal
            )
        }
    }

    fun getAllDescriptors(): List<EffectDescriptor> = descriptors

    fun getDescriptorsByCategory(category: String): List<EffectDescriptor> =
        descriptors.filter { it.category == category }

    fun getAyunanDescriptors(): List<EffectDescriptor> =
        getDescriptorsByCategory(CATEGORY_AYUNAN)

    fun createEffect(type: EffectType, customValue: Float? = null): MotionEffect =
        MotionEffect(
            id = UUID.randomUUID().toString(),
            type = type,
            value = customValue ?: type.defaultVal,
            isEnabled = true
        )
}
