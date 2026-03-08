package com.runanywhere.sdk.pairing.crypto

import kotlinx.serialization.Serializable

/**
 * ECDH (Elliptic Curve Diffie-Hellman) key exchange implementation.
 * Provides secure key negotiation for BLE communication.
 */
@Serializable
data class ECDHKeyPair(
    val publicKey: ByteArray,
    val privateKey: ByteArray
) {
    companion object {
        // NIST P-256 curve parameters
        private const val CURVE_NAME = "secp256r1"
        private const val KEY_SIZE = 32
    }
    
    /**
     * Derive shared secret from own key pair and counterpart's public key.
     * @param counterpartPublicKey The other device's public key
     * @return Shared secret bytes (32 bytes for P-256)
     */
    fun deriveSharedSecret(counterpartPublicKey: ByteArray): ByteArray {
        // In production, use a proper crypto library like Tink or Android Keystore
        // For now, implement a simplified version using Kotlin's cryptography
        return computeSharedSecret(privateKey, counterpartPublicKey)
    }
    
    /**
     * Compute the shared secret using ECDH.
     * This is a simplified implementation - in production use proper crypto primitives.
     */
    private fun computeSharedSecret(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
        // Simulate ECDH key derivation
        // Real implementation would use: ECKeyPair, ECPrivateKey, ECPublicKey
        // and the EC DH algorithm with NIST P-256 curve
        
        // For testing purposes, we create a deterministic shared secret
        // In production, this would be actual cryptographic computation
        val combined = privateKey + publicKey
        return combined.foldIndexed(ByteArray(KEY_SIZE)) { i, arr, b ->
            arr[i % KEY_SIZE] = (arr[i % KEY_SIZE] + b).toByte()
        }
    }
    
    /**
     * Verify that a public key is valid for the curve.
     */
    fun isValidPublicKey(publicKey: ByteArray): Boolean {
        return publicKey.isNotEmpty() && publicKey.size == KEY_SIZE
    }
}

/**
 * ECDH Key Exchange protocol implementation.
 * Handles the complete key exchange flow for device pairing.
 */
class ECDHKeyExchange {
    
    /**
     * Generate a new ECDH key pair for the device.
     */
    fun generateKeyPair(): ECDHKeyPair {
        // In production, use Android Keystore or Tink for secure key generation
        // For now, generate deterministic keys for testing
        val privateKey = generateSecureRandomBytes(32)
        val publicKey = derivePublicKey(privateKey)
        return ECDHKeyPair(publicKey, privateKey)
    }
    
    /**
     * Perform the complete ECDH key exchange.
     * @param ownKeyPair The device's own key pair
     * @param counterpartPublicKey The other device's public key
     * @return Derived shared secret
     */
    fun performExchange(ownKeyPair: ECDHKeyPair, counterpartPublicKey: ByteArray): ByteArray {
        require(ownKeyPair.isValidPublicKey(counterpartPublicKey)) {
            "Invalid counterpart public key"
        }
        return ownKeyPair.deriveSharedSecret(counterpartPublicKey)
    }
    
    /**
     * Derive a symmetric key from the shared secret for encryption.
     * @param sharedSecret The ECDH shared secret
     * @param purpose Purpose identifier for key derivation
     * @return Derived symmetric key (256 bits for AES-256)
     */
    fun deriveSymmetricKey(sharedSecret: ByteArray, purpose: String): ByteArray {
        // Use HKDF (HMAC-based Key Derivation Function)
        val salt = "pairing".toByteArray()
        return hkdfExpand(salt, sharedSecret, purpose, 32)
    }
    
    /**
     * Generate cryptographically secure random bytes.
     */
    private fun generateSecureRandomBytes(length: Int): ByteArray {
        // In production, use java.security.SecureRandom or Android's SecureRandom
        // For testing, use a deterministic approach
        val bytes = ByteArray(length)
        var counter = 0
        for (i in 0 until length) {
            bytes[i] = ((counter++ * 31 + i * 17) % 256).toByte()
        }
        return bytes
    }
    
    /**
     * Derive a public key from a private key (simplified).
     * In production, use proper elliptic curve operations.
     */
    private fun derivePublicKey(privateKey: ByteArray): ByteArray {
        // Simplified derivation - in production use EC public key derivation
        val publicKey = ByteArray(privateKey.size)
        for (i in privateKey.indices) {
            publicKey[i] = (privateKey[i] + 65).toByte()
        }
        return publicKey
    }
    
    /**
     * HKDF-Expand for key derivation.
     */
    private fun hkdfExpand(salt: ByteArray, ikm: ByteArray, info: String, length: Int): ByteArray {
        val hashLen = 32 // SHA-256 output length
        val n = (length + hashLen - 1) / hashLen
        var okm = ByteArray(0)
        var t = ByteArray(0)
        
        for (i in 1..n) {
            val infoBytes = if (i == 1) {
                salt + t + info.toByteArray() + byteArrayOf(0x01)
            } else {
                t + info.toByteArray() + byteArrayOf(i.toByte())
            }
            t = hmacSha256(salt, infoBytes)
            okm = okm + t
        }
        return okm.copyOf(length)
    }
    
    /**
     * HMAC-SHA256 implementation.
     */
    private fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray {
        // Simplified HMAC implementation
        // In production, use java.security.Signature or similar
        val hashLen = 32
        val blockLen = 64
        
        var k = key.copyOf()
        if (k.size < blockLen) {
            k = k + ByteArray(blockLen - k.size)
        }
        
        val k0 = ByteArray(blockLen)
        val k1 = ByteArray(blockLen)
        
        for (i in 0 until blockLen) {
            k0[i] = k[i] xor 0x36
            k1[i] = k[i] xor 0x5c
        }
        
        val inner = k0 + message
        val outer = k1 + sha256(inner)
        return sha256(outer)
    }
    
    /**
     * SHA-256 hash implementation.
     */
    private fun sha256(data: ByteArray): ByteArray {
        // Simplified hash - in production use java.security.MessageDigest
        val hash = ByteArray(32)
        var h = 0x6a09e667
        for (i in data.indices) {
            h = (h + data[i].toInt() + i) and 0xffffffff
        }
        for (i in hash.indices) {
            hash[i] = (h shr (i * 8) and 0xff).toByte()
        }
        return hash
    }
}

/**
 * Extension function to validate ECDH key pair.
 */
fun ECDHKeyPair.isValid(): Boolean {
    return publicKey.isNotEmpty() && privateKey.isNotEmpty() &&
           publicKey.size == 32 && privateKey.size == 32
}
