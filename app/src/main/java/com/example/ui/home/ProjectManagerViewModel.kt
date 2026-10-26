package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ProjectEntity
import com.example.data.repository.ProjectRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ProjectManagerUiState(
    val projects: List<ProjectEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val error: String? = null,
    val selectedProject: ProjectEntity? = null
)

class ProjectManagerViewModel(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectManagerUiState(isLoading = true))
    val uiState: StateFlow<ProjectManagerUiState> = _uiState.asStateFlow()

    init {
        observeProjects()
    }

    fun observeProjects() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            projectRepository.observeProjects()
                .catch { cause ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = cause.message ?: "Gagal memuat daftar proyek."
                    )
                }
                .collect { projectList ->
                    _uiState.value = _uiState.value.copy(
                        projects = projectList,
                        isLoading = false
                    )
                }
        }
    }

    fun createProject(
        name: String,
        aspectRatio: String,
        width: Long,
        height: Long,
        fps: Long,
        durationMs: Long,
        backgroundColor: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, error = null)
            val result = projectRepository.createProject(
                name = name,
                aspectRatio = aspectRatio,
                width = width,
                height = height,
                fps = fps,
                durationMs = durationMs,
                backgroundColor = backgroundColor,
                layersJson = "[]" // Initial empty layer list per Batasan Kosong
            )
            _uiState.value = _uiState.value.copy(isCreating = false)
            if (result.isSuccess) {
                val newId = result.getOrThrow()
                val createdProject = projectRepository.getProject(newId)
                if (createdProject != null) {
                    _uiState.value = _uiState.value.copy(selectedProject = createdProject)
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message ?: "Gagal membuat proyek baru."
                )
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            val result = projectRepository.deleteProject(projectId)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message ?: "Gagal menghapus proyek."
                )
            }
        }
    }

    fun selectProject(project: ProjectEntity) {
        _uiState.value = _uiState.value.copy(selectedProject = project)
    }

    fun clearSelectedProject() {
        _uiState.value = _uiState.value.copy(selectedProject = null)
    }

    fun signOut() {
        viewModelScope.launch {
            Firebase.auth.signOut()
            _uiState.value = _uiState.value.copy(selectedProject = null)
            observeProjects()
        }
    }
}

class ProjectManagerViewModelFactory(
    private val projectRepository: ProjectRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectManagerViewModel::class.java)) {
            return ProjectManagerViewModel(projectRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
