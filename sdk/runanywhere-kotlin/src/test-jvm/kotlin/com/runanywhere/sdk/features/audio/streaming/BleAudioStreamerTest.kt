package com.runanywhere.sdk.features.audio.streaming

import com.runanywhere.sdk.features.audio.streaming.AudioChunkStreamingProtocol.*
import com.runanywhere.sdk.features.audio.streaming.BleAudioStreamer.*
import com.runanywhere.sdk.features.stt.AudioChunk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * Tests for BleAudioStreamer
 */
class BleAudioStreamerTest {
    
    @Test
    fun testBleAudioStreamerConfiguration() {
        val config = BleAudioStreamerConfig(
            bitrate = BitrateConfig.MEDIUM,
            chunkDurationMs = 100,
            enableErrorCorrection = true,
            maxRetries = 3,
            retryDelayMs = 50
        )
        
        assertEquals(BitrateConfig.MEDIUM, config.bitrate)
        assertEquals(100, config.chunkDurationMs)
        assertTrue(config.enableErrorCorrection)
        assertEquals(3, config.maxRetries)
        assertEquals(50, config.retryDelayMs)
    }
    
    @Test
    fun testBleAudioStreamerConfigDefaults() {
        val config = BleAudioStreamerConfig()
        
        assertEquals(BitrateConfig.LOW, config.bitrate)
        assertEquals(DEFAULT_CHUNK_DURATION_MS, config.chunkDurationMs)
        assertTrue(config.enableErrorCorrection)
        assertEquals(DEFAULT_MAX_RETRIES, config.maxRetries)
        assertEquals(DEFAULT_RETRY_DELAY_MS, config.retryDelayMs)
    }
    
    @Test
    fun testBleAudioStreamerStateTransition() {
        val streamer = BleAudioStreamer(
            config = BleAudioStreamerConfig(
                bitrate = BitrateConfig.MEDIUM,
                chunkDurationMs = 100
            )
        )
        
        assertEquals(StreamingState.IDLE, streamer.state)
        
        // Simulate state transitions
        streamer.startStreaming()
        assertEquals(StreamingState.CONNECTING, streamer.state)
        
        streamer.connectToDevice("test-device")
        assertEquals(StreamingState.CONNECTED, streamer.state)
        
        streamer.startTransmission()
        assertEquals(StreamingState.TRANSMITTING, streamer.state)
        
        streamer.stopTransmission()
        assertEquals(StreamingState.IDLE, streamer.state)
    }
    
    @Test
    fun testBleAudioStreamerSequenceNumbering() {
        val streamer = BleAudioStreamer(
            config = BleAudioStreamerConfig(
                bitrate = BitrateConfig.MEDIUM,
                chunkDurationMs = 100
            )
        )
        
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
            ),
            AudioChunk(
                sequenceNumber = 3,
                data = byteArrayOf(7, 8, 9),
                timestampMs = 1200L,
                sampleRate = 16000,
                channelCount = 1,
                bitDepth = 16
            )
        )
        
        val encodedChunks = chunks.chunkForStreaming(
            bitrate = BitrateConfig.MEDIUM,
            chunkDurationMs = 100
        ).toList()
        
        assertEquals(3, encodedChunks.size)
        assertEquals(0, encodedChunks[0].sequenceNumber)
        assertEquals(1, encodedChunks[1].sequenceNumber)
        assertEquals(2, encodedChunks[2].sequenceNumber)
    }
    
    @Test
    fun testBleAudioStreamerErrorDetection() {
        val streamer = BleAudioStreamer(
            config = BleAudioStreamerConfig(
                bitrate = BitrateConfig.MEDIUM,
                chunkDurationMs = 100
            )
        )
        
        val chunk = AudioChunk(
            sequenceNumber = 1,
            data = byteArrayOf(1, 2, 3),
            timestampMs = 1000L,
            sampleRate = 16000,
            channelCount = 1,
            bitDepth = 16
        )
        
        val encodedChunk = chunk.encodeWithProtocol(0, Flags(
            isRetransmission = false,
            isLastChunk = true,
            errorCorrectionEnabled = true
        ))
        
        // Valid CRC should pass
        assertTrue(streamer.verifyChunkCrc(encodedChunk))
        
        // Corrupt the payload
        val corruptedPayload = encodedChunk.payload.copyOf()
        corruptedPayload[0] = (corruptedPayload[0] + 1).toByte()
        val corruptedChunk = encodedChunk.copy(payload = corruptedPayload)
        
        // Invalid CRC should fail
        assertFalse(streamer.verifyChunkCrc(corruptedChunk))
    }
    
    @Test
    fun testBleAudioStreamerRetransmissionRequest() {
        val streamer = BleAudioStreamer(
            config = BleAudioStreamerConfig(
                bitrate = BitrateConfig.MEDIUM,
                chunkDurationMs = 100
            )
        )
        
        // Simulate missing chunk detection
        val missingSequence = 5
        val retransmissionRequest = streamer.createRetransmissionRequest(missingSequence)
        
        assertEquals(missingSequence, retransmissionRequest.missingSequenceNumber)
        assertEquals(1, retransmissionRequest.requestCount)
        assertTrue(retransmissionRequest.isRetransmissionRequest)
    }
    
    @Test
    fun testBleAudioStreamerChunkQueue() {
        val queue = ChunkQueue(maxSize = 10)
        
        // Add chunks
        queue.enqueue(0, EncodedChunk(
            sequenceNumber = 0,
            crc = 12345,
            flags = Flags(false, false, true),
            payload = byteArrayOf(1, 2, 3),
            totalSize = 10
        ))
        
        queue.enqueue(1, EncodedChunk(
            sequenceNumber = 1,
            crc = 12346,
            flags = Flags(false, false, true),
            payload = byteArrayOf(4, 5, 6),
            totalSize = 10
        ))
        
        assertEquals(2, queue.size)
        assertEquals(0, queue.peek()?.sequenceNumber)
        
        // Dequeue
        val dequeued = queue.dequeue()
        assertEquals(0, dequeued?.sequenceNumber)
        assertEquals(1, queue.size)
    }
    
    @Test
    fun testBleAudioStreamerRetransmissionQueue() {
        val queue = RetransmissionQueue(maxSize = 5)
        
        // Add retransmission requests
        queue.enqueue(5, RetransmissionRequest(
            missingSequenceNumber = 5,
            requestCount = 1,
            timestampMs = 1000L,
            isRetransmissionRequest = true
        ))
        
        assertEquals(1, queue.size)
        assertEquals(5, queue.peek()?.missingSequenceNumber)
        
        // Dequeue
        val dequeued = queue.dequeue()
        assertEquals(5, dequeued?.missingSequenceNumber)
        assertEquals(0, queue.size)
    }
    
    @Test
    fun testBleAudioStreamerConfigSerialization() {
        val config = BleAudioStreamerConfig(
            bitrate = BitrateConfig.HIGH,
            chunkDurationMs = 200,
            enableErrorCorrection = false,
            maxRetries = 5,
            retryDelayMs = 100
        )
        
        val serialized = config.serialize()
        val deserialized = BleAudioStreamerConfig.deserialize(serialized)
        
        assertEquals(config.bitrate, deserialized.bitrate)
        assertEquals(config.chunkDurationMs, deserialized.chunkDurationMs)
        assertEquals(config.enableErrorCorrection, deserialized.enableErrorCorrection)
        assertEquals(config.maxRetries, deserialized.maxRetries)
        assertEquals(config.retryDelayMs, deserialized.retryDelayMs)
    }
    
    @Test
    fun testBleAudioStreamerChunkSizeCalculation() {
        val config = BleAudioStreamerConfig(
            bitrate = BitrateConfig.MEDIUM,
            chunkDurationMs = 100
        )
        
        val chunkSize = calculateChunkSize(config)
        
        assertTrue(chunkSize > 0)
        assertTrue(chunkSize <= MAX_CHUNK_SIZE_BYTES)
        
        // Higher bitrate should produce larger chunks
        val highConfig = config.copy(bitrate = BitrateConfig.HIGH)
        val highChunkSize = calculateChunkSize(highConfig)
        
        assertTrue(highChunkSize >= chunkSize)
    }
    
    @Test
    fun testBleAudioStreamerSequenceValidation() {
        val streamer = BleAudioStreamer(
            config = BleAudioStreamerConfig(
                bitrate = BitrateConfig.MEDIUM,
                chunkDurationMs = 100
            )
        )
        
        // Valid sequence
        assertTrue(streamer.validateSequenceNumber(0))
        assertTrue(streamer.validateSequenceNumber(100))
        
        // Invalid sequence (negative)
        assertFalse(streamer.validateSequenceNumber(-1))
    }
}
