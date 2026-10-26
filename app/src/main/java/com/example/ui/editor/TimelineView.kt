package com.example.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LayerType
import com.example.data.model.MotionLayer
import com.example.ui.theme.FlewsBackground
import com.example.ui.theme.FlewsBorder
import com.example.ui.theme.FlewsKeyframeActive
import com.example.ui.theme.FlewsPlayhead
import com.example.ui.theme.FlewsPrimary
import com.example.ui.theme.FlewsPrimaryLight
import com.example.ui.theme.FlewsSurface
import com.example.ui.theme.FlewsSurfaceHigh
import com.example.ui.theme.FlewsTextPrimary
import com.example.ui.theme.FlewsTextSecondary
import com.example.ui.theme.FlewsTimelineRuler
import kotlin.math.roundToInt

@Composable
fun TimelineView(
    layers: List<MotionLayer>,
    selectedLayerId: String?,
    currentTimeMs: Long,
    totalDurationMs: Long,
    fps: Long,
    onSeekTime: (Long) -> Unit,
    onSelectLayer: (String) -> Unit,
    onUpdateLayerDuration: (layerId: String, newInTime: Long, newOutTime: Long) -> Unit,
    onAddClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Zoom scale: pixels per second (default 120 dp per second)
    var pixelsPerSecond by remember { mutableFloatStateOf(100f) }
    val totalSeconds = totalDurationMs / 1000f
    val timelineWidthDp = (totalSeconds * pixelsPerSecond).coerceAtLeast(360f).dp

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FlewsBackground)
    ) {
        // Timeline Header with Zoom buttons & Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FlewsSurface)
                .border(1.dp, FlewsBorder)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Timeline",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = FlewsTextPrimary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(FlewsSurfaceHigh)
                        .border(1.dp, FlewsBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${layers.size} layer",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FlewsTextSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { pixelsPerSecond = (pixelsPerSecond - 25f).coerceAtLeast(40f) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ZoomOut,
                        contentDescription = "Perkecil Timeline",
                        tint = FlewsTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { pixelsPerSecond = (pixelsPerSecond + 25f).coerceAtMost(300f) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ZoomIn,
                        contentDescription = "Perbesar Timeline",
                        tint = FlewsTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Horizontal scrolling ruler & layer tracks area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .width(timelineWidthDp + 80.dp)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Time Ruler
                    TimelineRuler(
                        totalDurationMs = totalDurationMs,
                        pixelsPerSecond = pixelsPerSecond,
                        fps = fps,
                        onSeekTime = onSeekTime,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .testTag("timeline-ruler")
                    )

                    // 2. Multi-track layer list
                    if (layers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(FlewsBackground)
                                .testTag("empty-state"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Belum ada layer",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = FlewsTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ketuk + untuk membuat layer baru",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = FlewsTextSecondary
                                    )
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("layer-column")
                        ) {
                            items(layers, key = { it.id }) { layer ->
                                LayerTrackItem(
                                    layer = layer,
                                    isSelected = layer.id == selectedLayerId,
                                    totalDurationMs = totalDurationMs,
                                    pixelsPerSecond = pixelsPerSecond,
                                    onSelectLayer = { onSelectLayer(layer.id) },
                                    onSeekTime = onSeekTime,
                                    onUpdateDuration = { newIn, newOut ->
                                        onUpdateLayerDuration(layer.id, newIn, newOut)
                                    }
                                )
                            }
                        }
                    }
                }

                // 3. Playhead Line (Draggable vertical line across full height)
                val playheadOffsetDp = ((currentTimeMs / 1000f) * pixelsPerSecond).dp
                Box(
                    modifier = Modifier
                        .offset { IntOffset(x = playheadOffsetDp.roundToPx() - 10, y = 0) }
                        .fillMaxHeight()
                        .width(20.dp)
                        .pointerInput(totalDurationMs, pixelsPerSecond) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaSeconds = dragAmount.x / pixelsPerSecond
                                val newTime = (currentTimeMs + (deltaSeconds * 1000).toLong())
                                    .coerceIn(0L, totalDurationMs)
                                onSeekTime(newTime)
                            }
                        }
                        .testTag("playhead_touch_zone"),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Center line
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(2.dp)
                            .background(FlewsPlayhead)
                    )
                    // Top diamond handle
                    Canvas(
                        modifier = Modifier
                            .size(18.dp)
                            .testTag("playhead_handle")
                    ) {
                        val path = Path().apply {
                            moveTo(size.width / 2f, size.height)
                            lineTo(size.width, size.height / 2f)
                            lineTo(size.width / 2f, 0f)
                            lineTo(0f, size.height / 2f)
                            close()
                        }
                        drawPath(path, color = FlewsPlayhead)
                    }
                }

                // 4. Floating Action Button (+) in bottom-left corner
                if (onAddClick != null) {
                    androidx.compose.material3.FloatingActionButton(
                        onClick = onAddClick,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .size(48.dp)
                            .testTag("add-fab"),
                        shape = CircleShape,
                        containerColor = FlewsPrimary,
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Tambah Layer",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineRuler(
    totalDurationMs: Long,
    pixelsPerSecond: Float,
    fps: Long,
    onSeekTime: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .background(FlewsTimelineRuler)
            .border(1.dp, FlewsBorder)
            .pointerInput(totalDurationMs, pixelsPerSecond) {
                detectTapGestures { tapOffset ->
                    val tappedSeconds = tapOffset.x / pixelsPerSecond
                    val tappedTimeMs = (tappedSeconds * 1000f).toLong().coerceIn(0L, totalDurationMs)
                    onSeekTime(tappedTimeMs)
                }
            }
            .pointerInput(totalDurationMs, pixelsPerSecond) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val deltaSeconds = dragAmount.x / pixelsPerSecond
                    val newTime = (totalDurationMs).let {
                        val curr = (change.position.x / pixelsPerSecond * 1000f).toLong()
                        curr.coerceIn(0L, totalDurationMs)
                    }
                    onSeekTime(newTime)
                }
            }
    ) {
        val totalSec = (totalDurationMs / 1000f).toInt()
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#8A96BF")
            textSize = 24f
            isAntiAlias = true
        }

        for (sec in 0..totalSec) {
            val startX = sec * pixelsPerSecond
            // Major tick mark
            drawLine(
                color = FlewsBorder,
                start = Offset(startX, 0f),
                end = Offset(startX, size.height),
                strokeWidth = 1.5f
            )

            // Second label
            drawContext.canvas.nativeCanvas.drawText(
                "${sec}s",
                startX + 6f,
                size.height - 8f,
                textPaint
            )

            // Sub-ticks
            for (sub in 1..3) {
                val subX = startX + (sub * (pixelsPerSecond / 4f))
                drawLine(
                    color = FlewsBorder.copy(alpha = 0.5f),
                    start = Offset(subX, size.height * 0.6f),
                    end = Offset(subX, size.height),
                    strokeWidth = 1f
                )
            }
        }
    }
}

@Composable
private fun LayerTrackItem(
    layer: MotionLayer,
    isSelected: Boolean,
    totalDurationMs: Long,
    pixelsPerSecond: Float,
    onSelectLayer: () -> Unit,
    onSeekTime: (Long) -> Unit,
    onUpdateDuration: (newInTime: Long, newOutTime: Long) -> Unit
) {
    val inSeconds = layer.inTimeMs / 1000f
    val outSeconds = layer.outTimeMs / 1000f
    val trackStartOffsetDp = (inSeconds * pixelsPerSecond).dp
    val trackWidthDp = ((outSeconds - inSeconds) * pixelsPerSecond).coerceAtLeast(40f).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(if (isSelected) FlewsSurfaceHigh else FlewsBackground)
            .border(0.5.dp, FlewsBorder)
            .clickable(onClick = onSelectLayer)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = trackStartOffsetDp)
        ) {
            // Track bar container
            Box(
                modifier = Modifier
                    .width(trackWidthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) FlewsPrimary.copy(alpha = 0.4f)
                        else FlewsSurface.copy(alpha = 0.9f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) FlewsPrimaryLight else FlewsBorder,
                        shape = RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                // Main Layer info in center
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (layer.type) {
                        LayerType.SHAPE -> Icons.Outlined.Diamond
                        LayerType.TEXT -> Icons.Outlined.TextFields
                        LayerType.MEDIA -> Icons.Outlined.Image
                        LayerType.NULL_OBJECT -> Icons.Outlined.Videocam
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) FlewsPrimaryLight else FlewsTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = layer.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = FlewsTextPrimary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Quick reset inTime button ("Hapus Durasi Awal") if inTimeMs > 0
                    if (isSelected && layer.inTimeMs > 0L) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(FlewsPrimary)
                                .clickable { onUpdateDuration(0L, layer.outTimeMs) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("reset_in_time_${layer.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Durasi Awal = 0s",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // Render keyframe diamonds along the track!
                layer.keyframes.forEach { kf ->
                    val kfTimeRelSeconds = (kf.timeMs - layer.inTimeMs) / 1000f
                    val kfOffsetDp = (kfTimeRelSeconds * pixelsPerSecond).dp
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x = kfOffsetDp.roundToPx() - 8, y = 0) }
                            .size(14.dp)
                            .align(Alignment.CenterStart)
                            .clip(CircleShape)
                            .clickable { onSeekTime(kf.timeMs) }
                            .testTag("track_keyframe_${kf.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Diamond,
                            contentDescription = "Keyframe",
                            tint = FlewsKeyframeActive,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Left Drag Handle (Trim In Time)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(12.dp)
                        .fillMaxHeight()
                        .background(if (isSelected) FlewsPrimaryLight else FlewsBorder)
                        .pointerInput(layer.id, pixelsPerSecond, totalDurationMs) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaSec = dragAmount.x / pixelsPerSecond
                                val deltaMs = (deltaSec * 1000f).toLong()
                                val newIn = (layer.inTimeMs + deltaMs).coerceIn(0L, layer.outTimeMs - 100L)
                                onUpdateDuration(newIn, layer.outTimeMs)
                            }
                        }
                        .testTag("track_left_handle_${layer.id}")
                )

                // Right Drag Handle (Trim Out Time)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(12.dp)
                        .fillMaxHeight()
                        .background(if (isSelected) FlewsPrimaryLight else FlewsBorder)
                        .pointerInput(layer.id, pixelsPerSecond, totalDurationMs) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaSec = dragAmount.x / pixelsPerSecond
                                val deltaMs = (deltaSec * 1000f).toLong()
                                val newOut = (layer.outTimeMs + deltaMs).coerceIn(layer.inTimeMs + 100L, totalDurationMs)
                                onUpdateDuration(layer.inTimeMs, newOut)
                            }
                        }
                        .testTag("track_right_handle_${layer.id}")
                )
            }
        }
    }
}
