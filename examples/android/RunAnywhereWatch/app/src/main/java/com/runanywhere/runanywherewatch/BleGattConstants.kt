package com.runanywhere.runanywherewatch

/**
 * BLE GATT Constants for T-Watch communication
 * Defines service and characteristic UUIDs for T-Watch devices
 */
object BleGattConstants {
    // T-Watch custom service UUID
    const val TWATCH_SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb"
    
    // T-Watch characteristics
    const val TWATCH_CHARACTERISTIC_COMMAND = "0000fff1-0000-1000-8000-00805f9b34fb"
    const val TWATCH_CHARACTERISTIC_RESPONSE = "0000fff2-0000-1000-8000-00805f9b34fb"
    const val TWATCH_CHARACTERISTIC_DATA = "0000fff3-0000-1000-8000-00805f9b34fb"
    const val TWATCH_CHARACTERISTIC_STATUS = "0000fff4-0000-1000-8000-00805f9b34fb"
    
    // Standard BLE GATT UUIDs
    const val GATT_SERVICE_UUID = "00001801-0000-1000-8000-00805f9b34fb"
    const val GATT_CHARACTERISTIC_CLIENT_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"
    
    // Connection state values
    const val STATE_DISCONNECTED = 0
    const val STATE_CONNECTING = 1
    const val STATE_CONNECTED = 2
    const val STATE_DISCONNECTING = 3
    
    // Error codes
    const val ERROR_NONE = 0
    const val ERROR_TIMEOUT = 1
    const val ERROR_CONNECTION_LOST = 2
    const val ERROR_SERVICE_NOT_FOUND = 3
    const val ERROR_CHARACTERISTIC_NOT_FOUND = 4
    const val ERROR_PERMISSION_DENIED = 5
    const val ERROR_DEVICE_NOT_FOUND = 6
}
