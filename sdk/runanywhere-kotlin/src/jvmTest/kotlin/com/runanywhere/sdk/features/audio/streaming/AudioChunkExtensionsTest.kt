package com.runanywhere.sdk.features.audio.streaming

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for AudioChunkExtensions — extension functions and utility functions
 */
class AudioChunkExtensionsTest {

    // --- EncodedChunk extensions ---

    @Test
    fun testTotalSize() {
        val encoded = EncodedChunk(
            sequenceNumber = 0,
            payload = byteArrayOf(1, 2, 3, 4, 5),
            crc32 = 0,
            flags = Flags(),
            bitrate = BitrateConfig.MEDIUM,
            timestamp = 0
        )
        // HEADER_SIZE (20) + payload size (5) = 25
        assertEquals(25, encoded.totalSize())
    }

    @Test
    fun testHasErrorCorrection() {
        val withEC = EncodedChunk(
            sequenceNumber = 0,
            payload = byteArrayOf(),
            crc32 = 0,
            flags = Flags(errorCorrectionEnabled = true),
            bitrate = BitrateConfig.LOW,
            timestamp = 0
        )
        val withoutEC = EncodedChunk(
            sequenceNumber = 0,
            payload = byteArrayOf(),
            crc32 = 0,
            flags = Flags(errorCorrectionEnabled = false),
            bitrate = BitrateConfig.LOW,
            timestamp = 0
        )
        assertTrue(withEC.hasErrorCorrection())
        assertFalse(withoutEC.hasErrorCorrection())
    }

    @Test
    fun testIsRetransmission() {
        val retrans = EncodedChunk(
            sequenceNumber = 0,
            payload = byteArrayOf(),
            crc32 = 0,
            flags = Flags(isRetransmission = true),
            bitrate = BitrateConfig.LOW,
            timestamp = 0
        )
        val normal = EncodedChunk(
            sequenceNumber = 0,
            payload = byteArrayOf(),
            crc32 = 0,
            flags = Flags(isRetransmission = false),
            bitrate = BitrateConfig.LOW,
            timestamp = 0
        )
        assertTrue(retrans.isRetransmission())
        assertFalse(normal.isRetransmission())
    }

    @Test
    fun testGetBitrate() {
        val encoded = EncodedChunk(
            sequenceNumber = 0,
            payload = byteArrayOf(),
            crc32 = 0,
            flags = Flags(),
            bitrate = BitrateConfig.HIGH,
            timestamp = 0
        )
        assertEquals(24, encoded.getBitrate())
    }

    // --- Flags serialization ---

    @Test
    fun testFlagsToByteArray() {
        val flags = Flags(isRetransmission = true, isLastChunk = false, errorCorrectionEnabled = true)
        val bytes = flags.toByteArray()
        assertEquals(3, bytes.size)
        assertEquals(1.toByte(), bytes[0]) // isRetransmission = true
        assertEquals(0.toByte(), bytes[1]) // isLastChunk = false
        assertEquals(1.toByte(), bytes[2]) // errorCorrectionEnabled = true
    }

    @Test
    fun testCreateFlagsFromBytes() {
        val bytes = byteArrayOf(0, 1, 1)
        val flags = createFlagsFromBytes(bytes)
        assertFalse(flags.isRetransmission)
        assertTrue(flags.isLastChunk)
        assertTrue(flags.errorCorrectionEnabled)
    }

    @Test
    fun testFlagsRoundTrip() {
        val original = Flags(isRetransmission = true, isLastChunk = true, errorCorrectionEnabled = false)
        val bytes = original.toByteArray()
        val restored = createFlagsFromBytes(bytes)
        assertEquals(original.isRetransmission, restored.isRetransmission)
        assertEquals(original.isLastChunk, restored.isLastChunk)
        assertEquals(original.errorCorrectionEnabled, restored.errorCorrectionEnabled)
    }

    // --- Utility functions ---

    @Test
    fun testCalculateChunkDuration() {
        // 16 kbps = 2000 bytes/sec. 40 bytes → 20ms
        val duration = calculateChunkDuration(16, 40)
        assertEquals(20, duration)
    }

    @Test
    fun testValidateSequenceNumber() {
        assertTrue(validateSequenceNumber(5, 5))
        assertFalse(validateSequenceNumber(5, 6))
    }

    @Test
    fun testCalculateRetryDelay() {
        assertEquals(100L, calculateRetryDelay(1))
        assertEquals(200L, calculateRetryDelay(2))
        assertEquals(300L, calculateRetryDelay(3))
    }

    @Test
    fun testGetQualityScore() {
        assertEquals(0.3f, getQualityScore(8))
        assertEquals(0.6f, getQualityScore(16))
        assertEquals(1.0f, getQualityScore(24))
    }

    @Test
    fun testGetQualityScoreBoundaries() {
        assertEquals(0.3f, getQualityScore(4))  // below LOW
        assertEquals(0.6f, getQualityScore(12)) // between LOW and MEDIUM
        assertEquals(1.0f, getQualityScore(32)) // above HIGH
    }

    // --- EncodedChunk compareTo ---

    @Test
    fun testEncodedChunkCompareTo() {
        val chunk1 = EncodedChunk(
            sequenceNumber = 1, payload = byteArrayOf(), crc32 = 0,
            flags = Flags(), bitrate = BitrateConfig.LOW, timestamp = 0
        )
        val chunk2 = EncodedChunk(
            sequenceNumber = 5, payload = byteArrayOf(), crc32 = 0,
            flags = Flags(), bitrate = BitrateConfig.LOW, timestamp = 0
        )
        assertTrue(chunk1.compareTo(chunk2) < 0)
        assertTrue(chunk2.compareTo(chunk1) > 0)
        assertEquals(0, chunk1.compareTo(chunk1))
    }

    // --- createEncodedChunk factory ---

    @Test
    fun testCreateEncodedChunk() {
        val data = byteArrayOf(1, 2, 3)
        val flags = Flags(isLastChunk = true)
        val chunk = createEncodedChunk(
            data = data,
            sequenceNumber = 7,
            crc32 = 42,
            flags = flags,
            bitrate = BitrateConfig.MEDIUM,
            timestamp = 100
        )
        assertEquals(7, chunk.sequenceNumber)
        assertTrue(chunk.payload.contentEquals(data))
        assertEquals(42, chunk.crc32)
        assertTrue(chunk.flags.isLastChunk)
        assertEquals(100, chunk.timestamp)
    }
}
