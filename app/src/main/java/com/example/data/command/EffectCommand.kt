package com.example.data.command

import com.example.data.model.MotionEffect
import com.example.data.model.MotionLayer

/**
 * Base interface for the Command Pattern.
 */
interface Command {
    fun execute(layers: List<MotionLayer>): List<MotionLayer>
    fun undo(layers: List<MotionLayer>): List<MotionLayer>
}

/**
 * Command to add a new effect to a specific layer.
 */
data class AddEffectCommand(
    val layerId: String,
    val effect: MotionEffect
) : Command {
    override fun execute(layers: List<MotionLayer>): List<MotionLayer> {
        return layers.map { layer ->
            if (layer.id == layerId) {
                layer.copy(effects = layer.effects + effect)
            } else {
                layer
            }
        }
    }

    override fun undo(layers: List<MotionLayer>): List<MotionLayer> {
        return layers.map { layer ->
            if (layer.id == layerId) {
                layer.copy(effects = layer.effects.filterNot { it.id == effect.id })
            } else {
                layer
            }
        }
    }
}

/**
 * Command to remove an effect from a specific layer.
 */
data class RemoveEffectCommand(
    val layerId: String,
    val effect: MotionEffect,
    val index: Int
) : Command {
    override fun execute(layers: List<MotionLayer>): List<MotionLayer> {
        return layers.map { layer ->
            if (layer.id == layerId) {
                layer.copy(effects = layer.effects.filterNot { it.id == effect.id })
            } else {
                layer
            }
        }
    }

    override fun undo(layers: List<MotionLayer>): List<MotionLayer> {
        return layers.map { layer ->
            if (layer.id == layerId) {
                val currentEffects = layer.effects.toMutableList()
                val targetIndex = index.coerceIn(0, currentEffects.size)
                currentEffects.add(targetIndex, effect)
                layer.copy(effects = currentEffects)
            } else {
                layer
            }
        }
    }
}

/**
 * Command to reorder an effect within a layer's effects list.
 */
data class ReorderEffectCommand(
    val layerId: String,
    val fromIndex: Int,
    val toIndex: Int
) : Command {
    override fun execute(layers: List<MotionLayer>): List<MotionLayer> {
        return layers.map { layer ->
            if (layer.id == layerId) {
                val list = layer.effects.toMutableList()
                if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
                    val item = list.removeAt(fromIndex)
                    list.add(toIndex, item)
                }
                layer.copy(effects = list)
            } else {
                layer
            }
        }
    }

    override fun undo(layers: List<MotionLayer>): List<MotionLayer> {
        return layers.map { layer ->
            if (layer.id == layerId) {
                val list = layer.effects.toMutableList()
                if (toIndex in list.indices && fromIndex in list.indices && fromIndex != toIndex) {
                    val item = list.removeAt(toIndex)
                    list.add(fromIndex, item)
                }
                layer.copy(effects = list)
            } else {
                layer
            }
        }
    }
}

/**
 * Command to toggle an effect's enabled state.
 */
data class ToggleEffectCommand(
    val layerId: String,
    val effectId: String
) : Command {
    override fun execute(layers: List<MotionLayer>): List<MotionLayer> {
        return layers.map { layer ->
            if (layer.id == layerId) {
                val updatedEffects = layer.effects.map { fx ->
                    if (fx.id == effectId) fx.copy(isEnabled = !fx.isEnabled) else fx
                }
                layer.copy(effects = updatedEffects)
            } else {
                layer
            }
        }
    }

    override fun undo(layers: List<MotionLayer>): List<MotionLayer> {
        // Toggling again reverses the operation
        return execute(layers)
    }
}

/**
 * Command to update the value parameter of an effect.
 */
data class UpdateEffectValueCommand(
    val layerId: String,
    val effectId: String,
    val oldValue: Float,
    val newValue: Float
) : Command {
    override fun execute(layers: List<MotionLayer>): List<MotionLayer> {
        return layers.map { layer ->
            if (layer.id == layerId) {
                val updatedEffects = layer.effects.map { fx ->
                    if (fx.id == effectId) fx.copy(value = newValue) else fx
                }
                layer.copy(effects = updatedEffects)
            } else {
                layer
            }
        }
    }

    override fun undo(layers: List<MotionLayer>): List<MotionLayer> {
        return layers.map { layer ->
            if (layer.id == layerId) {
                val updatedEffects = layer.effects.map { fx ->
                    if (fx.id == effectId) fx.copy(value = oldValue) else fx
                }
                layer.copy(effects = updatedEffects)
            } else {
                layer
            }
        }
    }
}

/**
 * Command to update the colorHex parameter of an effect.
 */
data class UpdateEffectColorCommand(
    val layerId: String,
    val effectId: String,
    val oldColorHex: String,
    val newColorHex: String
) : Command {
    override fun execute(layers: List<MotionLayer>): List<MotionLayer> {
        return layers.map { layer ->
            if (layer.id == layerId) {
                val updatedEffects = layer.effects.map { fx ->
                    if (fx.id == effectId) fx.copy(colorHex = newColorHex) else fx
                }
                layer.copy(effects = updatedEffects)
            } else {
                layer
            }
        }
    }

    override fun undo(layers: List<MotionLayer>): List<MotionLayer> {
        return layers.map { layer ->
            if (layer.id == layerId) {
                val updatedEffects = layer.effects.map { fx ->
                    if (fx.id == effectId) fx.copy(colorHex = oldColorHex) else fx
                }
                layer.copy(effects = updatedEffects)
            } else {
                layer
            }
        }
    }
}

/**
 * Manager that maintains command stacks for undo/redo integration.
 */
class CommandHistoryManager {
    private val undoStack = mutableListOf<Command>()
    private val redoStack = mutableListOf<Command>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun execute(command: Command, currentLayers: List<MotionLayer>): List<MotionLayer> {
        val newLayers = command.execute(currentLayers)
        undoStack.add(command)
        redoStack.clear()
        return newLayers
    }

    fun undo(currentLayers: List<MotionLayer>): List<MotionLayer> {
        if (undoStack.isEmpty()) return currentLayers
        val command = undoStack.removeAt(undoStack.lastIndex)
        val newLayers = command.undo(currentLayers)
        redoStack.add(command)
        return newLayers
    }

    fun redo(currentLayers: List<MotionLayer>): List<MotionLayer> {
        if (redoStack.isEmpty()) return currentLayers
        val command = redoStack.removeAt(redoStack.lastIndex)
        val newLayers = command.execute(currentLayers)
        undoStack.add(command)
        return newLayers
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
