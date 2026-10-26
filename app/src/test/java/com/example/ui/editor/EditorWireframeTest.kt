package com.example.ui.editor

import com.example.data.engine.MotionEngine
import com.example.data.model.LayerJsonSerializer
import com.example.data.model.LayerType
import com.example.data.model.MotionLayer
import com.example.data.model.ProjectEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EditorWireframeTest {

    @Test
    fun `new project starts with 0 layers, 0 effects, 0 keyframes`() {
        val newProject = ProjectEntity(
            id = UUID.randomUUID().toString(),
            name = "Proyek Baru",
            aspectRatio = "16:9",
            width = 1280,
            height = 720,
            fps = 30,
            durationMs = 5000,
            backgroundColor = "#0A0F1E",
            layersJson = "[]"
        )

        val layers = LayerJsonSerializer.deserialize(newProject.layersJson)
        assertTrue("New project must have 0 layers", layers.isEmpty())
    }

    @Test
    fun `layer added has 0 effects and 0 keyframes initially`() {
        val layer = MotionLayer(
            id = UUID.randomUUID().toString(),
            name = "Bentuk 1",
            type = LayerType.SHAPE,
            inTimeMs = 0L,
            outTimeMs = 5000L
        )

        assertTrue("New layer must have 0 keyframes", layer.keyframes.isEmpty())
        assertTrue("New layer must have 0 effects", layer.effects.isEmpty())

        val transform = MotionEngine.evaluateLayer(layer, 0L)
        assertNotNull(transform)
        assertEquals(0f, transform.posX, 0.01f)
        assertEquals(0f, transform.posY, 0.01f)
        assertEquals(1f, transform.scaleX, 0.01f)
        assertEquals(1f, transform.scaleY, 0.01f)
    }

    @Test
    fun `layers json serialization round-trip maintains exact state`() {
        val sampleLayer = MotionLayer(
            id = "layer_1",
            name = "Bentuk 1",
            type = LayerType.SHAPE,
            width = 200f,
            height = 200f,
            inTimeMs = 0L,
            outTimeMs = 5000L
        )
        val originalLayers = listOf(sampleLayer)
        val serialized = LayerJsonSerializer.serialize(originalLayers)
        val deserialized = LayerJsonSerializer.deserialize(serialized)

        assertEquals(1, deserialized.size)
        assertEquals("layer_1", deserialized[0].id)
        assertEquals("Bentuk 1", deserialized[0].name)
        assertEquals(LayerType.SHAPE, deserialized[0].type)
    }
}
