package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.FirstPage
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MotionLayer
import com.example.ui.theme.FlewsBorder
import com.example.ui.theme.FlewsKeyframeActive
import com.example.ui.theme.FlewsPlayhead
import com.example.ui.theme.FlewsPrimary
import com.example.ui.theme.FlewsSurface
import com.example.ui.theme.FlewsSurfaceHigh
import com.example.ui.theme.FlewsTextPrimary
import com.example.ui.theme.FlewsTextSecondary
import java.util.Locale

@Composable
fun PlaybackControls(
    currentTimeMs: Long,
    totalDurationMs: Long,
    fps: Long,
    isPlaying: Boolean,
    isLooping: Boolean,
    selectedLayer: MotionLayer?,
    totalLayersCount: Int = 1,
    onTogglePlay: () -> Unit,
    onStepPrevFrame: () -> Unit,
    onStepNextFrame: () -> Unit,
    onSeekToStart: () -> Unit,
    onToggleLoop: () -> Unit,
    onToggleKeyframeAtCurrentTime: () -> Unit,
    modifier: Modifier = Modifier
) {
    val frameDurationMs = (1000f / fps).coerceAtLeast(1f)
    val clampedCurrentMs = currentTimeMs.coerceIn(0L, totalDurationMs)

    val currentMinutes = clampedCurrentMs / 60000
    val currentSeconds = (clampedCurrentMs % 60000) / 1000
    val currentFrames = ((clampedCurrentMs % 1000) * fps / 1000).coerceAtMost(fps - 1)

    val totalMinutes = totalDurationMs / 60000
    val totalSeconds = (totalDurationMs % 60000) / 1000
    val totalFrames = ((totalDurationMs % 1000) * fps / 1000).coerceAtMost(fps - 1)

    val timeFormatted = String.format(
        Locale.US,
        "%02d:%02d:%02d / %02d:%02d:%02d",
        currentMinutes,
        currentSeconds,
        currentFrames,
        totalMinutes,
        totalSeconds,
        totalFrames
    )

    // Check if selected layer has keyframe at current playhead
    val hasKeyframeAtCurrent = selectedLayer?.keyframes?.any {
        kotlin.math.abs(it.timeMs - clampedCurrentMs) < frameDurationMs
    } == true

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(FlewsSurface)
            .border(1.dp, FlewsBorder)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("transport"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Timecode
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = FlewsTextPrimary,
                    fontSize = 11.sp
                ),
                modifier = Modifier.testTag("timecode_display")
            )
        }

        // Center: Transport Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onSeekToStart,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("seek_to_start_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.FirstPage,
                    contentDescription = "Ke Awal",
                    tint = FlewsTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onStepPrevFrame,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("step_prev_frame_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.SkipPrevious,
                    contentDescription = "Frame Sebelumnya",
                    tint = FlewsTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Play / Pause toggle (disabled when 0 layers)
            val canPlay = totalLayersCount > 0
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (canPlay) FlewsPrimary else FlewsSurfaceHigh)
                    .clickable(enabled = canPlay, onClick = onTogglePlay)
                    .testTag("play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying && canPlay) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isPlaying) "Jeda" else "Putar",
                    tint = if (canPlay) Color.White else FlewsTextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = onStepNextFrame,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("step_next_frame_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.SkipNext,
                    contentDescription = "Frame Berikutnya",
                    tint = FlewsTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Loop Toggle
            IconButton(
                onClick = onToggleLoop,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("toggle_loop_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Loop,
                    contentDescription = "Ulangi Putaran",
                    tint = if (isLooping) FlewsPrimary else FlewsTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Right: Diamond Keyframe Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (hasKeyframeAtCurrent) FlewsKeyframeActive.copy(alpha = 0.25f)
                    else FlewsSurfaceHigh
                )
                .border(
                    width = 1.dp,
                    color = if (hasKeyframeAtCurrent) FlewsKeyframeActive else FlewsBorder,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(
                    enabled = selectedLayer != null,
                    onClick = onToggleKeyframeAtCurrentTime
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .testTag("toggle_keyframe_button"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Diamond,
                    contentDescription = "Keyframe",
                    tint = if (hasKeyframeAtCurrent) FlewsKeyframeActive
                    else if (selectedLayer != null) FlewsTextPrimary
                    else FlewsTextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (hasKeyframeAtCurrent) "Hapus KF" else "+ KF",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (hasKeyframeAtCurrent) FlewsKeyframeActive
                        else if (selectedLayer != null) FlewsTextPrimary
                        else FlewsTextSecondary.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
