package com.runanywhere.sdk.features.audio.streaming

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull

/**
 * Tests for BleAudioStreamer and BleTransport
 */
class BleAudioStreamerTest {

    /**
     * Concrete test implementation of BleAudioStreamer
     */
    private class TestStreamer(
        config: StreamingConfig = StreamingConfig()
    ) : BleAudioStreamer(config) {

        private val _testAudioChunks = MutableSharedFlow<AudioChunk>(replay = 10)

        override val audioChunks: Flow<AudioChunk> = _testAudioChunks
        override val streamingEvents: Flow<StreamingEvent> = _streamingEvents

        /** Expose protected members for testing */
        fun readSequenceNumber(): Int = sequenceNumber
        fun readLastSequenceNumber(): Int = lastSequenceNumber
        fun readRetransmissionCount(): Int = retransmissionCount

        /** Expose protected encodeChunk for testing */
        fun testEncodeChunk(chunk: AudioChunk): EncodedChunk = encodeChunk(chunk)
        fun testDecodeChunk(encoded: EncodedChunk): AudioChunk? = decodeChunk(encoded)
        fun testVerifyCRC32(data: ByteArray, expected: Int): Boolean = verifyCRC32(data, expected)
        fun testCalculateCRC32(data: ByteArray): Int = calculateCRC32(data)

        /** Feed chunks for streaming tests */
        suspend fun feedChunk(chunk: AudioChunk) {
            _testAudioChunks.emit(chunk)
        }
    }

    /**
     * Test BLE transport mock
     */
    private class MockTransport : BleTransport {
        val sentChunks = mutableListOf<EncodedChunk>()
        var connected = true

        override fun sendChunk(encoded: EncodedChunk) {
            sentChunks.add(encoded)
        }

        override fun receiveChunk(): EncodedChunk? = null
        override fun isConnected(): Boolean = connected
        override fun close() { connected = false }
    }

    // --- Construction ---

    @Test
    fun testStreamerInitialState() {
        val streamer = TestStreamer()
        assertEquals(0, streamer.readSequenceNumber())
        assertEquals(0, streamer.readLastSequenceNumber())
        assertEquals(0, streamer.readRetransmissionCount())
    }

    @Test
    fun testStreamerCustomConfig() {
        val config = StreamingConfig(
            bitrate = BitrateConfig.HIGH,
            chunkDurationMs = 50,
            enableErrorCorrection = false
        )
        val streamer = TestStreamer(config)
        assertEquals(0, streamer.readSequenceNumber())
    }

    // --- Chunk encoding ---

    @Test
    fun testEncodeChunkProducesValidCRC() {
        val streamer = TestStreamer()
        val chunk = AudioChunk(data = byteArrayOf(1, 2, 3), sequenceNumber = 0)
        val encoded = streamer.testEncodeChunk(chunk)

        val expectedCRC = streamer.testCalculateCRC32(byteArrayOf(1, 2, 3))
        assertEquals(expectedCRC, encoded.crc32)
    }

    @Test
    fun testEncodeChunkPreservesData() {
        val streamer = TestStreamer()
        val data = byteArrayOf(10, 20, 30, 40)
        val chunk = AudioChunk(data = data, sequenceNumber = 5)
        val encoded = streamer.testEncodeChunk(chunk)

        assertTrue(encoded.payload.contentEquals(data))
    }

    @Test
    fun testEncodeChunkLastChunkFlag() {
        val streamer = TestStreamer()
        val chunk = AudioChunk(data = byteArrayOf(1), sequenceNumber = 0, isLastChunk = true)
        val encoded = streamer.testEncodeChunk(chunk)
        assertTrue(encoded.flags.isLastChunk)
    }

    // --- Chunk decoding ---

    @Test
    fun testDecodeChunkRoundTrip() {
        val streamer = TestStreamer()
        val original = AudioChunk(data = byteArrayOf(5, 6, 7), sequenceNumber = 3, isLastChunk = true)
        val encoded = streamer.testEncodeChunk(original)
        val decoded = streamer.testDecodeChunk(encoded)

        assertTrue(decoded != null)
        assertTrue(original.data.contentEquals(decoded.data))
        assertEquals(original.isLastChunk, decoded.isLastChunk)
    }

    // --- CRC verification ---

    @Test
    fun testVerifyCRC32Valid() {
        val streamer = TestStreamer()
        val data = byteArrayOf(1, 2, 3)
        val crc = streamer.testCalculateCRC32(data)
        assertTrue(streamer.testVerifyCRC32(data, crc))
    }

    @Test
    fun testVerifyCRC32Invalid() {
        val streamer = TestStreamer()
        val data = byteArrayOf(1, 2, 3)
        assertTrue(!streamer.testVerifyCRC32(data, 99999))
    }

    // --- Retransmission ---

    @Test
    fun testRequestRetransmission() = runBlocking {
        val streamer = TestStreamer()
        streamer.requestRetransmission(setOf(5, 10))

        // Give coroutine time to execute
        kotlinx.coroutines.delay(100)
        assertEquals(2, streamer.readRetransmissionCount())
    }

    @Test
    fun testRequestRetransmissionEmitsEvents() = runBlocking {
        val streamer = TestStreamer()

        val event = withTimeoutOrNull(1000) {
            coroutineScope {
                val deferred = async {
                    streamer.streamingEvents.first()
                }
                kotlinx.coroutines.delay(50)
                streamer.requestRetransmission(setOf(7))
                deferred.await()
            }
        }

        assertTrue(event is StreamingEvent.RetransmissionRequested)
        assertEquals(7, (event as StreamingEvent.RetransmissionRequested).sequenceNumber)
    }

    // --- Handle received chunk ---

    @Test
    fun testHandleReceivedChunkValidSequence() = runBlocking {
        val streamer = TestStreamer()
        val data = byteArrayOf(1, 2, 3)
        val crc = streamer.testCalculateCRC32(data)

        val encoded = EncodedChunk(
            sequenceNumber = 1,
            payload = data,
            crc32 = crc,
            flags = Flags(),
            bitrate = BitrateConfig.MEDIUM,
            timestamp = 0
        )

        streamer.handleReceivedChunk(encoded)
        kotlinx.coroutines.delay(100)
        assertEquals(1, streamer.readLastSequenceNumber())
    }

    @Test
    fun testHandleReceivedChunkBadCRC() = runBlocking {
        val streamer = TestStreamer()

        val encoded = EncodedChunk(
            sequenceNumber = 1,
            payload = byteArrayOf(1, 2, 3),
            crc32 = 12345, // wrong CRC
            flags = Flags(),
            bitrate = BitrateConfig.MEDIUM,
            timestamp = 0
        )

        streamer.handleReceivedChunk(encoded)
        kotlinx.coroutines.delay(100)

        // Should have requested retransmission (bad CRC)
        assertTrue(streamer.readRetransmissionCount() > 0)
        // Should NOT have advanced sequence number
        assertEquals(0, streamer.readLastSequenceNumber())
    }

    // --- MockTransport ---

    @Test
    fun testMockTransport() {
        val transport = MockTransport()
        assertTrue(transport.isConnected())

        val chunk = EncodedChunk(
            sequenceNumber = 0,
            payload = byteArrayOf(1),
            crc32 = 0,
            flags = Flags(),
            bitrate = BitrateConfig.LOW,
            timestamp = 0
        )
        transport.sendChunk(chunk)
        assertEquals(1, transport.sentChunks.size)

        assertNull(transport.receiveChunk())

        transport.close()
        assertTrue(!transport.isConnected())
    }

    // --- Stop streaming ---

    @Test
    fun testStopStreamingResetsState() = runBlocking {
        val streamer = TestStreamer()
        streamer.requestRetransmission(setOf(1))
        kotlinx.coroutines.delay(100)
        assertTrue(streamer.readRetransmissionCount() > 0)

        streamer.stopStreaming()
        assertEquals(0, streamer.readSequenceNumber())
        assertEquals(0, streamer.readLastSequenceNumber())
        assertEquals(0, streamer.readRetransmissionCount())
    }
}
