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

    LaunchedEffect(transcriptions.size) {
        if (transcriptions.isNotEmpty()) {
            listState.animateScrollToItem(transcriptions.size - 1)
        }
    }

    AdaptiveLayout {
        val cfg = LocalScreenConfig.current
        // Round safe area: content must avoid bezel clipping
        val hPad = if (cfg.isWatch) 24.dp else 12.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0D0D))
                .padding(top = if (cfg.isWatch) 10.dp else 0.dp)
        ) {
            // Header — back button visible with background, title centered
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = hPad, vertical = if (cfg.isWatch) 6.dp else 8.dp)
            ) {
                // Back button — with visible background on watch
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(if (cfg.isWatch) 22.dp else 32.dp)
                        .then(
                            if (cfg.isWatch) Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF1A1A1A))
                            else Modifier
                        )
                ) {
                    Text(
                        "\u2190",
                        color = Color(0xFF00E5FF),
                        fontSize = if (cfg.isWatch) 12.sp else 18.sp
                    )
                }

                // Title — always centered in the row
                Text(
                    text = if (cfg.isWatch) "Transcripts" else "Transcriptions",
                    color = Color.White,
                    fontSize = cfg.headerFontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Listening indicator
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(cfg.statusDotSize)
                            .clip(CircleShape)
                            .background(Color(0xFFFF4444))
                    )
                }
            }

            // Divider — inset on watch to avoid bezel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (cfg.isWatch) (hPad + 4.dp) else hPad)
                    .height(1.dp)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
            )

            // Content
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
                            fontSize = cfg.bodyFontSize
                        )
                        Spacer(modifier = Modifier.height(cfg.itemSpacing))
                        Text(
                            text = "Tap the mic to start",
                            color = Color(0xFF00E5FF).copy(alpha = 0.5f),
                            fontSize = cfg.captionFontSize
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = hPad),
                    verticalArrangement = Arrangement.spacedBy(cfg.itemSpacing),
                    contentPadding = PaddingValues(vertical = cfg.itemSpacing)
                ) {
                    items(transcriptions) { item ->
                        TranscriptionCard(item, cfg)
                    }
                }
            }

            // Footer — compact on watch to avoid bezel clipping
            if (transcriptions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (cfg.isWatch) (hPad + 8.dp) else hPad,
                            vertical = if (cfg.isWatch) 2.dp else 6.dp
                        )
                        .padding(bottom = if (cfg.isWatch) 8.dp else 0.dp),
                    horizontalArrangement = if (cfg.isWatch) Arrangement.Center else Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!cfg.isWatch) {
                        Text(
                            text = "${transcriptions.size} entries",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = cfg.captionFontSize
                        )
                    }

                    TextButton(
                        onClick = onClearAll,
                        contentPadding = PaddingValues(
                            horizontal = if (cfg.isWatch) 4.dp else 8.dp,
                            vertical = 2.dp
                        )
                    ) {
                        Text(
                            text = if (cfg.isWatch) "Clear all" else "Clear",
                            color = Color(0xFFFF4444).copy(alpha = 0.7f),
                            fontSize = cfg.captionFontSize
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TranscriptionCard(item: TranscriptionItem, cfg: ScreenConfig) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (cfg.isWatch) 6.dp else 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Column(
            modifier = Modifier.padding(if (cfg.isWatch) 6.dp else 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(item.timestamp),
                    color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                    fontSize = if (cfg.isWatch) 8.sp else 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(sourceColor(item.source).copy(alpha = 0.2f))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = item.source,
                            color = sourceColor(item.source),
                            fontSize = if (cfg.isWatch) 7.sp else 9.sp
                        )
                    }

                    if (item.confidence < 0.8f) {
                        Text(
                            text = "${(item.confidence * 100).toInt()}%",
                            color = Color(0xFFFFAA00),
                            fontSize = if (cfg.isWatch) 7.sp else 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (cfg.isWatch) 2.dp else 4.dp))

            Text(
                text = item.text,
                color = Color.White,
                fontSize = if (cfg.isWatch) 10.sp else 13.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = if (cfg.isWatch) 2 else 4,
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
