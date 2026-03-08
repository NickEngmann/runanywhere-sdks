package com.runanywhere.sdk.pairing.qr

import kotlinx.serialization.Serializable

/**
 * QR Code payload structure for device pairing.
 * Contains device ID and public key for secure pairing.
 */
@Serializable
data class PairingQrPayload(
    val deviceId: String,
    val publicKey: ByteArray,
    val protocolVersion: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        private const val VERSION = 1
    }
    
    /**
     * Serialize payload to base64 string for QR code encoding.
     */
    fun toBase64(): String {
        val json = kotlinx.serialization.json.Json {
            encodeDefaults = true
        }.encodeToString(PairingQrPayload.serializer(), this)
        return android.util.Base64.encodeToString(json.toByteArray(), android.util.Base64.NO_WRAP)
    }
    
    /**
     * Deserialize from base64 string.
     */
    companion object {
        fun fromBase64(base64String: String): PairingQrPayload {
            val jsonBytes = android.util.Base64.decode(base64String, android.util.Base64.NO_WRAP)
            val json = String(jsonBytes)
            return kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            }.decodeFromString(PairingQrPayload.serializer(), json)
        }
    }
}

/**
 * QR Code generator for device pairing.
 * Creates QR codes containing device ID and public key.
 */
class PairingQrCodeGenerator {
    
    /**
     * Generate a QR code string containing device pairing information.
     * @param deviceId The device's unique identifier
     * @param publicKey The device's public key for ECDH
     * @return Base64-encoded QR code string
     */
    fun generate(deviceId: String, publicKey: ByteArray): String {
        val payload = PairingQrPayload(
            deviceId = deviceId,
            publicKey = publicKey
        )
        return payload.toBase64()
    }
    
    /**
     * Generate a QR code with additional metadata.
     * @param deviceId Device identifier
     * @param publicKey Device public key
     * @param deviceName Human-readable device name
     * @param capabilities Device capabilities bitmask
     * @return Base64-encoded QR code string
     */
    fun generateWithMetadata(
        deviceId: String,
        publicKey: ByteArray,
        deviceName: String = "",
        capabilities: Int = 0
    ): String {
        val payload = PairingQrPayload(
            deviceId = deviceId,
            publicKey = publicKey,
            timestamp = System.currentTimeMillis()
        )
        return payload.toBase64()
    }
}

/**
 * QR Code scanner for device pairing.
 * Parses QR codes to extract device information.
 */
class PairingQrCodeScanner {
    
    /**
     * Scan and parse a QR code to extract pairing information.
     * @param qrCodeString Base64-encoded QR code from scanner
     * @return Parsed pairing payload
     * @throws IllegalArgumentException if QR code is invalid or expired
     */
    fun scan(qrCodeString: String): PairingQrPayload {
        try {
            val payload = PairingQrPayload.fromBase64(qrCodeString)
            validatePayload(payload)
            return payload
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid QR code: ${e.message}", e)
        }
    }
    
    /**
     * Validate a parsed QR code payload.
     * @param payload The payload to validate
     * @throws IllegalArgumentException if validation fails
     */
    private fun validatePayload(payload: PairingQrPayload) {
        require(payload.deviceId.isNotEmpty()) {
            "Device ID cannot be empty"
        }
        require(payload.publicKey.isNotEmpty()) {
            "Public key cannot be empty"
        }
        require(payload.publicKey.size == 32) {
            "Public key must be 32 bytes (256 bits)"
        }
        require(payload.protocolVersion == VERSION) {
            "Unsupported protocol version: ${payload.protocolVersion}"
        }
        
        // Check for expired QR codes (valid for 5 minutes)
        val expirationTime = payload.timestamp + (5 * 60 * 1000)
        require(System.currentTimeMillis() < expirationTime) {
            "QR code has expired"
        }
    }
    
    /**
     * Scan QR code and return device information.
     * @param qrCodeString Base64-encoded QR code
     * @return Pair of (deviceId, publicKey)
     */
    fun scanToDeviceInfo(qrCodeString: String): Pair<String, ByteArray> {
        val payload = scan(qrCodeString)
        return Pair(payload.deviceId, payload.publicKey)
    }
}

/**
 * Extension function to generate QR code from payload.
 */
fun PairingQrPayload.toQrCodeString(): String {
    return this.toBase64()
}
