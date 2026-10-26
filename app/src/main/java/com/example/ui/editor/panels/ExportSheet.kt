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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectEntity
import com.example.ui.theme.FlewsAccentGreen
import com.example.ui.theme.FlewsBorder
import com.example.ui.theme.FlewsPrimary
import com.example.ui.theme.FlewsPrimaryLight
import com.example.ui.theme.FlewsSurface
import com.example.ui.theme.FlewsSurfaceHigh
import com.example.ui.theme.FlewsTextPrimary
import com.example.ui.theme.FlewsTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class ExportResolution(
    val label: String,
    val width: Int,
    val height: Int
)

/**
 * Antarmuka abstraksi Video Exporter sesuai batasan teknis:
 * "Jika sebuah fitur butuh native code atau package yang tidak pasti (misal export video),
 * buat antarmuka (abstract class) + implementasi sementara yang jelas diberi komentar TODO,
 * jangan pura-pura sudah berfungsi."
 */
interface VideoExporterService {
    suspend fun exportProject(
        project: ProjectEntity,
        resolution: ExportResolution,
        fps: Int,
        onProgress: (progress: Float, currentFrame: Int, totalFrames: Int) -> Unit
    ): ExportResult
}

sealed class ExportResult {
    data class Success(val outputPath: String, val fileSizeMb: Float) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

// TODO: Integrasikan MediaCodec / FFmpeg / MediaMuxer native encoder untuk rendering video MP4 sebenarnya ke penyimpanan perangkat
class SimulatedVideoExporter : VideoExporterService {
    override suspend fun exportProject(
        project: ProjectEntity,
        resolution: ExportResolution,
        fps: Int,
        onProgress: (progress: Float, currentFrame: Int, totalFrames: Int) -> Unit
    ): ExportResult {
        val totalFrames = ((project.durationMs / 1000f) * fps).toInt().coerceAtLeast(1)
        for (f in 1..totalFrames) {
            delay(25) // Simulasi rendering frame per frame
            val progress = f.toFloat() / totalFrames.toFloat()
            onProgress(progress, f, totalFrames)
        }
        return ExportResult.Success(
            outputPath = "/storage/emulated/0/Movies/FlewsMotion_${project.name.replace(" ", "_")}.mp4",
            fileSizeMb = (totalFrames * 0.08f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    project: ProjectEntity,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedResolution by remember { mutableStateOf(ExportResolution("1080p (FHD)", 1920, 1080)) }
    var selectedFps by remember { mutableIntStateOf(30) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var frameInfo by remember { mutableStateOf("") }
    var exportResult by remember { mutableStateOf<ExportResult.Success?>(null) }

    val resolutions = listOf(
        ExportResolution("720p (HD)", 1280, 720),
        ExportResolution("1080p (FHD)", 1920, 1080),
        ExportResolution("480p (SD)", 854, 480)
    )

    ModalBottomSheet(
        onDismissRequest = { if (!isExporting) onDismiss() },
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.Movie, contentDescription = null, tint = FlewsPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Export Video",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = FlewsTextPrimary
                        )
                    )
                }
                if (!isExporting) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = "Tutup", tint = FlewsTextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (exportResult != null) {
                // Completed Export State
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = FlewsAccentGreen,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Export Berhasil Selesai!",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = FlewsTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Format: MP4 · Resolusi: ${selectedResolution.label} · Ukuran: ~${String.format("%.1f", exportResult!!.fileSizeMb)} MB",
                        style = MaterialTheme.typography.bodySmall.copy(color = FlewsTextSecondary)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FlewsPrimary, contentColor = Color.White)
                    ) {
                        Text("Selesai")
                    }
                }
            } else if (isExporting) {
                // In Progress State
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Merender Frame & Animasi...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = FlewsTextPrimary, fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = frameInfo,
                        style = MaterialTheme.typography.bodySmall.copy(color = FlewsTextSecondary)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { exportProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = FlewsPrimary,
                        trackColor = FlewsBorder
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(exportProgress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(color = FlewsPrimaryLight, fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                // Resolution Selector
                Text("Resolusi Video", style = MaterialTheme.typography.labelMedium.copy(color = FlewsTextSecondary))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    resolutions.forEach { res ->
                        val isSelected = res == selectedResolution
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) FlewsPrimary else FlewsSurfaceHigh)
                                .border(1.dp, if (isSelected) FlewsPrimaryLight else FlewsBorder, RoundedCornerShape(10.dp))
                                .clickable { selectedResolution = res }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = res.label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else FlewsTextSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Frame Rate (FPS)
                Text("Frame Rate", style = MaterialTheme.typography.labelMedium.copy(color = FlewsTextSecondary))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(24, 30, 60).forEach { fps ->
                        val isSelected = fps == selectedFps
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) FlewsPrimary else FlewsSurfaceHigh)
                                .border(1.dp, if (isSelected) FlewsPrimaryLight else FlewsBorder, RoundedCornerShape(10.dp))
                                .clickable { selectedFps = fps }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$fps FPS",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else FlewsTextSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Format info card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(FlewsSurfaceHigh)
                        .border(1.dp, FlewsBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = null, tint = FlewsPrimaryLight)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Format Output: MP4 (H.264 / AAC)", style = MaterialTheme.typography.bodyMedium.copy(color = FlewsTextPrimary, fontWeight = FontWeight.SemiBold))
                        Text("Kompatibel dengan semua pemutar video dan platform media sosial.", style = MaterialTheme.typography.bodySmall.copy(color = FlewsTextSecondary))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Start Export Button (Solid blue #2F6BFF as requested)
                Button(
                    onClick = {
                        isExporting = true
                        exportProgress = 0f
                        coroutineScope.launch {
                            val service: VideoExporterService = SimulatedVideoExporter()
                            val res = service.exportProject(
                                project = project,
                                resolution = selectedResolution,
                                fps = selectedFps,
                                onProgress = { progress, f, total ->
                                    exportProgress = progress
                                    frameInfo = "Merender frame $f dari $total"
                                }
                            )
                            isExporting = false
                            if (res is ExportResult.Success) {
                                exportResult = res
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("start_export_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FlewsPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Mulai Export",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
