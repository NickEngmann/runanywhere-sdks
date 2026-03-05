package com.runanywhere.agent.toolcalling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCallParserTest {

    @Test
    fun `parse valid tool call with <tool_call> tags`() {
        val input = "<tool_call>{"tool": "ui_tap", "arguments": {"index": 5}}</tool_call>"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_tap", result[0].toolName)
        assertEquals(5, result[0].arguments["index"])
    }

    @Test
    fun `parse tool call with function style`() {
        val input = "ui_tap(index=5)"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_tap", result[0].toolName)
        assertEquals(5, result[0].arguments["index"])
    }

    @Test
    fun `parse tool call with app name`() {
        val input = "ui_open_app(app_name=\"Settings\")"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_open_app", result[0].toolName)
        assertEquals("Settings", result[0].arguments["app_name"])
    }

    @Test
    fun `parse tool call with text input`() {
        val input = "ui_type(text=\"Hello World\")"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_type", result[0].toolName)
        assertEquals("Hello World", result[0].arguments["text"])
    }

    @Test
    fun `parse tool call with swipe direction`() {
        val input = "ui_swipe(direction=\"up\")"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_swipe", result[0].toolName)
        assertEquals("up", result[0].arguments["direction"])
    }

    @Test
    fun `parse tool call with done reason`() {
        val input = "ui_done(reason=\"task completed\")"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_done", result[0].toolName)
        assertEquals("task completed", result[0].arguments["reason"])
    }

    @Test
    fun `parse tool call with long press`() {
        val input = "ui_long_press(index=10)"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_long_press", result[0].toolName)
        assertEquals(10, result[0].arguments["index"])
    }

    @Test
    fun `parse tool call with think tags`() {
        val input = "<think>Thinking about the task...</think><tool_call>{"tool": "ui_tap", "arguments": {"index": 5}}</tool_call>"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_tap", result[0].toolName)
    }

    @Test
    fun `parse multiple tool calls`() {
        val input = "<tool_call>{"tool": "ui_tap", "arguments": {"index": 5}}</tool_call><tool_call>{"tool": "ui_type", "arguments": {"text": "Hello"}}</tool_call>"
        val result = ToolCallParser.parse(input)
        assertEquals(2, result.size)
        assertEquals("ui_tap", result[0].toolName)
        assertEquals("ui_type", result[1].toolName)
    }

    @Test
    fun `parse tool call with markdown code fences`() {
        val input = "```json<tool_call>{"tool": "ui_tap", "arguments": {"index": 5}}</tool_call>```"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_tap", result[0].toolName)
    }

    @Test
    fun `parse tool call with unquoted keys`() {
        val input = "<tool_call>{tool: \"ui_tap\", arguments: {index: 5}}</tool_call>"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_tap", result[0].toolName)
        assertEquals(5, result[0].arguments["index"])
    }

    @Test
    fun `parse tool call with trailing comma`() {
        val input = "<tool_call>{"tool": "ui_tap", "arguments": {"index": 5,}}</tool_call>"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_tap", result[0].toolName)
    }

    @Test
    fun `parse tool call with inline format`() {
        val input = "{\"tool_call\": {\"tool\": \"ui_tap\", \"arguments\": {\"index\": 5}}}"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_tap", result[0].toolName)
    }

    @Test
    fun `parse tool call with arguments object`() {
        val input = "<tool_call>{"tool": "ui_tap", "args": {"index": 5}}</tool_call>"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_tap", result[0].toolName)
    }

    @Test
    fun `parse tool call with parameters object`() {
        val input = "<tool_call>{"tool": "ui_tap", "parameters": {"index": 5}}</tool_call>"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_tap", result[0].toolName)
    }

    @Test
    fun `parse tool call with name instead of tool`() {
        val input = "<tool_call>{"name": "ui_tap", "arguments": {"index": 5}}</tool_call>"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_tap", result[0].toolName)
    }

    @Test
    fun `parse tool call with function instead of tool`() {
        val input = "<tool_call>{"function": "ui_tap", "arguments": {"index": 5}}</tool_call>"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_tap", result[0].toolName)
    }

    @Test
    fun `parse tool call with single argument`() {
        val input = "ui_tap(5)"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_tap", result[0].toolName)
        assertEquals(5, result[0].arguments["index"])
    }

    @Test
    fun `parse tool call with string argument`() {
        val input = "ui_type(\"Hello World\")"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_type", result[0].toolName)
        assertEquals("Hello World", result[0].arguments["text"])
    }

    @Test
    fun `parse tool call with double quotes`() {
        val input = "ui_open_app(app_name=\"Settings\")"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_open_app", result[0].toolName)
        assertEquals("Settings", result[0].arguments["app_name"])
    }

    @Test
    fun `parse tool call with single quotes`() {
        val input = "ui_open_app(app_name='Settings')"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("ui_open_app", result[0].toolName)
        assertEquals("Settings", result[0].arguments["app_name"])
    }

    @Test
    fun `contains tool call returns true for valid tool call`() {
        val input = "<tool_call>{"tool": "ui_tap", "arguments": {"index": 5}}</tool_call>"
        assertTrue(ToolCallParser.containsToolCall(input))
    }

    @Test
    fun `contains tool call returns true for function style`() {
        val input = "ui_tap(index=5)"
        assertTrue(ToolCallParser.containsToolCall(input))
    }

    @Test
    fun `contains tool call returns true for inline format`() {
        val input = "{\"tool_call\": {\"tool\": \"ui_tap\"}}"
        assertTrue(ToolCallParser.containsToolCall(input))
    }

    @Test
    fun `contains tool call returns false for plain text`() {
        val input = "This is just plain text without any tool calls"
        assertFalse(ToolCallParser.containsToolCall(input))
    }

    @Test
    fun `extract clean text removes tool calls`() {
        val input = "Here is some text <tool_call>{"tool": "ui_tap", "arguments": {"index": 5}}</tool_call> more text"
        val result = ToolCallParser.extractCleanText(input)
        assertTrue(result.contains("Here is some text"))
        assertTrue(result.contains("more text"))
        assertFalse(result.contains("<tool_call>"))
    }

    @Test
    fun `extract clean text removes think tags`() {
        val input = "<think>Thinking...</think>Here is some text"
        val result = ToolCallParser.extractCleanText(input)
        assertTrue(result.contains("Here is some text"))
        assertFalse(result.contains("Thinking"))
    }

    @Test
    fun `parse tool call with integer value`() {
        val input = "ui_tap(index=5)"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals(5, result[0].arguments["index"])
        assertTrue(result[0].arguments["index"] is Int)
    }

    @Test
    fun `parse tool call with string value`() {
        val input = "ui_open_app(app_name=\"Settings\")"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals("Settings", result[0].arguments["app_name"])
        assertTrue(result[0].arguments["app_name"] is String)
    }

    @Test
    fun `parse tool call with mixed arguments`() {
        val input = "ui_tap(index=5, count=10)"
        val result = ToolCallParser.parse(input)
        assertEquals(1, result.size)
        assertEquals(5, result[0].arguments["index"])
        assertEquals(10, result[0].arguments["count"])
    }
}
