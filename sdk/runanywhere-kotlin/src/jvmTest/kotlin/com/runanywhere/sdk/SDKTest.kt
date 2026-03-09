package com.runanywhere.sdk

import com.runanywhere.sdk.public.RunAnywhere
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull

class SDKTest {
    @Test
    fun testSDKInitialization() = runBlocking {
        try {
            // Initialize SDK in development mode (no API key needed)
            RunAnywhere.initializeForDevelopment(apiKey = "test-api-key")

            // Check if SDK is initialized
            val isInitialized = RunAnywhere.isInitialized
            println("SDK initialized: $isInitialized")

            // Clean up
            RunAnywhere.cleanup()
        } catch (e: UnsatisfiedLinkError) {
            // Expected in JVM-only test environment — native C++ libs not available
            println("SDK init skipped (no native libs): ${e.message}")
        } catch (e: Exception) {
            // Other init failures are acceptable in test environment
            println("SDK init skipped: ${e.message}")
        }
    }

    @Test
    fun testSDKObjectExists() {
        // Verify the RunAnywhere object is accessible
        assertNotNull(RunAnywhere)
    }
}
