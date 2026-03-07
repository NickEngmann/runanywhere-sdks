package com.runanywhere.runanywherewatch

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for BleState and ScanResult data classes
 */
class BleStateTest {
    
    @Test
    fun `test default BleState is disconnected`() {
        val state = BleState()
        
        assertFalse(state.isConnected)
        assertFalse(state.isConnecting)
        assertNull(state.connectedDevice)
        assertNull(state.errorMessage)
        assertEquals(BleGattConstants.ERROR_NONE, state.lastError)
        assertTrue(state.scanResults.isEmpty())
    }
    
    @Test
    fun `test BleState with connected device`() {
        val state = BleState(
            isConnected = true,
            connectedDevice = "T-Watch-001",
            scanResults = listOf(
                ScanResult("T-Watch-001", "00:00:00:00:00:01", java.util.UUID.randomUUID(), -60)
            )
        )
        
        assertTrue(state.isConnected)
        assertEquals("T-Watch-001", state.connectedDevice)
        assertEquals(1, state.scanResults.size)
        assertEquals("T-Watch-001", state.scanResults[0].deviceName)
    }
    
    @Test
    fun `test BleState with error`() {
        val state = BleState(
            errorMessage = "Connection failed",
            lastError = BleGattConstants.ERROR_TIMEOUT
        )
        
        assertEquals("Connection failed", state.errorMessage)
        assertEquals(BleGattConstants.ERROR_TIMEOUT, state.lastError)
    }
    
    @Test
    fun `test BleState with connecting state`() {
        val state = BleState(isConnecting = true)
        
        assertTrue(state.isConnecting)
        assertFalse(state.isConnected)
    }
    
    @Test
    fun `test ScanResult has all properties`() {
        val scanResult = ScanResult(
            deviceName = "T-Watch",
            deviceAddress = "00:00:00:00:00:01",
            deviceUuid = java.util.UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"),
            rssi = -60,
            scanRecord = byteArrayOf(1, 2, 3)
        )
        
        assertEquals("T-Watch", scanResult.deviceName)
        assertEquals("00:00:00:00:00:01", scanResult.deviceAddress)
        assertEquals(-60, scanResult.rssi)
        assertNotNull(scanResult.scanRecord)
        assertEquals(3, scanResult.scanRecord?.size)
    }
    
    @Test
    fun `test ScanResult without scan record`() {
        val scanResult = ScanResult(
            deviceName = "T-Watch",
            deviceAddress = "00:00:00:00:00:01",
            deviceUuid = java.util.UUID.randomUUID(),
            rssi = -70
        )
        
        assertEquals("T-Watch", scanResult.deviceName)
        assertNull(scanResult.scanRecord)
    }
    
    @Test
    fun `test BleState copy creates new instance`() {
        val original = BleState(
            isConnected = true,
            connectedDevice = "T-Watch-001",
            errorMessage = "Error"
        )
        
        val copied = original.copy(isConnected = false, errorMessage = null)
        
        assertFalse(copied.isConnected)
        assertEquals("T-Watch-001", copied.connectedDevice)
        assertNull(copied.errorMessage)
    }
    
    @Test
    fun `test BleState with empty scan results`() {
        val state = BleState(scanResults = emptyList())
        
        assertTrue(state.scanResults.isEmpty())
    }
    
    @Test
    fun `test BleState with multiple scan results`() {
        val state = BleState(scanResults = listOf(
            ScanResult("T-Watch-1", "00:00:00:00:00:01", java.util.UUID.randomUUID(), -50),
            ScanResult("T-Watch-2", "00:00:00:00:00:02", java.util.UUID.randomUUID(), -60),
            ScanResult("T-Watch-3", "00:00:00:00:00:03", java.util.UUID.randomUUID(), -70)
        ))
        
        assertEquals(3, state.scanResults.size)
        assertEquals("T-Watch-1", state.scanResults[0].deviceName)
        assertEquals("T-Watch-2", state.scanResults[1].deviceName)
        assertEquals("T-Watch-3", state.scanResults[2].deviceName)
    }
    
    @Test
    fun `test BleState equals and hashCode`() {
        val state1 = BleState(
            isConnected = true,
            connectedDevice = "T-Watch",
            errorMessage = "Error",
            lastError = BleGattConstants.ERROR_TIMEOUT
        )
        
        val state2 = BleState(
            isConnected = true,
            connectedDevice = "T-Watch",
            errorMessage = "Error",
            lastError = BleGattConstants.ERROR_TIMEOUT
        )
        
        assertEquals(state1, state2)
        assertEquals(state1.hashCode(), state2.hashCode())
    }
    
    @Test
    fun `test BleState not equals when different`() {
        val state1 = BleState(isConnected = true)
        val state2 = BleState(isConnected = false)
        
        assertNotEquals(state1, state2)
    }
    
    @Test
    fun `test ScanResult equals and hashCode`() {
        val result1 = ScanResult(
            deviceName = "T-Watch",
            deviceAddress = "00:00:00:00:00:01",
            deviceUuid = java.util.UUID.randomUUID(),
            rssi = -60
        )
        
        val result2 = ScanResult(
            deviceName = "T-Watch",
            deviceAddress = "00:00:00:00:00:01",
            deviceUuid = result1.deviceUuid,
            rssi = -60
        )
        
        assertEquals(result1, result2)
        assertEquals(result1.hashCode(), result2.hashCode())
    }
}
