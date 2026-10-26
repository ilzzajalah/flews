package com.example.ui.editor

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.engine.EvaluatedTransform
import com.example.data.engine.EffectRenderer
import com.example.data.engine.MotionEngine
import com.example.data.model.EffectType
import com.example.data.model.LayerType
import com.example.data.model.MotionBlendMode
import com.example.data.model.MotionLayer
import com.example.data.model.ShapeType
import com.example.ui.theme.FlewsBorder
import com.example.ui.theme.FlewsKeyframeActive
import com.example.ui.theme.FlewsPrimary
import com.example.ui.theme.FlewsPrimaryLight
import com.example.ui.theme.FlewsSurface
import com.example.ui.theme.FlewsSurfaceHigh
import com.example.ui.theme.FlewsTextPrimary
import com.example.ui.theme.FlewsTextSecondary
import kotlin.math.atan2
import kotlin.math.roundToInt

@Composable
fun CanvasPreview(
    projectWidth: Long,
    projectHeight: Long,
    backgroundColorHex: String,
    layers: List<MotionLayer>,
    selectedLayerId: String?,
    currentTimeMs: Long,
    onSelectLayer: (String?) -> Unit,
    onUpdateLayerTransform: (layerId: String, newPosX: Float, newPosY: Float, newScaleX: Float, newScaleY: Float, newRotation: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val canvasRatio = (projectWidth.toFloat() / projectHeight.toFloat()).coerceIn(0.2f, 5f)
    val canvasBg = try {
        Color(android.graphics.Color.parseColor(backgroundColorHex))
    } catch (e: Exception) {
        FlewsSurface
    }

    val layerMap = remember(layers) { layers.associateBy { it.id } }
    val selectedLayer = layers.find { it.id == selectedLayerId }

    // Evaluated transform for selected layer
    val selectedTransform = if (selectedLayer != null) {
        remember(selectedLayer, currentTimeMs, layers) {
            MotionEngine.evaluateLayer(selectedLayer, currentTimeMs, layerMap)
        }
    } else null

    var dragMode by remember { mutableStateOf<DragHandleMode>(DragHandleMode.NONE) }
    var initialTouchOffset by remember { mutableStateOf(Offset.Zero) }

    // Local drag state to eliminate jitter/flickering (geter)
    var dragPosX by remember(selectedLayerId) { mutableFloatStateOf(selectedLayer?.positionX ?: 0f) }
    var dragPosY by remember(selectedLayerId) { mutableFloatStateOf(selectedLayer?.positionY ?: 0f) }
    var dragScaleX by remember(selectedLayerId) { mutableFloatStateOf(selectedLayer?.scaleX ?: 1f) }
    var dragScaleY by remember(selectedLayerId) { mutableFloatStateOf(selectedLayer?.scaleY ?: 1f) }
    var dragRotation by remember(selectedLayerId) { mutableFloatStateOf(selectedLayer?.rotationDeg ?: 0f) }

    LaunchedEffect(selectedLayer?.positionX, selectedLayer?.positionY, selectedLayer?.scaleX, selectedLayer?.scaleY, selectedLayer?.rotationDeg) {
        if (dragMode == DragHandleMode.NONE && selectedLayer != null) {
            dragPosX = selectedLayer.positionX
            dragPosY = selectedLayer.positionY
            dragScaleX = selectedLayer.scaleX
            dragScaleY = selectedLayer.scaleY
            dragRotation = selectedLayer.rotationDeg
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("preview"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(canvasRatio, matchHeightConstraintsFirst = false)
                .clip(RoundedCornerShape(8.dp))
                .background(canvasBg)
                .border(1.dp, FlewsBorder, RoundedCornerShape(8.dp))
                .testTag("canvas_preview_area")
        ) {
            if (layers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = "Ketuk + untuk menambah layer",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = FlewsTextSecondary.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("empty_canvas_text")
                    )
                }
            }
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedLayerId, layers) {
                        detectTapGestures { tapOffset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            // Hit test in reverse order (top layer first)
                            var hitLayer: MotionLayer? = null
                            for (layer in layers.reversed()) {
                                if (!layer.isVisible) continue
                                val t = MotionEngine.evaluateLayer(layer, currentTimeMs, layerMap)
                                if (!t.isTimeActive) continue

                                val layerCenter = center + Offset(t.posX, t.posY)
                                val halfW = (layer.width * t.scaleX) / 2f
                                val halfH = (layer.height * t.scaleY) / 2f

                                if (tapOffset.x in (layerCenter.x - halfW)..(layerCenter.x + halfW) &&
                                    tapOffset.y in (layerCenter.y - halfH)..(layerCenter.y + halfH)
                                ) {
                                    hitLayer = layer
                                    break
                                }
                            }
                            onSelectLayer(hitLayer?.id)
                        }
                    }
                    .pointerInput(selectedLayerId, selectedTransform) {
                        if (selectedLayer == null || selectedLayer.isLocked) return@pointerInput

                        detectDragGestures(
                            onDragStart = { offset ->
                                initialTouchOffset = offset
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val transform = selectedTransform ?: return@detectDragGestures
                                val boxCenter = center + Offset(transform.posX, transform.posY)
                                val halfW = (selectedLayer.width * transform.scaleX) / 2f
                                val halfH = (selectedLayer.height * transform.scaleY) / 2f

                                // Check rotation handle (above top center)
                                val rotateHandle = boxCenter + Offset(0f, -halfH - 30f)
                                if ((offset - rotateHandle).getDistance() <= 28f) {
                                    dragMode = DragHandleMode.ROTATE
                                    return@detectDragGestures
                                }

                                // Check scale corners
                                val cornerDist = 24f
                                val corners = listOf(
                                    boxCenter + Offset(-halfW, -halfH),
                                    boxCenter + Offset(halfW, -halfH),
                                    boxCenter + Offset(halfW, halfH),
                                    boxCenter + Offset(-halfW, halfH)
                                )
                                if (corners.any { (offset - it).getDistance() <= cornerDist }) {
                                    dragMode = DragHandleMode.SCALE
                                    return@detectDragGestures
                                }

                                // Otherwise position move
                                dragMode = DragHandleMode.MOVE
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                when (dragMode) {
                                    DragHandleMode.MOVE -> {
                                        dragPosX += dragAmount.x
                                        dragPosY += dragAmount.y
                                    }
                                    DragHandleMode.SCALE -> {
                                        val deltaScale = (dragAmount.x + dragAmount.y) / 200f
                                        dragScaleX = (dragScaleX + deltaScale).coerceIn(0.1f, 10f)
                                        dragScaleY = (dragScaleY + deltaScale).coerceIn(0.1f, 10f)
                                    }
                                    DragHandleMode.ROTATE -> {
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        val boxCenter = center + Offset(dragPosX, dragPosY)
                                        val angleRad = atan2(
                                            (change.position.y - boxCenter.y).toDouble(),
                                            (change.position.x - boxCenter.x).toDouble()
                                        )
                                        dragRotation = Math.toDegrees(angleRad).toFloat() + 90f
                                    }
                                    DragHandleMode.NONE -> Unit
                                }
                            },
                            onDragEnd = {
                                if (selectedLayer != null) {
                                    onUpdateLayerTransform(
                                        selectedLayer.id,
                                        dragPosX,
                                        dragPosY,
                                        dragScaleX,
                                        dragScaleY,
                                        dragRotation
                                    )
                                }
                                dragMode = DragHandleMode.NONE
                            },
                            onDragCancel = {
                                if (selectedLayer != null) {
                                    onUpdateLayerTransform(
                                        selectedLayer.id,
                                        dragPosX,
                                        dragPosY,
                                        dragScaleX,
                                        dragScaleY,
                                        dragRotation
                                    )
                                }
                                dragMode = DragHandleMode.NONE
                            }
                        )
                    }
            ) {
                val canvasCenter = Offset(size.width / 2f, size.height / 2f)

                // 1. Draw all visible layers at current time
                layers.forEach { layer ->
                    if (!layer.isVisible) return@forEach
                    val baseTransform = MotionEngine.evaluateLayer(layer, currentTimeMs, layerMap)
                    if (!baseTransform.isTimeActive) return@forEach

                    val renderTransform = if (selectedLayer != null && layer.id == selectedLayer.id && dragMode != DragHandleMode.NONE) {
                        baseTransform.copy(
                            posX = dragPosX,
                            posY = dragPosY,
                            scaleX = dragScaleX,
                            scaleY = dragScaleY,
                            rotationDeg = dragRotation
                        )
                    } else baseTransform

                    drawMotionLayer(
                        layer = layer,
                        transform = renderTransform,
                        canvasCenter = canvasCenter,
                        context = context,
                        currentTimeMs = currentTimeMs,
                        canvasWidth = size.width
                    )
                }

                // 2. Draw selection & transform handles for selected layer
                if (selectedLayer != null && selectedTransform != null && selectedTransform.isTimeActive) {
                    val handleTransform = if (dragMode != DragHandleMode.NONE) {
                        selectedTransform.copy(
                            posX = dragPosX,
                            posY = dragPosY,
                            scaleX = dragScaleX,
                            scaleY = dragScaleY,
                            rotationDeg = dragRotation
                        )
                    } else selectedTransform

                    drawTransformHandles(
                        layer = selectedLayer,
                        transform = handleTransform,
                        canvasCenter = canvasCenter
                    )
                }
            }
        }
    }
}

private enum class DragHandleMode {
    NONE,
    MOVE,
    SCALE,
    ROTATE
}

private fun DrawScope.drawMotionLayer(
    layer: MotionLayer,
    transform: EvaluatedTransform,
    canvasCenter: Offset,
    context: android.content.Context,
    currentTimeMs: Long,
    canvasWidth: Float
) {
    val layerCenter = canvasCenter + Offset(transform.posX, transform.posY)
    val calcWidth = if (layer.type == LayerType.TEXT) {
        (layer.text.length * layer.fontSize * 0.55f).coerceAtLeast(100f)
    } else layer.width

    val calcHeight = if (layer.type == LayerType.TEXT) {
        (layer.fontSize * 1.3f).coerceAtLeast(50f)
    } else layer.height

    val width = calcWidth * transform.scaleX
    val height = calcHeight * transform.scaleY
    val alpha = transform.opacity.coerceIn(0f, 1f)

    val nativeCanvas = drawContext.canvas.nativeCanvas
    nativeCanvas.save()

    // 1. Translate & Rotate base layer position
    nativeCanvas.translate(layerCenter.x, layerCenter.y)
    nativeCanvas.rotate(transform.rotationDeg)

    // 2. Apply Transform & Ayunan Effects matrix transformations
    EffectRenderer.applyMatrixTransformEffects(
        nativeCanvas = nativeCanvas,
        effects = layer.effects,
        tMs = currentTimeMs,
        layerWidth = calcWidth,
        layerHeight = calcHeight,
        canvasWidth = canvasWidth
    )

    // 3. Drop Shadow Effect pass matching exact layer geometry (text, circle, line, rect)
    val shadowParams = EffectRenderer.getDropShadowParams(layer.effects)
    if (shadowParams != null) {
        drawShadowPass(layer, transform, nativeCanvas, shadowParams, width, height)
    }

    // 4. Glow Effect pass matching exact layer geometry
    val glowParams = EffectRenderer.getGlowParams(layer.effects)
    if (glowParams != null) {
        drawGlowPass(layer, transform, nativeCanvas, glowParams, width, height)
    }

    // 5. Build combined ColorFilter & BlurMaskFilter for main layer drawing
    val colorMatrix = EffectRenderer.buildCombinedColorMatrix(layer.effects)
    val blurMaskFilter = EffectRenderer.getBlurMaskFilter(layer.effects)

    // 6. Draw Layer Content according to type
    when (layer.type) {
        LayerType.SHAPE -> {
            val baseFill = try {
                Color(android.graphics.Color.parseColor(layer.fillColor)).copy(alpha = alpha)
            } catch (e: Exception) {
                FlewsPrimary.copy(alpha = alpha)
            }
            val baseStroke = try {
                Color(android.graphics.Color.parseColor(layer.strokeColor)).copy(alpha = alpha)
            } catch (e: Exception) {
                FlewsPrimaryLight.copy(alpha = alpha)
            }

            when (layer.shapeType) {
                ShapeType.RECTANGLE -> {
                    if (colorMatrix != null || blurMaskFilter != null) {
                        val shapePaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor(layer.fillColor)
                            this.alpha = (alpha * 255).roundToInt().coerceIn(0, 255)
                            isAntiAlias = true
                            if (colorMatrix != null) colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
                            if (blurMaskFilter != null) maskFilter = blurMaskFilter
                        }
                        nativeCanvas.drawRect(-width / 2f, -height / 2f, width / 2f, height / 2f, shapePaint)
                    } else {
                        drawRect(
                            color = baseFill,
                            topLeft = Offset(-width / 2f, -height / 2f),
                            size = Size(width, height)
                        )
                    }

                    if (layer.strokeWidth > 0f) {
                        drawRect(
                            color = baseStroke,
                            topLeft = Offset(-width / 2f, -height / 2f),
                            size = Size(width, height),
                            style = Stroke(width = layer.strokeWidth * transform.scaleX)
                        )
                    }
                }
                ShapeType.CIRCLE -> {
                    val radius = (width.coerceAtMost(height)) / 2f
                    if (colorMatrix != null || blurMaskFilter != null) {
                        val shapePaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor(layer.fillColor)
                            this.alpha = (alpha * 255).roundToInt().coerceIn(0, 255)
                            isAntiAlias = true
                            if (colorMatrix != null) colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
                            if (blurMaskFilter != null) maskFilter = blurMaskFilter
                        }
                        nativeCanvas.drawCircle(0f, 0f, radius, shapePaint)
                    } else {
                        drawCircle(
                            color = baseFill,
                            radius = radius,
                            center = Offset.Zero
                        )
                    }

                    if (layer.strokeWidth > 0f) {
                        drawCircle(
                            color = baseStroke,
                            radius = radius,
                            center = Offset.Zero,
                            style = Stroke(width = layer.strokeWidth * transform.scaleX)
                        )
                    }
                }
                ShapeType.LINE -> {
                    drawLine(
                        color = baseFill,
                        start = Offset(-width / 2f, 0f),
                        end = Offset(width / 2f, 0f),
                        strokeWidth = (layer.strokeWidth.coerceAtLeast(4f)) * transform.scaleY
                    )
                }
            }
        }
        LayerType.TEXT -> {
            val textColor = try {
                android.graphics.Color.parseColor(layer.textColor)
            } catch (e: Exception) {
                android.graphics.Color.WHITE
            }
            val textPaint = android.graphics.Paint().apply {
                color = textColor
                this.alpha = (alpha * 255).roundToInt().coerceIn(0, 255)
                textSize = layer.fontSize * transform.scaleX
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = layer.isBold
                if (layer.isItalic) textSkewX = -0.25f
                if (colorMatrix != null) colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
                if (blurMaskFilter != null) maskFilter = blurMaskFilter
            }
            nativeCanvas.drawText(
                layer.text,
                0f,
                textPaint.textSize / 3f,
                textPaint
            )
        }
        LayerType.MEDIA -> {
            if (!layer.mediaUri.isNullOrBlank()) {
                try {
                    val uri = Uri.parse(layer.mediaUri)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            val imgBitmap = bitmap.asImageBitmap()
                            drawImage(
                                image = imgBitmap,
                                dstOffset = androidx.compose.ui.unit.IntOffset(
                                    (-width / 2f).roundToInt(),
                                    (-height / 2f).roundToInt()
                                ),
                                dstSize = androidx.compose.ui.unit.IntSize(
                                    width.roundToInt().coerceAtLeast(1),
                                    height.roundToInt().coerceAtLeast(1)
                                ),
                                alpha = alpha
                            )
                        }
                    }
                } catch (e: Exception) {
                    drawRect(
                        color = FlewsBorder.copy(alpha = alpha),
                        topLeft = Offset(-width / 2f, -height / 2f),
                        size = Size(width, height),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }
        LayerType.NULL_OBJECT -> {
            val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            drawRect(
                color = FlewsKeyframeActive.copy(alpha = 0.6f),
                topLeft = Offset(-width / 2f, -height / 2f),
                size = Size(width, height),
                style = Stroke(width = 1.5f, pathEffect = dashPathEffect)
            )
            drawLine(
                color = FlewsKeyframeActive.copy(alpha = 0.8f),
                start = Offset(-15f, 0f),
                end = Offset(15f, 0f),
                strokeWidth = 2f
            )
            drawLine(
                color = FlewsKeyframeActive.copy(alpha = 0.8f),
                start = Offset(0f, -15f),
                end = Offset(0f, 15f),
                strokeWidth = 2f
            )
        }
    }

    // 7. Render Overlay Visual Effects (Vignette, Noise, Glitch)
    EffectRenderer.renderOverlayEffects(
        drawScope = this,
        effects = layer.effects,
        width = width,
        height = height,
        tMs = currentTimeMs
    )

    nativeCanvas.restore()
}

private fun drawShadowPass(
    layer: MotionLayer,
    transform: EvaluatedTransform,
    nativeCanvas: android.graphics.Canvas,
    shadowParams: EffectRenderer.DropShadowParams,
    width: Float,
    height: Float
) {
    val shadowPaint = android.graphics.Paint().apply {
        color = shadowParams.color
        isAntiAlias = true
        maskFilter = android.graphics.BlurMaskFilter(shadowParams.blurRadius, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }

    nativeCanvas.save()
    nativeCanvas.translate(shadowParams.dx, shadowParams.dy)

    when (layer.type) {
        LayerType.TEXT -> {
            shadowPaint.textSize = layer.fontSize * transform.scaleX
            shadowPaint.textAlign = android.graphics.Paint.Align.CENTER
            shadowPaint.isFakeBoldText = layer.isBold
            if (layer.isItalic) shadowPaint.textSkewX = -0.25f
            nativeCanvas.drawText(layer.text, 0f, shadowPaint.textSize / 3f, shadowPaint)
        }
        LayerType.SHAPE -> {
            when (layer.shapeType) {
                ShapeType.CIRCLE -> {
                    val radius = width.coerceAtMost(height) / 2f
                    nativeCanvas.drawCircle(0f, 0f, radius, shadowPaint)
                }
                ShapeType.RECTANGLE -> {
                    nativeCanvas.drawRect(-width / 2f, -height / 2f, width / 2f, height / 2f, shadowPaint)
                }
                ShapeType.LINE -> {
                    shadowPaint.strokeWidth = (layer.strokeWidth.coerceAtLeast(4f)) * transform.scaleY
                    nativeCanvas.drawLine(-width / 2f, 0f, width / 2f, 0f, shadowPaint)
                }
            }
        }
        else -> {
            nativeCanvas.drawRect(-width / 2f, -height / 2f, width / 2f, height / 2f, shadowPaint)
        }
    }

    nativeCanvas.restore()
}

private fun drawGlowPass(
    layer: MotionLayer,
    transform: EvaluatedTransform,
    nativeCanvas: android.graphics.Canvas,
    glowParams: EffectRenderer.GlowParams,
    width: Float,
    height: Float
) {
    val glowPaint = android.graphics.Paint().apply {
        color = glowParams.color
        isAntiAlias = true
        maskFilter = android.graphics.BlurMaskFilter(glowParams.radius, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }

    when (layer.type) {
        LayerType.TEXT -> {
            glowPaint.textSize = layer.fontSize * transform.scaleX
            glowPaint.textAlign = android.graphics.Paint.Align.CENTER
            glowPaint.isFakeBoldText = layer.isBold
            if (layer.isItalic) glowPaint.textSkewX = -0.25f
            nativeCanvas.drawText(layer.text, 0f, glowPaint.textSize / 3f, glowPaint)
        }
        LayerType.SHAPE -> {
            when (layer.shapeType) {
                ShapeType.CIRCLE -> {
                    val radius = width.coerceAtMost(height) / 2f
                    glowPaint.style = android.graphics.Paint.Style.STROKE
                    glowPaint.strokeWidth = glowParams.radius
                    nativeCanvas.drawCircle(0f, 0f, radius, glowPaint)
                }
                ShapeType.RECTANGLE -> {
                    glowPaint.style = android.graphics.Paint.Style.STROKE
                    glowPaint.strokeWidth = glowParams.radius
                    nativeCanvas.drawRect(-width / 2f, -height / 2f, width / 2f, height / 2f, glowPaint)
                }
                ShapeType.LINE -> {
                    glowPaint.strokeWidth = (layer.strokeWidth.coerceAtLeast(4f) + glowParams.radius) * transform.scaleY
                    nativeCanvas.drawLine(-width / 2f, 0f, width / 2f, 0f, glowPaint)
                }
            }
        }
        else -> {
            glowPaint.style = android.graphics.Paint.Style.STROKE
            glowPaint.strokeWidth = glowParams.radius
            nativeCanvas.drawRect(-width / 2f, -height / 2f, width / 2f, height / 2f, glowPaint)
        }
    }
}

private fun DrawScope.drawTransformHandles(
    layer: MotionLayer,
    transform: EvaluatedTransform,
    canvasCenter: Offset
) {
    val layerCenter = canvasCenter + Offset(transform.posX, transform.posY)
    val calcWidth = if (layer.type == LayerType.TEXT) {
        (layer.text.length * layer.fontSize * 0.55f).coerceAtLeast(100f)
    } else layer.width

    val calcHeight = if (layer.type == LayerType.TEXT) {
        (layer.fontSize * 1.3f).coerceAtLeast(50f)
    } else layer.height

    val width = calcWidth * transform.scaleX
    val height = calcHeight * transform.scaleY

    translate(layerCenter.x, layerCenter.y) {
        rotate(transform.rotationDeg) {
            val halfW = width / 2f
            val halfH = height / 2f

            // Bounding box border
            drawRect(
                color = FlewsPrimaryLight,
                topLeft = Offset(-halfW, -halfH),
                size = Size(width, height),
                style = Stroke(width = 1.8f)
            )

            // 4 Corner scaling handles (Prominent & easy to drag for images, shapes, and text)
            val handleSize = 14f
            val corners = listOf(
                Offset(-halfW, -halfH),
                Offset(halfW, -halfH),
                Offset(halfW, halfH),
                Offset(-halfW, halfH)
            )
            corners.forEach { corner ->
                drawRect(
                    color = Color.White,
                    topLeft = corner - Offset(handleSize / 2f, handleSize / 2f),
                    size = Size(handleSize, handleSize)
                )
                drawRect(
                    color = FlewsPrimary,
                    topLeft = corner - Offset(handleSize / 2f, handleSize / 2f),
                    size = Size(handleSize, handleSize),
                    style = Stroke(width = 2f)
                )
            }

            // Top Rotation handle connector line & circle
            val rotHandleCenter = Offset(0f, -halfH - 24f)
            drawLine(
                color = FlewsPrimaryLight,
                start = Offset(0f, -halfH),
                end = rotHandleCenter,
                strokeWidth = 1.8f
            )
            drawCircle(
                color = Color.White,
                radius = 8f,
                center = rotHandleCenter
            )
            drawCircle(
                color = FlewsPrimary,
                radius = 8f,
                center = rotHandleCenter,
                style = Stroke(width = 2f)
            )
        }
    }
}
