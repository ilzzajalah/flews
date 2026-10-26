package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ProjectEntity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndGetProject_authenticatedUser_success() = runBlocking {
        val aliceUid = signInTestUser("alice_test@example.com")
        val repo = ProjectRepository(firestore)

        val createResult = withTimeout(10000L) {
            repo.createProject(
                name = "Proyek Animasi 1",
                aspectRatio = "16:9",
                width = 1920L,
                height = 1080L,
                fps = 30L,
                durationMs = 5000L,
                backgroundColor = "#0A0F1E",
                layersJson = "[]"
            )
        }

        assertTrue("Project creation should succeed", createResult.isSuccess)
        val projectId = createResult.getOrThrow()
        assertNotNull("Project ID must not be null", projectId)

        val fetchedProject = withTimeout(10000L) {
            repo.getProject(projectId)
        }
        assertNotNull("Fetched project must exist", fetchedProject)
        assertEquals("Name must match", "Proyek Animasi 1", fetchedProject?.name)
        assertEquals("User ID must match", aliceUid, fetchedProject?.userId)
    }

    @Test
    fun updateProject_authenticatedOwner_success() = runBlocking {
        signInTestUser("bob_test@example.com")
        val repo = ProjectRepository(firestore)

        val createResult = repo.createProject(
            name = "Proyek Bob",
            aspectRatio = "9:16",
            width = 1080L,
            height = 1920L,
            fps = 60L,
            durationMs = 3000L,
            backgroundColor = "#111833"
        )
        val projectId = createResult.getOrThrow()
        val project = repo.getProject(projectId)!!

        val updated = project.copy(name = "Proyek Bob Direvisi")
        val updateResult = repo.updateProject(updated)
        assertTrue("Project update should succeed", updateResult.isSuccess)

        val refetched = repo.getProject(projectId)
        assertEquals("Proyek Bob Direvisi", refetched?.name)
    }

    @Test
    fun deleteProject_authenticatedOwner_success() = runBlocking {
        signInTestUser("charlie_test@example.com")
        val repo = ProjectRepository(firestore)

        val createResult = repo.createProject(
            name = "Proyek Charlie Hapus",
            aspectRatio = "1:1",
            width = 1080L,
            height = 1080L,
            fps = 24L,
            durationMs = 4000L,
            backgroundColor = "#0A0F1E"
        )
        val projectId = createResult.getOrThrow()

        val deleteResult = repo.deleteProject(projectId)
        assertTrue("Project delete should succeed", deleteResult.isSuccess)

        val refetched = repo.getProject(projectId)
        assertEquals(null, refetched)
    }
}
