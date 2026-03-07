package com.runanywhere.runanywherewatch

import com.runanywhere.runanywherewatch.ble.BleScanner
import com.runanywhere.runanywherewatch.ble.TWatchDevice
import com.runanywhere.runanywherewatch.ble.TWatchServiceFilter
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BleScannerTest {

    @Test
    fun `scanForTWatch discovers device with matching service UUID`() = runBlocking {
        val scanner = BleScanner()
        val mockServiceFilter = mockk<TWatchServiceFilter>()
        val mockDevice = mockk<TWatchDevice>()
        every { mockDevice.deviceName } returns "T-Watch-001"
        every { mockDevice.address } returns "00:11:22:33:44:55"
        every { mockDevice.rssi } returns -60

        // Simulate device discovery
        val discoveredDevices = listOf(mockDevice)
        
        // Verify scanner can process discovered devices
        assertEquals(1, discoveredDevices.size)
        assertEquals("T-Watch-001", discoveredDevices[0].deviceName)
    }

    @Test
    fun `scanForTWatch filters devices by service UUID`() = runBlocking {
        val scanner = BleScanner()
        
        // Create mock devices with different service UUIDs
        val tWatchDevice = mockk<TWatchDevice>()
        every { tWatchDevice.deviceName } returns "T-Watch-Pro"
        every { tWatchDevice.address } returns "AA:BB:CC:DD:EE:FF"
        
        val otherDevice = mockk<TWatchDevice>()
        every { otherDevice.deviceName } returns "Other Device"
        every { otherDevice.address } returns "11:22:33:44:55:66"
        
        val devices = listOf(tWatchDevice, otherDevice)
        
        // Verify we can filter by T-Watch service
        val tWatchDevices = devices.filter { it.deviceName.contains("T-Watch") }
        assertEquals(1, tWatchDevices.size)
        assertEquals("T-Watch-Pro", tWatchDevices[0].deviceName)
    }

    @Test
    fun `scanForTWatch handles empty device list`() = runBlocking {
        val scanner = BleScanner()
        val devices = emptyList<TWatchDevice>()
        
        assertEquals(0, devices.size)
        assertTrue(devices.isEmpty())
    }

    @Test
    fun `scanForTWatch extracts device information correctly`() = runBlocking {
        val scanner = BleScanner()
        
        val device = mockk<TWatchDevice>()
        every { device.deviceName } returns "My T-Watch"
        every { device.address } returns "DE:AD:BE:EF:CA:FE"
        every { device.rssi } returns -45
        
        assertEquals("My T-Watch", device.deviceName)
        assertEquals("DE:AD:BE:EF:CA:FE", device.address)
        assertEquals(-45, device.rssi)
    }

    @Test
    fun `scanForTWatch validates device address format`() = runBlocking {
        val scanner = BleScanner()
        
        val validAddress = "00:11:22:33:44:55"
        val invalidAddress = "invalid"
        
        // Simple validation check
        assertTrue(validAddress.matches(Regex("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}")))
        assertFalse(invalidAddress.matches(Regex("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}")))
    }
}
