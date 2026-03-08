package com.runanywhere.runanywherewatch.audio

import java.nio.ByteBuffer
import java.util.UUID

/**
 * Protocol for chunking audio data for BLE transport.
 */
class AudioChunkProtocol {
    
    /**
     * Result of chunking operation.
     */
    sealed class ChunkResult {
        data class Success(
            val chunkId: Int,
            val sequenceNumber: Int,
            val payload: ByteArray,
            val totalChunks: Int
        ) : ChunkResult()
        
        data class Error(val message: String) : ChunkResult()
    }
    
    /**
     * Audio chunk representation.
     */
    data class AudioChunk(
        val chunkId: Int,
        val sequenceNumber: Int,
        val payload: ByteArray,
        val totalChunks: Int,
        val sessionId: UUID
    )
    
    /**
     * Create a new streaming session.
     */
    fun createSession(config: AudioStreamConfig): UUID = config.sessionId
    
    /**
     * Create a chunk from audio data.
     */
    fun createChunk(sessionId: UUID, audioData: ByteArray, sequenceNumber: Int): ChunkResult {
        if (audioData == null || audioData.isEmpty()) {
            return ChunkResult.Error("Audio data cannot be null or empty")
        }
        
        val chunkId = sequenceNumber
        val totalChunks = (audioData.size + 1023) / 1024
        
        val startOffset = sequenceNumber * 1024
        val endOffset = minOf(startOffset + 1024, audioData.size)
        val payload = audioData.copyOfRange(startOffset, endOffset)
        
        return ChunkResult.Success(
            chunkId = chunkId,
            sequenceNumber = sequenceNumber,
            payload = payload,
            totalChunks = totalChunks
        )
    }
    
    /**
     * Reassemble audio from chunks.
     */
    fun reassembleAudio(chunks: List<AudioChunk>): ByteArray? {
        if (chunks.isEmpty()) return null
        
        val sortedChunks = chunks.sortedBy { it.sequenceNumber }
        val totalSize = sortedChunks.sumOf { it.payload.size }
        val result = ByteArray(totalSize)
        
        var offset = 0
        for (chunk in sortedChunks) {
            System.arraycopy(chunk.payload, 0, result, offset, chunk.payload.size)
            offset += chunk.payload.size
        }
        
        return result
    }
    
    /**
     * Validate chunk sequence.
     */
    fun validateSequence(chunks: List<AudioChunk>): Boolean {
        if (chunks.isEmpty()) return false
        
        val sortedChunks = chunks.sortedBy { it.sequenceNumber }
        for (i in sortedChunks.indices) {
            if (sortedChunks[i].sequenceNumber != i) return false
        }
        
        return sortedChunks.first().chunkId == 0
    }
    
    /**
     * Get chunk count for audio data.
     */
    fun getChunkCount(audioData: ByteArray): Int {
        return (audioData.size + 1023) / 1024
    }
    
    /**
     * Create chunk at specific index.
     */
    fun createChunkAt(sessionId: UUID, audioData: ByteArray, index: Int): ChunkResult {
        if (audioData == null || audioData.isEmpty()) {
            return ChunkResult.Error("Audio data cannot be null or empty")
        }
        
        val totalChunks = getChunkCount(audioData)
        if (index >= totalChunks) {
            return ChunkResult.Error("Index out of bounds")
        }
        
        return createChunk(sessionId, audioData, index)
    }
}
