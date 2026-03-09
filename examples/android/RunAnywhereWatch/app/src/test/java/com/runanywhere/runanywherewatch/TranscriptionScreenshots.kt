package com.runanywhere.runanywherewatch

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

class TranscriptionWatchScreenshots {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.WEAR_OS_SMALL_ROUND,
        theme = "android:Theme.Material.NoActionBar"
    )

    private val sampleEntries = listOf(
        TranscriptionItem(
            id = 1,
            text = "Hey, check out the new RunAnywhere SDK.",
            timestamp = 1741500000000L,
            source = "voice",
            confidence = 0.95f
        ),
        TranscriptionItem(
            id = 2,
            text = "Running speech-to-text locally on the watch.",
            timestamp = 1741500060000L,
            source = "voice",
            confidence = 0.88f
        ),
        TranscriptionItem(
            id = 3,
            text = "MCP server started on port 8400",
            timestamp = 1741500120000L,
            source = "system",
            confidence = 1.0f
        )
    )

    @Test
    fun transcription_watch_empty() {
        paparazzi.snapshot {
            WatchFaceTheme {
                TranscriptionScreen(
                    transcriptions = emptyList(),
                    isListening = false
                )
            }
        }
    }

    @Test
    fun transcription_watch_withEntries() {
        paparazzi.snapshot {
            WatchFaceTheme {
                TranscriptionScreen(
                    transcriptions = sampleEntries,
                    isListening = true
                )
            }
        }
    }

    @Test
    fun transcription_watch_lowConfidence() {
        val entries = listOf(
            TranscriptionItem(
                id = 1,
                text = "Low confidence transcription",
                timestamp = 1741500000000L,
                source = "voice",
                confidence = 0.65f
            ),
            TranscriptionItem(
                id = 2,
                text = "Keyboard input — always high",
                timestamp = 1741500060000L,
                source = "keyboard",
                confidence = 1.0f
            )
        )

        paparazzi.snapshot {
            WatchFaceTheme {
                TranscriptionScreen(
                    transcriptions = entries,
                    isListening = false
                )
            }
        }
    }
}

class TranscriptionPhoneScreenshots {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoActionBar"
    )

    private val sampleEntries = listOf(
        TranscriptionItem(
            id = 1,
            text = "Hey, check out the new RunAnywhere SDK — it supports on-device Qwen3.5 inference now.",
            timestamp = 1741500000000L,
            source = "voice",
            confidence = 0.95f
        ),
        TranscriptionItem(
            id = 2,
            text = "Running speech-to-text locally with Whisper large-v3-turbo.",
            timestamp = 1741500060000L,
            source = "voice",
            confidence = 0.88f
        ),
        TranscriptionItem(
            id = 3,
            text = "System initialized MCP server on port 8400",
            timestamp = 1741500120000L,
            source = "system",
            confidence = 1.0f
        ),
        TranscriptionItem(
            id = 4,
            text = "This is a lower confidence result that may need review",
            timestamp = 1741500180000L,
            source = "voice",
            confidence = 0.72f
        )
    )

    @Test
    fun transcription_phone_empty() {
        paparazzi.snapshot {
            WatchFaceTheme {
                TranscriptionScreen(
                    transcriptions = emptyList(),
                    isListening = false
                )
            }
        }
    }

    @Test
    fun transcription_phone_withEntries() {
        paparazzi.snapshot {
            WatchFaceTheme {
                TranscriptionScreen(
                    transcriptions = sampleEntries,
                    isListening = true
                )
            }
        }
    }
}
