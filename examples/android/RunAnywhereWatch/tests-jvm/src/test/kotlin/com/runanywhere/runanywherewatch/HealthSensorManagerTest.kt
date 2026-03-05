package com.runanywhere.runanywherewatch

import org.junit.Test
import org.junit.Before
import org.junit.Assert.*

/**
 * Pure JVM tests for health sensor data collection and context injection.
 * No Android SDK dependencies - uses simulated sensor data.
 */

// ==================== DATA CLASSES ====================

enum class ActivityType {
    RESTING,
    WALKING,
    RUNNING,
    EXERCISE,
    UNKNOWN
}

data class HealthSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val heartRate: Int? = null,
    val steps: Int? = null,
    val calories: Int? = null,
    val activityType: ActivityType? = null,
    val stepCadence: Float? = null
)

// ==================== HEALTH SENSOR MANAGER ====================

class HealthSensorManager {
    private val heartRateHistory = mutableListOf<Int>()
    private val stepHistory = mutableListOf<Int>()
    private val calorieHistory = mutableListOf<Int>()
    private val activityHistory = mutableListOf<ActivityType>()
    private val cadenceHistory = mutableListOf<Float>()
    
    private var dailyStepCount = 0
    private var dailyCalorieCount = 0
    private var lastHeartRateUpdate = 0L
    private var lastStepUpdate = 0L
    
    private val rollingAverageWindow = 5 // 5 samples for rolling average
    
    fun updateHeartRate(heartRate: Int) {
        if (heartRate > 0 && heartRate < 250) {
            heartRateHistory.add(heartRate)
            lastHeartRateUpdate = System.currentTimeMillis()
            if (heartRateHistory.size > rollingAverageWindow) {
                heartRateHistory.removeAt(0)
            }
        }
    }
    
    fun updateSteps(steps: Int) {
        val now = System.currentTimeMillis()
        if (now - lastStepUpdate > 60000) { // Only update once per minute
            dailyStepCount += steps
            stepHistory.add(steps)
            lastStepUpdate = now
            if (stepHistory.size > rollingAverageWindow) {
                stepHistory.removeAt(0)
            }
        }
    }
    
    fun updateCalories(calories: Int) {
        if (calories > 0) {
            calorieHistory.add(calories)
            dailyCalorieCount += calories
            if (calorieHistory.size > rollingAverageWindow) {
                calorieHistory.removeAt(0)
            }
        }
    }
    
    fun updateActivityType(activity: ActivityType) {
        activityHistory.add(activity)
        if (activityHistory.size > rollingAverageWindow) {
            activityHistory.removeAt(0)
        }
    }
    
    fun updateCadence(cadence: Float) {
        cadenceHistory.add(cadence)
        if (cadenceHistory.size > rollingAverageWindow) {
            cadenceHistory.removeAt(0)
        }
    }
    
    fun getHeartRate(): Int? = heartRateHistory.lastOrNull()
    
    fun getHeartRateAverage(): Float? {
        if (heartRateHistory.isEmpty()) return null
        return heartRateHistory.average().toFloat()
    }
    
    fun getStepCount(): Int = dailyStepCount
    
    fun getStepsToday(): Int = dailyStepCount
    
    fun getCaloriesToday(): Int = dailyCalorieCount
    
    fun getActivityType(): ActivityType? = activityHistory.lastOrNull()
    
    fun getCadence(): Float? = cadenceHistory.lastOrNull()
    
    fun getCadenceAverage(): Float? {
        if (cadenceHistory.isEmpty()) return null
        return cadenceHistory.average().toFloat()
    }
    
    fun takeSnapshot(): HealthSnapshot {
        return HealthSnapshot(
            timestamp = System.currentTimeMillis(),
            heartRate = getHeartRate(),
            steps = getStepCount(),
            calories = getCaloriesToday(),
            activityType = getActivityType(),
            stepCadence = getCadence()
        )
    }
    
    fun reset() {
        heartRateHistory.clear()
        stepHistory.clear()
        calorieHistory.clear()
        activityHistory.clear()
        cadenceHistory.clear()
        dailyStepCount = 0
        dailyCalorieCount = 0
        lastHeartRateUpdate = 0
        lastStepUpdate = 0
    }
}

// ==================== ACTIVITY DETECTION ====================

class ActivityDetector {
    companion object {
        private const val WALKING_CADENCE_MIN = 50f
        private const val WALKING_CADENCE_MAX = 120f
        private const val RUNNING_CADENCE_MIN = 121f
        private const val RUNNING_CADENCE_MAX = 180f
        private const val EXERCISE_HEART_RATE_MIN = 120
        private const val EXERCISE_HEART_RATE_MAX = 170
    }
    
    fun classifyActivity(cadence: Float?, heartRate: Int?): ActivityType {
        if (cadence == null || heartRate == null) {
            return ActivityType.UNKNOWN
        }
        
        // Check for exercise based on heart rate
        if (heartRate >= EXERCISE_HEART_RATE_MIN && heartRate <= EXERCISE_HEART_RATE_MAX) {
            return ActivityType.EXERCISE
        }
        
        // Check cadence ranges
        return when {
            cadence >= RUNNING_CADENCE_MIN && cadence <= RUNNING_CADENCE_MAX -> ActivityType.RUNNING
            cadence >= WALKING_CADENCE_MIN && cadence <= WALKING_CADENCE_MAX -> ActivityType.WALKING
            cadence < WALKING_CADENCE_MIN -> ActivityType.RESTING
            else -> ActivityType.UNKNOWN
        }
    }
    
    fun classifyFromSnapshot(snapshot: HealthSnapshot): ActivityType {
        return classifyActivity(snapshot.stepCadence, snapshot.heartRate)
    }
}

// ==================== HEALTH CONTEXT BUILDER ====================

class HealthContextBuilder {
    fun buildContext(snapshot: HealthSnapshot): String {
        val parts = mutableListOf<String>()
        
        // Heart rate
        snapshot.heartRate?.let { hr ->
            val hrStatus = when {
                hr < 60 -> "low"
                hr > 100 -> "elevated"
                else -> "normal"
            }
            parts.add("User heart rate: ${hr}bpm ($hrStatus)")
        }
        
        // Steps
        snapshot.steps?.let { steps ->
            parts.add("${steps} steps today")
        }
        
        // Activity type
        snapshot.activityType?.let { activity ->
            val activityStr = when (activity) {
                ActivityType.RESTING -> "resting"
                ActivityType.WALKING -> "walking"
                ActivityType.RUNNING -> "running"
                ActivityType.EXERCISE -> "exercising"
                ActivityType.UNKNOWN -> "unknown activity"
            }
            parts.add("currently $activityStr")
        }
        
        // Calories
        snapshot.calories?.let { calories ->
            parts.add("${calories} calories burned")
        }
        
        return parts.joinToString(", ")
    }
    
    fun buildAlertMessage(snapshot: HealthSnapshot, alertType: AlertType): String {
        return when (alertType) {
            AlertType.HIGH_HEART_RATE -> {
                val hr = snapshot.heartRate ?: 0
                "Alert: Heart rate is elevated at ${hr}bpm. Consider resting."
            }
            AlertType.INACTIVITY -> {
                "Reminder: You've been inactive. Try to move around!"
            }
            AlertType.GOAL_COMPLETE -> {
                val steps = snapshot.steps ?: 0
                "Congratulations! You've completed your daily step goal of $steps steps!"
            }
        }
    }
}

enum class AlertType {
    HIGH_HEART_RATE,
    INACTIVITY,
    GOAL_COMPLETE
}

// ==================== HEALTH ALERT MANAGER ====================

class HealthAlertManager {
    companion object {
        private const val HIGH_HEART_RATE_THRESHOLD = 120
        private const val LOW_HEART_RATE_THRESHOLD = 50
        private const val INACTIVITY_MINUTES = 60
        private const val DAILY_STEP_GOAL = 10000
    }
    
    private var lastInactivityCheck = 0L
    
    fun checkAlerts(snapshot: HealthSnapshot): List<AlertType> {
        val alerts = mutableListOf<AlertType>()
        
        // High heart rate alert
        snapshot.heartRate?.let { hr ->
            if (hr > HIGH_HEART_RATE_THRESHOLD) {
                alerts.add(AlertType.HIGH_HEART_RATE)
            }
            if (hr < LOW_HEART_RATE_THRESHOLD && hr > 0) {
                alerts.add(AlertType.HIGH_HEART_RATE)
            }
        }
        
        // Goal completion alert
        snapshot.steps?.let { steps ->
            if (steps >= DAILY_STEP_GOAL) {
                alerts.add(AlertType.GOAL_COMPLETE)
            }
        }
        
        return alerts
    }
    
    fun reset() {
        lastInactivityCheck = 0L
    }
}

// ==================== TESTS ====================

class HealthSensorManagerTest {
    
    private lateinit var manager: HealthSensorManager
    
    @Before
    fun setUp() {
        manager = HealthSensorManager()
    }
    
    @Test
    fun `initial state has no sensor data`() {
        assertNull(manager.getHeartRate())
        assertNull(manager.getHeartRateAverage())
        assertEquals(0, manager.getStepCount())
        assertEquals(0, manager.getCaloriesToday())
        assertNull(manager.getActivityType())
        assertNull(manager.getCadence())
    }
    
    @Test
    fun `updateHeartRate stores and retrieves value`() {
        manager.updateHeartRate(75)
        assertEquals(75, manager.getHeartRate())
    }
    
    @Test
    fun `updateHeartRate calculates rolling average`() {
        manager.updateHeartRate(70)
        manager.updateHeartRate(80)
        manager.updateHeartRate(90)
        val avg = manager.getHeartRateAverage() ?: 0f
        assertEquals(80f, avg, 0.01f)
    }
    
    @Test
    fun `updateHeartRate invalid values are ignored`() {
        manager.updateHeartRate(0)
        manager.updateHeartRate(300)
        assertNull(manager.getHeartRate())
    }
    
    @Test
    fun `updateSteps accumulates daily count`() {
        manager.updateSteps(1000)
        assertEquals(1000, manager.getStepCount())
        
        manager.updateSteps(500)
        assertEquals(1500, manager.getStepCount())
    }
    
    @Test
    fun `updateSteps respects minute interval`() {
        manager.updateSteps(1000)
        val initialCount = manager.getStepCount()
        
        // Try to update immediately - should be ignored
        manager.updateSteps(500)
        assertEquals(initialCount, manager.getStepCount())
    }
    
    @Test
    fun `updateCalories accumulates daily count`() {
        manager.updateCalories(150)
        assertEquals(150, manager.getCaloriesToday())
        
        manager.updateCalories(100)
        assertEquals(250, manager.getCaloriesToday())
    }
    
    @Test
    fun `updateActivityType stores last activity`() {
        manager.updateActivityType(ActivityType.WALKING)
        assertEquals(ActivityType.WALKING, manager.getActivityType())
        
        manager.updateActivityType(ActivityType.RUNNING)
        assertEquals(ActivityType.RUNNING, manager.getActivityType())
    }
    
    @Test
    fun `updateCadence stores and retrieves value`() {
        manager.updateCadence(95.5f)
        assertEquals(95.5f, manager.getCadence() ?: 0f, 0.01f)
    }
    
    @Test
    fun `takeSnapshot captures all current data`() {
        manager.updateHeartRate(85)
        manager.updateSteps(1000)
        manager.updateCalories(200)
        manager.updateActivityType(ActivityType.WALKING)
        manager.updateCadence(100f)
        
        val snapshot = manager.takeSnapshot()
        
        assertEquals(85, snapshot.heartRate)
        assertEquals(1000, snapshot.steps)
        assertEquals(200, snapshot.calories)
        assertEquals(ActivityType.WALKING, snapshot.activityType)
        val cadence = snapshot.stepCadence ?: 0f
        assertEquals(100f, cadence, 0.01f)
    }
    
    @Test
    fun `reset clears all data`() {
        manager.updateHeartRate(85)
        manager.updateSteps(1000)
        manager.updateCalories(200)
        manager.updateActivityType(ActivityType.WALKING)
        
        manager.reset()
        
        assertNull(manager.getHeartRate())
        assertEquals(0, manager.getStepCount())
        assertEquals(0, manager.getCaloriesToday())
        assertNull(manager.getActivityType())
    }
}

class ActivityDetectorTest {
    
    private lateinit var detector: ActivityDetector
    
    @Before
    fun setUp() {
        detector = ActivityDetector()
    }
    
    @Test
    fun `classifyActivity returns UNKNOWN when no data`() {
        assertEquals(ActivityType.UNKNOWN, detector.classifyActivity(null, null))
    }
    
    @Test
    fun `classifyActivity returns UNKNOWN when only cadence provided`() {
        assertEquals(ActivityType.UNKNOWN, detector.classifyActivity(100f, null))
    }
    
    @Test
    fun `classifyActivity returns UNKNOWN when only heart rate provided`() {
        assertEquals(ActivityType.UNKNOWN, detector.classifyActivity(null, 80))
    }
    
    @Test
    fun `classifyActivity detects walking cadence`() {
        assertEquals(ActivityType.WALKING, detector.classifyActivity(90f, 75))
    }
    
    @Test
    fun `classifyActivity detects running cadence`() {
        assertEquals(ActivityType.RUNNING, detector.classifyActivity(150f, 140))
    }
    
    @Test
    fun `classifyActivity detects resting low cadence`() {
        assertEquals(ActivityType.RESTING, detector.classifyActivity(30f, 60))
    }
    
    @Test
    fun `classifyActivity detects exercise high heart rate`() {
        assertEquals(ActivityType.EXERCISE, detector.classifyActivity(130f, 145))
    }
    
    @Test
    fun `classifyActivity prefers exercise over running cadence`() {
        // Exercise HR takes precedence
        assertEquals(ActivityType.EXERCISE, detector.classifyActivity(150f, 145))
    }
    
    @Test
    fun `classifyFromSnapshot works correctly`() {
        val snapshot = HealthSnapshot(
            heartRate = 85,
            stepCadence = 95f,
            activityType = ActivityType.WALKING
        )
        assertEquals(ActivityType.WALKING, detector.classifyFromSnapshot(snapshot))
    }
}

class HealthContextBuilderTest {
    
    private lateinit var builder: HealthContextBuilder
    
    @Before
    fun setUp() {
        builder = HealthContextBuilder()
    }
    
    @Test
    fun `buildContext with empty snapshot returns empty string`() {
        val snapshot = HealthSnapshot()
        val context = builder.buildContext(snapshot)
        assertTrue(context.isEmpty())
    }
    
    @Test
    fun `buildContext includes heart rate info`() {
        val snapshot = HealthSnapshot(heartRate = 85)
        val context = builder.buildContext(snapshot)
        assertTrue(context.contains("85bpm"))
        assertTrue(context.contains("normal"))
    }
    
    @Test
    fun `buildContext identifies elevated heart rate`() {
        val snapshot = HealthSnapshot(heartRate = 110)
        val context = builder.buildContext(snapshot)
        assertTrue(context.contains("elevated"))
    }
    
    @Test
    fun `buildContext identifies low heart rate`() {
        val snapshot = HealthSnapshot(heartRate = 55)
        val context = builder.buildContext(snapshot)
        assertTrue(context.contains("low"))
    }
    
    @Test
    fun `buildContext includes step count`() {
        val snapshot = HealthSnapshot(steps = 5432)
        val context = builder.buildContext(snapshot)
        assertTrue(context.contains("5432 steps today"))
    }
    
    @Test
    fun `buildContext includes activity type`() {
        val snapshot = HealthSnapshot(activityType = ActivityType.RUNNING)
        val context = builder.buildContext(snapshot)
        assertTrue(context.contains("running"))
    }
    
    @Test
    fun `buildContext includes all data when available`() {
        val snapshot = HealthSnapshot(
            heartRate = 95,
            steps = 3500,
            calories = 450,
            activityType = ActivityType.WALKING,
            stepCadence = 100f
        )
        val context = builder.buildContext(snapshot)
        assertTrue(context.contains("95bpm"))
        assertTrue(context.contains("3500 steps today"))
        assertTrue(context.contains("walking"))
        assertTrue(context.contains("450 calories burned"))
    }
    
    @Test
    fun `buildAlertMessage for high heart rate`() {
        val snapshot = HealthSnapshot(heartRate = 130)
        val alert = builder.buildAlertMessage(snapshot, AlertType.HIGH_HEART_RATE)
        assertTrue(alert.contains("Alert"))
        assertTrue(alert.contains("130bpm"))
    }
    
    @Test
    fun `buildAlertMessage for inactivity`() {
        val snapshot = HealthSnapshot()
        val alert = builder.buildAlertMessage(snapshot, AlertType.INACTIVITY)
        assertTrue(alert.contains("inactive"))
        assertTrue(alert.contains("move"))
    }
    
    @Test
    fun `buildAlertMessage for goal complete`() {
        val snapshot = HealthSnapshot(steps = 10000)
        val alert = builder.buildAlertMessage(snapshot, AlertType.GOAL_COMPLETE)
        assertTrue(alert.contains("Congratulations"))
        assertTrue(alert.contains("10000 steps"))
    }
}

class HealthAlertManagerTest {
    
    private lateinit var alertManager: HealthAlertManager
    
    @Before
    fun setUp() {
        alertManager = HealthAlertManager()
    }
    
    @Test
    fun `checkAlerts returns empty when no alerts`() {
        val snapshot = HealthSnapshot(
            heartRate = 75,
            steps = 5000,
            calories = 200
        )
        val alerts = alertManager.checkAlerts(snapshot)
        assertTrue(alerts.isEmpty())
    }
    
    @Test
    fun `checkAlerts detects high heart rate`() {
        val snapshot = HealthSnapshot(heartRate = 130)
        val alerts = alertManager.checkAlerts(snapshot)
        assertTrue(alerts.contains(AlertType.HIGH_HEART_RATE))
    }
    
    @Test
    fun `checkAlerts detects low heart rate`() {
        val snapshot = HealthSnapshot(heartRate = 45)
        val alerts = alertManager.checkAlerts(snapshot)
        assertTrue(alerts.contains(AlertType.HIGH_HEART_RATE))
    }
    
    @Test
    fun `checkAlerts does not flag normal heart rate`() {
        val snapshot = HealthSnapshot(heartRate = 80)
        val alerts = alertManager.checkAlerts(snapshot)
        assertFalse(alerts.contains(AlertType.HIGH_HEART_RATE))
    }
    
    @Test
    fun `checkAlerts detects goal completion`() {
        val snapshot = HealthSnapshot(steps = 10000)
        val alerts = alertManager.checkAlerts(snapshot)
        assertTrue(alerts.contains(AlertType.GOAL_COMPLETE))
    }
    
    @Test
    fun `checkAlerts does not flag incomplete goal`() {
        val snapshot = HealthSnapshot(steps = 9999)
        val alerts = alertManager.checkAlerts(snapshot)
        assertFalse(alerts.contains(AlertType.GOAL_COMPLETE))
    }
    
    @Test
    fun `checkAlerts returns multiple alerts when applicable`() {
        val snapshot = HealthSnapshot(
            heartRate = 135,
            steps = 12000
        )
        val alerts = alertManager.checkAlerts(snapshot)
        assertTrue(alerts.contains(AlertType.HIGH_HEART_RATE))
        assertTrue(alerts.contains(AlertType.GOAL_COMPLETE))
    }
    
    @Test
    fun `reset clears state`() {
        alertManager.reset()
        // In this simplified version, reset just clears the timestamp
        // We can verify it doesn't crash
        val snapshot = HealthSnapshot(heartRate = 130)
        val alerts = alertManager.checkAlerts(snapshot)
        assertTrue(alerts.contains(AlertType.HIGH_HEART_RATE))
    }
}

class HealthSnapshotTest {
    
    @Test
    fun `default snapshot has null values`() {
        val snapshot = HealthSnapshot()
        assertNull(snapshot.heartRate)
        assertNull(snapshot.steps)
        assertNull(snapshot.calories)
        assertNull(snapshot.activityType)
        assertNull(snapshot.stepCadence)
    }
    
    @Test
    fun `snapshot with all data`() {
        val snapshot = HealthSnapshot(
            timestamp = 1234567890L,
            heartRate = 85,
            steps = 5000,
            calories = 300,
            activityType = ActivityType.WALKING,
            stepCadence = 95f
        )
        assertEquals(1234567890L, snapshot.timestamp)
        assertEquals(85, snapshot.heartRate)
        assertEquals(5000, snapshot.steps)
        assertEquals(300, snapshot.calories)
        assertEquals(ActivityType.WALKING, snapshot.activityType)
        val cadence = snapshot.stepCadence ?: 0f
        assertEquals(95f, cadence, 0.01f)
    }
    
    @Test
    fun `snapshot with partial data`() {
        val snapshot = HealthSnapshot(
            heartRate = 72,
            steps = 1200
        )
        assertEquals(72, snapshot.heartRate)
        assertEquals(1200, snapshot.steps)
        assertNull(snapshot.calories)
        assertNull(snapshot.activityType)
    }
}
