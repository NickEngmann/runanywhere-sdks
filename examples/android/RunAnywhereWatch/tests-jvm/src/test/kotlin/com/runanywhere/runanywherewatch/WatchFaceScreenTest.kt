package com.runanywhere.runanywherewatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class WatchFaceScreenTest {
    
    @Test
    fun testSDKStatusDescription() {
        assertEquals("Not Loaded", SDKStatus.NOT_LOADED.getDescription())
        assertEquals("Ready", SDKStatus.READY.getDescription())
        assertEquals("Thinking", SDKStatus.THINKING.getDescription())
    }
    
    @Test
    fun testConnectionStatusDescription() {
        assertEquals("Disconnected", ConnectionStatus.DISCONNECTED.getDescription())
        assertEquals("Connecting", ConnectionStatus.CONNECTING.getDescription())
        assertEquals("Connected", ConnectionStatus.CONNECTED.getDescription())
    }
    
    @Test
    fun testTranscriptionEntryCreation() {
        val entry = TranscriptionEntry(
            id = 1,
            text = "Test transcription",
            isComplete = false,
            timestamp = "2024-01-01T12:00:00"
        )
        
        assertEquals(1, entry.id)
        assertEquals("Test transcription", entry.text)
        assertFalse(entry.isComplete)
    }
    
    @Test
    fun testTranscriptionViewModel() {
        val viewModel = TranscriptionViewModel()
        
        assertEquals(0, viewModel.transcriptions.size)
        assertEquals(ConnectionStatus.DISCONNECTED, viewModel.connectionStatus)
        
        viewModel.addTranscription("Test transcription")
        
        assertEquals(1, viewModel.transcriptions.size)
        assertEquals("Test transcription", viewModel.transcriptions[0].text)
        assertFalse(viewModel.transcriptions[0].isComplete)
        
        viewModel.completeTranscription(0)
        assertTrue(viewModel.transcriptions[0].isComplete)
    }
    
    @Test
    fun testFormatTimestamp() {
        val timestamp = "2024-01-01T12:30:45"
        val formatted = formatTimestamp(timestamp)
        
        assertTrue(formatted.contains("2024"))
        assertTrue(formatted.contains("01"))
        assertTrue(formatted.contains("01"))
        assertTrue(formatted.contains("12"))
        assertTrue(formatted.contains("30"))
        assertTrue(formatted.contains("45"))
    }
}
