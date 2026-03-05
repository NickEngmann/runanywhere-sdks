package com.runanywhere.runanywherewatch

import org.junit.Test
import org.junit.Assert.*

/**
 * Pure JVM tests for ModelManager - tests model loading state machine,
 * configuration, and file path management without Android SDK.
 */

// Simplified ModelManager for testing (no Android dependencies)
enum class ModelState { IDLE, DOWNLOADING, LOADING, READY, ERROR }

data class ModelConfig(
    val modelName: String = "Qwen3.5-0.8B",
    val quantization: String = "Q4_K_M",
    val fileSizeBytes: Long = 533_000_000L,
    val contextLength: Int = 4096,
    val threads: Int = 4,
    val gpuLayers: Int = 0,
    val modelFileName: String = "qwen3.5-0.8b-q4_k_m.gguf"
)

class ModelManager private constructor() {
    var state: ModelState = ModelState.IDLE
        private set
    var config: ModelConfig = ModelConfig()
        private set
    var downloadProgress: Float = 0f
        private set
    var errorMessage: String? = null
        private set
    var modelPath: String? = null
        private set

    companion object {
        @Volatile private var instance: ModelManager? = null

        fun getInstance(): ModelManager =
            instance ?: synchronized(this) {
                instance ?: ModelManager().also { instance = it }
            }

        // For testing - reset singleton
        fun resetForTesting() {
            instance = null
        }
    }

    fun updateConfig(config: ModelConfig) {
        if (state != ModelState.IDLE && state != ModelState.ERROR) {
            throw IllegalStateException("Cannot update config while $state")
        }
        this.config = config
    }

    fun startDownload(targetDir: String): Boolean {
        if (state != ModelState.IDLE && state != ModelState.ERROR) return false
        state = ModelState.DOWNLOADING
        downloadProgress = 0f
        errorMessage = null
        modelPath = "$targetDir/${config.modelFileName}"
        return true
    }

    fun updateDownloadProgress(progress: Float) {
        if (state != ModelState.DOWNLOADING) return
        downloadProgress = progress.coerceIn(0f, 1f)
        if (downloadProgress >= 1f) {
            state = ModelState.LOADING
        }
    }

    fun onModelLoaded() {
        if (state != ModelState.LOADING) return
        state = ModelState.READY
    }

    fun onError(message: String) {
        state = ModelState.ERROR
        errorMessage = message
    }

    fun reset() {
        state = ModelState.IDLE
        downloadProgress = 0f
        errorMessage = null
        modelPath = null
    }

    fun isReady(): Boolean = state == ModelState.READY
    fun getModelSizeMB(): Long = config.fileSizeBytes / (1024 * 1024)
}

class ModelManagerTest {

    @org.junit.Before
    fun setUp() {
        ModelManager.resetForTesting()
    }

    @Test
    fun `singleton returns same instance`() {
        val a = ModelManager.getInstance()
        val b = ModelManager.getInstance()
        assertSame(a, b)
    }

    @Test
    fun `initial state is IDLE`() {
        val mgr = ModelManager.getInstance()
        assertEquals(ModelState.IDLE, mgr.state)
        assertEquals(0f, mgr.downloadProgress, 0.001f)
        assertNull(mgr.errorMessage)
        assertNull(mgr.modelPath)
    }

    @Test
    fun `default config is Qwen3_5 0_8B Q4_K_M`() {
        val mgr = ModelManager.getInstance()
        assertEquals("Qwen3.5-0.8B", mgr.config.modelName)
        assertEquals("Q4_K_M", mgr.config.quantization)
        assertEquals(533_000_000L, mgr.config.fileSizeBytes)
        assertEquals(4096, mgr.config.contextLength)
        assertEquals("qwen3.5-0.8b-q4_k_m.gguf", mgr.config.modelFileName)
    }

    @Test
    fun `model size in MB is correct`() {
        val mgr = ModelManager.getInstance()
        assertEquals(508L, mgr.getModelSizeMB())  // 533000000 / 1048576
    }

    @Test
    fun `startDownload transitions to DOWNLOADING`() {
        val mgr = ModelManager.getInstance()
        assertTrue(mgr.startDownload("/data/models"))
        assertEquals(ModelState.DOWNLOADING, mgr.state)
        assertEquals("/data/models/qwen3.5-0.8b-q4_k_m.gguf", mgr.modelPath)
    }

    @Test
    fun `startDownload fails when already downloading`() {
        val mgr = ModelManager.getInstance()
        mgr.startDownload("/data/models")
        assertFalse(mgr.startDownload("/data/models"))
    }

    @Test
    fun `download progress updates correctly`() {
        val mgr = ModelManager.getInstance()
        mgr.startDownload("/data/models")
        mgr.updateDownloadProgress(0.5f)
        assertEquals(0.5f, mgr.downloadProgress, 0.001f)
        assertEquals(ModelState.DOWNLOADING, mgr.state)
    }

    @Test
    fun `download progress clamps to 0-1 range`() {
        val mgr = ModelManager.getInstance()
        mgr.startDownload("/data/models")
        // Test negative clamp
        mgr.updateDownloadProgress(-0.5f)
        assertEquals(0f, mgr.downloadProgress, 0.001f)
        // Test within range
        mgr.updateDownloadProgress(0.7f)
        assertEquals(0.7f, mgr.downloadProgress, 0.001f)
        // Note: setting >= 1.0 transitions to LOADING state
    }

    @Test
    fun `download complete at 100 percent transitions to LOADING`() {
        val mgr = ModelManager.getInstance()
        mgr.startDownload("/data/models")
        mgr.updateDownloadProgress(1.0f)
        assertEquals(ModelState.LOADING, mgr.state)
    }

    @Test
    fun `full lifecycle IDLE to READY`() {
        val mgr = ModelManager.getInstance()
        assertEquals(ModelState.IDLE, mgr.state)
        mgr.startDownload("/data/models")
        assertEquals(ModelState.DOWNLOADING, mgr.state)
        mgr.updateDownloadProgress(1.0f)
        assertEquals(ModelState.LOADING, mgr.state)
        mgr.onModelLoaded()
        assertEquals(ModelState.READY, mgr.state)
        assertTrue(mgr.isReady())
    }

    @Test
    fun `error state sets message`() {
        val mgr = ModelManager.getInstance()
        mgr.startDownload("/data/models")
        mgr.onError("Network failure")
        assertEquals(ModelState.ERROR, mgr.state)
        assertEquals("Network failure", mgr.errorMessage)
    }

    @Test
    fun `can restart after error`() {
        val mgr = ModelManager.getInstance()
        mgr.startDownload("/data/models")
        mgr.onError("Timeout")
        assertTrue(mgr.startDownload("/data/models"))
        assertEquals(ModelState.DOWNLOADING, mgr.state)
        assertNull(mgr.errorMessage)
    }

    @Test
    fun `reset returns to IDLE`() {
        val mgr = ModelManager.getInstance()
        mgr.startDownload("/data/models")
        mgr.updateDownloadProgress(0.5f)
        mgr.reset()
        assertEquals(ModelState.IDLE, mgr.state)
        assertEquals(0f, mgr.downloadProgress, 0.001f)
        assertNull(mgr.modelPath)
    }

    @Test
    fun `config update rejected during download`() {
        val mgr = ModelManager.getInstance()
        mgr.startDownload("/data/models")
        try {
            mgr.updateConfig(ModelConfig(threads = 8))
            fail("Should throw IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("DOWNLOADING"))
        }
    }

    @Test
    fun `config update works when IDLE`() {
        val mgr = ModelManager.getInstance()
        val newConfig = ModelConfig(threads = 2, gpuLayers = 4, contextLength = 2048)
        mgr.updateConfig(newConfig)
        assertEquals(2, mgr.config.threads)
        assertEquals(4, mgr.config.gpuLayers)
        assertEquals(2048, mgr.config.contextLength)
    }

    @Test
    fun `config update works after error`() {
        val mgr = ModelManager.getInstance()
        mgr.startDownload("/data/models")
        mgr.onError("Failed")
        mgr.updateConfig(ModelConfig(threads = 1))
        assertEquals(1, mgr.config.threads)
    }
}
