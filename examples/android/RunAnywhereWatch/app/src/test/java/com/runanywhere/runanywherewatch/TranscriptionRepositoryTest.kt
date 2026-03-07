package com.runanywhere.runanywherewatch

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class TranscriptionRepositoryTest {
    
    @Test
    fun testInsertTranscription() {
        val repository = TranscriptionRepository()
        val transcription = TranscriptionEntity(
            sessionId = "test-session-1",
            transcription = "Hello world",
            timestamp = System.currentTimeMillis(),
            confidence = 0.95f,
            durationSeconds = 2.5,
            metadata = "test-metadata"
        )
        
        repository.insert(transcription)
        
        val all = repository.getAllSync()
        assertEquals("Should have 1 transcription", 1, all.size)
        assertEquals("Transcription should match", "Hello world", all[0].transcription)
    }
    
    @Test
    fun testInsertMultipleTranscriptions() {
        val repository = TranscriptionRepository()
        
        repository.insert(TranscriptionEntity(
            sessionId = "session-1",
            transcription = "First transcription",
            timestamp = 1000,
            confidence = 0.9f,
            durationSeconds = 1.0,
            metadata = "meta-1"
        ))
        
        repository.insert(TranscriptionEntity(
            sessionId = "session-2",
            transcription = "Second transcription",
            timestamp = 2000,
            confidence = 0.85f,
            durationSeconds = 1.5,
            metadata = "meta-2"
        ))
        
        val all = repository.getAllSync()
        assertEquals("Should have 2 transcriptions", 2, all.size)
        assertEquals("First transcription should be first", "First transcription", all[0].transcription)
        assertEquals("Second transcription should be second", "Second transcription", all[1].transcription)
    }
    
    @Test
    fun testGetTranscriptionsBySession() {
        val repository = TranscriptionRepository()
        val sessionId = "test-session"
        
        repository.insert(TranscriptionEntity(
            sessionId = sessionId,
            transcription = "Transcription 1",
            timestamp = 1000,
            confidence = 0.9f,
            durationSeconds = 1.0,
            metadata = "meta-1"
        ))
        
        repository.insert(TranscriptionEntity(
            sessionId = "other-session",
            transcription = "Other transcription",
            timestamp = 2000,
            confidence = 0.8f,
            durationSeconds = 2.0,
            metadata = "meta-2"
        ))
        
        repository.insert(TranscriptionEntity(
            sessionId = sessionId,
            transcription = "Transcription 2",
            timestamp = 3000,
            confidence = 0.95f,
            durationSeconds = 1.5,
            metadata = "meta-3"
        ))
        
        val sessionTranscriptions = repository.getTranscriptionsBySession(sessionId)
        assertEquals("Should have 2 transcriptions for session", 2, sessionTranscriptions.size)
        assertEquals("First should be Transcription 1", "Transcription 1", sessionTranscriptions[0].transcription)
        assertEquals("Second should be Transcription 2", "Transcription 2", sessionTranscriptions[1].transcription)
    }
    
    @Test
    fun testGetTranscriptionsBySessionNonExistent() {
        val repository = TranscriptionRepository()
        repository.insert(TranscriptionEntity(
            sessionId = "session-1",
            transcription = "Transcription 1",
            timestamp = 1000,
            confidence = 0.9f,
            durationSeconds = 1.0,
            metadata = "meta-1"
        ))
        
        val sessionTranscriptions = repository.getTranscriptionsBySession("non-existent-session")
        assertTrue("Should have no transcriptions", sessionTranscriptions.isEmpty())
    }
    
    @Test
    fun testDeleteTranscription() {
        val repository = TranscriptionRepository()
        val transcriptionId = "test-id"
        
        repository.insert(TranscriptionEntity(
            sessionId = transcriptionId,
            transcription = "To be deleted",
            timestamp = 1000,
            confidence = 0.9f,
            durationSeconds = 1.0,
            metadata = "meta-1"
        ))
        
        repository.delete(transcriptionId)
        
        val all = repository.getAllSync()
        assertEquals("Should have 0 transcriptions after delete", 0, all.size)
    }
    
    @Test
    fun testDeleteAll() {
        val repository = TranscriptionRepository()
        
        repository.insert(TranscriptionEntity(
            sessionId = "session-1",
            transcription = "Transcription 1",
            timestamp = 1000,
            confidence = 0.9f,
            durationSeconds = 1.0,
            metadata = "meta-1"
        ))
        
        repository.insert(TranscriptionEntity(
            sessionId = "session-2",
            transcription = "Transcription 2",
            timestamp = 2000,
            confidence = 0.85f,
            durationSeconds = 1.5,
            metadata = "meta-2"
        ))
        
        repository.deleteAll()
        
        val all = repository.getAllSync()
        assertEquals("Should have 0 transcriptions after delete all", 0, all.size)
    }
    
    @Test
    fun testGetAllSortedByTimestamp() {
        val repository = TranscriptionRepository()
        
        repository.insert(TranscriptionEntity(
            sessionId = "session-1",
            transcription = "Old transcription",
            timestamp = 1000,
            confidence = 0.9f,
            durationSeconds = 1.0,
            metadata = "meta-1"
        ))
        
        repository.insert(TranscriptionEntity(
            sessionId = "session-2",
            transcription = "New transcription",
            timestamp = 3000,
            confidence = 0.85f,
            durationSeconds = 1.5,
            metadata = "meta-2"
        ))
        
        repository.insert(TranscriptionEntity(
            sessionId = "session-3",
            transcription = "Middle transcription",
            timestamp = 2000,
            confidence = 0.8f,
            durationSeconds = 1.2,
            metadata = "meta-3"
        ))
        
        val all = repository.getAllSync()
        assertEquals("First should be oldest", "Old transcription", all[0].transcription)
        assertEquals("Middle should be middle", "Middle transcription", all[1].transcription)
        assertEquals("Last should be newest", "New transcription", all[2].transcription)
    }
    
    @Test
    fun testGetTranscriptionCount() {
        val repository = TranscriptionRepository()
        
        repository.insert(TranscriptionEntity(
            sessionId = "session-1",
            transcription = "Transcription 1",
            timestamp = 1000,
            confidence = 0.9f,
            durationSeconds = 1.0,
            metadata = "meta-1"
        ))
        
        repository.insert(TranscriptionEntity(
            sessionId = "session-2",
            transcription = "Transcription 2",
            timestamp = 2000,
            confidence = 0.85f,
            durationSeconds = 1.5,
            metadata = "meta-2"
        ))
        
        assertEquals("Should have 2 transcriptions", 2, repository.getTranscriptionCount())
    }
    
    @Test
    fun testEmptyRepository() {
        val repository = TranscriptionRepository()
        
        assertTrue("Should have no transcriptions", repository.getAllSync().isEmpty())
        assertEquals("Count should be 0", 0, repository.getTranscriptionCount())
    }
}
