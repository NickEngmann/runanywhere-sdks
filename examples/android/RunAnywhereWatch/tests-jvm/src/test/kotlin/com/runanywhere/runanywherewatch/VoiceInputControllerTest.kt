package com.runanywhere.runanywherewatch

import org.junit.Test
import org.junit.Before
import org.junit.Assert.*

/**
 * Pure JVM tests for voice input (STT), LLM response display, and conversation
 * management — no Android SDK dependencies.
 */

// --- Voice Input State Machine ---

enum class VoiceInputState { IDLE, LISTENING, PROCESSING, DONE, ERROR }

class VoiceInputController {
    var state: VoiceInputState = VoiceInputState.IDLE
        private set
    var transcript: String? = null
        private set
    var errorMessage: String? = null
        private set
    var listeningDurationMs: Long = 0
        private set
    private var listenStartTime: Long = 0

    fun startListening(): Boolean {
        if (state != VoiceInputState.IDLE && state != VoiceInputState.ERROR) return false
        state = VoiceInputState.LISTENING
        transcript = null
        errorMessage = null
        listenStartTime = System.currentTimeMillis()
        return true
    }

    fun stopListening(): Boolean {
        if (state != VoiceInputState.LISTENING) return false
        listeningDurationMs = System.currentTimeMillis() - listenStartTime
        state = VoiceInputState.PROCESSING
        return true
    }

    fun onTranscriptionResult(text: String) {
        if (state != VoiceInputState.PROCESSING) return
        transcript = text.trim()
        state = VoiceInputState.DONE
    }

    fun onError(message: String) {
        state = VoiceInputState.ERROR
        errorMessage = message
    }

    fun reset() {
        state = VoiceInputState.IDLE
        transcript = null
        errorMessage = null
        listeningDurationMs = 0
    }

    fun isActive(): Boolean = state == VoiceInputState.LISTENING || state == VoiceInputState.PROCESSING
}

// --- STT Processor ---

data class STTConfig(
    val language: String = "en",
    val sampleRate: Int = 16000,
    val maxDurationSec: Int = 30,
    val silenceThresholdMs: Int = 2000
)

class STTProcessor(val config: STTConfig = STTConfig()) {
    private val audioBuffer = mutableListOf<ByteArray>()
    var totalBytesBuffered: Long = 0
        private set

    fun addAudioChunk(chunk: ByteArray) {
        audioBuffer.add(chunk)
        totalBytesBuffered += chunk.size
    }

    fun clearBuffer() {
        audioBuffer.clear()
        totalBytesBuffered = 0
    }

    fun getChunkCount(): Int = audioBuffer.size

    fun estimateDurationSec(): Double {
        // 16kHz, 16-bit mono = 32000 bytes/sec
        val bytesPerSec = config.sampleRate * 2
        return totalBytesBuffered.toDouble() / bytesPerSec
    }

    fun isBufferFull(): Boolean = estimateDurationSec() >= config.maxDurationSec

    fun parseTranscription(raw: String): TranscriptionResult {
        val cleaned = raw.trim()
        val confidence = if (cleaned.length > 5) 0.85 else 0.5
        val language = if (cleaned.any { it in '\u00C0'..'\u00FF' || it in '\u0100'..'\u017F' }) "es" else config.language
        return TranscriptionResult(cleaned, confidence, language)
    }
}

data class TranscriptionResult(
    val text: String,
    val confidence: Double,
    val detectedLanguage: String
)

// --- LLM Response Display ---

class LLMResponseDisplay {
    private val chunks = mutableListOf<String>()
    var isStreaming: Boolean = false
        private set
    var isComplete: Boolean = false
        private set

    fun startStreaming() {
        chunks.clear()
        isStreaming = true
        isComplete = false
    }

    fun appendChunk(text: String) {
        if (!isStreaming) return
        chunks.add(text)
    }

    fun finishStreaming() {
        isStreaming = false
        isComplete = true
    }

    fun getFullText(): String = chunks.joinToString("")

    fun getChunkCount(): Int = chunks.size

    fun getWordCount(): Int = getFullText().split("\\s+".toRegex()).filter { it.isNotBlank() }.size

    fun reset() {
        chunks.clear()
        isStreaming = false
        isComplete = false
    }
}

// --- Conversation Manager ---

data class ConversationMessage(
    val role: String,  // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ConversationManager(
    private val maxMessages: Int = 20,
    private val tokenBudget: Int = 4096,
    private val systemPrompt: String = "You are a helpful watch assistant."
) {
    private val messages = mutableListOf<ConversationMessage>()

    fun addMessage(role: String, content: String) {
        messages.add(ConversationMessage(role, content))
        trimToFit()
    }

    fun getMessages(): List<ConversationMessage> = messages.toList()

    fun getMessageCount(): Int = messages.size

    fun clear() {
        messages.clear()
    }

    fun estimateTokens(): Int {
        // Rough estimate: ~4 chars per token
        val systemTokens = systemPrompt.length / 4
        val messageTokens = messages.sumOf { (it.role.length + it.content.length) / 4 }
        return systemTokens + messageTokens
    }

    fun getLastUserMessage(): String? =
        messages.lastOrNull { it.role == "user" }?.content

    fun getLastAssistantMessage(): String? =
        messages.lastOrNull { it.role == "assistant" }?.content

    private fun trimToFit() {
        // Trim oldest messages if over count limit
        while (messages.size > maxMessages) {
            messages.removeAt(0)
        }
        // Trim oldest messages if over token budget
        while (estimateTokens() > tokenBudget && messages.size > 1) {
            messages.removeAt(0)
        }
    }
}

// ========================= TESTS =========================

class VoiceInputControllerTest {

    private lateinit var controller: VoiceInputController

    @Before
    fun setUp() {
        controller = VoiceInputController()
    }

    @Test
    fun `initial state is IDLE`() {
        assertEquals(VoiceInputState.IDLE, controller.state)
        assertNull(controller.transcript)
        assertNull(controller.errorMessage)
        assertFalse(controller.isActive())
    }

    @Test
    fun `startListening transitions to LISTENING`() {
        assertTrue(controller.startListening())
        assertEquals(VoiceInputState.LISTENING, controller.state)
        assertTrue(controller.isActive())
    }

    @Test
    fun `startListening fails when already listening`() {
        controller.startListening()
        assertFalse(controller.startListening())
    }

    @Test
    fun `stopListening transitions to PROCESSING`() {
        controller.startListening()
        assertTrue(controller.stopListening())
        assertEquals(VoiceInputState.PROCESSING, controller.state)
        assertTrue(controller.isActive())
    }

    @Test
    fun `stopListening fails when not listening`() {
        assertFalse(controller.stopListening())
    }

    @Test
    fun `full lifecycle IDLE to DONE`() {
        controller.startListening()
        controller.stopListening()
        controller.onTranscriptionResult("Hello watch")
        assertEquals(VoiceInputState.DONE, controller.state)
        assertEquals("Hello watch", controller.transcript)
        assertFalse(controller.isActive())
    }

    @Test
    fun `error state sets message`() {
        controller.startListening()
        controller.onError("Microphone unavailable")
        assertEquals(VoiceInputState.ERROR, controller.state)
        assertEquals("Microphone unavailable", controller.errorMessage)
    }

    @Test
    fun `can restart after error`() {
        controller.startListening()
        controller.onError("Timeout")
        assertTrue(controller.startListening())
        assertEquals(VoiceInputState.LISTENING, controller.state)
        assertNull(controller.errorMessage)
    }

    @Test
    fun `reset returns to IDLE`() {
        controller.startListening()
        controller.stopListening()
        controller.onTranscriptionResult("test")
        controller.reset()
        assertEquals(VoiceInputState.IDLE, controller.state)
        assertNull(controller.transcript)
        assertEquals(0L, controller.listeningDurationMs)
    }

    @Test
    fun `transcript is trimmed`() {
        controller.startListening()
        controller.stopListening()
        controller.onTranscriptionResult("  hello world  ")
        assertEquals("hello world", controller.transcript)
    }
}

class STTProcessorTest {

    private lateinit var processor: STTProcessor

    @Before
    fun setUp() {
        processor = STTProcessor()
    }

    @Test
    fun `default config values`() {
        assertEquals("en", processor.config.language)
        assertEquals(16000, processor.config.sampleRate)
        assertEquals(30, processor.config.maxDurationSec)
    }

    @Test
    fun `audio buffer accumulates bytes`() {
        processor.addAudioChunk(ByteArray(1000))
        processor.addAudioChunk(ByteArray(2000))
        assertEquals(3000L, processor.totalBytesBuffered)
        assertEquals(2, processor.getChunkCount())
    }

    @Test
    fun `clear buffer resets state`() {
        processor.addAudioChunk(ByteArray(5000))
        processor.clearBuffer()
        assertEquals(0L, processor.totalBytesBuffered)
        assertEquals(0, processor.getChunkCount())
    }

    @Test
    fun `duration estimation is correct`() {
        // 16kHz 16-bit mono = 32000 bytes/sec
        processor.addAudioChunk(ByteArray(32000))
        assertEquals(1.0, processor.estimateDurationSec(), 0.01)
    }

    @Test
    fun `buffer full detection`() {
        // 30 seconds at 32000 bytes/sec = 960000 bytes
        processor.addAudioChunk(ByteArray(960000))
        assertTrue(processor.isBufferFull())
    }

    @Test
    fun `buffer not full for short audio`() {
        processor.addAudioChunk(ByteArray(16000))
        assertFalse(processor.isBufferFull())
    }

    @Test
    fun `parse transcription trims whitespace`() {
        val result = processor.parseTranscription("  hello world  ")
        assertEquals("hello world", result.text)
    }

    @Test
    fun `parse transcription detects English`() {
        val result = processor.parseTranscription("what time is it")
        assertEquals("en", result.detectedLanguage)
    }

    @Test
    fun `parse transcription confidence varies with length`() {
        val short = processor.parseTranscription("hi")
        val long = processor.parseTranscription("what is the weather today")
        assertTrue(long.confidence > short.confidence)
    }

    @Test
    fun `custom config applies`() {
        val custom = STTProcessor(STTConfig(language = "es", sampleRate = 8000, maxDurationSec = 10))
        assertEquals("es", custom.config.language)
        assertEquals(8000, custom.config.sampleRate)
    }
}

class LLMResponseDisplayTest {

    private lateinit var display: LLMResponseDisplay

    @Before
    fun setUp() {
        display = LLMResponseDisplay()
    }

    @Test
    fun `initial state is not streaming`() {
        assertFalse(display.isStreaming)
        assertFalse(display.isComplete)
        assertEquals("", display.getFullText())
    }

    @Test
    fun `start streaming sets flag`() {
        display.startStreaming()
        assertTrue(display.isStreaming)
        assertFalse(display.isComplete)
    }

    @Test
    fun `append chunks during streaming`() {
        display.startStreaming()
        display.appendChunk("Hello ")
        display.appendChunk("world!")
        assertEquals("Hello world!", display.getFullText())
        assertEquals(2, display.getChunkCount())
    }

    @Test
    fun `append chunk ignored when not streaming`() {
        display.appendChunk("ignored")
        assertEquals(0, display.getChunkCount())
    }

    @Test
    fun `finish streaming sets complete`() {
        display.startStreaming()
        display.appendChunk("Done")
        display.finishStreaming()
        assertFalse(display.isStreaming)
        assertTrue(display.isComplete)
        assertEquals("Done", display.getFullText())
    }

    @Test
    fun `word count is correct`() {
        display.startStreaming()
        display.appendChunk("The time is ")
        display.appendChunk("3:00 PM")
        display.finishStreaming()
        assertEquals(5, display.getWordCount())
    }

    @Test
    fun `reset clears everything`() {
        display.startStreaming()
        display.appendChunk("text")
        display.finishStreaming()
        display.reset()
        assertFalse(display.isStreaming)
        assertFalse(display.isComplete)
        assertEquals("", display.getFullText())
    }
}

class ConversationManagerTest {

    private lateinit var manager: ConversationManager

    @Before
    fun setUp() {
        manager = ConversationManager(maxMessages = 5, tokenBudget = 500)
    }

    @Test
    fun `initial state is empty`() {
        assertEquals(0, manager.getMessageCount())
        assertNull(manager.getLastUserMessage())
        assertNull(manager.getLastAssistantMessage())
    }

    @Test
    fun `add messages`() {
        manager.addMessage("user", "What time is it?")
        manager.addMessage("assistant", "It's 3:00 PM.")
        assertEquals(2, manager.getMessageCount())
    }

    @Test
    fun `get last user message`() {
        manager.addMessage("user", "first")
        manager.addMessage("assistant", "reply")
        manager.addMessage("user", "second")
        assertEquals("second", manager.getLastUserMessage())
    }

    @Test
    fun `get last assistant message`() {
        manager.addMessage("user", "question")
        manager.addMessage("assistant", "answer one")
        manager.addMessage("assistant", "answer two")
        assertEquals("answer two", manager.getLastAssistantMessage())
    }

    @Test
    fun `trim to max messages`() {
        for (i in 1..10) {
            manager.addMessage("user", "msg $i")
        }
        assertTrue(manager.getMessageCount() <= 5)
    }

    @Test
    fun `token estimation works`() {
        manager.addMessage("user", "a".repeat(100))
        val tokens = manager.estimateTokens()
        assertTrue(tokens > 0)
    }

    @Test
    fun `clear removes all messages`() {
        manager.addMessage("user", "test")
        manager.addMessage("assistant", "reply")
        manager.clear()
        assertEquals(0, manager.getMessageCount())
    }

    @Test
    fun `messages returned as copy`() {
        manager.addMessage("user", "hello")
        val msgs = manager.getMessages()
        assertEquals(1, msgs.size)
        assertEquals("hello", msgs[0].content)
        assertEquals("user", msgs[0].role)
    }

    @Test
    fun `token budget trimming`() {
        // Each message ~25 chars = ~6 tokens. System prompt ~10 tokens.
        // With budget 500, we can fit many messages before trimming.
        // Add enough to exceed budget:
        for (i in 1..50) {
            manager.addMessage("user", "a".repeat(200))
        }
        // Should have trimmed to fit within token budget
        assertTrue(manager.estimateTokens() <= 500 || manager.getMessageCount() <= 1)
    }
}
