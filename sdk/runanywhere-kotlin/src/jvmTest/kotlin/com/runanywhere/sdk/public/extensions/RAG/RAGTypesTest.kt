package com.runanywhere.sdk.public.extensions.RAG

import org.junit.Test
import org.junit.Before
import org.junit.Assert.*
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Pure JVM tests for RAG (Retrieval-Augmented Generation) types and utilities
 * No Android/C++ dependencies required
 */

// ==================== RAG UTILITIES ====================

/**
 * RAG result builder for constructing test results
 */
class RAGResultBuilder {
    private var answer: String = ""
    private var retrievedChunks: List<RAGSearchResult> = emptyList()
    private var contextUsed: String? = null
    private var retrievalTimeMs: Double = 0.0
    private var generationTimeMs: Double = 0.0
    private var totalTimeMs: Double = 0.0

    fun setAnswer(answer: String) = apply { this.answer = answer }
    fun setRetrievedChunks(chunks: List<RAGSearchResult>) = apply { this.retrievedChunks = chunks }
    fun setContextUsed(context: String?) = apply { this.contextUsed = context }
    fun setRetrievalTimeMs(time: Double) = apply { this.retrievalTimeMs = time }
    fun setGenerationTimeMs(time: Double) = apply { this.generationTimeMs = time }
    fun setTotalTimeMs(time: Double) = apply { this.totalTimeMs = time }

    fun build(): RAGResult = RAGResult(
        answer = answer,
        retrievedChunks = retrievedChunks,
        contextUsed = contextUsed,
        retrievalTimeMs = retrievalTimeMs,
        generationTimeMs = generationTimeMs,
        totalTimeMs = totalTimeMs
    )
}

/**
 * Validate RAG result consistency
 */
fun validateRAGResult(result: RAGResult): ValidationResult {
    return ValidationResult(
        isAnswerValid = result.answer.isNotEmpty(),
        isRetrievedChunksValid = result.retrievedChunks.all {
            it.similarityScore in 0.0f..1.0f
        },
        isTimeConsistent = result.totalTimeMs >= (result.retrievalTimeMs + result.generationTimeMs) * 0.95,
        retrievalTimeValid = result.retrievalTimeMs >= 0,
        generationTimeValid = result.generationTimeMs >= 0,
        totalTimeValid = result.totalTimeMs >= 0,
        chunkCount = result.retrievedChunks.size,
        answerLength = result.answer.length
    )
}

data class ValidationResult(
    val isAnswerValid: Boolean,
    val isRetrievedChunksValid: Boolean,
    val isTimeConsistent: Boolean,
    val retrievalTimeValid: Boolean,
    val generationTimeValid: Boolean,
    val totalTimeValid: Boolean,
    val chunkCount: Int,
    val answerLength: Int
) {
    fun allValid(): Boolean = isAnswerValid && isRetrievedChunksValid && isTimeConsistent &&
            retrievalTimeValid && generationTimeValid && totalTimeValid

    fun toSummary(): String {
        return if (allValid()) {
            "Valid: $chunkCount chunks, ${answerLength} chars answer, ${"%.2f".format(totalTimeMs)}ms total"
        } else {
            "Invalid: answer=${isAnswerValid}, chunks=${isRetrievedChunksValid}, time=${isTimeConsistent}"
        }
    }
}

/**
 * Calculate similarity threshold category
 */
fun getThresholdCategory(similarity: Float): SimilarityCategory {
    return when {
        similarity >= 0.8f -> SimilarityCategory.VERY_HIGH
        similarity >= 0.6f -> SimilarityCategory.HIGH
        similarity >= 0.4f -> SimilarityCategory.MEDIUM
        similarity >= 0.2f -> SimilarityCategory.LOW
        else -> SimilarityCategory.VERY_LOW
    }
}

enum class SimilarityCategory {
    VERY_HIGH, HIGH, MEDIUM, LOW, VERY_LOW
}

/**
 * Create a RAG query options with validation
 */
class RAGQueryOptionsBuilder {
    private var question: String = ""
    private var systemPrompt: String? = null
    private var maxTokens: Int = 512
    private var temperature: Float = 0.7f
    private var topP: Float = 0.9f
    private var topK: Int = 40

    fun setQuestion(question: String) = apply {
        require(question.isNotBlank()) { "Question cannot be blank" }
        this.question = question
    }

    fun setSystemPrompt(prompt: String?) = apply {
        this.systemPrompt = prompt
    }

    fun setMaxTokens(maxTokens: Int) = apply {
        require(maxTokens > 0) { "Max tokens must be positive" }
        this.maxTokens = maxTokens
    }

    fun setTemperature(temperature: Float) = apply {
        require(temperature in 0.0f..1.0f) { "Temperature must be between 0 and 1" }
        this.temperature = temperature
    }

    fun setTopP(topP: Float) = apply {
        require(topP in 0.0f..1.0f) { "Top P must be between 0 and 1" }
        this.topP = topP
    }

    fun setTopK(topK: Int) = apply {
        require(topK > 0) { "Top K must be positive" }
        this.topK = topK
    }

    fun build(): RAGQueryOptions = RAGQueryOptions(
        question = question,
        systemPrompt = systemPrompt,
        maxTokens = maxTokens,
        temperature = temperature,
        topP = topP,
        topK = topK
    )
}

// ==================== RAG CONFIGURATION TESTS ====================

class RAGConfigurationTest {

    private lateinit var defaultConfig: RAGConfiguration
    private lateinit var customConfig: RAGConfiguration

    @Before
    fun setUp() {
        defaultConfig = RAGConfiguration(
            embeddingModelPath = "models/embedding.onnx",
            llmModelPath = "models/llm.gguf"
        )

        customConfig = RAGConfiguration(
            embeddingModelPath = "models/all-MiniLM-L6-v2.onnx",
            llmModelPath = "models/Qwen3.5-0.8B.gguf",
            embeddingDimension = 384,
            topK = 5,
            similarityThreshold = 0.25f,
            maxContextTokens = 4096,
            chunkSize = 512,
            chunkOverlap = 50,
            promptTemplate = "Context: {context}\nQuestion: {query}\nAnswer:",
            embeddingConfigJson = "{\"model_name\": \"all-MiniLM-L6-v2\"}",
            llmConfigJson = "{\"temperature\": 0.7, \"top_p\": 0.9}"
        )
    }

    @Test
    fun `default RAG configuration has expected values`() {
        assertEquals("models/embedding.onnx", defaultConfig.embeddingModelPath)
        assertEquals("models/llm.gguf", defaultConfig.llmModelPath)
        assertEquals(384, defaultConfig.embeddingDimension)
        assertEquals(3, defaultConfig.topK)
        assertEquals(0.15f, defaultConfig.similarityThreshold)
        assertEquals(2048, defaultConfig.maxContextTokens)
        assertEquals(512, defaultConfig.chunkSize)
        assertEquals(50, defaultConfig.chunkOverlap)
        assertNull(defaultConfig.promptTemplate)
        assertNull(defaultConfig.embeddingConfigJson)
        assertNull(defaultConfig.llmConfigJson)
    }

    @Test
    fun `custom RAG configuration has all expected values`() {
        assertEquals("models/all-MiniLM-L6-v2.onnx", customConfig.embeddingModelPath)
        assertEquals("models/Qwen3.5-0.8B.gguf", customConfig.llmModelPath)
        assertEquals(384, customConfig.embeddingDimension)
        assertEquals(5, customConfig.topK)
        assertEquals(0.25f, customConfig.similarityThreshold)
        assertEquals(4096, customConfig.maxContextTokens)
        assertEquals(512, customConfig.chunkSize)
        assertEquals(50, customConfig.chunkOverlap)
        assertEquals("Context: {context}\nQuestion: {query}\nAnswer:", customConfig.promptTemplate)
        assertEquals("{\"model_name\": \"all-MiniLM-L6-v2\"}", customConfig.embeddingConfigJson)
        assertEquals("{\"temperature\": 0.7, \"top_p\": 0.9}", customConfig.llmConfigJson)
    }

    @Test
    fun `embedding dimension must be positive`() {
        val config = RAGConfiguration(
            embeddingModelPath = "model.onnx",
            llmModelPath = "llm.gguf",
            embeddingDimension = 128
        )
        assertEquals(128, config.embeddingDimension)

        val config2 = RAGConfiguration(
            embeddingModelPath = "model.onnx",
            llmModelPath = "llm.gguf",
            embeddingDimension = 1536
        )
        assertEquals(1536, config2.embeddingDimension)
    }

    @Test
    fun `topK value reflects configuration`() {
        val config1 = RAGConfiguration("e.onnx", "l.gguf", topK = 3)
        assertEquals(3, config1.topK)

        val config2 = RAGConfiguration("e.onnx", "l.gguf", topK = 10)
        assertEquals(10, config2.topK)
    }

    @Test
    fun `similarityThreshold in valid range`() {
        val config1 = RAGConfiguration("e.onnx", "l.gguf", similarityThreshold = 0.1f)
        assertEquals(0.1f, config1.similarityThreshold)

        val config2 = RAGConfiguration("e.onnx", "l.gguf", similarityThreshold = 0.9f)
        assertEquals(0.9f, config2.similarityThreshold)
    }

    @Test
    fun `maxContextTokens can be large value`() {
        val config = RAGConfiguration(
            "e.onnx", "l.gguf",
            maxContextTokens = 8192
        )
        assertEquals(8192, config.maxContextTokens)
    }

    @Test
    fun `chunkSize and chunkOverlap relationship`() {
        val config = RAGConfiguration(
            "e.onnx", "l.gguf",
            chunkSize = 1024,
            chunkOverlap = 100
        )
        assertEquals(1024, config.chunkSize)
        assertEquals(100, config.chunkOverlap)
        assertTrue(config.chunkOverlap < config.chunkSize)
    }

    @Test
    fun `promptTemplate with placeholders`() {
        val template = "Retrieve from {context}\nAnswer {query}"
        val config = RAGConfiguration(
            "e.onnx", "l.gguf",
            promptTemplate = template
        )
        assertEquals(template, config.promptTemplate)
        assertTrue(config.promptTemplate!!.contains("{context}"))
        assertTrue(config.promptTemplate!!.contains("{query}"))
    }

    @Test
    fun `embeddingConfigJson contains valid JSON`() {
        val json = "{\"model_name\": \"all-MiniLM-L6-v2\",\"dimensions\": 384}"
        val config = RAGConfiguration(
            "e.onnx", "l.gguf",
            embeddingConfigJson = json
        )
        assertEquals(json, config.embeddingConfigJson)
    }

    @Test
    fun `llmConfigJson contains valid JSON`() {
        val json = "{\"temperature\": 0.7, \"top_p\": 0.9, \"top_k\": 40}"
        val config = RAGConfiguration(
            "e.onnx", "l.gguf",
            llmConfigJson = json
        )
        assertEquals(json, config.llmConfigJson)
    }

    @Test
    fun `configuration validation - chunk size exceeds overlap`() {
        val config = RAGConfiguration(
            "e.onnx", "l.gguf",
            chunkSize = 512,
            chunkOverlap = 50
        )
        assertTrue(config.chunkSize > config.chunkOverlap)
    }
}

// ==================== RAG QUERY OPTIONS TESTS ====================

class RAGQueryOptionsTest {

    private lateinit var defaultOptions: RAGQueryOptions

    @Before
    fun setUp() {
        defaultOptions = RAGQueryOptions(
            question = "What is the weather today?",
            maxTokens = 512,
            temperature = 0.7f,
            topP = 0.9f,
            topK = 40
        )
    }

    @Test
    fun `default query options have expected values`() {
        assertEquals("What is the weather today?", defaultOptions.question)
        assertNull(defaultOptions.systemPrompt)
        assertEquals(512, defaultOptions.maxTokens)
        assertEquals(0.7f, defaultOptions.temperature, 0.01f)
        assertEquals(0.9f, defaultOptions.topP, 0.01f)
        assertEquals(40, defaultOptions.topK)
    }

    @Test
    fun `custom query options with system prompt`() {
        val options = RAGQueryOptions(
            question = "Describe this image",
            systemPrompt = "You are an image description assistant",
            maxTokens = 256,
            temperature = 0.5f
        )
        assertEquals("Describe this image", options.question)
        assertEquals("You are an image description assistant", options.systemPrompt)
        assertEquals(256, options.maxTokens)
        assertEquals(0.5f, options.temperature, 0.01f)
    }

    @Test
    fun `builder creates valid options`() {
        val builder = RAGQueryOptionsBuilder()
            .setQuestion("What is RAG?")
            .setMaxTokens(1024)
            .setTemperature(0.8f)
            .setTopP(0.95f)
            .setTopK(50)
            .build()

        assertEquals("What is RAG?", builder.question)
        assertEquals(1024, builder.maxTokens)
        assertEquals(0.8f, builder.temperature, 0.01f)
        assertEquals(0.95f, builder.topP, 0.01f)
        assertEquals(50, builder.topK)
    }

    @Test
    fun `builder rejects blank question`() {
        val builder = RAGQueryOptionsBuilder()
        try {
            builder.setQuestion("  ")
            fail("Should have thrown exception for blank question")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Question cannot be blank"))
        }
    }

    @Test
    fun `builder rejects negative max tokens`() {
        val builder = RAGQueryOptionsBuilder()
        try {
            builder.setMaxTokens(-1)
            fail("Should have thrown exception for negative max tokens")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Max tokens must be positive"))
        }
    }

    @Test
    fun `builder rejects invalid temperature`() {
        val builder = RAGQueryOptionsBuilder()
        try {
            builder.setTemperature(1.5f)
            fail("Should have thrown exception for temperature > 1")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Temperature must be between 0 and 1"))
        }

        try {
            builder.setTemperature(-0.1f)
            fail("Should have thrown exception for temperature < 0")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Temperature must be between 0 and 1"))
        }
    }

    @Test
    fun `builder rejects invalid topP`() {
        val builder = RAGQueryOptionsBuilder()
        try {
            builder.setTopP(1.5f)
            fail("Should have thrown exception for topP > 1")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Top P must be between 0 and 1"))
        }
    }

    @Test
    fun `builder rejects negative topK`() {
        val builder = RAGQueryOptionsBuilder()
        try {
            builder.setTopK(0)
            fail("Should have thrown exception for topK <= 0")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Top K must be positive"))
        }
    }

    @Test
    fun `builder fluent interface works`() {
        val builder = RAGQueryOptionsBuilder()
        val options = builder
            .setQuestion("Test question")
            .setSystemPrompt("System prompt")
            .setMaxTokens(512)
            .setTemperature(0.7f)
            .setTopP(0.9f)
            .setTopK(40)
            .build()

        assertNotNull(options)
        assertEquals("Test question", options.question)
    }

    @Test
    fun `options with minimal values`() {
        val builder = RAGQueryOptionsBuilder()
            .setQuestion("Simple")
            .setMaxTokens(100)
            .setTemperature(0.0f)
            .setTopP(0.0f)
            .setTopK(1)
            .build()

        assertEquals(0.0f, builder.temperature, 0.01f)
        assertEquals(0.0f, builder.topP, 0.01f)
        assertEquals(1, builder.topK)
    }

    @Test
    fun `options with maximum values`() {
        val builder = RAGQueryOptionsBuilder()
            .setQuestion("Complex")
            .setMaxTokens(4096)
            .setTemperature(1.0f)
            .setTopP(1.0f)
            .setTopK(100)
            .build()

        assertEquals(1.0f, builder.temperature, 0.01f)
        assertEquals(1.0f, builder.topP, 0.01f)
        assertEquals(100, builder.topK)
    }
}

// ==================== RAG SEARCH RESULT TESTS ====================

class RAGSearchResultTest {

    @Test
    fun `create search result with all fields`() {
        val result = RAGSearchResult(
            chunkId = "chunk-123",
            text = "This is the retrieved text content",
            similarityScore = 0.85f,
            metadataJson = "{\"source\": \"document.pdf\", \"page\": 5}"
        )
        assertEquals("chunk-123", result.chunkId)
        assertEquals("This is the retrieved text content", result.text)
        assertEquals(0.85f, result.similarityScore, 0.01f)
        assertEquals("{\"source\": \"document.pdf\", \"page\": 5}", result.metadataJson)
    }

    @Test
    fun `search result without metadata`() {
        val result = RAGSearchResult(
            chunkId = "chunk-456",
            text = "Simple chunk without metadata",
            similarityScore = 0.75f
        )
        assertEquals("chunk-456", result.chunkId)
        assertEquals("Simple chunk without metadata", result.text)
        assertEquals(0.75f, result.similarityScore, 0.01f)
        assertNull(result.metadataJson)
    }

    @Test
    fun `similarity score must be in valid range`() {
        val result1 = RAGSearchResult("c1", "text", 0.0f)
        assertEquals(0.0f, result1.similarityScore, 0.01f)

        val result2 = RAGSearchResult("c2", "text", 1.0f)
        assertEquals(1.0f, result2.similarityScore, 0.01f)

        val result3 = RAGSearchResult("c3", "text", 0.5f)
        assertEquals(0.5f, result3.similarityScore, 0.01f)
    }

    @Test
    fun `metadata JSON is parsed correctly`() {
        val json = """{"source": "doc.pdf", "page": 10, "position": "header"}"""
        val result = RAGSearchResult(
            chunkId = "chunk-789",
            text = "Metadata test",
            similarityScore = 0.9f,
            metadataJson = json
        )
        assertEquals(json, result.metadataJson)
        assertTrue(result.metadataJson!!.contains("doc.pdf"))
        assertTrue(result.metadataJson!!.contains("header"))
    }

    @Test
    fun `chunkId uniqueness`() {
        val result1 = RAGSearchResult("chunk-unique-1", "Text 1", 0.8f)
        val result2 = RAGSearchResult("chunk-unique-2", "Text 2", 0.7f)
        assertNotEquals(result1.chunkId, result2.chunkId)
    }
}

// ==================== RAG RESULT TESTS ====================

class RAGResultTest {

    private lateinit var validResult: RAGResult

    @Before
    fun setUp() {
        validResult = RAGResult(
            answer = "Based on the retrieved information, the weather today is sunny with a high of 75°F.",
            retrievedChunks = listOf(
                RAGSearchResult("c1", "It's a sunny day.", 0.85f),
                RAGSearchResult("c2", "Temperature will reach 75°F.", 0.78f)
            ),
            contextUsed = "It's a sunny day. Temperature will reach 75°F.",
            retrievalTimeMs = 125.5,
            generationTimeMs = 342.8,
            totalTimeMs = 468.3
        )
    }

    @Test
    fun `valid RAG result has all fields populated`() {
        assertTrue(validResult.answer.isNotEmpty())
        assertEquals(2, validResult.retrievedChunks.size)
        assertNotNull(validResult.contextUsed)
        assertTrue(validResult.retrievalTimeMs > 0)
        assertTrue(validResult.generationTimeMs > 0)
        assertTrue(validResult.totalTimeMs > 0)
    }

    @Test
    fun `RAG result without context used`() {
        val result = RAGResult(
            answer = "Simple answer",
            retrievedChunks = listOf(RAGSearchResult("c1", "Text", 0.8f)),
            contextUsed = null,
            retrievalTimeMs = 50.0,
            generationTimeMs = 100.0,
            totalTimeMs = 150.0
        )
        assertNull(result.contextUsed)
    }

    @Test
    fun `RAG result with empty chunks list`() {
        val result = RAGResult(
            answer = "No relevant chunks found",
            retrievedChunks = emptyList(),
            contextUsed = null,
            retrievalTimeMs = 10.0,
            generationTimeMs = 50.0,
            totalTimeMs = 60.0
        )
        assertEquals(0, result.retrievedChunks.size)
    }

    @Test
    fun `RAG result time consistency`() {
        assertEquals(468.3, validResult.totalTimeMs, 0.01)
        val sum = validResult.retrievalTimeMs + validResult.generationTimeMs
        assertTrue(Math.abs(validResult.totalTimeMs - sum) < 10.0) // Allow 10ms variance
    }

    @Test
    fun `builder creates valid result`() {
        val builder = RAGResultBuilder()
            .setAnswer("The answer is 42")
            .setRetrievedChunks(listOf(
                RAGSearchResult("c1", "Context 1", 0.9f),
                RAGSearchResult("c2", "Context 2", 0.85f)
            ))
            .setContextUsed("Context 1\nContext 2")
            .setRetrievalTimeMs(100.0)
            .setGenerationTimeMs(200.0)
            .setTotalTimeMs(300.0)
            .build()

        assertEquals("The answer is 42", builder.answer)
        assertEquals(2, builder.retrievedChunks.size)
        assertEquals(300.0, builder.totalTimeMs)
    }

    @Test
    fun `builder with minimal values`() {
        val builder = RAGResultBuilder()
            .setAnswer("Test")
            .setRetrievedChunks(emptyList())
            .setRetrievalTimeMs(0.0)
            .setGenerationTimeMs(0.0)
            .setTotalTimeMs(0.0)
            .build()

        assertEquals("Test", builder.answer)
        assertTrue(builder.retrievedChunks.isEmpty())
    }
}

// ==================== VALIDATION TESTS ====================

class RAGValidationTest {

    @Test
    fun `validate valid RAG result`() {
        val result = RAGResult(
            answer = "A valid answer",
            retrievedChunks = listOf(
                RAGSearchResult("c1", "Text", 0.8f)
            ),
            contextUsed = "Text",
            retrievalTimeMs = 50.0,
            generationTimeMs = 100.0,
            totalTimeMs = 150.0
        )

        val validation = validateRAGResult(result)
        assertTrue(validation.allValid())
        assertTrue(validation.isAnswerValid)
        assertTrue(validation.isRetrievedChunksValid)
        assertTrue(validation.isTimeConsistent)
        assertEquals(1, validation.chunkCount)
    }

    @Test
    fun `validate result with invalid answer`() {
        val result = RAGResult(
            answer = "",
            retrievedChunks = listOf(RAGSearchResult("c1", "Text", 0.8f)),
            contextUsed = "Text",
            retrievalTimeMs = 50.0,
            generationTimeMs = 100.0,
            totalTimeMs = 150.0
        )

        val validation = validateRAGResult(result)
        assertFalse(validation.allValid())
        assertFalse(validation.isAnswerValid)
    }

    @Test
    fun `validate result with invalid similarity score`() {
        val result = RAGResult(
            answer = "Answer",
            retrievedChunks = listOf(
                RAGSearchResult("c1", "Text", 1.5f) // Invalid: > 1.0
            ),
            contextUsed = "Text",
            retrievalTimeMs = 50.0,
            generationTimeMs = 100.0,
            totalTimeMs = 150.0
        )

        val validation = validateRAGResult(result)
        assertFalse(validation.allValid())
        assertFalse(validation.isRetrievedChunksValid)
    }

    @Test
    fun `validate result with inconsistent timing`() {
        val result = RAGResult(
            answer = "Answer",
            retrievedChunks = listOf(RAGSearchResult("c1", "Text", 0.8f)),
            contextUsed = "Text",
            retrievalTimeMs = 50.0,
            generationTimeMs = 100.0,
            totalTimeMs = 100.0 // Should be >= 150
        )

        val validation = validateRAGResult(result)
        assertFalse(validation.allValid())
        assertFalse(validation.isTimeConsistent)
    }

    @Test
    fun `toSummary for valid result`() {
        val result = RAGResult(
            answer = "Valid answer",
            retrievedChunks = listOf(RAGSearchResult("c1", "Text", 0.8f)),
            contextUsed = "Text",
            retrievalTimeMs = 50.0,
            generationTimeMs = 100.0,
            totalTimeMs = 150.0
        )

        val summary = validateRAGResult(result).toSummary()
        assertTrue(summary.startsWith("Valid:"))
        assertTrue(summary.contains("chunks"))
        assertTrue(summary.contains("answer"))
        assertTrue(summary.contains("ms"))
    }

    @Test
    fun `toSummary for invalid result`() {
        val result = RAGResult(
            answer = "",
            retrievedChunks = listOf(RAGSearchResult("c1", "Text", 1.5f)),
            contextUsed = null,
            retrievalTimeMs = 50.0,
            generationTimeMs = 100.0,
            totalTimeMs = 100.0
        )

        val summary = validateRAGResult(result).toSummary()
        assertTrue(summary.startsWith("Invalid:"))
    }
}

// ==================== SIMILARITY CATEGORY TESTS ====================

class SimilarityCategoryTest {

    @Test
    fun `getThresholdCategory for very high similarity`() {
        assertEquals(SimilarityCategory.VERY_HIGH, getThresholdCategory(0.95f))
        assertEquals(SimilarityCategory.VERY_HIGH, getThresholdCategory(0.85f))
    }

    @Test
    fun `getThresholdCategory for high similarity`() {
        assertEquals(SimilarityCategory.HIGH, getThresholdCategory(0.75f))
        assertEquals(SimilarityCategory.HIGH, getThresholdCategory(0.65f))
    }

    @Test
    fun `getThresholdCategory for medium similarity`() {
        assertEquals(SimilarityCategory.MEDIUM, getThresholdCategory(0.55f))
        assertEquals(SimilarityCategory.MEDIUM, getThresholdCategory(0.45f))
    }

    @Test
    fun `getThresholdCategory for low similarity`() {
        assertEquals(SimilarityCategory.LOW, getThresholdCategory(0.35f))
        assertEquals(SimilarityCategory.LOW, getThresholdCategory(0.25f))
    }

    @Test
    fun `getThresholdCategory for very low similarity`() {
        assertEquals(SimilarityCategory.VERY_LOW, getThresholdCategory(0.15f))
        assertEquals(SimilarityCategory.VERY_LOW, getThresholdCategory(0.05f))
        assertEquals(SimilarityCategory.VERY_LOW, getThresholdCategory(0.0f))
    }

    @Test
    fun `boundary values for thresholds`() {
        assertEquals(SimilarityCategory.HIGH, getThresholdCategory(0.6f)) // boundary: HIGH
        assertEquals(SimilarityCategory.MEDIUM, getThresholdCategory(0.4f)) // boundary: MEDIUM
        assertEquals(SimilarityCategory.LOW, getThresholdCategory(0.2f)) // boundary: LOW
    }
}
