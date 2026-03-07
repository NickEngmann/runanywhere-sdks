package com.runanywhere.runanywherewatch

import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for BleScanner
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class BleScannerTest {
    
    private lateinit var context: Context
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var bleScanner: BleScanner
    
    @Before
    fun setup() {
        // Mock Android context
        context = mockk(relaxed = true)
        bluetoothManager = mockk(relaxed = true)
        bluetoothAdapter = mockk(relaxed = true)
        bluetoothLeScanner = mockk(relaxed = true)
        
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns bluetoothManager
        every { bluetoothManager.adapter } returns bluetoothAdapter
        every { bluetoothAdapter.bluetoothLeScanner } returns bluetoothLeScanner
        every { bluetoothAdapter.isDiscovering } returns false
    }
    
    @Test
    fun `test scanner initializes correctly`() {
        bleScanner = BleScanner(context, object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        })
        
        assertNotNull(bleScanner)
    }
    
    @Test
    fun `test startScan enables scanning`() {
        bleScanner = BleScanner(context, object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        })
        
        bleScanner.startScan()
        
        verify { bluetoothLeScanner.startScan(any()) }
    }
    
    @Test
    fun `test startScan with filter`() {
        bleScanner = BleScanner(context, object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        })
        
        bleScanner.startScan()
        
        // Verify scan filter was set up
        verify { bluetoothLeScanner.startScan(capture<ScanFilter>()) }
    }
    
    @Test
    fun `test stopScan disables scanning`() {
        bleScanner = BleScanner(context, object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        })
        
        bleScanner.stopScan()
        
        verify { bluetoothLeScanner.stopScan(any()) }
    }
    
    @Test
    fun `test scan filter matches T-Watch service UUID`() {
        bleScanner = BleScanner(context, object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        })
        
        // Create a mock scan result with T-Watch service
        val mockDevice = mockk<BluetoothDevice>()
        val mockScanRecord = mockk<ScanRecord>()
        val mockServiceData = mutableMapOf<ParcelUuid, ByteArray>()
        mockServiceData[ParcelUuid.fromString(BleGattConstants.TWATCH_SERVICE_UUID)] = byteArrayOf(1, 2, 3)
        
        every { mockDevice.address } returns "00:00:00:00:00:01"
        every { mockDevice.name } returns "T-Watch"
        every { mockScanRecord.serviceData } returns mockServiceData
        every { mockScanRecord.rssi } returns -60
        
        bleScanner.onScanResults(mockDevice, mockScanRecord, -60)
        
        // Verify callback was called with correct result
        verify(exactly = 1) { bleScanner.callback.onDeviceFound(any()) }
    }
    
    @Test
    fun `test scan filter does not match non-T-Watch device`() {
        bleScanner = BleScanner(context, object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        })
        
        val mockDevice = mockk<BluetoothDevice>()
        val mockScanRecord = mockk<ScanRecord>()
        
        every { mockDevice.address } returns "00:00:00:00:00:02"
        every { mockDevice.name } returns "Other Device"
        every { mockScanRecord.serviceData } returns emptyMap()
        
        bleScanner.onScanResults(mockDevice, mockScanRecord, -70)
        
        // Verify no device was reported
        verify(exactly = 0) { bleScanner.callback.onDeviceFound(any()) }
    }
    
    @Test
    fun `test cleanup stops scanning`() {
        bleScanner = BleScanner(context, object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        })
        
        bleScanner.startScan()
        bleScanner.cleanup()
        
        verify { bluetoothLeScanner.stopScan(any()) }
    }
    
    @Test
    fun `test scan results are filtered by RSSI threshold`() {
        bleScanner = BleScanner(context, object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        })
        
        val mockDevice = mockk<BluetoothDevice>()
        val mockScanRecord = mockk<ScanRecord>()
        val mockServiceData = mutableMapOf<ParcelUuid, ByteArray>()
        mockServiceData[ParcelUuid.fromString(BleGattConstants.TWATCH_SERVICE_UUID)] = byteArrayOf(1, 2, 3)
        
        every { mockDevice.address } returns "00:00:00:00:00:01"
        every { mockDevice.name } returns "T-Watch"
        every { mockScanRecord.serviceData } returns mockServiceData
        
        // Strong signal should be reported
        bleScanner.onScanResults(mockDevice, mockScanRecord, -50)
        verify(exactly = 1) { bleScanner.callback.onDeviceFound(any()) }
        
        // Reset and test weak signal
        unmockkAll()
        setup()
        bleScanner = BleScanner(context, object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        })
        
        every { mockDevice.address } returns "00:00:00:00:00:01"
        every { mockDevice.name } returns "T-Watch"
        every { mockScanRecord.serviceData } returns mockServiceData
        
        // Very weak signal should not be reported
        bleScanner.onScanResults(mockDevice, mockScanRecord, -90)
        verify(exactly = 0) { bleScanner.callback.onDeviceFound(any()) }
    }
}
