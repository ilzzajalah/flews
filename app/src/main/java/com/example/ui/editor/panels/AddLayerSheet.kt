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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLayerSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onAddMedia: () -> Unit,
    onAddCamera: () -> Unit,
    onAddText: () -> Unit,
    onAddShape: () -> Unit,
    onAddNull: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FlewsSurface,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        modifier = Modifier.testTag("add_layer_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Tambah Layer",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = FlewsTextPrimary
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 3-column Grid arrangement for options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AddOptionCard(
                    icon = Icons.Outlined.Image,
                    label = "Foto / Video",
                    testTag = "add_photo_option",
                    onClick = {
                        onDismiss()
                        onAddMedia()
                    },
                    modifier = Modifier.weight(1f)
                )

                AddOptionCard(
                    icon = Icons.Outlined.CameraAlt,
                    label = "Kamera",
                    testTag = "add_camera_option",
                    onClick = {
                        onDismiss()
                        onAddCamera()
                    },
                    modifier = Modifier.weight(1f)
                )

                AddOptionCard(
                    icon = Icons.Outlined.TextFields,
                    label = "Teks",
                    testTag = "add_text_option",
                    onClick = {
                        onDismiss()
                        onAddText()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AddOptionCard(
                    icon = Icons.Outlined.Category,
                    label = "Bentuk",
                    testTag = "add_shape_option",
                    onClick = {
                        onDismiss()
                        onAddShape()
                    },
                    modifier = Modifier.weight(1f)
                )

                AddOptionCard(
                    icon = Icons.Outlined.Videocam,
                    label = "Null Object",
                    testTag = "add_null_option",
                    onClick = {
                        onDismiss()
                        onAddNull()
                    },
                    modifier = Modifier.weight(1f)
                )

                // Placeholder space for balanced 3-column grid
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AddOptionCard(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(FlewsSurfaceHigh)
            .border(1.dp, FlewsBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(FlewsPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = FlewsPrimaryLight,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                color = FlewsTextPrimary,
                fontSize = 12.sp
            ),
            maxLines = 1
        )
    }
}
