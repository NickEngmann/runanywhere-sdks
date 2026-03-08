package com.runanywhere.sdk.features.audio.streaming

/**
 * Extension functions for AudioChunk and related types
 */

/**
 * Calculate size of encoded chunk including protocol overhead
 */
fun EncodedChunk.totalSize(): Int {
    return HEADER_SIZE + payload.size
}

/**
 * Check if chunk has error correction enabled
 */
fun EncodedChunk.hasErrorCorrection(): Boolean {
    return flags.errorCorrectionEnabled
}

/**
 * Check if chunk is a retransmission
 */
fun EncodedChunk.isRetransmission(): Boolean {
    return flags.isRetransmission
}

/**
 * Calculate bitrate from EncodedChunk
 */
fun EncodedChunk.getBitrate(): Int {
    return bitrate.kbps
}

/**
 * Create flags from byte array
 */
fun createFlagsFromBytes(flagsBytes: ByteArray): Flags {
    return Flags(
        isRetransmission = flagsBytes[0] != 0.toByte(),
        isLastChunk = flagsBytes[1] != 0.toByte(),
        errorCorrectionEnabled = flagsBytes[2] != 0.toByte()
    )
}

/**
 * Serialize flags to byte array
 */
fun Flags.toByteArray(): ByteArray {
    return byteArrayOf(
        if (isRetransmission) 1.toByte() else 0.toByte(),
        if (isLastChunk) 1.toByte() else 0.toByte(),
        if (errorCorrectionEnabled) 1.toByte() else 0.toByte()
    )
}

/**
 * Calculate chunk duration from bitrate and size
 */
fun calculateChunkDuration(bitrate: Int, chunkSize: Int): Int {
    val bytesPerSecond = (bitrate * 1000) / 8
    return ((chunkSize * 1000) / bytesPerSecond).toInt()
}

/**
 * Validate chunk sequence number
 */
fun validateSequenceNumber(sequenceNumber: Int, expectedSequence: Int): Boolean {
    return sequenceNumber == expectedSequence
}

/**
 * Calculate retry delay based on attempt number
 */
fun calculateRetryDelay(attempt: Int): Long {
    return 100L * attempt // Exponential backoff: 100ms, 200ms, 300ms, etc.
}

/**
 * Get chunk quality score based on bitrate
 */
fun getQualityScore(bitrate: Int): Float {
    return when {
        bitrate <= 8 -> 0.3f
        bitrate <= 16 -> 0.6f
        else -> 1.0f
    }
}

/**
 * Compare two EncodedChunks by sequence number
 */
fun EncodedChunk.compareTo(other: EncodedChunk): Int {
    return this.sequenceNumber.compareTo(other.sequenceNumber)
}

/**
 * Create EncodedChunk from raw data
 */
fun createEncodedChunk(
    data: ByteArray,
    sequenceNumber: Int,
    crc32: Int,
    flags: Flags,
    bitrate: BitrateConfig,
    timestamp: Int
): EncodedChunk {
    return EncodedChunk(
        sequenceNumber = sequenceNumber,
        payload = data,
        crc32 = crc32,
        flags = flags,
        bitrate = bitrate,
        timestamp = timestamp
    )
}
