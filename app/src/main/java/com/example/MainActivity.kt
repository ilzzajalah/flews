package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ProjectEntity
import com.example.data.repository.ProjectRepository
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.editor.EditorScreen
import com.example.ui.home.HomeScreen
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.FlewsBackground
import com.example.ui.theme.FlewsMotionTheme
import com.example.ui.theme.FlewsPrimary
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

import com.example.ui.home.ProjectManagerViewModel
import com.example.ui.home.ProjectManagerViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FlewsMotionTheme {
                FlewsMotionApp()
            }
        }
    }
}

@Composable
fun FlewsMotionApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var showSplash by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                },
                onUnauthenticated = {
                    // Allowed in Guest Mode
                },
                scope = coroutineScope
            )
        }
    }

    if (showSplash) {
        SplashScreen(
            onSplashFinished = {
                showSplash = false
            }
        )
    } else {
        // Enters directly into Guest Mode or Google authenticated session
        val currentUid = currentUser?.uid ?: "guest_user"
        MainContent(currentUserId = currentUid)
    }
}

@Composable
fun MainContent(
    currentUserId: String,
    viewModel: ProjectManagerViewModel = viewModel(
        factory = ProjectManagerViewModelFactory(ProjectRepository(LocalContext.current))
    )
) {
    val context = LocalContext.current
    val projectRepository = remember(currentUserId) { ProjectRepository(context) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.selectedProject != null) {
        EditorScreen(
            initialProject = uiState.selectedProject!!,
            projectRepository = projectRepository,
            onBack = { viewModel.clearSelectedProject() }
        )
    } else {
        HomeScreen(
            projects = uiState.projects,
            isLoading = uiState.isLoading || uiState.isCreating,
            onProjectClick = { project ->
                viewModel.selectProject(project)
            },
            onCreateProject = { name, ratio, width, height, fps, durationMs, bgColor ->
                viewModel.createProject(
                    name = name,
                    aspectRatio = ratio,
                    width = width,
                    height = height,
                    fps = fps,
                    durationMs = durationMs,
                    backgroundColor = bgColor
                )
            },
            onDeleteProject = { projectId ->
                viewModel.deleteProject(projectId)
            },
            onSignOut = {
                viewModel.signOut()
            }
        )
    }
}
