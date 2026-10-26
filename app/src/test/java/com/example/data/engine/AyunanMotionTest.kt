package com.example.data.engine

import androidx.compose.ui.graphics.Matrix
import com.example.data.model.EffectType
import com.example.data.model.LayerJsonSerializer
import com.example.data.model.LayerType
import com.example.data.model.MotionEffect
import com.example.data.model.MotionLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AyunanMotionTest {

    private val ayunanTypes = listOf(
        EffectType.OMBANG_AMBING,
        EffectType.AYUN,
        EffectType.GOYANG,
        EffectType.MENGAMBANG,
        EffectType.OLENG,
        EffectType.JELLY,
        EffectType.PEGAS,
        EffectType.ORBIT,
        EffectType.PUTAR,
        EffectType.MELAYANG
    )

    @Test
    fun `test identity matrix when amplitude is zero or effect disabled`() {
        val identityMatrix = Matrix()

        ayunanTypes.forEach { type ->
            // Amplitude = 0
            val zeroAmpEffect = MotionEffect(id = "test_0", type = type, value = 0.0f, isEnabled = true)
            val matrixZero = AyunanEngine.motionMatrix(zeroAmpEffect, tMs = 1500L, canvasWidth = 1920f)
            assertMatrixEquals(identityMatrix, matrixZero, "Effect ${type.name} with amp=0 must return identity matrix")

            // Disabled effect
            val disabledEffect = MotionEffect(id = "test_disabled", type = type, value = type.defaultVal, isEnabled = false)
            val matrixDisabled = AyunanEngine.motionMatrix(disabledEffect, tMs = 1500L, canvasWidth = 1920f)
            assertMatrixEquals(identityMatrix, matrixDisabled, "Disabled effect ${type.name} must return identity matrix")
        }
    }

    @Test
    fun `test wave shape helper formulas and bounds`() {
        // Sinus at 0 and PI/2
        assertEquals(0.0f, AyunanEngine.wave(WaveShape.SINUS, 0.0f), 0.001f)
        assertEquals(1.0f, AyunanEngine.wave(WaveShape.SINUS, (Math.PI / 2).toFloat()), 0.001f)

        // Segitiga (triangle) peak at PI/2
        val triPeak = AyunanEngine.wave(WaveShape.SEGITIGA, (Math.PI / 2).toFloat())
        assertEquals(1.0f, triPeak, 0.01f)

        // Halus (smooth) peak at PI/2
        val halusPeak = AyunanEngine.wave(WaveShape.HALUS, (Math.PI / 2).toFloat())
        assertEquals(1.0f, halusPeak, 0.01f)
    }

    @Test
    fun `test continuous phase integration`() {
        val phase0 = AyunanEngine.calculateIntegratedPhase(tMs = 0L, frequencyHz = 1.0f)
        val phase500 = AyunanEngine.calculateIntegratedPhase(tMs = 500L, frequencyHz = 1.0f)
        val phase1000 = AyunanEngine.calculateIntegratedPhase(tMs = 1000L, frequencyHz = 1.0f)

        assertEquals(0.0f, phase0, 0.001f)
        assertEquals(Math.PI.toFloat(), phase500, 0.01f)
        assertEquals((2 * Math.PI).toFloat(), phase1000, 0.01f)
    }

    @Test
    fun `test wave periodicity for constant frequency`() {
        val effect = MotionEffect(id = "omb1", type = EffectType.OMBANG_AMBING, value = 10.0f, isEnabled = true)
        val matrix1 = AyunanEngine.motionMatrix(effect, tMs = 200L, canvasWidth = 1920f)
        val matrix2 = AyunanEngine.motionMatrix(effect, tMs = 1200L, canvasWidth = 1920f) // +1000ms = 1 full period at 1Hz

        assertMatrixEquals(matrix1, matrix2, "Ombang-ambing must be periodic with T=1000ms")
    }

    @Test
    fun `test matrix calculation determinism`() {
        val effect = MotionEffect(id = "ayun1", type = EffectType.AYUN, value = 15.0f, isEnabled = true)
        val m1 = AyunanEngine.motionMatrix(effect, tMs = 750L, layerWidth = 240f, layerHeight = 240f, canvasWidth = 1920f)
        val m2 = AyunanEngine.motionMatrix(effect, tMs = 750L, layerWidth = 240f, layerHeight = 240f, canvasWidth = 1920f)

        assertMatrixEquals(m1, m2, "AyunanEngine must be 100% deterministic")
    }

    @Test
    fun `test json serialization round-trip for ayunan effects`() {
        ayunanTypes.forEach { type ->
            val originalEffect = MotionEffect(id = "fx_${type.name}", type = type, value = type.defaultVal, isEnabled = true)
            val json = originalEffect.toJson()
            val deserialized = MotionEffect.fromJson(json)

            assertEquals(originalEffect.id, deserialized.id)
            assertEquals(originalEffect.type, deserialized.type)
            assertEquals(originalEffect.value, deserialized.value, 0.001f)
            assertEquals(originalEffect.isEnabled, deserialized.isEnabled)
        }
    }

    @Test
    fun `test initial project state has 0 active effects`() {
        // Registry must contain 10 Ayunan descriptors
        val ayunanDescriptors = EffectRegistry.getAyunanDescriptors()
        assertEquals(10, ayunanDescriptors.size)

        // Total 35 effects registered
        assertEquals(35, EffectRegistry.getAllDescriptors().size)

        // New layer must start with 0 active effects
        val newLayer = MotionLayer(id = "l1", name = "Layer 1", type = com.example.data.model.LayerType.SHAPE)
        assertTrue("New layer must start with 0 active effects", newLayer.effects.isEmpty())
    }

    private fun assertMatrixEquals(expected: Matrix, actual: Matrix, message: String) {
        val expValues = expected.values
        val actValues = actual.values
        assertEquals("${message} - length mismatch", expValues.size, actValues.size)
        for (i in expValues.indices) {
            assertEquals("${message} at index $i", expValues[i], actValues[i], 0.001f)
        }
    }
}
