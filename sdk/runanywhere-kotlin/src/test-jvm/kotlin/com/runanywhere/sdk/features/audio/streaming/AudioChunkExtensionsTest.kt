package com.runanywhere.sdk.features.audio.streaming

import com.runanywhere.sdk.features.audio.streaming.AudioChunkStreamingProtocol.*
import com.runanywhere.sdk.features.stt.AudioChunk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

/**
 * Tests for AudioChunk extension functions
 */
class AudioChunkExtensionsTest {
    
    @Test
    fun testEncodeAndDecodeAudioChunk() {
        val originalChunk = AudioChunk(
            sequenceNumber = 1,
            data = byteArrayOf(1, 2, 3, 4, 5),
            timestampMs = 1000L,
            sampleRate = 16000,
            channelCount = 1,
            bitDepth = 16
        )
        
        val encoded = originalChunk.encode()
        val decoded = encoded.decodeAudioChunk()
        
        assertEquals(originalChunk.sequenceNumber, decoded.sequenceNumber)
        assertEquals(originalChunk.timestampMs, decoded.timestampMs)
        assertEquals(originalChunk.sampleRate, decoded.sampleRate)
        assertEquals(originalChunk.channelCount, decoded.channelCount)
        assertEquals(originalChunk.bitDepth, decoded.bitDepth)
        assertTrue(decoded.data.contentEquals(originalChunk.data))
    }
    
    @Test
    fun testEncodeWithProtocol() {
        val chunk = AudioChunk(
            sequenceNumber = 1,
            data = byteArrayOf(1, 2, 3, 4, 5),
            timestampMs = 1000L,
            sampleRate = 16000,
            channelCount = 1,
            bitDepth = 16
        )
        
        val flags = Flags(
            isRetransmission = false,
            isLastChunk = true,
            errorCorrectionEnabled = true
        )
        
        val encodedChunk = chunk.encodeWithProtocol(1, flags)
        
        assertEquals(1, encodedChunk.sequenceNumber)
        assertEquals(flags, encodedChunk.flags)
        assertEquals(chunk.encode().size + HEADER_SIZE, encodedChunk.totalSize)
        assertTrue(encodedChunk.crc != 0)
    }
    
    @Test
    fun testDecodeEncodedChunk() {
        val originalChunk = AudioChunk(
            sequenceNumber = 1,
            data = byteArrayOf(1, 2, 3, 4, 5),
            timestampMs = 1000L,
            sampleRate = 16000,
            channelCount = 1,
            bitDepth = 16
        )
        
        val flags = Flags(
            isRetransmission = false,
            isLastChunk = true,
            errorCorrectionEnabled = true
        )
        
        val encodedChunk = originalChunk.encodeWithProtocol(1, flags)
        val encodedBytes = encodedChunk.payload
        
        val decoded = encodedBytes.decodeEncodedChunk()
        
        assertEquals(1, decoded.sequenceNumber)
        assertEquals(flags, decoded.flags)
        assertTrue(decoded.payload.contentEquals(encodedBytes))
        assertEquals(encodedChunk.crc, decoded.crc)
    }
    
    @Test
    fun testCalculateMaxChunkSize() {
        val lowSize = calculateMaxChunkSize(BitrateConfig.LOW, 100)
        val mediumSize = calculateMaxChunkSize(BitrateConfig.MEDIUM, 100)
        val highSize = calculateMaxChunkSize(BitrateConfig.HIGH, 100)
        
        assertTrue(lowSize <= mediumSize)
        assertTrue(mediumSize <= highSize)
        assertTrue(highSize <= MAX_CHUNK_SIZE_BYTES)
    }
    
    @Test
    fun testChunkForStreaming() {
        val chunks = listOf(
            AudioChunk(
                sequenceNumber = 1,
                data = byteArrayOf(1, 2, 3, 4, 5),
                timestampMs = 1000L,
                sampleRate = 16000,
                channelCount = 1,
                bitDepth = 16
            ),
            AudioChunk(
                sequenceNumber = 2,
                data = byteArrayOf(6, 7, 8, 9, 10),
                timestampMs = 1100L,
                sampleRate = 16000,
                channelCount = 1,
                bitDepth = 16
            )
        )
        
        val encodedChunks = chunks.chunkForStreaming(
            bitrate = BitrateConfig.MEDIUM,
            chunkDurationMs = 100
        ).toList()
        
        assertEquals(2, encodedChunks.size)
        assertEquals(0, encodedChunks[0].sequenceNumber)
        assertEquals(1, encodedChunks[1].sequenceNumber)
        assertTrue(encodedChunks[1].flags.isLastChunk)
    }
    
    @Test
    fun testReassembleChunks() {
        val chunks = listOf(
            AudioChunk(
                sequenceNumber = 1,
                data = byteArrayOf(1, 2, 3),
                timestampMs = 1000L,
                sampleRate = 16000,
                channelCount = 1,
                bitDepth = 16
            ),
            AudioChunk(
                sequenceNumber = 2,
                data = byteArrayOf(4, 5, 6),
                timestampMs = 1100L,
                sampleRate = 16000,
                channelCount = 1,
                bitDepth = 16
            )
        )
        
        val encodedChunks = chunks.chunkForStreaming(
            bitrate = BitrateConfig.MEDIUM,
            chunkDurationMs = 100
        )
        
        val reassembled = encodedChunks.reassembleChunks()
        
        assertEquals(2, reassembled.size)
        assertEquals(chunks[0].sequenceNumber, reassembled[0].sequenceNumber)
        assertEquals(chunks[1].sequenceNumber, reassembled[1].sequenceNumber)
    }
    
    @Test
    fun testReassembleChunksWithGap() {
        val chunks = listOf(
            AudioChunk(
                sequenceNumber = 1,
                data = byteArrayOf(1, 2, 3),
                timestampMs = 1000L,
                sampleRate = 16000,
                channelCount = 1,
                bitDepth = 16
            ),
            AudioChunk(
                sequenceNumber = 3,  // Gap: sequence 2 is missing
                data = byteArrayOf(4, 5, 6),
                timestampMs = 1200L,
                sampleRate = 16000,
                channelCount = 1,
                bitDepth = 16
            )
        )
        
        val encodedChunks = chunks.chunkForStreaming(
            bitrate = BitrateConfig.MEDIUM,
            chunkDurationMs = 100
        )
        
        val reassembled = encodedChunks.reassembleChunks()
        
        // Only first chunk should be reassembled due to gap
        assertEquals(1, reassembled.size)
    }
    
    @Test
    fun testDecodeEncodedChunkInvalidSize() {
        val invalidData = byteArrayOf(1, 2, 3)  // Too small for header
        
        assertFailsWith<IllegalArgumentException> {
            invalidData.decodeEncodedChunk()
        }
    }
}
