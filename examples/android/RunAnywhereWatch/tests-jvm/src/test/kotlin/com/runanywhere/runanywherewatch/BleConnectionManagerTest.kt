package com.runanywhere.runanywherewatch

import org.junit.Test
import org.junit.Before
import org.junit.Assert.*

// ==================== BLE UUIDs ====================

object BleUuids {
    const val T_WATCH_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb"
    const val AUDIO_DATA_CHAR_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"
}

// ==================== BLE State Machine ====================

enum class BleConnectionState {
    DISCONNECTED, SCANNING, CONNECTING, CONNECTED, DISCONNECTED_AFTER_ERROR
}

enum class BleScanFilterMode { ALL_DEVICES, T_WATCH_ONLY }

// ==================== Mock BLE Devices ====================

data class BleDevice(
    val address: String,
    val name: String,
    val rssi: Int,
    val serviceUuids: List<String>
) {
    fun hasTWatchService(): Boolean = serviceUuids.contains(BleUuids.T_WATCH_SERVICE_UUID)
}

// ==================== BLE Scanner ====================

class BleScanner(
    private val scanFilterMode: BleScanFilterMode = BleScanFilterMode.T_WATCH_ONLY,
    private val scanResultCallback: ((List<BleDevice>) -> Unit)? = null
) {
    private var isScanning: Boolean = false
    private val discoveredDevices = mutableListOf<BleDevice>()
    private var scanCallback: ((List<BleDevice>) -> Unit)? = null

    fun startScan(callback: (List<BleDevice>) -> Unit) {
        if (isScanning) return
        isScanning = true
        discoveredDevices.clear()
        scanCallback = callback
        simulateScanResults()
    }

    fun stopScan() { isScanning = false }
    fun isScanning(): Boolean = isScanning
    fun getDiscoveredDevices(): List<BleDevice> = discoveredDevices.toList()

    private fun simulateScanResults() {
        val mockDevices = listOf(
            BleDevice("AA:BB:CC:DD:EE:01", "T-Watch-01", -65, listOf(BleUuids.T_WATCH_SERVICE_UUID)),
            BleDevice("AA:BB:CC:DD:EE:02", "T-Watch-02", -72, listOf(BleUuids.T_WATCH_SERVICE_UUID)),
            BleDevice("AA:BB:CC:DD:EE:03", "OtherDevice", -80, listOf("0000110a-0000-1000-8000-00805f9b34fb"))
        )
        discoveredDevices.addAll(mockDevices.filter { device ->
            when (scanFilterMode) {
                BleScanFilterMode.T_WATCH_ONLY -> device.hasTWatchService()
                BleScanFilterMode.ALL_DEVICES -> true
            }
        })
        isScanning = false
        scanCallback?.invoke(discoveredDevices)
    }
}

// ==================== GATT Client ====================

class BleGattClient(
    private val device: BleDevice,
    private val audioDataCallback: ((ByteArray) -> Unit)? = null,
    private val connectionStateCallback: ((BleConnectionState) -> Unit)? = null
) {
    private var isConnected: Boolean = false
    private var isSubscribed: Boolean = false
    private val notifications = mutableListOf<ByteArray>()

    fun connect(): Boolean {
        if (isConnected) return false
        isConnected = true
        connectionStateCallback?.invoke(BleConnectionState.CONNECTED)
        return true
    }

    fun disconnect() {
        isConnected = false
        isSubscribed = false
        notifications.clear()
        connectionStateCallback?.invoke(BleConnectionState.DISCONNECTED)
    }

    fun subscribeToAudioData(): Boolean {
        if (!isConnected) return false
        isSubscribed = true
        return true
    }

    fun unsubscribeFromAudioData() { isSubscribed = false }
    fun isSubscribed(): Boolean = isSubscribed
    fun isConnected(): Boolean = isConnected

    fun simulateNotification(data: ByteArray) {
        if (!isSubscribed) return
        notifications.add(data.clone())
        audioDataCallback?.invoke(data)
    }

    fun getNotifications(): List<ByteArray> = notifications.toList()
    fun clearNotifications() { notifications.clear() }
}

// ==================== Connection Manager ====================

class ConnectionManagerConfig(
    val maxReconnectAttempts: Int = 5,
    val initialBackoffMs: Long = 1000,
    val maxBackoffMs: Long = 30000,
    val backoffMultiplier: Double = 2.0
)

class BleConnectionManager(
    private val config: ConnectionManagerConfig = ConnectionManagerConfig(),
    private val scanner: BleScanner = BleScanner(),
    private val onConnectionStateChanged: ((BleConnectionState) -> Unit)? = null
) {
    private var currentState: BleConnectionState = BleConnectionState.DISCONNECTED
    private var currentDevice: BleDevice? = null
    private var gattClient: BleGattClient? = null
    private var reconnectAttempts: Int = 0
    private var currentBackoffMs: Long = config.initialBackoffMs
    private var isAutoReconnectEnabled: Boolean = true

    fun connectToDevice(device: BleDevice): Boolean {
        if (currentState == BleConnectionState.CONNECTED || currentState == BleConnectionState.SCANNING) return false
        currentDevice = device
        currentState = BleConnectionState.CONNECTING
        onConnectionStateChanged?.invoke(currentState)
        gattClient = BleGattClient(device)
        val connected = gattClient?.connect() ?: false
        if (connected) {
            currentState = BleConnectionState.CONNECTED
            onConnectionStateChanged?.invoke(currentState)
            reconnectAttempts = 0
            currentBackoffMs = config.initialBackoffMs
        } else {
            currentState = BleConnectionState.DISCONNECTED_AFTER_ERROR
            onConnectionStateChanged?.invoke(currentState)
            if (isAutoReconnectEnabled) scheduleReconnect()
        }
        return connected
    }

    fun disconnect() {
        gattClient?.disconnect()
        gattClient = null
        currentDevice = null
        currentState = BleConnectionState.DISCONNECTED
        onConnectionStateChanged?.invoke(currentState)
    }

    fun subscribeToAudioData(): Boolean {
        if (currentState != BleConnectionState.CONNECTED) return false
        return gattClient?.subscribeToAudioData() ?: false
    }

    fun unsubscribeFromAudioData() { gattClient?.unsubscribeFromAudioData() }
    fun startScanning(callback: (List<BleDevice>) -> Unit) {
        currentState = BleConnectionState.SCANNING
        onConnectionStateChanged?.invoke(currentState)
        scanner.startScan(callback)
    }
    fun stopScanning() {
        scanner.stopScan()
        currentState = BleConnectionState.DISCONNECTED
        onConnectionStateChanged?.invoke(currentState)
    }
    fun isScanning(): Boolean = currentState == BleConnectionState.SCANNING
    fun isConnected(): Boolean = currentState == BleConnectionState.CONNECTED
    fun getCurrentState(): BleConnectionState = currentState
    fun getCurrentDevice(): BleDevice? = currentDevice
    fun getGattClient(): BleGattClient? = gattClient
    fun enableAutoReconnect(enable: Boolean) { isAutoReconnectEnabled = enable }
    fun isAutoReconnectEnabled(): Boolean = isAutoReconnectEnabled
    fun getCurrentBackoffMs(): Long = currentBackoffMs

    private fun scheduleReconnect() {
        if (reconnectAttempts >= config.maxReconnectAttempts) {
            currentState = BleConnectionState.DISCONNECTED
            onConnectionStateChanged?.invoke(currentState)
            return
        }
        reconnectAttempts++
        val backoffTime = currentBackoffMs
        currentBackoffMs = (currentBackoffMs * config.backoffMultiplier).toLong().coerceAtMost(config.maxBackoffMs)
        simulateReconnect(backoffTime)
    }

    private fun simulateReconnect(delayMs: Long) {
        currentDevice?.let { device ->
            gattClient = BleGattClient(device)
            val connected = gattClient?.connect() ?: false
            if (connected) {
                currentState = BleConnectionState.CONNECTED
                onConnectionStateChanged?.invoke(currentState)
                reconnectAttempts = 0
                currentBackoffMs = config.initialBackoffMs
            } else scheduleReconnect()
        }
    }

    fun setReconnectAttempts(attempts: Int) { reconnectAttempts = attempts }
    fun getReconnectAttempts(): Int = reconnectAttempts

    fun simulateConnectionFailure() {
        gattClient?.disconnect()
        gattClient = null
        currentDevice = null
        currentState = BleConnectionState.DISCONNECTED_AFTER_ERROR
        onConnectionStateChanged?.invoke(currentState)
        if (isAutoReconnectEnabled) scheduleReconnect()
    }
}

// ==================== TESTS ====================

class BleScannerTest {
    private lateinit var scanner: BleScanner

    @Before fun setUp() { scanner = BleScanner() }

    @Test fun initial_state_is_not_scanning() {
        assertFalse(scanner.isScanning())
        assertTrue(scanner.getDiscoveredDevices().isEmpty())
    }

    @Test fun startScan_discovers_devices() {
        val devices = mutableListOf<BleDevice>()
        scanner.startScan { devices.addAll(it) }
        assertTrue(devices.isNotEmpty())
        assertTrue(devices.any { it.hasTWatchService() })
    }

    @Test fun T_WATCH_ONLY_filter_excludes_non_T_Watch_devices() {
        val devices = mutableListOf<BleDevice>()
        scanner.startScan { devices.addAll(it) }
        assertEquals(2, devices.filter { it.hasTWatchService() }.size)
    }

    @Test fun stopScan_stops_scanning() {
        scanner.startScan { }
        scanner.stopScan()
        assertFalse(scanner.isScanning())
    }

    @Test fun startScan_with_ALL_DEVICES_filter_includes_all() {
        val allScanner = BleScanner(scanFilterMode = BleScanFilterMode.ALL_DEVICES)
        val devices = mutableListOf<BleDevice>()
        allScanner.startScan { devices.addAll(it) }
        assertEquals(3, devices.size)
    }
}

class BleGattClientTest {
    private lateinit var client: BleGattClient
    private lateinit var device: BleDevice

    @Before fun setUp() {
        device = BleDevice("AA:BB:CC:DD:EE:01", "T-Watch-01", -65, listOf(BleUuids.T_WATCH_SERVICE_UUID))
        client = BleGattClient(device)
    }

    @Test fun initial_state_is_not_connected() {
        assertFalse(client.isConnected())
        assertFalse(client.isSubscribed())
    }

    @Test fun connect_sets_connected_state() {
        assertTrue(client.connect())
        assertTrue(client.isConnected())
    }

    @Test fun connect_fails_when_already_connected() {
        client.connect()
        assertFalse(client.connect())
    }

    @Test fun disconnect_clears_state() {
        client.connect()
        client.disconnect()
        assertFalse(client.isConnected())
        assertFalse(client.isSubscribed())
    }

    @Test fun subscribeToAudioData_requires_connection() {
        assertFalse(client.subscribeToAudioData())
        client.connect()
        assertTrue(client.subscribeToAudioData())
    }

    @Test fun simulateNotification_only_works_when_subscribed() {
        client.connect()
        client.simulateNotification(ByteArray(10))
        assertTrue(client.getNotifications().isEmpty())
        client.subscribeToAudioData()
        client.simulateNotification(ByteArray(20))
        assertEquals(1, client.getNotifications().size)
    }
}

class BleConnectionManagerTest {
    private lateinit var manager: BleConnectionManager

    @Before fun setUp() { manager = BleConnectionManager() }

    @Test fun initial_state_is_DISCONNECTED() {
        assertEquals(BleConnectionState.DISCONNECTED, manager.getCurrentState())
        assertNull(manager.getCurrentDevice())
    }

    @Test fun connectToDevice_transitions_to_CONNECTED() {
        val device = BleDevice("AA:BB:CC:DD:EE:01", "T-Watch-01", -65, listOf(BleUuids.T_WATCH_SERVICE_UUID))
        assertTrue(manager.connectToDevice(device))
        assertEquals(BleConnectionState.CONNECTED, manager.getCurrentState())
        assertNotNull(manager.getGattClient())
    }

    @Test fun connectToDevice_fails_when_already_connected() {
        val device = BleDevice("AA:BB:CC:DD:EE:01", "T-Watch-01", -65, listOf(BleUuids.T_WATCH_SERVICE_UUID))
        manager.connectToDevice(device)
        assertFalse(manager.connectToDevice(device))
    }

    @Test fun disconnect_clears_state() {
        val device = BleDevice("AA:BB:CC:DD:EE:01", "T-Watch-01", -65, listOf(BleUuids.T_WATCH_SERVICE_UUID))
        manager.connectToDevice(device)
        manager.disconnect()
        assertEquals(BleConnectionState.DISCONNECTED, manager.getCurrentState())
        assertNull(manager.getCurrentDevice())
    }

    @Test fun subscribeToAudioData_requires_connection() {
        assertFalse(manager.subscribeToAudioData())
        val device = BleDevice("AA:BB:CC:DD:EE:01", "T-Watch-01", -65, listOf(BleUuids.T_WATCH_SERVICE_UUID))
        manager.connectToDevice(device)
        assertTrue(manager.subscribeToAudioData())
    }

    @Test fun startScanning_sets_scanning_state() {
        manager.startScanning { }
        assertTrue(manager.isScanning())
        assertEquals(BleConnectionState.SCANNING, manager.getCurrentState())
    }

    @Test fun stopScanning_clears_scanning_state() {
        manager.startScanning { }
        manager.stopScanning()
        assertFalse(manager.isScanning())
        assertEquals(BleConnectionState.DISCONNECTED, manager.getCurrentState())
    }

    @Test fun auto_reconnect_on_connection_failure() {
        val device = BleDevice("AA:BB:CC:DD:EE:01", "T-Watch-01", -65, listOf(BleUuids.T_WATCH_SERVICE_UUID))
        manager.connectToDevice(device)
        assertEquals(BleConnectionState.CONNECTED, manager.getCurrentState())
        manager.simulateConnectionFailure()
        assertEquals(BleConnectionState.CONNECTED, manager.getCurrentState())
    }

    @Test fun reconnect_attempts_limited_by_config() {
        val config = ConnectionManagerConfig(maxReconnectAttempts = 2, initialBackoffMs = 100)
        val testManager = BleConnectionManager(config)
        val device = BleDevice("AA:BB:CC:DD:EE:01", "T-Watch-01", -65, listOf(BleUuids.T_WATCH_SERVICE_UUID))
        testManager.connectToDevice(device)
        testManager.simulateConnectionFailure()
        testManager.simulateConnectionFailure()
        assertEquals(2, testManager.getReconnectAttempts())
        testManager.simulateConnectionFailure()
        assertEquals(BleConnectionState.DISCONNECTED, testManager.getCurrentState())
    }

    @Test fun exponential_backoff_increases_delay() {
        val config = ConnectionManagerConfig(maxReconnectAttempts = 10, initialBackoffMs = 100, backoffMultiplier = 2.0)
        val testManager = BleConnectionManager(config)
        val device = BleDevice("AA:BB:CC:DD:EE:01", "T-Watch-01", -65, listOf(BleUuids.T_WATCH_SERVICE_UUID))
        testManager.connectToDevice(device)
        testManager.simulateConnectionFailure()
        testManager.simulateConnectionFailure()
        assertTrue(testManager.getCurrentBackoffMs() >= config.initialBackoffMs * 2)
    }

    @Test fun disable_auto_reconnect_prevents_reconnection() {
        val device = BleDevice("AA:BB:CC:DD:EE:01", "T-Watch-01", -65, listOf(BleUuids.T_WATCH_SERVICE_UUID))
        manager.connectToDevice(device)
        manager.enableAutoReconnect(false)
        manager.simulateConnectionFailure()
        assertEquals(BleConnectionState.DISCONNECTED_AFTER_ERROR, manager.getCurrentState())
    }

    @Test fun connectToDevice_fails_when_scanning() {
        manager.startScanning { }
        val device = BleDevice("AA:BB:CC:DD:EE:01", "T-Watch-01", -65, listOf(BleUuids.T_WATCH_SERVICE_UUID))
        assertFalse(manager.connectToDevice(device))
        assertEquals(BleConnectionState.SCANNING, manager.getCurrentState())
    }
}

class BleConnectionIntegrationTest {
    private lateinit var scanner: BleScanner
    private lateinit var manager: BleConnectionManager
    private var discoveredDevices: List<BleDevice> = emptyList()

    @Before fun setUp() {
        scanner = BleScanner()
        manager = BleConnectionManager(scanner = scanner)
    }

    @Test fun full_workflow_scan_connect_subscribe_disconnect() {
        scanner.startScan { discoveredDevices = it }
        assertTrue(discoveredDevices.isNotEmpty())
        val device = discoveredDevices.first { it.hasTWatchService() }
        assertTrue(manager.connectToDevice(device))
        assertEquals(BleConnectionState.CONNECTED, manager.getCurrentState())
        assertTrue(manager.subscribeToAudioData())
        manager.disconnect()
        assertEquals(BleConnectionState.DISCONNECTED, manager.getCurrentState())
    }

    @Test fun cannot_connect_while_scanning() {
        scanner.startScan { discoveredDevices = it }
        assertEquals(BleConnectionState.SCANNING, manager.getCurrentState())
        val device = discoveredDevices.firstOrNull { it.hasTWatchService() }
        if (device != null) {
            assertFalse(manager.connectToDevice(device))
            assertEquals(BleConnectionState.SCANNING, manager.getCurrentState())
        }
    }
}

class BleConnectionEdgeCasesTest {
    @Test fun disconnect_when_not_connected_is_safe() {
        val manager = BleConnectionManager()
        manager.disconnect()
        assertEquals(BleConnectionState.DISCONNECTED, manager.getCurrentState())
    }

    @Test fun subscribe_when_not_connected_returns_false() {
        val manager = BleConnectionManager()
        assertFalse(manager.subscribeToAudioData())
    }

    @Test fun unsubscribing_clears_subscription_state() {
        val manager = BleConnectionManager()
        val device = BleDevice("AA:BB:CC:DD:EE:01", "T-Watch-01", -65, listOf(BleUuids.T_WATCH_SERVICE_UUID))
        manager.connectToDevice(device)
        manager.subscribeToAudioData()
        manager.unsubscribeFromAudioData()
        val gattClient = manager.getGattClient()
        assertFalse(gattClient?.isSubscribed() ?: false)
    }

    @Test fun backoff_capped_at_max_value() {
        val config = ConnectionManagerConfig(maxReconnectAttempts = 10, initialBackoffMs = 100, maxBackoffMs = 500, backoffMultiplier = 3.0)
        val testManager = BleConnectionManager(config)
        val device = BleDevice("AA:BB:CC:DD:EE:01", "T-Watch-01", -65, listOf(BleUuids.T_WATCH_SERVICE_UUID))
        testManager.connectToDevice(device)
        testManager.simulateConnectionFailure()
        testManager.simulateConnectionFailure()
        testManager.simulateConnectionFailure()
        assertTrue(testManager.getCurrentBackoffMs() <= config.maxBackoffMs)
    }
}
