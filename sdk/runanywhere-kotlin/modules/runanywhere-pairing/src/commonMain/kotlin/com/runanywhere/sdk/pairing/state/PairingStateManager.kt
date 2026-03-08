package com.runanywhere.sdk.pairing.state

import kotlinx.serialization.Serializable

/**
 * Pairing state enumeration representing the current state of the pairing process.
 */
@Serializable
eenum class PairingState {
    /**
     * Initial state - no pairing information available.
     */
    Unpaired,
    
    /**
     * Device has been initialized with local information.
     */
    Initialized,
    
    /**
     * QR code has been scanned and counterpart device identified.
     */
    Scanned,
    
    /**
     * ECDH key exchange initiated.
     */
    KeyExchangeInitiated,
    
    /**
     * ECDH key exchange completed successfully.
     */
    KeyExchangeCompleted,
    
    /**
     * Pairing challenge-response verification in progress.
     */
    Verifying,
    
    /**
     * Pairing completed successfully - devices are paired.
     */
    Paired,
    
    /**
     * Pairing failed - reset required.
     */
    Failed,
    
    /**
     * Devices have been unpaired.
     */
    Unpaired;
    
    companion object {
        /**
         * Get the next state in the pairing flow.
         */
        fun getNextState(current: PairingState): PairingState {
            return when (current) {
                Unpaired -> Initialized
                Initialized -> Scanned
                Scanned -> KeyExchangeInitiated
                KeyExchangeInitiated -> KeyExchangeCompleted
                KeyExchangeCompleted -> Verifying
                Verifying -> Paired
                Paired -> Paired
                Failed -> Unpaired
            }
        }
    }
}

/**
 * Pairing session data containing all information needed for a pairing session.
 */
@Serializable
data class PairingSession(
    val sessionId: String,
    val deviceId: String,
    val publicKey: ByteArray,
    val counterpartDeviceId: String,
    val counterpartPublicKey: ByteArray,
    val sharedSecret: ByteArray,
    val symmetricKey: ByteArray,
    val state: PairingState,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Check if session is valid (not expired).
     */
    fun isValid(): Boolean {
        val expirationTime = timestamp + (30 * 60 * 1000) // 30 minutes
        return System.currentTimeMillis() < expirationTime
    }
    
    /**
     * Create a new session with updated state.
     */
    fun withState(newState: PairingState): PairingSession {
        return this.copy(
            state = newState,
            timestamp = System.currentTimeMillis()
        )
    }
}

/**
 * Manages pairing state and session persistence.
 * Handles saving, loading, and updating pairing information.
 */
class PairingStateManager {
    
    private var currentSession: PairingSession? = null
    private val pairedDevices = mutableMapOf<String, PairingSession>()
    
    /**
     * Initialize the pairing manager with device information.
     * @param deviceId Unique device identifier
     * @param publicKey Device's public key
     */
    fun initialize(deviceId: String, publicKey: ByteArray) {
        require(deviceId.isNotEmpty()) { "Device ID cannot be empty" }
        require(publicKey.isNotEmpty()) { "Public key cannot be empty" }
        
        val session = PairingSession(
            sessionId = generateSessionId(),
            deviceId = deviceId,
            publicKey = publicKey,
            counterpartDeviceId = "",
            counterpartPublicKey = ByteArray(0),
            sharedSecret = ByteArray(0),
            symmetricKey = ByteArray(0),
            state = PairingState.Initialized
        )
        currentSession = session
    }
    
    /**
     * Save device ID to persistent storage.
     */
    fun saveDeviceId(deviceId: String) {
        if (currentSession != null) {
            currentSession = currentSession!!.copy(deviceId = deviceId)
        }
    }
    
    /**
     * Save public key to persistent storage.
     */
    fun savePublicKey(publicKey: ByteArray) {
        if (currentSession != null) {
            currentSession = currentSession!!.copy(publicKey = publicKey)
        }
    }
    
    /**
     * Get saved device ID.
     */
    fun getDeviceId(): String? {
        return currentSession?.deviceId
    }
    
    /**
     * Get saved public key.
     */
    fun getPublicKey(): ByteArray? {
        return currentSession?.publicKey
    }
    
    /**
     * Update session with counterpart device information.
     * @param counterpartDeviceId Counterpart device ID
     * @param counterpartPublicKey Counterpart public key
     */
    fun updateCounterpart(counterpartDeviceId: String, counterpartPublicKey: ByteArray) {
        currentSession = currentSession?.copy(
            counterpartDeviceId = counterpartDeviceId,
            counterpartPublicKey = counterpartPublicKey,
            state = PairingState.Scanned
        )
    }
    
    /**
     * Update session with shared secret from ECDH exchange.
     * @param sharedSecret The derived shared secret
     * @param symmetricKey The derived symmetric key for encryption
     */
    fun updateKeyExchange(sharedSecret: ByteArray, symmetricKey: ByteArray) {
        currentSession = currentSession?.copy(
            sharedSecret = sharedSecret,
            symmetricKey = symmetricKey,
            state = PairingState.KeyExchangeCompleted
        )
    }
    
    /**
     * Complete the pairing process and save the session.
     */
    fun completePairing() {
        currentSession?.let { session ->
            pairedDevices[session.deviceId] = session
            currentSession = session.withState(PairingState.Paired)
        }
    }
    
    /**
     * Get the current pairing session.
     */
    fun getCurrentSession(): PairingSession? {
        return currentSession
    }
    
    /**
     * Get a paired device session by device ID.
     */
    fun getPairedDevice(deviceId: String): PairingSession? {
        return pairedDevices[deviceId]
    }
    
    /**
     * List all paired devices.
     */
    fun listPairedDevices(): List<PairingSession> {
        return pairedDevices.values.toList()
    }
    
    /**
     * Remove a paired device (unpair).
     * @param deviceId Device ID to unpair
     */
    fun unpairDevice(deviceId: String) {
        pairedDevices.remove(deviceId)
        if (currentSession?.deviceId == deviceId) {
            currentSession = null
        }
    }
    
    /**
     * Clear all pairing state.
     */
    fun clearAll() {
        currentSession = null
        pairedDevices.clear()
    }
    
    /**
     * Get current pairing state.
     */
    fun getCurrentState(): PairingState {
        return currentSession?.state ?: PairingState.Unpaired
    }
    
    /**
     * Generate a unique session ID.
     */
    private fun generateSessionId(): String {
        return "pairing_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().substring(0, 8)}"
    }
}

/**
 * Extension function to check if device is paired.
 */
fun PairingStateManager.isPaired(): Boolean {
    return getCurrentState() == PairingState.Paired
}

/**
 * Extension function to get paired device count.
 */
fun PairingStateManager.getPairedDeviceCount(): Int {
    return pairedDevices.size
}
