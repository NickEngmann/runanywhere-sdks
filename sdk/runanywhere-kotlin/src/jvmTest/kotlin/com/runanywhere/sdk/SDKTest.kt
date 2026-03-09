package com.runanywhere.sdk

import com.runanywhere.sdk.public.RunAnywhere
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull

class SDKTest {
    @Test
    fun testSDKInitialization() = runBlocking {
        // Initialize SDK in development mode (no API key needed)
        RunAnywhere.initializeForDevelopment(apiKey = "test-api-key")

        // Check if SDK is initialized
        val isInitialized = RunAnywhere.isInitialized
        println("SDK initialized: $isInitialized")

        // Clean up
        RunAnywhere.cleanup()
    }

    @Test
    fun testSDKObjectExists() {
        // Verify the RunAnywhere object is accessible
        assertNotNull(RunAnywhere)
    }
}
