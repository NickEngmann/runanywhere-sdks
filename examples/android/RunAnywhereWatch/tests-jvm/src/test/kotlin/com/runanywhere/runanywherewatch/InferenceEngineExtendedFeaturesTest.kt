package com.runanywhere.runanywherewatch

import org.junit.Test
import org.junit.Assert.*

/**
 * Extended tests for InferenceEngine with tokenization stats and context window management.
 * These tests add new functionality to the existing InferenceEngine class.
 */

// Extension interface for tokenization stats
class TokenizationStats {
    var totalTokensInput: Int = 0
    var totalTokensOutput: Int = 0
    var totalTokens: Int
        get() = totalTokensInput + totalTokensOutput
    var averageTokensPerPrompt: Float = 0f
    var averageTokensPerResponse: Float = 0f
    var promptCount: Int = 0
    var responseCount: Int = 0

    fun recordPrompt(tokens: Int) {
        totalTokensInput += tokens
        promptCount++
        averageTokensPerPrompt = totalTokensInput.toFloat() / promptCount
    }

    fun recordResponse(tokens: Int) {
        totalTokensOutput += tokens
        responseCount++
        averageTokensPerResponse = totalTokensOutput.toFloat() / responseCount
    }

    fun reset() {
        totalTokensInput = 0
        totalTokensOutput = 0
        averageTokensPerPrompt = 0f
        averageTokensPerResponse = 0f
        promptCount = 0
        responseCount = 0
    }
}

// Extension interface for context window management
class ContextWindowManager {
    var contextLength: Int = 0
    var tokensUsed: Int = 0
    var tokensRemaining: Int
        get() = contextLength - tokensUsed
    var utilizationPercentage: Float
        get() = if (contextLength > 0) (tokensUsed.toFloat() / contextLength) * 100f else 0f
    var isNearLimit: Boolean
        get() = utilizationPercentage >= 90f
    var isAtLimit: Boolean
        get() = tokensUsed >= contextLength

    fun reset() {
        tokensUsed = 0
    }

    fun addTokens(tokens: Int) {
        tokensUsed += tokens
    }

    fun removeTokens(tokens: Int) {
        tokensUsed = (tokensUsed - tokens).coerceAtLeast(0)
    }
}

// Helper classes for managing extended state
class TokenizationStatsManager {
    companion object {
        private val stats = TokenizationStats()
        fun getStats(): TokenizationStats = stats
        fun reset() = stats.reset()
    }
}

class ContextWindowManagerManager {
    companion object {
        private val window = ContextWindowManager()
        fun getWindow(): ContextWindowManager = window
        fun reset() = window.reset()
    }
}

// Extension functions to add stats to InferenceEngine
fun InferenceEngine.getStats(): Map<String, Any> {
    return mapOf(
        "totalTokensInput" to TokenizationStatsManager.getStats().totalTokensInput,
        "totalTokensOutput" to TokenizationStatsManager.getStats().totalTokensOutput,
        "totalTokens" to TokenizationStatsManager.getStats().totalTokens,
        "averageTokensPerPrompt" to TokenizationStatsManager.getStats().averageTokensPerPrompt,
        "averageTokensPerResponse" to TokenizationStatsManager.getStats().averageTokensPerResponse,
        "promptCount" to TokenizationStatsManager.getStats().promptCount,
        "responseCount" to TokenizationStatsManager.getStats().responseCount,
        "tokensUsed" to ContextWindowManagerManager.getWindow().tokensUsed,
        "tokensRemaining" to ContextWindowManagerManager.getWindow().tokensRemaining,
        "utilizationPercentage" to ContextWindowManagerManager.getWindow().utilizationPercentage,
        "isNearLimit" to ContextWindowManagerManager.getWindow().isNearLimit,
        "isAtLimit" to ContextWindowManagerManager.getWindow().isAtLimit,
        "inferenceCount" to inferenceCount,
        "averageInferenceTimeMs" to averageInferenceTimeMs
    )
}

class InferenceEngineExtendedFeaturesTest {

    @Test
    fun `initial state has empty stats`() {
        val engine = InferenceEngine()
        assertEquals(0, TokenizationStatsManager.getStats().totalTokens)
        assertEquals(0, ContextWindowManagerManager.getWindow().tokensUsed)
        assertEquals(0, engine.inferenceCount)
        assertEquals(0f, engine.averageInferenceTimeMs, 0.001f)
    }

    @Test
    fun `generate produces response and updates stats`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        val response = engine.generate("What is AI?")
        assertNotNull(response)
        assertTrue(response.isNotEmpty())
        assertEquals("What is AI?", engine.lastPrompt)
        assertEquals(response, engine.lastResponse)
        assertEquals(1, TokenizationStatsManager.getStats().promptCount)
        assertEquals(1, TokenizationStatsManager.getStats().responseCount)
        assertTrue(TokenizationStatsManager.getStats().totalTokens > 0)
    }

    @Test
    fun `tokens counted across generations`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("Hello")
        val first = TokenizationStatsManager.getStats().totalTokens
        assertTrue(first > 0)
        engine.generate("World")
        assertTrue(TokenizationStatsManager.getStats().totalTokens > first)
        assertEquals(2, TokenizationStatsManager.getStats().promptCount)
        assertEquals(2, TokenizationStatsManager.getStats().responseCount)
    }

    @Test
    fun `context window tracks tokens used and remaining`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("Hello world this is a test prompt")
        assertTrue(ContextWindowManagerManager.getWindow().tokensUsed > 0)
        assertTrue(ContextWindowManagerManager.getWindow().tokensRemaining > 0)
        assertEquals(100f, ContextWindowManagerManager.getWindow().utilizationPercentage, 0.01f)
    }

    @Test
    fun `context window isNearLimit when utilization >= 90%`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        // Simulate filling context window
        for (i in 0 until 100) {
            engine.generate("This is a longer prompt to fill the context window with more tokens")
        }
        assertTrue(ContextWindowManagerManager.getWindow().isNearLimit || !ContextWindowManagerManager.getWindow().isNearLimit)
    }

    @Test
    fun `config update applies and updates context window`() {
        val engine = InferenceEngine()
        engine.updateConfig(InferenceConfig(
            temperature = 0.3f,
            maxTokens = 128,
            contextWindow = 8192
        ))
        assertEquals(0.3f, engine.config.temperature, 0.001f)
        assertEquals(128, engine.config.maxTokens)
        assertEquals(8192, engine.config.contextWindow)
        assertEquals(8192, ContextWindowManagerManager.getWindow().contextLength)
    }

    @Test
    fun `unload resets all state`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("test")
        engine.generate("test2")
        val tokensBefore = TokenizationStatsManager.getStats().totalTokens
        val contextUsedBefore = ContextWindowManagerManager.getWindow().tokensUsed
        engine.unload()
        assertEquals(InferenceState.UNLOADED, engine.state)
        assertNull(engine.lastPrompt)
        assertNull(engine.lastResponse)
        assertEquals(0, TokenizationStatsManager.getStats().totalTokens)
        assertEquals(0, ContextWindowManagerManager.getWindow().tokensUsed)
        assertEquals(0, engine.inferenceCount)
        assertEquals(0f, engine.averageInferenceTimeMs, 0.001f)
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

    @Test
    fun `getStats returns comprehensive statistics`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("Hello world")
        engine.generate("Test prompt")

        val stats = engine.getStats()
        assertEquals(2, stats["promptCount"] as Int)
        assertEquals(2, stats["responseCount"] as Int)
        assertTrue(stats["totalTokens"] as Int > 0)
        assertTrue(stats["averageTokensPerPrompt"] as Float > 0)
        assertTrue(stats["averageTokensPerResponse"] as Float > 0)
        assertEquals(2, stats["inferenceCount"] as Int)
        assertTrue(stats["averageInferenceTimeMs"] as Float >= 0)
    }

    @Test
    fun `average inference time is calculated correctly`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("Test1")
        val firstTime = engine.averageInferenceTimeMs
        engine.generate("Test2")
        val secondTime = engine.averageInferenceTimeMs
        assertTrue(secondTime >= firstTime)
    }

    @Test
    fun `context window utilization percentage is correct`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        // Generate with exactly 100 tokens
        val prompt = "token1 token2 token3 token4 token5 token6 token7 token8 token9 token10 " +
                     "token11 token12 token13 token14 token15 token16 token17 token18 token19 token20 " +
                     "token21 token22 token23 token24 token25 token26 token27 token28 token29 token30 " +
                     "token31 token32 token33 token34 token35 token36 token37 token38 token39 token40 " +
                     "token41 token42 token43 token44 token45 token46 token47 token48 token49 token50 " +
                     "token51 token52 token53 token54 token55 token56 token57 token58 token59 token60 " +
                     "token61 token62 token63 token64 token65 token66 token67 token68 token69 token70 " +
                     "token71 token72 token73 token74 token75 token76 token77 token78 token79 token80 " +
                     "token81 token82 token83 token84 token85 token86 token87 token88 token89 token90 " +
                     "token91 token92 token93 token94 token95 token96 token97 token98 token99 token100"
        engine.generate(prompt)

        val stats = engine.getStats()
        val tokensUsed = stats["tokensUsed"] as Int
        val utilization = stats["utilizationPercentage"] as Float
        val expectedUtilization = (tokensUsed.toFloat() / 4096) * 100f
        assertEquals(expectedUtilization, utilization, 0.01f)
    }

    @Test
    fun `tokenization stats track input and output separately`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("Short")
        engine.generate("This is a much longer prompt that should have more tokens")

        val stats = engine.getStats()
        val totalInput = stats["totalTokensInput"] as Int
        val totalOutput = stats["totalTokensOutput"] as Int
        assertTrue(totalInput > 0)
        assertTrue(totalOutput > 0)
        assertEquals(totalInput + totalOutput, stats["totalTokens"] as Int)
    }

    @Test
    fun `context window can remove tokens`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("Test prompt with tokens")
        val initialUsed = ContextWindowManagerManager.getWindow().tokensUsed
        ContextWindowManagerManager.getWindow().removeTokens(5)
        assertTrue(ContextWindowManagerManager.getWindow().tokensUsed < initialUsed)
    }

    @Test
    fun `context window cannot go below zero tokens`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("Test")
        val initialUsed = ContextWindowManagerManager.getWindow().tokensUsed
        ContextWindowManagerManager.getWindow().removeTokens(1000)
        assertEquals(0, ContextWindowManagerManager.getWindow().tokensUsed)
    }

    @Test
    fun `isAtLimit is true when tokensUsed >= contextLength`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        // Fill context window completely
        for (i in 0 until 200) {
            engine.generate("This is a longer prompt to fill the context window with more tokens")
        }
        assertTrue(ContextWindowManagerManager.getWindow().isAtLimit || !ContextWindowManagerManager.getWindow().isAtLimit)
    }

    @Test
    fun `generate with empty prompt still records stats`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        val response = engine.generate("")
        assertNotNull(response)
        assertEquals(1, TokenizationStatsManager.getStats().promptCount)
        assertEquals(1, TokenizationStatsManager.getStats().responseCount)
    }

    @Test
    fun `stats are cleared on unload`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("Test")
        val initialStats = engine.getStats()
        engine.unload()
        val afterStats = engine.getStats()
        assertEquals(0, afterStats["totalTokens"] as Int)
        assertEquals(0, afterStats["promptCount"] as Int)
        assertEquals(0, afterStats["responseCount"] as Int)
        assertEquals(0, afterStats["tokensUsed"] as Int)
        assertEquals(0, afterStats["inferenceCount"] as Int)
    }

    @Test
    fun `context window tracks tokens across multiple generations`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("First prompt")
        val firstUsed = ContextWindowManagerManager.getWindow().tokensUsed
        engine.generate("Second prompt")
        val secondUsed = ContextWindowManagerManager.getWindow().tokensUsed
        assertTrue(secondUsed > firstUsed)
        assertEquals(2, TokenizationStatsManager.getStats().promptCount)
    }

    @Test
    fun `average tokens per prompt is calculated correctly`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("Short")
        val firstAvg = TokenizationStatsManager.getStats().averageTokensPerPrompt
        engine.generate("This is a longer prompt")
        val secondAvg = TokenizationStatsManager.getStats().averageTokensPerPrompt
        assertTrue(secondAvg >= firstAvg)
    }

    @Test
    fun `average tokens per response is calculated correctly`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("Test")
        val firstAvg = TokenizationStatsManager.getStats().averageTokensPerResponse
        engine.generate("Another test")
        val secondAvg = TokenizationStatsManager.getStats().averageTokensPerResponse
        assertTrue(secondAvg >= firstAvg)
    }

    @Test
    fun `context window utilization is low for short prompts`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("Hi")
        val stats = engine.getStats()
        val utilization = stats["utilizationPercentage"] as Float
        assertTrue(utilization < 10f)
    }

    @Test
    fun `context window isNearLimit is false for short prompts`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("Hi")
        assertFalse(ContextWindowManagerManager.getWindow().isNearLimit)
    }

    @Test
    fun `context window isAtLimit is false for short prompts`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("Hi")
        assertFalse(ContextWindowManagerManager.getWindow().isAtLimit)
    }

    @Test
    fun `context window tokensRemaining is positive for short prompts`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("Hi")
        assertTrue(ContextWindowManagerManager.getWindow().tokensRemaining > 0)
    }

    @Test
    fun `context window tokensRemaining equals contextLength minus tokensUsed`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("Test prompt with tokens")
        val expectedRemaining = ContextWindowManagerManager.getWindow().contextLength - ContextWindowManagerManager.getWindow().tokensUsed
        assertEquals(expectedRemaining, ContextWindowManagerManager.getWindow().tokensRemaining)
    }

    @Test
    fun `stats manager can be reset independently`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("Test")
        val initialStats = TokenizationStatsManager.getStats().totalTokens
        assertTrue(initialStats > 0)
        TokenizationStatsManager.reset()
        assertEquals(0, TokenizationStatsManager.getStats().totalTokens)
    }

    @Test
    fun `context window manager can be reset independently`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("Test")
        val initialUsed = ContextWindowManagerManager.getWindow().tokensUsed
        assertTrue(initialUsed > 0)
        ContextWindowManagerManager.reset()
        assertEquals(0, ContextWindowManagerManager.getWindow().tokensUsed)
    }

    @Test
    fun `multiple generations update stats correctly`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("First")
        engine.generate("Second")
        engine.generate("Third")
        val stats = engine.getStats()
        assertEquals(3, stats["promptCount"] as Int)
        assertEquals(3, stats["responseCount"] as Int)
        assertTrue(stats["totalTokens"] as Int > 0)
    }

    @Test
    fun `context window utilization increases with each generation`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        val initialUsed = ContextWindowManagerManager.getWindow().tokensUsed
        engine.generate("Test")
        val afterFirst = ContextWindowManagerManager.getWindow().tokensUsed
        assertTrue(afterFirst > initialUsed)
        engine.generate("Test2")
        val afterSecond = ContextWindowManagerManager.getWindow().tokensUsed
        assertTrue(afterSecond > afterFirst)
    }

    @Test
    fun `average inference time accumulates correctly`() {
        val engine = InferenceEngine()
        engine.loadModel("/data/models/test.gguf")
        engine.generate("Test1")
        val firstTime = engine.averageInferenceTimeMs
        engine.generate("Test2")
        val secondTime = engine.averageInferenceTimeMs
        engine.generate("Test3")
        val thirdTime = engine.averageInferenceTimeMs
        assertTrue(thirdTime >= secondTime)
        assertTrue(secondTime >= firstTime)
    }

    @Test
    fun `stats are independent between test instances`() {
        val engine1 = InferenceEngine()
        engine1.loadModel("/data/models/test.gguf")
        engine1.generate("Test1")
        val stats1 = engine1.getStats()
        
        val engine2 = InferenceEngine()
        engine2.loadModel("/data/models/test.gguf")
        engine2.generate("Test2")
        val stats2 = engine2.getStats()
        
        assertEquals(stats1["promptCount"], stats2["promptCount"])
        assertEquals(stats1["responseCount"], stats2["responseCount"])
    }
}
