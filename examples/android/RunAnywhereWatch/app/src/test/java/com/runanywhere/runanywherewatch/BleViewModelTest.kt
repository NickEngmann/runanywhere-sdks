package com.runanywhere.runanywherewatch

import android.bluetooth.*
import android.content.Context
import android.os.Build
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for BleViewModel
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class BleViewModelTest {
    
    private lateinit var context: Context
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var bleScanner: BleScanner
    private lateinit var bleConnectionManager: BleConnectionManager
    private lateinit var viewModel: BleViewModel
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        bluetoothManager = mockk(relaxed = true)
        bluetoothAdapter = mockk(relaxed = true)
        bluetoothLeScanner = mockk(relaxed = true)
        
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns bluetoothManager
        every { bluetoothManager.adapter } returns bluetoothAdapter
        every { bluetoothAdapter.bluetoothLeScanner } returns bluetoothLeScanner
        every { bluetoothAdapter.isDiscovering } returns false
        
        // Mock scanner and connection manager
        bleScanner = mockk(relaxed = true)
        bleConnectionManager = mockk(relaxed = true)
        
        every { bleScanner.startScan() } returns Unit
        every { bleScanner.stopScan() } returns Unit
        every { bleScanner.cleanup() } returns Unit
        every { bleConnectionManager.connect(any()) } returns true
        every { bleConnectionManager.disconnect() } returns Unit
        every { bleConnectionManager.isConnected() } returns false
        every { bleConnectionManager.cleanup() } returns Unit
    }
    
    @Test
    fun `test viewModel initializes with disconnected state`() {
        viewModel = BleViewModel(context, bleScanner, bleConnectionManager)
        
        val state = viewModel.state.value
        assertNotNull(state)
        assertFalse(state!!.isConnected)
        assertFalse(state.isConnecting)
        assertTrue(state.scanResults.isEmpty())
    }
    
    @Test
    fun `test startScan updates state and calls scanner`() = runTest {
        viewModel = BleViewModel(context, bleScanner, bleConnectionManager)
        
        var callbackState: BleState? = null
        val callback = object : BleStateCallback {
            override fun onStateChange(state: BleState) { callbackState = state }
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        }
        
        // Create new viewModel with callback
        viewModel = BleViewModel(context, bleScanner, bleConnectionManager, callback)
        
        viewModel.startScan()
        
        verify { bleScanner.startScan() }
        assertNotNull(callbackState)
        assertTrue(callbackState!!.scanResults.isEmpty())
    }
    
    @Test
    fun `test stopScan calls scanner cleanup`() = runTest {
        viewModel = BleViewModel(context, bleScanner, bleConnectionManager)
        
        viewModel.stopScan()
        
        verify { bleScanner.stopScan() }
        verify { bleScanner.cleanup() }
    }
    
    @Test
    fun `test connect calls connection manager and updates state`() = runTest {
        viewModel = BleViewModel(context, bleScanner, bleConnectionManager)
        
        var callbackState: BleState? = null
        val callback = object : BleStateCallback {
            override fun onStateChange(state: BleState) { callbackState = state }
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        }
        
        viewModel = BleViewModel(context, bleScanner, bleConnectionManager, callback)
        
        val result = viewModel.connect("00:00:00:00:00:01")
        
        assertTrue(result)
        verify { bleConnectionManager.connect("00:00:00:00:00:01") }
        assertNotNull(callbackState)
        assertTrue(callbackState!!.isConnecting)
    }
    
    @Test
    fun `test disconnect calls connection manager cleanup`() = runTest {
        viewModel = BleViewModel(context, bleScanner, bleConnectionManager)
        
        viewModel.disconnect()
        
        verify { bleConnectionManager.disconnect() }
        verify { bleConnectionManager.cleanup() }
    }
    
    @Test
    fun `test scan results are collected from callback`() = runTest {
        var callbackState: BleState? = null
        var callbackResult: ScanResult? = null
        
        val callback = object : BleStateCallback {
            override fun onStateChange(state: BleState) { callbackState = state }
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) { callbackResult = result }
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        }
        
        viewModel = BleViewModel(context, bleScanner, bleConnectionManager, callback)
        
        // Simulate device found
        val mockDevice = mockk<BluetoothDevice>()
        val mockScanRecord = mockk<ScanRecord>()
        val mockServiceData = mutableMapOf<ParcelUuid, ByteArray>()
        mockServiceData[ParcelUuid.fromString(BleGattConstants.TWATCH_SERVICE_UUID)] = byteArrayOf(1, 2, 3)
        
        every { mockDevice.address } returns "00:00:00:00:00:01"
        every { mockDevice.name } returns "T-Watch"
        every { mockScanRecord.serviceData } returns mockServiceData
        
        bleScanner.onScanResults(mockDevice, mockScanRecord, -60)
        
        assertNotNull(callbackResult)
        assertEquals("00:00:00:00:00:01", callbackResult!!.deviceAddress)
        assertEquals("T-Watch", callbackResult!!.deviceName)
        assertEquals(-60, callbackResult!!.rssi)
    }
    
    @Test
    fun `test connection state updates on callback`() = runTest {
        var callbackState: BleState? = null
        var connectedDevice: String? = null
        
        val callback = object : BleStateCallback {
            override fun onStateChange(state: BleState) { callbackState = state }
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {
                connectedDevice = deviceName
            }
        }
        
        viewModel = BleViewModel(context, bleScanner, bleConnectionManager, callback)
        
        // Simulate connection state change
        bleConnectionManager.connect("00:00:00:00:00:01")
        every { bleConnectionManager.isConnected() } returns true
        
        // Trigger callback
        callback.onConnectionStateChanged(true, "00:00:00:00:00:01")
        
        assertNotNull(callbackState)
        assertTrue(callbackState!!.isConnected)
        assertEquals("00:00:00:00:00:01", connectedDevice)
    }
    
    @Test
    fun `test error handling updates state`() = runTest {
        var callbackState: BleState? = null
        var errorCode: Int = 0
        var errorMessage: String = ""
        
        val callback = object : BleStateCallback {
            override fun onStateChange(state: BleState) { callbackState = state }
            override fun onError(errorCode: Int, errorMessage: String) {
                this.errorCode = errorCode
                this.errorMessage = errorMessage
            }
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        }
        
        viewModel = BleViewModel(context, bleScanner, bleConnectionManager, callback)
        
        // Simulate error
        callback.onError(BleGattConstants.ERROR_TIMEOUT, "Connection timeout")
        
        assertEquals(BleGattConstants.ERROR_TIMEOUT, errorCode)
        assertEquals("Connection timeout", errorMessage)
        assertNotNull(callbackState)
        assertEquals("Connection timeout", callbackState!!.errorMessage)
    }
    
    @Test
    fun `test cleanup stops scanning and disconnects`() = runTest {
        viewModel = BleViewModel(context, bleScanner, bleConnectionManager)
        
        viewModel.cleanup()
        
        verify { bleScanner.stopScan() }
        verify { bleScanner.cleanup() }
        verify { bleConnectionManager.disconnect() }
        verify { bleConnectionManager.cleanup() }
    }
    
    @Test
    fun `test connect returns false when connection fails`() = runTest {
        viewModel = BleViewModel(context, bleScanner, bleConnectionManager)
        
        every { bleConnectionManager.connect(any()) } returns false
        
        val result = viewModel.connect("00:00:00:00:00:01")
        
        assertFalse(result)
    }
    
    @Test
    fun `test viewModel observes state changes`() = runTest {
        viewModel = BleViewModel(context, bleScanner, bleConnectionManager)
        
        var observedState: BleState? = null
        
        viewModel.state.observeForever { state ->
            observedState = state
        }
        
        // Trigger state change
        val callback = object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        }
        
        viewModel = BleViewModel(context, bleScanner, bleConnectionManager, callback)
        
        assertNotNull(observedState)
        assertFalse(observedState!!.isConnected)
    }
}
