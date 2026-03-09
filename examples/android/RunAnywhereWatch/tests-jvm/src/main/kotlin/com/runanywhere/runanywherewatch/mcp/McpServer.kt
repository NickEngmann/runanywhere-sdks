package com.runanywhere.runanywherewatch.mcp

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Lightweight JSON-RPC 2.0 MCP server for the watch app.
 * Exposes transcription data, model status, and device health
 * over a local TCP socket for companion app / Lotus Tree integration.
 *
 * Tools exposed:
 * - get_transcriptions: Returns recent transcriptions with timestamps
 * - get_status: Returns model state, battery, connectivity
 * - get_health: Returns device metrics (memory, CPU, battery temp)
 * - clear_transcriptions: Clears transcription history
 */
class McpServer(private val port: Int = 8400) {

    private var serverSocket: ServerSocket? = null
    private val running = AtomicBoolean(false)
    private val requestCount = AtomicInteger(0)

    val transcriptions = CopyOnWriteArrayList<TranscriptionEntry>()
    var modelStatus: ModelStatusInfo = ModelStatusInfo()
    var deviceHealth: DeviceHealthInfo = DeviceHealthInfo()

    private val maxTranscriptions = 100

    fun start() {
        if (running.getAndSet(true)) return
        Thread {
            try {
                serverSocket = ServerSocket(port)
                while (running.get()) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        Thread { handleClient(client) }.start()
                    } catch (e: Exception) {
                        if (running.get()) { /* log error */ }
                    }
                }
            } catch (e: Exception) {
                running.set(false)
            }
        }.start()
    }

    fun stop() {
        running.set(false)
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
    }

    fun isRunning(): Boolean = running.get()
    fun getRequestCount(): Int = requestCount.get()

    fun addTranscription(text: String, source: String = "voice", confidence: Float = 1.0f) {
        val entry = TranscriptionEntry(
            id = transcriptions.size + 1,
            text = text,
            timestamp = System.currentTimeMillis(),
            source = source,
            confidence = confidence
        )
        transcriptions.add(entry)
        while (transcriptions.size > maxTranscriptions) {
            transcriptions.removeAt(0)
        }
    }

    fun clearTranscriptions() {
        transcriptions.clear()
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 30_000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            val requestLine = reader.readLine() ?: return

            val jsonBody: String = if (requestLine.startsWith("{")) {
                requestLine
            } else {
                var contentLength = 0
                var line = reader.readLine()
                while (line != null && line.isNotEmpty()) {
                    if (line.lowercase().startsWith("content-length:")) {
                        contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                    line = reader.readLine()
                }
                if (contentLength > 0) {
                    val body = CharArray(contentLength)
                    reader.read(body, 0, contentLength)
                    String(body)
                } else {
                    ""
                }
            }

            val response = processJsonRpc(jsonBody)
            requestCount.incrementAndGet()

            val responseBytes = response.toByteArray()
            writer.print("HTTP/1.1 200 OK\r\n")
            writer.print("Content-Type: application/json\r\n")
            writer.print("Content-Length: ${responseBytes.size}\r\n")
            writer.print("\r\n")
            writer.print(response)
            writer.flush()
        } catch (_: Exception) {
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    internal fun processJsonRpc(json: String): String {
        val id = extractJsonValue(json, "id")
        val method = extractJsonString(json, "method")

        return when (method) {
            "tools/list" -> toolsListResponse(id)
            "tools/call" -> {
                val paramsBlock = extractJsonObject(json, "params")
                val toolName = extractJsonString(paramsBlock, "name")
                toolCallResponse(id, toolName, paramsBlock)
            }
            "initialize" -> initializeResponse(id)
            else -> errorResponse(id, -32601, "Method not found: $method")
        }
    }

    private fun initializeResponse(id: String): String {
        return """{"jsonrpc":"2.0","id":$id,"result":{"protocolVersion":"2024-11-05","capabilities":{"tools":{}},"serverInfo":{"name":"runanywhere-watch-mcp","version":"1.0.0"}}}"""
    }

    private fun toolsListResponse(id: String): String {
        return """{"jsonrpc":"2.0","id":$id,"result":{"tools":[""" +
            """{"name":"get_transcriptions","description":"Get recent voice transcriptions with timestamps","inputSchema":{"type":"object","properties":{"limit":{"type":"integer","description":"Max entries to return (default 20)"}}}},""" +
            """{"name":"get_status","description":"Get watch model status, battery level, and connectivity","inputSchema":{"type":"object","properties":{}}},""" +
            """{"name":"get_health","description":"Get device health metrics (memory, CPU, temperature)","inputSchema":{"type":"object","properties":{}}},""" +
            """{"name":"clear_transcriptions","description":"Clear all stored transcriptions","inputSchema":{"type":"object","properties":{}}}""" +
            """]}}"""
    }

    private fun toolCallResponse(id: String, toolName: String, params: String): String {
        val result = when (toolName) {
            "get_transcriptions" -> {
                val limit = extractJsonValue(extractJsonObject(params, "arguments"), "limit")
                    .toIntOrNull() ?: 20
                getTranscriptionsJson(limit)
            }
            "get_status" -> getStatusJson()
            "get_health" -> getHealthJson()
            "clear_transcriptions" -> {
                clearTranscriptions()
                """{"cleared":true,"message":"All transcriptions cleared"}"""
            }
            else -> return errorResponse(id, -32602, "Unknown tool: $toolName")
        }
        return """{"jsonrpc":"2.0","id":$id,"result":{"content":[{"type":"text","text":${escapeJsonString(result)}}]}}"""
    }

    private fun getTranscriptionsJson(limit: Int): String {
        val entries = transcriptions.takeLast(limit)
        if (entries.isEmpty()) return """{"transcriptions":[],"count":0}"""

        val items = entries.joinToString(",") { entry ->
            """{"id":${entry.id},"text":${escapeJsonString(entry.text)},"timestamp":${entry.timestamp},"source":${escapeJsonString(entry.source)},"confidence":${entry.confidence}}"""
        }
        return """{"transcriptions":[$items],"count":${entries.size}}"""
    }

    private fun getStatusJson(): String {
        val s = modelStatus
        return """{"model_state":${escapeJsonString(s.modelState)},"model_name":${escapeJsonString(s.modelName)},"battery_level":${s.batteryLevel},"is_charging":${s.isCharging},"wifi_connected":${s.wifiConnected},"bluetooth_connected":${s.bluetoothConnected},"uptime_ms":${s.uptimeMs}}"""
    }

    private fun getHealthJson(): String {
        val h = deviceHealth
        return """{"memory_used_mb":${h.memoryUsedMb},"memory_total_mb":${h.memoryTotalMb},"cpu_usage_percent":${h.cpuUsagePercent},"battery_temp_c":${h.batteryTempC},"storage_free_mb":${h.storageFreeMb}}"""
    }

    private fun errorResponse(id: String, code: Int, message: String): String {
        return """{"jsonrpc":"2.0","id":$id,"error":{"code":$code,"message":${escapeJsonString(message)}}}"""
    }

    internal fun extractJsonString(json: String, key: String): String {
        val searchKey = "\"$key\""
        val keyIdx = json.indexOf(searchKey)
        if (keyIdx == -1) return ""
        val colonIdx = json.indexOf(':', keyIdx + searchKey.length)
        if (colonIdx == -1) return ""
        val afterColon = json.substring(colonIdx + 1).trimStart()
        if (afterColon.isEmpty() || afterColon[0] != '"') return ""
        val endQuote = afterColon.indexOf('"', 1)
        if (endQuote == -1) return ""
        return afterColon.substring(1, endQuote)
    }

    internal fun extractJsonValue(json: String, key: String): String {
        val searchKey = "\"$key\""
        val keyIdx = json.indexOf(searchKey)
        if (keyIdx == -1) return ""
        val colonIdx = json.indexOf(':', keyIdx + searchKey.length)
        if (colonIdx == -1) return ""
        val afterColon = json.substring(colonIdx + 1).trimStart()
        if (afterColon.isEmpty()) return ""
        if (afterColon[0] == '"') {
            val endQuote = afterColon.indexOf('"', 1)
            if (endQuote == -1) return ""
            return afterColon.substring(1, endQuote)
        }
        val end = afterColon.indexOfFirst { it == ',' || it == '}' || it == ' ' }
        return if (end == -1) afterColon.trim() else afterColon.substring(0, end).trim()
    }

    internal fun extractJsonObject(json: String, key: String): String {
        val searchKey = "\"$key\""
        val keyIdx = json.indexOf(searchKey)
        if (keyIdx == -1) return "{}"
        val colonIdx = json.indexOf(':', keyIdx + searchKey.length)
        if (colonIdx == -1) return "{}"
        val braceStart = json.indexOf('{', colonIdx)
        if (braceStart == -1) return "{}"
        var depth = 0
        for (i in braceStart until json.length) {
            when (json[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return json.substring(braceStart, i + 1)
                }
            }
        }
        return "{}"
    }

    private fun escapeJsonString(value: String): String {
        val sb = StringBuilder("\"")
        for (ch in value) {
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(ch)
            }
        }
        sb.append("\"")
        return sb.toString()
    }
}

data class TranscriptionEntry(
    val id: Int,
    val text: String,
    val timestamp: Long,
    val source: String = "voice",
    val confidence: Float = 1.0f
)

data class ModelStatusInfo(
    val modelState: String = "idle",
    val modelName: String = "Qwen3.5-0.8B",
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val wifiConnected: Boolean = false,
    val bluetoothConnected: Boolean = false,
    val uptimeMs: Long = 0
)

data class DeviceHealthInfo(
    val memoryUsedMb: Int = 0,
    val memoryTotalMb: Int = 512,
    val cpuUsagePercent: Float = 0f,
    val batteryTempC: Float = 25f,
    val storageFreeMb: Int = 1024
)
