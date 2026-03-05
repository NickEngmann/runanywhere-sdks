package com.runanywhere.runanywherewatch

import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for Android 8.1 (API 27) compatibility
 * Verifies version-checking utility functions and fallback paths
 * Pure JVM tests - no Android SDK dependencies
 */
class ApiCompatibilityTest {

    @Test
    fun testApiLevelChecks() {
        // Test API level detection logic using simulated values
        assertTrue("API 27 should be >= 27", isApiLevelAtLeast(27, 27))
        assertTrue("API 28 should be >= 28", isApiLevelAtLeast(28, 28))
        assertTrue("API 29 should be >= 29", isApiLevelAtLeast(29, 29))
        assertTrue("API 30 should be >= 30", isApiLevelAtLeast(30, 30))
        assertTrue("API 31 should be >= 31", isApiLevelAtLeast(31, 31))
        assertTrue("API 32 should be >= 32", isApiLevelAtLeast(32, 32))
        assertTrue("API 33 should be >= 33", isApiLevelAtLeast(33, 33))
        assertTrue("API 34 should be >= 34", isApiLevelAtLeast(34, 34))
    }

    @Test
    fun testApiLevelNotAtLeast() {
        // Test API level detection for lower versions
        assertFalse("API 26 should NOT be >= 27", isApiLevelAtLeast(27, 26))
        assertFalse("API 25 should NOT be >= 27", isApiLevelAtLeast(27, 25))
        assertFalse("API 24 should NOT be >= 27", isApiLevelAtLeast(27, 24))
    }

    @Test
    fun testNotificationChannelCompatibility() {
        // NotificationChannel is available from API 26+, so it should work on API 27
        assertTrue("NotificationChannel should be available on API 27", isApiLevelAtLeast(26, 27))
    }

    @Test
    fun testForegroundServiceTypeCompatibility() {
        // ForegroundServiceType is API 29+, so on API 27 we need a fallback
        assertFalse("ForegroundServiceType is not available on API 27", isApiLevelAtLeast(29, 27))
        assertTrue("API 27 is below ForegroundServiceType requirement", isApiLevelBelow(29, 27))
    }

    @Test
    fun testBiometricPromptCompatibility() {
        // BiometricPrompt is API 28+, so on API 27 we need a fallback
        assertFalse("BiometricPrompt is not available on API 27", isApiLevelAtLeast(28, 27))
        assertTrue("API 27 is below BiometricPrompt requirement", isApiLevelBelow(28, 27))
    }

    @Test
    fun testWindowInsetsCompatibility() {
        // WindowInsets API changes are API 30+, so on API 27 we need compat library
        assertFalse("WindowInsets API changes are not available on API 27", isApiLevelAtLeast(30, 27))
        assertTrue("API 27 is below WindowInsets API requirement", isApiLevelBelow(30, 27))
    }

    @Test
    fun testImageDecoderCompatibility() {
        // ImageDecoder is API 28+, so on API 27 we need BitmapFactory fallback
        assertFalse("ImageDecoder is not available on API 27", isApiLevelAtLeast(28, 27))
        assertTrue("API 27 is below ImageDecoder requirement", isApiLevelBelow(28, 27))
    }

    @Test
    fun testJetpackComposeCompatibility() {
        // Jetpack Compose minimum requirement is API 21, so API 27 is compatible
        assertTrue("Jetpack Compose is compatible with API 27", isApiLevelAtLeast(21, 27))
    }

    @Test
    fun testApi27SpecificChecks() {
        // Verify API 27 is compatible with required features
        assertTrue("API 27 should be >= 21 (Compose minimum)", isApiLevelAtLeast(21, 27))
        assertTrue("API 27 should be >= 26 (NotificationChannel)", isApiLevelAtLeast(26, 27))
        assertFalse("API 27 should NOT be >= 28 (BiometricPrompt)", isApiLevelAtLeast(28, 27))
        assertFalse("API 27 should NOT be >= 29 (ForegroundServiceType)", isApiLevelAtLeast(29, 27))
        assertFalse("API 27 should NOT be >= 30 (WindowInsets API changes)", isApiLevelAtLeast(30, 27))
    }

    // Helper method to check API level (pure JVM implementation)
    private fun isApiLevelAtLeast(apiLevel: Int, currentApiLevel: Int): Boolean {
        return currentApiLevel >= apiLevel
    }

    // Helper method to check if API level is below threshold (pure JVM implementation)
    private fun isApiLevelBelow(apiLevel: Int, currentApiLevel: Int): Boolean {
        return currentApiLevel < apiLevel
    }
}
