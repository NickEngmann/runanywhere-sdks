package com.runanywhere.runanywherewatch

import java.util.UUID

/**
 * Represents the current state of BLE connection
 */
data class BleState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val connectedDevice: String? = null,
    val errorMessage: String? = null,
    val lastError: Int = BleGattConstants.ERROR_NONE,
    val scanResults: List<ScanResult> = emptyList()
)

/**
 * Represents a discovered BLE device during scanning
 */
data class ScanResult(
    val deviceName: String,
    val deviceAddress: String,
    val deviceUuid: UUID,
    val rssi: Int,
    val scanRecord: ByteArray? = null
)

/**
 * Callback interface for BLE state changes
 */
interface BleStateCallback {
    fun onStateChange(state: BleState)
    fun onError(errorCode: Int, errorMessage: String)
    fun onDeviceFound(result: ScanResult)
    fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?)
}
