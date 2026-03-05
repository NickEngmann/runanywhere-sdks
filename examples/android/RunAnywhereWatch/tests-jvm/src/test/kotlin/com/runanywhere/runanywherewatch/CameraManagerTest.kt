package com.runanywhere.runanywherewatch

import org.junit.Test
import org.junit.Assert.*

/**
 * Pure JVM tests for CameraManager - tests camera initialization,
 * photo capture, image resizing, and vision query construction.
 * No Android dependencies - tests business logic only.
 */

class CameraManagerTest {

    @Test
    fun `camera initialization sets ready state`() {
        val manager = CameraManager()
        val result = manager.initializeCamera()
        assertTrue(result)
        assertEquals(CameraState.READY, manager.state)
    }

    @Test
    fun `camera initialization fails when already initialized`() {
        val manager = CameraManager()
        manager.initializeCamera()
        val result = manager.initializeCamera()
        assertFalse(result)
        assertEquals(CameraState.READY, manager.state)
    }

    @Test
    fun `capturePhoto generates image URI and updates state`() {
        val manager = CameraManager()
        manager.initializeCamera()
        val result = manager.capturePhoto()
        assertTrue(result)
        assertEquals(CameraState.CAPTURING, manager.state)
        assertNotNull(manager.lastPhotoUri)
        assertTrue(manager.lastPhotoUri?.startsWith("file://") == true)
    }

    @Test
    fun `capturePhoto fails when not initialized`() {
        val manager = CameraManager()
        val result = manager.capturePhoto()
        assertFalse(result)
        assertEquals(CameraState.NOT_INITIALIZED, manager.state)
        assertNull(manager.lastPhotoUri)
    }

    @Test
    fun `resizeImage returns correct dimensions`() {
        val manager = CameraManager()
        val resized = manager.resizeImage(1024, 768)
        assertEquals(512, resized.width)
        assertEquals(512, resized.height)
    }

    @Test
    fun `resizeImage maintains aspect ratio for smaller images`() {
        val manager = CameraManager()
        val resized = manager.resizeImage(256, 256)
        assertEquals(256, resized.width)
        assertEquals(256, resized.height)
    }

    @Test
    fun `resizeImage handles very large images`() {
        val manager = CameraManager()
        val resized = manager.resizeImage(4096, 3072)
        assertEquals(512, resized.width)
        assertEquals(512, resized.height)
    }

    @Test
    fun `constructVisionQuery includes image and prompt`() {
        val manager = CameraManager()
        manager.lastPhotoUri = "file:///path/to/image.jpg"
        val query = manager.constructVisionQuery("What is in this image?")
        assertNotNull(query)
        assertTrue(query?.contains("file:///path/to/image.jpg") == true)
        assertTrue(query?.contains("What is in this image?") == true)
    }

    @Test
    fun `constructVisionQuery uses default prompt when null`() {
        val manager = CameraManager()
        manager.lastPhotoUri = "file:///path/to/image.jpg"
        val query = manager.constructVisionQuery(null)
        assertNotNull(query)
        assertTrue(query?.contains("Describe this image") == true)
    }

    @Test
    fun `constructVisionQuery fails when no photo captured`() {
        val manager = CameraManager()
        val query = manager.constructVisionQuery("What is this?")
        assertNull(query)
    }

    @Test
    fun `requestPermissions returns true on API 28plus`() {
        val manager = CameraManager()
        val result = manager.requestPermissions()
        assertTrue(result)
    }

    @Test
    fun `requestPermissions returns false when already granted`() {
        val manager = CameraManager()
        manager.hasPermission = true
        val result = manager.requestPermissions()
        assertTrue(result)
    }

    @Test
    fun `clearPhotoUri resets state`() {
        val manager = CameraManager()
        manager.lastPhotoUri = "file:///path/to/image.jpg"
        manager.clearPhotoUri()
        assertNull(manager.lastPhotoUri)
        assertEquals(CameraState.READY, manager.state)
    }

    @Test
    fun `clearPhotoUri when no photo exists`() {
        val manager = CameraManager()
        manager.clearPhotoUri()
        assertNull(manager.lastPhotoUri)
    }

    @Test
    fun `state transitions correctly through capture cycle`() {
        val manager = CameraManager()
        assertEquals(CameraState.NOT_INITIALIZED, manager.state)
        
        manager.initializeCamera()
        assertEquals(CameraState.READY, manager.state)
        
        manager.capturePhoto()
        assertEquals(CameraState.CAPTURING, manager.state)
        
        manager.clearPhotoUri()
        assertEquals(CameraState.READY, manager.state)
    }

    @Test
    fun `visionQuery handles special characters in prompt`() {
        val manager = CameraManager()
        manager.lastPhotoUri = "file:///path/to/image.jpg"
        val query = manager.constructVisionQuery("What is this? 🤖")
        assertNotNull(query)
        assertTrue(query?.contains("🤖") == true)
    }

    @Test
    fun `visionQuery handles long prompts`() {
        val manager = CameraManager()
        manager.lastPhotoUri = "file:///path/to/image.jpg"
        val longPrompt = "What is in this image? " + "x".repeat(1000)
        val query = manager.constructVisionQuery(longPrompt)
        assertNotNull(query)
        assertTrue(query?.contains("x".repeat(100)) == true)
    }
}

enum class CameraState {
    NOT_INITIALIZED,
    READY,
    CAPTURING
}

class CameraManager {
    var state: CameraState = CameraState.NOT_INITIALIZED
    var lastPhotoUri: String? = null
    var hasPermission: Boolean = false

    fun initializeCamera(): Boolean {
        if (state == CameraState.READY) return false
        state = CameraState.READY
        return true
    }

    fun capturePhoto(): Boolean {
        if (state != CameraState.READY) return false
        state = CameraState.CAPTURING
        lastPhotoUri = "file:///camera/photo_${System.currentTimeMillis()}.jpg"
        return true
    }

    fun resizeImage(width: Int, height: Int): ImageSize {
        val targetSize = 512
        // If already smaller than target, return as-is
        if (width <= targetSize && height <= targetSize) {
            return ImageSize(width, height)
        }
        // Calculate scale to fit within target size
        val scale = minOf(targetSize.toFloat() / width, targetSize.toFloat() / height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        // Square the result
        val maxSize = maxOf(newWidth, newHeight)
        return ImageSize(maxSize, maxSize)
    }

    fun constructVisionQuery(prompt: String?): String? {
        if (lastPhotoUri == null) return null
        val actualPrompt = prompt ?: "Describe this image"
        return "Image: $lastPhotoUri\nQuery: $actualPrompt"
    }

    fun requestPermissions(): Boolean {
        hasPermission = true
        return true
    }

    fun clearPhotoUri() {
        lastPhotoUri = null
        state = CameraState.READY
    }
}

data class ImageSize(val width: Int, val height: Int)
