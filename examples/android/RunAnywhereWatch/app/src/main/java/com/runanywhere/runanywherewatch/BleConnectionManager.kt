package com.runanywhere.runanywherewatch

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * BLE Connection Manager for T-Watch devices
 * Handles connection, disconnection, and auto-reconnect functionality
 */
class BleConnectionManager(
    private val context: Context,
    private val callback: BleStateCallback
) {
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var gattServer: BluetoothGattServer? = null
    private var gattClient: BluetoothGatt? = null
    private var connectedDevice: BluetoothDevice? = null
    private var isConnecting = false
    private var isConnected = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private var reconnectDelay = 2000L

    // T-Watch service and characteristics
    private var twatchService: BluetoothGattService? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var responseCharacteristic: BluetoothGattCharacteristic? = null
    private var dataCharacteristic: BluetoothGattCharacteristic? = null
    private var statusCharacteristic: BluetoothGattCharacteristic? = null

    init {
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
    }

    /**
     * Connect to a T-Watch device
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(deviceAddress: String): Boolean {
        return suspendCoroutine { continuation ->
            if (isConnecting || isConnected) {
                continuation.resume(false)
                return@suspendCoroutine
            }

            isConnecting = true
            callback.onError(BleGattConstants.ERROR_NONE, "Connecting to device...")

            val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
            if (device == null) {
                isConnecting = false
                callback.onError(
                    BleGattConstants.ERROR_DEVICE_NOT_FOUND,
                    "Device not found: $deviceAddress"
                )
                continuation.resume(false)
                return@suspendCoroutine
            }

            connectedDevice = device

            // Create GATT client
            gattClient = device.connectGatt(
                context,
                false,
                object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                        when (newState) {
                            BluetoothProfile.STATE_CONNECTED -> {
                                onConnected(gatt, status)
                                continuation.resume(true)
                            }
                            BluetoothProfile.STATE_DISCONNECTED -> {
                                onDisconnected(gatt, status)
                                continuation.resume(false)
                            }
                        }
                    }

                    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            findTWatchServices(gatt)
                        }
                    }

                    override fun onCharacteristicRead(
                        gatt: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        value: ByteArray,
                        status: Int
                    ) {
                        // Handle characteristic read
                    }

                    override fun onCharacteristicWrite(
                        gatt: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        value: ByteArray,
                        status: Int
                    ) {
                        // Handle characteristic write
                    }

                    override fun onCharacteristicChanged(
                        gatt: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        value: ByteArray
                    ) {
                        // Handle characteristic change notification
                    }
                },
                BluetoothDevice.TRANSPORT_LE
            )
        }
    }

    /**
     * Handle connection established
     */
    @SuppressLint("MissingPermission")
    private fun onConnected(gatt: BluetoothGatt, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            gatt.discoverServices()
            isConnected = true
            isConnecting = false
            reconnectAttempts = 0
            callback.onConnectionStateChanged(true, connectedDevice?.name)
        } else {
            handleConnectionError(status)
        }
    }

    /**
     * Handle disconnection
     */
    @SuppressLint("MissingPermission")
    private fun onDisconnected(gatt: BluetoothGatt, status: Int) {
        isConnected = false
        isConnecting = false
        gatt.close()
        gattClient = null
        callback.onConnectionStateChanged(false, connectedDevice?.name)

        // Attempt auto-reconnect
        if (reconnectAttempts < maxReconnectAttempts && connectedDevice != null) {
            reconnectAttempts++
            callback.onError(
                BleGattConstants.ERROR_CONNECTION_LOST,
                "Disconnected. Reconnecting attempt $reconnectAttempts/$maxReconnectAttempts"
            )
            scheduleReconnect()
        } else {
            callback.onError(
                BleGattConstants.ERROR_CONNECTION_LOST,
                "Max reconnect attempts reached"
            )
        }
    }

    /**
     * Schedule auto-reconnect
     */
    private fun scheduleReconnect() {
        val handler = android.os.Handler(context.mainLooper)
        handler.postDelayed({
            connectedDevice?.let { device ->
                gattClient = device.connectGatt(
                    context,
                    false,
                    createGattCallback(),
                    BluetoothDevice.TRANSPORT_LE
                )
            }
        }, reconnectDelay)
    }

    /**
     * Find T-Watch services and characteristics
     */
    @SuppressLint("MissingPermission")
    private fun findTWatchServices(gatt: BluetoothGatt) {
        gatt.services.forEach { service ->
            if (service.uuid.toString() == BleGattConstants.TWATCH_SERVICE_UUID) {
                twatchService = service
                service.characteristics.forEach { characteristic ->
                    when (characteristic.uuid.toString()) {
                        BleGattConstants.TWATCH_CHARACTERISTIC_COMMAND ->
                            commandCharacteristic = characteristic
                        BleGattConstants.TWATCH_CHARACTERISTIC_RESPONSE ->
                            responseCharacteristic = characteristic
                        BleGattConstants.TWATCH_CHARACTERISTIC_DATA ->
                            dataCharacteristic = characteristic
                        BleGattConstants.TWATCH_CHARACTERISTIC_STATUS ->
                            statusCharacteristic = characteristic
                    }
                }
                
                // Enable notifications for response and data characteristics
                enableNotifications(gatt, responseCharacteristic)
                enableNotifications(gatt, dataCharacteristic)
                
                callback.onError(
                    BleGattConstants.ERROR_NONE,
                    "T-Watch services found. Commands: ${commandCharacteristic != null}, Responses: ${responseCharacteristic != null}, Data: ${dataCharacteristic != null}, Status: ${statusCharacteristic != null}"
                )
            }
        }
    }

    /**
     * Enable notifications on a characteristic
     */
    @SuppressLint("MissingPermission")
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic?) {
        characteristic?.let {
            gatt.setCharacteristicNotification(it, true)
            val descriptor = it.getDescriptor(UUID.fromString(BleGattConstants.GATT_CHARACTERISTIC_CLIENT_CONFIG))
            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
    }

    /**
     * Write data to a characteristic
     */
    @SuppressLint("MissingPermission")
    suspend fun writeCharacteristic(
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray
    ): Boolean {
        return suspendCoroutine { continuation ->
            if (!isConnected || gattClient == null) {
                continuation.resume(false)
                return@suspendCoroutine
            }

            characteristic.value = data
            gattClient?.writeCharacteristic(characteristic)
            
            // Wait for write response
            val handler = android.os.Handler(context.mainLooper)
            handler.postDelayed({
                continuation.resume(true)
            }, 100)
        }
    }

    /**
     * Read from a characteristic
     */
    @SuppressLint("MissingPermission")
    suspend fun readCharacteristic(characteristic: BluetoothGattCharacteristic): ByteArray? {
        return suspendCoroutine { continuation ->
            if (!isConnected || gattClient == null) {
                continuation.resume(null)
                return@suspendCoroutine
            }

            gattClient?.readCharacteristic(characteristic)
            
            val handler = android.os.Handler(context.mainLooper)
            handler.postDelayed({
                continuation.resume(characteristic.value)
            }, 100)
        }
    }

    /**
     * Disconnect from the device
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        if (gattClient != null) {
            gattClient?.close()
            gattClient = null
        }
        connectedDevice = null
        isConnected = false
        isConnecting = false
        reconnectAttempts = 0
        callback.onConnectionStateChanged(false, null)
    }

    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean = isConnected

    /**
     * Check if currently connecting
     */
    fun isConnecting(): Boolean = isConnecting

    /**
     * Get the connected device address
     */
    fun getConnectedDeviceAddress(): String? = connectedDevice?.address

    /**
     * Get the connected device name
     */
    fun getConnectedDeviceName(): String? = connectedDevice?.name

    /**
     * Create GATT callback for connection
     */
    @SuppressLint("MissingPermission")
    private fun createGattCallback(): BluetoothGattCallback {
        return object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> onConnected(gatt, status)
                    BluetoothProfile.STATE_DISCONNECTED -> onDisconnected(gatt, status)
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    findTWatchServices(gatt)
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                // Handle characteristic read
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                // Handle characteristic write
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                // Handle characteristic change notification
            }
        }
    }

    /**
     * Handle connection errors
     */
    private fun handleConnectionError(status: Int) {
        val errorMessage = when (status) {
            BluetoothGatt.GATT_CONNECTION_TIMEOUT -> "Connection timeout"
            BluetoothGatt.GATT_INTERNAL_ERROR -> "Internal error"
            BluetoothGatt.GATT_INVALID_OFFSET -> "Invalid offset"
            BluetoothGatt.GATT_READ_NOT_PERMITTED -> "Read not permitted"
            BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "Write not permitted"
            else -> "Unknown error: $status"
        }
        callback.onError(BleGattConstants.ERROR_TIMEOUT, errorMessage)
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        disconnect()
        gattServer?.close()
        gattServer = null
        bluetoothAdapter = null
        bluetoothManager = null
    }
}
