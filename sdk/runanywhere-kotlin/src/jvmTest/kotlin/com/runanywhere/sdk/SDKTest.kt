package com.runanywhere.sdk

import com.runanywhere.sdk.public.SDKEnvironment
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.events.EventBus
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SDKTest {
    @Test
    fun testSDKEnvironmentValues() {
        // Test SDKEnvironment enum values
        assertEquals(0, SDKEnvironment.DEVELOPMENT.cEnvironment)
        assertEquals(1, SDKEnvironment.STAGING.cEnvironment)
        assertEquals(2, SDKEnvironment.PRODUCTION.cEnvironment)
    }

    @Test
    fun testSDKEnvironmentFromCEnvironment() {
        // Test SDKEnvironment.fromCEnvironment
        assertEquals(SDKEnvironment.DEVELOPMENT, SDKEnvironment.fromCEnvironment(0))
        assertEquals(SDKEnvironment.STAGING, SDKEnvironment.fromCEnvironment(1))
        assertEquals(SDKEnvironment.PRODUCTION, SDKEnvironment.fromCEnvironment(2))
        assertEquals(SDKEnvironment.DEVELOPMENT, SDKEnvironment.fromCEnvironment(-1)) // Default
    }

    @Test
    fun testSDKVersion() {
        // Test that SDK version is not empty
        val version = RunAnywhere.version
        assertTrue(version.isNotEmpty(), "SDK version should not be empty")
        println("SDK version: $version")
    }

    @Test
    fun testEventBusAccess() {
        // Test that event bus can be accessed
        val events = RunAnywhere.events
        assertNotNull(events, "Event bus should not be null")
        assertTrue(events is EventBus, "Event bus should be an EventBus instance")
    }

    @Test
    fun testSDKStateBeforeInitialization() {
        // Test SDK state before initialization
        assertFalse(RunAnywhere.isInitialized, "SDK should not be initialized before initialization")
        assertFalse(RunAnywhere.areServicesReady, "Services should not be ready before initialization")
        assertNull(RunAnywhere.environment, "Environment should be null before initialization")
        assertFalse(RunAnywhere.isActive, "SDK should not be active before initialization")
    }

    @Test
    fun testRunAnywhereObjectExists() {
        // Test that RunAnywhere object exists and has expected properties
        val runAnywhere = RunAnywhere
        assertNotNull(runAnywhere, "RunAnywhere object should not be null")
        assertTrue(runAnywhere is com.runanywhere.sdk.public.RunAnywhere, "RunAnywhere should be the correct type")
    }

    @Test
    fun testLogLevelValues() {
        // Test SDKLogger log levels
        val debugLevel = com.runanywhere.sdk.foundation.LogLevel.DEBUG
        val infoLevel = com.runanywhere.sdk.foundation.LogLevel.INFO
        val warningLevel = com.runanywhere.sdk.foundation.LogLevel.WARNING
        val errorLevel = com.runanywhere.sdk.foundation.LogLevel.ERROR

        assertNotNull(debugLevel)
        assertNotNull(infoLevel)
        assertNotNull(warningLevel)
        assertNotNull(errorLevel)
    }

    @Test
    fun testSDKConstants() {
        // Test SDKConstants values
        val constants = com.runanywhere.sdk.utils.SDKConstants
        assertNotNull(constants.SDK_VERSION, "SDK_VERSION should not be null")
        assertTrue(constants.SDK_VERSION.isNotEmpty(), "SDK_VERSION should not be empty")
    }
}
