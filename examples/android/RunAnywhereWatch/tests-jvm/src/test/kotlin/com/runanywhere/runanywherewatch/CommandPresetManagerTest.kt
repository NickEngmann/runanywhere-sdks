package com.runanywhere.runanywherewatch

import org.junit.Test
import org.junit.Before
import org.junit.Assert.*

/**
 * Pure JVM tests for CommandPresetManager - quick command presets and shortcuts for watch face.
 * Tests CRUD operations, fuzzy matching, built-in defaults, and priority handling.
 */

// ==================== DATA CLASSES ====================

enum class CommandType {
    WEATHER, TIME, TIMER, REMINDER, CALL, MESSAGE, NAVIGATION
}

sealed class CommandResult {
    data class Success(val commandType: CommandType, val action: String) : CommandResult()
    data class Error(val message: String) : CommandResult()
    data class FallbackToLLM(val originalInput: String) : CommandResult()
}

data class PresetCommand(
    var id: String,
    var name: String,
    var triggerPhrase: String,
    var commandType: CommandType,
    var action: String,
    var isBuiltIn: Boolean = false
)

// ==================== PRODUCTION CLASSES ====================

class CommandPresetManager {
    private val presets = mutableListOf<PresetCommand>()
    private var nextId = 5

    // Built-in defaults
    companion object {
        private val DEFAULT_PRESETS = listOf(
            PresetCommand("1", "What's the time", "time", CommandType.TIME, "get_current_time", true),
            PresetCommand("2", "Set timer", "timer", CommandType.TIMER, "start_timer", true),
            PresetCommand("3", "Weather", "weather", CommandType.WEATHER, "get_weather", true),
            PresetCommand("4", "Call home", "call home", CommandType.CALL, "call_contact_home", true)
        )
    }

    init {
        // Load built-in defaults
        presets.addAll(DEFAULT_PRESETS.mapIndexed { index, preset ->
            PresetCommand(
                id = (index + 1).toString(),
                name = preset.name,
                triggerPhrase = preset.triggerPhrase,
                commandType = preset.commandType,
                action = preset.action,
                isBuiltIn = preset.isBuiltIn
            )
        })
    }

    fun getAllPresets(): List<PresetCommand> = presets.toList()

    fun getBuiltInPresets(): List<PresetCommand> = presets.filter { it.isBuiltIn }

    fun getCustomPresets(): List<PresetCommand> = presets.filter { !it.isBuiltIn }

    fun addPreset(name: String, triggerPhrase: String, commandType: CommandType, action: String): PresetCommand {
        val preset = PresetCommand(
            id = nextId++.toString(),
            name = name,
            triggerPhrase = triggerPhrase,
            commandType = commandType,
            action = action,
            isBuiltIn = false
        )
        presets.add(preset)
        return preset
    }

    fun removePreset(id: String): Boolean {
        val preset = presets.find { it.id == id }
        return if (preset != null && !preset.isBuiltIn) {
            presets.remove(preset)
            true
        } else {
            false
        }
    }

    fun updatePreset(id: String, name: String, triggerPhrase: String, commandType: CommandType, action: String): Boolean {
        val preset = presets.find { it.id == id }
        return if (preset != null && !preset.isBuiltIn) {
            preset.name = name
            preset.triggerPhrase = triggerPhrase
            preset.commandType = commandType
            preset.action = action
            true
        } else {
            false
        }
    }

    fun matchCommand(input: String): CommandResult {
        val trimmedInput = input.trim().lowercase()

        // Priority 1: Exact match
        val exactMatch = presets.find { it.triggerPhrase.lowercase() == trimmedInput }
        if (exactMatch != null) {
            return CommandResult.Success(exactMatch.commandType, exactMatch.action)
        }

        // Priority 2: Fuzzy match (contains or Levenshtein)
        val fuzzyMatch = presets.find { preset ->
            val trigger = preset.triggerPhrase.lowercase()
            // Check if input contains trigger or trigger contains input
            trimmedInput.contains(trigger) || trigger.contains(trimmedInput)
        }
        if (fuzzyMatch != null) {
            return CommandResult.Success(fuzzyMatch.commandType, fuzzyMatch.action)
        }

        // Priority 3: Levenshtein distance match (within threshold)
        val levenshteinMatch = presets.minByOrNull { preset ->
            levenshteinDistance(trimmedInput, preset.triggerPhrase.lowercase())
        }?.let { preset ->
            if (levenshteinDistance(trimmedInput, preset.triggerPhrase.lowercase()) <= 3) {
                CommandResult.Success(preset.commandType, preset.action)
            } else null
        }
        if (levenshteinMatch != null) {
            return levenshteinMatch
        }

        // Fallback to LLM
        return CommandResult.FallbackToLLM(input)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length

        if (m == 0) return n
        if (n == 0) return m

        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        return dp[m][n]
    }

    fun getPresetCount(): Int = presets.size

    fun hasBuiltInDefaults(): Boolean = presets.any { it.isBuiltIn }
}

// ========================= TESTS =========================

class CommandPresetManagerTest {

    private lateinit var manager: CommandPresetManager

    @Before
    fun setUp() {
        manager = CommandPresetManager()
    }

    @Test
    fun `initial state has built-in defaults`() {
        assertTrue(manager.hasBuiltInDefaults())
        assertEquals(4, manager.getPresetCount())
    }

    @Test
    fun `built-in presets are not removable`() {
        val builtIn = manager.getBuiltInPresets()
        assertTrue(builtIn.isNotEmpty())

        // Try to remove a built-in preset
        val removed = manager.removePreset(builtIn[0].id)
        assertFalse(removed)

        // Count should remain the same
        assertEquals(4, manager.getPresetCount())
    }

    @Test
    fun `get all presets includes built-in and custom`() {
        manager.addPreset("Custom Timer", "start timer", CommandType.TIMER, "start_custom_timer")
        val all = manager.getAllPresets()
        assertEquals(5, all.size)
    }

    @Test
    fun `get built-in presets only`() {
        val builtIn = manager.getBuiltInPresets()
        assertEquals(4, builtIn.size)
        assertTrue(builtIn.all { it.isBuiltIn })
    }

    @Test
    fun `get custom presets only`() {
        val custom = manager.getCustomPresets()
        assertEquals(0, custom.size)

        manager.addPreset("Custom Timer", "start timer", CommandType.TIMER, "start_custom_timer")
        val customAfter = manager.getCustomPresets()
        assertEquals(1, customAfter.size)
        assertFalse(customAfter[0].isBuiltIn)
    }

    @Test
    fun `add custom preset returns preset with new id`() {
        val preset = manager.addPreset("My Timer", "timer now", CommandType.TIMER, "start_timer")
        assertEquals("My Timer", preset.name)
        assertEquals("timer now", preset.triggerPhrase)
        assertEquals(CommandType.TIMER, preset.commandType)
        assertEquals("start_timer", preset.action)
        assertFalse(preset.isBuiltIn)
        assertEquals("5", preset.id) // First custom preset gets id 5
    }

    @Test
    fun `remove custom preset succeeds`() {
        val preset = manager.addPreset("Custom", "test", CommandType.TIME, "test")
        val removed = manager.removePreset(preset.id)
        assertTrue(removed)
        assertEquals(4, manager.getPresetCount())
    }

    @Test
    fun `remove non-existent preset fails`() {
        val removed = manager.removePreset("999")
        assertFalse(removed)
        assertEquals(4, manager.getPresetCount())
    }

    @Test
    fun `update custom preset succeeds`() {
        val preset = manager.addPreset("Old Name", "old trigger", CommandType.TIME, "old_action")
        val updated = manager.updatePreset(preset.id, "New Name", "new trigger", CommandType.WEATHER, "new_action")
        assertTrue(updated)

        val all = manager.getAllPresets()
        val updatedPreset = all.find { it.id == preset.id }
        assertEquals("New Name", updatedPreset?.name)
        assertEquals("new trigger", updatedPreset?.triggerPhrase)
        assertEquals(CommandType.WEATHER, updatedPreset?.commandType)
        assertEquals("new_action", updatedPreset?.action)
    }

    @Test
    fun `update built-in preset fails`() {
        val builtIn = manager.getBuiltInPresets()[0]
        val updated = manager.updatePreset(builtIn.id, "New Name", "new trigger", CommandType.TIME, "new_action")
        assertFalse(updated)
    }

    @Test
    fun `update non-existent preset fails`() {
        val updated = manager.updatePreset("999", "New Name", "new trigger", CommandType.TIME, "new_action")
        assertFalse(updated)
    }

    @Test
    fun `exact match returns success`() {
        val result = manager.matchCommand("What's the time")
        assertTrue(result is CommandResult.Success)
        assertEquals(CommandType.TIME, (result as CommandResult.Success).commandType)
        assertEquals("get_current_time", (result as CommandResult.Success).action)
    }

    @Test
    fun `case insensitive exact match`() {
        val result = manager.matchCommand("WHAT'S THE TIME")
        assertTrue(result is CommandResult.Success)
        assertEquals(CommandType.TIME, (result as CommandResult.Success).commandType)
    }

    @Test
    fun `trim whitespace before matching`() {
        val result = manager.matchCommand("  What's the time  ")
        assertTrue(result is CommandResult.Success)
        assertEquals(CommandType.TIME, (result as CommandResult.Success).commandType)
    }

    @Test
    fun `contains match works`() {
        // "what time" should match "What's the time" via contains
        val result = manager.matchCommand("what time")
        assertTrue(result is CommandResult.Success)
        assertEquals(CommandType.TIME, (result as CommandResult.Success).commandType)
    }

    @Test
    fun `fuzzy match with partial trigger`() {
        val result = manager.matchCommand("weather")
        assertTrue(result is CommandResult.Success)
        assertEquals(CommandType.WEATHER, (result as CommandResult.Success).commandType)
        assertEquals("get_weather", (result as CommandResult.Success).action)
    }

    @Test
    fun `fallback to LLM when no match`() {
        val result = manager.matchCommand("completely unknown command")
        assertTrue(result is CommandResult.FallbackToLLM)
        assertEquals("completely unknown command", (result as CommandResult.FallbackToLLM).originalInput)
    }

    @Test
    fun `priority exact match over fuzzy match`() {
        // Add a custom preset that could match both ways
        manager.addPreset("Exact", "exact trigger", CommandType.TIME, "exact_action")

        // Exact match should win
        val result = manager.matchCommand("exact trigger")
        assertTrue(result is CommandResult.Success)
        assertEquals("exact_action", (result as CommandResult.Success).action)
    }

    @Test
    fun `priority fuzzy match over levenshtein match`() {
        // Add preset for Levenshtein test
        manager.addPreset("Timer", "set timer", CommandType.TIMER, "set_timer_action")

        // "set timer" should match via contains (fuzzy) not Levenshtein
        val result = manager.matchCommand("set timer")
        assertTrue(result is CommandResult.Success)
        assertEquals(CommandType.TIMER, (result as CommandResult.Success).commandType)
    }

    @Test
    fun `levenshtein match works for typos`() {
        // "whats the time" is close to "What's the time" (Levenshtein distance ~3)
        val result = manager.matchCommand("whats the time")
        assertTrue(result is CommandResult.Success)
        assertEquals(CommandType.TIME, (result as CommandResult.Success).commandType)
    }

    @Test
    fun `levenshtein match with multiple typos`() {
        val result = manager.matchCommand("wether")
        assertTrue(result is CommandResult.Success)
        assertEquals(CommandType.WEATHER, (result as CommandResult.Success).commandType)
    }

    @Test
    fun `levenshtein threshold prevents distant matches`() {
        // "xyz" is too far from any preset
        val result = manager.matchCommand("xyz")
        assertTrue(result is CommandResult.FallbackToLLM)
    }

    @Test
    fun `custom preset matching`() {
        manager.addPreset("Quick Timer", "quick timer", CommandType.TIMER, "quick_timer_action")
        val result = manager.matchCommand("quick timer")
        assertTrue(result is CommandResult.Success)
        assertEquals("quick_timer_action", (result as CommandResult.Success).action)
    }

    @Test
    fun `all built-in commands match`() {
        val builtIn = manager.getBuiltInPresets()
        for (preset in builtIn) {
            val result = manager.matchCommand(preset.triggerPhrase)
            assertTrue(result is CommandResult.Success)
            assertEquals(preset.commandType, (result as CommandResult.Success).commandType)
        }
    }

    @Test
    fun `remove preset reduces count`() {
        val preset = manager.addPreset("To Remove", "remove me", CommandType.TIME, "remove_action")
        assertEquals(5, manager.getPresetCount())

        manager.removePreset(preset.id)
        assertEquals(4, manager.getPresetCount())
    }

    @Test
    fun `update preset preserves id`() {
        val preset = manager.addPreset("Old", "old", CommandType.TIME, "old")
        val oldId = preset.id

        manager.updatePreset(oldId, "New", "new", CommandType.WEATHER, "new")

        val all = manager.getAllPresets()
        val updated = all.find { it.id == oldId }
        assertNotNull(updated)
        assertEquals("New", updated?.name)
    }
}
