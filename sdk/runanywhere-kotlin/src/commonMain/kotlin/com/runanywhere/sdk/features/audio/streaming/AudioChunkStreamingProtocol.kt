package com.runanywhere.sdk.features.audio.streaming

import kotlinx.serialization.Serializable

/**
 * Audio Chunk Streaming Protocol for BLE transport
 * 
 * This protocol handles:
 * - Variable bitrate encoding (8/16/24 kbps)
 * - Sequence numbering for packet ordering
 * - CRC error detection
 * - Retransmission requests for lost chunks
 */

/**
 * Audio chunk data structure for streaming
 */
@Serializable
data class AudioChunk(
    val data: ByteArray,
    val sequenceNumber: Int,
    val isLastChunk: Boolean = false
) {
    /**
     * Encode chunk data to byte array
     */
    fun encode(): ByteArray = data.copyOf()
}

/**
 * Encoded audio chunk with protocol metadata
 */
@Serializable
data class EncodedChunk(
    val sequenceNumber: Int,
    val payload: ByteArray,
    val crc32: Int,
    val flags: Flags,
    val bitrate: BitrateConfig,
    val timestamp: Int
)

/**
 * Chunk flags for protocol control
 */
@Serializable
data class Flags(
    val isRetransmission: Boolean = false,
    val isLastChunk: Boolean = false,
    val errorCorrectionEnabled: Boolean = true
)

/**
 * Bitrate configuration for audio encoding
 */
@Serializable
data class BitrateConfig(
    val kbps: Int,
    val sampleRate: Int = 44100,
    val channels: Int = 1
) {
    companion object {
        val LOW = BitrateConfig(8)
        val MEDIUM = BitrateConfig(16)
        val HIGH = BitrateConfig(24)
    }
}

/**
 * Streaming configuration
 */
@Serializable
data class StreamingConfig(
    val bitrate: BitrateConfig = BitrateConfig.MEDIUM,
    val chunkDurationMs: Int = DEFAULT_CHUNK_DURATION_MS,
    val enableErrorCorrection: Boolean = true
)

/**
 * Default chunk duration in milliseconds
 */
const val DEFAULT_CHUNK_DURATION_MS = 20

/**
 * Header size for encoded chunk protocol
 */
const val HEADER_SIZE = 20 // sequence(4) + crc(4) + flags(3) + bitrate(4) + timestamp(4) + padding(1)

/**
 * Calculate chunk size in bytes based on bitrate and duration
 */
fun calculateChunkSize(config: StreamingConfig): Int {
    val bytesPerSecond = (config.bitrate.kbps * 1000) / 8
    val durationSeconds = config.chunkDurationMs / 1000.0
    return (bytesPerSecond * durationSeconds).toInt()
}

/**
 * Encode audio chunk with protocol metadata
 */
fun encodeChunk(chunk: AudioChunk, config: StreamingConfig): EncodedChunk {
    val payload = chunk.encode()
    val crc = calculateCRC32(payload)
    val flags = Flags(
        isRetransmission = false,
        isLastChunk = chunk.isLastChunk,
        errorCorrectionEnabled = config.enableErrorCorrection
    )
    
    val seqNum: Int = chunk.sequenceNumber.toInt()
    return EncodedChunk(
        sequenceNumber = seqNum,
        payload = payload,
        crc32 = crc,
        flags = flags,
        bitrate = config.bitrate,
        timestamp = seqNum
    )
}

/**
 * Decode encoded chunk back to AudioChunk
 */
fun decodeChunk(encoded: EncodedChunk): AudioChunk {
    return AudioChunk(
        data = encoded.payload,
        sequenceNumber = encoded.sequenceNumber,
        isLastChunk = encoded.flags.isLastChunk
    )
}

/**
 * Calculate CRC32 checksum for data
 */
fun calculateCRC32(data: ByteArray): Int {
    var crc: Int = 0xFFFFFFFF.toInt()
    for (byte in data) {
        crc = crc xor (byte.toInt() and 0xFF)
        for (bit in 0..7) {
            if ((crc and 1) != 0) {
                crc = (crc ushr 1) xor 0xEDB88320.toInt()
            } else {
                crc = crc ushr 1
            }
        }
    }
    return crc xor 0xFFFFFFFF.toInt()
}

/**
 * Verify CRC32 of received chunk
 */
fun verifyCRC32(data: ByteArray, expectedCRC: Int): Boolean {
    return calculateCRC32(data) == expectedCRC
}

/**
 * Calculate required bitrate for target quality
 */
fun calculateRequiredBitrate(targetQuality: Float): BitrateConfig {
    return when {
        targetQuality < 0.3 -> BitrateConfig.LOW
        targetQuality < 0.7 -> BitrateConfig.MEDIUM
        else -> BitrateConfig.HIGH
    }
}

/**
 * Streaming events for monitoring audio stream status
 */
sealed class StreamingEvent {
    object StreamStarted : StreamingEvent()
    object StreamCompleted : StreamingEvent()
    data class StreamError(val exception: Exception) : StreamingEvent()
    data class RetransmissionRequested(val sequenceNumber: Int, val attempt: Int) : StreamingEvent()
    data class ChunkReceived(val sequenceNumber: Int) : StreamingEvent()
    data class ChunkSent(val sequenceNumber: Int) : StreamingEvent()
}
