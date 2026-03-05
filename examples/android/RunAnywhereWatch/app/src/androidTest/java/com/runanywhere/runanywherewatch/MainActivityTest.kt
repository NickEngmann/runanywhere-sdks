package com.runanywhere.runanywherewatch

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for RunAnywhereWatch module.
 * This will run on devices.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    
    @Test
    fun testApplicationContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.runanywhere.runanywherewatch", appContext.packageName)
    }
    
    @Test
    fun testSDKStatusEnum() {
        val notLoaded = SDKStatus.NOT_LOADED
        val ready = SDKStatus.READY
        val thinking = SDKStatus.THINKING
        
        assertNotNull(notLoaded)
        assertNotNull(ready)
        assertNotNull(thinking)
        
        assertEquals(3, SDKStatus.values().size)
    }
    
    @Test
    fun testSDKStatusValues() {
        val values = SDKStatus.values()
        
        assertEquals(SDKStatus.NOT_LOADED, values[0])
        assertEquals(SDKStatus.READY, values[1])
        assertEquals(SDKStatus.THINKING, values[2])
    }
    
    @Test
    fun testSDKStatusFromOrdinal() {
        val status = SDKStatus.fromOrdinal(0)
        assertEquals(SDKStatus.NOT_LOADED, status)
        
        val status2 = SDKStatus.fromOrdinal(1)
        assertEquals(SDKStatus.READY, status2)
        
        val status3 = SDKStatus.fromOrdinal(2)
        assertEquals(SDKStatus.THINKING, status3)
    }
    
    @Test
    fun testSDKStatusToString() {
        assertEquals("NOT_LOADED", SDKStatus.NOT_LOADED.toString())
        assertEquals("READY", SDKStatus.READY.toString())
        assertEquals("THINKING", SDKStatus.THINKING.toString())
    }
}

// Extension function to get SDKStatus by ordinal
fun SDKStatus.fromOrdinal(ordinal: Int): SDKStatus {
    return SDKStatus.values()[ordinal]
}
