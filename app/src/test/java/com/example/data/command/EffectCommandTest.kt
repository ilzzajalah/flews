package com.example.data.command

import com.example.data.model.EffectType
import com.example.data.model.LayerType
import com.example.data.model.MotionEffect
import com.example.data.model.MotionLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class EffectCommandTest {

    private val testLayerId = "layer_1"
    private val initialLayer = MotionLayer(
        id = testLayerId,
        name = "Shape Layer",
        type = LayerType.SHAPE
    )
    private val initialLayers = listOf(initialLayer)

    @Test
    fun testAddEffectCommand_ExecuteAndUndo() {
        val effect = MotionEffect(
            id = "fx_blur",
            type = EffectType.BLUR,
            value = 10f,
            isEnabled = true
        )

        val cmd = AddEffectCommand(testLayerId, effect)

        // Execute
        val afterAdd = cmd.execute(initialLayers)
        assertEquals(1, afterAdd.first().effects.size)
        assertEquals(EffectType.BLUR, afterAdd.first().effects.first().type)

        // Undo
        val afterUndo = cmd.undo(afterAdd)
        assertTrue(afterUndo.first().effects.isEmpty())
    }

    @Test
    fun testRemoveEffectCommand_ExecuteAndUndo() {
        val effect = MotionEffect(
            id = "fx_blur",
            type = EffectType.BLUR,
            value = 10f,
            isEnabled = true
        )
        val layersWithFx = listOf(initialLayer.copy(effects = listOf(effect)))

        val cmd = RemoveEffectCommand(testLayerId, effect, 0)

        // Execute
        val afterRemove = cmd.execute(layersWithFx)
        assertTrue(afterRemove.first().effects.isEmpty())

        // Undo
        val afterUndo = cmd.undo(afterRemove)
        assertEquals(1, afterUndo.first().effects.size)
        assertEquals("fx_blur", afterUndo.first().effects.first().id)
    }

    @Test
    fun testReorderEffectCommand_ExecuteAndUndo() {
        val fx1 = MotionEffect(id = "fx_1", type = EffectType.BLUR, value = 5f)
        val fx2 = MotionEffect(id = "fx_2", type = EffectType.GLITCH, value = 0.5f)
        val fx3 = MotionEffect(id = "fx_3", type = EffectType.AYUN, value = 15f)

        val layersWith3Fx = listOf(initialLayer.copy(effects = listOf(fx1, fx2, fx3)))

        val cmd = ReorderEffectCommand(testLayerId, fromIndex = 0, toIndex = 2)

        // Execute: move fx1 from index 0 to index 2 -> [fx2, fx3, fx1]
        val afterReorder = cmd.execute(layersWith3Fx)
        assertEquals(listOf("fx_2", "fx_3", "fx_1"), afterReorder.first().effects.map { it.id })

        // Undo: move fx1 back from index 2 to index 0 -> [fx1, fx2, fx3]
        val afterUndo = cmd.undo(afterReorder)
        assertEquals(listOf("fx_1", "fx_2", "fx_3"), afterUndo.first().effects.map { it.id })
    }

    @Test
    fun testToggleEffectCommand_ExecuteAndUndo() {
        val fx = MotionEffect(id = "fx_1", type = EffectType.BLUR, value = 5f, isEnabled = true)
        val layersWithFx = listOf(initialLayer.copy(effects = listOf(fx)))

        val cmd = ToggleEffectCommand(testLayerId, "fx_1")

        // Execute: toggles true -> false
        val afterToggle = cmd.execute(layersWithFx)
        assertFalse(afterToggle.first().effects.first().isEnabled)

        // Undo: toggles false -> true
        val afterUndo = cmd.undo(afterToggle)
        assertTrue(afterUndo.first().effects.first().isEnabled)
    }

    @Test
    fun testUpdateEffectValueCommand_ExecuteAndUndo() {
        val fx = MotionEffect(id = "fx_1", type = EffectType.BLUR, value = 5f)
        val layersWithFx = listOf(initialLayer.copy(effects = listOf(fx)))

        val cmd = UpdateEffectValueCommand(testLayerId, "fx_1", oldValue = 5f, newValue = 20f)

        // Execute
        val afterUpdate = cmd.execute(layersWithFx)
        assertEquals(20f, afterUpdate.first().effects.first().value)

        // Undo
        val afterUndo = cmd.undo(afterUpdate)
        assertEquals(5f, afterUndo.first().effects.first().value)
    }

    @Test
    fun testCommandHistoryManager_FullFlow() {
        val manager = CommandHistoryManager()
        val fx1 = MotionEffect(id = "fx_1", type = EffectType.BRIGHTNESS, value = 0.2f)

        // 1. Add Effect
        var layers = manager.execute(AddEffectCommand(testLayerId, fx1), initialLayers)
        assertEquals(1, layers.first().effects.size)
        assertTrue(manager.canUndo)
        assertFalse(manager.canRedo)

        // 2. Update Value from 0.2f to 0.8f
        layers = manager.execute(UpdateEffectValueCommand(testLayerId, "fx_1", 0.2f, 0.8f), layers)
        assertEquals(0.8f, layers.first().effects.first().value)

        // 3. Undo Update Value -> 0.2f
        layers = manager.undo(layers)
        assertEquals(0.2f, layers.first().effects.first().value)
        assertTrue(manager.canRedo)

        // 4. Redo Update Value -> 0.8f
        layers = manager.redo(layers)
        assertEquals(0.8f, layers.first().effects.first().value)

        // 5. Undo twice -> back to initial 0 effects
        layers = manager.undo(layers)
        layers = manager.undo(layers)
        assertTrue(layers.first().effects.isEmpty())
    }
}
