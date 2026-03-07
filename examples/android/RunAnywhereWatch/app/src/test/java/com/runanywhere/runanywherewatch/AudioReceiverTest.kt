package com.runanywhere.runanywherewatch

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class AudioReceiverTest {
    
    @Test
    fun testCreateNewSession() {
        val receiver = AudioReceiver()
        val sessionId = receiver.createSession()
        
        assertNotNull("Session ID should not be null", sessionId)
        assertTrue("Session ID should be valid UUID", sessionId.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")))
        assertEquals("Should have one active session", 1, receiver.activeSessions.size)
    }
    
    @Test
    fun testAddAudioChunk() {
        val receiver = AudioReceiver()
        val sessionId = receiver.createSession()
        val chunkData = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        
        val result = receiver.addAudioChunk(sessionId, chunkData)
        
        assertTrue("Chunk should be added successfully", result)
        assertEquals("Should have one chunk", 1, receiver.getChunkCount(sessionId))
    }
    
    @Test
    fun testAddChunkToNonExistentSession() {
        val receiver = AudioReceiver()
        val sessionId = "non-existent-session"
        val chunkData = byteArrayOf(0x01, 0x02)
        
        val result = receiver.addAudioChunk(sessionId, chunkData)
        
        assertFalse("Should fail to add chunk to non-existent session", result)
        assertEquals("Should have no active sessions", 0, receiver.activeSessions.size)
    }
    
    @Test
    fun testClearSession() {
        val receiver = AudioReceiver()
        val sessionId = receiver.createSession()
        receiver.addAudioChunk(sessionId, byteArrayOf(0x01, 0x02, 0x03))
        
        receiver.clearSession(sessionId)
        
        assertEquals("Session should be cleared", 0, receiver.getChunkCount(sessionId))
        assertFalse("Session should not be in active sessions", receiver.activeSessions.containsKey(sessionId))
    }
    
    @Test
    fun testClearAll() {
        val receiver = AudioReceiver()
        val sessionId1 = receiver.createSession()
        val sessionId2 = receiver.createSession()
        
        receiver.addAudioChunk(sessionId1, byteArrayOf(0x01, 0x02))
        receiver.addAudioChunk(sessionId2, byteArrayOf(0x03, 0x04))
        
        receiver.clearAll()
        
        assertEquals("Should have no active sessions", 0, receiver.activeSessions.size)
        assertEquals("Session 1 should be empty", 0, receiver.getChunkCount(sessionId1))
        assertEquals("Session 2 should be empty", 0, receiver.getChunkCount(sessionId2))
    }
    
    @Test
    fun testMultipleChunksInSession() {
        val receiver = AudioReceiver()
        val sessionId = receiver.createSession()
        
        receiver.addAudioChunk(sessionId, byteArrayOf(0x01, 0x02))
        receiver.addAudioChunk(sessionId, byteArrayOf(0x03, 0x04))
        receiver.addAudioChunk(sessionId, byteArrayOf(0x05, 0x06))
        
        assertEquals("Should have 3 chunks", 3, receiver.getChunkCount(sessionId))
    }
    
    @Test
    fun testGetActiveSessions() {
        val receiver = AudioReceiver()
        val sessionId1 = receiver.createSession()
        val sessionId2 = receiver.createSession()
        
        val sessions = receiver.getActiveSessions()
        
        assertTrue("Should have at least 2 sessions", sessions.size >= 2)
        assertTrue("Session 1 should be in list", sessions.contains(sessionId1))
        assertTrue("Session 2 should be in list", sessions.contains(sessionId2))
    }
}
