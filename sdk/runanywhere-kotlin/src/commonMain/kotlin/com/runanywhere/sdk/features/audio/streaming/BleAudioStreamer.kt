package com.runanywhere.sdk.features.audio.streaming

import com.runanywhere.sdk.features.audio.streaming.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.min

/**
 * BLE Audio Streamer - Manages audio chunk streaming over BLE transport
 * with automatic retransmission and error correction.
 *
 * This class handles:
 * - Chunk encoding with configurable bitrate
 * - Sequence numbering and ordering
 * - CRC error detection and retransmission requests
 * - BLE transport integration
 */
abstract class BleAudioStreamer(
    private val config: StreamingConfig = StreamingConfig(
        bitrate = BitrateConfig.MEDIUM,
        chunkDurationMs = DEFAULT_CHUNK_DURATION_MS,
        enableErrorCorrection = true
    )
) {
    
    protected val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    protected var sequenceNumber: Int = 0
    protected var lastSequenceNumber: Int = 0
    protected var retransmissionCount: Int = 0
    
    /**
     * Flow of audio chunks ready to be transmitted
     */
    abstract val audioChunks: Flow<AudioChunk>
    
    /**
     * Flow of streaming events (status, errors, completion)
     */
    abstract val streamingEvents: Flow<StreamingEvent>
    
    protected val _streamingEvents = MutableSharedFlow<StreamingEvent>()
    protected val _audioChunks = MutableSharedFlow<AudioChunk>()
    
    private var isStreaming: Boolean = false
    private var streamingJob: Job? = null
    
    /**
     * Request retransmission of specific chunk(s) based on missing sequence numbers
     */
    fun requestRetransmission(missingSequenceNumbers: Set<Int>) {
        scope.launch {
            missingSequenceNumbers.forEach { seqNum ->
                retransmissionCount++
                _streamingEvents.emit(StreamingEvent.RetransmissionRequested(seqNum, retransmissionCount))
            }
        }
    }
    
    /**
     * Calculate CRC32 for chunk data
     */
    protected fun calculateCRC32(data: ByteArray): Int {
        var crc: Int = 0xFFFFFFFF.toInt()
        for (byte in data) {
            crc = crc xor (byte.toInt() and 0xFF)
            for (bit in 0..7) {
                if ((crc and 1) != 0) {
                    crc = (crc ushr 1) xor 0xEDB88320
                } else {
                    crc = crc ushr 1
                }
            }
        }
        return crc xor 0xFFFFFFFF
    }
    
    /**
     * Verify CRC32 of received chunk
     */
    protected fun verifyCRC32(data: ByteArray, expectedCRC: Int): Boolean {
        return calculateCRC32(data) == expectedCRC
    }
    
    /**
     * Encode audio chunk with sequence number and CRC
     */
    protected fun encodeChunk(chunk: AudioChunk): EncodedChunk {
        val payload = chunk.encode()
        val crc = calculateCRC32(payload)
        val flags = Flags(
            isRetransmission = false,
            isLastChunk = chunk.isLastChunk,
            errorCorrectionEnabled = config.enableErrorCorrection
        )
        
        val seqNum = sequenceNumber
        return EncodedChunk(
            sequenceNumber = seqNum,
            payload = payload,
            crc32 = crc,
            flags = flags,
            bitrate = config.bitrate,
            timestamp = 0
        )
    }
    
    /**
     * Decode encoded chunk back to AudioChunk
     */
    protected fun decodeChunk(encoded: EncodedChunk): AudioChunk? {
        val payload = encoded.payload
        val isLast = encoded.flags.isLastChunk
        
        return AudioChunk(
            data = payload,
            sequenceNumber = encoded.sequenceNumber,
            isLastChunk = isLast
        )
    }
    
    /**
     * Start audio streaming session
     */
    fun startStreaming(bleTransport: BleTransport) {
        if (isStreaming) return
        
        streamingJob = scope.launch {
            try {
                _streamingEvents.emit(StreamingEvent.StreamStarted)
                isStreaming = true
                
                // Collect audio chunks and stream them
                audioChunks.collect { chunk ->
                    val encoded = encodeChunk(chunk)
                    bleTransport.sendChunk(encoded)
                    
                    if (chunk.isLastChunk) {
                        _streamingEvents.emit(StreamingEvent.StreamCompleted)
                        isStreaming = false
                    }
                }
            } catch (e: Exception) {
                _streamingEvents.emit(StreamingEvent.StreamError(e))
                isStreaming = false
            }
        }
    }
    
    /**
     * Stop audio streaming session
     */
    fun stopStreaming() {
        isStreaming = false
        streamingJob?.cancel()
        reset()
    }
    
    /**
     * Handle received chunk from BLE transport
     */
    fun handleReceivedChunk(encoded: EncodedChunk) {
        scope.launch {
            // Verify sequence number
            if (encoded.sequenceNumber != lastSequenceNumber + 1) {
                // Missing chunk detected
                val missingSeq = encoded.sequenceNumber - lastSequenceNumber - 1
                requestRetransmission(setOf(missingSeq))
                return@launch
            }
            
            // Verify CRC
            if (!verifyCRC32(encoded.payload, encoded.crc32)) {
                requestRetransmission(setOf(encoded.sequenceNumber))
                return@launch
            }
            
            // Decode chunk
            val chunk = decodeChunk(encoded)
            if (chunk != null) {
                lastSequenceNumber = encoded.sequenceNumber
                _audioChunks.emit(chunk)
                
                if (encoded.flags.isLastChunk) {
                    _streamingEvents.emit(StreamingEvent.StreamCompleted)
                }
            } else {
                // Decoding failed, request retransmission
                requestRetransmission(setOf(encoded.sequenceNumber))
            }
        }
    }
    
    /**
     * Reset streaming state
     */
    protected fun reset() {
        sequenceNumber = 0
        lastSequenceNumber = 0
        retransmissionCount = 0
    }
}

/**
 * BLE transport interface for audio chunk streaming
 */
interface BleTransport {
    /**
     * Send encoded audio chunk over BLE
     */
    fun sendChunk(encoded: EncodedChunk)
    
    /**
     * Receive encoded audio chunk from BLE
     */
    fun receiveChunk(): EncodedChunk?
    
    /**
     * Check if BLE connection is active
     */
    fun isConnected(): Boolean
    
    /**
     * Close BLE connection
     */
    fun close()
}

