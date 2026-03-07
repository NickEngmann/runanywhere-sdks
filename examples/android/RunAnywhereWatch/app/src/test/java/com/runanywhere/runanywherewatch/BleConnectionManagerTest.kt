package com.runanywhere.runanywherewatch

import android.bluetooth.*
import android.content.Context
import android.os.Build
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for BleConnectionManager
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class BleConnectionManagerTest {
    
    private lateinit var context: Context
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothGatt: BluetoothGatt
    private lateinit var bluetoothDevice: BluetoothDevice
    private lateinit var bleConnectionManager: BleConnectionManager
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        bluetoothManager = mockk(relaxed = true)
        bluetoothAdapter = mockk(relaxed = true)
        bluetoothGatt = mockk(relaxed = true)
        bluetoothDevice = mockk(relaxed = true)
        
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns bluetoothManager
        every { bluetoothManager.adapter } returns bluetoothAdapter
        every { bluetoothAdapter.getRemoteDevice(any()) } returns bluetoothDevice
        every { bluetoothDevice.connectGatt(any(), any(), any()) } returns bluetoothGatt
        every { bluetoothGatt.service } returns null
        every { bluetoothGatt.state } returns BluetoothGatt.STATE_CONNECTED
        every { bluetoothGatt.connectState } returns BluetoothProfile.STATE_CONNECTED
    }
    
    @Test
    fun `test connection manager initializes correctly`() {
        bleConnectionManager = BleConnectionManager(context, object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        })
        
        assertNotNull(bleConnectionManager)
    }
    
    @Test
    fun `test connect returns true when connection succeeds`() {
        bleConnectionManager = BleConnectionManager(context, object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        })
        
        val mockGatt = mockk<BluetoothGatt>(relaxed = true)
        every { bluetoothDevice.connectGatt(any(), any(), any()) } returns mockGatt
        every { mockGatt.connectState } returns BluetoothProfile.STATE_CONNECTED
        every { mockGatt.state } returns BluetoothGatt.STATE_CONNECTED
        
        val result = bleConnectionManager.connect("00:00:00:00:00:01")
        
        assertTrue(result)
    }
    
    @Test
    fun `test connect returns false when connection fails`() {
        bleConnectionManager = BleConnectionManager(context, object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        })
        
        val mockGatt = mockk<BluetoothGatt>(relaxed = true)
        every { bluetoothDevice.connectGatt(any(), any(), any()) } returns mockGatt
        every { mockGatt.connectState } returns BluetoothProfile.STATE_CONNECTING
        every { mockGatt.state } returns BluetoothGatt.STATE_CONNECTING
        
        val result = bleConnectionManager.connect("00:00:00:00:00:01")
        
        assertFalse(result)
    }
    
    @Test
    fun `test disconnect closes GATT connection`() {
        bleConnectionManager = BleConnectionManager(context, object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        })
        
        bleConnectionManager.disconnect()
        
        verify { bluetoothGatt.close() }
    }
    
    @Test
    fun `test isConnected returns true when connected`() {
        bleConnectionManager = BleConnectionManager(context, object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        })
        
        val mockGatt = mockk<BluetoothGatt>(relaxed = true)
        every { bluetoothDevice.connectGatt(any(), any(), any()) } returns mockGatt
        every { mockGatt.connectState } returns BluetoothProfile.STATE_CONNECTED
        every { mockGatt.state } returns BluetoothGatt.STATE_CONNECTED
        
        bleConnectionManager.connect("00:00:00:00:00:01")
        
        assertTrue(bleConnectionManager.isConnected())
    }
    
    @Test
    fun `test isConnected returns false when not connected`() {
        bleConnectionManager = BleConnectionManager(context, object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        })
        
        val mockGatt = mockk<BluetoothGatt>(relaxed = true)
        every { bluetoothDevice.connectGatt(any(), any(), any()) } returns mockGatt
        every { mockGatt.connectState } returns BluetoothProfile.STATE_DISCONNECTED
        every { mockGatt.state } returns BluetoothGatt.STATE_DISCONNECTED
        
        bleConnectionManager.connect("00:00:00:00:00:01")
        
        assertFalse(bleConnectionManager.isConnected())
    }
    
    @Test
    fun `test writeCharacteristic writes data to characteristic`() {
        bleConnectionManager = BleConnectionManager(context, object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        })
        
        val mockGatt = mockk<BluetoothGatt>(relaxed = true)
        val mockCharacteristic = mockk<BluetoothGattCharacteristic>(relaxed = true)
        val mockService = mockk<BluetoothGattService>(relaxed = true)
        
        every { bluetoothDevice.connectGatt(any(), any(), any()) } returns mockGatt
        every { mockGatt.connectState } returns BluetoothProfile.STATE_CONNECTED
        every { mockGatt.state } returns BluetoothGatt.STATE_CONNECTED
        every { mockGatt.services } returns listOf(mockService)
        every { mockService.characteristics } returns listOf(mockCharacteristic)
        every { mockCharacteristic.value } returns byteArrayOf()
        every { mockCharacteristic.writeType } returns BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        
        bleConnectionManager.connect("00:00:00:00:00:01")
        
        val command = byteArrayOf(0x01, 0x02, 0x03)
        bleConnectionManager.writeCharacteristic(mockCharacteristic, command)
        
        verify { mockCharacteristic.value = command }
        verify { mockGatt.writeCharacteristic(mockCharacteristic) }
    }
    
    @Test
    fun `test readCharacteristic reads data from characteristic`() {
        bleConnectionManager = BleConnectionManager(context, object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        })
        
        val mockGatt = mockk<BluetoothGatt>(relaxed = true)
        val mockCharacteristic = mockk<BluetoothGattCharacteristic>(relaxed = true)
        val mockService = mockk<BluetoothGattService>(relaxed = true)
        
        every { bluetoothDevice.connectGatt(any(), any(), any()) } returns mockGatt
        every { mockGatt.connectState } returns BluetoothProfile.STATE_CONNECTED
        every { mockGatt.state } returns BluetoothGatt.STATE_CONNECTED
        every { mockGatt.services } returns listOf(mockService)
        every { mockService.characteristics } returns listOf(mockCharacteristic)
        every { mockCharacteristic.value } returns byteArrayOf(0x01, 0x02, 0x03)
        
        bleConnectionManager.connect("00:00:00:00:00:01")
        
        val result = bleConnectionManager.readCharacteristic(mockCharacteristic)
        
        assertNotNull(result)
        assertEquals(3, result?.size)
    }
    
    @Test
    fun `test cleanup closes GATT connection`() {
        bleConnectionManager = BleConnectionManager(context, object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {}
        })
        
        bleConnectionManager.cleanup()
        
        verify { bluetoothGatt.close() }
    }
    
    @Test
    fun `test onConnectionStateChanged callback is invoked on connection`() {
        var callbackInvoked = false
        var callbackDeviceName: String? = null
        
        bleConnectionManager = BleConnectionManager(context, object : BleStateCallback {
            override fun onStateChange(state: BleState) {}
            override fun onError(errorCode: Int, errorMessage: String) {}
            override fun onDeviceFound(result: ScanResult) {}
            override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {
                callbackInvoked = true
                callbackDeviceName = deviceName
            }
        })
        
        val mockGatt = mockk<BluetoothGatt>(relaxed = true)
        val mockCharacteristic = mockk<BluetoothGattCharacteristic>(relaxed = true)
        val mockService = mockk<BluetoothGattService>(relaxed = true)
        
        every { bluetoothDevice.connectGatt(any(), any(), any()) } returns mockGatt
        every { mockGatt.connectState } returns BluetoothProfile.STATE_CONNECTED
        every { mockGatt.state } returns BluetoothGatt.STATE_CONNECTED
        every { mockGatt.services } returns listOf(mockService)
        every { mockService.characteristics } returns listOf(mockCharacteristic)
        every { mockCharacteristic.value } returns byteArrayOf()
        
        bleConnectionManager.connect("00:00:00:00:00:01")
        
        assertTrue(callbackInvoked)
        assertEquals("00:00:00:00:00:01", callbackDeviceName)
    }
}
