package com.example.ui.editor.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.ToggleOn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.EffectRegistry
import com.example.data.model.EffectType
import com.example.data.model.MotionEffect
import com.example.data.model.MotionLayer
import com.example.ui.theme.FlewsAccentRed
import com.example.ui.theme.FlewsBackground
import com.example.ui.theme.FlewsBorder
import com.example.ui.theme.FlewsPrimary
import com.example.ui.theme.FlewsPrimaryLight
import com.example.ui.theme.FlewsSurface
import com.example.ui.theme.FlewsSurfaceHigh
import com.example.ui.theme.FlewsTextPrimary
import com.example.ui.theme.FlewsTextSecondary
import java.util.Locale

@Composable
fun EffectsPanel(
    selectedLayer: MotionLayer?,
    onAddEffect: (EffectType) -> Unit,
    onUpdateEffectValue: (effectId: String, newValue: Float) -> Unit,
    onUpdateEffectColor: (effectId: String, newColorHex: String) -> Unit = { _, _ -> },
    onToggleEffect: (effectId: String) -> Unit,
    onRemoveEffect: (effectId: String) -> Unit,
    onReorderEffect: (fromIndex: Int, toIndex: Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = FlewsPrimaryLight,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "EFEK VISUAL MOTION",
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
                    .testTag("close_effects_panel_button")
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
                    .height(90.dp)
                    .background(FlewsBackground, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Pilih layer terlebih dahulu untuk menerapkan efek",
                    style = MaterialTheme.typography.bodySmall.copy(color = FlewsTextSecondary)
                )
            }
        } else {
            // Section 1: Effect Catalog (horizontal scroll)
            Text(
                text = "Katalog Efek (Tambah Efek)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = FlewsTextSecondary
                )
            )

            // Category filter chips
            var selectedCategory by remember { mutableStateOf("Semua") }
            val categories = listOf("Semua", EffectRegistry.CATEGORY_VISUAL, EffectRegistry.CATEGORY_TRANSFORM, EffectRegistry.CATEGORY_AYUNAN)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) FlewsPrimary else FlewsSurfaceHigh)
                            .border(1.dp, if (isSelected) FlewsPrimaryLight else FlewsBorder, RoundedCornerShape(16.dp))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else FlewsTextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val catalogList = if (selectedCategory == "Semua") {
                    EffectRegistry.getAllDescriptors()
                } else {
                    EffectRegistry.getDescriptorsByCategory(selectedCategory)
                }

                catalogList.forEach { desc ->
                    val fxType = desc.type
                    val isAlreadyAdded = selectedLayer.effects.any { it.type == fxType }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isAlreadyAdded) FlewsSurfaceHigh else FlewsPrimary)
                            .border(
                                1.dp,
                                if (isAlreadyAdded) FlewsBorder else FlewsPrimaryLight,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                if (!isAlreadyAdded) onAddEffect(fxType)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("add_effect_${fxType.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isAlreadyAdded) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = desc.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isAlreadyAdded) FontWeight.Normal else FontWeight.Bold,
                                    color = if (isAlreadyAdded) FlewsTextSecondary else Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section 2: Active Effects List on Selected Layer
            Text(
                text = "Efek Aktif (${selectedLayer.effects.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = FlewsTextSecondary
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (selectedLayer.effects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(FlewsBackground, RoundedCornerShape(8.dp))
                        .border(1.dp, FlewsBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada efek aktif pada layer ini.\nPilih efek dari katalog di atas.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = FlewsTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(selectedLayer.effects, key = { _, fx -> fx.id }) { index, fx ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(FlewsSurfaceHigh)
                                .border(1.dp, FlewsBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = fx.type.displayName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (fx.isEnabled) FlewsTextPrimary else FlewsTextSecondary
                                    )
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f", fx.value),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = FlewsPrimaryLight,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    // Move Up
                                    IconButton(
                                        onClick = { onReorderEffect(index, index - 1) },
                                        enabled = index > 0,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .testTag("move_up_effect_${fx.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ArrowUpward,
                                            contentDescription = "Naikkan Efek",
                                            tint = if (index > 0) FlewsTextPrimary else FlewsBorder,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    // Move Down
                                    IconButton(
                                        onClick = { onReorderEffect(index, index + 1) },
                                        enabled = index < selectedLayer.effects.size - 1,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .testTag("move_down_effect_${fx.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ArrowDownward,
                                            contentDescription = "Turunkan Efek",
                                            tint = if (index < selectedLayer.effects.size - 1) FlewsTextPrimary else FlewsBorder,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    // Toggle
                                    IconButton(
                                        onClick = { onToggleEffect(fx.id) },
                                        modifier = Modifier
                                            .size(24.dp)
                                            .testTag("toggle_effect_${fx.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (fx.isEnabled) Icons.Outlined.ToggleOn else Icons.Outlined.ToggleOff,
                                            contentDescription = "Aktifkan Efek",
                                            tint = if (fx.isEnabled) FlewsPrimaryLight else FlewsTextSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    // Delete
                                    IconButton(
                                        onClick = { onRemoveEffect(fx.id) },
                                        modifier = Modifier
                                            .size(24.dp)
                                            .testTag("remove_effect_${fx.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Hapus Efek",
                                            tint = FlewsAccentRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (fx.isEnabled) {
                                Slider(
                                    value = fx.value.coerceIn(fx.type.minVal, fx.type.maxVal),
                                    onValueChange = { onUpdateEffectValue(fx.id, it) },
                                    valueRange = fx.type.minVal..fx.type.maxVal,
                                    colors = SliderDefaults.colors(
                                        thumbColor = FlewsPrimary,
                                        activeTrackColor = FlewsPrimary,
                                        inactiveTrackColor = FlewsBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("slider_effect_${fx.id}")
                                )

                                val isColorizable = fx.type in listOf(EffectType.GLOW, EffectType.SHADOW, EffectType.TINT, EffectType.DUOTONE)
                                if (isColorizable) {
                                    val palette = listOf("#00E5FF", "#FF0055", "#FFB800", "#00FF66", "#9D00FF", "#FFFFFF", "#000000")
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Warna:",
                                            style = MaterialTheme.typography.labelSmall.copy(color = FlewsTextSecondary, fontSize = 10.sp)
                                        )
                                        palette.forEach { hex ->
                                            val isSelected = fx.colorHex.equals(hex, ignoreCase = true)
                                            val swColor = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Cyan }
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(swColor)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) FlewsPrimaryLight else FlewsBorder,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { onUpdateEffectColor(fx.id, hex) }
                                                    .testTag("effect_color_${fx.id}_$hex")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
