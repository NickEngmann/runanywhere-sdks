package com.runanywhere.runanywherewatch

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class AudioBufferAssemblerTest {
    
    @Test
    fun testAddChunk() {
        val assembler = AudioBufferAssembler()
        val sessionId = "test-session"
        val chunkData = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        
        assembler.addChunk(sessionId, 0, chunkData)
        
        assertEquals("Should have 1 chunk", 1, assembler.getPendingChunkCount(sessionId))
    }
    
    @Test
    fun testAddMultipleChunks() {
        val assembler = AudioBufferAssembler()
        val sessionId = "test-session"
        
        assembler.addChunk(sessionId, 0, byteArrayOf(0x01, 0x02))
        assembler.addChunk(sessionId, 1, byteArrayOf(0x03, 0x04))
        assembler.addChunk(sessionId, 2, byteArrayOf(0x05, 0x06))
        
        assertEquals("Should have 3 chunks", 3, assembler.getPendingChunkCount(sessionId))
    }
    
    @Test
    fun testAssembleBuffer() {
        val assembler = AudioBufferAssembler()
        val sessionId = "test-session"
        
        assembler.addChunk(sessionId, 0, byteArrayOf(0x01, 0x02))
        assembler.addChunk(sessionId, 1, byteArrayOf(0x03, 0x04))
        
        val buffer = assembler.assemble(sessionId)
        
        assertNotNull("Buffer should not be null", buffer)
        assertEquals("Buffer should have 4 bytes", 4, buffer.size)
        assertArrayEquals("Buffer should contain correct data", byteArrayOf(0x01, 0x02, 0x03, 0x04), buffer)
    }
    
    @Test
    fun testAssembleOutOfOrderChunks() {
        val assembler = AudioBufferAssembler()
        val sessionId = "test-session"
        
        assembler.addChunk(sessionId, 2, byteArrayOf(0x05, 0x06))
        assembler.addChunk(sessionId, 0, byteArrayOf(0x01, 0x02))
        assembler.addChunk(sessionId, 1, byteArrayOf(0x03, 0x04))
        
        val buffer = assembler.assemble(sessionId)
        
        assertNotNull("Buffer should not be null", buffer)
        assertEquals("Buffer should have 6 bytes", 6, buffer.size)
        assertArrayEquals("Buffer should be in order", byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06), buffer)
    }
    
    @Test
    fun testAssembleNonExistentSession() {
        val assembler = AudioBufferAssembler()
        val buffer = assembler.assemble("non-existent-session")
        
        assertNull("Buffer should be null for non-existent session", buffer)
    }
    
    @Test
    fun testClearAll() {
        val assembler = AudioBufferAssembler()
        val sessionId = "test-session"
        
        assembler.addChunk(sessionId, 0, byteArrayOf(0x01, 0x02))
        assembler.clearAll()
        
        assertEquals("Should have no pending chunks", 0, assembler.getPendingChunkCount(sessionId))
        assertNull("Assembled buffer should be null", assembler.assemble(sessionId))
    }
    
    @Test
    fun testPCMToFloatArray() {
        val assembler = AudioBufferAssembler()
        val sessionId = "test-session"
        
        // Create a simple PCM buffer
        val pcmData = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07)
        assembler.addChunk(sessionId, 0, pcmData)
        
        val buffer = assembler.assemble(sessionId)
        val floatArray = assembler.pcmToFloatArray(buffer!!)
        
        assertNotNull("Float array should not be null", floatArray)
        assertEquals("Float array should have 4 elements (8 bytes / 2 bytes per sample)", 4, floatArray.size)
    }
    
    @Test
    fun testGetDurationSeconds() {
        val assembler = AudioBufferAssembler()
        val sessionId = "test-session"
        
        // Create a PCM buffer with 16-bit samples at 16kHz
        val pcmData = ByteArray(320) // 320 bytes = 160 samples at 16kHz = 10 seconds
        assembler.addChunk(sessionId, 0, pcmData)
        
        val buffer = assembler.assemble(sessionId)
        val duration = assembler.getDurationSeconds(buffer!!)
        
        assertTrue("Duration should be approximately 10 seconds", Math.abs(duration - 10.0) < 0.1)
    }
    
    @Test
    fun testAudioFormatInfo() {
        val assembler = AudioBufferAssembler()
        val info = assembler.AudioFormatInfo()
        
        assertNotNull("Format info should not be null", info)
        assertEquals("Sample rate should be 16000", 16000, info.sampleRate)
        assertEquals("Channels should be 1", 1, info.channels)
        assertEquals("Bits per sample should be 16", 16, info.bitsPerSample)
    }
    
    @Test
    fun testMultipleSessions() {
        val assembler = AudioBufferAssembler()
        val sessionId1 = "session-1"
        val sessionId2 = "session-2"
        
        assembler.addChunk(sessionId1, 0, byteArrayOf(0x01, 0x02))
        assembler.addChunk(sessionId2, 0, byteArrayOf(0x03, 0x04))
        
        val buffer1 = assembler.assemble(sessionId1)
        val buffer2 = assembler.assemble(sessionId2)
        
        assertNotNull("Buffer 1 should not be null", buffer1)
        assertNotNull("Buffer 2 should not be null", buffer2)
        assertArrayEquals("Buffer 1 should have correct data", byteArrayOf(0x01, 0x02), buffer1)
        assertArrayEquals("Buffer 2 should have correct data", byteArrayOf(0x03, 0x04), buffer2)
    }
}
