package com.runanywhere.runanywherewatch

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * BLE Scanner for discovering T-Watch devices
 * Uses Android BLE API to scan for devices with specific service UUIDs
 */
class BleScanner(
    private val context: Context,
    private val callback: BleStateCallback
) {
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private var isScanning = false
    private var scanFilter: List<ScanFilter>? = null

    init {
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    /**
     * Start scanning for T-Watch devices
     * Filters by T-Watch service UUID
     */
    @SuppressLint("MissingPermission")
    fun startScan() {
        if (isScanning) {
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter?.isEnabled == true) {
            callback.onError(
                BleGattConstants.ERROR_DEVICE_NOT_FOUND,
                "Bluetooth is not available or enabled"
            )
            return
        }

        // Create scan filter for T-Watch service UUID
        scanFilter = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(UUID.fromString(BleGattConstants.TWATCH_SERVICE_UUID)))
                .build()
        )

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0L)
            .build()

        // Create scan callback
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val scanRecord = result.scanRecord
                val rssi = result.rssi
                
                val scanResult = ScanResult(
                    deviceName = device.name ?: "Unknown",
                    deviceAddress = device.address,
                    deviceUuid = device.uuids.firstOrNull() ?: UUID.randomUUID(),
                    rssi = rssi,
                    scanRecord = scanRecord?.bytes
                )
                
                callback.onDeviceFound(scanResult)
            }

            override fun onScanFailed(errorCode: Int) {
                callback.onError(errorCode, "Scan failed with error code: $errorCode")
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { result ->
                    val device = result.device
                    val scanRecord = result.scanRecord
                    val rssi = result.rssi
                    
                    val scanResult = ScanResult(
                        deviceName = device.name ?: "Unknown",
                        deviceAddress = device.address,
                        deviceUuid = device.uuids.firstOrNull() ?: UUID.randomUUID(),
                        rssi = rssi,
                        scanRecord = scanRecord?.bytes
                    )
                    callback.onDeviceFound(scanResult)
                }
            }
        }

        bluetoothLeScanner?.startScan(scanFilter, settings, scanCallback)
        isScanning = true
    }

    /**
     * Stop scanning for devices
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) {
            return
        }

        bluetoothLeScanner?.stopScan(scanCallback)
        scanCallback = null
        isScanning = false
        scanFilter = null
    }

    /**
     * Check if currently scanning
     */
    fun isScanning(): Boolean = isScanning

    /**
     * Check if Bluetooth is available
     */
    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null && bluetoothAdapter?.isEnabled == true

    /**
     * Get list of discovered devices
     */
    fun getDiscoveredDevices(): List<ScanResult> {
        // This would need to be maintained by the callback in a real implementation
        return emptyList()
    }

    /**
     * Suspend scanning and wait for a device with the specified address
     */
    @SuppressLint("MissingPermission")
    suspend fun waitForDevice(deviceAddress: String, timeoutMillis: Long = 10000): ScanResult? {
        return suspendCoroutine { continuation ->
            var foundResult: ScanResult? = null
            var found = false
            val startTime = System.currentTimeMillis()

            val tempCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    if (result.deviceAddress == deviceAddress && !found) {
                        foundResult = result
                        found = true
                        stopScan()
                        continuation.resume(result)
                    }
                }

                override fun onBatchScanResults(results: MutableList<ScanResult>) {
                    results.forEach { result ->
                        if (result.deviceAddress == deviceAddress && !found) {
                            foundResult = result
                            found = true
                            stopScan()
                            continuation.resume(result)
                        }
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    if (!found) {
                        stopScan()
                        continuation.resume(null)
                    }
                }
            }

            // Start scanning
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            bluetoothLeScanner?.startScan(
                scanFilter ?: emptyList(),
                settings,
                tempCallback
            )

            // Timeout handler
            val timeoutHandler = android.os.Handler(context.mainLooper)
            timeoutHandler.postDelayed({
                if (!found) {
                    stopScan()
                    continuation.resume(null)
                }
            }, timeoutMillis)
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopScan()
        bluetoothLeScanner = null
        bluetoothAdapter = null
        bluetoothManager = null
    }
}
