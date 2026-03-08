package com.runanywhere.sdk.features.audio.streaming

import com.runanywhere.sdk.features.audio.streaming.AudioChunkStreamingProtocol.*
import com.runanywhere.sdk.features.audio.streaming.BleAudioStreamer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

/**
 * Tests for AudioChunkStreamingProtocol and BleAudioStreamer
 */
class AudioChunkStreamingProtocolTest {
    
    @Test
    fun testBitrateConfigurations() {
        assertEquals(8, BitrateConfig.LOW.kbps)
        assertEquals(8000, BitrateConfig.LOW.sampleRate)
        assertEquals(16, BitrateConfig.LOW.bitsPerSample)
        
        assertEquals(16, BitrateConfig.MEDIUM.kbps)
        assertEquals(16000, BitrateConfig.MEDIUM.sampleRate)
        
        assertEquals(24, BitrateConfig.HIGH.kbps)
        assertEquals(24000, BitrateConfig.HIGH.sampleRate)
    }
    
    @Test
    fun testCalculateCRC32() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val crc = AudioChunkStreamingProtocol.calculateCRC32(data)
        
        // CRC should be non-zero for non-empty data
        assertNotEquals(0, crc)
        
        // Same data should produce same CRC
        val crc2 = AudioChunkStreamingProtocol.calculateCRC32(data)
        assertEquals(crc, crc2)
        
        // Different data should produce different CRC
        val data2 = byteArrayOf(1, 2, 3, 4, 6)
        val crc3 = AudioChunkStreamingProtocol.calculateCRC32(data2)
        assertNotEquals(crc, crc3)
    }
    
    @Test
    fun testVerifyCRC32() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val crc = AudioChunkStreamingProtocol.calculateCRC32(data)
        
        // Valid CRC should pass verification
        assertTrue(AudioChunkStreamingProtocol.verifyCRC32(data, crc))
        
        // Invalid CRC should fail verification
        assertFalse(AudioChunkStreamingProtocol.verifyCRC32(data, crc + 1))
    }
    
    @Test
    fun testFlagsSerialization() {
        val flags = Flags(
            isRetransmission = true,
            isLastChunk = false,
            errorCorrectionEnabled = true
        )
        
        val byte = flags.toByte()
        val restoredFlags = Flags.fromByte(byte)
        
        assertEquals(flags.isRetransmission, restoredFlags.isRetransmission)
        assertEquals(flags.isLastChunk, restoredFlags.isLastChunk)
        assertEquals(flags.errorCorrectionEnabled, restoredFlags.errorCorrectionEnabled)
    }
    
    @Test
    fun testStreamingConfigSerialization() {
        val config = StreamingConfig(
            bitrate = BitrateConfig.MEDIUM,
            chunkDurationMs = 200,
            enableErrorCorrection = true
        )
        
        val serialized = AudioChunkStreamingProtocol.encodeStreamingConfig(config)
        val deserialized = AudioChunkStreamingProtocol.decodeStreamingConfig(serialized)
        
        assertEquals(config.bitrate, deserialized.bitrate)
        assertEquals(config.chunkDurationMs, deserialized.chunkDurationMs)
        assertEquals(config.enableErrorCorrection, deserialized.enableErrorCorrection)
    }
    
    @Test
    fun testAudioChunkEncoding() {
        val chunk = AudioChunk(
            data = byteArrayOf(1, 2, 3, 4, 5),
            timestampMs = 1000,
            sequenceNumber = 1
        )
        
        val encoded = chunk.encode()
        
        // Encoded chunk should include header and payload
        assertTrue(encoded.size > chunk.data.size)
        
        // Decode should restore original chunk
        val decoded = AudioChunk.decode(encoded)
        assertEquals(chunk.data, decoded.data)
        assertEquals(chunk.timestampMs, decoded.timestampMs)
        assertEquals(chunk.sequenceNumber, decoded.sequenceNumber)
    }
    
    @Test
    fun testEncodedChunkStructure() {
        val chunk = AudioChunk(
            data = byteArrayOf(1, 2, 3, 4, 5),
            timestampMs = 1000,
            sequenceNumber = 1
        )
        
        val encoded = chunk.encode()
        
        // Verify structure: sequence(4) + crc(4) + length(2) + flags(2) + payload
        assertEquals(HEADER_SIZE + chunk.data.size, encoded.size)
        
        // Extract and verify sequence number from header
        val sequenceNumber = encoded.sliceArray(0..3).decodeSequenceNumber()
        assertEquals(chunk.sequenceNumber, sequenceNumber)
        
        // Extract and verify payload
        val payload = encoded.sliceArray(HEADER_SIZE until encoded.size)
        assertArrayEquals(chunk.data, payload)
    }
    
    @Test
    fun testRetransmissionRequest() {
        val streamer = TestBleAudioStreamer()
        val missingNumbers = setOf(5, 10, 15)
        
        streamer.requestRetransmission(mmissingNumbers)
        
        // Verify retransmission was requested
        assertEquals(3, streamer.retransmissionCount)
    }
    
    @Test
    fun testChunkSizeLimit() {
        val largeData = ByteArray(MAX_CHUNK_SIZE_BYTES + 100) { i -> i.toByte() }
        val chunk = AudioChunk(
            data = largeData,
            timestampMs = 1000,
            sequenceNumber = 1
        )
        
        val encoded = chunk.encode()
        
        // Chunk should be split into multiple encoded chunks
        assertTrue(encoded.size > MAX_CHUNK_SIZE_BYTES)
    }
    
    @Test
    fun testSequenceNumberOrdering() {
        val chunks = (1..10).map { i ->
            AudioChunk(
                data = byteArrayOf(i.toByte()),
                timestampMs = (i * 100).toLong(),
                sequenceNumber = i
            )
        }
        
        // Sort by sequence number
        val sorted = chunks.sortedBy { it.sequenceNumber }
        
        assertEquals(chunks, sorted)
    }
    
    @Test
    fun testLastChunkFlag() {
        val chunk1 = AudioChunk(
            data = byteArrayOf(1, 2, 3),
            timestampMs = 1000,
            sequenceNumber = 1
        )
        
        val chunk2 = AudioChunk(
            data = byteArrayOf(4, 5, 6),
            timestampMs = 1100,
            sequenceNumber = 2,
            isLast = true
        )
        
        val encoded1 = chunk1.encode()
        val encoded2 = chunk2.encode()
        
        // Verify last chunk flag is set
        assertTrue(encoded2.decodeFlags().isLastChunk)
        assertFalse(encoded1.decodeFlags().isLastChunk)
    }
}

/**
 * Test implementation of BleAudioStreamer
 */
class TestBleAudioStreamer : BleAudioStreamer(
    config = StreamingConfig(
        bitrate = BitrateConfig.MEDIUM,
        chunkDurationMs = 100,
        enableErrorCorrection = true
    )
) {
    override val audioChunks: Flow<AudioChunk> = flow {
        emit(AudioChunk(
            data = byteArrayOf(1, 2, 3),
            timestampMs = 1000,
            sequenceNumber = 1
        ))
    }
    
    override val streamingEvents: Flow<StreamingEvent> = flow {
        emit(StreamingEvent.StreamingStarted)
    }
    
    override fun emitEvent(event: StreamingEvent) {
        // No-op for test
    }
    
    override fun startStreaming() {
        // No-op for test
    }
    
    override fun stopStreaming() {
        // No-op for test
    }
}

/**
 * Helper function for array comparison in tests
 */
private fun assertArrayEquals(expected: ByteArray, actual: ByteArray) {
    assertEquals(expected.size, actual.size)
    for (i in expected.indices) {
        assertEquals(expected[i], actual[i])
    }
}
