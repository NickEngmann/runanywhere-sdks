package com.runanywhere.runanywherewatch

import android.util.Log
import java.util.UUID
import kotlin.collections.ArrayList

/**
 * UUID for the audio data notification characteristic from T-Watch
 */
object BleGattConstants {
    val AUDIO_DATA_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
}

/**
 * Manages receiving audio chunks from T-Watch via BLE notify callbacks
 * Reassembles chunks into complete audio buffers for transcription
 */
class AudioReceiver {
    private val TAG = "AudioReceiver"
    
    private val audioChunks = mutableMapOf<String, MutableList<ByteArray>>()
    private val chunkSequences = mutableMapOf<String, Int>()
    private val listeners = mutableListOf<AudioReceiverListener>()
    
    /**
     * Receives an audio chunk from BLE notify callback
     * @param sessionId Unique session identifier for this audio recording
     * @param chunkIndex Index of this chunk in the sequence
     * @param chunkData The audio data chunk (PCM format)
     * @return True if chunk was successfully added, false otherwise
     */
    fun receiveChunk(sessionId: String, chunkIndex: Int, chunkData: ByteArray): Boolean {
        Log.d(TAG, "Received chunk $chunkIndex for session $sessionId, size: ${chunkData.size}")
        
        // Initialize storage for new sessions
        if (!audioChunks.containsKey(sessionId)) {
            audioChunks[sessionId] = mutableListOf()
            chunkSequences[sessionId] = 0
        }
        
        // Validate chunk index
        val expectedIndex = chunkSequences[sessionId] ?: 0
        if (chunkIndex != expectedIndex) {
            Log.w(TAG, "Chunk index mismatch: expected $expectedIndex, got $chunkIndex")
            // Still accept it - BLE can be unreliable
            chunkSequences[sessionId] = chunkIndex + 1
        } else {
            chunkSequences[sessionId] = chunkIndex + 1
        }
        
        // Store the chunk
        audioChunks[sessionId]?.add(chunkData)
        
        return true
    }
    
    /**
     * Registers a listener for audio buffer completion events
     */
    fun addListener(listener: AudioReceiverListener) {
        listeners.add(listener)
    }
    
    /**
     * Unregisters a listener
     */
    fun removeListener(listener: AudioReceiverListener) {
        listeners.remove(listener)
    }
    
    /**
     * Attempts to assemble a complete audio buffer from received chunks
     * Returns the assembled buffer if all expected chunks are received
     */
    fun assembleBuffer(sessionId: String): ByteArray? {
        val chunks = audioChunks[sessionId]
        if (chunks == null || chunks.isEmpty()) {
            return null
        }
        
        // Check if we have all chunks (no gaps in sequence)
        val expectedCount = chunkSequences[sessionId] ?: 0
        if (chunks.size < expectedCount) {
            Log.d(TAG, "Session $sessionId has ${chunks.size} chunks, expected $expectedCount")
            return null
        }
        
        // Assemble the buffer
        val bufferSize = chunks.sumOf { it.size }
        val buffer = ByteArray(bufferSize)
        var offset = 0
        
        for (chunk in chunks) {
            chunk.copyInto(buffer, offset)
            offset += chunk.size
        }
        
        // Clear stored chunks after successful assembly
        audioChunks.remove(sessionId)
        chunkSequences.remove(sessionId)
        
        Log.d(TAG, "Assembled buffer for session $sessionId, size: $bufferSize")
        return buffer
    }
    
    /**
     * Clears all stored data for a session
     */
    fun clearSession(sessionId: String) {
        audioChunks.remove(sessionId)
        chunkSequences.remove(sessionId)
        Log.d(TAG, "Cleared session $sessionId")
    }
    
    /**
     * Clears all stored data
     */
    fun clearAll() {
        audioChunks.clear()
        chunkSequences.clear()
        Log.d(TAG, "Cleared all audio data")
    }
    
    /**
     * Listener interface for audio buffer completion events
     */
    interface AudioReceiverListener {
        fun onAudioBufferReady(sessionId: String, buffer: ByteArray)
        fun onAudioChunkReceived(sessionId: String, chunkIndex: Int, chunkSize: Int)
    }
}

/**
 * Simple implementation that logs audio buffer events
 */
class LoggingAudioReceiverListener : AudioReceiver.AudioReceiverListener {
    private val TAG = "AudioReceiverListener"
    
    override fun onAudioBufferReady(sessionId: String, buffer: ByteArray) {
        Log.d(TAG, "Audio buffer ready for session $sessionId, size: ${buffer.size}")
    }
    
    override fun onAudioChunkReceived(sessionId: String, chunkIndex: Int, chunkSize: Int) {
        Log.d(TAG, "Chunk $chunkIndex received for session $sessionId, size: $chunkSize")
    }
}
