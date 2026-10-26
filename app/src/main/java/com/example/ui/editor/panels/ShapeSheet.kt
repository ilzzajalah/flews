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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.data.model.ShapeType
import com.example.ui.theme.FlewsBorder
import com.example.ui.theme.FlewsPrimary
import com.example.ui.theme.FlewsPrimaryLight
import com.example.ui.theme.FlewsSurface
import com.example.ui.theme.FlewsSurfaceHigh
import com.example.ui.theme.FlewsTextPrimary
import com.example.ui.theme.FlewsTextSecondary
import kotlin.math.roundToInt

val SHAPE_COLORS = listOf(
    "#2F6BFF" to "Biru Flews",
    "#5C8DFF" to "Biru Muda",
    "#F2F5FF" to "Putih Salju",
    "#FF4D4D" to "Merah Coral",
    "#00E676" to "Hijau Neon",
    "#FFD600" to "Kuning Terang"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShapeSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onAddShape: (type: ShapeType, width: Float, height: Float, fill: String, stroke: String, strokeW: Float) -> Unit
) {
    var selectedShape by remember { mutableStateOf(ShapeType.RECTANGLE) }
    var shapeSize by remember { mutableFloatStateOf(240f) }
    var selectedColor by remember { mutableStateOf("#2F6BFF") }
    var strokeWidth by remember { mutableFloatStateOf(0f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FlewsSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tambah Bentuk Vektor",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FlewsTextPrimary
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "Tutup", tint = FlewsTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Shape Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ShapeType.RECTANGLE to "Persegi",
                    ShapeType.CIRCLE to "Lingkaran",
                    ShapeType.LINE to "Garis"
                ).forEach { (type, label) ->
                    val isSelected = type == selectedShape
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) FlewsPrimary else FlewsSurfaceHigh)
                            .border(1.dp, if (isSelected) FlewsPrimaryLight else FlewsBorder, RoundedCornerShape(10.dp))
                            .clickable { selectedShape = type }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else FlewsTextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Size Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Ukuran Bentuk", style = MaterialTheme.typography.labelMedium.copy(color = FlewsTextSecondary))
                Text("${shapeSize.roundToInt()} px", style = MaterialTheme.typography.labelMedium.copy(color = FlewsPrimaryLight, fontWeight = FontWeight.Bold))
            }
            Slider(
                value = shapeSize,
                onValueChange = { shapeSize = it },
                valueRange = 40f..600f,
                colors = SliderDefaults.colors(thumbColor = FlewsPrimary, activeTrackColor = FlewsPrimary, inactiveTrackColor = FlewsBorder)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Color Palette
            Text("Warna Bentuk", style = MaterialTheme.typography.labelMedium.copy(color = FlewsTextSecondary))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SHAPE_COLORS.forEach { (hex, _) ->
                    val isSelected = hex == selectedColor
                    val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { FlewsPrimary }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) Color.White else FlewsBorder, CircleShape)
                            .clickable { selectedColor = hex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onAddShape(
                        selectedShape,
                        shapeSize,
                        if (selectedShape == ShapeType.LINE) 8f else shapeSize,
                        selectedColor,
                        "#5C8DFF",
                        strokeWidth
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("confirm_add_shape_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlewsPrimary, contentColor = Color.White)
            ) {
                Text(
                    text = "Tambahkan ke Timeline",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
