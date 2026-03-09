package com.runanywhere.sdk.features.audio.streaming

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

/**
 * Tests for AudioChunkStreamingProtocol — data classes, encoding, CRC, bitrate
 */
class AudioChunkStreamingProtocolTest {

    // --- AudioChunk ---

    @Test
    fun testAudioChunkCreation() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val chunk = AudioChunk(data = data, sequenceNumber = 0)
        assertEquals(0, chunk.sequenceNumber)
        assertFalse(chunk.isLastChunk)
        assertEquals(5, chunk.data.size)
    }

    @Test
    fun testAudioChunkLastChunk() {
        val chunk = AudioChunk(data = byteArrayOf(10), sequenceNumber = 42, isLastChunk = true)
        assertTrue(chunk.isLastChunk)
        assertEquals(42, chunk.sequenceNumber)
    }

    @Test
    fun testAudioChunkEncode() {
        val data = byteArrayOf(1, 2, 3)
        val chunk = AudioChunk(data = data, sequenceNumber = 0)
        val encoded = chunk.encode()
        assertTrue(encoded.contentEquals(data))
        // encode returns a copy, not the same reference
        assert(encoded !== data)
    }

    // --- BitrateConfig ---

    @Test
    fun testBitrateConfigPresets() {
        assertEquals(8, BitrateConfig.LOW.kbps)
        assertEquals(16, BitrateConfig.MEDIUM.kbps)
        assertEquals(24, BitrateConfig.HIGH.kbps)
    }

    @Test
    fun testBitrateConfigDefaults() {
        val config = BitrateConfig(32)
        assertEquals(44100, config.sampleRate)
        assertEquals(1, config.channels)
    }

    @Test
    fun testBitrateConfigCustom() {
        val config = BitrateConfig(kbps = 48, sampleRate = 16000, channels = 2)
        assertEquals(48, config.kbps)
        assertEquals(16000, config.sampleRate)
        assertEquals(2, config.channels)
    }

    // --- Flags ---

    @Test
    fun testFlagsDefaults() {
        val flags = Flags()
        assertFalse(flags.isRetransmission)
        assertFalse(flags.isLastChunk)
        assertTrue(flags.errorCorrectionEnabled)
    }

    @Test
    fun testFlagsCustom() {
        val flags = Flags(isRetransmission = true, isLastChunk = true, errorCorrectionEnabled = false)
        assertTrue(flags.isRetransmission)
        assertTrue(flags.isLastChunk)
        assertFalse(flags.errorCorrectionEnabled)
    }

    // --- StreamingConfig ---

    @Test
    fun testStreamingConfigDefaults() {
        val config = StreamingConfig()
        assertEquals(BitrateConfig.MEDIUM.kbps, config.bitrate.kbps)
        assertEquals(DEFAULT_CHUNK_DURATION_MS, config.chunkDurationMs)
        assertTrue(config.enableErrorCorrection)
    }

    // --- CRC32 ---

    @Test
    fun testCalculateCRC32NonZero() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val crc = calculateCRC32(data)
        assertNotEquals(0, crc)
    }

    @Test
    fun testCalculateCRC32Deterministic() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val crc1 = calculateCRC32(data)
        val crc2 = calculateCRC32(data)
        assertEquals(crc1, crc2)
    }

    @Test
    fun testCalculateCRC32DifferentData() {
        val crc1 = calculateCRC32(byteArrayOf(1, 2, 3))
        val crc2 = calculateCRC32(byteArrayOf(4, 5, 6))
        assertNotEquals(crc1, crc2)
    }

    @Test
    fun testVerifyCRC32Valid() {
        val data = byteArrayOf(10, 20, 30)
        val crc = calculateCRC32(data)
        assertTrue(verifyCRC32(data, crc))
    }

    @Test
    fun testVerifyCRC32Invalid() {
        val data = byteArrayOf(10, 20, 30)
        assertFalse(verifyCRC32(data, 12345))
    }

    // --- encodeChunk / decodeChunk ---

    @Test
    fun testEncodeChunk() {
        val chunk = AudioChunk(data = byteArrayOf(1, 2, 3), sequenceNumber = 5, isLastChunk = true)
        val config = StreamingConfig(bitrate = BitrateConfig.HIGH)
        val encoded = encodeChunk(chunk, config)

        assertEquals(5, encoded.sequenceNumber)
        assertTrue(encoded.payload.contentEquals(byteArrayOf(1, 2, 3)))
        assertTrue(encoded.flags.isLastChunk)
        assertFalse(encoded.flags.isRetransmission)
        assertTrue(encoded.flags.errorCorrectionEnabled)
        assertEquals(BitrateConfig.HIGH.kbps, encoded.bitrate.kbps)
    }

    @Test
    fun testEncodeChunkCRC() {
        val data = byteArrayOf(10, 20, 30, 40)
        val chunk = AudioChunk(data = data, sequenceNumber = 0)
        val encoded = encodeChunk(chunk, StreamingConfig())
        val expectedCRC = calculateCRC32(data)
        assertEquals(expectedCRC, encoded.crc32)
    }

    @Test
    fun testDecodeChunkRoundTrip() {
        val original = AudioChunk(data = byteArrayOf(7, 8, 9), sequenceNumber = 3, isLastChunk = false)
        val config = StreamingConfig()
        val encoded = encodeChunk(original, config)
        val decoded = decodeChunk(encoded)

        assertEquals(original.sequenceNumber, decoded.sequenceNumber)
        assertTrue(original.data.contentEquals(decoded.data))
        assertEquals(original.isLastChunk, decoded.isLastChunk)
    }

    // --- calculateChunkSize ---

    @Test
    fun testCalculateChunkSize() {
        val config = StreamingConfig(bitrate = BitrateConfig.LOW, chunkDurationMs = 1000)
        val size = calculateChunkSize(config)
        // 8 kbps = 1000 bytes/sec, duration = 1s → 1000 bytes
        assertEquals(1000, size)
    }

    @Test
    fun testCalculateChunkSizeShortDuration() {
        val config = StreamingConfig(bitrate = BitrateConfig.MEDIUM, chunkDurationMs = 20)
        val size = calculateChunkSize(config)
        // 16 kbps = 2000 bytes/sec, duration = 0.02s → 40 bytes
        assertEquals(40, size)
    }

    // --- calculateRequiredBitrate ---

    @Test
    fun testCalculateRequiredBitrateLow() {
        val bitrate = calculateRequiredBitrate(0.1f)
        assertEquals(BitrateConfig.LOW.kbps, bitrate.kbps)
    }

    @Test
    fun testCalculateRequiredBitrateMedium() {
        val bitrate = calculateRequiredBitrate(0.5f)
        assertEquals(BitrateConfig.MEDIUM.kbps, bitrate.kbps)
    }

    @Test
    fun testCalculateRequiredBitrateHigh() {
        val bitrate = calculateRequiredBitrate(0.9f)
        assertEquals(BitrateConfig.HIGH.kbps, bitrate.kbps)
    }

    // --- EncodedChunk ---

    @Test
    fun testEncodedChunkFields() {
        val encoded = EncodedChunk(
            sequenceNumber = 10,
            payload = byteArrayOf(1, 2),
            crc32 = 999,
            flags = Flags(isRetransmission = true),
            bitrate = BitrateConfig.LOW,
            timestamp = 100
        )
        assertEquals(10, encoded.sequenceNumber)
        assertEquals(999, encoded.crc32)
        assertTrue(encoded.flags.isRetransmission)
        assertEquals(100, encoded.timestamp)
    }

    // --- StreamingEvent ---

    @Test
    fun testStreamingEventTypes() {
        val started = StreamingEvent.StreamStarted
        val completed = StreamingEvent.StreamCompleted
        val error = StreamingEvent.StreamError(RuntimeException("test"))
        val retrans = StreamingEvent.RetransmissionRequested(5, 1)
        val received = StreamingEvent.ChunkReceived(3)
        val sent = StreamingEvent.ChunkSent(4)

        assertTrue(started is StreamingEvent)
        assertTrue(completed is StreamingEvent)
        assertTrue(error is StreamingEvent)
        assertEquals(5, retrans.sequenceNumber)
        assertEquals(1, retrans.attempt)
        assertEquals(3, received.sequenceNumber)
        assertEquals(4, sent.sequenceNumber)
    }
}
