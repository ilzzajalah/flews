package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FlewsBackground
import com.example.ui.theme.FlewsBorder
import com.example.ui.theme.FlewsPrimary
import com.example.ui.theme.FlewsPrimaryLight
import com.example.ui.theme.FlewsSurface
import com.example.ui.theme.FlewsSurfaceHigh
import com.example.ui.theme.FlewsTextPrimary
import com.example.ui.theme.FlewsTextSecondary

data class AspectRatioOption(
    val id: String,
    val label: String,
    val width: Int,
    val height: Int,
    val ratioValue: Float
)

val PRESET_RATIOS = listOf(
    AspectRatioOption("1:1", "1:1", 1080, 1080, 1f),
    AspectRatioOption("4:5", "4:5", 1080, 1350, 4f / 5f),
    AspectRatioOption("9:16", "9:16", 1080, 1920, 9f / 16f),
    AspectRatioOption("16:9", "16:9", 1920, 1080, 16f / 9f),
    AspectRatioOption("4:3", "4:3", 1440, 1080, 4f / 3f),
    AspectRatioOption("21:9", "21:9", 2560, 1080, 21f / 9f),
    AspectRatioOption("custom", "Kustom", 1080, 1080, 1f)
)

val FPS_OPTIONS = listOf(24, 30, 60)
val DURATION_OPTIONS = listOf(3, 5, 10, 15, 30)

val COLOR_OPTIONS = listOf(
    "#0A0F1E" to "Navy Gelap",
    "#000000" to "Hitam",
    "#111833" to "Biru Tua",
    "#FFFFFF" to "Putih"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatioSelectionSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onCreateProject: (name: String, ratio: String, width: Long, height: Long, fps: Long, durationMs: Long, bgColor: String) -> Unit
) {
    var projectName by remember { mutableStateOf("Proyek Baru") }
    var selectedRatioId by remember { mutableStateOf("16:9") }
    var customWidthStr by remember { mutableStateOf("1080") }
    var customHeightStr by remember { mutableStateOf("1080") }
    var selectedFps by remember { mutableIntStateOf(30) }
    var selectedDurationSec by remember { mutableLongStateOf(5L) }
    var selectedBgColor by remember { mutableStateOf("#0A0F1E") }

    val currentRatio = PRESET_RATIOS.find { it.id == selectedRatioId } ?: PRESET_RATIOS[3]

    val effectiveWidth = if (selectedRatioId == "custom") {
        customWidthStr.toIntOrNull()?.coerceIn(100, 7680) ?: 1080
    } else {
        currentRatio.width
    }

    val effectiveHeight = if (selectedRatioId == "custom") {
        customHeightStr.toIntOrNull()?.coerceIn(100, 4320) ?: 1080
    } else {
        currentRatio.height
    }

    val previewRatio = (effectiveWidth.toFloat() / effectiveHeight.toFloat()).coerceIn(0.2f, 4f)

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
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Proyek Baru",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = FlewsTextPrimary
                    )
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_ratio_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = FlewsTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Project Name input
            OutlinedTextField(
                value = projectName,
                onValueChange = { projectName = it },
                label = { Text("Nama Proyek", color = FlewsTextSecondary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("project_name_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FlewsPrimary,
                    unfocusedBorderColor = FlewsBorder,
                    focusedTextColor = FlewsTextPrimary,
                    unfocusedTextColor = FlewsTextPrimary,
                    cursorColor = FlewsPrimary
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Ratio selection
            Text(
                text = "Rasio Aspek",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = FlewsTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PRESET_RATIOS.forEach { ratio ->
                    val isSelected = ratio.id == selectedRatioId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) FlewsPrimary else FlewsSurfaceHigh)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) FlewsPrimaryLight else FlewsBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedRatioId = ratio.id }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("ratio_option_${ratio.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ratio.label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else FlewsTextSecondary
                            )
                        )
                    }
                }
            }

            if (selectedRatioId == "custom") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = customWidthStr,
                        onValueChange = { customWidthStr = it },
                        label = { Text("Lebar (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_width_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlewsPrimary,
                            unfocusedBorderColor = FlewsBorder,
                            focusedTextColor = FlewsTextPrimary,
                            unfocusedTextColor = FlewsTextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = customHeightStr,
                        onValueChange = { customHeightStr = it },
                        label = { Text("Tinggi (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_height_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlewsPrimary,
                            unfocusedBorderColor = FlewsBorder,
                            focusedTextColor = FlewsTextPrimary,
                            unfocusedTextColor = FlewsTextPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Aspect Ratio Live Box Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(FlewsBackground, RoundedCornerShape(12.dp))
                    .border(1.dp, FlewsBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .height(100.dp)
                        .aspectRatio(previewRatio, matchHeightConstraintsFirst = true)
                        .background(
                            try {
                                Color(android.graphics.Color.parseColor(selectedBgColor))
                            } catch (e: Exception) {
                                FlewsBackground
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .border(1.5.dp, FlewsPrimaryLight, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${effectiveWidth} × ${effectiveHeight}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (selectedBgColor == "#FFFFFF") Color.Black else FlewsTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // FPS Selection
            Text(
                text = "Frame Rate (FPS)",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = FlewsTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FPS_OPTIONS.forEach { fps ->
                    val isSelected = fps == selectedFps
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) FlewsPrimary else FlewsSurfaceHigh)
                            .border(
                                1.dp,
                                if (isSelected) FlewsPrimaryLight else FlewsBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedFps = fps }
                            .padding(vertical = 10.dp)
                            .testTag("fps_option_$fps"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$fps FPS",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else FlewsTextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Duration Selection
            Text(
                text = "Durasi Awal",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = FlewsTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DURATION_OPTIONS.forEach { dur ->
                    val isSelected = dur.toLong() == selectedDurationSec
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) FlewsPrimary else FlewsSurfaceHigh)
                            .border(
                                1.dp,
                                if (isSelected) FlewsPrimaryLight else FlewsBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedDurationSec = dur.toLong() }
                            .padding(vertical = 10.dp)
                            .testTag("duration_option_$dur"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${dur}s",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else FlewsTextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Canvas Background Color
            Text(
                text = "Warna Latar Canvas",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = FlewsTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                COLOR_OPTIONS.forEach { (colorHex, colorLabel) ->
                    val isSelected = colorHex == selectedBgColor
                    val color = try {
                        Color(android.graphics.Color.parseColor(colorHex))
                    } catch (e: Exception) {
                        FlewsBackground
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) FlewsPrimary else FlewsBorder,
                                shape = CircleShape
                            )
                            .clickable { selectedBgColor = colorHex }
                            .testTag("bg_color_$colorHex"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = if (colorHex == "#FFFFFF") Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Create Project Action Button
            Button(
                onClick = {
                    val finalName = projectName.ifBlank { "Proyek Tanpa Judul" }
                    onCreateProject(
                        finalName,
                        selectedRatioId,
                        effectiveWidth.toLong(),
                        effectiveHeight.toLong(),
                        selectedFps.toLong(),
                        selectedDurationSec * 1000L,
                        selectedBgColor
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_create_project_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FlewsPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Buat Proyek",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
