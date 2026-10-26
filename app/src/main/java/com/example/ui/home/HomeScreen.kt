package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.model.LayerJsonSerializer
import com.example.data.model.ProjectEntity
import com.example.ui.auth.signOutUser
import com.example.ui.theme.FlewsAccentRed
import com.example.ui.theme.FlewsBackground
import com.example.ui.theme.FlewsBorder
import com.example.ui.theme.FlewsPrimary
import com.example.ui.theme.FlewsPrimaryLight
import com.example.ui.theme.FlewsSurface
import com.example.ui.theme.FlewsSurfaceHigh
import com.example.ui.theme.FlewsTextPrimary
import com.example.ui.theme.FlewsTextSecondary
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    projects: List<ProjectEntity>,
    isLoading: Boolean,
    onProjectClick: (ProjectEntity) -> Unit,
    onCreateProject: (name: String, ratio: String, width: Long, height: Long, fps: Long, durationMs: Long, bgColor: String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentUser = Firebase.auth.currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Flews Motion",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = FlewsTextPrimary,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(FlewsSurfaceHigh)
                            .border(1.dp, FlewsBorder, CircleShape)
                            .clickable { showProfileSheet = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("top_profile_icon_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Outlined.Person,
                                contentDescription = "Profil",
                                tint = if (currentUser != null) FlewsPrimaryLight else FlewsTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (currentUser != null) currentUser.email?.substringBefore("@") ?: "Google" else "Tamu",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (currentUser != null) FlewsTextPrimary else FlewsTextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FlewsSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = FlewsPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("create_project_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Proyek Baru",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        containerColor = FlewsBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = FlewsPrimary)
                    }
                }
                // BATASAN KOSONG: Empty State MUST display simple outline icon, text "Belum ada proyek", and button "+ Proyek Baru"
                projects.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = FlewsTextSecondary,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("empty_state_icon")
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Belum ada proyek",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = FlewsTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Buat proyek baru untuk memulai animasi motion graphics",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = FlewsTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showCreateSheet = true },
                            modifier = Modifier.testTag("empty_state_create_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FlewsPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+ Proyek Baru",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(projects, key = { it.id }) { project ->
                            ProjectCard(
                                project = project,
                                onClick = { onProjectClick(project) },
                                onDelete = { projectToDelete = project }
                            )
                        }
                    }
                }
            }
        }

        if (showCreateSheet) {
            RatioSelectionSheet(
                sheetState = sheetState,
                onDismiss = { showCreateSheet = false },
                onCreateProject = { name, ratio, width, height, fps, durationMs, bgColor ->
                    showCreateSheet = false
                    onCreateProject(name, ratio, width, height, fps, durationMs, bgColor)
                }
            )
        }

        if (showProfileSheet) {
            ProfileSheet(
                sheetState = sheetState,
                onDismiss = { showProfileSheet = false },
                onGoogleAuthSuccess = {
                    showProfileSheet = false
                },
                onSignOut = {
                    showProfileSheet = false
                    onSignOut()
                }
            )
        }

        projectToDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { projectToDelete = null },
                containerColor = FlewsSurface,
                title = {
                    Text(
                        text = "Hapus Proyek?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = FlewsTextPrimary
                        )
                    )
                },
                text = {
                    Text(
                        text = "Proyek \"${target.name}\" akan dihapus permanen dari cloud. Tindakan ini tidak dapat dibatalkan.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = FlewsTextSecondary)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = target.id
                            projectToDelete = null
                            onDeleteProject(id)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FlewsAccentRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { projectToDelete = null }) {
                        Text("Batal", color = FlewsTextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun ProjectCard(
    project: ProjectEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val layers = remember(project.layersJson) {
        LayerJsonSerializer.deserialize(project.layersJson)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, FlewsBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("project_card_${project.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FlewsSurface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail: solid surfaceHigh box with outline icon (as required by BATASAN KOSONG:
            // "Thumbnail proyek MUST berupa kotak polos warna surfaceHigh dengan ikon outline jika proyek belum punya layer visual; jika sudah ada, thumbnail diambil dari render frame proyek itu sendiri.")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(FlewsSurfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                if (layers.isEmpty()) {
                    Icon(
                        imageVector = Icons.Outlined.Movie,
                        contentDescription = null,
                        tint = FlewsTextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    // Render minimalist layer representation preview
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(project.backgroundColor))
                                } catch (e: Exception) {
                                    FlewsBackground
                                },
                                RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${layers.size} Layer",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = FlewsPrimaryLight,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // Aspect ratio badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(FlewsBackground.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = project.aspectRatio,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = FlewsTextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Info section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = FlewsTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_project_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Hapus",
                            tint = FlewsTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${project.fps} FPS · ${(project.durationMs / 1000f)}s",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = FlewsTextSecondary,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "${project.width}x${project.height}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = FlewsTextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}
