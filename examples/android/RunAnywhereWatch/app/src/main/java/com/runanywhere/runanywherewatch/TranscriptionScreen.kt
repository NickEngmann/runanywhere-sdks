package com.runanywhere.runanywherewatch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Data class representing a transcription entry with timestamp
 */
data class TranscriptionEntry(
    val id: String,
    val text: String,
    val timestamp: Long,
    val isComplete: Boolean = true
)

/**
 * Enum for connection status
 */
enum class ConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    CONNECTING,
    ERROR
}

/**
 * Composable to display connection status indicator
 */
@Composable
fun ConnectionStatusIndicator(status: ConnectionStatus, modifier: Modifier = Modifier) {
    val statusColor = when (status) {
        ConnectionStatus.CONNECTED -> Color.Green
        ConnectionStatus.DISCONNECTED -> Color.Red
        ConnectionStatus.CONNECTING -> Color.Yellow
        ConnectionStatus.ERROR -> Color.Red
    }
    
    val statusText = when (status) {
        ConnectionStatus.CONNECTED -> "Connected"
        ConnectionStatus.DISCONNECTED -> "Disconnected"
        ConnectionStatus.CONNECTING -> "Connecting..."
        ConnectionStatus.ERROR -> "Error"
    }
    
    Row(
        modifier = modifier
            .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(statusColor, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = statusText,
            fontSize = 12.sp,
            color = statusColor,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Composable to display a single transcription entry with timestamp
 */
@Composable
fun TranscriptionEntryItem(entry: TranscriptionEntry, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(entry.timestamp),
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                if (!entry.isComplete) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.text,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Composable to display the main transcription screen
 */
@Composable
fun TranscriptionScreen(
    transcriptions: List<TranscriptionEntry>,
    connectionStatus: ConnectionStatus,
    modifier: Modifier = Modifier,
    onConnectionStatusChanged: ((ConnectionStatus) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Header with connection status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transcriptions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (onConnectionStatusChanged != null) {
                Surface(
                    modifier = Modifier.clickable {
                        onConnectionStatusChanged(
                            when (connectionStatus) {
                                ConnectionStatus.CONNECTED -> ConnectionStatus.DISCONNECTED
                                ConnectionStatus.DISCONNECTED -> ConnectionStatus.CONNECTED
                                else -> connectionStatus
                            }
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    ConnectionStatusIndicator(
                        status = connectionStatus,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                ConnectionStatusIndicator(
                    status = connectionStatus,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Transcription list
        if (transcriptions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No transcriptions yet",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transcriptions, key = { it.id }) { entry ->
                    TranscriptionEntryItem(
                        entry = entry,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

/**
 * Helper function to format timestamp to readable string
 */
@Composable
fun formatTimestamp(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(date)
}

/**
 * ViewModel state holder for TranscriptionScreen
 */
@Stable
class TranscriptionViewModel(
    private val initialConnectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED
) {
    private val _transcriptions = mutableStateListOf<TranscriptionEntry>()
    val transcriptions: List<TranscriptionEntry> get() = _transcriptions
    
    private val _connectionStatus = mutableStateOf(initialConnectionStatus)
    val connectionStatus: ConnectionStatus get() = _connectionStatus.value
    
    fun addTranscription(text: String, timestamp: Long = System.currentTimeMillis()) {
        val id = java.util.UUID.randomUUID().toString()
        _transcriptions.add(
            TranscriptionEntry(
                id = id,
                text = text,
                timestamp = timestamp,
                isComplete = true
            )
        )
        // Keep only last 100 entries to prevent memory issues
        if (_transcriptions.size > 100) {
            _transcriptions.removeAt(0)
        }
    }
    
    fun updateConnectionStatus(status: ConnectionStatus) {
        _connectionStatus.value = status
    }
    
    fun clearTranscriptions() {
        _transcriptions.clear()
    }
}

/**
 * Preview function for TranscriptionScreen
 */
@Composable
@Preview(showBackground = true)
fun TranscriptionScreenPreview() {
    val viewModel = remember {
        TranscriptionViewModel(ConnectionStatus.CONNECTED)
    }
    
    // Add some sample transcriptions
    LaunchedEffect(Unit) {
        viewModel.addTranscription("Hello, this is a test transcription.")
        viewModel.addTranscription("The connection status is working properly.")
        viewModel.addTranscription("Jetpack Compose makes UI development much easier.")
    }
    
    TranscriptionScreen(
        transcriptions = viewModel.transcriptions,
        connectionStatus = viewModel.connectionStatus
    )
}
