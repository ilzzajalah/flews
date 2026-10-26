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
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.ui.theme.FlewsBorder
import com.example.ui.theme.FlewsPrimary
import com.example.ui.theme.FlewsPrimaryLight
import com.example.ui.theme.FlewsSurface
import com.example.ui.theme.FlewsSurfaceHigh
import com.example.ui.theme.FlewsTextPrimary
import com.example.ui.theme.FlewsTextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onAddText: (text: String, fontSize: Float, color: String, isBold: Boolean, isItalic: Boolean) -> Unit
) {
    var textInput by remember { mutableStateOf("Motion Graphics") }
    var fontSize by remember { mutableFloatStateOf(42f) }
    var selectedColor by remember { mutableStateOf("#F2F5FF") }
    var isBold by remember { mutableStateOf(true) }
    var isItalic by remember { mutableStateOf(false) }

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
                    text = "Tambah Layer Teks",
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

            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text("Teks Animasi") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("text_layer_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FlewsPrimary,
                    unfocusedBorderColor = FlewsBorder,
                    focusedTextColor = FlewsTextPrimary,
                    unfocusedTextColor = FlewsTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Ukuran Huruf", style = MaterialTheme.typography.labelMedium.copy(color = FlewsTextSecondary))
                Text("${fontSize.roundToInt()} sp", style = MaterialTheme.typography.labelMedium.copy(color = FlewsPrimaryLight, fontWeight = FontWeight.Bold))
            }
            Slider(
                value = fontSize,
                onValueChange = { fontSize = it },
                valueRange = 16f..120f,
                colors = SliderDefaults.colors(thumbColor = FlewsPrimary, activeTrackColor = FlewsPrimary, inactiveTrackColor = FlewsBorder)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Style Toggles: Bold & Italic
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isBold) FlewsPrimary else FlewsSurfaceHigh)
                        .border(1.dp, if (isBold) FlewsPrimaryLight else FlewsBorder, RoundedCornerShape(8.dp))
                        .clickable { isBold = !isBold }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.FormatBold, contentDescription = null, tint = if (isBold) Color.White else FlewsTextSecondary)
                        Text(" Tebal", color = if (isBold) Color.White else FlewsTextSecondary, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isItalic) FlewsPrimary else FlewsSurfaceHigh)
                        .border(1.dp, if (isItalic) FlewsPrimaryLight else FlewsBorder, RoundedCornerShape(8.dp))
                        .clickable { isItalic = !isItalic }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.FormatItalic, contentDescription = null, tint = if (isItalic) Color.White else FlewsTextSecondary)
                        Text(" Miring", color = if (isItalic) Color.White else FlewsTextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Text Colors
            Text("Warna Teks", style = MaterialTheme.typography.labelMedium.copy(color = FlewsTextSecondary))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    "#F2F5FF" to "Putih",
                    "#2F6BFF" to "Biru",
                    "#5C8DFF" to "Biru Muda",
                    "#FF4D4D" to "Merah",
                    "#00E676" to "Hijau",
                    "#FFD600" to "Kuning"
                ).forEach { (hex, _) ->
                    val isSelected = hex == selectedColor
                    val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.White }
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
                    val finalTxt = textInput.ifBlank { "Teks Baru" }
                    onAddText(finalTxt, fontSize, selectedColor, isBold, isItalic)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("confirm_add_text_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlewsPrimary, contentColor = Color.White)
            ) {
                Text(text = "Tambahkan ke Timeline", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
