package com.runanywhere.runanywherewatch

import org.junit.Test
import org.junit.Before
import org.junit.Assert.*

/**
 * Pure JVM tests for ambient display mode - no Android SDK dependencies.
 * Tests state machine, burn-in protection, battery thresholds, and config.
 */

// ==================== AMBIENT CONFIG ====================

data class AmbientConfig(
    val burnInOffsetX: Int = 2,
    val burnInOffsetY: Int = 2,
    val burnInIntervalSec: Int = 30,
    val updateIntervalMs: Long = 1000,
    val brightnessLevel: Float = 0.3f,
    val showSeconds: Boolean = false,
    val batteryThresholdAmbient: Int = 20,
    val batteryThresholdCritical: Int = 5
)

// ==================== AMBIENT STATE MACHINE ====================

enum class AmbientState {
    ACTIVE,      // Normal watch face
    AMBIENT,     // Low-power ambient mode
    TRANSITIONING, // Transition between modes
    OFF          // Display off
}

class AmbientDisplayController(
    private val config: AmbientConfig = AmbientConfig()
) {
    var state: AmbientState = AmbientState.ACTIVE
        private set
    var burnInOffsetX: Int = 0
        private set
    var burnInOffsetY: Int = 0
        private set
    var lastBurnInUpdate: Long = 0
        private set
    var batteryLevel: Int = 100
        private set
    var isBurnInProtectionActive: Boolean = false
        private set
    var transitionProgress: Float = 0f
        private set
    var lastUpdateTimestamp: Long = 0
        private set
    private var targetState: AmbientState = AmbientState.ACTIVE
        private set

    init {
        lastBurnInUpdate = System.currentTimeMillis()
        lastUpdateTimestamp = System.currentTimeMillis()
    }

    fun setBatteryLevel(level: Int) {
        batteryLevel = level.coerceIn(0, 100)
        checkBatteryThresholds()
    }

    private fun checkBatteryThresholds() {
        if (batteryLevel <= config.batteryThresholdCritical) {
            // Disable ambient mode below critical threshold
            if (state == AmbientState.AMBIENT) {
                state = AmbientState.ACTIVE
            }
            isBurnInProtectionActive = false
        } else if (batteryLevel <= config.batteryThresholdAmbient) {
            // Enable burn-in protection below ambient threshold
            isBurnInProtectionActive = true
        } else {
            isBurnInProtectionActive = false
        }
    }

    fun enterAmbientMode(): Boolean {
        if (state == AmbientState.AMBIENT || state == AmbientState.OFF) return false
        if (batteryLevel <= config.batteryThresholdCritical) return false
        state = AmbientState.TRANSITIONING
        targetState = AmbientState.AMBIENT
        transitionProgress = 0f
        return true
    }

    fun exitAmbientMode(): Boolean {
        if (state != AmbientState.AMBIENT && state != AmbientState.TRANSITIONING) return false
        state = AmbientState.TRANSITIONING
        targetState = AmbientState.ACTIVE
        transitionProgress = 0f
        return true
    }

    fun update(deltaTimeMs: Long = 16) {
        if (state == AmbientState.TRANSITIONING) {
            transitionProgress += deltaTimeMs / 500f // 500ms transition
            if (transitionProgress >= 1f) {
                transitionProgress = 1f
                state = targetState
            }
        }

        if (state == AmbientState.AMBIENT && isBurnInProtectionActive) {
            checkBurnInInterval()
        }

        lastUpdateTimestamp = System.currentTimeMillis()
    }

    private fun checkBurnInInterval() {
        val now = System.currentTimeMillis()
        if (now - lastBurnInUpdate >= config.burnInIntervalSec * 1000L) {
            calculateBurnInOffset()
            lastBurnInUpdate = now
        }
    }

    fun calculateBurnInOffset(): Pair<Int, Int> {
        val offsetX = (Math.random() * config.burnInOffsetX * 2 - config.burnInOffsetX).toInt()
        val offsetY = (Math.random() * config.burnInOffsetY * 2 - config.burnInOffsetY).toInt()
        burnInOffsetX = offsetX
        burnInOffsetY = offsetY
        return Pair(offsetX, offsetY)
    }

    fun getBurnInOffset(): Pair<Int, Int> = Pair(burnInOffsetX, burnInOffsetY)

    fun formatTimeForAmbient(hour: Int, minute: Int, second: Int): String {
        val hourStr = if (hour < 10) "0$hour" else "$hour"
        val minuteStr = if (minute < 10) "0$minute" else "$minute"
        return if (config.showSeconds) {
            "$hourStr:$minuteStr:${if (second < 10) "0$second" else second}"
        } else {
            "$hourStr:$minuteStr"
        }
    }

    fun isTransitioning(): Boolean = state == AmbientState.TRANSITIONING

    fun enterOffMode(): Boolean {
        if (state == AmbientState.OFF) return false
        state = AmbientState.OFF
        transitionProgress = 0f
        return true
    }

    fun exitOffMode(): Boolean {
        if (state != AmbientState.OFF) return false
        state = AmbientState.ACTIVE
        transitionProgress = 0f
        return true
    }
}

// ==================== AMBIENT LAYOUT ====================

data class AmbientLayout(
    val showTime: Boolean = true,
    val showSeconds: Boolean = false,
    val showLastResponse: Boolean = true,
    val maxResponseChars: Int = 50,
    val backgroundColor: Int = 0xFF000000.toInt(),
    val textColor: Int = 0xFFFFFFFF.toInt()
)

class AmbientLayoutRenderer(val layout: AmbientLayout = AmbientLayout()) {
    fun renderTime(hour: Int, minute: Int, second: Int, showSeconds: Boolean): String {
        val hourStr = if (hour < 10) "0$hour" else "$hour"
        val minuteStr = if (minute < 10) "0$minute" else "$minute"
        return if (showSeconds) {
            "$hourStr:$minuteStr:${if (second < 10) "0$second" else second}"
        } else {
            "$hourStr:$minuteStr"
        }
    }

    fun renderLastResponse(response: String?): String {
        if (response == null) return ""
        if (response.length > layout.maxResponseChars) {
            return response.substring(0, layout.maxResponseChars - 3) + "..."
        }
        return response
    }

    fun getRenderedContent(hour: Int, minute: Int, second: Int, lastResponse: String?): String {
        val timeStr = if (layout.showTime) renderTime(hour, minute, second, layout.showSeconds) else ""
        val responseStr = if (layout.showLastResponse) renderLastResponse(lastResponse) else ""
        return listOf(timeStr, responseStr).filter { it.isNotEmpty() }.joinToString("\n")
    }
}

// ==================== BATTERY MONITOR ====================

class BatteryMonitor(
    private val config: AmbientConfig = AmbientConfig()
) {
    var batteryLevel: Int = 100
        private set
    var isCharging: Boolean = false
        private set
    var lastUpdate: Long = 0
        private set

    fun updateBatteryLevel(level: Int, charging: Boolean = false) {
        batteryLevel = level.coerceIn(0, 100)
        isCharging = charging
        lastUpdate = System.currentTimeMillis()
    }

    fun isBelowAmbientThreshold(): Boolean = batteryLevel <= config.batteryThresholdAmbient

    fun isBelowCriticalThreshold(): Boolean = batteryLevel <= config.batteryThresholdCritical

    fun getBatteryStatus(): String {
        return when {
            isCharging -> "Charging (${batteryLevel}%)"
            isBelowCriticalThreshold() -> "Critical (${batteryLevel}%)"
            isBelowAmbientThreshold() -> "Low (${batteryLevel}%)"
            else -> "Normal (${batteryLevel}%)"
        }
    }
}

// ==================== TRANSITION ANIMATOR ====================

class TransitionAnimator(
    private val transitionDurationMs: Long = 500
) {
    var isAnimating: Boolean = false
        private set
    var progress: Float = 0f
        private set
    var elapsedMs: Long = 0
        private set
    var targetState: AmbientState = AmbientState.ACTIVE
        private set

    fun startTransition(target: AmbientState): Boolean {
        if (isAnimating) return false
        isAnimating = true
        targetState = target
        elapsedMs = 0
        progress = 0f
        return true
    }

    fun update(deltaTimeMs: Long = 16): Boolean {
        if (!isAnimating) return false

        elapsedMs += deltaTimeMs
        progress = (elapsedMs.toFloat() / transitionDurationMs).coerceIn(0f, 1f)

        if (progress >= 1f) {
            isAnimating = false
            progress = 1f
            return true // Animation complete
        }
        return false // Still animating
    }

    fun getAlpha(): Float = progress

    fun cancel() {
        isAnimating = false
        progress = 0f
        elapsedMs = 0
    }

    // For testing: simulate elapsed time directly
    fun simulateUpdate(elapsedMs: Long) {
        this.elapsedMs = elapsedMs
        progress = (elapsedMs.toFloat() / transitionDurationMs).coerceIn(0f, 1f)
        if (progress >= 1f) {
            isAnimating = false
            progress = 1f
        }
    }
}

// ========================= TESTS =========================

class AmbientDisplayControllerTest {

    private lateinit var controller: AmbientDisplayController
    private lateinit var config: AmbientConfig

    @Before
    fun setUp() {
        config = AmbientConfig(
            burnInOffsetX = 2,
            burnInOffsetY = 2,
            burnInIntervalSec = 30,
            updateIntervalMs = 1000,
            brightnessLevel = 0.3f,
            showSeconds = false,
            batteryThresholdAmbient = 20,
            batteryThresholdCritical = 5
        )
        controller = AmbientDisplayController(config)
    }

    @Test
    fun `initial state is ACTIVE`() {
        assertEquals(AmbientState.ACTIVE, controller.state)
        assertEquals(0, controller.burnInOffsetX)
        assertEquals(0, controller.burnInOffsetY)
        assertFalse(controller.isBurnInProtectionActive)
        assertEquals(100, controller.batteryLevel)
    }

    @Test
    fun `enterAmbientMode transitions from ACTIVE to TRANSITIONING`() {
        assertTrue(controller.enterAmbientMode())
        assertEquals(AmbientState.TRANSITIONING, controller.state)
        assertEquals(0f, controller.transitionProgress, 0.01f)
    }

    @Test
    fun `enterAmbientMode fails when already in AMBIENT`() {
        controller.enterAmbientMode()
        controller.update(600) // Complete transition
        assertFalse(controller.enterAmbientMode())
        assertEquals(AmbientState.AMBIENT, controller.state)
    }

    @Test
    fun `enterAmbientMode fails when already in OFF`() {
        controller.enterOffMode()
        assertFalse(controller.enterAmbientMode())
        assertEquals(AmbientState.OFF, controller.state)
    }

    @Test
    fun `enterAmbientMode fails when battery critical`() {
        controller.setBatteryLevel(3)
        assertFalse(controller.enterAmbientMode())
        assertEquals(AmbientState.ACTIVE, controller.state)
    }

    @Test
    fun `exitAmbientMode transitions from AMBIENT to TRANSITIONING`() {
        controller.enterAmbientMode()
        controller.update(600) // Complete transition to AMBIENT
        assertTrue(controller.exitAmbientMode())
        assertEquals(AmbientState.TRANSITIONING, controller.state)
    }

    @Test
    fun `exitAmbientMode fails when not in AMBIENT or TRANSITIONING`() {
        assertFalse(controller.exitAmbientMode())
        assertEquals(AmbientState.ACTIVE, controller.state)
    }

    @Test
    fun `transition completes to AMBIENT state`() {
        controller.enterAmbientMode()
        controller.update(600)
        assertEquals(AmbientState.AMBIENT, controller.state)
        assertEquals(1f, controller.transitionProgress, 0.01f)
    }

    @Test
    fun `transition completes to ACTIVE state`() {
        controller.enterAmbientMode()
        controller.update(600)
        controller.exitAmbientMode()
        controller.update(600)
        assertEquals(AmbientState.ACTIVE, controller.state)
    }

    @Test
    fun `enterOffMode transitions to OFF`() {
        assertTrue(controller.enterOffMode())
        assertEquals(AmbientState.OFF, controller.state)
    }

    @Test
    fun `exitOffMode returns to ACTIVE`() {
        controller.enterOffMode()
        assertTrue(controller.exitOffMode())
        assertEquals(AmbientState.ACTIVE, controller.state)
    }

    @Test
    fun `setBatteryLevel updates and checks thresholds`() {
        controller.setBatteryLevel(15)
        assertEquals(15, controller.batteryLevel)
        assertTrue(controller.isBurnInProtectionActive)
    }

    @Test
    fun `setBatteryLevel disables burn-in above threshold`() {
        controller.setBatteryLevel(15)
        assertTrue(controller.isBurnInProtectionActive)
        controller.setBatteryLevel(25)
        assertFalse(controller.isBurnInProtectionActive)
    }

    @Test
    fun `setBatteryLevel critical disables ambient`() {
        controller.setBatteryLevel(15)
        controller.enterAmbientMode()
        controller.update(600)
        assertEquals(AmbientState.AMBIENT, controller.state)
        controller.setBatteryLevel(3)
        assertEquals(AmbientState.ACTIVE, controller.state)
        assertFalse(controller.isBurnInProtectionActive)
    }

    @Test
    fun `calculateBurnInOffset returns values within bounds`() {
        val (offsetX, offsetY) = controller.calculateBurnInOffset()
        assertTrue(offsetX in -config.burnInOffsetX..config.burnInOffsetX)
        assertTrue(offsetY in -config.burnInOffsetY..config.burnInOffsetY)
    }

    @Test
    fun `burnInOffset stays within bounds after multiple calls`() {
        repeat(10) {
            val (offsetX, offsetY) = controller.calculateBurnInOffset()
            assertTrue(offsetX in -config.burnInOffsetX..config.burnInOffsetX)
            assertTrue(offsetY in -config.burnInOffsetY..config.burnInOffsetY)
        }
    }

    @Test
    fun `formatTimeForAmbient without seconds`() {
        val time = controller.formatTimeForAmbient(9, 5, 3)
        assertEquals("09:05", time)
    }

    @Test
    fun `formatTimeForAmbient with seconds`() {
        val time = controller.formatTimeForAmbient(9, 5, 3)
        val configWithSeconds = AmbientConfig(showSeconds = true)
        val controllerWithSeconds = AmbientDisplayController(configWithSeconds)
        val timeWithSeconds = controllerWithSeconds.formatTimeForAmbient(9, 5, 3)
        assertEquals("09:05:03", timeWithSeconds)
    }

    @Test
    fun `formatTimeForAmbient single digit hours and minutes`() {
        val time = controller.formatTimeForAmbient(1, 9, 5)
        assertEquals("01:09", time)
    }

    @Test
    fun `isTransitioning returns true during transition`() {
        controller.enterAmbientMode()
        assertTrue(controller.isTransitioning())
        controller.update(600)
        assertFalse(controller.isTransitioning())
    }

    @Test
    fun `transition progress increases during transition`() {
        controller.enterAmbientMode()
        assertEquals(0f, controller.transitionProgress, 0.01f)
        controller.update(250)
        assertTrue(controller.transitionProgress > 0f)
        assertTrue(controller.transitionProgress < 1f)
    }

    @Test
    fun `batteryLevel is clamped to valid range`() {
        controller.setBatteryLevel(150)
        assertEquals(100, controller.batteryLevel)
        controller.setBatteryLevel(-10)
        assertEquals(0, controller.batteryLevel)
    }

    @Test
    fun `state machine ACTIVE to AMBIENT to ACTIVE`() {
        controller.enterAmbientMode()
        controller.update(600)
        assertEquals(AmbientState.AMBIENT, controller.state)
        controller.exitAmbientMode()
        controller.update(600)
        assertEquals(AmbientState.ACTIVE, controller.state)
    }

    @Test
    fun `state machine ACTIVE to OFF to ACTIVE`() {
        controller.enterOffMode()
        assertEquals(AmbientState.OFF, controller.state)
        controller.exitOffMode()
        assertEquals(AmbientState.ACTIVE, controller.state)
    }
}

class AmbientConfigTest {

    @Test
    fun `default config values`() {
        val config = AmbientConfig()
        assertEquals(2, config.burnInOffsetX)
        assertEquals(2, config.burnInOffsetY)
        assertEquals(30, config.burnInIntervalSec)
        assertEquals(1000L, config.updateIntervalMs)
        assertEquals(0.3f, config.brightnessLevel, 0.01f)
        assertFalse(config.showSeconds)
        assertEquals(20, config.batteryThresholdAmbient)
        assertEquals(5, config.batteryThresholdCritical)
    }

    @Test
    fun `custom config values`() {
        val config = AmbientConfig(
            burnInOffsetX = 4,
            burnInOffsetY = 4,
            burnInIntervalSec = 60,
            showSeconds = true,
            batteryThresholdAmbient = 15,
            batteryThresholdCritical = 3
        )
        assertEquals(4, config.burnInOffsetX)
        assertEquals(60, config.burnInIntervalSec)
        assertTrue(config.showSeconds)
        assertEquals(15, config.batteryThresholdAmbient)
        assertEquals(3, config.batteryThresholdCritical)
    }
}

class AmbientLayoutRendererTest {

    private lateinit var renderer: AmbientLayoutRenderer

    @Before
    fun setUp() {
        renderer = AmbientLayoutRenderer(AmbientLayout())
    }

    @Test
    fun `renderTime without seconds`() {
        val time = renderer.renderTime(9, 5, 3, false)
        assertEquals("09:05", time)
    }

    @Test
    fun `renderTime with seconds`() {
        val time = renderer.renderTime(9, 5, 3, true)
        assertEquals("09:05:03", time)
    }

    @Test
    fun `renderLastResponse truncates long text`() {
        val longText = "a".repeat(100)
        val truncated = renderer.renderLastResponse(longText)
        assertEquals(50, truncated.length) // maxResponseChars
    }

    @Test
    fun `renderLastResponse returns null as empty string`() {
        assertEquals("", renderer.renderLastResponse(null))
    }

    @Test
    fun `renderLastResponse short text unchanged`() {
        val response = renderer.renderLastResponse("Hello")
        assertEquals("Hello", response)
    }

    @Test
    fun `getRenderedContent combines time and response`() {
        val content = renderer.getRenderedContent(9, 5, 3, "Last: AI")
        assertTrue(content.contains("09:05"))
        assertTrue(content.contains("Last: AI"))
    }

    @Test
    fun `getRenderedContent with empty response`() {
        val content = renderer.getRenderedContent(9, 5, 3, null)
        assertEquals("09:05", content)
    }

    @Test
    fun `getRenderedContent with showTime false`() {
        val rendererNoTime = AmbientLayoutRenderer(AmbientLayout(showTime = false))
        val content = rendererNoTime.getRenderedContent(9, 5, 3, "Response")
        assertEquals("Response", content)
    }
}

class BatteryMonitorTest {

    private lateinit var monitor: BatteryMonitor

    @Before
    fun setUp() {
        monitor = BatteryMonitor()
    }

    @Test
    fun `initial state is 100 percent battery not charging`() {
        assertEquals(100, monitor.batteryLevel)
        assertFalse(monitor.isCharging)
    }

    @Test
    fun `updateBatteryLevel sets values`() {
        monitor.updateBatteryLevel(45, true)
        assertEquals(45, monitor.batteryLevel)
        assertTrue(monitor.isCharging)
    }

    @Test
    fun `isBelowAmbientThreshold returns true when at threshold`() {
        monitor.updateBatteryLevel(20)
        assertTrue(monitor.isBelowAmbientThreshold())
    }

    @Test
    fun `isBelowAmbientThreshold returns false when above threshold`() {
        monitor.updateBatteryLevel(21)
        assertFalse(monitor.isBelowAmbientThreshold())
    }

    @Test
    fun `isBelowCriticalThreshold returns true when at threshold`() {
        monitor.updateBatteryLevel(5)
        assertTrue(monitor.isBelowCriticalThreshold())
    }

    @Test
    fun `isBelowCriticalThreshold returns false when above threshold`() {
        monitor.updateBatteryLevel(6)
        assertFalse(monitor.isBelowCriticalThreshold())
    }

    @Test
    fun `getBatteryStatus returns charging status`() {
        monitor.updateBatteryLevel(50, true)
        assertEquals("Charging (50%)", monitor.getBatteryStatus())
    }

    @Test
    fun `getBatteryStatus returns critical status`() {
        monitor.updateBatteryLevel(3)
        assertEquals("Critical (3%)", monitor.getBatteryStatus())
    }

    @Test
    fun `getBatteryStatus returns low status`() {
        monitor.updateBatteryLevel(15)
        assertEquals("Low (15%)", monitor.getBatteryStatus())
    }

    @Test
    fun `getBatteryStatus returns normal status`() {
        monitor.updateBatteryLevel(50)
        assertEquals("Normal (50%)", monitor.getBatteryStatus())
    }

    @Test
    fun `batteryLevel is clamped to valid range`() {
        monitor.updateBatteryLevel(150)
        assertEquals(100, monitor.batteryLevel)
        monitor.updateBatteryLevel(-10)
        assertEquals(0, monitor.batteryLevel)
    }
}

class TransitionAnimatorTest {

    private lateinit var animator: TransitionAnimator

    @Before
    fun setUp() {
        animator = TransitionAnimator(transitionDurationMs = 500)
    }

    @Test
    fun `initial state is not animating`() {
        assertFalse(animator.isAnimating)
        assertEquals(0f, animator.progress, 0.01f)
    }

    @Test
    fun `startTransition sets animating state`() {
        assertTrue(animator.startTransition(AmbientState.AMBIENT))
        assertTrue(animator.isAnimating)
        assertEquals(AmbientState.AMBIENT, animator.targetState)
        assertEquals(0f, animator.progress, 0.01f)
    }

    @Test
    fun `startTransition fails when already animating`() {
        animator.startTransition(AmbientState.AMBIENT)
        assertFalse(animator.startTransition(AmbientState.OFF))
    }

    @Test
    fun `update returns true when animation complete`() {
        animator.startTransition(AmbientState.AMBIENT)
        animator.update(600)
        assertFalse(animator.isAnimating)
        assertEquals(1f, animator.progress, 0.01f)
    }

    @Test
    fun `update returns false when still animating`() {
        animator.startTransition(AmbientState.AMBIENT)
        animator.update(250)
        assertTrue(animator.isAnimating)
        assertEquals(0.5f, animator.progress, 0.01f)
    }

    @Test
    fun `getAlpha returns progress`() {
        animator.startTransition(AmbientState.AMBIENT)
        animator.update(250)
        assertEquals(0.5f, animator.getAlpha(), 0.01f)
    }

    @Test
    fun `cancel stops animation`() {
        animator.startTransition(AmbientState.AMBIENT)
        animator.cancel()
        assertFalse(animator.isAnimating)
        assertEquals(0f, animator.progress, 0.01f)
    }

    @Test
    fun `update returns false when not animating`() {
        assertFalse(animator.update(100))
    }
}

class AmbientDisplayControllerIntegrationTest {

    private lateinit var controller: AmbientDisplayController
    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var animator: TransitionAnimator
    private lateinit var renderer: AmbientLayoutRenderer

    @Before
    fun setUp() {
        controller = AmbientDisplayController()
        batteryMonitor = BatteryMonitor()
        animator = TransitionAnimator(500)
        renderer = AmbientLayoutRenderer(AmbientLayout())
    }

    @Test
    fun `full lifecycle ACTIVE to AMBIENT to ACTIVE`() {
        // Start in ACTIVE
        assertEquals(AmbientState.ACTIVE, controller.state)

        // Enter ambient mode
        assertTrue(controller.enterAmbientMode())
        assertEquals(AmbientState.TRANSITIONING, controller.state)

        // Complete transition
        animator.startTransition(AmbientState.AMBIENT)
        while (animator.isAnimating) {
            controller.update(16)
            animator.update(16)
        }
        assertEquals(AmbientState.AMBIENT, controller.state)

        // Exit ambient mode
        assertTrue(controller.exitAmbientMode())
        assertEquals(AmbientState.TRANSITIONING, controller.state)

        // Complete transition back to ACTIVE
        animator.startTransition(AmbientState.ACTIVE)
        while (animator.isAnimating) {
            controller.update(16)
            animator.update(16)
        }
        assertEquals(AmbientState.ACTIVE, controller.state)
    }

    @Test
    fun `battery critical disables ambient mode`() {
        batteryMonitor.updateBatteryLevel(3)
        controller.setBatteryLevel(3)

        // Should not be able to enter ambient mode
        assertFalse(controller.enterAmbientMode())
        assertEquals(AmbientState.ACTIVE, controller.state)
    }

    @Test
    fun `burn-in protection activates at low battery`() {
        controller.setBatteryLevel(15)
        assertTrue(controller.isBurnInProtectionActive)

        controller.setBatteryLevel(25)
        assertFalse(controller.isBurnInProtectionActive)
    }

    @Test
    fun `time formatting works in ambient mode`() {
        controller.setBatteryLevel(50)
        controller.enterAmbientMode()
        controller.update(600)

        assertEquals(AmbientState.AMBIENT, controller.state)

        val timeStr = controller.formatTimeForAmbient(14, 30, 45)
        assertEquals("14:30", timeStr)

        val configWithSeconds = AmbientConfig(showSeconds = true)
        val controllerWithSeconds = AmbientDisplayController(configWithSeconds)
        val timeWithSeconds = controllerWithSeconds.formatTimeForAmbient(14, 30, 45)
        assertEquals("14:30:45", timeWithSeconds)
    }

    @Test
    fun `transition progress increases over time`() {
        controller.enterAmbientMode()
        assertEquals(0f, controller.transitionProgress, 0.01f)

        controller.update(100)
        assertTrue(controller.transitionProgress > 0f)
        assertTrue(controller.transitionProgress < 1f)

        controller.update(500)
        assertEquals(1f, controller.transitionProgress, 0.01f)
        assertEquals(AmbientState.AMBIENT, controller.state) // Should have transitioned to AMBIENT
    }
}
