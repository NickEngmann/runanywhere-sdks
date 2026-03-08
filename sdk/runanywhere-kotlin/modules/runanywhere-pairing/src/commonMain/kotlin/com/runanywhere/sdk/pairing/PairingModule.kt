package com.runanywhere.sdk.pairing

import com.runanywhere.sdk.pairing.ble.BlePairingProtocol
import com.runanywhere.sdk.pairing.crypto.ECDHKeyExchange
import com.runanywhere.sdk.pairing.state.PairingSession
import com.runanywhere.sdk.pairing.state.PairingState
import com.runanywhere.sdk.pairing.state.PairingStateManager

/**
 * Main pairing module entry point.
 * Provides a unified API for device pairing functionality.
 */
class PairingModule {
    
    private val pairingManager: PairingStateManager
    private val keyExchange: ECDHKeyExchange
    private val bleProtocol: BlePairingProtocol
    
    /**
     * Create a new pairing module instance.
     */
    constructor() {
        pairingManager = PairingStateManager()
        keyExchange = ECDHKeyExchange()
        bleProtocol = BlePairingProtocol(pairingManager, keyExchange)
    }
    
    /**
     * Create a pairing module with custom components.
     * @param manager Custom pairing state manager
     * @param keyExchange Custom key exchange implementation
     * @param protocol Custom BLE pairing protocol
     */
    constructor(
        manager: PairingStateManager,
        keyExchange: ECDHKeyExchange,
        protocol: BlePairingProtocol
    ) {
        pairingManager = manager
        this.keyExchange = keyExchange
        bleProtocol = protocol
    }
    
    /**
     * Initialize the pairing module with device information.
     * @param deviceId Unique device identifier
     * @param publicKey Device's public key
     */
    fun initialize(deviceId: String, publicKey: ByteArray) {
        bleProtocol.startPairing(deviceId, publicKey)
    }
    
    /**
     * Process a scanned QR code from the counterpart device.
     * @param qrCodeString Base64-encoded QR code string
     */
    fun processScannedQrCode(qrCodeString: String) {
        bleProtocol.processScannedQrCode(qrCodeString)
    }
    
    /**
     * Perform ECDH key exchange with the counterpart device.
     * @param counterpartPublicKey The counterpart's public key
     * @return The derived shared secret
     */
    fun performKeyExchange(counterpartPublicKey: ByteArray): ByteArray {
        return bleProtocol.performKeyExchange(counterpartPublicKey)
    }
    
    /**
     * Complete the pairing process.
     * @return The derived symmetric key for secure communication
     */
    fun completePairing(): ByteArray {
        return bleProtocol.completePairing()
    }
    
    /**
     * Unpair a device.
     * @param deviceId The device to unpair
     */
    fun unpairDevice(deviceId: String) {
        bleProtocol.unpairDevice(deviceId)
    }
    
    /**
     * Check if devices are currently paired.
     */
    fun isPaired(): Boolean {
        return bleProtocol.isPaired()
    }
    
    /**
     * Get the current pairing session.
     */
    fun getCurrentSession(): PairingSession? {
        return bleProtocol.getCurrentSession()
    }
    
    /**
     * Get the symmetric key for encrypted communication.
     */
    fun getSymmetricKey(): ByteArray? {
        return bleProtocol.getSymmetricKey()
    }
    
    /**
     * Get the pairing state manager.
     */
    fun getPairingManager(): PairingStateManager {
        return pairingManager
    }
    
    /**
     * Get the key exchange implementation.
     */
    fun getKeyExchange(): ECDHKeyExchange {
        return keyExchange
    }
    
    /**
     * Get the BLE pairing protocol.
     */
    fun getBleProtocol(): BlePairingProtocol {
        return bleProtocol
    }
    
    /**
     * List all paired devices.
     */
    fun listPairedDevices(): List<PairingSession> {
        return pairingManager.listPairedDevices()
    }
    
    /**
     * Clear all pairing state.
     */
    fun clearAll() {
        pairingManager.clearAll()
    }
    
    /**
     * Get the current pairing state.
     */
    fun getCurrentState(): PairingState {
        return pairingManager.getCurrentState()
    }
    
    /**
     * Get the device ID.
     */
    fun getDeviceId(): String? {
        return pairingManager.getDeviceId()
    }
    
    /**
     * Get the public key.
     */
    fun getPublicKey(): ByteArray? {
        return pairingManager.getPublicKey()
    }
}

/**
 * Companion object for factory methods.
 */
object PairingModule {
    /**
     * Create a new pairing module instance.
     */
    fun create(): PairingModule {
        return PairingModule()
    }
    
    /**
     * Create a pairing module with custom components.
     */
    fun create(
        manager: PairingStateManager,
        keyExchange: ECDHKeyExchange,
        protocol: BlePairingProtocol
    ): PairingModule {
        return PairingModule(manager, keyExchange, protocol)
    }
}

/**
 * Extension function to initialize pairing with device information.
 */
fun PairingModule.init(deviceId: String, publicKey: ByteArray) {
    initialize(deviceId, publicKey)
}

/**
 * Extension function to check if paired.
 */
fun PairingModule.pairingComplete(): Boolean {
    return isPaired()
}

/**
 * Extension function to get paired device count.
 */
fun PairingModule.getPairedDeviceCount(): Int {
    return pairingManager.getPairedDeviceCount()
}
