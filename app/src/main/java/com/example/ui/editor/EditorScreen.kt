package com.example.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.command.AddEffectCommand
import com.example.data.command.RemoveEffectCommand
import com.example.data.command.ReorderEffectCommand
import com.example.data.command.ToggleEffectCommand
import com.example.data.command.UpdateEffectValueCommand
import com.example.data.engine.EffectRegistry
import com.example.data.model.EasingType
import com.example.data.model.KeyframeProperty
import com.example.data.model.LayerJsonSerializer
import com.example.data.model.LayerType
import com.example.data.model.MotionEffect
import com.example.data.model.MotionKeyframe
import com.example.data.model.MotionLayer
import com.example.data.model.ProjectEntity
import com.example.data.repository.ProjectRepository
import com.example.ui.editor.panels.AddLayerSheet
import com.example.ui.editor.panels.EffectsPanel
import com.example.ui.editor.panels.ExportSheet
import com.example.ui.editor.panels.KeyframePanel
import com.example.ui.editor.panels.LayersPanel
import com.example.ui.editor.panels.ProjectSettingsDialog
import com.example.ui.editor.panels.ShapeSheet
import com.example.ui.editor.panels.TextSheet
import com.example.ui.theme.FlewsBackground
import com.example.ui.theme.FlewsBorder
import com.example.ui.theme.FlewsPrimary
import com.example.ui.theme.FlewsPrimaryLight
import com.example.ui.theme.FlewsSurface
import com.example.ui.theme.FlewsSurfaceHigh
import com.example.ui.theme.FlewsTextPrimary
import com.example.ui.theme.FlewsTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class ActivePanel {
    NONE,
    LAYERS,
    KEYFRAMES,
    EFFECTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    initialProject: ProjectEntity,
    projectRepository: ProjectRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var project by remember { mutableStateOf(initialProject) }
    var layers by remember {
        mutableStateOf(LayerJsonSerializer.deserialize(initialProject.layersJson))
    }
    var selectedLayerId by remember { mutableStateOf<String?>(null) }
    var currentTimeMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLooping by remember { mutableStateOf(true) }

    // Undo / Redo history
    var undoStack by remember { mutableStateOf(listOf<List<MotionLayer>>()) }
    var redoStack by remember { mutableStateOf(listOf<List<MotionLayer>>()) }

    fun pushUndo(newLayers: List<MotionLayer>) {
        undoStack = undoStack + listOf(layers)
        redoStack = emptyList()
        layers = newLayers
        // Auto-save project changes
        coroutineScope.launch {
            val updated = project.copy(layersJson = LayerJsonSerializer.serialize(newLayers))
            project = updated
            projectRepository.updateProject(updated)
        }
    }

    // Panels & Sheets state
    var activePanel by remember { mutableStateOf(ActivePanel.NONE) }
    var showAddLayerSheet by remember { mutableStateOf(false) }
    var showProjectSettingsDialog by remember { mutableStateOf(false) }
    var showShapeSheet by remember { mutableStateOf(false) }
    var showTextSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Media Photo & Video Picker (Zero-permission)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val isVideo = mimeType.startsWith("video/")
            val newLayer = MotionLayer(
                id = UUID.randomUUID().toString(),
                name = if (isVideo) "Video ${layers.count { it.isVideo } + 1}" else "Foto ${layers.count { it.type == LayerType.MEDIA && !it.isVideo } + 1}",
                type = LayerType.MEDIA,
                mediaUri = uri.toString(),
                isVideo = isVideo,
                width = 320f,
                height = 240f,
                inTimeMs = 0L,
                outTimeMs = project.durationMs
            )
            pushUndo(layers + newLayer)
            selectedLayerId = newLayer.id
        }
    }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val file = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
                val newLayer = MotionLayer(
                    id = UUID.randomUUID().toString(),
                    name = "Kamera ${layers.count { it.type == LayerType.MEDIA } + 1}",
                    type = LayerType.MEDIA,
                    mediaUri = Uri.fromFile(file).toString(),
                    width = 300f,
                    height = 300f,
                    inTimeMs = 0L,
                    outTimeMs = project.durationMs
                )
                pushUndo(layers + newLayer)
                selectedLayerId = newLayer.id
            } catch (e: Exception) {
                // Ignore failure
            }
        }
    }

    // Playback Loop
    LaunchedEffect(isPlaying, project.durationMs, project.fps, isLooping, layers.size) {
        if (layers.isEmpty()) {
            isPlaying = false
        }
        val frameMs = (1000f / project.fps).toLong().coerceAtLeast(16L)
        while (isPlaying && layers.isNotEmpty()) {
            delay(frameMs)
            val nextTime = currentTimeMs + frameMs
            if (nextTime >= project.durationMs) {
                if (isLooping) {
                    currentTimeMs = 0L
                } else {
                    currentTimeMs = project.durationMs
                    isPlaying = false
                }
            } else {
                currentTimeMs = nextTime
            }
        }
    }

    val handleSaveAndBack = {
        coroutineScope.launch {
            val updated = project.copy(layersJson = LayerJsonSerializer.serialize(layers))
            projectRepository.updateProject(updated)
            onBack()
        }
    }

    BackHandler {
        if (activePanel != ActivePanel.NONE) {
            activePanel = ActivePanel.NONE
        } else {
            handleSaveAndBack()
        }
    }

    val selectedLayer = layers.find { it.id == selectedLayerId }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.testTag("topbar"),
                navigationIcon = {
                    IconButton(
                        onClick = { handleSaveAndBack() },
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Kembali ke Beranda",
                            tint = FlewsTextPrimary
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showRenameDialog = true }
                            .padding(vertical = 4.dp)
                            .testTag("project_name_header")
                    ) {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = FlewsTextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Ubah Nama",
                            tint = FlewsTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                actions = {
                    // Undo
                    IconButton(
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                val prev = undoStack.last()
                                undoStack = undoStack.dropLast(1)
                                redoStack = redoStack + listOf(layers)
                                layers = prev
                                coroutineScope.launch {
                                    val updated = project.copy(layersJson = LayerJsonSerializer.serialize(prev))
                                    project = updated
                                    projectRepository.updateProject(updated)
                                }
                            }
                        },
                        enabled = undoStack.isNotEmpty(),
                        modifier = Modifier.testTag("undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Undo,
                            contentDescription = "Urungkan",
                            tint = if (undoStack.isNotEmpty()) FlewsTextPrimary else FlewsBorder
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                val next = redoStack.last()
                                redoStack = redoStack.dropLast(1)
                                undoStack = undoStack + listOf(layers)
                                layers = next
                                coroutineScope.launch {
                                    val updated = project.copy(layersJson = LayerJsonSerializer.serialize(next))
                                    project = updated
                                    projectRepository.updateProject(updated)
                                }
                            }
                        },
                        enabled = redoStack.isNotEmpty(),
                        modifier = Modifier.testTag("redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Redo,
                            contentDescription = "Ulangi",
                            tint = if (redoStack.isNotEmpty()) FlewsTextPrimary else FlewsBorder
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // 36dp Circular Icon Export Button
                    IconButton(
                        onClick = { showExportSheet = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(FlewsPrimary)
                            .testTag("top_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileUpload,
                            contentDescription = "Export Proyek",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FlewsSurface)
            )
        },
        bottomBar = {
            // ALWAYS RENDERED 56dp Persistent Dock with 150ms AnimatedContent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(FlewsSurface)
                    .border(1.dp, FlewsBorder)
                    .testTag("dock_container")
            ) {
                AnimatedContent(
                    targetState = (selectedLayer != null),
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(150)) + slideInVertically(animationSpec = tween(150)) { it })
                            .togetherWith(fadeOut(animationSpec = tween(150)) + slideOutVertically(animationSpec = tween(150)) { it })
                    },
                    label = "dockTransition"
                ) { isLayerSelected ->
                    if (!isLayerSelected) {
                        // MAIN DOCK (0 Layer / No selection)
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DockButton(
                                icon = Icons.Outlined.Add,
                                label = "Tambah",
                                testTag = "dock_add_button",
                                onClick = { showAddLayerSheet = true }
                            )
                            DockButton(
                                icon = Icons.Outlined.ColorLens,
                                label = "Latar",
                                testTag = "dock_bg_button",
                                onClick = { showColorDialog = true }
                            )
                            DockButton(
                                icon = Icons.Outlined.Settings,
                                label = "Proyek",
                                testTag = "dock_project_button",
                                onClick = { showProjectSettingsDialog = true }
                            )
                            DockButton(
                                icon = Icons.Outlined.Layers,
                                label = "Layer",
                                isSelected = activePanel == ActivePanel.LAYERS,
                                testTag = "dock_layers_button",
                                onClick = {
                                    activePanel = if (activePanel == ActivePanel.LAYERS) ActivePanel.NONE else ActivePanel.LAYERS
                                }
                            )
                        }
                    } else {
                        // LAYER DOCK (1 Layer selected)
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DockButton(
                                icon = Icons.Outlined.Edit,
                                label = "Edit",
                                testTag = "dock_edit_button",
                                onClick = {
                                    when (selectedLayer?.type) {
                                        LayerType.TEXT -> showTextSheet = true
                                        LayerType.SHAPE -> showShapeSheet = true
                                        LayerType.MEDIA -> photoPickerLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                            )
                                        )
                                        else -> showRenameDialog = true
                                    }
                                }
                            )
                            DockButton(
                                icon = Icons.Outlined.Diamond,
                                label = "Transform",
                                isSelected = activePanel == ActivePanel.KEYFRAMES,
                                testTag = "dock_transform_button",
                                onClick = {
                                    activePanel = if (activePanel == ActivePanel.KEYFRAMES) ActivePanel.NONE else ActivePanel.KEYFRAMES
                                }
                            )
                            DockButton(
                                icon = Icons.Outlined.Palette,
                                label = "Warna",
                                testTag = "dock_color_button",
                                onClick = { showColorDialog = true }
                            )
                            DockButton(
                                icon = Icons.Outlined.AutoAwesome,
                                label = "Efek",
                                isSelected = activePanel == ActivePanel.EFFECTS,
                                testTag = "dock_effects_button",
                                onClick = {
                                    activePanel = if (activePanel == ActivePanel.EFFECTS) ActivePanel.NONE else ActivePanel.EFFECTS
                                }
                            )
                            DockButton(
                                icon = Icons.Outlined.Layers,
                                label = "Blending",
                                isSelected = activePanel == ActivePanel.LAYERS,
                                testTag = "dock_blending_button",
                                onClick = {
                                    activePanel = if (activePanel == ActivePanel.LAYERS) ActivePanel.NONE else ActivePanel.LAYERS
                                }
                            )
                            DockButton(
                                icon = Icons.Outlined.Schedule,
                                label = "Timing",
                                testTag = "dock_timing_button",
                                onClick = {
                                    activePanel = if (activePanel == ActivePanel.LAYERS) ActivePanel.NONE else ActivePanel.LAYERS
                                }
                            )
                            DockButton(
                                icon = Icons.Outlined.Layers,
                                label = "Layer",
                                isSelected = activePanel == ActivePanel.LAYERS,
                                testTag = "dock_layers_panel_button",
                                onClick = {
                                    activePanel = if (activePanel == ActivePanel.LAYERS) ActivePanel.NONE else ActivePanel.LAYERS
                                }
                            )
                            DockButton(
                                icon = Icons.Outlined.Close,
                                label = "Batal",
                                testTag = "dock_deselect_button",
                                onClick = { selectedLayerId = null }
                            )
                        }
                    }
                }
            }
        },
        containerColor = FlewsBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Canvas Pratinjau
            CanvasPreview(
                projectWidth = project.width,
                projectHeight = project.height,
                backgroundColorHex = project.backgroundColor,
                layers = layers,
                selectedLayerId = selectedLayerId,
                currentTimeMs = currentTimeMs,
                onSelectLayer = { selectedLayerId = it },
                onUpdateLayerTransform = { id, posX, posY, scaleX, scaleY, rot ->
                    val updated = layers.map { l ->
                        if (l.id == id) {
                            l.copy(
                                positionX = posX,
                                positionY = posY,
                                scaleX = scaleX,
                                scaleY = scaleY,
                                rotationDeg = rot
                            )
                        } else l
                    }
                    pushUndo(updated)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f)
            )

            // 2. Baris Kontrol Pemutaran
            PlaybackControls(
                currentTimeMs = currentTimeMs,
                totalDurationMs = project.durationMs,
                fps = project.fps,
                isPlaying = isPlaying,
                isLooping = isLooping,
                selectedLayer = selectedLayer,
                totalLayersCount = layers.size,
                onTogglePlay = { isPlaying = !isPlaying },
                onStepPrevFrame = {
                    val frameMs = (1000f / project.fps).toLong().coerceAtLeast(1L)
                    currentTimeMs = (currentTimeMs - frameMs).coerceAtLeast(0L)
                },
                onStepNextFrame = {
                    val frameMs = (1000f / project.fps).toLong().coerceAtLeast(1L)
                    currentTimeMs = (currentTimeMs + frameMs).coerceAtMost(project.durationMs)
                },
                onSeekToStart = { currentTimeMs = 0L },
                onToggleLoop = { isLooping = !isLooping },
                onToggleKeyframeAtCurrentTime = {
                    if (selectedLayer != null) {
                        val hasKf = selectedLayer.keyframes.any {
                            kotlin.math.abs(it.timeMs - currentTimeMs) < 50
                        }
                        val updatedKf = if (hasKf) {
                            selectedLayer.keyframes.filterNot { kotlin.math.abs(it.timeMs - currentTimeMs) < 50 }
                        } else {
                            selectedLayer.keyframes + listOf(
                                MotionKeyframe(
                                    id = UUID.randomUUID().toString(),
                                    property = KeyframeProperty.POSITION_X,
                                    timeMs = currentTimeMs,
                                    value = selectedLayer.positionX,
                                    easing = EasingType.EASE_IN_OUT
                                ),
                                MotionKeyframe(
                                    id = UUID.randomUUID().toString(),
                                    property = KeyframeProperty.POSITION_Y,
                                    timeMs = currentTimeMs,
                                    value = selectedLayer.positionY,
                                    easing = EasingType.EASE_IN_OUT
                                )
                            )
                        }
                        val updatedLayers = layers.map {
                            if (it.id == selectedLayer.id) it.copy(keyframes = updatedKf) else it
                        }
                        pushUndo(updatedLayers)
                    }
                }
            )

            // 3. Dynamic Sliding Panel (Layers, Keyframes, Effects)
            AnimatedVisibility(
                visible = activePanel != ActivePanel.NONE,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                when (activePanel) {
                    ActivePanel.LAYERS -> {
                        LayersPanel(
                            layers = layers,
                            selectedLayerId = selectedLayerId,
                            onSelectLayer = { selectedLayerId = it },
                            onToggleVisibility = { id ->
                                pushUndo(layers.map { if (it.id == id) it.copy(isVisible = !it.isVisible) else it })
                            },
                            onToggleLock = { id ->
                                pushUndo(layers.map { if (it.id == id) it.copy(isLocked = !it.isLocked) else it })
                            },
                            onDuplicateLayer = { id ->
                                val orig = layers.find { it.id == id } ?: return@LayersPanel
                                val copy = orig.copy(
                                    id = UUID.randomUUID().toString(),
                                    name = "${orig.name} (Salinan)",
                                    positionX = orig.positionX + 30f,
                                    positionY = orig.positionY + 30f
                                )
                                pushUndo(layers + copy)
                                selectedLayerId = copy.id
                            },
                            onDeleteLayer = { id ->
                                pushUndo(layers.filterNot { it.id == id })
                                if (selectedLayerId == id) selectedLayerId = null
                            },
                            onMoveLayerUp = { idx ->
                                if (idx > 0) {
                                    val list = layers.toMutableList()
                                    val item = list.removeAt(idx)
                                    list.add(idx - 1, item)
                                    pushUndo(list)
                                }
                            },
                            onMoveLayerDown = { idx ->
                                if (idx < layers.size - 1) {
                                    val list = layers.toMutableList()
                                    val item = list.removeAt(idx)
                                    list.add(idx + 1, item)
                                    pushUndo(list)
                                }
                            },
                            onUpdateOpacity = { id, op ->
                                pushUndo(layers.map { if (it.id == id) it.copy(opacity = op) else it })
                            },
                            onUpdateBlendMode = { id, bm ->
                                pushUndo(layers.map { if (it.id == id) it.copy(blendMode = bm) else it })
                            },
                            onUpdateParent = { id, pId ->
                                pushUndo(layers.map { if (it.id == id) it.copy(parentId = pId) else it })
                            },
                            onClose = { activePanel = ActivePanel.NONE }
                        )
                    }
                    ActivePanel.KEYFRAMES -> {
                        KeyframePanel(
                            selectedLayer = selectedLayer,
                            currentTimeMs = currentTimeMs,
                            onAddOrUpdateKeyframe = { prop, value, easing ->
                                if (selectedLayer != null) {
                                    val remaining = selectedLayer.keyframes.filterNot {
                                        it.property == prop && kotlin.math.abs(it.timeMs - currentTimeMs) < 50
                                    }
                                    val newKf = MotionKeyframe(
                                        id = UUID.randomUUID().toString(),
                                        property = prop,
                                        timeMs = currentTimeMs,
                                        value = value,
                                        easing = easing
                                    )
                                    val updatedLayers = layers.map {
                                        if (it.id == selectedLayer.id) it.copy(keyframes = remaining + newKf) else it
                                    }
                                    pushUndo(updatedLayers)
                                }
                            },
                            onRemoveKeyframe = { kfId ->
                                if (selectedLayer != null) {
                                    val updatedLayers = layers.map {
                                        if (it.id == selectedLayer.id) {
                                            it.copy(keyframes = it.keyframes.filterNot { kf -> kf.id == kfId })
                                        } else it
                                    }
                                    pushUndo(updatedLayers)
                                }
                            },
                            onClose = { activePanel = ActivePanel.NONE }
                        )
                    }
                    ActivePanel.EFFECTS -> {
                        EffectsPanel(
                            selectedLayer = selectedLayer,
                            onAddEffect = { fxType ->
                                if (selectedLayer != null) {
                                    val newFx = EffectRegistry.createEffect(fxType)
                                    val cmd = AddEffectCommand(selectedLayer.id, newFx)
                                    val newLayers = cmd.execute(layers)
                                    pushUndo(newLayers)
                                }
                            },
                            onUpdateEffectValue = { fxId, newVal ->
                                if (selectedLayer != null) {
                                    val fx = selectedLayer.effects.find { it.id == fxId }
                                    if (fx != null) {
                                        val cmd = UpdateEffectValueCommand(selectedLayer.id, fxId, fx.value, newVal)
                                        val newLayers = cmd.execute(layers)
                                        pushUndo(newLayers)
                                    }
                                }
                            },
                            onUpdateEffectColor = { fxId, newColorHex ->
                                if (selectedLayer != null) {
                                    val fx = selectedLayer.effects.find { it.id == fxId }
                                    if (fx != null) {
                                        val cmd = com.example.data.command.UpdateEffectColorCommand(
                                            selectedLayer.id,
                                            fxId,
                                            fx.colorHex,
                                            newColorHex
                                        )
                                        val newLayers = cmd.execute(layers)
                                        pushUndo(newLayers)
                                    }
                                }
                            },
                            onToggleEffect = { fxId ->
                                if (selectedLayer != null) {
                                    val cmd = ToggleEffectCommand(selectedLayer.id, fxId)
                                    val newLayers = cmd.execute(layers)
                                    pushUndo(newLayers)
                                }
                            },
                            onRemoveEffect = { fxId ->
                                if (selectedLayer != null) {
                                    val index = selectedLayer.effects.indexOfFirst { it.id == fxId }
                                    val fx = selectedLayer.effects.find { it.id == fxId }
                                    if (index >= 0 && fx != null) {
                                        val cmd = RemoveEffectCommand(selectedLayer.id, fx, index)
                                        val newLayers = cmd.execute(layers)
                                        pushUndo(newLayers)
                                    }
                                }
                            },
                            onReorderEffect = { fromIdx, toIdx ->
                                if (selectedLayer != null) {
                                    val cmd = ReorderEffectCommand(selectedLayer.id, fromIdx, toIdx)
                                    val newLayers = cmd.execute(layers)
                                    pushUndo(newLayers)
                                }
                            },
                            onClose = { activePanel = ActivePanel.NONE }
                        )
                    }
                    ActivePanel.NONE -> Unit
                }
            }

            // 4. Timeline Multi-Track View with FAB (+) trigger
            TimelineView(
                layers = layers,
                selectedLayerId = selectedLayerId,
                currentTimeMs = currentTimeMs,
                totalDurationMs = project.durationMs,
                fps = project.fps,
                onSeekTime = { currentTimeMs = it },
                onSelectLayer = { selectedLayerId = it },
                onUpdateLayerDuration = { id, inTime, outTime ->
                    pushUndo(layers.map { if (it.id == id) it.copy(inTimeMs = inTime, outTimeMs = outTime) else it })
                },
                onAddClick = { showAddLayerSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f)
            )
        }

        // Modals & Sheets
        if (showAddLayerSheet) {
            AddLayerSheet(
                sheetState = bottomSheetState,
                onDismiss = { showAddLayerSheet = false },
                onAddMedia = {
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageAndVideo
                        )
                    )
                },
                onAddCamera = { cameraLauncher.launch(null) },
                onAddText = { showTextSheet = true },
                onAddShape = { showShapeSheet = true },
                onAddNull = {
                    val nullLayer = MotionLayer(
                        id = UUID.randomUUID().toString(),
                        name = "Null Object ${layers.count { it.type == LayerType.NULL_OBJECT } + 1}",
                        type = LayerType.NULL_OBJECT,
                        width = 200f,
                        height = 200f,
                        inTimeMs = 0L,
                        outTimeMs = project.durationMs
                    )
                    pushUndo(layers + nullLayer)
                    selectedLayerId = nullLayer.id
                }
            )
        }

        if (showProjectSettingsDialog) {
            ProjectSettingsDialog(
                project = project,
                onDismiss = { showProjectSettingsDialog = false },
                onSaveProjectSettings = { w, h, fps, durationMs ->
                    val updated = project.copy(width = w, height = h, fps = fps, durationMs = durationMs)
                    project = updated
                    coroutineScope.launch { projectRepository.updateProject(updated) }
                }
            )
        }

        if (showShapeSheet) {
            ShapeSheet(
                sheetState = bottomSheetState,
                onDismiss = { showShapeSheet = false },
                onAddShape = { type, w, h, fill, stroke, strokeW ->
                    showShapeSheet = false
                    val newLayer = MotionLayer(
                        id = UUID.randomUUID().toString(),
                        name = "Bentuk ${type.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        type = LayerType.SHAPE,
                        shapeType = type,
                        width = w,
                        height = h,
                        fillColor = fill,
                        strokeColor = stroke,
                        strokeWidth = strokeW,
                        inTimeMs = 0L,
                        outTimeMs = project.durationMs
                    )
                    pushUndo(layers + newLayer)
                    selectedLayerId = newLayer.id
                }
            )
        }

        if (showTextSheet) {
            TextSheet(
                sheetState = bottomSheetState,
                onDismiss = { showTextSheet = false },
                onAddText = { txt, sz, col, bold, italic ->
                    showTextSheet = false
                    val newLayer = MotionLayer(
                        id = UUID.randomUUID().toString(),
                        name = "Teks \"${txt.take(12)}\"",
                        type = LayerType.TEXT,
                        text = txt,
                        fontSize = sz,
                        textColor = col,
                        isBold = bold,
                        isItalic = italic,
                        inTimeMs = 0L,
                        outTimeMs = project.durationMs
                    )
                    pushUndo(layers + newLayer)
                    selectedLayerId = newLayer.id
                }
            )
        }

        if (showExportSheet) {
            ExportSheet(
                project = project,
                sheetState = bottomSheetState,
                onDismiss = { showExportSheet = false }
            )
        }

        if (showRenameDialog) {
            var tempName by remember { mutableStateOf(project.name) }
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                containerColor = FlewsSurface,
                title = { Text("Ubah Nama Proyek", color = FlewsTextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlewsPrimary,
                            unfocusedBorderColor = FlewsBorder,
                            focusedTextColor = FlewsTextPrimary,
                            unfocusedTextColor = FlewsTextPrimary
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newName = tempName.ifBlank { project.name }
                            showRenameDialog = false
                            val updated = project.copy(name = newName)
                            project = updated
                            coroutineScope.launch { projectRepository.updateProject(updated) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FlewsPrimary)
                    ) {
                        Text("Simpan", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("Batal", color = FlewsTextSecondary)
                    }
                }
            )
        }

        if (showColorDialog) {
            val canvasColors = listOf("#0A0F1E", "#000000", "#111833", "#FFFFFF", "#1E284F")
            AlertDialog(
                onDismissRequest = { showColorDialog = false },
                containerColor = FlewsSurface,
                title = { Text("Warna Canvas", color = FlewsTextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        canvasColors.forEach { hex ->
                            val isSel = hex == project.backgroundColor
                            val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Black }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color)
                                    .border(if (isSel) 2.5.dp else 1.dp, if (isSel) FlewsPrimaryLight else FlewsBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        showColorDialog = false
                                        val updated = project.copy(backgroundColor = hex)
                                        project = updated
                                        coroutineScope.launch { projectRepository.updateProject(updated) }
                                    }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showColorDialog = false }) {
                        Text("Tutup", color = FlewsTextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun DockButton(
    icon: ImageVector,
    label: String,
    testTag: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) FlewsPrimary else FlewsSurfaceHigh)
            .border(
                1.dp,
                if (isSelected) FlewsPrimaryLight else FlewsBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else FlewsTextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else FlewsTextPrimary,
                    fontSize = 11.sp
                )
            )
        }
    }
}
