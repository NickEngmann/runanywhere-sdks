package com.runanywhere.runanywherewatch

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * Assembles audio chunks into PCM buffers with proper format handling
 * Supports 16-bit PCM, 16kHz sample rate (T-Watch standard)
 */
class AudioBufferAssembler {
    private val TAG = "AudioBufferAssembler"
    
    // Audio format constants for T-Watch
    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNELS = 1
        const val BITS_PER_SAMPLE = 16
        const val AUDIO_FORMAT = "pcm_s16le" // Little-endian signed 16-bit PCM
    }
    
    private val pendingBuffers = mutableMapOf<String, MutableList<ByteArray>>()
    private val bufferSequences = mutableMapOf<String, Int>()
    
    /**
     * Adds an audio chunk to the pending buffer assembly
     * @param sessionId Unique session identifier
     * @param chunkIndex Index of this chunk
     * @param chunkData Raw audio chunk data
     * @return True if chunk was added successfully
     */
    fun addChunk(sessionId: String, chunkIndex: Int, chunkData: ByteArray): Boolean {
        Log.d(TAG, "Adding chunk $chunkIndex to session $sessionId, size: ${chunkData.size}")
        
        if (!pendingBuffers.containsKey(sessionId)) {
            pendingBuffers[sessionId] = mutableListOf()
            bufferSequences[sessionId] = 0
        }
        
        pendingBuffers[sessionId]?.add(chunkData)
        bufferSequences[sessionId] = chunkIndex + 1
        
        return true
    }
    
    /**
     * Attempts to assemble a complete PCM buffer from pending chunks
     * @param sessionId The session to assemble
     * @return Assembled PCM buffer if complete, null otherwise
     */
    fun assemble(sessionId: String): ByteArray? {
        val chunks = pendingBuffers[sessionId]
        if (chunks == null || chunks.isEmpty()) {
            return null
        }
        
        val expectedCount = bufferSequences[sessionId] ?: 0
        if (chunks.size < expectedCount) {
            Log.d(TAG, "Session $sessionId incomplete: ${chunks.size}/${expectedCount}")
            return null
        }
        
        // Calculate total size
        val totalSize = chunks.sumOf { it.size }
        
        // Create ByteBuffer for proper endianness handling
        val byteBuffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        
        for (chunk in chunks) {
            byteBuffer.put(chunk)
        }
        
        // Clear pending data
        pendingBuffers.remove(sessionId)
        bufferSequences.remove(sessionId)
        
        Log.d(TAG, "Assembled PCM buffer: $totalSize bytes, ${totalSize / 2} samples")
        return byteBuffer.array()
    }
    
    /**
     * Converts PCM byte array to float array for ML inference
     * Normalizes values to -1.0 to 1.0 range
     */
    fun pcmToFloatArray(pcmData: ByteArray): FloatArray {
        val floatArray = FloatArray(pcmData.size / 2)
        val byteBuffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN)
        
        for (i in floatArray.indices) {
            val sample = byteBuffer.short
            floatArray[i] = sample / 32768.0f
        }
        
        return floatArray
    }
    
    /**
     * Calculates audio duration in seconds from PCM data
     */
    fun getDurationSeconds(pcmData: ByteArray): Double {
        val numSamples = pcmData.size / 2
        return numSamples.toDouble() / SAMPLE_RATE
    }
    
    /**
     * Clears all pending buffers
     */
    fun clearAll() {
        pendingBuffers.clear()
        bufferSequences.clear()
        Log.d(TAG, "Cleared all pending buffers")
    }
    
    /**
     * Gets the number of pending chunks for a session
     */
    fun getPendingChunkCount(sessionId: String): Int {
        return pendingBuffers[sessionId]?.size ?: 0
    }
}

/**
 * Audio format information for metadata
 */
data class AudioFormatInfo(
    val sampleRate: Int = SAMPLE_RATE,
    val channels: Int = CHANNELS,
    val bitsPerSample: Int = BITS_PER_SAMPLE,
    val format: String = AUDIO_FORMAT
) {
    fun getBytesPerSecond(): Int = sampleRate * channels * bitsPerSample / 8
    fun getSamplesPerSecond(): Int = sampleRate * channels
}
