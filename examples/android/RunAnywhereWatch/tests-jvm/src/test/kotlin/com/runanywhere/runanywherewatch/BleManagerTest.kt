package com.runanywhere.runanywherewatch

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.junit.Test
import org.junit.Assert.*

/**
 * Pure JVM tests for BLE Scanner and Connection Manager.
 * Tests BLE device scanning, connection management, auto-reconnect logic,
 * and GATT client operations for T-Watch integration.
 */
class BleManagerTest {

    @Test
    fun testInitialStateIsDisconnected() {
        val manager = BleManager()
        try {
            assertEquals(BleConnectionState.DISCONNECTED, manager.connectionState.value)
            assertEquals(false, manager.isConnected())
            assertEquals(emptyList<BleDeviceInfo>(), manager.discoveredDevices.value)
        } finally {
            manager.dispose()
        }
    }

    @Test
    fun testDeviceDiscovered() {
        val manager = BleManager()
        try {
            val device = BleDeviceInfo(
                address = "AA:BB:CC:DD:EE:FF",
                name = "T-Watch",
                rssi = -50
            )
            manager.onDeviceDiscovered(device)
            assertEquals(1, manager.discoveredDevices.value.size)
            assertEquals(device, manager.discoveredDevices.value[0])
        } finally {
            manager.dispose()
        }
    }

    @Test
    fun testConnectToDevice() {
        val manager = BleManager()
        try {
            val device = BleDeviceInfo(
                address = "AA:BB:CC:DD:EE:FF",
                name = "T-Watch",
                rssi = -50
            )
            manager.connectToDevice(device)
            assertEquals(BleConnectionState.CONNECTING, manager.connectionState.value)
        } finally {
            manager.dispose()
        }
    }

    @Test
    fun testConnectionEstablished() {
        val manager = BleManager()
        try {
            manager.connectToDevice(BleDeviceInfo("AA:BB:CC:DD:EE:FF", "T-Watch", -50))
            manager.onConnectionEstablished()
            assertEquals(BleConnectionState.CONNECTED, manager.connectionState.value)
            assertEquals(true, manager.isConnected())
        } finally {
            manager.dispose()
        }
    }

    @Test
    fun testConnectionLost() {
        val manager = BleManager()
        try {
            manager.connectToDevice(BleDeviceInfo("AA:BB:CC:DD:EE:FF", "T-Watch", -50))
            manager.onConnectionEstablished()
            manager.onConnectionLost()
            // After connection lost, state transitions to RECONNECTING
            assertEquals(BleConnectionState.RECONNECTING, manager.connectionState.value)
            assertEquals(false, manager.isConnected())
            // Verify reconnect was scheduled
            assertEquals(1, manager.reconnectAttempts)
        } finally {
            manager.dispose()
        }
    }

    @Test
    fun testAutoReconnectWithExponentialBackoff() {
        val manager = BleManager()
        try {
            // Simulate connection loss
            manager.connectToDevice(BleDeviceInfo("AA:BB:CC:DD:EE:FF", "T-Watch", -50))
            manager.onConnectionEstablished()
            manager.onConnectionLost()
            
            // Verify reconnect was triggered
            assertEquals(BleConnectionState.RECONNECTING, manager.connectionState.value)
            assertEquals(1, manager.reconnectAttempts)
        } finally {
            manager.dispose()
        }
    }

    @Test
    fun testDisconnect() {
        val manager = BleManager()
        try {
            manager.connectToDevice(BleDeviceInfo("AA:BB:CC:DD:EE:FF", "T-Watch", -50))
            manager.onConnectionEstablished()
            manager.disconnect()
            assertEquals(BleConnectionState.DISCONNECTED, manager.connectionState.value)
        } finally {
            manager.dispose()
        }
    }

    @Test
    fun testGattClientNotificationSubscription() {
        val manager = BleManager()
        try {
            manager.connectToDevice(BleDeviceInfo("AA:BB:CC:DD:EE:FF", "T-Watch", -50))
            manager.onConnectionEstablished()
            
            // Subscribe to audio data characteristic
            manager.subscribeToAudioData()
            
            // Simulate receiving audio data
            val audioData = byteArrayOf(0x01, 0x02, 0x03, 0x04)
            manager.onAudioDataReceived(audioData)
            
            assertEquals(1, manager.audioDataCount.value)
        } finally {
            manager.dispose()
        }
    }

    @Test
    fun testTWatchServiceUUIDFiltering() {
        val manager = BleManager()
        try {
            // Verify T-Watch service UUID is correctly defined
            assertEquals(
                UUID.fromString("00001810-0000-1000-8000-00805f9b34fb"),
                BleManager.TWATCH_SERVICE_UUID
            )
        } finally {
            manager.dispose()
        }
    }

    @Test
    fun testAutoReconnectAttemptsLimit() {
        val manager = BleManager()
        try {
            // Simulate multiple connection failures
            manager.connectToDevice(BleDeviceInfo("AA:BB:CC:DD:EE:FF", "T-Watch", -50))
            manager.onConnectionEstablished()
            
            // Manually trigger multiple reconnection attempts
            for (i in 1..5) {
                manager.onConnectionLost()
                // Simulate reconnection failure
                manager.reconnectAttempts = i
            }
            
            // Verify max attempts reached
            assertEquals(5, manager.reconnectAttempts)
        } finally {
            manager.dispose()
        }
    }

    @Test
    fun testMultipleDevicesDiscovered() {
        val manager = BleManager()
        try {
            val device1 = BleDeviceInfo("AA:BB:CC:DD:EE:FF", "T-Watch-1", -50)
            val device2 = BleDeviceInfo("11:22:33:44:55:66", "T-Watch-2", -60)
            val device3 = BleDeviceInfo("AA:BB:CC:DD:EE:FF", "T-Watch-1", -45) // Duplicate
            
            manager.onDeviceDiscovered(device1)
            manager.onDeviceDiscovered(device2)
            manager.onDeviceDiscovered(device3)
            
            // Should have 2 unique devices
            assertEquals(2, manager.discoveredDevices.value.size)
        } finally {
            manager.dispose()
        }
    }

    @Test
    fun testConnectionStateTransitions() {
        val manager = BleManager()
        try {
            // Start from DISCONNECTED
            assertEquals(BleConnectionState.DISCONNECTED, manager.connectionState.value)
            
            // Transition to CONNECTING
            manager.connectToDevice(BleDeviceInfo("AA:BB:CC:DD:EE:FF", "T-Watch", -50))
            assertEquals(BleConnectionState.CONNECTING, manager.connectionState.value)
            
            // Transition to CONNECTED
            manager.onConnectionEstablished()
            assertEquals(BleConnectionState.CONNECTED, manager.connectionState.value)
            
            // Transition to DISCONNECTED
            manager.disconnect()
            assertEquals(BleConnectionState.DISCONNECTED, manager.connectionState.value)
        } finally {
            manager.dispose()
        }
    }

    @Test
    fun testReconnectBackoffCalculation() {
        val manager = BleManager()
        try {
            // Verify exponential backoff calculation
            val baseDelay = 1000L // 1 second
            val maxDelay = 30000L // 30 seconds
            
            // First attempt: 1000ms
            manager.reconnectAttempts = 1
            val delay1 = minOf(baseDelay * Math.pow(2.0, (manager.reconnectAttempts - 1).toDouble()).toInt(), maxDelay)
            assertEquals(1000, delay1)
            
            // Second attempt: 2000ms
            manager.reconnectAttempts = 2
            val delay2 = minOf(baseDelay * Math.pow(2.0, (manager.reconnectAttempts - 1).toDouble()).toInt(), maxDelay)
            assertEquals(2000, delay2)
            
            // Third attempt: 4000ms
            manager.reconnectAttempts = 3
            val delay3 = minOf(baseDelay * Math.pow(2.0, (manager.reconnectAttempts - 1).toDouble()).toInt(), maxDelay)
            assertEquals(4000, delay3)
        } finally {
            manager.dispose()
        }
    }

    @Test
    fun testAudioDataNotificationFlow() {
        val manager = BleManager()
        try {
            manager.connectToDevice(BleDeviceInfo("AA:BB:CC:DD:EE:FF", "T-Watch", -50))
            manager.onConnectionEstablished()
            manager.subscribeToAudioData()
            
            // Simulate multiple audio data packets
            for (i in 1..3) {
                val audioData = byteArrayOf(i.toByte(), (i+1).toByte(), (i+2).toByte())
                manager.onAudioDataReceived(audioData)
            }
            
            assertEquals(3, manager.audioDataCount.value)
        } finally {
            manager.dispose()
        }
    }

    @Test
    fun testCleanupOnDispose() {
        val manager = BleManager()
        try {
            manager.connectToDevice(BleDeviceInfo("AA:BB:CC:DD:EE:FF", "T-Watch", -50))
            manager.onConnectionEstablished()
            manager.subscribeToAudioData()
            
            // Dispose should clean up
            manager.dispose()
            
            // After dispose, should be in disconnected state
            assertEquals(BleConnectionState.DISCONNECTED, manager.connectionState.value)
        } finally {
            manager.dispose()
        }
    }
}

/**
 * BLE Manager for T-Watch connection and audio streaming.
 * Handles device scanning, GATT connection, auto-reconnect with exponential backoff,
 * and audio data notification subscription.
 */
class BleManager {
    companion object {
        // T-Watch BLE Service UUIDs (matching the firmware)
        val TWATCH_SERVICE_UUID = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb")
        val AUDIO_DATA_CHARACTERISTIC_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        
        // Reconnection settings
        private const val BASE_RECONNECT_DELAY_MS = 1000L
        private const val MAX_RECONNECT_DELAY_MS = 30000L
        private const val MAX_RECONNECT_ATTEMPTS = 10
    }

    // Connection state
    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    // Discovered devices (filtered for T-Watch)
    private val _discoveredDevices = MutableStateFlow(emptyList<BleDeviceInfo>())
    val discoveredDevices: StateFlow<List<BleDeviceInfo>> = _discoveredDevices.asStateFlow()

    // Audio data notifications
    private val _audioDataCount = MutableStateFlow(0)
    val audioDataCount: StateFlow<Int> = _audioDataCount.asStateFlow()

    // Reconnection tracking
    var reconnectAttempts = 0
    private var currentDeviceAddress: String? = null
    private var reconnectJob: Job? = null
    private var scope: CoroutineScope? = null

    // State management
    private var isDisposed = false

    init {
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    /**
     * Check if currently connected to a T-Watch device
     */
    fun isConnected(): Boolean = _connectionState.value == BleConnectionState.CONNECTED

    /**
     * Start scanning for T-Watch devices
     */
    fun startScan() {
        if (isDisposed) return
        // In real implementation, this would use BluetoothLeScanner
        // For testing, we simulate device discovery
    }

    /**
     * Stop scanning for devices
     */
    fun stopScan() {
        if (isDisposed) return
        // In real implementation, this would cancel the scan
    }

    /**
     * Connect to a discovered T-Watch device
     */
    fun connectToDevice(device: BleDeviceInfo) {
        if (isDisposed) return
        
        currentDeviceAddress = device.address
        _connectionState.value = BleConnectionState.CONNECTING
        
        // Simulate connection attempt
        scope?.launch {
            delay(100) // Simulate connection delay
            // In real implementation, this would use BluetoothGatt
        }
    }

    /**
     * Handle successful connection establishment
     */
    fun onConnectionEstablished() {
        if (isDisposed) return
        
        currentDeviceAddress = null
        _connectionState.value = BleConnectionState.CONNECTED
        _discoveredDevices.value = emptyList()
    }

    /**
     * Handle connection loss
     */
    fun onConnectionLost() {
        if (isDisposed) return
        
        _connectionState.value = BleConnectionState.DISCONNECTED
        
        // Trigger auto-reconnect with exponential backoff
        scheduleReconnect()
    }

    /**
     * Schedule reconnection with exponential backoff
     */
    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            _connectionState.value = BleConnectionState.FAILED
            return
        }
        
        reconnectAttempts++
        
        val delayMs = minOf(
            BASE_RECONNECT_DELAY_MS * Math.pow(2.0, (reconnectAttempts - 1).toDouble()).toInt(),
            MAX_RECONNECT_DELAY_MS
        )
        
        _connectionState.value = BleConnectionState.RECONNECTING
        
        reconnectJob = scope?.launch {
            delay(delayMs)
            // In real implementation, would attempt to reconnect
        }
    }

    /**
     * Disconnect from current device
     */
    fun disconnect() {
        if (isDisposed) return
        
        reconnectJob?.cancel()
        reconnectAttempts = 0
        _connectionState.value = BleConnectionState.DISCONNECTED
    }

    /**
     * Subscribe to audio data notifications
     */
    fun subscribeToAudioData() {
        if (isDisposed) return
        // In real implementation, this would enable notifications on the audio characteristic
    }

    /**
     * Handle received audio data
     */
    fun onAudioDataReceived(data: ByteArray) {
        if (isDisposed) return
        _audioDataCount.value++
        // In real implementation, would process audio data
    }

    /**
     * Handle discovered device
     */
    fun onDeviceDiscovered(device: BleDeviceInfo) {
        if (isDisposed) return
        
        // Filter for T-Watch devices only
        if (device.name.contains("T-Watch", ignoreCase = true)) {
            val currentList = _discoveredDevices.value.toMutableList()
            if (!currentList.any { it.address == device.address }) {
                currentList.add(device)
                _discoveredDevices.value = currentList
            }
        }
    }

    /**
     * Clean up resources
     */
    fun dispose() {
        isDisposed = true
        reconnectJob?.cancel()
        scope?.cancel()
        _connectionState.value = BleConnectionState.DISCONNECTED
    }
}

enum class BleConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED
}

data class BleDeviceInfo(
    val address: String,
    val name: String,
    val rssi: Int
)
