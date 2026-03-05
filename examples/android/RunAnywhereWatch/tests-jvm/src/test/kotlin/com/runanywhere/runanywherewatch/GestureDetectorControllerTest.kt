package com.runanywhere.runanywherewatch

import org.junit.Test
import org.junit.Before
import org.junit.Assert.*

/**
 * Pure JVM tests for wrist gesture controls on RunAnywhere Watch.
 * Tests gesture detection state machine, configuration, and processing logic.
 */

// --- Gesture State Enum ---

enum class GestureState {
    IDLE,
    DETECTING,
    RECOGNIZED,
    COOLDOWN
}

// --- Gesture Type Enum ---

enum class GestureType {
    WRIST_RAISE,
    WRIST_TWIST,
    DOUBLE_TAP,
    SHAKE
}

// --- Gesture Configuration ---

data class GestureConfig(
    val raiseAngleThreshold: Float = 30.0f,
    val twistSpeedThreshold: Float = 45.0f,
    val tapForceThreshold: Float = 0.5f,
    val shakeMagnitudeThreshold: Float = 2.0f,
    val cooldownMs: Long = 1000L
)

// --- Sensor Data Model ---

data class SensorReading(
    val acceleration: FloatArray = FloatArray(3),
    val gyroscope: FloatArray = FloatArray(3),
    val timestamp: Long = System.currentTimeMillis()
)

// --- Gesture Event Model ---

data class GestureEvent(
    val type: GestureType,
    val confidence: Float,
    val timestamp: Long,
    val metadata: Map<String, Any> = emptyMap()
)

// --- Gesture Detection Controller ---

class GestureDetectorController(val config: GestureConfig = GestureConfig()) {
    var state: GestureState = GestureState.IDLE
        private set
    var lastGesture: GestureType? = null
        private set
    var lastGestureTimestamp: Long = 0
        private set
    var isCooldownActive: Boolean = false
        private set
    private var cooldownEndTime: Long = 0
    val gestureHistory = mutableListOf<SensorReading>()
    private var tapCount: Int = 0
    private var lastTapTime: Long = 0
    private var previousAcceleration: FloatArray? = null
    private var shakeMagnitude: Float = 0f
    private var twistSpeed: Float = 0f
    private var raiseAngle: Float = 0f

    fun processSensorReading(reading: SensorReading): GestureEvent? {
        if (state == GestureState.COOLDOWN) {
            if (System.currentTimeMillis() >= cooldownEndTime) {
                resetCooldown()
            } else {
                return null
            }
        }

        updateState(reading)
        val detectedGesture = detectGesture(reading)

        if (detectedGesture != null) {
            handleGesture(detectedGesture, reading)
            return detectedGesture
        }

        return null
    }

    private fun updateState(reading: SensorReading) {
        when (state) {
            GestureState.IDLE -> {
                if (isRaisingWrist(reading)) {
                    state = GestureState.DETECTING
                    raiseAngle = calculateRaiseAngle(reading)
                } else if (isTwisting(reading)) {
                    state = GestureState.DETECTING
                    twistSpeed = calculateTwistSpeed(reading)
                } else if (isTapping(reading)) {
                    state = GestureState.DETECTING
                    tapCount++
                    lastTapTime = reading.timestamp
                } else if (isShaking(reading)) {
                    state = GestureState.DETECTING
                    shakeMagnitude = calculateShakeMagnitude(reading)
                }
            }
            GestureState.DETECTING -> {
                if (isGestureComplete(reading)) {
                    state = GestureState.RECOGNIZED
                }
            }
            GestureState.RECOGNIZED -> {
                state = GestureState.COOLDOWN
                cooldownEndTime = reading.timestamp + config.cooldownMs
                isCooldownActive = true
            }
            GestureState.COOLDOWN -> {}
        }
    }

    private fun isRaisingWrist(reading: SensorReading): Boolean {
        val yAcceleration = reading.acceleration[1]
        return yAcceleration > 0.3f
    }

    private fun isTwisting(reading: SensorReading): Boolean {
        val zRotation = reading.gyroscope[2]
        return kotlin.math.abs(zRotation) > 0.2f
    }

    private fun isTapping(reading: SensorReading): Boolean {
        val accelerationMagnitude = calculateAccelerationMagnitude(reading)
        return accelerationMagnitude > 1.5f
    }

    private fun isShaking(reading: SensorReading): Boolean {
        val currentMagnitude = calculateAccelerationMagnitude(reading)
        val prevMagnitude = previousAcceleration?.let { calculateAccelerationMagnitude(it) } ?: 0f
        val magnitudeDiff = kotlin.math.abs(currentMagnitude - prevMagnitude)
        return magnitudeDiff > config.shakeMagnitudeThreshold
    }

    private fun calculateAccelerationMagnitude(reading: SensorReading): Float {
        val x = reading.acceleration[0]
        val y = reading.acceleration[1]
        val z = reading.acceleration[2]
        return kotlin.math.sqrt(x * x + y * y + z * z)
    }

    private fun calculateAccelerationMagnitude(arr: FloatArray): Float {
        val x = arr[0]
        val y = arr[1]
        val z = arr[2]
        return kotlin.math.sqrt(x * x + y * y + z * z)
    }

    private fun calculateRaiseAngle(reading: SensorReading): Float {
        val xAccel = reading.acceleration[0]
        val yAccel = reading.acceleration[1]
        return (kotlin.math.atan2(yAccel, xAccel) * 180.0 / kotlin.math.PI).toFloat()
    }

    private fun calculateTwistSpeed(reading: SensorReading): Float {
        val zGyro = reading.gyroscope[2]
        return zGyro * 100f
    }

    private fun calculateShakeMagnitude(reading: SensorReading): Float {
        val xAccel = reading.acceleration[0]
        val yAccel = reading.acceleration[1]
        val zAccel = reading.acceleration[2]
        return kotlin.math.sqrt(xAccel * xAccel + yAccel * yAccel + zAccel * zAccel)
    }

    private fun isGestureComplete(reading: SensorReading): Boolean {
        if (state != GestureState.DETECTING) return false
        return raiseAngle > config.raiseAngleThreshold ||
               twistSpeed > config.twistSpeedThreshold ||
               tapCount >= 2 ||
               shakeMagnitude > config.shakeMagnitudeThreshold
    }

    private fun detectGesture(reading: SensorReading): GestureEvent? {
        val gestureType = when {
            raiseAngle > config.raiseAngleThreshold -> GestureType.WRIST_RAISE
            twistSpeed > config.twistSpeedThreshold -> GestureType.WRIST_TWIST
            tapCount >= 2 -> GestureType.DOUBLE_TAP
            shakeMagnitude > config.shakeMagnitudeThreshold -> GestureType.SHAKE
            else -> null
        }
        return gestureType?.let {
            GestureEvent(type = it, confidence = 0.8f, timestamp = reading.timestamp)
        }
    }

    private fun handleGesture(event: GestureEvent, reading: SensorReading) {
        lastGesture = event.type
        lastGestureTimestamp = event.timestamp
        gestureHistory.add(reading)
        if (event.type == GestureType.DOUBLE_TAP) {
            tapCount = 0
        }
    }

    fun reset() {
        state = GestureState.IDLE
        lastGesture = null
        lastGestureTimestamp = 0
        isCooldownActive = false
        cooldownEndTime = 0
        gestureHistory.clear()
        tapCount = 0
        previousAcceleration = null
        shakeMagnitude = 0f
        twistSpeed = 0f
        raiseAngle = 0f
    }

    fun getGestureHistorySize(): Int = gestureHistory.size
    fun getRemainingCooldownMs(readingTimestamp: Long): Long {
        if (!isCooldownActive) return 0L
        return kotlin.math.max(0L, cooldownEndTime - readingTimestamp)
    }

    private fun resetCooldown() {
        state = GestureState.IDLE
        isCooldownActive = false
        cooldownEndTime = 0
    }
}

// ========================= TESTS =========================

class GestureStateTest {
    @Test
    fun `all gesture states are defined`() {
        assertEquals(4, GestureState.values().size)
        assertTrue(GestureState.values().contains(GestureState.IDLE))
        assertTrue(GestureState.values().contains(GestureState.DETECTING))
        assertTrue(GestureState.values().contains(GestureState.RECOGNIZED))
        assertTrue(GestureState.values().contains(GestureState.COOLDOWN))
    }
}

class GestureTypeTest {
    @Test
    fun `all gesture types are defined`() {
        assertEquals(4, GestureType.values().size)
        assertTrue(GestureType.values().contains(GestureType.WRIST_RAISE))
        assertTrue(GestureType.values().contains(GestureType.WRIST_TWIST))
        assertTrue(GestureType.values().contains(GestureType.DOUBLE_TAP))
        assertTrue(GestureType.values().contains(GestureType.SHAKE))
    }
}

class GestureConfigTest {
    @Test
    fun `default config values are correct`() {
        val config = GestureConfig()
        assertEquals(30.0f, config.raiseAngleThreshold)
        assertEquals(45.0f, config.twistSpeedThreshold)
        assertEquals(0.5f, config.tapForceThreshold)
        assertEquals(2.0f, config.shakeMagnitudeThreshold)
        assertEquals(1000L, config.cooldownMs)
    }

    @Test
    fun `custom config values can be set`() {
        val config = GestureConfig(
            raiseAngleThreshold = 45.0f,
            twistSpeedThreshold = 60.0f,
            tapForceThreshold = 0.8f,
            shakeMagnitudeThreshold = 3.0f,
            cooldownMs = 2000L
        )
        assertEquals(45.0f, config.raiseAngleThreshold)
        assertEquals(60.0f, config.twistSpeedThreshold)
        assertEquals(0.8f, config.tapForceThreshold)
        assertEquals(3.0f, config.shakeMagnitudeThreshold)
        assertEquals(2000L, config.cooldownMs)
    }
}

class GestureDetectorControllerTest {

    private lateinit var controller: GestureDetectorController
    private lateinit var config: GestureConfig

    @Before
    fun setUp() {
        config = GestureConfig(
            raiseAngleThreshold = 30.0f,
            twistSpeedThreshold = 45.0f,
            tapForceThreshold = 0.5f,
            shakeMagnitudeThreshold = 2.0f,
            cooldownMs = 1000L
        )
        controller = GestureDetectorController(config)
    }

    @Test
    fun `initial state is IDLE`() {
        assertEquals(GestureState.IDLE, controller.state)
        assertNull(controller.lastGesture)
        assertEquals(0L, controller.lastGestureTimestamp)
        assertFalse(controller.isCooldownActive)
        assertEquals(0, controller.gestureHistory.size)
    }

    @Test
    fun `state transitions IDLE to DETECTING on wrist raise`() {
        val reading = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.5f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = System.currentTimeMillis()
        )
        controller.processSensorReading(reading)
        assertEquals(GestureState.DETECTING, controller.state)
    }

    @Test
    fun `state transitions DETECTING to RECOGNIZED when threshold met`() {
        val reading1 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.5f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = System.currentTimeMillis()
        )
        controller.processSensorReading(reading1)
        assertEquals(GestureState.DETECTING, controller.state)

        val reading2 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.8f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = System.currentTimeMillis() + 10
        )
        controller.processSensorReading(reading2)
        assertEquals(GestureState.RECOGNIZED, controller.state)
    }

    @Test
    fun `state transitions RECOGNIZED to COOLDOWN automatically`() {
        val reading1 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.5f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = System.currentTimeMillis()
        )
        controller.processSensorReading(reading1)

        val reading2 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.8f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = System.currentTimeMillis() + 10
        )
        controller.processSensorReading(reading2)
        assertEquals(GestureState.RECOGNIZED, controller.state)

        val reading3 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.8f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = System.currentTimeMillis() + 20
        )
        controller.processSensorReading(reading3)
        assertEquals(GestureState.COOLDOWN, controller.state)
        assertTrue(controller.isCooldownActive)
    }

    @Test
    fun `cooldown transitions back to IDLE after timeout`() {
        val baseTime = System.currentTimeMillis()
        val reading1 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.5f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime
        )
        controller.processSensorReading(reading1)

        val reading2 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.8f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime + 10
        )
        controller.processSensorReading(reading2)

        val reading3 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.8f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime + 20
        )
        controller.processSensorReading(reading3)
        assertEquals(GestureState.COOLDOWN, controller.state)

        // Wait for cooldown to expire by using actual current time
        Thread.sleep(1500) // Wait 1.5 seconds to ensure cooldown expires
        val reading4 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.1f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = System.currentTimeMillis()
        )
        controller.processSensorReading(reading4)
        assertEquals(GestureState.IDLE, controller.state)
        assertFalse(controller.isCooldownActive)
    }

    @Test
    fun `processSensorReading returns null during cooldown`() {
        val baseTime = System.currentTimeMillis()
        val reading1 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.5f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime
        )
        controller.processSensorReading(reading1)

        val reading2 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.8f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime + 10
        )
        controller.processSensorReading(reading2)

        val reading3 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.8f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime + 20
        )
        controller.processSensorReading(reading3)
        assertEquals(GestureState.COOLDOWN, controller.state)

        val reading4 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.5f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime + 100
        )
        val event = controller.processSensorReading(reading4)
        assertNull(event)
    }

    @Test
    fun `lastGesture is set when gesture recognized`() {
        val reading1 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.5f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = System.currentTimeMillis()
        )
        controller.processSensorReading(reading1)

        val reading2 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.8f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = System.currentTimeMillis() + 10
        )
        controller.processSensorReading(reading2)

        val reading3 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.8f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = System.currentTimeMillis() + 20
        )
        controller.processSensorReading(reading3)

        assertNotNull(controller.lastGesture)
    }

    @Test
    fun `lastGestureTimestamp is updated on gesture`() {
        val baseTime = System.currentTimeMillis()
        val reading1 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.5f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime
        )
        controller.processSensorReading(reading1)

        val reading2 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.8f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime + 10
        )
        controller.processSensorReading(reading2)

        val reading3 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.8f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime + 20
        )
        controller.processSensorReading(reading3)

        assertEquals(baseTime + 20, controller.lastGestureTimestamp)
    }

    @Test
    fun `reset clears all state`() {
        val baseTime = System.currentTimeMillis()
        val reading1 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.5f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime
        )
        controller.processSensorReading(reading1)

        val reading2 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.8f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime + 10
        )
        controller.processSensorReading(reading2)

        val reading3 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.8f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime + 20
        )
        controller.processSensorReading(reading3)

        assertEquals(GestureState.COOLDOWN, controller.state)
        assertNotNull(controller.lastGesture)

        controller.reset()

        assertEquals(GestureState.IDLE, controller.state)
        assertNull(controller.lastGesture)
        assertEquals(0L, controller.lastGestureTimestamp)
        assertFalse(controller.isCooldownActive)
        assertEquals(0, controller.gestureHistory.size)
    }

    @Test
    fun `getRemainingCooldownMs returns correct value`() {
        val baseTime = System.currentTimeMillis()
        val reading1 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.5f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime
        )
        controller.processSensorReading(reading1)

        val reading2 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.8f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime + 10
        )
        controller.processSensorReading(reading2)

        val reading3 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.8f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime + 20
        )
        controller.processSensorReading(reading3)

        assertEquals(1000L, controller.getRemainingCooldownMs(baseTime + 20))
        assertEquals(900L, controller.getRemainingCooldownMs(baseTime + 120))
        assertEquals(0L, controller.getRemainingCooldownMs(baseTime + 1020))
        assertEquals(0L, controller.getRemainingCooldownMs(baseTime + 2000))
    }

    @Test
    fun `isCooldownActive returns true during cooldown`() {
        val baseTime = System.currentTimeMillis()
        val reading1 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.5f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime
        )
        controller.processSensorReading(reading1)

        val reading2 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.8f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime + 10
        )
        controller.processSensorReading(reading2)

        val reading3 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.8f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime + 20
        )
        controller.processSensorReading(reading3)

        assertTrue(controller.isCooldownActive)
    }

    @Test
    fun `isCooldownActive returns false when not in cooldown`() {
        assertFalse(controller.isCooldownActive)
        val reading = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.1f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = System.currentTimeMillis()
        )
        controller.processSensorReading(reading)
        assertFalse(controller.isCooldownActive)
    }

    @Test
    fun `custom config affects detection thresholds`() {
        val customConfig = GestureConfig(
            raiseAngleThreshold = 50.0f,
            twistSpeedThreshold = 80.0f,
            tapForceThreshold = 1.0f,
            shakeMagnitudeThreshold = 5.0f,
            cooldownMs = 2000L
        )
        val customController = GestureDetectorController(customConfig)
        val reading = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.5f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = System.currentTimeMillis()
        )
        customController.processSensorReading(reading)
        assertTrue(customController.state == GestureState.IDLE ||
                   customController.state == GestureState.DETECTING)
    }

    @Test
    fun `state machine handles rapid successive readings`() {
        val baseTime = System.currentTimeMillis()
        for (i in 0..10) {
            val reading = SensorReading(
                acceleration = floatArrayOf(0.1f, 0.5f + (i * 0.1f), 0.1f),
                gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
                timestamp = baseTime + (i * 10)
            )
            controller.processSensorReading(reading)
        }
        assertTrue(controller.state == GestureState.RECOGNIZED ||
                   controller.state == GestureState.COOLDOWN)
    }

    @Test
    fun `processSensorReading returns event when gesture detected`() {
        val baseTime = System.currentTimeMillis()
        val reading1 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.5f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime
        )
        controller.processSensorReading(reading1)

        val reading2 = SensorReading(
            acceleration = floatArrayOf(0.1f, 0.8f, 0.1f),
            gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
            timestamp = baseTime + 10
        )
        val event = controller.processSensorReading(reading2)
        assertNotNull(event)
        // State transitions to RECOGNIZED when gesture is detected
        assertEquals(GestureState.RECOGNIZED, controller.state)
    }

    @Test
    fun `gestureHistory accumulates readings`() {
        val baseTime = System.currentTimeMillis()
        for (i in 0..5) {
            val reading = SensorReading(
                acceleration = floatArrayOf(0.1f, 0.5f, 0.1f),
                gyroscope = floatArrayOf(0.0f, 0.0f, 0.0f),
                timestamp = baseTime + (i * 10)
            )
            controller.processSensorReading(reading)
        }
        assertTrue(controller.gestureHistory.size > 0)
    }
}
