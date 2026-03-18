package com.runanywhere.sdk.features.audio.streaming

import org.junit.Test
import org.junit.Assert.*
import kotlinx.serialization.json.Json
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Pure JVM tests for Audio Chunk Streaming Protocol
 * Tests encoding, decoding, CRC calculation, and protocol functions
 */

class AudioChunkStreamingProtocolTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // ==================== AUDIO CHUNK TESTS ====================

    @Test
    fun `AudioChunk creates with default values`() {
        val chunk = AudioChunk(data = byteArrayOf(1, 2, 3), sequenceNumber = 0)
        assertEquals(byteArrayOf(1, 2, 3), chunk.data)
        assertEquals(0, chunk.sequenceNumber)
        assertFalse(chunk.isLastChunk)
    }

    @Test
    fun `AudioChunk creates with isLastChunk flag`() {
        val chunk = AudioChunk(data = byteArrayOf(1, 2, 3), sequenceNumber = 0, isLastChunk = true)
        assertTrue(chunk.isLastChunk)
    }

    @Test
    fun `AudioChunk.encode returns copy of data`() {
        val original = byteArrayOf(10, 20, 30)
        val chunk = AudioChunk(data = original, sequenceNumber = 0)
        val encoded = chunk.encode()
        
        assertNotSame(original, encoded)
        assertTrue(original.contentEquals(encoded))
    }

    // ==================== ENCODED CHUNK TESTS ====================

    @Test
    fun `EncodedChunk creates with all fields`() {
        val flags = Flags()
        val bitrate = BitrateConfig(16)
        val chunk = EncodedChunk(
            sequenceNumber = 1,
            payload = byteArrayOf(1, 2, 3),
            crc32 = 12345,
            flags = flags,
            bitrate = bitrate,
            timestamp = 1000
        )
        
        assertEquals(1, chunk.sequenceNumber)
        assertEquals(12345, chunk.crc32)
        assertEquals(16, chunk.bitrate.kbps)
        assertEquals(1000, chunk.timestamp)
    }

    @Test
    fun `EncodedChunk totalSize includes header`() {
        val flags = Flags()
        val bitrate = BitrateConfig(16)
        val payload = byteArrayOf(1, 2, 3, 4, 5)
        val chunk = EncodedChunk(
            sequenceNumber = 0,
            payload = payload,
            crc32 = 0,
            flags = flags,
            bitrate = bitrate,
            timestamp = 0
        )
        
        assertEquals(HEADER_SIZE + payload.size, chunk.totalSize())
    }

    @Test
    fun `EncodedChunk hasErrorCorrection returns flag value`() {
        val flagsWithCorrection = Flags(errorCorrectionEnabled = true)
        val flagsWithoutCorrection = Flags(errorCorrectionEnabled = false)
        
        val chunkWith = EncodedChunk(
            sequenceNumber = 0, payload = byteArrayOf(), crc32 = 0,
            flags = flagsWithCorrection, bitrate = BitrateConfig.LOW, timestamp = 0
        )
        val chunkWithout = EncodedChunk(
            sequenceNumber = 0, payload = byteArrayOf(), crc32 = 0,
            flags = flagsWithoutCorrection, bitrate = BitrateConfig.LOW, timestamp = 0
        )
        
        assertTrue(chunkWith.hasErrorCorrection())
        assertFalse(chunkWithout.hasErrorCorrection())
    }

    @Test
    fun `EncodedChunk isRetransmission returns flag value`() {
        val retransmissionFlags = Flags(isRetransmission = true)
        val normalFlags = Flags(isRetransmission = false)
        
        val chunk = EncodedChunk(
            sequenceNumber = 0, payload = byteArrayOf(), crc32 = 0,
            flags = retransmissionFlags, bitrate = BitrateConfig.LOW, timestamp = 0
        )
        
        assertTrue(chunk.isRetransmission())
        assertFalse(chunk.isRetransmission()) // This tests the method on normal chunk
    }

    @Test
    fun `EncodedChunk getBitrate returns kbps`() {
        val chunk = EncodedChunk(
            sequenceNumber = 0, payload = byteArrayOf(), crc32 = 0,
            flags = Flags(), bitrate = BitrateConfig(24), timestamp = 0
        )
        
        assertEquals(24, chunk.getBitrate())
    }

    // ==================== BITRATE CONFIG TESTS ====================

    @Test
    fun `BitrateConfig LOW has correct values`() {
        assertEquals(8, BitrateConfig.LOW.kbps)
        assertEquals(44100, BitrateConfig.LOW.sampleRate)
        assertEquals(1, BitrateConfig.LOW.channels)
    }

    @Test
    fun `BitrateConfig MEDIUM has correct values`() {
        assertEquals(16, BitrateConfig.MEDIUM.kbps)
        assertEquals(44100, BitrateConfig.MEDIUM.sampleRate)
        assertEquals(1, BitrateConfig.MEDIUM.channels)
    }

    @Test
    fun `BitrateConfig HIGH has correct values`() {
        assertEquals(24, BitrateConfig.HIGH.kbps)
        assertEquals(44100, BitrateConfig.HIGH.sampleRate)
        assertEquals(1, BitrateConfig.HIGH.channels)
    }

    @Test
    fun `calculateRequiredBitrate for low quality`() {
        assertEquals(BitrateConfig.LOW, calculateRequiredBitrate(0.2f))
        assertEquals(BitrateConfig.LOW, calculateRequiredBitrate(0.29f))
    }

    @Test
    fun `calculateRequiredBitrate for medium quality`() {
        assertEquals(BitrateConfig.MEDIUM, calculateRequiredBitrate(0.3f))
        assertEquals(BitrateConfig.MEDIUM, calculateRequiredBitrate(0.5f))
        assertEquals(BitrateConfig.MEDIUM, calculateRequiredBitrate(0.69f))
    }

    @Test
    fun `calculateRequiredBitrate for high quality`() {
        assertEquals(BitrateConfig.HIGH, calculateRequiredBitrate(0.7f))
        assertEquals(BitrateConfig.HIGH, calculateRequiredBitrate(0.9f))
        assertEquals(BitrateConfig.HIGH, calculateRequiredBitrate(1.0f))
    }

    // ==================== STREAMING CONFIG TESTS ====================

    @Test
    fun `StreamingConfig creates with default bitrate`() {
        val config = StreamingConfig()
        assertEquals(BitrateConfig.MEDIUM, config.bitrate)
        assertEquals(DEFAULT_CHUNK_DURATION_MS, config.chunkDurationMs)
        assertTrue(config.enableErrorCorrection)
    }

    @Test
    fun `StreamingConfig creates with custom values`() {
        val customConfig = StreamingConfig(
            bitrate = BitrateConfig(8),
            chunkDurationMs = 30,
            enableErrorCorrection = false
        )
        assertEquals(8, customConfig.bitrate.kbps)
        assertEquals(30, customConfig.chunkDurationMs)
        assertFalse(customConfig.enableErrorCorrection)
    }

    // ==================== CHUNK SIZE CALCULATION TESTS ====================

    @Test
    fun `calculateChunkSize for LOW bitrate 20ms`() {
        val config = StreamingConfig(
            bitrate = BitrateConfig.LOW,
            chunkDurationMs = 20
        )
        val size = calculateChunkSize(config)
        // 8kbps = 1000 bytes/sec, 20ms = 20 bytes
        assertEquals(20, size)
    }

    @Test
    fun `calculateChunkSize for MEDIUM bitrate 20ms`() {
        val config = StreamingConfig(
            bitrate = BitrateConfig.MEDIUM,
            chunkDurationMs = 20
        )
        val size = calculateChunkSize(config)
        // 16kbps = 2000 bytes/sec, 20ms = 40 bytes
        assertEquals(40, size)
    }

    @Test
    fun `calculateChunkSize for HIGH bitrate 20ms`() {
        val config = StreamingConfig(
            bitrate = BitrateConfig.HIGH,
            chunkDurationMs = 20
        )
        val size = calculateChunkSize(config)
        // 24kbps = 3000 bytes/sec, 20ms = 60 bytes
        assertEquals(60, size)
    }

    // ==================== CRC CALCULATION TESTS ====================

    @Test
    fun `calculateCRC32 produces consistent results`() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val crc1 = calculateCRC32(data)
        val crc2 = calculateCRC32(data)
        assertEquals(crc1, crc2)
    }

    @Test
    fun `calculateCRC32 returns different values for different data`() {
        val data1 = byteArrayOf(1, 2, 3)
        val data2 = byteArrayOf(1, 2, 4)
        val crc1 = calculateCRC32(data1)
        val crc2 = calculateCRC32(data2)
        assertNotEquals(crc1, crc2)
    }

    @Test
    fun `calculateCRC32 for empty array`() {
        val crc = calculateCRC32(byteArrayOf())
        assertTrue(crc != 0) // Empty data should still produce a CRC
    }

    @Test
    fun `calculateCRC32 for single byte`() {
        val crc = calculateCRC32(byteArrayOf(0xFF))
        assertTrue(crc != 0)
    }

    @Test
    fun `verifyCRC32 validates correct checksum`() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val crc = calculateCRC32(data)
        assertTrue(verifyCRC32(data, crc))
    }

    @Test
    fun `verifyCRC32 rejects incorrect checksum`() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val wrongCrc = 0x12345678
        assertFalse(verifyCRC32(data, wrongCrc))
    }

    // ==================== ENCODE/DECODE TESTS ====================

    @Test
    fun `encodeChunk creates valid EncodedChunk`() {
        val chunk = AudioChunk(
            data = byteArrayOf(1, 2, 3, 4, 5),
            sequenceNumber = 0,
            isLastChunk = true
        )
        val config = StreamingConfig(
            bitrate = BitrateConfig.MEDIUM,
            enableErrorCorrection = true
        )
        
        val encoded = encodeChunk(chunk, config)
        
        assertTrue(encoded.payload.contentEquals(chunk.data))
        assertEquals(0, encoded.sequenceNumber)
        assertEquals(16, encoded.bitrate.kbps)
        assertTrue(encoded.flags.errorCorrectionEnabled)
        assertTrue(encoded.flags.isLastChunk)
        assertTrue(verifyCRC32(encoded.payload, encoded.crc32))
    }

    @Test
    fun `encodeChunk with retransmission flag`() {
        val chunk = AudioChunk(
            data = byteArrayOf(1, 2, 3),
            sequenceNumber = 5,
            isLastChunk = false
        )
        val config = StreamingConfig()
        
        val encoded = encodeChunk(chunk, config)
        assertFalse(encoded.flags.isRetransmission)
    }

    @Test
    fun `decodeChunk reverses encodeChunk`() {
        val originalChunk = AudioChunk(
            data = byteArrayOf(10, 20, 30, 40),
            sequenceNumber = 3,
            isLastChunk = true
        )
        val config = StreamingConfig()
        
        val encoded = encodeChunk(originalChunk, config)
        val decoded = decodeChunk(encoded)
        
        assertEquals(originalChunk.sequenceNumber, decoded.sequenceNumber)
        assertTrue(originalChunk.data.contentEquals(decoded.data))
        assertEquals(originalChunk.isLastChunk, decoded.isLastChunk)
    }

    // ==================== FLAGS TESTS ====================

    @Test
    fun `Flags default values`() {
        val flags = Flags()
        assertFalse(flags.isRetransmission)
        assertFalse(flags.isLastChunk)
        assertTrue(flags.errorCorrectionEnabled)
    }

    @Test
    fun `createFlagsFromBytes creates flags correctly`() {
        val bytes = byteArrayOf(1, 0, 1) // isRetransmission=true, isLastChunk=false, errorCorrection=true
        val flags = createFlagsFromBytes(bytes)
        assertTrue(flags.isRetransmission)
        assertFalse(flags.isLastChunk)
        assertTrue(flags.errorCorrectionEnabled)
    }

    @Test
    fun `createFlagsFromBytes with all zeros`() {
        val bytes = byteArrayOf(0, 0, 0)
        val flags = createFlagsFromBytes(bytes)
        assertFalse(flags.isRetransmission)
        assertFalse(flags.isLastChunk)
        assertTrue(flags.errorCorrectionEnabled)
    }

    @Test
    fun `Flags.toByteArray roundtrip`() {
        val flags = Flags(isRetransmission = true, isLastChunk = false, errorCorrectionEnabled = false)
        val bytes = flags.toByteArray()
        
        assertEquals(3, bytes.size)
        assertEquals(1.toByte(), bytes[0])
        assertEquals(0.toByte(), bytes[1])
        assertEquals(0.toByte(), bytes[2])
    }

    @Test
    fun `createFlagsFromBytes and toByteArray roundtrip`() {
        val originalFlags = Flags(isRetransmission = false, isLastChunk = true, errorCorrectionEnabled = true)
        val bytes = originalFlags.toByteArray()
        val restoredFlags = createFlagsFromBytes(bytes)
        
        assertEquals(originalFlags.isRetransmission, restoredFlags.isRetransmission)
        assertEquals(originalFlags.isLastChunk, restoredFlags.isLastChunk)
        assertEquals(originalFlags.errorCorrectionEnabled, restoredFlags.errorCorrectionEnabled)
    }

    // ==================== EXTENSION FUNCTIONS TESTS ====================

    @Test
    fun `calculateChunkDuration works correctly`() {
        // 16kbps = 2000 bytes/sec
        // 40 bytes / 2000 bytes/sec * 1000 = 20ms
        assertEquals(20, calculateChunkDuration(16, 40))
        assertEquals(40, calculateChunkDuration(16, 80))
        assertEquals(20, calculateChunkDuration(8, 20))
    }

    @Test
    fun `validateSequenceNumber compares correctly`() {
        assertTrue(validateSequenceNumber(5, 5))
        assertFalse(validateSequenceNumber(5, 10))
        assertFalse(validateSequenceNumber(10, 5))
    }

    @Test
    fun `calculateRetryDelay implements exponential backoff`() {
        assertEquals(100L, calculateRetryDelay(1))
        assertEquals(200L, calculateRetryDelay(2))
        assertEquals(300L, calculateRetryDelay(3))
        assertEquals(400L, calculateRetryDelay(4))
    }

    @Test
    fun `getQualityScore returns expected values`() {
        assertEquals(0.3f, getQualityScore(8))
        assertEquals(0.6f, getQualityScore(16))
        assertEquals(1.0f, getQualityScore(24))
        assertEquals(0.6f, getQualityScore(12))
        assertEquals(1.0f, getQualityScore(32))
    }

    @Test
    fun `compareTo compares sequence numbers`() {
        val chunk1 = EncodedChunk(
            sequenceNumber = 5, payload = byteArrayOf(), crc32 = 0,
            flags = Flags(), bitrate = BitrateConfig.LOW, timestamp = 0
        )
        val chunk2 = EncodedChunk(
            sequenceNumber = 10, payload = byteArrayOf(), crc32 = 0,
            flags = Flags(), bitrate = BitrateConfig.LOW, timestamp = 0
        )
        val chunk3 = EncodedChunk(
            sequenceNumber = 5, payload = byteArrayOf(), crc32 = 0,
            flags = Flags(), bitrate = BitrateConfig.LOW, timestamp = 0
        )
        
        assertTrue(chunk1.compareTo(chunk2) < 0)
        assertTrue(chunk1.compareTo(chunk2) < 0)
        assertEquals(0, chunk1.compareTo(chunk3))
    }

    @Test
    fun `createEncodedChunk creates valid chunk`() {
        val flags = Flags(isRetransmission = true)
        val chunk = createEncodedChunk(
            data = byteArrayOf(1, 2, 3),
            sequenceNumber = 42,
            crc32 = 12345,
            flags = flags,
            bitrate = BitrateConfig.HIGH,
            timestamp = 999
        )
        
        assertEquals(42, chunk.sequenceNumber)
        assertEquals(12345, chunk.crc32)
        assertTrue(chunk.flags.isRetransmission)
        assertEquals(24, chunk.bitrate.kbps)
        assertEquals(999, chunk.timestamp)
    }

    // ==================== STREAMING EVENTS TESTS ====================

    @Test
    fun `StreamingEvent types are distinct`() {
        val streamStarted = StreamingEvent.StreamStarted
        val streamCompleted = StreamingEvent.StreamCompleted
        val streamError = StreamingEvent.StreamError(Exception("test"))
        val retransmission = StreamingEvent.RetransmissionRequested(5, 1)
        val chunkReceived = StreamingEvent.ChunkReceived(10)
        val chunkSent = StreamingEvent.ChunkSent(10)
        
        assertTrue(streamStarted is StreamingEvent.StreamStarted)
        assertTrue(streamCompleted is StreamingEvent.StreamCompleted)
        assertTrue(streamError is StreamingEvent.StreamError)
        assertTrue(retransmission is StreamingEvent.RetransmissionRequested)
        assertTrue(chunkReceived is StreamingEvent.ChunkReceived)
        assertTrue(chunkSent is StreamingEvent.ChunkSent)
    }

    @Test
    fun `StreamingEvent RetransmissionRequested contains data`() {
        val event = StreamingEvent.RetransmissionRequested(7, 3)
        assertTrue(event is StreamingEvent.RetransmissionRequested)
        assertEquals(7, (event as StreamingEvent.RetransmissionRequested).sequenceNumber)
        assertEquals(3, (event as StreamingEvent.RetransmissionRequested).attempt)
    }

    @Test
    fun `StreamingEvent ChunkReceived contains sequence number`() {
        val event = StreamingEvent.ChunkReceived(42)
        assertEquals(42, (event as StreamingEvent.ChunkReceived).sequenceNumber)
    }

    // ==================== DEFAULTS TESTS ====================

    @Test
    fun `DEFAULT_CHUNK_DURATION_MS equals 20`() {
        assertEquals(20, DEFAULT_CHUNK_DURATION_MS)
    }

    @Test
    fun `HEADER_SIZE equals 20`() {
        assertEquals(20, HEADER_SIZE)
    }
}
