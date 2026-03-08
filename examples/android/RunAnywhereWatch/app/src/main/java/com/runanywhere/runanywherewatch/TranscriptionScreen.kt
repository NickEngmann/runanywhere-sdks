package com.runanywhere.runanywherewatch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Transcription display screen for the watch app.
 * Shows timestamped transcriptions in a scrollable list
 * with real-time updates and source indicators.
 */

data class TranscriptionItem(
    val id: Int,
    val text: String,
    val timestamp: Long,
    val source: String = "voice",
    val confidence: Float = 1.0f
)

@Composable
fun TranscriptionScreen(
    transcriptions: List<TranscriptionItem>,
    isListening: Boolean = false,
    onClearAll: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new transcriptions
    LaunchedEffect(transcriptions.size) {
        if (transcriptions.isNotEmpty()) {
            listState.animateScrollToItem(transcriptions.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(32.dp)
            ) {
                Text("<", color = Color(0xFF00E5FF), fontSize = 18.sp)
            }

            Text(
                text = "Transcriptions",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )

            // Listening indicator
            if (isListening) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF4444))
                )
            } else {
                Spacer(modifier = Modifier.size(12.dp))
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF00E5FF).copy(alpha = 0.3f))
        )

        // Transcription list
        if (transcriptions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No transcriptions yet",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap the mic to start",
                        color = Color(0xFF00E5FF).copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(transcriptions) { item ->
                    TranscriptionCard(item)
                }
            }
        }

        // Footer with count and clear
        if (transcriptions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${transcriptions.size} entries",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )

                TextButton(
                    onClick = onClearAll,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Clear",
                        color = Color(0xFFFF4444).copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TranscriptionCard(item: TranscriptionItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            // Timestamp and source row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(item.timestamp),
                    color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Source badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(sourceColor(item.source).copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = item.source,
                            color = sourceColor(item.source),
                            fontSize = 9.sp
                        )
                    }

                    // Confidence indicator
                    if (item.confidence < 0.8f) {
                        Text(
                            text = "${(item.confidence * 100).toInt()}%",
                            color = Color(0xFFFFAA00),
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Transcription text
            Text(
                text = item.text,
                color = Color.White,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return format.format(Date(timestamp))
}

private fun sourceColor(source: String): Color {
    return when (source.lowercase()) {
        "voice" -> Color(0xFF00E5FF)
        "keyboard" -> Color(0xFF00FF88)
        "system" -> Color(0xFFFFAA00)
        else -> Color(0xFF888888)
    }
}
