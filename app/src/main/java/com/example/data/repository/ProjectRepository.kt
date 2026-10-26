package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ProjectEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ProjectRepository(
    private val db: FirebaseFirestore,
    context: Context? = null
) {

    private val appContext: Context? = context?.applicationContext

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        context.applicationContext
    )

    private val auth = Firebase.auth
    private val localGuestProjects = MutableStateFlow<List<ProjectEntity>>(loadLocalProjectsFromDisk())

    private fun loadLocalProjectsFromDisk(): List<ProjectEntity> {
        val ctx = appContext ?: return emptyList()
        return try {
            val file = File(ctx.filesDir, "local_guest_projects.json")
            if (!file.exists()) return emptyList()
            val jsonStr = file.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ProjectEntity>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ProjectEntity(
                        id = obj.optString("id"),
                        userId = obj.optString("userId", "guest_user"),
                        name = obj.optString("name", "Proyek Baru"),
                        aspectRatio = obj.optString("aspectRatio", "16:9"),
                        width = obj.optLong("width", 1280L),
                        height = obj.optLong("height", 720L),
                        fps = obj.optLong("fps", 30L),
                        durationMs = obj.optLong("durationMs", 5000L),
                        backgroundColor = obj.optString("backgroundColor", "#0A0F1E"),
                        layersJson = obj.optString("layersJson", "[]")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveLocalProjectsToDisk(projects: List<ProjectEntity>) {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, "local_guest_projects.json")
            val array = JSONArray()
            projects.forEach { p ->
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("userId", p.userId)
                obj.put("name", p.name)
                obj.put("aspectRatio", p.aspectRatio)
                obj.put("width", p.width)
                obj.put("height", p.height)
                obj.put("fps", p.fps)
                obj.put("durationMs", p.durationMs)
                obj.put("backgroundColor", p.backgroundColor)
                obj.put("layersJson", p.layersJson)
                array.put(obj)
            }
            file.writeText(array.toString())
        } catch (e: Exception) {
            // Ignore disk write errors
        }
    }

    private fun getEffectiveUserId(): String {
        return auth.currentUser?.uid ?: "guest_user"
    }

    fun observeProjects(): Flow<List<ProjectEntity>> = flow {
        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid
            val path = "users/$uid/projects"

            // If there were local guest projects created before logging in, sync them to Firestore
            val pendingGuestProjects = localGuestProjects.value
            if (pendingGuestProjects.isNotEmpty()) {
                pendingGuestProjects.forEach { guestProj ->
                    try {
                        val docRef = db.collection("users").document(uid).collection("projects").document(guestProj.id)
                        val payload = mapOf(
                            "userId" to uid,
                            "name" to guestProj.name,
                            "aspectRatio" to guestProj.aspectRatio,
                            "width" to guestProj.width,
                            "height" to guestProj.height,
                            "fps" to guestProj.fps,
                            "durationMs" to guestProj.durationMs,
                            "backgroundColor" to guestProj.backgroundColor,
                            "layersJson" to guestProj.layersJson,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        docRef.set(payload).await()
                    } catch (e: Exception) {
                        // Ignore individual sync errors
                    }
                }
                localGuestProjects.value = emptyList()
                saveLocalProjectsToDisk(emptyList())
            }

            emitAll(
                db.collection("users").document(uid).collection("projects")
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(ProjectEntity::class.java) }
                    .catch { error ->
                        if (error is Exception) {
                            handleFirestoreError(error, OperationType.LIST, path)
                        }
                        emit(emptyList())
                    }
            )
        } else {
            // Guest Mode: Observe local in-memory projects without unauthenticated Firestore queries
            emitAll(localGuestProjects)
        }
    }

    suspend fun getProject(projectId: String): ProjectEntity? {
        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid
            val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
            return try {
                val snapshot = docRef.get().await()
                snapshot.toObject(ProjectEntity::class.java)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, docRef.path)
                null
            }
        } else {
            return localGuestProjects.value.find { it.id == projectId }
        }
    }

    suspend fun createProject(
        name: String,
        aspectRatio: String,
        width: Long,
        height: Long,
        fps: Long,
        durationMs: Long,
        backgroundColor: String,
        layersJson: String = "[]"
    ): Result<String> {
        val user = auth.currentUser
        val newId = UUID.randomUUID().toString()

        if (user != null) {
            val uid = user.uid
            val projectsRef = db.collection("users").document(uid).collection("projects")
            val docRef = projectsRef.document(newId)
            val data = mapOf(
                "userId" to uid,
                "name" to name,
                "aspectRatio" to aspectRatio,
                "width" to width,
                "height" to height,
                "fps" to fps,
                "durationMs" to durationMs,
                "backgroundColor" to backgroundColor,
                "layersJson" to layersJson,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            return try {
                docRef.set(data).await()
                Result.success(docRef.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, projectsRef.path)
                Result.failure(e)
            }
        } else {
            // Guest Mode creation
            val guestProj = ProjectEntity(
                id = newId,
                userId = "guest_user",
                name = name,
                aspectRatio = aspectRatio,
                width = width,
                height = height,
                fps = fps,
                durationMs = durationMs,
                backgroundColor = backgroundColor,
                layersJson = layersJson
            )
            val newList = localGuestProjects.value + guestProj
            localGuestProjects.value = newList
            saveLocalProjectsToDisk(newList)
            return Result.success(newId)
        }
    }

    suspend fun updateProject(project: ProjectEntity): Result<Unit> {
        val user = auth.currentUser
        require(project.id.isNotBlank()) { "ID proyek tidak boleh kosong saat pembaruan." }

        if (user != null) {
            val uid = user.uid
            val docRef = db.collection("users").document(uid).collection("projects").document(project.id)
            return try {
                val updatePayload = mapOf(
                    "userId" to uid,
                    "name" to project.name,
                    "aspectRatio" to project.aspectRatio,
                    "width" to project.width,
                    "height" to project.height,
                    "fps" to project.fps,
                    "durationMs" to project.durationMs,
                    "backgroundColor" to project.backgroundColor,
                    "layersJson" to project.layersJson,
                    "createdAt" to (project.createdAt ?: FieldValue.serverTimestamp()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(updatePayload).await()
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                Result.failure(e)
            }
        } else {
            // Guest Mode update
            val newList = localGuestProjects.value.map {
                if (it.id == project.id) project else it
            }
            localGuestProjects.value = newList
            saveLocalProjectsToDisk(newList)
            return Result.success(Unit)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid
            val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
            return try {
                docRef.delete().await()
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, docRef.path)
                Result.failure(e)
            }
        } else {
            // Guest Mode delete
            val newList = localGuestProjects.value.filterNot { it.id == projectId }
            localGuestProjects.value = newList
            saveLocalProjectsToDisk(newList)
            return Result.success(Unit)
        }
    }
}
