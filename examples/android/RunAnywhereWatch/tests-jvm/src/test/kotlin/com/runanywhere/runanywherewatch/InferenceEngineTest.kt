package com.runanywhere.runanywherewatch

import org.junit.Test
import org.junit.Assert.*

/**
 * Pure JVM tests for InferenceEngine configuration and state management.
 * Tests the inference wrapper without requiring native llama.cpp bindings.
 */

data class InferenceConfig(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 256,
    val repeatPenalty: Float = 1.1f,
    val seed: Int = -1  // -1 = random
)

enum class InferenceState { UNLOADED, LOADING, READY, GENERATING, ERROR }

class InferenceEngine {
    var state: InferenceState = InferenceState.UNLOADED
        private set
    var config: InferenceConfig = InferenceConfig()
        private set
    var lastPrompt: String? = null
        private set
    var lastResponse: String? = null
        private set
    var tokensGenerated: Int = 0
        private set

    fun loadModel(modelPath: String): Boolean {
        if (state == InferenceState.GENERATING) return false
        state = InferenceState.LOADING
        // In real implementation, this would call llama.cpp JNI
        state = InferenceState.READY
        return true
    }

    fun updateConfig(config: InferenceConfig) {
        this.config = config
    }

    fun generate(prompt: String): String {
        if (state != InferenceState.READY) {
            throw IllegalStateException("Engine not ready: $state")
        }
        state = InferenceState.GENERATING
        lastPrompt = prompt
        // Simulated response for testing
        val response = "[Generated response for: ${prompt.take(50)}]"
        lastResponse = response
        tokensGenerated += response.split(" ").size
        state = InferenceState.READY
        return response
    }

    fun unload() {
        state = InferenceState.UNLOADED
        lastPrompt = null
        lastResponse = null
    }
}

class InferenceEngineTest {

    @Test
    fun `initial state is UNLOADED`() {
        val engine = InferenceEngine()
        assertEquals(InferenceState.UNLOADED, engine.state)
        assertNull(engine.lastPrompt)
        assertNull(engine.lastResponse)
        assertEquals(0, engine.tokensGenerated)
    }

    @Test
    fun `default config values`() {
        val engine = InferenceEngine()
        assertEquals(0.7f, engine.config.temperature, 0.001f)
        assertEquals(0.9f, engine.config.topP, 0.001f)
        assertEquals(256, engine.config.maxTokens)
        assertEquals(1.1f, engine.config.repeatPenalty, 0.001f)
        assertEquals(-1, engine.config.seed)
    }

    @Test
    fun `loadModel transitions to READY`() {
        val engine = InferenceEngine()
        assertTrue(engine.loadModel("/data/models/test.gguf"))
        assertEquals(InferenceState.READY, engine.state)
    }

    @Test
    fun `generate produces response`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        val response = engine.generate("What is AI?")
        assertNotNull(response)
        assertTrue(response.isNotEmpty())
        assertEquals("What is AI?", engine.lastPrompt)
    }

    @Test
    fun `generate fails when not ready`() {
        val engine = InferenceEngine()
        try {
            engine.generate("test")
            fail("Should throw")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("UNLOADED"))
        }
    }

    @Test
    fun `tokens counted across generations`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("Hello")
        val first = engine.tokensGenerated
        assertTrue(first > 0)
        engine.generate("World")
        assertTrue(engine.tokensGenerated > first)
    }

    @Test
    fun `config update applies`() {
        val engine = InferenceEngine()
        engine.updateConfig(InferenceConfig(temperature = 0.3f, maxTokens = 128))
        assertEquals(0.3f, engine.config.temperature, 0.001f)
        assertEquals(128, engine.config.maxTokens)
    }

    @Test
    fun `unload resets state`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("test")
        engine.unload()
        assertEquals(InferenceState.UNLOADED, engine.state)
        assertNull(engine.lastPrompt)
        assertNull(engine.lastResponse)
    }

    @Test
    fun `can reload after unload`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.unload()
        assertTrue(engine.loadModel("/data/models/other.gguf"))
        assertEquals(InferenceState.READY, engine.state)
    }

    @Test
    fun `state returns to READY after generate`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("test prompt")
        assertEquals(InferenceState.READY, engine.state)
    }
}
