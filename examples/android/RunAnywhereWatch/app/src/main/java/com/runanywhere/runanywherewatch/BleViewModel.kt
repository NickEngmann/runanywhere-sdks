package com.runanywhere.runanywherewatch

import android.app.Application
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for BLE functionality
 * Manages BLE scanning and connection state using MVVM pattern
 */
class BleViewModel(application: Application) : AndroidViewModel(application), BleStateCallback {
    
    private val context: Context = application.applicationContext
    
    // State management
    private val _bleState = MutableStateFlow(BleState())
    val bleState: StateFlow<BleState> = _bleState.asStateFlow()
    
    private val _connectionManager: MutableLiveData<BleConnectionManager> = MutableLiveData()
    private val _scanner: MutableLiveData<BleScanner> = MutableLiveData()
    
    // Background jobs
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null
    private var reconnectJob: Job? = null
    
    init {
        initializeBleComponents()
    }
    
    /**
     * Initialize BLE components
     */
    private fun initializeBleComponents() {
        try {
            val scanner = BleScanner(context, this)
            val connectionManager = BleConnectionManager(context, this)
            
            _scanner.value = scanner
            _connectionManager.value = connectionManager
            
            updateState {
                it.copy(
                    errorMessage = "BLE components initialized"
                )
            }
        } catch (e: Exception) {
            onError(BleGattConstants.ERROR_NONE, "Failed to initialize BLE: ${e.message}")
        }
    }
    
    /**
     * Check if BLE is supported
     */
    fun isBleSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }
    
    /**
     * Check if Bluetooth is enabled
     */
    fun isBluetoothEnabled(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter?.isEnabled == true
    }
    
    /**
     * Start scanning for T-Watch devices
     */
    fun startScan() {
        _scanner.value?.startScan()
        updateState {
            it.copy(
                scanResults = emptyList(),
                errorMessage = "Starting scan for T-Watch devices..."
            )
        }
    }
    
    /**
     * Stop scanning
     */
    fun stopScan() {
        _scanner.value?.stopScan()
        updateState {
            it.copy(
                errorMessage = "Scan stopped"
            )
        }
    }
    
    /**
     * Connect to a T-Watch device
     */
    fun connectToDevice(deviceAddress: String) {
        if (_connectionManager.value == null) {
            onError(BleGattConstants.ERROR_NONE, "Connection manager not initialized")
            return
        }
        
        scope.launch {
            updateState {
                it.copy(
                    isConnecting = true,
                    errorMessage = "Connecting to device..."
                )
            }
            
            val success = _connectionManager.value?.connect(deviceAddress) == true
            
            if (success) {
                updateState {
                    it.copy(
                        isConnected = true,
                        connectedDevice = deviceAddress,
                        isConnecting = false,
                        errorMessage = null
                    )
                }
            } else {
                updateState {
                    it.copy(
                        isConnecting = false,
                        errorMessage = "Connection failed"
                    )
                }
            }
        }
    }
    
    /**
     * Disconnect from current device
     */
    fun disconnect() {
        _connectionManager.value?.disconnect()
        updateState {
            it.copy(
                isConnected = false,
                connectedDevice = null,
                errorMessage = "Disconnected"
            )
        }
    }
    
    /**
     * Write command to T-Watch
     */
    fun writeCommand(command: ByteArray) {
        if (!_connectionManager.value?.isConnected() == true) {
            onError(BleGattConstants.ERROR_NONE, "Not connected to device")
            return
        }
        
        val commandChar = _connectionManager.value?.let { manager ->
            manager.twatchService?.getCharacteristic(
                UUID.fromString(BleGattConstants.TWATCH_CHARACTERISTIC_COMMAND)
            )
        }
        
        commandChar?.let { characteristic ->
            scope.launch {
                _connectionManager.value?.writeCharacteristic(characteristic, command)
            }
        }
    }
    
    /**
     * Read from T-Watch characteristic
     */
    fun readCharacteristic(characteristicUuid: String): ByteArray? {
        val connectionManager = _connectionManager.value ?: return null
        val characteristic = connectionManager.twatchService?.getCharacteristic(
            UUID.fromString(characteristicUuid)
        )
        return characteristic?.value
    }
    
    /**
     * Update BLE state
     */
    private fun updateState(update: (BleState) -> BleState) {
        val newState = update(_bleState.value)
        _bleState.value = newState
    }
    
    // BleStateCallback implementation
    override fun onStateChange(state: BleState) {
        updateState { state }
    }
    
    override fun onError(errorCode: Int, errorMessage: String) {
        updateState {
            it.copy(
                errorMessage = errorMessage,
                lastError = errorCode
            )
        }
    }
    
    override fun onDeviceFound(result: ScanResult) {
        updateState {
            it.copy(
                scanResults = it.scanResults + result
            )
        }
    }
    
    override fun onConnectionStateChanged(isConnected: Boolean, deviceName: String?) {
        updateState {
            it.copy(
                isConnected = isConnected,
                connectedDevice = deviceName,
                isConnecting = false
            )
        }
    }
    
    /**
     * Get discovered devices
     */
    fun getDiscoveredDevices(): List<ScanResult> {
        return _bleState.value.scanResults
    }
    
    /**
     * Cleanup resources
     */
    override fun onCleared() {
        super.onCleared()
        scope.cancel()
        _scanner.value?.cleanup()
        _connectionManager.value?.cleanup()
    }
}
