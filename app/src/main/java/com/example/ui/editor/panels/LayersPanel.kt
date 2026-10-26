package com.example.ui.editor.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MotionBlendMode
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
import kotlin.math.roundToInt

@Composable
fun LayersPanel(
    layers: List<MotionLayer>,
    selectedLayerId: String?,
    onSelectLayer: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onDuplicateLayer: (String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onMoveLayerUp: (Int) -> Unit,
    onMoveLayerDown: (Int) -> Unit,
    onUpdateOpacity: (layerId: String, opacity: Float) -> Unit,
    onUpdateBlendMode: (layerId: String, blendMode: MotionBlendMode) -> Unit,
    onUpdateParent: (layerId: String, parentId: String?) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedLayer = layers.find { it.id == selectedLayerId }
    var showBlendMenu by remember { mutableStateOf(false) }
    var showParentMenu by remember { mutableStateOf(false) }

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
            Text(
                text = "PANEL LAYER (${layers.size})",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = FlewsTextPrimary,
                    letterSpacing = 0.5.sp
                )
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("close_layers_panel_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Tutup",
                    tint = FlewsTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected layer properties bar (Opacity, Blend, Parent)
        if (selectedLayer != null) {
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
                        text = "Layer: ${selectedLayer.name}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = FlewsPrimaryLight
                        )
                    )
                    Text(
                        text = "Opasitas ${(selectedLayer.opacity * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(color = FlewsTextSecondary)
                    )
                }

                Slider(
                    value = selectedLayer.opacity,
                    onValueChange = { onUpdateOpacity(selectedLayer.id, it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = FlewsPrimary,
                        activeTrackColor = FlewsPrimary,
                        inactiveTrackColor = FlewsBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Blend Mode Button
                    Box(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(FlewsSurface)
                                .border(1.dp, FlewsBorder, RoundedCornerShape(6.dp))
                                .clickable { showBlendMenu = true }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Blend: ${selectedLayer.blendMode.name}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FlewsTextPrimary,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        DropdownMenu(
                            expanded = showBlendMenu,
                            onDismissRequest = { showBlendMenu = false },
                            modifier = Modifier.background(FlewsSurface)
                        ) {
                            MotionBlendMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.name, color = FlewsTextPrimary) },
                                    onClick = {
                                        onUpdateBlendMode(selectedLayer.id, mode)
                                        showBlendMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Parent to Null Object / Another Layer Button
                    val parentLayer = layers.find { it.id == selectedLayer.parentId }
                    Box(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(FlewsSurface)
                                .border(1.dp, FlewsBorder, RoundedCornerShape(6.dp))
                                .clickable { showParentMenu = true }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Parent: ${parentLayer?.name ?: "Tidak Ada"}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = FlewsTextPrimary,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        DropdownMenu(
                            expanded = showParentMenu,
                            onDismissRequest = { showParentMenu = false },
                            modifier = Modifier.background(FlewsSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tanpa Parent", color = FlewsTextSecondary) },
                                onClick = {
                                    onUpdateParent(selectedLayer.id, null)
                                    showParentMenu = false
                                }
                            )
                            layers.filter { it.id != selectedLayer.id }.forEach { potentialParent ->
                                DropdownMenuItem(
                                    text = { Text(potentialParent.name, color = FlewsTextPrimary) },
                                    onClick = {
                                        onUpdateParent(selectedLayer.id, potentialParent.id)
                                        showParentMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Layer List (Empty state or full list)
        if (layers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(FlewsBackground, RoundedCornerShape(8.dp))
                    .border(1.dp, FlewsBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Daftar layer masih kosong.\nTambahkan bentuk, teks, atau media dari toolbar.",
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
                    .height(180.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(layers, key = { _, l -> l.id }) { index, layer ->
                    val isSelected = layer.id == selectedLayerId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) FlewsSurfaceHigh else FlewsBackground)
                            .border(
                                1.dp,
                                if (isSelected) FlewsPrimaryLight else FlewsBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectLayer(layer.id) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Move Up / Down
                        IconButton(
                            onClick = { onMoveLayerUp(index) },
                            enabled = index > 0,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowUpward,
                                contentDescription = "Pindah Ke Atas",
                                tint = if (index > 0) FlewsTextSecondary else FlewsBorder,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { onMoveLayerDown(index) },
                            enabled = index < layers.size - 1,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowDownward,
                                contentDescription = "Pindah Ke Bawah",
                                tint = if (index < layers.size - 1) FlewsTextSecondary else FlewsBorder,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = layer.name,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = FlewsTextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Visibility Toggle
                        IconButton(
                            onClick = { onToggleVisibility(layer.id) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = if (layer.isVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = "Visibilitas",
                                tint = if (layer.isVisible) FlewsPrimaryLight else FlewsTextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Lock Toggle
                        IconButton(
                            onClick = { onToggleLock(layer.id) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = if (layer.isLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                                contentDescription = "Kunci",
                                tint = if (layer.isLocked) FlewsPrimaryLight else FlewsTextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Duplicate
                        IconButton(
                            onClick = { onDuplicateLayer(layer.id) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Duplikat",
                                tint = FlewsTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Delete
                        IconButton(
                            onClick = { onDeleteLayer(layer.id) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Hapus",
                                tint = FlewsAccentRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
