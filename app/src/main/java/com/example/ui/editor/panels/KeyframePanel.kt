package com.example.ui.editor.panels

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.MotionEngine
import com.example.data.model.EasingType
import com.example.data.model.KeyframeProperty
import com.example.data.model.MotionKeyframe
import com.example.data.model.MotionLayer
import com.example.ui.theme.FlewsBackground
import com.example.ui.theme.FlewsBorder
import com.example.ui.theme.FlewsKeyframeActive
import com.example.ui.theme.FlewsPrimary
import com.example.ui.theme.FlewsPrimaryLight
import com.example.ui.theme.FlewsSurface
import com.example.ui.theme.FlewsSurfaceHigh
import com.example.ui.theme.FlewsTextPrimary
import com.example.ui.theme.FlewsTextSecondary
import java.util.Locale

@Composable
fun KeyframePanel(
    selectedLayer: MotionLayer?,
    currentTimeMs: Long,
    onAddOrUpdateKeyframe: (property: KeyframeProperty, value: Float, easing: EasingType) -> Unit,
    onRemoveKeyframe: (keyframeId: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProperty by remember { mutableStateOf(KeyframeProperty.POSITION_X) }
    var selectedEasing by remember { mutableStateOf(EasingType.EASE_IN_OUT) }

    val properties = listOf(
        KeyframeProperty.POSITION_X to "Posisi X",
        KeyframeProperty.POSITION_Y to "Posisi Y",
        KeyframeProperty.SCALE_X to "Skala X",
        KeyframeProperty.SCALE_Y to "Skala Y",
        KeyframeProperty.ROTATION to "Rotasi",
        KeyframeProperty.OPACITY to "Opasitas"
    )

    // Current property base / interpolated value
    val matchingKeyframe = selectedLayer?.keyframes?.find {
        it.property == selectedProperty && kotlin.math.abs(it.timeMs - currentTimeMs) < 50
    }

    var sliderValue by remember(selectedProperty, selectedLayer, matchingKeyframe) {
        val v = matchingKeyframe?.value ?: when (selectedProperty) {
            KeyframeProperty.POSITION_X -> selectedLayer?.positionX ?: 0f
            KeyframeProperty.POSITION_Y -> selectedLayer?.positionY ?: 0f
            KeyframeProperty.SCALE_X -> selectedLayer?.scaleX ?: 1f
            KeyframeProperty.SCALE_Y -> selectedLayer?.scaleY ?: 1f
            KeyframeProperty.ROTATION -> selectedLayer?.rotationDeg ?: 0f
            KeyframeProperty.OPACITY -> selectedLayer?.opacity ?: 1f
            KeyframeProperty.EFFECT_VALUE -> 0f
        }
        mutableStateOf(v)
    }

    val valueRange = when (selectedProperty) {
        KeyframeProperty.POSITION_X, KeyframeProperty.POSITION_Y -> -600f..600f
        KeyframeProperty.SCALE_X, KeyframeProperty.SCALE_Y -> 0.1f..5f
        KeyframeProperty.ROTATION -> -360f..360f
        KeyframeProperty.OPACITY -> 0f..1f
        KeyframeProperty.EFFECT_VALUE -> 0f..100f
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FlewsSurface)
            .border(1.dp, FlewsBorder)
            .padding(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Diamond,
                    contentDescription = null,
                    tint = FlewsKeyframeActive,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "EDITOR KEYFRAME & KURVA EASING",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = FlewsTextPrimary,
                        letterSpacing = 0.5.sp
                    )
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("close_keyframe_panel_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Tutup",
                    tint = FlewsTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedLayer == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(FlewsBackground, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Pilih layer terlebih dahulu untuk mengatur keyframe",
                    style = MaterialTheme.typography.bodySmall.copy(color = FlewsTextSecondary)
                )
            }
        } else {
            // Property Tab Selector (Horizontal scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                properties.forEach { (prop, label) ->
                    val isSelected = prop == selectedProperty
                    val hasKf = selectedLayer.keyframes.any { it.property == prop }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) FlewsPrimary else FlewsSurfaceHigh)
                            .border(
                                1.dp,
                                if (isSelected) FlewsPrimaryLight else FlewsBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedProperty = prop }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hasKf) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(FlewsKeyframeActive, androidx.compose.foundation.shape.CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else FlewsTextSecondary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Value Slider & Direct Edit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nilai Properti",
                    style = MaterialTheme.typography.labelMedium.copy(color = FlewsTextSecondary)
                )
                Text(
                    text = String.format(Locale.US, "%.2f", sliderValue),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FlewsPrimaryLight
                    )
                )
            }

            Slider(
                value = sliderValue.coerceIn(valueRange.start, valueRange.endInclusive),
                onValueChange = {
                    sliderValue = it
                    // Update live
                    onAddOrUpdateKeyframe(selectedProperty, it, selectedEasing)
                },
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = FlewsPrimary,
                    activeTrackColor = FlewsPrimary,
                    inactiveTrackColor = FlewsBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Easing Selector (Linear, Ease In, Ease Out, Ease In-Out)
            Text(
                text = "Kurva Easing",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = FlewsTextSecondary
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EasingType.values().forEach { easing ->
                    val isSelected = easing == selectedEasing
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) FlewsPrimary else FlewsSurfaceHigh)
                            .border(
                                1.dp,
                                if (isSelected) FlewsPrimaryLight else FlewsBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedEasing = easing
                                if (matchingKeyframe != null) {
                                    onAddOrUpdateKeyframe(selectedProperty, sliderValue, easing)
                                }
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (easing) {
                                EasingType.LINEAR -> "Linear"
                                EasingType.EASE_IN -> "Ease In"
                                EasingType.EASE_OUT -> "Ease Out"
                                EasingType.EASE_IN_OUT -> "Ease I-O"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else FlewsTextSecondary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Visual Curve Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(FlewsBackground, RoundedCornerShape(8.dp))
                    .border(1.dp, FlewsBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path()
                    val steps = 40
                    for (i in 0..steps) {
                        val t = i.toFloat() / steps
                        val easedT = MotionEngine.applyEasing(t, selectedEasing)
                        val x = t * size.width
                        val y = size.height - (easedT * size.height)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = FlewsPrimaryLight,
                        style = Stroke(width = 3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Keyframe Action Buttons (+ Keyframe at time, - Keyframe)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onAddOrUpdateKeyframe(selectedProperty, sliderValue, selectedEasing)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FlewsPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Diamond,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (matchingKeyframe != null) "Update Keyframe" else "+ Simpan Keyframe",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (matchingKeyframe != null) {
                    Button(
                        onClick = { onRemoveKeyframe(matchingKeyframe.id) },
                        modifier = Modifier.height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FlewsSurfaceHigh,
                            contentColor = FlewsBorder
                        )
                    ) {
                        Text(
                            text = "Hapus KF",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White)
                        )
                    }
                }
            }
        }
    }
}
