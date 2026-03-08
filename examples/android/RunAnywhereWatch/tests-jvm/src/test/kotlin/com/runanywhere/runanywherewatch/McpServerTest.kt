package com.runanywhere.runanywherewatch

import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.Assert.*
import com.runanywhere.runanywherewatch.mcp.*

/**
 * Pure JVM tests for McpServer — JSON-RPC 2.0 protocol handling,
 * transcription management, and tool responses.
 */
class McpServerTest {

    private lateinit var server: McpServer

    @Before
    fun setUp() {
        // Use port 0 to avoid binding conflicts — we test processJsonRpc directly
        server = McpServer(port = 0)
    }

    @Test
    fun `initial state has no transcriptions`() {
        assertTrue(server.transcriptions.isEmpty())
        assertFalse(server.isRunning())
        assertEquals(0, server.getRequestCount())
    }

    @Test
    fun `add transcription stores entry`() {
        server.addTranscription("Hello world")
        assertEquals(1, server.transcriptions.size)
        assertEquals("Hello world", server.transcriptions[0].text)
        assertEquals("voice", server.transcriptions[0].source)
        assertEquals(1.0f, server.transcriptions[0].confidence, 0.01f)
    }

    @Test
    fun `add transcription with custom source and confidence`() {
        server.addTranscription("Test input", source = "keyboard", confidence = 0.85f)
        assertEquals(1, server.transcriptions.size)
        assertEquals("keyboard", server.transcriptions[0].source)
        assertEquals(0.85f, server.transcriptions[0].confidence, 0.01f)
    }

    @Test
    fun `transcription list capped at max`() {
        for (i in 1..110) {
            server.addTranscription("Entry $i")
        }
        assertTrue(server.transcriptions.size <= 100)
    }

    @Test
    fun `clear transcriptions removes all`() {
        server.addTranscription("One")
        server.addTranscription("Two")
        server.clearTranscriptions()
        assertTrue(server.transcriptions.isEmpty())
    }

    @Test
    fun `initialize method returns protocol version`() {
        val response = server.processJsonRpc("""{"jsonrpc":"2.0","method":"initialize","id":1}""")
        assertTrue(response.contains("2024-11-05"))
        assertTrue(response.contains("runanywhere-watch-mcp"))
        assertTrue(response.contains("\"id\":1"))
    }

    @Test
    fun `tools list returns four tools`() {
        val response = server.processJsonRpc("""{"jsonrpc":"2.0","method":"tools/list","id":2}""")
        assertTrue(response.contains("get_transcriptions"))
        assertTrue(response.contains("get_status"))
        assertTrue(response.contains("get_health"))
        assertTrue(response.contains("clear_transcriptions"))
    }

    @Test
    fun `get_transcriptions returns empty list`() {
        val response = server.processJsonRpc(
            """{"jsonrpc":"2.0","method":"tools/call","params":{"name":"get_transcriptions","arguments":{}},"id":3}"""
        )
        assertTrue(response.contains("transcriptions"))
        assertTrue(response.contains("count"))
        assertTrue(response.contains("0"))
    }

    @Test
    fun `get_transcriptions returns added entries`() {
        server.addTranscription("Test one")
        server.addTranscription("Test two")
        val response = server.processJsonRpc(
            """{"jsonrpc":"2.0","method":"tools/call","params":{"name":"get_transcriptions","arguments":{}},"id":4}"""
        )
        // Response wraps inner JSON in escaped text field
        assertTrue("Response should contain Test one", response.contains("Test one"))
        assertTrue("Response should contain Test two", response.contains("Test two"))
        assertTrue("Response should contain count 2", response.contains(":2"))
    }

    @Test
    fun `get_transcriptions respects limit`() {
        for (i in 1..10) {
            server.addTranscription("Entry $i")
        }
        val response = server.processJsonRpc(
            """{"jsonrpc":"2.0","method":"tools/call","params":{"name":"get_transcriptions","arguments":{"limit":3}},"id":5}"""
        )
        assertTrue("Response should contain count 3", response.contains(":3"))
    }

    @Test
    fun `get_status returns model info`() {
        val response = server.processJsonRpc(
            """{"jsonrpc":"2.0","method":"tools/call","params":{"name":"get_status","arguments":{}},"id":6}"""
        )
        assertTrue(response.contains("Qwen3.5-0.8B"))
        assertTrue(response.contains("model_state"))
        assertTrue(response.contains("battery_level"))
    }

    @Test
    fun `get_status reflects updated model status`() {
        server.modelStatus = ModelStatusInfo(
            modelState = "ready",
            batteryLevel = 42,
            isCharging = true
        )
        val response = server.processJsonRpc(
            """{"jsonrpc":"2.0","method":"tools/call","params":{"name":"get_status","arguments":{}},"id":7}"""
        )
        assertTrue(response.contains("ready"))
        assertTrue(response.contains("42"))
        assertTrue(response.contains("true"))
    }

    @Test
    fun `get_health returns device metrics`() {
        val response = server.processJsonRpc(
            """{"jsonrpc":"2.0","method":"tools/call","params":{"name":"get_health","arguments":{}},"id":8}"""
        )
        assertTrue(response.contains("memory_used_mb"))
        assertTrue(response.contains("memory_total_mb"))
        assertTrue(response.contains("cpu_usage_percent"))
        assertTrue(response.contains("battery_temp_c"))
    }

    @Test
    fun `get_health reflects updated device info`() {
        server.deviceHealth = DeviceHealthInfo(
            memoryUsedMb = 256,
            memoryTotalMb = 512,
            cpuUsagePercent = 45.5f,
            batteryTempC = 32.1f,
            storageFreeMb = 800
        )
        val response = server.processJsonRpc(
            """{"jsonrpc":"2.0","method":"tools/call","params":{"name":"get_health","arguments":{}},"id":9}"""
        )
        assertTrue(response.contains("256"))
        assertTrue(response.contains("45.5"))
        assertTrue(response.contains("32.1"))
    }

    @Test
    fun `clear_transcriptions tool clears all`() {
        server.addTranscription("To be cleared")
        val response = server.processJsonRpc(
            """{"jsonrpc":"2.0","method":"tools/call","params":{"name":"clear_transcriptions","arguments":{}},"id":10}"""
        )
        assertTrue(response.contains("cleared"))
        assertTrue(server.transcriptions.isEmpty())
    }

    @Test
    fun `unknown method returns error`() {
        val response = server.processJsonRpc("""{"jsonrpc":"2.0","method":"unknown/method","id":11}""")
        assertTrue(response.contains("error"))
        assertTrue(response.contains("-32601"))
        assertTrue(response.contains("Method not found"))
    }

    @Test
    fun `unknown tool returns error`() {
        val response = server.processJsonRpc(
            """{"jsonrpc":"2.0","method":"tools/call","params":{"name":"nonexistent","arguments":{}},"id":12}"""
        )
        assertTrue(response.contains("error"))
        assertTrue(response.contains("-32602"))
        assertTrue(response.contains("Unknown tool"))
    }

    @Test
    fun `extractJsonString parses correctly`() {
        val json = """{"name":"test_tool","version":"1.0"}"""
        assertEquals("test_tool", server.extractJsonString(json, "name"))
        assertEquals("1.0", server.extractJsonString(json, "version"))
    }

    @Test
    fun `extractJsonString returns empty for missing key`() {
        val json = """{"name":"test"}"""
        assertEquals("", server.extractJsonString(json, "missing"))
    }

    @Test
    fun `extractJsonValue parses numbers`() {
        val json = """{"limit":5,"name":"test"}"""
        assertEquals("5", server.extractJsonValue(json, "limit"))
    }

    @Test
    fun `extractJsonObject parses nested object`() {
        val json = """{"params":{"name":"tool","arguments":{"limit":10}}}"""
        val params = server.extractJsonObject(json, "params")
        assertTrue(params.contains("name"))
        assertTrue(params.contains("tool"))
        val args = server.extractJsonObject(params, "arguments")
        assertTrue(args.contains("limit"))
    }

    @Test
    fun `transcription ids are sequential`() {
        server.addTranscription("First")
        server.addTranscription("Second")
        server.addTranscription("Third")
        assertEquals(1, server.transcriptions[0].id)
        assertEquals(2, server.transcriptions[1].id)
        assertEquals(3, server.transcriptions[2].id)
    }

    @Test
    fun `transcription timestamps are set`() {
        val before = System.currentTimeMillis()
        server.addTranscription("Test")
        val after = System.currentTimeMillis()
        val ts = server.transcriptions[0].timestamp
        assertTrue(ts in before..after)
    }

    @Test
    fun `default model status values`() {
        val status = server.modelStatus
        assertEquals("idle", status.modelState)
        assertEquals("Qwen3.5-0.8B", status.modelName)
        assertEquals(100, status.batteryLevel)
        assertFalse(status.isCharging)
    }

    @Test
    fun `default device health values`() {
        val health = server.deviceHealth
        assertEquals(0, health.memoryUsedMb)
        assertEquals(512, health.memoryTotalMb)
        assertEquals(0f, health.cpuUsagePercent, 0.01f)
    }

    @Test
    fun `response format is valid jsonrpc`() {
        val response = server.processJsonRpc("""{"jsonrpc":"2.0","method":"initialize","id":99}""")
        assertTrue(response.startsWith("{"))
        assertTrue(response.endsWith("}"))
        assertTrue(response.contains("\"jsonrpc\":\"2.0\""))
        assertTrue(response.contains("\"id\":99"))
    }
}
