package com.runanywhere.runanywherewatch

import org.junit.Test
import org.junit.Assert.*

/**
 * Extended tests for ModelManager with download URL management and checksum verification.
 * These tests add new functionality to the existing ModelManager class.
 */

// Extension interface for checksum verification
interface ChecksumVerifier {
    fun verifyChecksum(filePath: String, expectedChecksum: String): Boolean
}

// Simple implementation for testing
class SimpleChecksumVerifier : ChecksumVerifier {
    override fun verifyChecksum(filePath: String, expectedChecksum: String): Boolean {
        return expectedChecksum.isNotEmpty()
    }
}

// Helper classes for managing extended state
class DownloadUrlManager {
    companion object {
        private var url: String? = null
        fun setUrl(url: String?) { this.url = url }
        fun getUrl(): String? = url
    }
}

class ExpectedChecksumManager {
    companion object {
        private var checksum: String? = null
        fun setChecksum(checksum: String?) { this.checksum = checksum }
        fun getChecksum(): String? = checksum
    }
}

class ChecksumVerifierManager {
    companion object {
        private var verifier: ChecksumVerifier? = null
        fun setVerifier(verifier: ChecksumVerifier?) { this.verifier = verifier }
        fun getVerifier(): ChecksumVerifier? = verifier
    }
}

// Extension functions to add download URL support to ModelManager
fun ModelManager.setDownloadUrl(url: String?) {
    DownloadUrlManager.setUrl(url)
}

fun ModelManager.getDownloadUrl(): String? {
    return DownloadUrlManager.getUrl()
}

fun ModelManager.setExpectedChecksum(checksum: String?) {
    ExpectedChecksumManager.setChecksum(checksum)
}

fun ModelManager.getExpectedChecksum(): String? {
    return ExpectedChecksumManager.getChecksum()
}

fun ModelManager.setChecksumVerifier(verifier: ChecksumVerifier?) {
    ChecksumVerifierManager.setVerifier(verifier)
}

fun ModelManager.verifyChecksum(): Boolean {
    val verifier = ChecksumVerifierManager.getVerifier()
    val checksum = ExpectedChecksumManager.getChecksum()
    val path = modelPath
    if (verifier != null && checksum != null && path != null) {
        return verifier.verifyChecksum(path, checksum)
    }
    return false
}

class ModelManagerExtendedFeaturesTest {

    @Test
    fun `download URL can be set and retrieved`() {
        val mgr = ModelManager.getInstance()
        mgr.setDownloadUrl("https://huggingface.co/models/qwen3.5-0.8b.gguf")
        assertEquals("https://huggingface.co/models/qwen3.5-0.8b.gguf", mgr.getDownloadUrl())
    }

    @Test
    fun `download URL is null when not set`() {
        val mgr = ModelManager.getInstance()
        assertNull(mgr.getDownloadUrl())
    }

    @Test
    fun `expected checksum can be set and retrieved`() {
        val mgr = ModelManager.getInstance()
        mgr.setExpectedChecksum("sha256:abc123def456")
        assertEquals("sha256:abc123def456", mgr.getExpectedChecksum())
    }

    @Test
    fun `expected checksum is null when not set`() {
        val mgr = ModelManager.getInstance()
        assertNull(mgr.getExpectedChecksum())
    }

    @Test
    fun `checksum verifier can be set and retrieved`() {
        val mgr = ModelManager.getInstance()
        val verifier = SimpleChecksumVerifier()
        mgr.setChecksumVerifier(verifier)
        assertEquals(verifier, ChecksumVerifierManager.getVerifier())
    }

    @Test
    fun `checksum verification succeeds when verifier returns true`() {
        val mgr = ModelManager.getInstance()
        val config = ModelConfig()
        mgr.updateConfig(config)
        mgr.setDownloadUrl("https://example.com/model.gguf")
        mgr.setExpectedChecksum("sha256:test123")
        mgr.setChecksumVerifier(SimpleChecksumVerifier())
        mgr.startDownload("/data/models")
        mgr.updateDownloadProgress(1.0f)
        assertTrue(mgr.verifyChecksum())
    }

    @Test
    fun `checksum verification fails when verifier returns false`() {
        val mgr = ModelManager.getInstance()
        val config = ModelConfig()
        mgr.updateConfig(config)
        mgr.setDownloadUrl("https://example.com/model.gguf")
        mgr.setExpectedChecksum("sha256:test123")
        mgr.setChecksumVerifier(object : ChecksumVerifier {
            override fun verifyChecksum(filePath: String, expectedChecksum: String): Boolean = false
        })
        mgr.startDownload("/data/models")
        mgr.updateDownloadProgress(1.0f)
        assertFalse(mgr.verifyChecksum())
    }

    @Test
    fun `checksum verification returns false when no verifier set`() {
        val mgr = ModelManager.getInstance()
        val config = ModelConfig()
        mgr.updateConfig(config)
        mgr.setDownloadUrl("https://example.com/model.gguf")
        mgr.setExpectedChecksum("sha256:test123")
        assertFalse(mgr.verifyChecksum())
    }

    @Test
    fun `checksum verification returns false when no path set`() {
        val mgr = ModelManager.getInstance()
        mgr.setDownloadUrl("https://example.com/model.gguf")
        mgr.setExpectedChecksum("sha256:test123")
        mgr.setChecksumVerifier(SimpleChecksumVerifier())
        assertFalse(mgr.verifyChecksum())
    }

    @Test
    fun `checksum verification returns false when no checksum set`() {
        val mgr = ModelManager.getInstance()
        val config = ModelConfig()
        mgr.updateConfig(config)
        mgr.setDownloadUrl("https://example.com/model.gguf")
        mgr.setChecksumVerifier(SimpleChecksumVerifier())
        mgr.startDownload("/data/models")
        assertFalse(mgr.verifyChecksum())
    }

    @Test
    fun `can change checksum verifier`() {
        val mgr = ModelManager.getInstance()
        val verifier1 = SimpleChecksumVerifier()
        val verifier2 = object : ChecksumVerifier {
            override fun verifyChecksum(filePath: String, expectedChecksum: String): Boolean = false
        }
        mgr.setChecksumVerifier(verifier1)
        assertEquals(verifier1, ChecksumVerifierManager.getVerifier())
        mgr.setChecksumVerifier(verifier2)
        assertEquals(verifier2, ChecksumVerifierManager.getVerifier())
    }

    @Test
    fun `download URL and checksum can be cleared`() {
        val mgr = ModelManager.getInstance()
        mgr.setDownloadUrl("https://example.com/model.gguf")
        mgr.setExpectedChecksum("sha256:test123")
        assertEquals("https://example.com/model.gguf", mgr.getDownloadUrl())
        assertEquals("sha256:test123", mgr.getExpectedChecksum())
        // Clear by setting null/empty
        DownloadUrlManager.setUrl(null)
        ExpectedChecksumManager.setChecksum("")
        assertNull(mgr.getDownloadUrl())
        assertEquals("", mgr.getExpectedChecksum())
    }

    @Test
    fun `checksum verifier can be cleared`() {
        val mgr = ModelManager.getInstance()
        val verifier = SimpleChecksumVerifier()
        mgr.setChecksumVerifier(verifier)
        assertEquals(verifier, ChecksumVerifierManager.getVerifier())
        ChecksumVerifierManager.setVerifier(null)
        assertNull(ChecksumVerifierManager.getVerifier())
    }

    @Test
    fun `full lifecycle with download URL and checksum`() {
        val mgr = ModelManager.getInstance()
        val config = ModelConfig()
        mgr.updateConfig(config)
        mgr.setDownloadUrl("https://example.com/model.gguf")
        mgr.setExpectedChecksum("sha256:test123")
        mgr.setChecksumVerifier(SimpleChecksumVerifier())
        assertTrue(mgr.startDownload("/data/models"))
        assertEquals(ModelState.DOWNLOADING, mgr.state)
        mgr.updateDownloadProgress(0.5f)
        assertEquals(0.5f, mgr.downloadProgress, 0.001f)
        mgr.updateDownloadProgress(1.0f)
        assertEquals(ModelState.LOADING, mgr.state)
        assertTrue(mgr.verifyChecksum())
        mgr.onModelLoaded()
        assertEquals(ModelState.READY, mgr.state)
        assertTrue(mgr.isReady())
    }

    @Test
    fun `config update with download URL and checksum`() {
        val mgr = ModelManager.getInstance()
        val newConfig = ModelConfig()
        mgr.updateConfig(newConfig)
        mgr.setDownloadUrl("https://example.com/test.gguf")
        mgr.setExpectedChecksum("sha256:test")
        assertEquals("https://example.com/test.gguf", mgr.getDownloadUrl())
        assertEquals("sha256:test", mgr.getExpectedChecksum())
    }

    @Test
    fun `checksum verification works after error and restart`() {
        val mgr = ModelManager.getInstance()
        val config = ModelConfig()
        mgr.updateConfig(config)
        mgr.setDownloadUrl("https://example.com/model.gguf")
        mgr.setExpectedChecksum("sha256:test123")
        mgr.setChecksumVerifier(SimpleChecksumVerifier())
        mgr.startDownload("/data/models")
        mgr.onError("Failed")
        assertTrue(mgr.startDownload("/data/models"))
        assertEquals(ModelState.DOWNLOADING, mgr.state)
        mgr.updateDownloadProgress(1.0f)
        assertTrue(mgr.verifyChecksum())
    }

    @Test
    fun `checksum verification works when modelPath is set`() {
        val mgr = ModelManager.getInstance()
        val config = ModelConfig()
        mgr.updateConfig(config)
        mgr.setDownloadUrl("https://example.com/model.gguf")
        mgr.setExpectedChecksum("sha256:test123")
        mgr.setChecksumVerifier(SimpleChecksumVerifier())
        mgr.startDownload("/data/models")
        assertTrue(mgr.verifyChecksum())
    }

    @Test
    fun `multiple checksum verifiers can be tested`() {
        val mgr = ModelManager.getInstance()
        val successVerifier = SimpleChecksumVerifier()
        val failVerifier = object : ChecksumVerifier {
            override fun verifyChecksum(filePath: String, expectedChecksum: String): Boolean = false
        }
        mgr.setChecksumVerifier(successVerifier)
        assertTrue(mgr.verifyChecksum())
        mgr.setChecksumVerifier(failVerifier)
        assertFalse(mgr.verifyChecksum())
    }

    @Test
    fun `download URL can be updated multiple times`() {
        val mgr = ModelManager.getInstance()
        mgr.setDownloadUrl("https://example.com/model1.gguf")
        assertEquals("https://example.com/model1.gguf", mgr.getDownloadUrl())
        mgr.setDownloadUrl("https://example.com/model2.gguf")
        assertEquals("https://example.com/model2.gguf", mgr.getDownloadUrl())
        mgr.setDownloadUrl("https://example.com/model3.gguf")
        assertEquals("https://example.com/model3.gguf", mgr.getDownloadUrl())
    }

    @Test
    fun `expected checksum can be updated multiple times`() {
        val mgr = ModelManager.getInstance()
        mgr.setExpectedChecksum("sha256:checksum1")
        assertEquals("sha256:checksum1", mgr.getExpectedChecksum())
        mgr.setExpectedChecksum("sha256:checksum2")
        assertEquals("sha256:checksum2", mgr.getExpectedChecksum())
        mgr.setExpectedChecksum("sha256:checksum3")
        assertEquals("sha256:checksum3", mgr.getExpectedChecksum())
    }
}
