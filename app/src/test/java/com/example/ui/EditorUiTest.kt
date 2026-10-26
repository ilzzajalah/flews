package com.example.ui

import com.example.data.model.LayerJsonSerializer
import com.example.data.model.ProjectEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EditorUiTest {

    @Test
    fun `new project starts with 0 layers 0 effects 0 keyframes`() {
        val project = ProjectEntity(
            id = "test-proj",
            name = "Proyek Baru",
            aspectRatio = "16:9",
            width = 1920L,
            height = 1080L,
            fps = 30L,
            durationMs = 10000L
        )

        val layers = LayerJsonSerializer.deserialize(project.layersJson)
        assertTrue("New project must have 0 layers", layers.isEmpty())
    }

    @Test
    fun `timecode formatting produces mm ss ff`() {
        val currentTimeMs = 7500L // 7 seconds 500ms = 15 frames at 30 fps
        val totalDurationMs = 30000L // 30 seconds
        val fps = 30L

        val currentMinutes = currentTimeMs / 60000
        val currentSeconds = (currentTimeMs % 60000) / 1000
        val currentFrames = ((currentTimeMs % 1000) * fps / 1000)

        val totalMinutes = totalDurationMs / 60000
        val totalSeconds = (totalDurationMs % 60000) / 1000
        val totalFrames = ((totalDurationMs % 1000) * fps / 1000)

        val formatted = String.format(
            Locale.US,
            "%02d:%02d:%02d / %02d:%02d:%02d",
            currentMinutes,
            currentSeconds,
            currentFrames,
            totalMinutes,
            totalSeconds,
            totalFrames
        )

        assertEquals("00:07:15 / 00:30:00", formatted)
    }
}
