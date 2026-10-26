package com.example.data.engine

import com.example.data.model.EasingType
import com.example.data.model.KeyframeProperty
import com.example.data.model.MotionKeyframe
import com.example.data.model.MotionLayer
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyframeInterpolationTest {

    @Test
    fun testLinearEasing() {
        val t = 0.5f
        val eased = MotionEngine.applyEasing(t, EasingType.LINEAR)
        assertEquals(0.5f, eased, 0.001f)
    }

    @Test
    fun testEaseInEasing() {
        val t = 0.5f
        val eased = MotionEngine.applyEasing(t, EasingType.EASE_IN)
        assertEquals(0.125f, eased, 0.001f)
    }

    @Test
    fun testEaseOutEasing() {
        val t = 0.5f
        val eased = MotionEngine.applyEasing(t, EasingType.EASE_OUT)
        assertEquals(0.875f, eased, 0.001f)
    }

    @Test
    fun testEaseInOutEasing() {
        val tMid = 0.5f
        val easedMid = MotionEngine.applyEasing(tMid, EasingType.EASE_IN_OUT)
        assertEquals(0.5f, easedMid, 0.001f)

        val tQuarter = 0.25f
        val easedQuarter = MotionEngine.applyEasing(tQuarter, EasingType.EASE_IN_OUT)
        assertEquals(0.0625f, easedQuarter, 0.001f)
    }

    @Test
    fun testLayerKeyframeInterpolationAcrossTime() {
        val keyframes = listOf(
            MotionKeyframe(
                id = "kf1",
                property = KeyframeProperty.POSITION_X,
                timeMs = 0L,
                value = 0f,
                easing = EasingType.EASE_IN
            ),
            MotionKeyframe(
                id = "kf2",
                property = KeyframeProperty.POSITION_X,
                timeMs = 1000L,
                value = 100f,
                easing = EasingType.LINEAR
            )
        )

        val layer = MotionLayer(
            id = "layer1",
            name = "Test Layer",
            type = com.example.data.model.LayerType.SHAPE,
            inTimeMs = 0L,
            outTimeMs = 5000L,
            keyframes = keyframes
        )

        // At time = 0ms
        val transformStart = MotionEngine.evaluateLayer(layer, 0L)
        assertEquals(0f, transformStart.posX, 0.01f)

        // At time = 500ms (halfway, with Ease In t^3 -> 0.125 * 100 = 12.5f)
        val transformMid = MotionEngine.evaluateLayer(layer, 500L)
        assertEquals(12.5f, transformMid.posX, 0.01f)

        // At time = 1000ms
        val transformEnd = MotionEngine.evaluateLayer(layer, 1000L)
        assertEquals(100f, transformEnd.posX, 0.01f)
    }
}
