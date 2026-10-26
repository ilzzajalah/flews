package com.example.ui.editor.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectEntity
import com.example.ui.theme.FlewsBorder
import com.example.ui.theme.FlewsPrimary
import com.example.ui.theme.FlewsSurface
import com.example.ui.theme.FlewsSurfaceHigh
import com.example.ui.theme.FlewsTextPrimary
import com.example.ui.theme.FlewsTextSecondary

@Composable
fun ProjectSettingsDialog(
    project: ProjectEntity,
    onDismiss: () -> Unit,
    onSaveProjectSettings: (width: Long, height: Long, fps: Long, durationMs: Long) -> Unit
) {
    var selectedAspect by remember {
        mutableStateOf(
            when {
                project.width == 1080L && project.height == 1920L -> "9:16"
                project.width == 1920L && project.height == 1080L -> "16:9"
                project.width == 1080L && project.height == 1080L -> "1:1"
                project.width == 1080L && project.height == 1350L -> "4:5"
                else -> "Custom"
            }
        )
    }

    var customW by remember { mutableStateOf(project.width.toString()) }
    var customH by remember { mutableStateOf(project.height.toString()) }
    var selectedFps by remember { mutableLongStateOf(project.fps) }
    var durationSecStr by remember { mutableStateOf((project.durationMs / 1000L).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FlewsSurface,
        modifier = Modifier.testTag("project_settings_dialog"),
        title = {
            Text(
                text = "Pengaturan Proyek",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = FlewsTextPrimary
                )
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Rasio Kanvas
                Text(
                    text = "Rasio Kanvas",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = FlewsTextSecondary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("16:9", "9:16", "1:1", "4:5").forEach { ratio ->
                        val isSel = selectedAspect == ratio
                        TextButton(
                            onClick = { selectedAspect = ratio },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (isSel) FlewsPrimary else FlewsSurfaceHigh
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Text(
                                text = ratio,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else FlewsTextPrimary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Frame Rate (FPS)
                Text(
                    text = "Frame Rate (FPS)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = FlewsTextSecondary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(24L, 30L, 60L).forEach { fps ->
                        val isSel = selectedFps == fps
                        TextButton(
                            onClick = { selectedFps = fps },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (isSel) FlewsPrimary else FlewsSurfaceHigh
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Text(
                                text = "${fps} FPS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else FlewsTextPrimary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Durasi Proyek (detik)
                Text(
                    text = "Durasi Proyek (Detik)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = FlewsTextSecondary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = durationSecStr,
                    onValueChange = { durationSecStr = it.filter { char -> char.isDigit() } },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlewsPrimary,
                        unfocusedBorderColor = FlewsBorder,
                        focusedTextColor = FlewsTextPrimary,
                        unfocusedTextColor = FlewsTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val (w, h) = when (selectedAspect) {
                        "16:9" -> 1920L to 1080L
                        "9:16" -> 1080L to 1920L
                        "1:1" -> 1080L to 1080L
                        "4:5" -> 1080L to 1350L
                        else -> (customW.toLongOrNull() ?: 1080L) to (customH.toLongOrNull() ?: 1920L)
                    }
                    val durationSec = durationSecStr.toLongOrNull()?.coerceIn(1L, 300L) ?: 10L
                    onSaveProjectSettings(w, h, selectedFps, durationSec * 1000L)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = FlewsPrimary)
            ) {
                Text("Simpan", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = FlewsTextSecondary)
            }
        }
    )
}
